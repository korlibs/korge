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

internal object Bands {

    private val bit_interleave_table = byteArrayOf(0, 1, 1, 1, 2, 3, 3, 3, 2, 3, 3, 3, 2, 3, 3, 3)

    private val bit_deinterleave_table =
        shortArrayOf(0x00, 0x03, 0x0C, 0x0F, 0x30, 0x33, 0x3C, 0x3F, 0xC0, 0xC3, 0xCC, 0xCF, 0xF0, 0xF3, 0xFC, 0xFF)

    fun hysteresis_decision(
        `val`: Int,
        thresholds: IntArray,
        hysteresis: IntArray,
        N: Int,
        prev: Int
    ): Int {
        var i: Int
        i = 0
        while (i < N) {
            if (`val` < thresholds[i]) {
                break
            }
            i++
        }

        if (i > prev && `val` < thresholds[prev] + hysteresis[prev]) {
            i = prev
        }

        if (i < prev && `val` > thresholds[prev - 1] - hysteresis[prev - 1]) {
            i = prev
        }

        return i
    }

    fun celt_lcg_rand(seed: Int): Int {
        return 1664525 * seed + 1013904223
    }

    /* This is a cos() approximation designed to be bit-exact on any platform. Bit exactness
       with this approximation is important because it has an impact on the bit allocation */
    fun bitexact_cos(x: Int): Int {
        val tmp: Int
        var x2: Int
        tmp = 4096 + x * x shr 13
        Inlines.OpusAssert(tmp <= 32767)
        x2 = tmp
        x2 = 32767 - x2 + Inlines.FRAC_MUL16(x2, -7651 + Inlines.FRAC_MUL16(x2, 8277 + Inlines.FRAC_MUL16(-626, x2)))
        Inlines.OpusAssert(x2 <= 32766)
        return 1 + x2
    }

    fun bitexact_log2tan(isin: Int, icos: Int): Int {
        var isin = isin
        var icos = icos
        val lc = Inlines.EC_ILOG(icos.toLong())
        val ls = Inlines.EC_ILOG(isin.toLong())
        icos = icos shl 15 - lc
        isin = isin shl 15 - ls
        return (ls - lc) * (1 shl 11) + Inlines.FRAC_MUL16(
            isin,
            Inlines.FRAC_MUL16(isin, -2597) + 7932
        ) - Inlines.FRAC_MUL16(icos, Inlines.FRAC_MUL16(icos, -2597) + 7932)
    }

    /* Compute the amplitude (sqrt energy) in each of the bands */
    fun compute_band_energies(m: CeltMode, X: Array<IntArray>, bandE: Array<IntArray>, end: Int, C: Int, LM: Int) {
        var i: Int
        var c: Int
        val N: Int
        val eBands = m.eBands
        N = m.shortMdctSize shl LM
        c = 0

        do {
            i = 0
            while (i < end) {
                var j: Int
                var maxval = 0
                var sum = 0
                maxval = Inlines.celt_maxabs32(X[c], eBands!![i] shl LM, eBands!![i + 1] - eBands!![i] shl LM)
                if (maxval > 0) {
                    val shift = Inlines.celt_ilog2(maxval) - 14 + ((m.logN!![i] shr EntropyCoder.BITRES) + LM + 1 shr 1)
                    j = eBands!![i] shl LM
                    if (shift > 0) {
                        do {
                            sum = Inlines.MAC16_16(
                                sum, Inlines.EXTRACT16(Inlines.SHR32(X[c][j], shift)),
                                Inlines.EXTRACT16(Inlines.SHR32(X[c][j], shift))
                            )
                        } while (++j < eBands!![i + 1] shl LM)
                    } else {
                        do {
                            sum = Inlines.MAC16_16(
                                sum, Inlines.EXTRACT16(Inlines.SHL32(X[c][j], -shift)),
                                Inlines.EXTRACT16(Inlines.SHL32(X[c][j], -shift))
                            )
                        } while (++j < eBands!![i + 1] shl LM)
                    }
                    /* We're adding one here to ensure the normalized band isn't larger than unity norm */
                    bandE[c][i] = CeltConstants.EPSILON + Inlines.VSHR32(Inlines.celt_sqrt(sum), -shift)
                } else {
                    bandE[c][i] = CeltConstants.EPSILON
                }
                i++
                /*printf ("%f ", bandE[i+c*m->nbEBands]);*/
            }
        } while (++c < C)
    }

    /* Normalise each band such that the energy is one. */
    fun normalise_bands(
        m: CeltMode,
        freq: Array<IntArray>,
        X: Array<IntArray>,
        bandE: Array<IntArray>,
        end: Int,
        C: Int,
        M: Int
    ) {
        var i: Int
        var c: Int
        val eBands = m.eBands
        c = 0
        do {
            i = 0
            do {
                val g: Int
                var j: Int
                val shift: Int
                val E: Int
                shift = Inlines.celt_zlog2(bandE[c][i]) - 13
                E = Inlines.VSHR32(bandE[c][i], shift)
                g = Inlines.EXTRACT16(Inlines.celt_rcp(Inlines.SHL32(E, 3))).toInt()
                j = M * eBands!![i]
                do {
                    X[c][j] = Inlines.MULT16_16_Q15(Inlines.VSHR32(freq[c][j], shift - 1), g)
                } while (++j < M * eBands!![i + 1])
            } while (++i < end)
        } while (++c < C)
    }

    /* De-normalise the energy to produce the synthesis from the unit-energy bands */
    fun denormalise_bands(
        m: CeltMode, X: IntArray,
        freq: IntArray, freq_ptr: Int, bandLogE: IntArray, bandLogE_ptr: Int, start: Int,
        end: Int, M: Int, downsample: Int, silence: Int
    ) {
        var start = start
        var end = end
        var i: Int
        val N: Int
        var bound: Int
        var f: Int
        var x: Int
        val eBands = m.eBands
        N = M * m.shortMdctSize
        bound = M * eBands!![end]
        if (downsample != 1) {
            bound = Inlines.IMIN(bound, N / downsample)
        }
        if (silence != 0) {
            bound = 0
            end = 0
            start = end
        }
        f = freq_ptr
        x = M * eBands!![start]

        i = 0
        while (i < M * eBands!![start]) {
            freq[f++] = 0
            i++
        }

        i = start
        while (i < end) {
            var j: Int
            val band_end: Int
            var g: Int
            val lg: Int
            var shift: Int

            j = M * eBands!![i]
            band_end = M * eBands!![i + 1]
            lg = Inlines.ADD16(bandLogE[bandLogE_ptr + i], Inlines.SHL16(CeltTables.eMeans[i].toShort(), 6).toInt())

            /* Handle the integer part of the log energy */
            shift = 16 - (lg shr CeltConstants.DB_SHIFT)
            if (shift > 31) {
                shift = 0
                g = 0
            } else {
                /* Handle the fractional part. */
                g = Inlines.celt_exp2_frac(lg and (1 shl CeltConstants.DB_SHIFT) - 1)
            }
            /* Handle extreme gains with negative shift. */
            if (shift < 0) {
                /* For shift < -2 we'd be likely to overflow, so we're capping
                      the gain here. This shouldn't happen unless the bitstream is
                      already corrupted. */
                if (shift < -2) {
                    g = 32767
                    shift = -2
                }
                do {
                    freq[f] = Inlines.SHR32(Inlines.MULT16_16(X[x], g), -shift)
                } while (++j < band_end)
            } else {
                do {
                    freq[f++] = Inlines.SHR32(Inlines.MULT16_16(X[x++], g), shift)
                } while (++j < band_end)
            }
            i++
        }

        Inlines.OpusAssert(start <= end)
        Arrays.MemSetWithOffset(freq, 0, freq_ptr + bound, N - bound)
    }

    /* This prevents energy collapse for transients with multiple short MDCTs */
    fun anti_collapse(
        m: CeltMode, X_: Array<IntArray>, collapse_masks: ShortArray, LM: Int, C: Int, size: Int,
        start: Int, end: Int, logE: IntArray, prev1logE: IntArray,
        prev2logE: IntArray, pulses: IntArray, seed: Int
    ) {
        var seed = seed
        var c: Int
        var i: Int
        var j: Int
        var k: Int
        i = start
        while (i < end) {
            val N0: Int
            val thresh: Int
            var sqrt_1: Int = 0
            val depth: Int
            var shift: Int = 0
            val thresh32: Int

            N0 = m.eBands!![i + 1] - m.eBands!![i]
            /* depth in 1/8 bits */
            Inlines.OpusAssert(pulses[i] >= 0)
            depth = Inlines.celt_udiv(1 + pulses[i], m.eBands!![i + 1] - m.eBands!![i]) shr LM

            thresh32 = Inlines.SHR32(Inlines.celt_exp2(0 - Inlines.SHL16(depth, 10 - EntropyCoder.BITRES)), 1)
            thresh = Inlines.MULT16_32_Q15(
                (0.5 + 0.5f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.5f, 15)*/,
                Inlines.MIN32(32767, thresh32)
            )
            run {
                var t: Int
                t = N0 shl LM
                shift = Inlines.celt_ilog2(t) shr 1
                t = Inlines.SHL32(t, 7 - shift shl 1)
                sqrt_1 = Inlines.celt_rsqrt_norm(t)
            }

            c = 0
            do {
                val X: Int
                var prev1: Int
                var prev2: Int
                var Ediff: Int
                var r: Int
                var renormalize = 0
                prev1 = prev1logE[c * m.nbEBands + i]
                prev2 = prev2logE[c * m.nbEBands + i]
                if (C == 1) {
                    prev1 = Inlines.MAX16(prev1, prev1logE[m.nbEBands + i])
                    prev2 = Inlines.MAX16(prev2, prev2logE[m.nbEBands + i])
                }
                Ediff = Inlines.EXTEND32(logE[c * m.nbEBands + i]) - Inlines.EXTEND32(Inlines.MIN16(prev1, prev2))
                Ediff = Inlines.MAX32(0, Ediff)

                if (Ediff < 16384) {
                    val r32 = Inlines.SHR32(Inlines.celt_exp2((0 - Inlines.EXTRACT16(Ediff)).toShort().toInt()), 1)
                    r = 2 * Inlines.MIN16(16383, r32)
                } else {
                    r = 0
                }
                if (LM == 3) {
                    r = Inlines.MULT16_16_Q14(23170, Inlines.MIN32(23169, r)) // opus bug: was MIN32
                }
                r = Inlines.SHR16(Inlines.MIN16(thresh, r), 1)
                r = Inlines.SHR32(Inlines.MULT16_16_Q15(sqrt_1, r), shift)

                X = m.eBands!![i] shl LM
                k = 0
                while (k < 1 shl LM) {
                    /* Detect collapse */
                    if (collapse_masks[i * C + c] and (1 shl k) == 0) {
                        /* Fill with noise */
                        val Xk = X + k
                        j = 0
                        while (j < N0) {
                            seed = celt_lcg_rand(seed)
                            X_[c][Xk + (j shl LM)] = if (seed and 0x8000 != 0) r else 0 - r
                            j++
                        }
                        renormalize = 1
                    }
                    k++
                }
                /* We just added some energy, so we need to renormalise */
                if (renormalize != 0) {
                    VQ.renormalise_vector(X_[c], X, N0 shl LM, CeltConstants.Q15ONE)
                }
            } while (++c < C)
            i++
        }
    }

    fun intensity_stereo(
        m: CeltMode,
        X: IntArray?,
        X_ptr: Int,
        Y: IntArray?,
        Y_ptr: Int,
        bandE: Array<IntArray>,
        bandID: Int,
        N: Int
    ) {
        var j: Int
        val a1: Int
        val a2: Int
        val left: Int
        val right: Int
        val norm: Int
        val shift = Inlines.celt_zlog2(Inlines.MAX32(bandE[0][bandID], bandE[1][bandID])) - 13
        left = Inlines.VSHR32(bandE[0][bandID], shift)
        right = Inlines.VSHR32(bandE[1][bandID], shift)
        norm = CeltConstants.EPSILON + Inlines.celt_sqrt(
            CeltConstants.EPSILON + Inlines.MULT16_16(left, left) + Inlines.MULT16_16(
                right,
                right
            )
        )
        a1 = Inlines.DIV32_16(Inlines.SHL32(left, 14), norm)
        a2 = Inlines.DIV32_16(Inlines.SHL32(right, 14), norm)
        j = 0
        while (j < N) {
            val r: Int
            val l: Int
            l = X!![X_ptr + j]
            r = Y!![Y_ptr + j]
            X[X_ptr + j] =
                    Inlines.EXTRACT16(Inlines.SHR32(Inlines.MAC16_16(Inlines.MULT16_16(a1, l), a2, r), 14)).toInt()
            j++
            /* Side is not encoded, no need to calculate */
        }
    }

    fun stereo_split(X: IntArray?, X_ptr: Int, Y: IntArray?, Y_ptr: Int, N: Int) {
        var j: Int
        j = 0
        while (j < N) {
            val r: Int
            val l: Int
            l = Inlines.MULT16_16(
                (0.5 + .70710678f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.70710678f, 15)*/,
                X!![X_ptr + j]
            )
            r = Inlines.MULT16_16(
                (0.5 + .70710678f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.70710678f, 15)*/,
                Y!![Y_ptr + j]
            )
            X[X_ptr + j] = Inlines.EXTRACT16(Inlines.SHR32(Inlines.ADD32(l, r), 15)).toInt()
            Y[Y_ptr + j] = Inlines.EXTRACT16(Inlines.SHR32(Inlines.SUB32(r, l), 15)).toInt()
            j++
        }
    }

    fun stereo_merge(X: IntArray, X_ptr: Int, Y: IntArray, Y_ptr: Int, mid: Int, N: Int) {
        var j: Int
        val xp = BoxedValueInt(0)
        val side = BoxedValueInt(0)
        val El: Int
        val Er: Int
        val mid2: Int
        var kl: Int
        var kr: Int
        var t: Int
        val lgain: Int
        val rgain: Int

        /* Compute the norm of X+Y and X-Y as |X|^2 + |Y|^2 +/- sum(xy) */
        Kernels.dual_inner_prod(Y, Y_ptr, X, X_ptr, Y, Y_ptr, N, xp, side)
        /* Compensating for the mid normalization */
        xp.Val = Inlines.MULT16_32_Q15(mid, xp.Val)
        /* mid and side are in Q15, not Q14 like X and Y */
        mid2 = Inlines.SHR16(mid, 1)
        El = Inlines.MULT16_16(mid2, mid2) + side.Val - 2 * xp.Val
        Er = Inlines.MULT16_16(mid2, mid2) + side.Val + 2 * xp.Val
        if (Er < (0.5 + 6e-4f * (1 shl 28)).toInt()/*Inlines.QCONST32(6e-4f, 28)*/ || El < (0.5 + 6e-4f * (1 shl 28)).toInt()/*Inlines.QCONST32(6e-4f, 28)*/) {
            arraycopy(X, X_ptr, Y!!, Y_ptr, N)
            return
        }

        kl = Inlines.celt_ilog2(El) shr 1
        kr = Inlines.celt_ilog2(Er) shr 1
        t = Inlines.VSHR32(El, kl - 7 shl 1)
        lgain = Inlines.celt_rsqrt_norm(t)
        t = Inlines.VSHR32(Er, kr - 7 shl 1)
        rgain = Inlines.celt_rsqrt_norm(t)

        if (kl < 7) {
            kl = 7
        }
        if (kr < 7) {
            kr = 7
        }

        j = 0
        while (j < N) {
            val r: Int
            val l: Int
            /* Apply mid scaling (side is already scaled) */
            l = Inlines.MULT16_16_P15(mid, X[X_ptr + j])
            r = Y!![Y_ptr + j]
            X[X_ptr + j] =
                    Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MULT16_16(lgain, Inlines.SUB16(l, r)), kl + 1)).toInt()
            Y[Y_ptr + j] =
                    Inlines.EXTRACT16(Inlines.PSHR32(Inlines.MULT16_16(rgain, Inlines.ADD16(l, r)), kr + 1)).toInt()
            j++
        }
    }

    /* Decide whether we should spread the pulses in the current frame */
    fun spreading_decision(
        m: CeltMode, X: Array<IntArray>, average: BoxedValueInt,
        last_decision: Int, hf_average: BoxedValueInt, tapset_decision: BoxedValueInt, update_hf: Int,
        end: Int, C: Int, M: Int
    ): Int {
        var i: Int
        var c: Int
        var sum = 0
        var nbBands = 0
        val eBands = m.eBands
        val decision: Int
        var hf_sum = 0

        Inlines.OpusAssert(end > 0)

        if (M * (eBands!![end] - eBands!![end - 1]) <= 8) {
            return Spread.SPREAD_NONE
        }

        c = 0

        do {
            i = 0
            while (i < end) {
                var j: Int
                val N: Int
                var tmp = 0
                val tcount = intArrayOf(0, 0, 0)
                val x = X[c]
                val x_ptr = M * eBands!![i]
                N = M * (eBands!![i + 1] - eBands!![i])
                if (N <= 8) {
                    i++
                    continue
                }
                /* Compute rough CDF of |x[j]| */
                j = x_ptr
                while (j < N + x_ptr) {
                    val x2N: Int
                    /* Q13 */

                    x2N = Inlines.MULT16_16(Inlines.MULT16_16_Q15(x[j], x[j]), N)
                    if (x2N < (0.5 + 0.25f * (1 shl 13)).toShort()/*Inlines.QCONST16(0.25f, 13)*/) {
                        tcount[0]++
                    }
                    if (x2N < (0.5 + 0.0625f * (1 shl 13)).toShort()/*Inlines.QCONST16(0.0625f, 13)*/) {
                        tcount[1]++
                    }
                    if (x2N < (0.5 + 0.015625f * (1 shl 13)).toShort()/*Inlines.QCONST16(0.015625f, 13)*/) {
                        tcount[2]++
                    }
                    j++
                }

                /* Only include four last bands (8 kHz and up) */
                if (i > m.nbEBands - 4) {
                    hf_sum += Inlines.celt_udiv(32 * (tcount[1] + tcount[0]), N)
                }

                tmp = (if (2 * tcount[2] >= N) 1 else 0) + (if (2 * tcount[1] >= N) 1 else 0) +
                        if (2 * tcount[0] >= N) 1 else 0
                sum += tmp * 256
                nbBands++
                i++
            }
        } while (++c < C)

        if (update_hf != 0) {
            if (hf_sum != 0) {
                hf_sum = Inlines.celt_udiv(hf_sum, C * (4 - m.nbEBands + end))
            }

            hf_average.Val = hf_average.Val + hf_sum shr 1
            hf_sum = hf_average.Val

            if (tapset_decision.Val == 2) {
                hf_sum += 4
            } else if (tapset_decision.Val == 0) {
                hf_sum -= 4
            }
            if (hf_sum > 22) {
                tapset_decision.Val = 2
            } else if (hf_sum > 18) {
                tapset_decision.Val = 1
            } else {
                tapset_decision.Val = 0
            }
        }

        Inlines.OpusAssert(nbBands > 0)
        /* end has to be non-zero */
        Inlines.OpusAssert(sum >= 0)
        sum = Inlines.celt_udiv(sum, nbBands)

        /* Recursive averaging */
        sum = sum + average.Val shr 1
        average.Val = sum

        /* Hysteresis */
        sum = 3 * sum + ((3 - last_decision shl 7) + 64) + 2 shr 2
        if (sum < 80) {
            decision = Spread.SPREAD_AGGRESSIVE
        } else if (sum < 256) {
            decision = Spread.SPREAD_NORMAL
        } else if (sum < 384) {
            decision = Spread.SPREAD_LIGHT
        } else {
            decision = Spread.SPREAD_NONE
        }
        return decision
    }

    fun deinterleave_hadamard(X: IntArray?, X_ptr: Int, N0: Int, stride: Int, hadamard: Int) {
        var i: Int
        var j: Int
        val N: Int
        N = N0 * stride
        val tmp = IntArray(N)

        Inlines.OpusAssert(stride > 0)
        if (hadamard != 0) {
            val ordery = stride - 2

            i = 0
            while (i < stride) {
                j = 0
                while (j < N0) {
                    tmp[CeltTables.ordery_table[ordery + i] * N0 + j] = X!![j * stride + i + X_ptr]
                    j++
                }
                i++
            }
        } else {
            i = 0
            while (i < stride) {
                j = 0
                while (j < N0) {
                    tmp[i * N0 + j] = X!![j * stride + i + X_ptr]
                    j++
                }
                i++
            }
        }

        arraycopy(tmp, 0, X!!, X_ptr, N)
    }

    fun interleave_hadamard(X: IntArray?, X_ptr: Int, N0: Int, stride: Int, hadamard: Int) {
        var i: Int
        var j: Int
        val N: Int
        N = N0 * stride
        val tmp = IntArray(N)

        if (hadamard != 0) {
            val ordery = stride - 2
            i = 0
            while (i < stride) {
                j = 0
                while (j < N0) {
                    tmp[j * stride + i] = X!![CeltTables.ordery_table[ordery + i] * N0 + j + X_ptr]
                    j++
                }
                i++
            }
        } else {
            i = 0
            while (i < stride) {
                j = 0
                while (j < N0) {
                    tmp[j * stride + i] = X!![i * N0 + j + X_ptr]
                    j++
                }
                i++
            }
        }

        arraycopy(tmp, 0, X!!, X_ptr, N)
    }

    fun haar1(X: IntArray?, X_ptr: Int, N0: Int, stride: Int) {
        var N0 = N0
        var i: Int
        var j: Int
        N0 = N0 shr 1
        i = 0
        while (i < stride) {
            j = 0
            while (j < N0) {
                val tmpidx = X_ptr + i + stride * 2 * j
                val tmp1: Int
                val tmp2: Int
                tmp1 = Inlines.MULT16_16(
                    (0.5 + .70710678f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.70710678f, 15)*/,
                    X!![tmpidx]
                )
                tmp2 = Inlines.MULT16_16(
                    (0.5 + .70710678f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.70710678f, 15)*/,
                    X[tmpidx + stride]
                )
                X[tmpidx] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.ADD32(tmp1, tmp2), 15)).toInt()
                X[tmpidx + stride] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.SUB32(tmp1, tmp2), 15)).toInt()
                j++
            }
            i++
        }
    }

    fun haar1ZeroOffset(X: IntArray, N0: Int, stride: Int) {
        var N0 = N0
        var i: Int
        var j: Int
        N0 = N0 shr 1
        i = 0
        while (i < stride) {
            j = 0
            while (j < N0) {
                val tmpidx = i + stride * 2 * j
                val tmp1: Int
                val tmp2: Int
                tmp1 = Inlines.MULT16_16(
                    (0.5 + .70710678f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.70710678f, 15)*/,
                    X[tmpidx]
                )
                tmp2 = Inlines.MULT16_16(
                    (0.5 + .70710678f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.70710678f, 15)*/,
                    X[tmpidx + stride]
                )
                X[tmpidx] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.ADD32(tmp1, tmp2), 15)).toInt()
                X[tmpidx + stride] = Inlines.EXTRACT16(Inlines.PSHR32(Inlines.SUB32(tmp1, tmp2), 15)).toInt()
                j++
            }
            i++
        }
    }

    fun compute_qn(N: Int, b: Int, offset: Int, pulse_cap: Int, stereo: Int): Int {
        val exp2_table8 = shortArrayOf(16384, 17866, 19483, 21247, 23170, 25267, 27554, 30048)
        var qn: Int
        var qb: Int
        var N2 = 2 * N - 1
        if (stereo != 0 && N == 2) {
            N2--
        }

        /* The upper limit ensures that in a stereo split with itheta==16384, we'll
            always have enough bits left over to code at least one pulse in the
            side; otherwise it would collapse, since it doesn't get folded. */
        qb = Inlines.celt_sudiv(b + N2 * offset, N2)
        qb = Inlines.IMIN(b - pulse_cap - (4 shl EntropyCoder.BITRES), qb)

        qb = Inlines.IMIN(8 shl EntropyCoder.BITRES, qb)

        if (qb < 1 shl EntropyCoder.BITRES shr 1) {
            qn = 1
        } else {
            qn = exp2_table8[qb and 0x7] shr 14 - (qb shr EntropyCoder.BITRES)
            qn = qn + 1 shr 1 shl 1
        }
        Inlines.OpusAssert(qn <= 256)
        return qn
    }

    class band_ctx {

        var encode: Int = 0
        var m: CeltMode? = null
        var i: Int = 0
        var intensity: Int = 0
        var spread: Int = 0
        var tf_change: Int = 0
        var ec: EntropyCoder? = null
        var remaining_bits: Int = 0
        var bandE: Array<IntArray>? = null
        var seed: Int = 0
    }

    class split_ctx {

        var inv: Int = 0
        var imid: Int = 0
        var iside: Int = 0
        var delta: Int = 0
        var itheta: Int = 0
        var qalloc: Int = 0
    }

    fun compute_theta(
        ctx: band_ctx, sctx: split_ctx,
        X: IntArray, X_ptr: Int, Y: IntArray, Y_ptr: Int, N: Int, b: BoxedValueInt, B: Int, B0: Int,
        LM: Int,
        stereo: Int, fill: BoxedValueInt
    ) {
        var qn: Int
        var itheta = 0
        val delta: Int
        val imid: Int
        val iside: Int
        val qalloc: Int
        val pulse_cap: Int
        val offset: Int
        val tell: Int
        var inv = 0
        val encode: Int
        val m: CeltMode?
        val i: Int
        val intensity: Int
        val ec: EntropyCoder? // porting note: pointer
        val bandE: Array<IntArray>?

        encode = ctx.encode
        m = ctx.m
        i = ctx.i
        intensity = ctx.intensity
        ec = ctx.ec
        bandE = ctx.bandE

        /* Decide on the resolution to give to the split parameter theta */
        pulse_cap = m!!.logN!![i] + LM * (1 shl EntropyCoder.BITRES)
        offset = (pulse_cap shr 1) -
                if (stereo != 0 && N == 2) CeltConstants.QTHETA_OFFSET_TWOPHASE else CeltConstants.QTHETA_OFFSET
        qn = compute_qn(N, b.Val, offset, pulse_cap, stereo)
        if (stereo != 0 && i >= intensity) {
            qn = 1
        }

        if (encode != 0) {
            /* theta is the atan() of the ratio between the (normalized)
               side and mid. With just that parameter, we can re-scale both
               mid and side because we know that 1) they have unit norm and
               2) they are orthogonal. */
            itheta = VQ.stereo_itheta(X, X_ptr, Y, Y_ptr, stereo, N)
        }

        tell = ec!!.tell_frac().toInt()

        if (qn != 1) {
            if (encode != 0) {
                itheta = itheta * qn + 8192 shr 14
            }

            /* Entropy coding of the angle. We use a uniform pdf for the
               time split, a step for stereo, and a triangular one for the rest. */
            if (stereo != 0 && N > 2) {
                val p0 = 3
                var x = itheta
                val x0 = qn / 2
                val ft = Inlines.CapToUInt32(p0 * (x0 + 1) + x0)
                /* Use a probability of p0 up to itheta=8192 and then use 1 after */
                if (encode != 0) {
                    ec!!.encode(
                        (if (x <= x0)
                            p0 * x
                        else
                            x - 1 - x0 + (x0 + 1) * p0).toLong(),
                        (if (x <= x0)
                            p0 * (x + 1)
                        else
                            x - x0 + (x0 + 1) * p0).toLong(),
                        ft
                    )
                } else {
                    val fs = ec!!.decode(ft).toInt()
                    if (fs < (x0 + 1) * p0) {
                        x = fs / p0
                    } else {
                        x = x0 + 1 + (fs - (x0 + 1) * p0)
                    }

                    ec!!.dec_update(
                        (if (x <= x0)
                            p0 * x
                        else
                            x - 1 - x0 + (x0 + 1) * p0).toLong(),
                        (if (x <= x0)
                            p0 * (x + 1)
                        else
                            x - x0 + (x0 + 1) * p0).toLong(),
                        ft
                    )
                    itheta = x
                }
            } else if (B0 > 1 || stereo != 0) {
                /* Uniform pdf */
                if (encode != 0) {
                    ec!!.enc_uint(itheta.toLong(), (qn + 1).toLong())
                } else {
                    itheta = ec!!.dec_uint((qn + 1).toLong()).toInt()
                }
            } else {
                var fs = 1
                val ft: Int
                ft = ((qn shr 1) + 1) * ((qn shr 1) + 1)
                if (encode != 0) {
                    val fl: Int

                    fs = if (itheta <= qn shr 1) itheta + 1 else qn + 1 - itheta
                    fl = if (itheta <= qn shr 1)
                        itheta * (itheta + 1) shr 1
                    else
                        ft - ((qn + 1 - itheta) * (qn + 2 - itheta) shr 1)

                    ec!!.encode(fl.toLong(), (fl + fs).toLong(), ft.toLong())
                } else {
                    /* Triangular pdf */
                    var fl = 0
                    val fm: Int
                    fm = ec!!.decode(ft.toLong()).toInt()

                    if (fm < (qn shr 1) * ((qn shr 1) + 1) shr 1) {
                        itheta = Inlines.isqrt32((8 * fm + 1).toLong()) - 1 shr 1
                        fs = itheta + 1
                        fl = itheta * (itheta + 1) shr 1
                    } else {
                        itheta = 2 * (qn + 1) - Inlines.isqrt32((8 * (ft - fm - 1) + 1).toLong()) shr 1
                        fs = qn + 1 - itheta
                        fl = ft - ((qn + 1 - itheta) * (qn + 2 - itheta) shr 1)
                    }

                    ec!!.dec_update(fl.toLong(), (fl + fs).toLong(), ft.toLong())
                }
            }
            Inlines.OpusAssert(itheta >= 0)
            itheta = Inlines.celt_udiv(itheta * 16384, qn)
            if (encode != 0 && stereo != 0) {
                if (itheta == 0) {
                    intensity_stereo(m, X, X_ptr, Y, Y_ptr, bandE!!, i, N)
                } else {
                    stereo_split(X, X_ptr, Y, Y_ptr, N)
                }
            }
        } else if (stereo != 0) {
            if (encode != 0) {
                inv = if (itheta > 8192) 1 else 0
                if (inv != 0) {
                    var j: Int
                    j = 0
                    while (j < N) {
                        Y[Y_ptr + j] = 0 - Y!![Y_ptr + j]
                        j++
                    }
                }
                intensity_stereo(m, X, X_ptr, Y, Y_ptr, bandE!!, i, N)
            }
            if (b.Val > 2 shl EntropyCoder.BITRES && ctx.remaining_bits > 2 shl EntropyCoder.BITRES) {
                if (encode != 0) {
                    ec!!.enc_bit_logp(inv, 2)
                } else {
                    inv = ec!!.dec_bit_logp(2)
                }
            } else {
                inv = 0
            }
            itheta = 0
        }
        qalloc = ec!!.tell_frac().toInt() - tell
        b.Val -= qalloc

        if (itheta == 0) {
            imid = 32767
            iside = 0
            fill.Val = fill.Val and (1 shl B) - 1
            delta = -16384
        } else if (itheta == 16384) {
            imid = 0
            iside = 32767
            fill.Val = fill.Val and ((1 shl B) - 1 shl B)
            delta = 16384
        } else {
            imid = bitexact_cos(itheta.toShort().toInt())
            iside = bitexact_cos((16384 - itheta).toShort().toInt())
            /* This is the mid vs side allocation that minimizes squared error
               in that band. */
            delta = Inlines.FRAC_MUL16(N - 1 shl 7, bitexact_log2tan(iside, imid))
        }

        sctx.inv = inv
        sctx.imid = imid
        sctx.iside = iside
        sctx.delta = delta
        sctx.itheta = itheta
        sctx.qalloc = qalloc
    }

    fun quant_band_n1(
        ctx: band_ctx, X: IntArray?, X_ptr: Int, Y: IntArray?, Y_ptr: Int, b: Int,
        lowband_out: IntArray?, lowband_out_ptr: Int
    ): Int {
        var b = b
        val resynth = if (ctx.encode == 0) 1 else 0
        var c: Int
        val stereo: Int
        var x = X
        var x_ptr = X_ptr
        val encode: Int
        val ec: EntropyCoder? // porting note: pointer

        encode = ctx.encode
        ec = ctx.ec

        stereo = if (Y != null) 1 else 0
        c = 0
        do {
            var sign = 0
            if (ctx.remaining_bits >= 1 shl EntropyCoder.BITRES) {
                if (encode != 0) {
                    sign = if (x!![x_ptr] < 0) 1 else 0
                    ec!!.enc_bits(sign.toLong(), 1)
                } else {
                    sign = ec!!.dec_bits(1)
                }
                ctx.remaining_bits -= 1 shl EntropyCoder.BITRES
                b -= 1 shl EntropyCoder.BITRES
            }
            if (resynth != 0) {
                x!![x_ptr] = if (sign != 0) 0 - CeltConstants.NORM_SCALING else CeltConstants.NORM_SCALING
            }
            x = Y
            x_ptr = Y_ptr
        } while (++c < 1 + stereo)
        if (lowband_out != null) {
            lowband_out[lowband_out_ptr] = Inlines.SHR16(X!![X_ptr], 4)
        }

        return 1
    }

    /* This function is responsible for encoding and decoding a mono partition.
       It can split the band in two and transmit the energy difference with
       the two half-bands. It can be called recursively so bands can end up being
       split in 8 parts. */
    fun quant_partition(
        ctx: band_ctx, X: IntArray, X_ptr: Int,
        N: Int, b: Int, B: Int, lowband: IntArray?, lowband_ptr: Int,
        LM: Int,
        gain: Int, fill: Int
    ): Int {
        var N = N
        var b = b
        var B = B
        var LM = LM
        var fill = fill
        val cache_ptr: Int
        var q: Int
        var curr_bits: Int
        var imid = 0
        var iside = 0
        val B0 = B
        var mid = 0
        var side = 0
        var cm = 0
        val resynth = if (ctx.encode == 0) 1 else 0
        var Y = 0
        val encode: Int
        val m: CeltMode? //porting note: pointer
        val i: Int
        val spread: Int
        lateinit var ec: EntropyCoder //porting note: pointer

        encode = ctx.encode
        m = ctx.m
        i = ctx.i
        spread = ctx.spread
        ec = ctx.ec!!
        val cache = m!!.cache.bits
        /* If we need 1.5 more bits than we can produce, split the band in two. */
        cache_ptr = m!!.cache.index!![(LM + 1) * m!!.nbEBands + i].toInt()
        if (LM != -1 && b > cache!![cache_ptr + cache!![cache_ptr]] + 12 && N > 2) {
            var mbits: Int
            var sbits: Int
            var delta: Int
            val itheta: Int
            val qalloc: Int
            val sctx = split_ctx()
            var next_lowband2 = 0
            var rebalance: Int

            N = N shr 1
            Y = X_ptr + N
            LM -= 1
            if (B == 1) {
                fill = fill and 1 or (fill shl 1)
            }

            B = B + 1 shr 1

            val boxed_b = BoxedValueInt(b)
            val boxed_fill = BoxedValueInt(fill)
            compute_theta(ctx, sctx, X, X_ptr, X, Y, N, boxed_b, B, B0, LM, 0, boxed_fill)
            b = boxed_b.Val
            fill = boxed_fill.Val

            imid = sctx.imid
            iside = sctx.iside
            delta = sctx.delta
            itheta = sctx.itheta
            qalloc = sctx.qalloc
            mid = imid
            side = iside

            /* Give more bits to low-energy MDCTs than they would otherwise deserve */
            if (B0 > 1 && itheta and 0x3fff != 0) {
                if (itheta > 8192)
                /* Rough approximation for pre-echo masking */ {
                    delta -= delta shr 4 - LM
                } else
                /* Corresponds to a forward-masking slope of 1.5 dB per 10 ms */ {
                    delta = Inlines.IMIN(0, delta + (N shl EntropyCoder.BITRES shr 5 - LM))
                }
            }
            mbits = Inlines.IMAX(0, Inlines.IMIN(b, (b - delta) / 2))
            sbits = b - mbits
            ctx.remaining_bits -= qalloc

            if (lowband != null) {
                next_lowband2 = lowband_ptr + N
                /* >32-bit split case */
            }

            rebalance = ctx.remaining_bits
            if (mbits >= sbits) {
                cm = quant_partition(
                    ctx, X, X_ptr, N, mbits, B,
                    lowband, lowband_ptr, LM,
                    Inlines.MULT16_16_P15(gain, mid), fill
                )
                rebalance = mbits - (rebalance - ctx.remaining_bits)
                if (rebalance > 3 shl EntropyCoder.BITRES && itheta != 0) {
                    sbits += rebalance - (3 shl EntropyCoder.BITRES)
                }
                cm = cm or (quant_partition(
                    ctx, X, Y, N, sbits, B,
                    lowband, next_lowband2, LM,
                    Inlines.MULT16_16_P15(gain, side), fill shr B
                ) shl (B0 shr 1))
            } else {
                cm = quant_partition(
                    ctx, X, Y, N, sbits, B,
                    lowband, next_lowband2, LM,
                    Inlines.MULT16_16_P15(gain, side), fill shr B
                ) shl (B0 shr 1)
                rebalance = sbits - (rebalance - ctx.remaining_bits)
                if (rebalance > 3 shl EntropyCoder.BITRES && itheta != 16384) {
                    mbits += rebalance - (3 shl EntropyCoder.BITRES)
                }
                cm = cm or quant_partition(
                    ctx, X, X_ptr, N, mbits, B,
                    lowband, lowband_ptr, LM,
                    Inlines.MULT16_16_P15(gain, mid), fill
                )
            }
        } else {
            /* This is the basic no-split case */
            q = Rate.bits2pulses(m!!, i, LM, b)
            curr_bits = Rate.pulses2bits(m, i, LM, q)
            ctx.remaining_bits -= curr_bits

            /* Ensures we can never bust the budget */
            while (ctx.remaining_bits < 0 && q > 0) {
                ctx.remaining_bits += curr_bits
                q--
                curr_bits = Rate.pulses2bits(m, i, LM, q)
                ctx.remaining_bits -= curr_bits
            }

            if (q != 0) {
                val K = Rate.get_pulses(q)

                /* Finally do the actual quantization */
                if (encode != 0) {
                    cm = VQ.alg_quant(X, X_ptr, N, K, spread, B, ec)
                } else {
                    cm = VQ.alg_unquant(X, X_ptr, N, K, spread, B, ec, gain)
                }
            } else {
                /* If there's no pulse, fill the band anyway */
                var j: Int

                if (resynth != 0) {
                    val cm_mask: Int
                    /* B can be as large as 16, so this shift might overflow an int on a
                       16-bit platform; use a long to get defined behavior.*/
                    cm_mask = (1 shl B) - 1
                    fill = fill and cm_mask

                    if (fill == 0) {
                        Arrays.MemSetWithOffset(X, 0, X_ptr, N)
                    } else {
                        if (lowband == null) {
                            /* Noise */
                            j = 0
                            while (j < N) {
                                ctx.seed = celt_lcg_rand(ctx.seed)
                                X[X_ptr + j] = ctx.seed shr 20
                                j++
                            }
                            cm = cm_mask
                        } else {
                            /* Folded spectrum */
                            j = 0
                            while (j < N) {
                                var tmp: Int
                                ctx.seed = celt_lcg_rand(ctx.seed)
                                /* About 48 dB below the "normal" folding level */
                                tmp = (0.5 + 1.0f / 256 * (1 shl 10)).toShort()
                                    .toInt()/*Inlines.QCONST16(1.0f / 256, 10)*/
                                tmp = if (ctx.seed and 0x8000 != 0) tmp else 0 - tmp
                                X[X_ptr + j] = lowband[lowband_ptr + j] + tmp
                                j++
                            }
                            cm = fill
                        }

                        VQ.renormalise_vector(X, X_ptr, N, gain)
                    }
                }
            }
        }

        return cm
    }

    /* This function is responsible for encoding and decoding a band for the mono case. */
    fun quant_band(
        ctx: band_ctx, X: IntArray, X_ptr: Int,
        N: Int, b: Int, B: Int, lowband: IntArray?, lowband_ptr: Int,
        LM: Int, lowband_out: IntArray?, lowband_out_ptr: Int,
        gain: Int, lowband_scratch: IntArray?, lowband_scratch_ptr: Int, fill: Int
    ): Int {
        var B = B
        var lowband = lowband
        var lowband_ptr = lowband_ptr
        var fill = fill
        var N_B = N
        val N_B0: Int
        var B0 = B
        var time_divide = 0
        var recombine = 0
        val longBlocks: Int
        var cm = 0
        val resynth = if (ctx.encode == 0) 1 else 0
        var k: Int
        val encode: Int
        var tf_change: Int

        encode = ctx.encode
        tf_change = ctx.tf_change

        longBlocks = if (B0 == 1) 1 else 0

        N_B = Inlines.celt_udiv(N_B, B)

        /* Special case for one sample */
        if (N == 1) {
            return quant_band_n1(ctx, X, X_ptr, null, 0, b, lowband_out, lowband_out_ptr)
        }

        if (tf_change > 0) {
            recombine = tf_change
        }
        /* Band recombining to increase frequency resolution */

        if (lowband_scratch != null && lowband != null && (recombine != 0 || N_B and 1 == 0 && tf_change < 0 || B0 > 1)) {
            arraycopy(lowband, lowband_ptr, lowband_scratch, lowband_scratch_ptr, N)
            lowband = lowband_scratch
            lowband_ptr = lowband_scratch_ptr
        }

        k = 0
        while (k < recombine) {
            if (encode != 0) {
                haar1(X, X_ptr, N shr k, 1 shl k)
            }
            if (lowband != null) {
                haar1(lowband, lowband_ptr, N shr k, 1 shl k)
            }
            val idx1 = fill and 0xF
            val idx2 = fill shr 4
            if (idx1 < 0) {
                println("e")
            }
            if (idx2 < 0) {
                println("e")
            }
            fill = bit_interleave_table[fill and 0xF] or (bit_interleave_table[fill shr 4] shl 2)
            k++
        }
        B = B shr recombine
        N_B = N_B shl recombine

        /* Increasing the time resolution */
        while (N_B and 1 == 0 && tf_change < 0) {
            if (encode != 0) {
                haar1(X, X_ptr, N_B, B)
            }
            if (lowband != null) {
                haar1(lowband, lowband_ptr, N_B, B)
            }
            fill = fill or (fill shl B)
            B = B shl 1
            N_B = N_B shr 1
            time_divide++
            tf_change++
        }
        B0 = B
        N_B0 = N_B

        /* Reorganize the samples in time order instead of frequency order */
        if (B0 > 1) {
            if (encode != 0) {
                deinterleave_hadamard(X, X_ptr, N_B shr recombine, B0 shl recombine, longBlocks)
            }
            if (lowband != null) {
                deinterleave_hadamard(lowband, lowband_ptr, N_B shr recombine, B0 shl recombine, longBlocks)
            }
        }

        cm = quant_partition(ctx, X, X_ptr, N, b, B, lowband, lowband_ptr, LM, gain, fill)

        /* This code is used by the decoder and by the resynthesis-enabled encoder */
        if (resynth != 0) {
            /* Undo the sample reorganization going from time order to frequency order */
            if (B0 > 1) {
                interleave_hadamard(X, X_ptr, N_B shr recombine, B0 shl recombine, longBlocks)
            }

            /* Undo time-freq changes that we did earlier */
            N_B = N_B0
            B = B0
            k = 0
            while (k < time_divide) {
                B = B shr 1
                N_B = N_B shl 1
                cm = cm or (cm shr B)
                haar1(X, X_ptr, N_B, B)
                k++
            }

            k = 0
            while (k < recombine) {
                cm = bit_deinterleave_table[cm].toInt()
                haar1(X, X_ptr, N shr k, 1 shl k)
                k++
            }
            B = B shl recombine

            /* Scale output for later folding */
            if (lowband_out != null) {
                var j: Int
                val n: Int
                n = Inlines.celt_sqrt(Inlines.SHL32(N, 22))
                j = 0
                while (j < N) {
                    lowband_out[lowband_out_ptr + j] = Inlines.MULT16_16_Q15(n, X!![X_ptr + j])
                    j++
                }
            }

            cm = cm and (1 shl B) - 1
        }
        return cm
    }

    /* This function is responsible for encoding and decoding a band for the stereo case. */
    fun quant_band_stereo(
        ctx: band_ctx, X: IntArray, X_ptr: Int, Y: IntArray, Y_ptr: Int,
        N: Int, b: Int, B: Int, lowband: IntArray?, lowband_ptr: Int,
        LM: Int, lowband_out: IntArray?, lowband_out_ptr: Int,
        lowband_scratch: IntArray?, lowband_scratch_ptr: Int, fill: Int
    ): Int {
        var b = b
        var fill = fill
        var imid = 0
        var iside = 0
        var inv = 0
        var mid = 0
        var side = 0
        var cm = 0
        val resynth = if (ctx.encode == 0) 1 else 0
        var mbits: Int
        var sbits: Int
        val delta: Int
        val itheta: Int
        val qalloc: Int
        val sctx = split_ctx() // porting note: stack var
        val orig_fill: Int
        val encode: Int
        val ec: EntropyCoder? //porting note: pointer

        encode = ctx.encode
        ec = ctx.ec

        /* Special case for one sample */
        if (N == 1) {
            return quant_band_n1(ctx, X, X_ptr, Y, Y_ptr, b, lowband_out, lowband_out_ptr)
        }

        orig_fill = fill

        val boxed_b = BoxedValueInt(b)
        val boxed_fill = BoxedValueInt(fill)
        compute_theta(ctx, sctx, X, X_ptr, Y, Y_ptr, N, boxed_b, B, B, LM, 1, boxed_fill)
        b = boxed_b.Val
        fill = boxed_fill.Val

        inv = sctx.inv
        imid = sctx.imid
        iside = sctx.iside
        delta = sctx.delta
        itheta = sctx.itheta
        qalloc = sctx.qalloc
        mid = imid
        side = iside

        /* This is a special case for N=2 that only works for stereo and takes
           advantage of the fact that mid and side are orthogonal to encode
           the side with just one bit. */
        if (N == 2) {
            val c: Int
            var sign = 0
            val x2: IntArray?
            val y2: IntArray?
            val x2_ptr: Int
            val y2_ptr: Int
            mbits = b
            sbits = 0
            /* Only need one bit for the side. */
            if (itheta != 0 && itheta != 16384) {
                sbits = 1 shl EntropyCoder.BITRES
            }
            mbits -= sbits
            c = if (itheta > 8192) 1 else 0
            ctx.remaining_bits -= qalloc + sbits
            if (c != 0) {
                x2 = Y
                x2_ptr = Y_ptr
                y2 = X
                y2_ptr = X_ptr
            } else {
                x2 = X
                x2_ptr = X_ptr
                y2 = Y
                y2_ptr = Y_ptr
            }

            if (sbits != 0) {
                if (encode != 0) {
                    /* Here we only need to encode a sign for the side. */
                    sign = if (x2!![x2_ptr] * y2!![Y_ptr + 1] - x2[x2_ptr + 1] * y2[Y_ptr] < 0) 1 else 0
                    ec!!.enc_bits(sign.toLong(), 1)
                } else {
                    sign = ec!!.dec_bits(1)
                }
            }
            sign = 1 - 2 * sign
            /* We use orig_fill here because we want to fold the side, but if
               itheta==16384, we'll have cleared the low bits of fill. */
            cm = quant_band(
                ctx, x2, x2_ptr, N, mbits, B, lowband, lowband_ptr,
                LM, lowband_out, lowband_out_ptr, CeltConstants.Q15ONE, lowband_scratch, lowband_scratch_ptr, orig_fill
            )

            /* We don't split N=2 bands, so cm is either 1 or 0 (for a fold-collapse),
               and there's no need to worry about mixing with the other channel. */
            y2[Y_ptr] = (0 - sign) * x2!![x2_ptr + 1]
            y2[Y_ptr + 1] = sign * x2[x2_ptr]
            if (resynth != 0) {
                var tmp: Int
                X[X_ptr] = Inlines.MULT16_16_Q15(mid, X[X_ptr])
                X[X_ptr + 1] = Inlines.MULT16_16_Q15(mid, X[X_ptr + 1])
                Y[Y_ptr] = Inlines.MULT16_16_Q15(side, Y!![Y_ptr])
                Y[Y_ptr + 1] = Inlines.MULT16_16_Q15(side, Y[Y_ptr + 1])
                tmp = X[X_ptr]
                X[X_ptr] = Inlines.SUB16(tmp, Y[Y_ptr])
                Y[Y_ptr] = Inlines.ADD16(tmp, Y[Y_ptr])
                tmp = X[X_ptr + 1]
                X[X_ptr + 1] = Inlines.SUB16(tmp, Y[Y_ptr + 1])
                Y[Y_ptr + 1] = Inlines.ADD16(tmp, Y[Y_ptr + 1])
            }
        } else {
            /* "Normal" split code */
            var rebalance: Int

            mbits = Inlines.IMAX(0, Inlines.IMIN(b, (b - delta) / 2))
            sbits = b - mbits
            ctx.remaining_bits -= qalloc

            rebalance = ctx.remaining_bits
            if (mbits >= sbits) {
                /* In stereo mode, we do not apply a scaling to the mid because we need the normalized
                   mid for folding later. */
                cm = quant_band(
                    ctx, X, X_ptr, N, mbits, B,
                    lowband, lowband_ptr, LM, lowband_out, lowband_out_ptr,
                    CeltConstants.Q15ONE, lowband_scratch, lowband_scratch_ptr, fill
                )
                rebalance = mbits - (rebalance - ctx.remaining_bits)
                if (rebalance > 3 shl EntropyCoder.BITRES && itheta != 0) {
                    sbits += rebalance - (3 shl EntropyCoder.BITRES)
                }

                /* For a stereo split, the high bits of fill are always zero, so no
                   folding will be done to the side. */
                cm = cm or quant_band(
                    ctx, Y, Y_ptr, N, sbits, B, null, 0, LM, null, 0,
                    side, null, 0, fill shr B
                )
            } else {
                /* For a stereo split, the high bits of fill are always zero, so no
                   folding will be done to the side. */
                cm = quant_band(
                    ctx, Y, Y_ptr, N, sbits, B, null, 0, LM, null, 0,
                    side, null, 0, fill shr B
                )
                rebalance = sbits - (rebalance - ctx.remaining_bits)
                if (rebalance > 3 shl EntropyCoder.BITRES && itheta != 16384) {
                    mbits += rebalance - (3 shl EntropyCoder.BITRES)
                }
                /* In stereo mode, we do not apply a scaling to the mid because we need the normalized
                   mid for folding later. */
                cm = cm or quant_band(
                    ctx, X, X_ptr, N, mbits, B,
                    lowband, lowband_ptr, LM, lowband_out, lowband_out_ptr,
                    CeltConstants.Q15ONE, lowband_scratch, lowband_scratch_ptr, fill
                )
            }
        }


        /* This code is used by the decoder and by the resynthesis-enabled encoder */
        if (resynth != 0) {
            if (N != 2) {
                stereo_merge(X, X_ptr, Y, Y_ptr, mid, N)
            }
            if (inv != 0) {
                var j: Int
                j = Y_ptr
                while (j < N + Y_ptr) {
                    Y[j] = (0 - Y!![j]).toShort().toInt()
                    j++
                }
            }
        }

        return cm
    }

    fun quant_all_bands(
		encode: Int, m: CeltMode, start: Int, end: Int,
		X_: IntArray, Y_: IntArray?, collapse_masks: ShortArray,
		bandE: Array<IntArray>?, pulses: IntArray, shortBlocks: Int, spread: Int,
		dual_stereo: Int, intensity: Int, tf_res: IntArray, total_bits: Int,
		balance: Int, ec: EntropyCoder, LM: Int, codedBands: Int,
		seed: BoxedValueInt
    ) {
        var dual_stereo = dual_stereo
        var balance = balance
        var i: Int
        var remaining_bits: Int
        val eBands = m.eBands
        val norm: IntArray
        val norm2: Int
        var lowband_scratch: IntArray?
        val lowband_scratch_ptr: Int
        val B: Int
        val M: Int
        var lowband_offset: Int
        var update_lowband = 1
        val C = if (Y_ != null) 2 else 1
        val norm_offset: Int
        val resynth = if (encode == 0) 1 else 0
        val ctx = band_ctx() // porting note: stack var

        M = 1 shl LM
        B = if (shortBlocks != 0) M else 1
        norm_offset = M * eBands!![start]

        /* No need to allocate norm for the last band because we don't need an
           output in that band. */
        norm = IntArray(C * (M * eBands!![m.nbEBands - 1] - norm_offset))
        norm2 = M * eBands!![m.nbEBands - 1] - norm_offset

        /* We can use the last band as scratch space because we don't need that
           scratch space for the last band. */
        lowband_scratch = X_
        lowband_scratch_ptr = M * eBands!![m.nbEBands - 1]

        lowband_offset = 0
        ctx.bandE = bandE
        ctx.ec = ec
        ctx.encode = encode
        ctx.intensity = intensity
        ctx.m = m
        ctx.seed = seed.Val
        ctx.spread = spread
        i = start
        while (i < end) {
            val tell: Int
            val b: Int
            val N: Int
            val curr_balance: Int
            var effective_lowband = -1
            var X: IntArray
            var Y: IntArray?
            var X_ptr: Int
            var Y_ptr: Int
            Y_ptr = 0
            var tf_change = 0
            var x_cm: Long
            var y_cm: Long
            val last: Int

            ctx.i = i
            last = if (i == end - 1) 1 else 0

            X = X_
            X_ptr = M * eBands!![i]
            if (Y_ != null) {
                Y = Y_
                Y_ptr = M * eBands!![i]
            } else {
                Y = null
            }
            N = M * eBands!![i + 1] - M * eBands!![i]
            tell = ec.tell_frac().toInt()

            /* Compute how many bits we want to allocate to this band */
            if (i != start) {
                balance -= tell
            }
            remaining_bits = total_bits - tell - 1
            ctx.remaining_bits = remaining_bits
            if (i <= codedBands - 1) {
                curr_balance = Inlines.celt_sudiv(balance, Inlines.IMIN(3, codedBands - i))
                b = Inlines.IMAX(0, Inlines.IMIN(16383, Inlines.IMIN(remaining_bits + 1, pulses[i] + curr_balance)))
            } else {
                b = 0
            }

            if (resynth != 0 && M * eBands!![i] - N >= M * eBands!![start] && (update_lowband != 0 || lowband_offset == 0)) {
                lowband_offset = i
            }

            tf_change = tf_res[i]
            ctx.tf_change = tf_change
            if (i >= m.effEBands) {
                X = norm
                X_ptr = 0
                if (Y_ != null) {
                    Y = norm
                    Y_ptr = 0
                }
                lowband_scratch = null
            }
            if (i == end - 1) {
                lowband_scratch = null
            }

            /* Get a conservative estimate of the collapse_mask's for the bands we're
               going to be folding from. */
            if (lowband_offset != 0 && (spread != Spread.SPREAD_AGGRESSIVE || B > 1 || tf_change < 0)) {
                var fold_start: Int
                var fold_end: Int
                var fold_i: Int
                /* This ensures we never repeat spectral content within one band */
                effective_lowband = Inlines.IMAX(0, M * eBands!![lowband_offset] - norm_offset - N)
                fold_start = lowband_offset
                while (M * eBands!![--fold_start] > effective_lowband + norm_offset);
                fold_end = lowband_offset - 1
                while (M * eBands!![++fold_end] < effective_lowband + norm_offset + N);
                y_cm = 0
                x_cm = y_cm
                fold_i = fold_start
                do {
                    x_cm = x_cm or collapse_masks[fold_i * C + 0].toLong()
                    y_cm = y_cm or collapse_masks[fold_i * C + C - 1].toLong()
                } while (++fold_i < fold_end)
            } /* Otherwise, we'll be using the LCG to fold, so all blocks will (almost
               always) be non-zero. */
            else {
                y_cm = ((1 shl B) - 1).toLong()
                x_cm = y_cm
            }

            if (dual_stereo != 0 && i == intensity) {
                var j: Int

                /* Switch off dual stereo to do intensity. */
                dual_stereo = 0
                if (resynth != 0) {
                    j = 0
                    while (j < M * eBands!![i] - norm_offset) {
                        norm[j] = Inlines.HALF32(norm[j] + norm[norm2 + j])
                        j++
                    }
                }
            }
            if (dual_stereo != 0) {
                x_cm = quant_band(
                    ctx,
                    X,
                    X_ptr,
                    N,
                    b / 2,
                    B,
                    if (effective_lowband != -1) norm else null,
                    effective_lowband,
                    LM,
                    if (last != 0) null else norm,
                    M * eBands!![i] - norm_offset,
                    CeltConstants.Q15ONE,
                    lowband_scratch,
                    lowband_scratch_ptr,
                    x_cm.toInt()
                ).toLong()
                y_cm = quant_band(
                    ctx,
                    Y!!,
                    Y_ptr,
                    N,
                    b / 2,
                    B,
                    if (effective_lowband != -1) norm else null,
                    norm2 + effective_lowband,
                    LM,
                    if (last != 0) null else norm,
                    norm2 + (M * eBands!![i] - norm_offset),
                    CeltConstants.Q15ONE,
                    lowband_scratch,
                    lowband_scratch_ptr,
                    y_cm.toInt()
                ).toLong()
            } else {
                if (Y != null) {
                    x_cm = quant_band_stereo(
                        ctx,
                        X,
                        X_ptr,
                        Y,
                        Y_ptr,
                        N,
                        b,
                        B,
                        if (effective_lowband != -1) norm else null,
                        effective_lowband,
                        LM,
                        if (last != 0) null else norm,
                        M * eBands!![i] - norm_offset,
                        lowband_scratch,
                        lowband_scratch_ptr,
                        (x_cm or y_cm).toInt()
                    ).toLong()
                } else {
                    x_cm = quant_band(
                        ctx,
                        X,
                        X_ptr,
                        N,
                        b,
                        B,
                        if (effective_lowband != -1) norm else null,
                        effective_lowband,
                        LM,
                        if (last != 0) null else norm,
                        M * eBands!![i] - norm_offset,
                        CeltConstants.Q15ONE,
                        lowband_scratch,
                        lowband_scratch_ptr,
                        (x_cm or y_cm).toInt()
                    ).toLong() // opt: lots of pointers are created here too
                }
                y_cm = x_cm
            }
            collapse_masks[i * C + 0] = (x_cm and 0xFF).toShort()
            collapse_masks[i * C + C - 1] = (y_cm and 0xFF).toShort()
            balance += pulses[i] + tell

            /* Update the folding position only as long as we have 1 bit/sample depth. */
            update_lowband = if (b > N shl EntropyCoder.BITRES) 1 else 0
            i++
        }

        seed.Val = ctx.seed
    }
}
