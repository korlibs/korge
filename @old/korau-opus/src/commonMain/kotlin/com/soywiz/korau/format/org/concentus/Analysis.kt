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

import kotlin.math.*

internal object Analysis {

    private val M_PI = 3.141592653
    private val cA = 0.43157974f
    private val cB = 0.67848403f
    private val cC = 0.08595542f
    private val cE = M_PI.toFloat() / 2

    private val NB_TONAL_SKIP_BANDS = 9

    fun fast_atan2f(y: Float, x: Float): Float {
        var y = y
        var x = x
        val x2: Float
        val y2: Float
        /* Should avoid underflow on the values we'll get */
        if (Inlines.ABS16(x) + Inlines.ABS16(y) < 1e-9f) {
            x *= 1e12f
            y *= 1e12f
        }
        x2 = x * x
        y2 = y * y
        if (x2 < y2) {
            val den = (y2 + cB * x2) * (y2 + cC * x2)
            return if (den != 0f) {
                -x * y * (y2 + cA * x2) / den + if (y < 0) -cE else cE
            } else {
                if (y < 0) -cE else cE
            }
        } else {
            val den = (x2 + cB * y2) * (x2 + cC * y2)
            return if (den != 0f) {
                x * y * (x2 + cA * y2) / den + (if (y < 0) -cE else cE) - if (x * y < 0) -cE else cE
            } else {
                (if (y < 0) -cE else cE) - if (x * y < 0) -cE else cE
            }
        }
    }

    fun tonality_analysis_init(tonal: TonalityAnalysisState) {
        tonal.Reset()
    }

    fun tonality_get_info(tonal: TonalityAnalysisState, info_out: AnalysisInfo, len: Int) {
        var pos: Int
        var curr_lookahead: Int
        var psum: Float
        var i: Int

        pos = tonal.read_pos
        curr_lookahead = tonal.write_pos - tonal.read_pos
        if (curr_lookahead < 0) {
            curr_lookahead += OpusConstants.DETECT_SIZE
        }

        if (len > 480 && pos != tonal.write_pos) {
            pos++
            if (pos == OpusConstants.DETECT_SIZE) {
                pos = 0
            }
        }
        if (pos == tonal.write_pos) {
            pos--
        }
        if (pos < 0) {
            pos = OpusConstants.DETECT_SIZE - 1
        }

        info_out.Assign(tonal.info[pos]!!)
        tonal.read_subframe += len / 120
        while (tonal.read_subframe >= 4) {
            tonal.read_subframe -= 4
            tonal.read_pos++
        }
        if (tonal.read_pos >= OpusConstants.DETECT_SIZE) {
            tonal.read_pos -= OpusConstants.DETECT_SIZE
        }

        /* Compensate for the delay in the features themselves.
           FIXME: Need a better estimate the 10 I just made up */
        curr_lookahead = Inlines.IMAX(curr_lookahead - 10, 0)

        psum = 0f
        /* Summing the probability of transition patterns that involve music at
           time (DETECT_SIZE-curr_lookahead-1) */
        i = 0
        while (i < OpusConstants.DETECT_SIZE - curr_lookahead) {
            psum += tonal.pmusic[i]
            i++
        }
        while (i < OpusConstants.DETECT_SIZE) {
            psum += tonal.pspeech[i]
            i++
        }
        psum = psum * tonal.music_confidence + (1 - psum) * tonal.speech_confidence
        /*printf("%f %f %f\n", psum, info_out.music_prob, info_out.tonality);*/

        info_out.music_prob = psum
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="tonal"></param>
    /// <param name="celt_mode"></param>
    /// <param name="x"></param>
    /// <param name="len"></param>
    /// <param name="offset"></param>
    /// <param name="c1"></param>
    /// <param name="c2"></param>
    /// <param name="C"></param>
    /// <param name="lsb_depth"></param>
    fun tonality_analysis(
        tonal: TonalityAnalysisState,
        celt_mode: CeltMode,
        x: ShortArray,
        x_ptr: Int,
        len: Int,
        offset: Int,
        c1: Int,
        c2: Int,
        C: Int,
        lsb_depth: Int
    ) {
        var i: Int
        var b: Int
        val kfft: FFTState
        val input: IntArray
        val output: IntArray
        val N = 480
        val N2 = 240
        val A = tonal.angle
        val dA = tonal.d_angle
        val d2A = tonal.d2_angle
        val tonality: FloatArray
        val noisiness: FloatArray
        val band_tonality = FloatArray(OpusConstants.NB_TBANDS)
        val logE = FloatArray(OpusConstants.NB_TBANDS)
        val BFCC = FloatArray(8)
        val features = FloatArray(25)
        var frame_tonality: Float
        var max_frame_tonality: Float
        /*float tw_sum=0;*/
        var frame_noisiness: Float
        val pi4 = (M_PI * M_PI * M_PI * M_PI).toFloat()
        var slope = 0f
        var frame_stationarity: Float
        var relativeE: Float
        val frame_probs = FloatArray(2)
        val alpha: Float
        val alphaE: Float
        val alphaE2: Float
        var frame_loudness: Float
        var bandwidth_mask: Float
        var bandwidth = 0
        var maxE = 0f
        var noise_floor: Float
        val remaining: Int
        val info: AnalysisInfo //[porting note] pointer

        tonal.last_transition++
        alpha = 1.0f / Inlines.IMIN(20, 1 + tonal.count)
        alphaE = 1.0f / Inlines.IMIN(50, 1 + tonal.count)
        alphaE2 = 1.0f / Inlines.IMIN(1000, 1 + tonal.count)

        if (tonal.count < 4) {
            tonal.music_prob = 0.5f
        }
        kfft = celt_mode.mdct.kfft[0]!!
        if (tonal.count == 0) {
            tonal.mem_fill = 240
        }

        Downmix.downmix_int(
            x,
            x_ptr,
            tonal.inmem,
            tonal.mem_fill,
            Inlines.IMIN(len, OpusConstants.ANALYSIS_BUF_SIZE - tonal.mem_fill),
            offset,
            c1,
            c2,
            C
        )

        if (tonal.mem_fill + len < OpusConstants.ANALYSIS_BUF_SIZE) {
            tonal.mem_fill += len
            /* Don't have enough to update the analysis */
            return
        }

        info = tonal.info[tonal.write_pos++]!!
        if (tonal.write_pos >= OpusConstants.DETECT_SIZE) {
            tonal.write_pos -= OpusConstants.DETECT_SIZE
        }

        input = IntArray(960)
        output = IntArray(960)
        tonality = FloatArray(240)
        noisiness = FloatArray(240)
        i = 0
        while (i < N2) {
            val w = OpusTables.analysis_window[i]
            input[2 * i] = (w * tonal.inmem[i]).toInt()
            input[2 * i + 1] = (w * tonal.inmem[N2 + i]).toInt()
            input[2 * (N - i - 1)] = (w * tonal.inmem[N - i - 1]).toInt()
            input[2 * (N - i - 1) + 1] = (w * tonal.inmem[N + N2 - i - 1]).toInt()
            i++
        }
        Arrays.MemMove(tonal.inmem, OpusConstants.ANALYSIS_BUF_SIZE - 240, 0, 240)

        remaining = len - (OpusConstants.ANALYSIS_BUF_SIZE - tonal.mem_fill)
        Downmix.downmix_int(
            x,
            x_ptr,
            tonal.inmem,
            240,
            remaining,
            offset + OpusConstants.ANALYSIS_BUF_SIZE - tonal.mem_fill,
            c1,
            c2,
            C
        )
        tonal.mem_fill = 240 + remaining

        KissFFT.opus_fft(kfft, input, output)

        i = 1
        while (i < N2) {
            val X1r: Float
            val X2r: Float
            val X1i: Float
            val X2i: Float
            val angle: Float
            val d_angle: Float
            val d2_angle: Float
            val angle2: Float
            val d_angle2: Float
            val d2_angle2: Float
            var mod1: Float
            var mod2: Float
            val avg_mod: Float
            X1r = output[2 * i].toFloat() + output[2 * (N - i)]
            X1i = output[2 * i + 1].toFloat() - output[2 * (N - i) + 1]
            X2r = output[2 * i + 1].toFloat() + output[2 * (N - i) + 1]
            X2i = output[2 * (N - i)].toFloat() - output[2 * i]

            angle = (.5f / M_PI).toFloat() * fast_atan2f(X1i, X1r)
            d_angle = angle - A[i]
            d2_angle = d_angle - dA[i]

            angle2 = (.5f / M_PI).toFloat() * fast_atan2f(X2i, X2r)
            d_angle2 = angle2 - angle
            d2_angle2 = d_angle2 - d_angle

            mod1 = d2_angle - floor((0.5f + d2_angle).toDouble()).toFloat()
            noisiness[i] = Inlines.ABS16(mod1)
            mod1 *= mod1
            mod1 *= mod1

            mod2 = d2_angle2 - floor((0.5f + d2_angle2).toDouble()).toFloat()
            noisiness[i] += Inlines.ABS16(mod2)
            mod2 *= mod2
            mod2 *= mod2

            avg_mod = .25f * (d2A[i] + 2.0f * mod1 + mod2)
            tonality[i] = 1.0f / (1.0f + 40.0f * 16.0f * pi4 * avg_mod) - .015f

            A[i] = angle2
            dA[i] = d_angle2
            d2A[i] = mod2
            i++
        }

        frame_tonality = 0f
        max_frame_tonality = 0f
        /*tw_sum = 0;*/
        info.activity = 0f
        frame_noisiness = 0f
        frame_stationarity = 0f
        if (tonal.count == 0) {
            b = 0
            while (b < OpusConstants.NB_TBANDS) {
                tonal.lowE[b] = 1e10f
                tonal.highE[b] = -1e10f
                b++
            }
        }
        relativeE = 0f
        frame_loudness = 0f
        b = 0
        while (b < OpusConstants.NB_TBANDS) {
            var E = 0f
            var tE = 0f
            var nE = 0f
            var L1: Float
            var L2: Float
            var stationarity: Float
            i = OpusTables.tbands[b]
            while (i < OpusTables.tbands[b + 1]) {
                var binE =
                    (output[2 * i] * output[2 * i].toFloat() + output[2 * (N - i)] * output[2 * (N - i)].toFloat()
                            + output[2 * i + 1] * output[2 * i + 1].toFloat() + output[2 * (N - i) + 1] * output[2 * (N - i) + 1].toFloat())
                /* FIXME: It's probably best to change the BFCC filter initial state instead */
                binE *= 5.55e-17f
                E += binE
                tE += binE * tonality[i]
                nE += binE * 2.0f * (.5f - noisiness[i])
                i++
            }

            tonal.E[tonal.E_count][b] = E
            frame_noisiness += nE / (1e-15f + E)

            frame_loudness += sqrt((E + 1e-10f).toDouble()).toFloat()
            logE[b] = log((E + 1e-10f).toDouble(), E.toDouble()).toFloat()
            tonal.lowE[b] = Inlines.MIN32(logE[b], tonal.lowE[b] + 0.01f)
            tonal.highE[b] = Inlines.MAX32(logE[b], tonal.highE[b] - 0.1f)
            if (tonal.highE[b] < tonal.lowE[b] + 1.0f) {
                tonal.highE[b] += 0.5f
                tonal.lowE[b] -= 0.5f
            }
            relativeE += (logE[b] - tonal.lowE[b]) / (1e-15f + tonal.highE[b] - tonal.lowE[b])

            L2 = 0f
            L1 = L2
            i = 0
            while (i < OpusConstants.NB_FRAMES) {
                L1 += sqrt(tonal.E[i][b].toDouble()).toFloat()
                L2 += tonal.E[i][b]
                i++
            }

            stationarity = Inlines.MIN16(0.99f, L1 / sqrt(1e-15 + OpusConstants.NB_FRAMES * L2).toFloat())
            stationarity *= stationarity
            stationarity *= stationarity
            frame_stationarity += stationarity
            /*band_tonality[b] = tE/(1e-15+E)*/
            band_tonality[b] = Inlines.MAX16(tE / (1e-15f + E), stationarity * tonal.prev_band_tonality[b])
            frame_tonality += band_tonality[b]
            if (b >= OpusConstants.NB_TBANDS - OpusConstants.NB_TONAL_SKIP_BANDS) {
                frame_tonality -= band_tonality[b - OpusConstants.NB_TBANDS + OpusConstants.NB_TONAL_SKIP_BANDS]
            }
            max_frame_tonality =
                    Inlines.MAX16(max_frame_tonality, (1.0f + .03f * (b - OpusConstants.NB_TBANDS)) * frame_tonality)
            slope += band_tonality[b] * (b - 8)
            tonal.prev_band_tonality[b] = band_tonality[b]
            b++
        }

        bandwidth_mask = 0f
        bandwidth = 0
        maxE = 0f
        noise_floor = 5.7e-4f / (1 shl Inlines.IMAX(0, lsb_depth - 8))
        noise_floor *= (1 shl 15 + CeltConstants.SIG_SHIFT).toFloat()
        noise_floor *= noise_floor
        b = 0
        while (b < OpusConstants.NB_TOT_BANDS) {
            var E = 0f
            val band_start: Int
            val band_end: Int
            /* Keep a margin of 300 Hz for aliasing */
            band_start = OpusTables.extra_bands[b]
            band_end = OpusTables.extra_bands[b + 1]
            i = band_start
            while (i < band_end) {
                val binE =
                    (output[2 * i] * output[2 * i].toFloat() + output[2 * (N - i)] * output[2 * (N - i)].toFloat()
                            + output[2 * i + 1] * output[2 * i + 1].toFloat() + output[2 * (N - i) + 1] * output[2 * (N - i) + 1].toFloat())
                E += binE
                i++
            }
            maxE = Inlines.MAX32(maxE, E)
            tonal.meanE[b] = Inlines.MAX32((1 - alphaE2) * tonal.meanE[b], E)
            E = Inlines.MAX32(E, tonal.meanE[b])
            /* Use a simple follower with 13 dB/Bark slope for spreading function */
            bandwidth_mask = Inlines.MAX32(.05f * bandwidth_mask, E)
            /* Consider the band "active" only if all these conditions are met:
               1) less than 10 dB below the simple follower
               2) less than 90 dB below the peak band (maximal masking possible considering
                  both the ATH and the loudness-dependent slope of the spreading function)
               3) above the PCM quantization noise floor
             */
            if (E > .1 * bandwidth_mask && E * 1e9f > maxE && E > noise_floor * (band_end - band_start)) {
                bandwidth = b
            }
            b++
        }
        if (tonal.count <= 2) {
            bandwidth = 20
        }
        frame_loudness = 20 * log10(frame_loudness.toDouble()).toFloat()
        tonal.Etracker = Inlines.MAX32(tonal.Etracker - .03f, frame_loudness)
        tonal.lowECount *= 1 - alphaE
        if (frame_loudness < tonal.Etracker - 30) {
            tonal.lowECount += alphaE
        }

        i = 0
        while (i < 8) {
            var sum = 0f
            b = 0
            while (b < 16) {
                sum += OpusTables.dct_table[i * 16 + b] * logE[b]
                b++
            }
            BFCC[i] = sum
            i++
        }

        frame_stationarity /= OpusConstants.NB_TBANDS.toFloat()
        relativeE /= OpusConstants.NB_TBANDS.toFloat()
        if (tonal.count < 10) {
            relativeE = 0.5f
        }
        frame_noisiness /= OpusConstants.NB_TBANDS.toFloat()
        info.activity = frame_noisiness + (1 - frame_noisiness) * relativeE
        frame_tonality = max_frame_tonality / (OpusConstants.NB_TBANDS - OpusConstants.NB_TONAL_SKIP_BANDS)
        frame_tonality = Inlines.MAX16(frame_tonality, tonal.prev_tonality * .8f)
        tonal.prev_tonality = frame_tonality

        slope /= (8 * 8).toFloat()
        info.tonality_slope = slope

        tonal.E_count = (tonal.E_count + 1) % OpusConstants.NB_FRAMES
        tonal.count++
        info.tonality = frame_tonality

        i = 0
        while (i < 4) {
            features[i] = -0.12299f * (BFCC[i] + tonal.mem[i + 24]) + 0.49195f *
                    (tonal.mem[i] + tonal.mem[i + 16]) + 0.69693f * tonal.mem[i + 8] - 1.4349f * tonal.cmean[i]
            i++
        }

        i = 0
        while (i < 4) {
            tonal.cmean[i] = (1 - alpha) * tonal.cmean[i] + alpha * BFCC[i]
            i++
        }

        i = 0
        while (i < 4) {
            features[4 + i] = 0.63246f * (BFCC[i] - tonal.mem[i + 24]) + 0.31623f * (tonal.mem[i] - tonal.mem[i + 16])
            i++
        }
        i = 0
        while (i < 3) {
            features[8 + i] = 0.53452f * (BFCC[i] + tonal.mem[i + 24]) - 0.26726f *
                    (tonal.mem[i] + tonal.mem[i + 16]) - 0.53452f * tonal.mem[i + 8]
            i++
        }

        if (tonal.count > 5) {
            i = 0
            while (i < 9) {
                tonal.std[i] = (1 - alpha) * tonal.std[i] + alpha * features[i] * features[i]
                i++
            }
        }

        i = 0
        while (i < 8) {
            tonal.mem[i + 24] = tonal.mem[i + 16]
            tonal.mem[i + 16] = tonal.mem[i + 8]
            tonal.mem[i + 8] = tonal.mem[i]
            tonal.mem[i] = BFCC[i]
            i++
        }
        i = 0
        while (i < 9) {
            features[11 + i] = sqrt(tonal.std[i].toDouble()).toFloat()
            i++
        }
        features[20] = info.tonality
        features[21] = info.activity
        features[22] = frame_stationarity
        features[23] = info.tonality_slope
        features[24] = tonal.lowECount

        if (info.enabled) {
            MultiLayerPerceptron.mlp_process(OpusTables.net, features, frame_probs)
            frame_probs[0] = .5f * (frame_probs[0] + 1)
            /* Curve fitting between the MLP probability and the actual probability */
            frame_probs[0] = .01f + 1.21f * frame_probs[0] * frame_probs[0] - .23f *
                    frame_probs[0].toDouble().pow(10.0).toFloat()
            /* Probability of active audio (as opposed to silence) */
            frame_probs[1] = .5f * frame_probs[1] + .5f
            /* Consider that silence has a 50-50 probability. */
            frame_probs[0] = frame_probs[1] * frame_probs[0] + (1 - frame_probs[1]) * .5f

            /*printf("%f %f ", frame_probs[0], frame_probs[1]);*/
            run {
                /* Probability of state transition */
                val tau: Float
                /* Represents independence of the MLP probabilities, where
                   beta=1 means fully independent. */
                var beta: Float
                /* Denormalized probability of speech (p0) and music (p1) after update */
                var p0: Float
                var p1: Float
                /* Probabilities for "all speech" and "all music" */
                val s0: Float
                val m0: Float
                /* Probability sum for renormalisation */
                var psum: Float
                /* Instantaneous probability of speech and music, with beta pre-applied. */
                val speech0: Float
                val music0: Float

                /* One transition every 3 minutes of active audio */
                tau = .00005f * frame_probs[1]
                beta = .05f
                //if (1)
                run {
                    /* Adapt beta based on how "unexpected" the new prob is */
                    val p: Float
                    val q: Float
                    p = Inlines.MAX16(.05f, Inlines.MIN16(.95f, frame_probs[0]))
                    q = Inlines.MAX16(.05f, Inlines.MIN16(.95f, tonal.music_prob))
                    beta = .01f + .05f * Inlines.ABS16(p - q) / (p * (1 - q) + q * (1 - p))
                }
                /* p0 and p1 are the probabilities of speech and music at this frame
                   using only information from previous frame and applying the
                   state transition model */
                p0 = (1 - tonal.music_prob) * (1 - tau) + tonal.music_prob * tau
                p1 = tonal.music_prob * (1 - tau) + (1 - tonal.music_prob) * tau
                /* We apply the current probability with exponent beta to work around
                   the fact that the probability estimates aren't independent. */
                p0 *= (1 - frame_probs[0]).toDouble().pow(beta.toDouble()).toFloat()
                p1 *= frame_probs[0].toDouble().pow(beta.toDouble()).toFloat()
                /* Normalise the probabilities to get the Marokv probability of music. */
                tonal.music_prob = p1 / (p0 + p1)
                info.music_prob = tonal.music_prob

                /* This chunk of code deals with delayed decision. */
                psum = 1e-20f
                /* Instantaneous probability of speech and music, with beta pre-applied. */
                speech0 = (1 - frame_probs[0]).toDouble().pow(beta.toDouble()).toFloat()
                music0 = frame_probs[0].toDouble().pow(beta.toDouble()).toFloat()
                if (tonal.count == 1) {
                    tonal.pspeech[0] = 0.5f
                    tonal.pmusic[0] = 0.5f
                }
                /* Updated probability of having only speech (s0) or only music (m0),
                   before considering the new observation. */
                s0 = tonal.pspeech[0] + tonal.pspeech[1]
                m0 = tonal.pmusic[0] + tonal.pmusic[1]
                /* Updates s0 and m0 with instantaneous probability. */
                tonal.pspeech[0] = s0 * (1 - tau) * speech0
                tonal.pmusic[0] = m0 * (1 - tau) * music0
                /* Propagate the transition probabilities */
                i = 1
                while (i < OpusConstants.DETECT_SIZE - 1) {
                    tonal.pspeech[i] = tonal.pspeech[i + 1] * speech0
                    tonal.pmusic[i] = tonal.pmusic[i + 1] * music0
                    i++
                }
                /* Probability that the latest frame is speech, when all the previous ones were music. */
                tonal.pspeech[OpusConstants.DETECT_SIZE - 1] = m0 * tau * speech0
                /* Probability that the latest frame is music, when all the previous ones were speech. */
                tonal.pmusic[OpusConstants.DETECT_SIZE - 1] = s0 * tau * music0

                /* Renormalise probabilities to 1 */
                i = 0
                while (i < OpusConstants.DETECT_SIZE) {
                    psum += tonal.pspeech[i] + tonal.pmusic[i]
                    i++
                }
                psum = 1.0f / psum
                i = 0
                while (i < OpusConstants.DETECT_SIZE) {
                    tonal.pspeech[i] *= psum
                    tonal.pmusic[i] *= psum
                    i++
                }
                psum = tonal.pmusic[0]
                i = 1
                while (i < OpusConstants.DETECT_SIZE) {
                    psum += tonal.pspeech[i]
                    i++
                }

                /* Estimate our confidence in the speech/music decisions */
                if (frame_probs[1] > .75) {
                    if (tonal.music_prob > .9) {
                        val adapt: Float
                        adapt = 1.0f / ++tonal.music_confidence_count
                        tonal.music_confidence_count = Inlines.IMIN(tonal.music_confidence_count, 500)
                        tonal.music_confidence += adapt * Inlines.MAX16(-.2f, frame_probs[0] - tonal.music_confidence)
                    }
                    if (tonal.music_prob < .1) {
                        val adapt: Float
                        adapt = 1.0f / ++tonal.speech_confidence_count
                        tonal.speech_confidence_count = Inlines.IMIN(tonal.speech_confidence_count, 500)
                        tonal.speech_confidence += adapt * Inlines.MIN16(.2f, frame_probs[0] - tonal.speech_confidence)
                    }
                } else {
                    if (tonal.music_confidence_count == 0) {
                        tonal.music_confidence = .9f
                    }
                    if (tonal.speech_confidence_count == 0) {
                        tonal.speech_confidence = .1f
                    }
                }
            }
            if (tonal.last_music != (if (tonal.music_prob > .5f) 1 else 0)) {
                tonal.last_transition = 0
            }
            tonal.last_music = if (tonal.music_prob > .5f) 1 else 0
        } else {
            info.music_prob = 0f
        }

        info.bandwidth = bandwidth
        info.noisiness = frame_noisiness
        info.valid = 1
    }

    fun run_analysis(
        analysis: TonalityAnalysisState, celt_mode: CeltMode, analysis_pcm: ShortArray?, analysis_pcm_ptr: Int,
        analysis_frame_size: Int, frame_size: Int, c1: Int, c2: Int, C: Int, Fs: Int,
        lsb_depth: Int, analysis_info: AnalysisInfo
    ) {
        var analysis_frame_size = analysis_frame_size
        var offset: Int
        var pcm_len: Int

        if (analysis_pcm != null) {
            /* Avoid overflow/wrap-around of the analysis buffer */
            analysis_frame_size = Inlines.IMIN((OpusConstants.DETECT_SIZE - 5) * Fs / 100, analysis_frame_size)

            pcm_len = analysis_frame_size - analysis.analysis_offset
            offset = analysis.analysis_offset
            do {
                tonality_analysis(
                    analysis,
                    celt_mode,
                    analysis_pcm,
                    analysis_pcm_ptr,
                    Inlines.IMIN(480, pcm_len),
                    offset,
                    c1,
                    c2,
                    C,
                    lsb_depth
                )
                offset += 480
                pcm_len -= 480
            } while (pcm_len > 0)
            analysis.analysis_offset = analysis_frame_size

            analysis.analysis_offset -= frame_size
        }

        analysis_info.valid = 0
        tonality_get_info(analysis, analysis_info, frame_size)
    }
}
