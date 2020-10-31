/* Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Originally written by Jean-Marc Valin, Gregory Maxwell, Koen Vos,
   Timothy B. Terriberry, and the Opus open-source contributors
   Ported to Java by Logan Stromberg

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:

   - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

   - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

   - Neither the name of Internet Society, IETF or IETF Trust, nor the
   names of specific contributors, may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
   OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.soywiz.korau.format.org.concentus

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korio.lang.*

internal object CeltCommon {

    /* Table of 6*64/x, trained on real data to minimize the average error */
    private val inv_table = shortArrayOf(
        255,
        255,
        156,
        110,
        86,
        70,
        59,
        51,
        45,
        40,
        37,
        33,
        31,
        28,
        26,
        25,
        23,
        22,
        21,
        20,
        19,
        18,
        17,
        16,
        16,
        15,
        15,
        14,
        13,
        13,
        12,
        12,
        12,
        12,
        11,
        11,
        11,
        10,
        10,
        10,
        9,
        9,
        9,
        9,
        9,
        9,
        8,
        8,
        8,
        8,
        8,
        7,
        7,
        7,
        7,
        7,
        7,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        6,
        5,
        5,
        5,
        5,
        5,
        5,
        5,
        5,
        5,
        5,
        5,
        5,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        4,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        3,
        2
    )

    private val gains = arrayOf(
        shortArrayOf(
            (0.5 + 0.3066406250f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.3066406250f, 15)*/,
            (0.5 + 0.2170410156f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.2170410156f, 15)*/,
            (0.5 + 0.1296386719f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.1296386719f, 15)*/
        ),
        shortArrayOf(
            (0.5 + 0.4638671875f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.4638671875f, 15)*/,
            (0.5 + 0.2680664062f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.2680664062f, 15)*/,
            (0.5 + 0.0f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.0f, 15)*/
        ),
        shortArrayOf(
            (0.5 + 0.7998046875f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.7998046875f, 15)*/,
            (0.5 + 0.1000976562f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.1000976562f, 15)*/,
            (0.5 + 0.0f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.0f, 15)*/
        )
    )

    private val tf_select_table = arrayOf(
        byteArrayOf(0, -1, 0, -1, 0, -1, 0, -1),
        byteArrayOf(0, -1, 0, -2, 1, 0, 1, -1),
        byteArrayOf(0, -2, 0, -3, 2, 0, 1, -1),
        byteArrayOf(0, -2, 0, -3, 3, 0, 1, -1)
    )

    fun compute_vbr(
        mode: CeltMode, analysis: AnalysisInfo, base_target: Int,
        LM: Int, bitrate: Int, lastCodedBands: Int, C: Int, intensity: Int,
        constrained_vbr: Int, stereo_saving: Int, tot_boost: Int,
        tf_estimate: Int, pitch_change: Int, maxDepth: Int,
        variable_duration: OpusFramesize, lfe: Int, has_surround_mask: Int, surround_masking: Int,
        temporal_vbr: Int
    ): Int {
        var stereo_saving = stereo_saving
        /* The target rate in 8th bits per frame */
        var target: Int
        var coded_bins: Int
        val coded_bands: Int
        val tf_calibration: Int
        val nbEBands: Int
        val eBands: ShortArray?

        nbEBands = mode.nbEBands
        eBands = mode.eBands

        coded_bands = if (lastCodedBands != 0) lastCodedBands else nbEBands
        coded_bins = eBands!![coded_bands] shl LM
        if (C == 2) {
            coded_bins += eBands[Inlines.IMIN(intensity, coded_bands)] shl LM
        }

        target = base_target
        if (analysis.enabled && analysis.valid != 0 && analysis.activity < .4) {
            target -= ((coded_bins shl EntropyCoder.BITRES) * (.4f - analysis.activity)).toInt()
        }

        /* Stereo savings */
        if (C == 2) {
            val coded_stereo_bands: Int
            val coded_stereo_dof: Int
            val max_frac: Int
            coded_stereo_bands = Inlines.IMIN(intensity, coded_bands)
            coded_stereo_dof = (eBands[coded_stereo_bands] shl LM) - coded_stereo_bands
            /* Maximum fraction of the bits we can save if the signal is mono. */
            max_frac = Inlines.DIV32_16(
                Inlines.MULT16_16(
                    (0.5 + 0.8f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(0.8f, 15)*/,
                    coded_stereo_dof
                ), coded_bins
            )
            stereo_saving = Inlines.MIN16(
                stereo_saving,
                (0.5 + 1.0f * (1 shl 8)).toShort().toInt()/*Inlines.QCONST16(1.0f, 8)*/
            )
            /*printf("%d %d %d ", coded_stereo_dof, coded_bins, tot_boost);*/
            target -= Inlines.MIN32(
                Inlines.MULT16_32_Q15(max_frac, target),
                Inlines.SHR32(
                    Inlines.MULT16_16(
                        stereo_saving - (0.5 + 0.1f * (1 shl 8)).toShort()/*Inlines.QCONST16(0.1f, 8)*/,
                        coded_stereo_dof shl EntropyCoder.BITRES
                    ), 8
                )
            ).toInt()
        }
        /* Boost the rate according to dynalloc (minus the dynalloc average for calibration). */
        target += tot_boost - (16 shl LM)
        /* Apply transient boost, compensating for average boost. */
        tf_calibration = (if (variable_duration == OpusFramesize.OPUS_FRAMESIZE_VARIABLE)
            (0.5 + 0.02f * (1 shl 14)).toShort()/*Inlines.QCONST16(0.02f, 14)*/
        else
            (0.5 + 0.04f * (1 shl 14)).toShort()).toInt()/*Inlines.QCONST16(0.04f, 14)*/
        target += Inlines.SHL32(Inlines.MULT16_32_Q15(tf_estimate - tf_calibration, target), 1).toInt()

        /* Apply tonality boost */
        if (analysis.enabled && analysis.valid != 0 && lfe == 0) {
            var tonal_target: Int
            val tonal: Float

            /* Tonality boost (compensating for the average). */
            tonal = Inlines.MAX16(0f, analysis.tonality - .15f) - 0.09f
            tonal_target = target + ((coded_bins shl EntropyCoder.BITRES).toFloat() * 1.2f * tonal).toInt()
            if (pitch_change != 0) {
                tonal_target += ((coded_bins shl EntropyCoder.BITRES) * .8f).toInt()
            }
            target = tonal_target
        }

        if (has_surround_mask != 0 && lfe == 0) {
            val surround_target = target + Inlines.SHR32(
                Inlines.MULT16_16(surround_masking, coded_bins shl EntropyCoder.BITRES),
                CeltConstants.DB_SHIFT
            ).toInt()
            /*printf("%f %d %d %d %d %d %d ", surround_masking, coded_bins, st.end, st.intensity, surround_target, target, st.bitrate);*/
            target = Inlines.IMAX(target / 4, surround_target)
        }

        run {
            var floor_depth: Int
            val bins: Int
            bins = eBands[nbEBands - 2] shl LM
            /*floor_depth = Inlines.SHR32(Inlines.MULT16_16((C*bins<<EntropyCoder.BITRES),celt_log2(Inlines.SHL32(Inlines.MAX16(1,sample_max),13))), CeltConstants.DB_SHIFT);*/
            floor_depth =
                    Inlines.SHR32(Inlines.MULT16_16(C * bins shl EntropyCoder.BITRES, maxDepth), CeltConstants.DB_SHIFT)
                        .toInt()
            floor_depth = Inlines.IMAX(floor_depth, target shr 2)
            target = Inlines.IMIN(target, floor_depth)
            /*printf("%f %d\n", maxDepth, floor_depth);*/
        }

        if ((has_surround_mask == 0 || lfe != 0) && (constrained_vbr != 0 || bitrate < 64000)) {
            var rate_factor: Int
            rate_factor = Inlines.MAX16(0, bitrate - 32000)
            if (constrained_vbr != 0) {
                rate_factor = Inlines.MIN16(
                    rate_factor,
                    (0.5 + 0.67f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(0.67f, 15)*/
                )
            }
            target = base_target + Inlines.MULT16_32_Q15(rate_factor, target - base_target).toInt()
        }

        if (has_surround_mask == 0 && tf_estimate < (0.5 + .2f * (1 shl 14)).toShort()/*Inlines.QCONST16(.2f, 14)*/) {
            val amount: Int
            val tvbr_factor: Int
            amount = Inlines.MULT16_16_Q15(
                (0.5 + .0000031f * (1 shl 30)).toShort().toInt()/*Inlines.QCONST16(.0000031f, 30)*/,
                Inlines.IMAX(0, Inlines.IMIN(32000, 96000 - bitrate))
            )
            tvbr_factor = Inlines.SHR32(Inlines.MULT16_16(temporal_vbr, amount), CeltConstants.DB_SHIFT)
            target += Inlines.MULT16_32_Q15(tvbr_factor, target).toInt()
        }

        /* Don't allow more than doubling the rate */
        target = Inlines.IMIN(2 * base_target, target)

        return target
    }

    fun transient_analysis(
        input: Array<IntArray>,
        len: Int,
        C: Int,
        tf_estimate: BoxedValueInt,
        tf_chan: BoxedValueInt
    ): Int {
        var i: Int
        val tmp: IntArray
        var mem0: Int
        var mem1: Int
        var is_transient = 0
        var mask_metric = 0
        var c: Int
        val tf_max: Int
        val len2: Int
        tf_chan.Val = 0
        tmp = IntArray(len)

        len2 = len / 2
        c = 0
        while (c < C) {
            var mean: Int
            var unmask = 0
            val norm: Int
            var maxE: Int
            mem0 = 0
            mem1 = 0
            /* High-pass filter: (1 - 2*z^-1 + z^-2) / (1 - z^-1 + .5*z^-2) */
            i = 0
            while (i < len) {
                val x: Int
                val y: Int
                x = Inlines.SHR32(input[c][i], CeltConstants.SIG_SHIFT)
                y = Inlines.ADD32(mem0, x)
                mem0 = mem1 + y - Inlines.SHL32(x, 1)
                mem1 = x - Inlines.SHR32(y, 1)
                tmp[i] = Inlines.EXTRACT16(Inlines.SHR32(y, 2)).toInt()
                i++
                /*printf("%f ", tmp[i]);*/
            }
            /*printf("\n");*/
            /* First few samples are bad because we don't propagate the memory */
            Arrays.MemSet(tmp, 0, 12)

            /* Normalize tmp to max range */
            run {
                var shift = 0
                shift = 14 - Inlines.celt_ilog2(1 + Inlines.celt_maxabs32(tmp, 0, len))
                if (shift != 0) {
                    i = 0
                    while (i < len) {
                        tmp[i] = Inlines.SHL16(tmp[i], shift)
                        i++
                    }
                }
            }

            mean = 0
            mem0 = 0
            /* Grouping by two to reduce complexity */
            /* Forward pass to compute the post-echo threshold*/
            i = 0
            while (i < len2) {
                val x2 = Inlines.PSHR32(
                    Inlines.MULT16_16(tmp[2 * i], tmp[2 * i]) + Inlines.MULT16_16(
                        tmp[2 * i + 1],
                        tmp[2 * i + 1]
                    ), 16
                )
                mean += x2
                tmp[i] = mem0 + Inlines.PSHR32(x2 - mem0, 4)
                mem0 = tmp[i]
                i++
            }

            mem0 = 0
            maxE = 0
            /* Backward pass to compute the pre-echo threshold */
            i = len2 - 1
            while (i >= 0) {
                tmp[i] = mem0 + Inlines.PSHR32(tmp[i] - mem0, 3)
                mem0 = tmp[i]
                maxE = Inlines.MAX16(maxE, mem0)
                i--
            }
            /*for (i=0;i<len2;i++)printf("%f ", tmp[i]/mean);printf("\n");*/

            /* Compute the ratio of the "frame energy" over the harmonic mean of the energy.
               This essentially corresponds to a bitrate-normalized temporal noise-to-mask
               ratio */

            /* As a compromise with the old transient detector, frame energy is the
               geometric mean of the energy and half the max */
            /* Costs two sqrt() to avoid overflows */
            mean = Inlines.MULT16_16(Inlines.celt_sqrt(mean), Inlines.celt_sqrt(Inlines.MULT16_16(maxE, len2 shr 1)))
            /* Inverse of the mean energy in Q15+6 */
            norm = Inlines.SHL32(len2, 6 + 14) / Inlines.ADD32(CeltConstants.EPSILON, Inlines.SHR32(mean, 1))
            /* Compute harmonic mean discarding the unreliable boundaries
               The data is smooth, so we only take 1/4th of the samples */
            unmask = 0
            i = 12
            while (i < len2 - 5) {
                val id: Int
                id = Inlines.MAX32(0, Inlines.MIN32(127, Inlines.MULT16_32_Q15(tmp[i] + CeltConstants.EPSILON, norm)))
                /* Do not round to nearest */
                unmask += inv_table[id].toInt()
                i += 4
            }
            /*printf("%d\n", unmask);*/
            /* Normalize, compensate for the 1/4th of the sample and the factor of 6 in the inverse table */
            unmask = 64 * unmask * 4 / (6 * (len2 - 17))
            if (unmask > mask_metric) {
                tf_chan.Val = c
                mask_metric = unmask
            }
            c++
        }
        is_transient = if (mask_metric > 200) 1 else 0

        /* Arbitrary metric for VBR boost */
        tf_max = Inlines.MAX16(0, Inlines.celt_sqrt(27 * mask_metric) - 42)
        /* *tf_estimate = 1 + Inlines.MIN16(1, sqrt(Inlines.MAX16(0, tf_max-30))/20); */
        tf_estimate.Val = Inlines.celt_sqrt(
            Inlines.MAX32(
                0,
                Inlines.SHL32(
                    Inlines.MULT16_16(
                        (0.5 + 0.0069f * (1 shl 14)).toShort().toInt()/*Inlines.QCONST16(0.0069f, 14)*/,
                        Inlines.MIN16(163, tf_max)
                    ), 14
                ) - (0.5 + 0.139f * (1 shl 28)).toInt()/*Inlines.QCONST32(0.139f, 28)*/
            )
        )
        /*printf("%d %f\n", tf_max, mask_metric);*/

        /*printf("%d %f %d\n", is_transient, (float)*tf_estimate, tf_max);*/
        return is_transient
    }

    /* Looks for sudden increases of energy to decide whether we need to patch
       the transient decision */
    fun patch_transient_decision(
        newE: Array<IntArray>, oldE: Array<IntArray>, nbEBands: Int,
        start: Int, end: Int, C: Int
    ): Int {
        var i: Int
        var c: Int
        var mean_diff = 0
        val spread_old = IntArray(26)
        /* Apply an aggressive (-6 dB/Bark) spreading function to the old frame to
           avoid false detection caused by irrelevant bands */
        if (C == 1) {
            spread_old[start] = oldE[0][start]
            i = start + 1
            while (i < end) {
                spread_old[i] = Inlines.MAX16(
                    spread_old[i - 1] - (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort(),
                    oldE[0][i]
                )
                i++
            }
        } else {
            spread_old[start] = Inlines.MAX16(oldE[0][start], oldE[1][start])
            i = start + 1
            while (i < end) {
                spread_old[i] = Inlines.MAX16(
                    spread_old[i - 1] - (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort(),
                    Inlines.MAX16(oldE[0][i], oldE[1][i])
                )
                i++
            }
        }
        i = end - 2
        while (i >= start) {
            spread_old[i] = Inlines.MAX16(
                spread_old[i],
                spread_old[i + 1] - (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
            )
            i--
        }
        /* Compute mean increase */
        c = 0
        do {
            i = Inlines.IMAX(2, start)
            while (i < end - 1) {
                val x1: Int
                val x2: Int
                x1 = Inlines.MAX16(0, newE[c][i])
                x2 = Inlines.MAX16(0, spread_old[i])
                mean_diff = Inlines.ADD32(mean_diff, Inlines.MAX16(0, Inlines.SUB16(x1, x2)))
                i++
            }
        } while (++c < C)
        mean_diff = Inlines.DIV32(mean_diff, C * (end - 1 - Inlines.IMAX(2, start)))
        /*printf("%f %f %d\n", mean_diff, max_diff, count);*/
        return if (mean_diff > (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()) 1 else 0
    }

    /**
     * Apply window and compute the MDCT for all sub-frames and all channels in
     * a frame
     */
    fun compute_mdcts(
        mode: CeltMode, shortBlocks: Int, input: Array<IntArray>,
        output: Array<IntArray>, C: Int, CC: Int, LM: Int, upsample: Int
    ) {
        val overlap = mode.overlap
        val N: Int
        val B: Int
        val shift: Int
        var i: Int
        var b: Int
        var c: Int
        if (shortBlocks != 0) {
            B = shortBlocks
            N = mode.shortMdctSize
            shift = mode.maxLM
        } else {
            B = 1
            N = mode.shortMdctSize shl LM
            shift = mode.maxLM - LM
        }
        c = 0
        do {
            b = 0
            while (b < B) {
                /* Interleaving the sub-frames while doing the MDCTs */
                MDCT.clt_mdct_forward(
                    mode.mdct,
                    input[c],
                    b * N,
                    output[c],
                    b,
					mode.window!!,
                    overlap,
                    shift,
                    B
                )
                b++
            }
        } while (++c < CC)

        if (CC == 2 && C == 1) {
            i = 0
            while (i < B * N) {
                output[0][i] = Inlines.ADD32(Inlines.HALF32(output[0][i]), Inlines.HALF32(output[1][i]))
                i++
            }
        }
        if (upsample != 1) {
            c = 0
            do {
                val bound = B * N / upsample
                i = 0
                while (i < bound) {
                    output[c][i] *= upsample
                    i++
                }
                Arrays.MemSetWithOffset(output[c], 0, bound, B * N - bound)
            } while (++c < C)
        }
    }

    fun celt_preemphasis(
        pcmp: ShortArray, pcmp_ptr: Int, inp: IntArray, inp_ptr: Int,
        N: Int, CC: Int, upsample: Int, coef: IntArray, mem: BoxedValueInt, clip: Int
    ) {
        var i: Int
        val coef0: Int
        var m: Int
        val Nu: Int

        coef0 = coef[0]
        m = mem.Val

        /* Fast path for the normal 48kHz case and no clipping */
        if (coef[1] == 0 && upsample == 1 && clip == 0) {
            i = 0
            while (i < N) {
                val x = pcmp[pcmp_ptr + CC * i].toInt()
                /* Apply pre-emphasis */
                inp[inp_ptr + i] = Inlines.SHL32(x, CeltConstants.SIG_SHIFT) - m
                m = Inlines.SHR32(Inlines.MULT16_16(coef0, x), 15 - CeltConstants.SIG_SHIFT)
                i++
            }
            mem.Val = m
            return
        }

        Nu = N / upsample
        if (upsample != 1) {
            Arrays.MemSetWithOffset(inp, 0, inp_ptr, N)
        }
        i = 0
        while (i < Nu) {
            inp[inp_ptr + i * upsample] = pcmp[pcmp_ptr + CC * i].toInt()
            i++
        }

        i = 0
        while (i < N) {
            val x: Int
            x = inp[inp_ptr + i]
            /* Apply pre-emphasis */
            inp[inp_ptr + i] = Inlines.SHL32(x, CeltConstants.SIG_SHIFT) - m
            m = Inlines.SHR32(Inlines.MULT16_16(coef0, x), 15 - CeltConstants.SIG_SHIFT)
            i++
        }

        mem.Val = m
    }

    fun celt_preemphasis(
        pcmp: ShortArray, inp: IntArray, inp_ptr: Int,
        N: Int, CC: Int, upsample: Int, coef: IntArray, mem: BoxedValueInt, clip: Int
    ) {
        var i: Int
        val coef0: Int
        var m: Int
        val Nu: Int

        coef0 = coef[0]
        m = mem.Val

        /* Fast path for the normal 48kHz case and no clipping */
        if (coef[1] == 0 && upsample == 1 && clip == 0) {
            i = 0
            while (i < N) {
                val x: Int
                x = pcmp[CC * i].toInt()
                /* Apply pre-emphasis */
                inp[inp_ptr + i] = Inlines.SHL32(x, CeltConstants.SIG_SHIFT) - m
                m = Inlines.SHR32(Inlines.MULT16_16(coef0, x), 15 - CeltConstants.SIG_SHIFT)
                i++
            }
            mem.Val = m
            return
        }

        Nu = N / upsample
        if (upsample != 1) {
            Arrays.MemSetWithOffset(inp, 0, inp_ptr, N)
        }
        i = 0
        while (i < Nu) {
            inp[inp_ptr + i * upsample] = pcmp[CC * i].toInt()
            i++
        }

        i = 0
        while (i < N) {
            val x: Int
            x = inp[inp_ptr + i]
            /* Apply pre-emphasis */
            inp[inp_ptr + i] = Inlines.SHL32(x, CeltConstants.SIG_SHIFT) - m
            m = Inlines.SHR32(Inlines.MULT16_16(coef0, x), 15 - CeltConstants.SIG_SHIFT)
            i++
        }

        mem.Val = m
    }

    fun l1_metric(tmp: IntArray, N: Int, LM: Int, bias: Int): Int {
        var i: Int
        var L1: Int
        L1 = 0
        i = 0
        while (i < N) {
            L1 += Inlines.EXTEND32(Inlines.ABS32(tmp[i]))
            i++
        }

        /* When in doubt, prefer good freq resolution */
        L1 = Inlines.MAC16_32_Q15(L1, LM * bias, L1)
        return L1

    }

    fun tf_analysis(
        m: CeltMode, len: Int, isTransient: Int,
        tf_res: IntArray, lambda: Int, X: Array<IntArray>, N0: Int, LM: Int,
        tf_sum: BoxedValueInt, tf_estimate: Int, tf_chan: Int
    ): Int {
        var i: Int
        val metric: IntArray
        var cost0: Int
        var cost1: Int
        val path0: IntArray
        val path1: IntArray
        val tmp: IntArray
        val tmp_1: IntArray
        var sel: Int
        val selcost = IntArray(2)
        var tf_select = 0
        val bias: Int

        bias = Inlines.MULT16_16_Q14(
            (0.5 + .04f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.04f, 15)*/,
            Inlines.MAX16(
                (0 - (0.5 + .25f * (1 shl 14)).toShort()/*Inlines.QCONST16(.25f, 14)*/).toShort().toInt(),
                (0.5 + .5f * (1 shl 14)).toShort()/*Inlines.QCONST16(.5f, 14)*/ - tf_estimate
            )
        )
        /*printf("%f ", bias);*/

        metric = IntArray(len)
        tmp = IntArray(m.eBands!![len] - m.eBands!![len - 1] shl LM)
        tmp_1 = IntArray(m.eBands!![len] - m.eBands!![len - 1] shl LM)
        path0 = IntArray(len)
        path1 = IntArray(len)

        tf_sum.Val = 0
        i = 0
        while (i < len) {
            var k: Int
            val N: Int
            val narrow: Int
            var L1: Int
            var best_L1: Int
            var best_level = 0
            N = m.eBands!![i + 1] - m.eBands!![i] shl LM
            /* band is too narrow to be split down to LM=-1 */
            narrow = if (m.eBands!![i + 1] - m.eBands!![i] == 1) 1 else 0
            arraycopy(X[tf_chan], m.eBands!![i] shl LM, tmp, 0, N)
            /* Just add the right channel if we're in stereo */
            /*if (C==2)
               for (j=0;j<N;j++)
                  tmp[j] = ADD16(SHR16(tmp[j], 1),SHR16(X[N0+j+(m.eBands[i]<<LM)], 1));*/
            L1 = l1_metric(tmp, N, if (isTransient != 0) LM else 0, bias)
            best_L1 = L1
            /* Check the -1 case for transients */
            if (isTransient != 0 && narrow == 0) {
                arraycopy(tmp, 0, tmp_1, 0, N)
                Bands.haar1ZeroOffset(tmp_1, N shr LM, 1 shl LM)
                L1 = l1_metric(tmp_1, N, LM + 1, bias)
                if (L1 < best_L1) {
                    best_L1 = L1
                    best_level = -1
                }
            }
            /*printf ("%f ", L1);*/
            k = 0
            while (k < LM + if (!(isTransient != 0 || narrow != 0)) 1 else 0) {
                val B: Int

                if (isTransient != 0) {
                    B = LM - k - 1
                } else {
                    B = k + 1
                }

                Bands.haar1ZeroOffset(tmp, N shr k, 1 shl k)

                L1 = l1_metric(tmp, N, B, bias)

                if (L1 < best_L1) {
                    best_L1 = L1
                    best_level = k + 1
                }
                k++
            }
            /*printf ("%d ", isTransient ? LM-best_level : best_level);*/
            /* metric is in Q1 to be able to select the mid-point (-0.5) for narrower bands */
            if (isTransient != 0) {
                metric[i] = 2 * best_level
            } else {
                metric[i] = -2 * best_level
            }
            tf_sum.Val += (if (isTransient != 0) LM else 0) - metric[i] / 2
            /* For bands that can't be split to -1, set the metric to the half-way point to avoid
               biasing the decision */
            if (narrow != 0 && (metric[i] == 0 || metric[i] == -2 * LM)) {
                metric[i] -= 1
            }
            i++
            /*printf("%d ", metric[i]);*/
        }
        /*printf("\n");*/
        /* Search for the optimal tf resolution, including tf_select */
        tf_select = 0
        sel = 0
        while (sel < 2) {
            cost0 = 0
            cost1 = if (isTransient != 0) 0 else lambda
            i = 1
            while (i < len) {
                val curr0: Int
                val curr1: Int
                curr0 = Inlines.IMIN(cost0, cost1 + lambda)
                curr1 = Inlines.IMIN(cost0 + lambda, cost1)
                cost0 = curr0 +
                        Inlines.abs(metric[i] - 2 * CeltTables.tf_select_table[LM][4 * isTransient + 2 * sel + 0])
                cost1 = curr1 +
                        Inlines.abs(metric[i] - 2 * CeltTables.tf_select_table[LM][4 * isTransient + 2 * sel + 1])
                i++
            }
            cost0 = Inlines.IMIN(cost0, cost1)
            selcost[sel] = cost0
            sel++
        }
        /* For now, we're conservative and only allow tf_select=1 for transients.
         * If tests confirm it's useful for non-transients, we could allow it. */
        if (selcost[1] < selcost[0] && isTransient != 0) {
            tf_select = 1
        }
        cost0 = 0
        cost1 = if (isTransient != 0) 0 else lambda
        /* Viterbi forward pass */
        i = 1
        while (i < len) {
            val curr0: Int
            val curr1: Int
            var from0: Int
            var from1: Int

            from0 = cost0
            from1 = cost1 + lambda
            if (from0 < from1) {
                curr0 = from0
                path0[i] = 0
            } else {
                curr0 = from1
                path0[i] = 1
            }

            from0 = cost0 + lambda
            from1 = cost1
            if (from0 < from1) {
                curr1 = from0
                path1[i] = 0
            } else {
                curr1 = from1
                path1[i] = 1
            }
            cost0 = curr0 +
                    Inlines.abs(metric[i] - 2 * CeltTables.tf_select_table[LM][4 * isTransient + 2 * tf_select + 0])
            cost1 = curr1 +
                    Inlines.abs(metric[i] - 2 * CeltTables.tf_select_table[LM][4 * isTransient + 2 * tf_select + 1])
            i++
        }
        tf_res[len - 1] = if (cost0 < cost1) 0 else 1
        /* Viterbi backward pass to check the decisions */
        i = len - 2
        while (i >= 0) {
            if (tf_res[i + 1] == 1) {
                tf_res[i] = path1[i + 1]
            } else {
                tf_res[i] = path0[i + 1]
            }
            i--
        }
        /*printf("%d %f\n", *tf_sum, tf_estimate);*/

        return tf_select
    }

    fun tf_encode(
        start: Int,
        end: Int,
        isTransient: Int,
        tf_res: IntArray,
        LM: Int,
        tf_select: Int,
        enc: EntropyCoder
    ) {
        var tf_select = tf_select
        var curr: Int
        var i: Int
        val tf_select_rsv: Int
        var tf_changed: Int
        var logp: Int
        var budget: Int
        var tell: Int
        budget = enc.storage * 8
        tell = enc.tell()
        logp = if (isTransient != 0) 2 else 4
        /* Reserve space to code the tf_select decision. */
        tf_select_rsv = if (LM > 0 && tell + logp + 1 <= budget) 1 else 0
        budget -= tf_select_rsv
        tf_changed = 0
        curr = tf_changed
        i = start
        while (i < end) {
            if (tell + logp <= budget) {
                enc.enc_bit_logp(tf_res[i] xor curr, logp)
                tell = enc.tell()
                curr = tf_res[i]
                tf_changed = tf_changed or curr
            } else {
                tf_res[i] = curr
            }
            logp = if (isTransient != 0) 4 else 5
            i++
        }
        /* Only code tf_select if it would actually make a difference. */
        if (tf_select_rsv != 0 && CeltTables.tf_select_table[LM][4 * isTransient + 0 + tf_changed] != CeltTables.tf_select_table[LM][4 * isTransient + 2 + tf_changed]) {
            enc.enc_bit_logp(tf_select, 1)
        } else {
            tf_select = 0
        }
        i = start
        while (i < end) {
            tf_res[i] = CeltTables.tf_select_table[LM][4 * isTransient + 2 * tf_select + tf_res[i]].toInt()
            i++
        }
        /*for(i=0;i<end;i++)printf("%d ", isTransient ? tf_res[i] : LM+tf_res[i]);printf("\n");*/
    }

    fun alloc_trim_analysis(
        m: CeltMode, X: Array<IntArray>,
        bandLogE: Array<IntArray>, end: Int, LM: Int, C: Int,
        analysis: AnalysisInfo, stereo_saving: BoxedValueInt, tf_estimate: Int,
        intensity: Int, surround_trim: Int
    ): Int {
        var i: Int
        var diff = 0
        var c: Int
        var trim_index: Int
        var trim = (0.5 + 5.0f * (1 shl 8)).toShort().toInt()/*Inlines.QCONST16(5.0f, 8)*/
        var logXC: Int
        var logXC2: Int
        if (C == 2) {
            var sum = 0
            /* Q10 */
            var minXC: Int
            /* Q10 */
            /* Compute inter-channel correlation for low frequencies */
            i = 0
            while (i < 8) {
                val partial: Int
                partial = Kernels.celt_inner_prod(
                    X[0], m.eBands!![i] shl LM, X[1], m.eBands!![i] shl LM,
                    m.eBands!![i + 1] - m.eBands!![i] shl LM
                )
                sum = Inlines.ADD16(sum, Inlines.EXTRACT16(Inlines.SHR32(partial, 18)).toInt())
                i++
            }
            sum = Inlines.MULT16_16_Q15(
                (0.5 + 1.0f / 8 * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(1.0f / 8, 15)*/,
                sum
            )
            sum = Inlines.MIN16(
                (0.5 + 1.0f * (1 shl 10)).toShort().toInt()/*Inlines.QCONST16(1.0f, 10)*/,
                Inlines.ABS32(sum)
            )
            minXC = sum
            i = 8
            while (i < intensity) {
                val partial: Int
                partial = Kernels.celt_inner_prod(
                    X[0], m.eBands!![i] shl LM, X[1], m.eBands!![i] shl LM,
                    m.eBands!![i + 1] - m.eBands!![i] shl LM
                )
                minXC = Inlines.MIN16(minXC, Inlines.ABS16(Inlines.EXTRACT16(Inlines.SHR32(partial, 18))).toInt())
                i++
            }
            minXC = Inlines.MIN16(
                (0.5 + 1.0f * (1 shl 10)).toShort().toInt()/*Inlines.QCONST16(1.0f, 10)*/,
                Inlines.ABS32(minXC)
            )
            /*printf ("%f\n", sum);*/
            /* mid-side savings estimations based on the LF average*/
            logXC = Inlines.celt_log2(
                (0.5 + 1.001f * (1 shl 20)).toInt()/*Inlines.QCONST32(1.001f, 20)*/ - Inlines.MULT16_16(
                    sum,
                    sum
                )
            )
            /* mid-side savings estimations based on min correlation */
            logXC2 = Inlines.MAX16(
                Inlines.HALF16(logXC),
                Inlines.celt_log2(
                    (0.5 + 1.001f * (1 shl 20)).toInt()/*Inlines.QCONST32(1.001f, 20)*/ - Inlines.MULT16_16(
                        minXC,
                        minXC
                    )
                )
            )
            /* Compensate for Q20 vs Q14 input and convert output to Q8 */
            logXC = Inlines.PSHR32(
                logXC - (0.5 + 6.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(6.0f, CeltConstants.DB_SHIFT)*/,
                CeltConstants.DB_SHIFT - 8
            )
            logXC2 = Inlines.PSHR32(
                logXC2 - (0.5 + 6.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(6.0f, CeltConstants.DB_SHIFT)*/,
                CeltConstants.DB_SHIFT - 8
            )

            trim += Inlines.MAX16(
                0 - (0.5 + 4.0f * (1 shl 8)).toShort(),
                Inlines.MULT16_16_Q15((0.5 + .75f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.75f, 15)*/, logXC)
            )
            stereo_saving.Val =
                    Inlines.MIN16(stereo_saving.Val + (0.5 + 0.25f * (1 shl 8)).toShort(), 0 - Inlines.HALF16(logXC2))
        }

        /* Estimate spectral tilt */
        c = 0
        do {
            i = 0
            while (i < end - 1) {
                diff += bandLogE[c][i] * (2 + 2 * i - end)
                i++
            }
        } while (++c < C)
        diff /= C * (end - 1)
        /*printf("%f\n", diff);*/
        trim -= Inlines.MAX16(
            Inlines.NEG16((0.5 + 2.0f * (1 shl 8)).toShort()/*Inlines.QCONST16(2.0f, 8)*/).toInt(),
            Inlines.MIN16(
                (0.5 + 2.0f * (1 shl 8)).toShort().toInt()/*Inlines.QCONST16(2.0f, 8)*/,
                Inlines.SHR16(
                    diff + (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort(),
                    CeltConstants.DB_SHIFT - 8
                ) / 6
            )
        )
        trim -= Inlines.SHR16(surround_trim, CeltConstants.DB_SHIFT - 8)
        trim = trim - 2 * Inlines.SHR16(tf_estimate, 14 - 8)
        if (analysis.enabled && analysis.valid != 0) {
            trim -= Inlines.MAX16(
                -(0.5 + 2.0f * (1 shl 8)).toShort()/*Inlines.QCONST16(2.0f, 8)*/, Inlines.MIN16(
                    (0.5 + 2.0f * (1 shl 8)).toShort().toInt()/*Inlines.QCONST16(2.0f, 8)*/,
                    ((0.5 + 2.0f * (1 shl 8)).toShort()/*Inlines.QCONST16(2.0f, 8)*/ * (analysis.tonality_slope + .05f)).toInt()
                )
            )
        }
        trim_index = Inlines.PSHR32(trim, 8)
        trim_index = Inlines.IMAX(0, Inlines.IMIN(10, trim_index))
        /*printf("%d\n", trim_index);*/

        return trim_index
    }

    fun stereo_analysis(
        m: CeltMode, X: Array<IntArray>,
        LM: Int
    ): Int {
        var i: Int
        var thetas: Int
        var sumLR = CeltConstants.EPSILON
        var sumMS = CeltConstants.EPSILON

        /* Use the L1 norm to model the entropy of the L/R signal vs the M/S signal */
        i = 0
        while (i < 13) {
            var j: Int
            j = m.eBands!![i] shl LM
            while (j < m.eBands!![i + 1] shl LM) {
                val L: Int
                val R: Int
                val M: Int
                val S: Int
                /* We cast to 32-bit first because of the -32768 case */
                L = Inlines.EXTEND32(X[0][j])
                R = Inlines.EXTEND32(X[1][j])
                M = Inlines.ADD32(L, R)
                S = Inlines.SUB32(L, R)
                sumLR = Inlines.ADD32(sumLR, Inlines.ADD32(Inlines.ABS32(L), Inlines.ABS32(R)))
                sumMS = Inlines.ADD32(sumMS, Inlines.ADD32(Inlines.ABS32(M), Inlines.ABS32(S)))
                j++
            }
            i++
        }
        sumMS = Inlines.MULT16_32_Q15(
            (0.5 + 0.707107f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.707107f, 15)*/,
            sumMS
        )
        thetas = 13
        /* We don't need thetas for lower bands with LM<=1 */
        if (LM <= 1) {
            thetas -= 8
        }
        return if (Inlines.MULT16_32_Q15(
                (m.eBands!![13] shl LM + 1) + thetas,
                sumMS
            ) > Inlines.MULT16_32_Q15(m.eBands!![13] shl LM + 1, sumLR)
        )
            1
        else
            0
    }

    fun median_of_5(x: IntArray, x_ptr: Int): Int {
        var t0: Int
        var t1: Int
        val t2: Int
        var t3: Int
        var t4: Int
        t2 = x[x_ptr + 2]
        if (x[x_ptr] > x[x_ptr + 1]) {
            t0 = x[x_ptr + 1]
            t1 = x[x_ptr]
        } else {
            t0 = x[x_ptr]
            t1 = x[x_ptr + 1]
        }
        if (x[x_ptr + 3] > x[x_ptr + 4]) {
            t3 = x[x_ptr + 4]
            t4 = x[x_ptr + 3]
        } else {
            t3 = x[x_ptr + 3]
            t4 = x[x_ptr + 4]
        }
        if (t0 > t3) {
            // swap the pairs
            var tmp = t3
            t3 = t0
            t0 = tmp
            tmp = t4
            t4 = t1
            t1 = tmp
        }
        return if (t2 > t1) {
            if (t1 < t3) {
                Inlines.MIN16(t2, t3)
            } else {
                Inlines.MIN16(t4, t1)
            }
        } else if (t2 < t3) {
            Inlines.MIN16(t1, t3)
        } else {
            Inlines.MIN16(t2, t4)
        }
    }

    fun median_of_3(x: IntArray, x_ptr: Int): Int {
        val t0: Int
        val t1: Int
        val t2: Int
        if (x[x_ptr] > x[x_ptr + 1]) {
            t0 = x[x_ptr + 1]
            t1 = x[x_ptr]
        } else {
            t0 = x[x_ptr]
            t1 = x[x_ptr + 1]
        }
        t2 = x[x_ptr + 2]
        return if (t1 < t2) {
            t1
        } else if (t0 < t2) {
            t2
        } else {
            t0
        }
    }

    fun dynalloc_analysis(
        bandLogE: Array<IntArray>, bandLogE2: Array<IntArray>,
        nbEBands: Int, start: Int, end: Int, C: Int, offsets: IntArray, lsb_depth: Int, logN: ShortArray,
        isTransient: Int, vbr: Int, constrained_vbr: Int, eBands: ShortArray, LM: Int,
        effectiveBytes: Int, tot_boost_: BoxedValueInt, lfe: Int, surround_dynalloc: IntArray
    ): Int {
        var i: Int
        var c: Int
        var tot_boost = 0
        var maxDepth: Int
        val follower = Arrays.InitTwoDimensionalArrayInt(2, nbEBands)
        val noise_floor = IntArray(C * nbEBands) // opt: partitioned array

        Arrays.MemSet(offsets, 0, nbEBands)
        /* Dynamic allocation code */
        maxDepth = 0 - (0.5 + 31.9f * (1 shl CeltConstants.DB_SHIFT)).toShort()
        i = 0
        while (i < end) {
            /* Noise floor must take into account eMeans, the depth, the width of the bands
               and the preemphasis filter (approx. square of bark band ID) */
            noise_floor[i] = (Inlines.MULT16_16(
                (0.5 + 0.0625f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(0.0625f, CeltConstants.DB_SHIFT)*/,
                logN[i]
            )
                    + (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/ + Inlines.SHL16(
                9 - lsb_depth,
                CeltConstants.DB_SHIFT
            )) - Inlines.SHL16(CeltTables.eMeans[i].toShort(), 6) + Inlines.MULT16_16(
                (0.5 + 0.0062f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0062f, CeltConstants.DB_SHIFT)*/,
                (i + 5) * (i + 5)
            )
            i++
        }
        c = 0
        do {
            i = 0
            while (i < end) {
                maxDepth = Inlines.MAX16(maxDepth, bandLogE[c][i] - noise_floor[i])
                i++
            }
        } while (++c < C)
        /* Make sure that dynamic allocation can't make us bust the budget */
        if (effectiveBytes > 50 && LM >= 1 && lfe == 0) {
            var last = 0
            c = 0
            do {
                val offset: Int
                var tmp: Int
                val f = follower[c]
                f[0] = bandLogE2[c][0]
                i = 1
                while (i < end) {
                    /* The last band to be at least 3 dB higher than the previous one
                       is the last we'll consider. Otherwise, we run into problems on
                       bandlimited signals. */
                    if (bandLogE2[c][i] > bandLogE2[c][i - 1] + (0.5 + 0.5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(0.5f, CeltConstants.DB_SHIFT)*/) {
                        last = i
                    }
                    f[i] = Inlines.MIN16(
                        f[i - 1] + (0.5 + 1.5f * (1 shl CeltConstants.DB_SHIFT)).toShort(),
                        bandLogE2[c][i]
                    )
                    i++
                }
                i = last - 1
                while (i >= 0) {
                    f[i] = Inlines.MIN16(
                        f[i],
                        Inlines.MIN16(
                            f[i + 1] + (0.5 + 2.0f * (1 shl CeltConstants.DB_SHIFT)).toShort(),
                            bandLogE2[c][i]
                        )
                    )
                    i--
                }

                /* Combine with a median filter to avoid dynalloc triggering unnecessarily.
                   The "offset" value controls how conservative we are -- a higher offset
                   reduces the impact of the median filter and makes dynalloc use more bits. */
                offset = (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                    .toInt()/*Inlines.QCONST16(1.0f, CeltConstants.DB_SHIFT)*/
                i = 2
                while (i < end - 2) {
                    f[i] = Inlines.MAX16(f[i], median_of_5(bandLogE2[c], i - 2) - offset)
                    i++
                }
                tmp = median_of_3(bandLogE2[c], 0) - offset
                f[0] = Inlines.MAX16(f[0], tmp)
                f[1] = Inlines.MAX16(f[1], tmp)
                tmp = median_of_3(bandLogE2[c], end - 3) - offset
                f[end - 2] = Inlines.MAX16(f[end - 2], tmp)
                f[end - 1] = Inlines.MAX16(f[end - 1], tmp)

                i = 0
                while (i < end) {
                    f[i] = Inlines.MAX16(f[i], noise_floor[i])
                    i++
                }
            } while (++c < C)
            if (C == 2) {
                i = start
                while (i < end) {
                    /* Consider 24 dB "cross-talk" */
                    follower[1][i] = Inlines.MAX16(
                        follower[1][i],
                        follower[0][i] - (0.5 + 4.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(4.0f, CeltConstants.DB_SHIFT)*/
                    )
                    follower[0][i] = Inlines.MAX16(
                        follower[0][i],
                        follower[1][i] - (0.5 + 4.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(4.0f, CeltConstants.DB_SHIFT)*/
                    )
                    follower[0][i] = Inlines.HALF16(
                        Inlines.MAX16(0, bandLogE[0][i] - follower[0][i]) + Inlines.MAX16(
                            0,
                            bandLogE[1][i] - follower[1][i]
                        )
                    )
                    i++
                }
            } else {
                i = start
                while (i < end) {
                    follower[0][i] = Inlines.MAX16(0, bandLogE[0][i] - follower[0][i])
                    i++
                }
            }
            i = start
            while (i < end) {
                follower[0][i] = Inlines.MAX16(follower[0][i], surround_dynalloc[i])
                i++
            }
            /* For non-transient CBR/CVBR frames, halve the dynalloc contribution */
            if ((vbr == 0 || constrained_vbr != 0) && isTransient == 0) {
                i = start
                while (i < end) {
                    follower[0][i] = Inlines.HALF16(follower[0][i])
                    i++
                }
            }
            i = start
            while (i < end) {
                val width: Int
                val boost: Int
                val boost_bits: Int

                if (i < 8) {
                    follower[0][i] *= 2
                }
                if (i >= 12) {
                    follower[0][i] = Inlines.HALF16(follower[0][i])
                }
                follower[0][i] = Inlines.MIN16(
                    follower[0][i],
                    (0.5 + 4 * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(4, CeltConstants.DB_SHIFT)*/
                )

                width = C * (eBands[i + 1] - eBands[i]) shl LM
                if (width < 6) {
                    boost = Inlines.SHR32(follower[0][i], CeltConstants.DB_SHIFT).toInt()
                    boost_bits = boost * width shl EntropyCoder.BITRES
                } else if (width > 48) {
                    boost = Inlines.SHR32(follower[0][i] * 8, CeltConstants.DB_SHIFT).toInt()
                    boost_bits = (boost * width shl EntropyCoder.BITRES) / 8
                } else {
                    boost = Inlines.SHR32(follower[0][i] * width / 6, CeltConstants.DB_SHIFT).toInt()
                    boost_bits = boost * 6 shl EntropyCoder.BITRES
                }
                /* For CBR and non-transient CVBR frames, limit dynalloc to 1/4 of the bits */
                if ((vbr == 0 || constrained_vbr != 0 && isTransient == 0) && tot_boost + boost_bits shr EntropyCoder.BITRES shr 3 > effectiveBytes / 4) {
                    val cap = effectiveBytes / 4 shl EntropyCoder.BITRES shl 3
                    offsets[i] = cap - tot_boost
                    tot_boost = cap
                    break
                } else {
                    offsets[i] = boost
                    tot_boost += boost_bits
                }
                i++
            }
        }

        tot_boost_.Val = tot_boost

        return maxDepth
    }

    fun deemphasis(
		input: Array<IntArray?>,
		input_ptrs: IntArray,
		pcm: ShortArray,
		pcm_ptr: Int,
		N: Int,
		C: Int,
		downsample: Int,
		coef: IntArray,
		mem: IntArray,
		accum: Int
    ) {
        var c: Int
        val Nd: Int
        var apply_downsampling = 0
        val coef0: Int
        val scratch = IntArray(N)
        coef0 = coef[0]
        Nd = N / downsample
        c = 0
        do {
            var j: Int
            val x_ptr: Int
            val y: Int
            var m = mem[c]
            val x = input[c]
            x_ptr = input_ptrs[c]
            y = pcm_ptr + c
            if (downsample > 1) {
                /* Shortcut for the standard (non-custom modes) case */
                j = 0
                while (j < N) {
                    val tmp = x!![x_ptr + j] + m + CeltConstants.VERY_SMALL
                    m = Inlines.MULT16_32_Q15(coef0, tmp)
                    scratch[j] = tmp
                    j++
                }
                apply_downsampling = 1
            } else /* Shortcut for the standard (non-custom modes) case */ if (accum != 0)
            // should never hit this branch?
            {
                j = 0
                while (j < N) {
                    val tmp = x!![x_ptr + j] + m + CeltConstants.VERY_SMALL
                    m = Inlines.MULT16_32_Q15(coef0, tmp)
                    pcm[y + j * C] =
                            Inlines.SAT16(Inlines.ADD32(pcm[y + j * C].toInt(), Inlines.SIG2WORD16(tmp).toInt()))
                    j++
                }
            } else {
                j = 0
                while (j < N) {
                    var tmp = x!![x_ptr + j] + m + CeltConstants.VERY_SMALL // Opus bug: This can overflow.
                    if (x!![x_ptr + j] > 0 && m > 0 && tmp < 0)
                    // This is a hack to saturate to INT_MAXVALUE
                    {
                        tmp = Int.MAX_VALUE
                        m = Int.MAX_VALUE
                    } else {
                        m = Inlines.MULT16_32_Q15(coef0, tmp)
                    }
                    pcm[y + j * C] = Inlines.SIG2WORD16(tmp)
                    j++
                }
            }
            mem[c] = m

            if (apply_downsampling != 0) {
                /* Perform down-sampling */
                run {
                    j = 0
                    while (j < Nd) {
                        pcm[y + j * C] = Inlines.SIG2WORD16(scratch[j * downsample])
                        j++
                    }
                }
            }
        } while (++c < C)

    }

    fun celt_synthesis(
		mode: CeltMode, X: Array<IntArray>, out_syn: Array<IntArray?>, out_syn_ptrs: IntArray,
		oldBandE: IntArray, start: Int, effEnd: Int, C: Int, CC: Int,
		isTransient: Int, LM: Int, downsample: Int,
		silence: Int
    ) {
        var c: Int
        var i: Int
        val M: Int
        var b: Int
        val B: Int
        val N: Int
        val NB: Int
        val shift: Int
        val nbEBands: Int
        val overlap: Int
        val freq: IntArray

        overlap = mode.overlap
        nbEBands = mode.nbEBands
        N = mode.shortMdctSize shl LM
        freq = IntArray(N)
        /**
         * < Interleaved signal MDCTs
         */
        M = 1 shl LM

        if (isTransient != 0) {
            B = M
            NB = mode.shortMdctSize
            shift = mode.maxLM
        } else {
            B = 1
            NB = mode.shortMdctSize shl LM
            shift = mode.maxLM - LM
        }

        if (CC == 2 && C == 1) {
            /* Copying a mono streams to two channels */
            val freq2: Int
            Bands.denormalise_bands(
                mode, X[0], freq, 0, oldBandE, 0, start, effEnd, M,
                downsample, silence
            )
            /* Store a temporary copy in the output buffer because the IMDCT destroys its input. */
            freq2 = out_syn_ptrs[1] + overlap / 2
            arraycopy(freq, 0, out_syn[1]!!, freq2, N)
            b = 0
            while (b < B) {
                MDCT.clt_mdct_backward(
                    mode.mdct,
                    out_syn[1]!!,
                    freq2 + b,
                    out_syn[0]!!,
                    out_syn_ptrs[0] + NB * b,
					mode.window!!,
                    overlap,
                    shift,
                    B
                )
                b++
            }
            b = 0
            while (b < B) {
                MDCT.clt_mdct_backward(
                    mode.mdct,
                    freq,
                    b,
                    out_syn[1]!!,
                    out_syn_ptrs[1] + NB * b,
					mode.window!!,
                    overlap,
                    shift,
                    B
                )
                b++
            }
        } else if (CC == 1 && C == 2) {
            /* Downmixing a stereo stream to mono */
            val freq2 = out_syn_ptrs[0] + overlap / 2
            Bands.denormalise_bands(
                mode, X[0], freq, 0, oldBandE, 0, start, effEnd, M,
                downsample, silence
            )
            /* Use the output buffer as temp array before downmixing. */
            Bands.denormalise_bands(
                mode, X[1], out_syn[0]!!, freq2, oldBandE, nbEBands, start, effEnd, M,
                downsample, silence
            )
            i = 0
            while (i < N) {
                freq[i] = Inlines.HALF32(Inlines.ADD32(freq[i], out_syn[0]!![freq2 + i]!!))
                i++
            }
            b = 0
            while (b < B) {
                MDCT.clt_mdct_backward(
                    mode.mdct,
                    freq,
                    b,
                    out_syn[0]!!,
                    out_syn_ptrs[0] + NB * b,
					mode.window!!,
                    overlap,
                    shift,
                    B
                )
                b++
            }
        } else {
            /* Normal case (mono or stereo) */
            c = 0
            do {
                Bands.denormalise_bands(
                    mode, X[c], freq, 0, oldBandE, c * nbEBands, start, effEnd, M,
                    downsample, silence
                )
                b = 0
                while (b < B) {
                    MDCT.clt_mdct_backward(
                        mode.mdct,
                        freq,
                        b,
                        out_syn[c]!!,
                        out_syn_ptrs[c] + NB * b,
						mode.window!!,
                        overlap,
                        shift,
                        B
                    )
                    b++
                }
            } while (++c < CC)
        }

    }

    fun tf_decode(start: Int, end: Int, isTransient: Int, tf_res: IntArray, LM: Int, dec: EntropyCoder) {
        var i: Int
        var curr: Int
        var tf_select: Int
        val tf_select_rsv: Int
        var tf_changed: Int
        var logp: Int
        var budget: Int
        var tell: Int

        budget = dec.storage * 8
        tell = dec.tell()
        logp = if (isTransient != 0) 2 else 4
        tf_select_rsv = if (LM > 0 && tell + logp + 1 <= budget) 1 else 0
        budget -= tf_select_rsv
        curr = 0
        tf_changed = curr
        i = start
        while (i < end) {
            if (tell + logp <= budget) {
                curr = curr xor dec.dec_bit_logp(logp.toLong())
                tell = dec.tell()
                tf_changed = tf_changed or curr
            }
            tf_res[i] = curr
            logp = if (isTransient != 0) 4 else 5
            i++
        }
        tf_select = 0
        if (tf_select_rsv != 0 && CeltTables.tf_select_table[LM][4 * isTransient + 0 + tf_changed] != CeltTables.tf_select_table[LM][4 * isTransient + 2 + tf_changed]) {
            tf_select = dec.dec_bit_logp(1)
        }
        i = start
        while (i < end) {
            tf_res[i] = CeltTables.tf_select_table[LM][4 * isTransient + 2 * tf_select + tf_res[i]].toInt()
            i++
        }
    }

    fun celt_plc_pitch_search(decode_mem: Array<IntArray>, C: Int): Int {
        val pitch_index = BoxedValueInt(0)
        val lp_pitch_buf = IntArray(CeltConstants.DECODE_BUFFER_SIZE shr 1)
        Pitch.pitch_downsample(
            decode_mem, lp_pitch_buf,
            CeltConstants.DECODE_BUFFER_SIZE, C
        )
        Pitch.pitch_search(
            lp_pitch_buf, CeltConstants.PLC_PITCH_LAG_MAX shr 1, lp_pitch_buf,
            CeltConstants.DECODE_BUFFER_SIZE - CeltConstants.PLC_PITCH_LAG_MAX,
            CeltConstants.PLC_PITCH_LAG_MAX - CeltConstants.PLC_PITCH_LAG_MIN, pitch_index
        )
        pitch_index.Val = CeltConstants.PLC_PITCH_LAG_MAX - pitch_index.Val

        return pitch_index.Val
    }

    fun resampling_factor(rate: Int): Int {
        val ret: Int
        when (rate) {
            48000 -> ret = 1
            24000 -> ret = 2
            16000 -> ret = 3
            12000 -> ret = 4
            8000 -> ret = 6
            else -> {
                Inlines.OpusAssert(false)
                ret = 0
            }
        }
        return ret
    }

    fun comb_filter_const(
        y: IntArray, y_ptr: Int, x: IntArray, x_ptr: Int, T: Int, N: Int,
        g10: Int, g11: Int, g12: Int
    ) {
        var x0: Int
        var x1: Int
        var x2: Int
        var x3: Int
        var x4: Int
        var i: Int
        val xpt = x_ptr - T
        x4 = x[xpt - 2]
        x3 = x[xpt - 1]
        x2 = x[xpt]
        x1 = x[xpt + 1]
        i = 0
        while (i < N) {
            x0 = x[xpt + i + 2]
            y[y_ptr + i] = (x[x_ptr + i]
                    + Inlines.MULT16_32_Q15(g10, x2)
                    + Inlines.MULT16_32_Q15(g11, Inlines.ADD32(x1, x3))
                    + Inlines.MULT16_32_Q15(g12, Inlines.ADD32(x0, x4)))
            x4 = x3
            x3 = x2
            x2 = x1
            x1 = x0
            i++
        }
    }

    fun comb_filter(
		y: IntArray, y_ptr: Int, x: IntArray, x_ptr: Int, T0: Int, T1: Int, N: Int,
		g0: Int, g1: Int, tapset0: Int, tapset1: Int,
		window: IntArray?, overlap: Int
    ) {
        var overlap = overlap
        var i: Int
        /* printf ("%d %d %f %f\n", T0, T1, g0, g1); */
        val g00: Int
        val g01: Int
        val g02: Int
        val g10: Int
        val g11: Int
        val g12: Int
        var x0: Int
        var x1: Int
        var x2: Int
        var x3: Int
        var x4: Int

        if (g0 == 0 && g1 == 0) {
            /* OPT: Happens to work without the OPUS_MOVE(), but only because the current encoder already copies x to y */
            if (x_ptr != y_ptr) {
                //x.MemMoveTo(y, N);
            }

            return
        }
        g00 = Inlines.MULT16_16_P15(g0, gains[tapset0][0].toInt())
        g01 = Inlines.MULT16_16_P15(g0, gains[tapset0][1].toInt())
        g02 = Inlines.MULT16_16_P15(g0, gains[tapset0][2].toInt())
        g10 = Inlines.MULT16_16_P15(g1, gains[tapset1][0].toInt())
        g11 = Inlines.MULT16_16_P15(g1, gains[tapset1][1].toInt())
        g12 = Inlines.MULT16_16_P15(g1, gains[tapset1][2].toInt())
        x1 = x[x_ptr - T1 + 1]
        x2 = x[x_ptr - T1]
        x3 = x[x_ptr - T1 - 1]
        x4 = x[x_ptr - T1 - 2]
        /* If the filter didn't change, we don't need the overlap */
        if (g0 == g1 && T0 == T1 && tapset0 == tapset1) {
            overlap = 0
        }
        i = 0
        while (i < overlap) {
            val f: Int
            x0 = x[x_ptr + i - T1 + 2]
            f = Inlines.MULT16_16_Q15(window!![i]!!, window!![i]!!)
            y[y_ptr + i] = (x[x_ptr + i]
                    + Inlines.MULT16_32_Q15(
                Inlines.MULT16_16_Q15((CeltConstants.Q15ONE - f).toShort().toInt(), g00),
                x[x_ptr + i - T0]
            )
                    + Inlines.MULT16_32_Q15(
                Inlines.MULT16_16_Q15((CeltConstants.Q15ONE - f).toShort().toInt(), g01),
                Inlines.ADD32(x[x_ptr + i - T0 + 1], x[x_ptr + i - T0 - 1])
            )
                    + Inlines.MULT16_32_Q15(
                Inlines.MULT16_16_Q15((CeltConstants.Q15ONE - f).toShort().toInt(), g02),
                Inlines.ADD32(x[x_ptr + i - T0 + 2], x[x_ptr + i - T0 - 2])
            )
                    + Inlines.MULT16_32_Q15(Inlines.MULT16_16_Q15(f, g10), x2)
                    + Inlines.MULT16_32_Q15(Inlines.MULT16_16_Q15(f, g11), Inlines.ADD32(x1, x3))
                    + Inlines.MULT16_32_Q15(Inlines.MULT16_16_Q15(f, g12), Inlines.ADD32(x0, x4)))
            x4 = x3
            x3 = x2
            x2 = x1
            x1 = x0
            i++

        }
        if (g1 == 0) {
            /* OPT: Happens to work without the OPUS_MOVE(), but only because the current encoder already copies x to y */
            if (x_ptr != y_ptr) {
                //x.Point(overlap).MemMoveTo(y.Point(overlap), N - overlap);
            }
            return
        }

        /* Compute the part with the constant filter. */
        comb_filter_const(y, y_ptr + i, x, x_ptr + i, T1, N - i, g10, g11, g12)
    }

    fun init_caps(m: CeltMode, cap: IntArray, LM: Int, C: Int) {
        var i: Int
        i = 0
        while (i < m.nbEBands) {
            val N: Int
            N = m.eBands!![i + 1] - m.eBands!![i] shl LM
            cap[i] = (m.cache.caps!![m.nbEBands * (2 * LM + C - 1) + i] + 64) * C * N shr 2
            i++
        }
    }
}
