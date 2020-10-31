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

internal object Pitch {

    private val second_check = intArrayOf(0, 0, 3, 2, 3, 2, 5, 2, 3, 2, 3, 2, 5, 2, 3, 2)

    fun find_best_pitch(
        xcorr: IntArray, y: IntArray, len: Int,
        max_pitch: Int, best_pitch: IntArray,
        yshift: Int, maxcorr: Int
    ) {
        var i: Int
        var j: Int
        var Syy = 1
        var best_num_0: Int
        var best_num_1: Int
        var best_den_0: Int
        var best_den_1: Int
        val xshift = Inlines.celt_ilog2(maxcorr) - 14

        best_num_0 = -1
        best_num_1 = -1
        best_den_0 = 0
        best_den_1 = 0
        best_pitch[0] = 0
        best_pitch[1] = 1
        j = 0
        while (j < len) {
            Syy = Inlines.ADD32(Syy, Inlines.SHR32(Inlines.MULT16_16(y[j], y[j]), yshift))
            j++
        }
        i = 0
        while (i < max_pitch) {
            if (xcorr[i] > 0) {
                val num: Int
                val xcorr16: Int
                xcorr16 = Inlines.EXTRACT16(Inlines.VSHR32(xcorr[i], xshift)).toInt()
                num = Inlines.MULT16_16_Q15(xcorr16, xcorr16)
                if (Inlines.MULT16_32_Q15(num, best_den_1) > Inlines.MULT16_32_Q15(best_num_1, Syy)) {
                    if (Inlines.MULT16_32_Q15(num, best_den_0) > Inlines.MULT16_32_Q15(best_num_0, Syy)) {
                        best_num_1 = best_num_0
                        best_den_1 = best_den_0
                        best_pitch[1] = best_pitch[0]
                        best_num_0 = num
                        best_den_0 = Syy
                        best_pitch[0] = i
                    } else {
                        best_num_1 = num
                        best_den_1 = Syy
                        best_pitch[1] = i
                    }
                }
            }

            Syy += Inlines.SHR32(
                Inlines.MULT16_16(y[i + len], y[i + len]),
                yshift
            ) - Inlines.SHR32(Inlines.MULT16_16(y[i], y[i]), yshift)
            Syy = Inlines.MAX32(1, Syy)
            i++
        }
    }

    fun celt_fir5(
        x: IntArray,
        num: IntArray,
        y: IntArray,
        N: Int,
        mem: IntArray
    ) {
        var i: Int
        val num0: Int
        val num1: Int
        val num2: Int
        val num3: Int
        val num4: Int
        var mem0: Int
        var mem1: Int
        var mem2: Int
        var mem3: Int
        var mem4: Int
        num0 = num[0]
        num1 = num[1]
        num2 = num[2]
        num3 = num[3]
        num4 = num[4]
        mem0 = mem[0]
        mem1 = mem[1]
        mem2 = mem[2]
        mem3 = mem[3]
        mem4 = mem[4]
        i = 0
        while (i < N) {
            var sum = Inlines.SHL32(Inlines.EXTEND32(x[i]), CeltConstants.SIG_SHIFT)
            sum = Inlines.MAC16_16(sum, num0, mem0)
            sum = Inlines.MAC16_16(sum, num1, mem1)
            sum = Inlines.MAC16_16(sum, num2, mem2)
            sum = Inlines.MAC16_16(sum, num3, mem3)
            sum = Inlines.MAC16_16(sum, num4, mem4)
            mem4 = mem3
            mem3 = mem2
            mem2 = mem1
            mem1 = mem0
            mem0 = x[i]
            y[i] = Inlines.ROUND16(sum, CeltConstants.SIG_SHIFT)
            i++
        }
        mem[0] = mem0
        mem[1] = mem1
        mem[2] = mem2
        mem[3] = mem3
        mem[4] = mem4
    }

    fun pitch_downsample(x: Array<IntArray>, x_lp: IntArray, len: Int, C: Int) {
        var i: Int
        val ac = IntArray(5)
        var tmp = CeltConstants.Q15ONE
        val lpc = IntArray(4)
        val mem = intArrayOf(0, 0, 0, 0, 0)
        val lpc2 = IntArray(5)
        val c1 = (0.5 + 0.8f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(0.8f, 15)*/

        var shift: Int
        var maxabs = Inlines.celt_maxabs32(x[0], 0, len)
        if (C == 2) {
            val maxabs_1 = Inlines.celt_maxabs32(x[1], 0, len)
            maxabs = Inlines.MAX32(maxabs, maxabs_1)
        }
        if (maxabs < 1) {
            maxabs = 1
        }
        shift = Inlines.celt_ilog2(maxabs) - 10
        if (shift < 0) {
            shift = 0
        }
        if (C == 2) {
            shift++
        }

        val halflen = len shr 1 // cached for performance
        i = 1
        while (i < halflen) {
            x_lp[i] = Inlines.SHR32(
                Inlines.HALF32(Inlines.HALF32(x[0][2 * i - 1] + x[0][2 * i + 1]) + x[0][2 * i]),
                shift
            )
            i++
        }

        x_lp[0] = Inlines.SHR32(Inlines.HALF32(Inlines.HALF32(x[0][1]) + x[0][0]), shift)

        if (C == 2) {
            i = 1
            while (i < halflen) {
                x_lp[i] += Inlines.SHR32(
                    Inlines.HALF32(Inlines.HALF32(x[1][2 * i - 1] + x[1][2 * i + 1]) + x[1][2 * i]),
                    shift
                )
                i++
            }
            x_lp[0] += Inlines.SHR32(Inlines.HALF32(Inlines.HALF32(x[1][1]) + x[1][0]), shift)
        }

        Autocorrelation._celt_autocorr(x_lp, ac, null, 0, 4, halflen)

        /* Noise floor -40 dB */
        ac[0] += Inlines.SHR32(ac[0], 13)
        /* Lag windowing */
        i = 1
        while (i <= 4) {
            /*ac[i] *= exp(-.5*(2*M_PI*.002*i)*(2*M_PI*.002*i));*/
            ac[i] -= Inlines.MULT16_32_Q15(2 * i * i, ac[i])
            i++
        }

        CeltLPC.celt_lpc(lpc, ac, 4)
        i = 0
        while (i < 4) {
            tmp = Inlines.MULT16_16_Q15((0.5 + .9f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.9f, 15)*/, tmp)
            lpc[i] = Inlines.MULT16_16_Q15(lpc[i], tmp)
            i++
        }
        /* Add a zero */
        lpc2[0] = lpc[0] + (0.5 + 0.8f * (1 shl CeltConstants.SIG_SHIFT)).toShort()
        lpc2[1] = lpc[1] + Inlines.MULT16_16_Q15(c1, lpc[0])
        lpc2[2] = lpc[2] + Inlines.MULT16_16_Q15(c1, lpc[1])
        lpc2[3] = lpc[3] + Inlines.MULT16_16_Q15(c1, lpc[2])
        lpc2[4] = Inlines.MULT16_16_Q15(c1, lpc[3])

        celt_fir5(x_lp, lpc2, x_lp, halflen, mem)
    }

    // Fixme: remove pointers and optimize
    fun pitch_search(
        x_lp: IntArray, x_lp_ptr: Int, y: IntArray,
        len: Int, max_pitch: Int, pitch: BoxedValueInt
    ) {
        var i: Int
        var j: Int
        val lag: Int
        val best_pitch = intArrayOf(0, 0)
        var maxcorr: Int
        val xmax: Int
        val ymax: Int
        var shift = 0
        val offset: Int

        Inlines.OpusAssert(len > 0)
        Inlines.OpusAssert(max_pitch > 0)
        lag = len + max_pitch

        val x_lp4 = IntArray(len shr 2)
        val y_lp4 = IntArray(lag shr 2)
        val xcorr = IntArray(max_pitch shr 1)

        /* Downsample by 2 again */
        j = 0
        while (j < len shr 2) {
            x_lp4[j] = x_lp[x_lp_ptr + 2 * j]
            j++
        }
        j = 0
        while (j < lag shr 2) {
            y_lp4[j] = y[2 * j]
            j++
        }

        xmax = Inlines.celt_maxabs32(x_lp4, 0, len shr 2)
        ymax = Inlines.celt_maxabs32(y_lp4, 0, lag shr 2)
        shift = Inlines.celt_ilog2(Inlines.MAX32(1, Inlines.MAX32(xmax, ymax))) - 11
        if (shift > 0) {
            j = 0
            while (j < len shr 2) {
                x_lp4[j] = Inlines.SHR16(x_lp4[j], shift)
                j++
            }
            j = 0
            while (j < lag shr 2) {
                y_lp4[j] = Inlines.SHR16(y_lp4[j], shift)
                j++
            }
            /* Use double the shift for a MAC */
            shift *= 2
        } else {
            shift = 0
        }

        /* Coarse search with 4x decimation */
        maxcorr = CeltPitchXCorr.pitch_xcorr(x_lp4, y_lp4, xcorr, len shr 2, max_pitch shr 2)

        find_best_pitch(xcorr, y_lp4, len shr 2, max_pitch shr 2, best_pitch, 0, maxcorr)

        /* Finer search with 2x decimation */
        maxcorr = 1
        i = 0
        while (i < max_pitch shr 1) {
            var sum: Int
            xcorr[i] = 0
            if (Inlines.abs(i - 2 * best_pitch[0]) > 2 && Inlines.abs(i - 2 * best_pitch[1]) > 2) {
                i++
                continue
            }
            sum = 0
            j = 0
            while (j < len shr 1) {
                sum += Inlines.SHR32(Inlines.MULT16_16(x_lp[x_lp_ptr + j], y[i + j]), shift)
                j++
            }

            xcorr[i] = Inlines.MAX32(-1, sum)
            maxcorr = Inlines.MAX32(maxcorr, sum)
            i++
        }
        find_best_pitch(xcorr, y, len shr 1, max_pitch shr 1, best_pitch, shift + 1, maxcorr)

        /* Refine by pseudo-interpolation */
        if (best_pitch[0] > 0 && best_pitch[0] < (max_pitch shr 1) - 1) {
            val a: Int
            val b: Int
            val c: Int
            a = xcorr[best_pitch[0] - 1]
            b = xcorr[best_pitch[0]]
            c = xcorr[best_pitch[0] + 1]
            if (c - a > Inlines.MULT16_32_Q15((0.5 + .7f * (1 shl 15)).toShort()/*Inlines.QCONST16(.7f, 15)*/, b - a)) {
                offset = 1
            } else if (a - c > Inlines.MULT16_32_Q15(
                    (0.5 + .7f * (1 shl 15)).toShort()/*Inlines.QCONST16(.7f, 15)*/,
                    b - c
                )
            ) {
                offset = -1
            } else {
                offset = 0
            }
        } else {
            offset = 0
        }

        pitch.Val = 2 * best_pitch[0] - offset
    }

    fun remove_doubling(
        x: IntArray, maxperiod: Int, minperiod: Int,
        N: Int, T0_: BoxedValueInt, prev_period: Int, prev_gain: Int
    ): Int {
        var maxperiod = maxperiod
        var minperiod = minperiod
        var N = N
        var prev_period = prev_period
        var k: Int
        var i: Int
        var T: Int
        val T0: Int
        var g: Int = 0
        var g0: Int = 0
        var pg: Int
        var yy: Int
        val xx: Int
        var xy: Int
        var xy2: Int
        val xcorr = IntArray(3)
        var best_xy: Int
        var best_yy: Int
        val offset: Int
        val minperiod0 = minperiod
        maxperiod /= 2
        minperiod /= 2
        T0_.Val /= 2
        prev_period /= 2
        N /= 2
        val x_ptr = maxperiod
        if (T0_.Val >= maxperiod) {
            T0_.Val = maxperiod - 1
        }

        T0 = T0_.Val
        T = T0
        val yy_lookup = IntArray(maxperiod + 1)
        val boxed_xx = BoxedValueInt(0)
        val boxed_xy = BoxedValueInt(0)
        val boxed_xy2 = BoxedValueInt(0)
        Kernels.dual_inner_prod(x, x_ptr, x, x_ptr, x, x_ptr - T0, N, boxed_xx, boxed_xy)
        xx = boxed_xx.Val
        xy = boxed_xy.Val
        yy_lookup[0] = xx
        yy = xx
        i = 1
        while (i <= maxperiod) {
            val xi = x_ptr - i
            yy = yy + Inlines.MULT16_16(x[xi], x[xi]) - Inlines.MULT16_16(x[xi + N], x[xi + N])
            yy_lookup[i] = Inlines.MAX32(0, yy)
            i++
        }
        yy = yy_lookup[T0]
        best_xy = xy
        best_yy = yy

        run {
            val x2y2: Int
            val sh: Int
            val t: Int
            x2y2 = 1 + Inlines.HALF32(Inlines.MULT32_32_Q31(xx, yy))
            sh = Inlines.celt_ilog2(x2y2) shr 1
            t = Inlines.VSHR32(x2y2, 2 * (sh - 7))
            g = Inlines.VSHR32(Inlines.MULT16_32_Q15(Inlines.celt_rsqrt_norm(t), xy), sh + 1)
            g0 = g
        }

        /* Look for any pitch at T/k */
        k = 2
        while (k <= 15) {
            val T1: Int
            val T1b: Int
            var g1: Int = 0
            var cont = 0
            var thresh: Int
            T1 = Inlines.celt_udiv(2 * T0 + k, 2 * k)
            if (T1 < minperiod) {
                break
            }

            /* Look for another strong correlation at T1b */
            if (k == 2) {
                if (T1 + T0 > maxperiod) {
                    T1b = T0
                } else {
                    T1b = T0 + T1
                }
            } else {
                T1b = Inlines.celt_udiv(2 * second_check[k] * T0 + k, 2 * k)
            }

            Kernels.dual_inner_prod(x, x_ptr, x, x_ptr - T1, x, x_ptr - T1b, N, boxed_xy, boxed_xy2)
            xy = boxed_xy.Val
            xy2 = boxed_xy2.Val

            xy += xy2
            yy = yy_lookup[T1] + yy_lookup[T1b]

            run {
                val x2y2: Int
                val sh: Int
                val t: Int
                x2y2 = 1 + Inlines.MULT32_32_Q31(xx, yy)
                sh = Inlines.celt_ilog2(x2y2) shr 1
                t = Inlines.VSHR32(x2y2, 2 * (sh - 7))
                g1 = Inlines.VSHR32(Inlines.MULT16_32_Q15(Inlines.celt_rsqrt_norm(t), xy), sh + 1)
            }

            if (Inlines.abs(T1 - prev_period) <= 1) {
                cont = prev_gain
            } else if (Inlines.abs(T1 - prev_period) <= 2 && 5 * k * k < T0) {
                cont = Inlines.HALF16(prev_gain)
            } else {
                cont = 0
            }
            thresh = Inlines.MAX16(
                (0.5 + .3f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.3f, 15)*/,
                Inlines.MULT16_16_Q15(
                    (0.5 + .7f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.7f, 15)*/,
                    g0
                ) - cont
            )

            /* Bias against very high pitch (very short period) to avoid false-positives
               due to short-term correlation */
            if (T1 < 3 * minperiod) {
                thresh = Inlines.MAX16(
                    (0.5 + .4f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.4f, 15)*/,
                    Inlines.MULT16_16_Q15(
                        (0.5 + .85f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.85f, 15)*/,
                        g0
                    ) - cont
                )
            } else if (T1 < 2 * minperiod) {
                thresh = Inlines.MAX16(
                    (0.5 + .5f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.5f, 15)*/,
                    Inlines.MULT16_16_Q15(
                        (0.5 + .9f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.9f, 15)*/,
                        g0
                    ) - cont
                )
            }
            if (g1 > thresh) {
                best_xy = xy
                best_yy = yy
                T = T1
                g = g1
            }
            k++
        }

        best_xy = Inlines.MAX32(0, best_xy)
        if (best_yy <= best_xy) {
            pg = CeltConstants.Q15ONE
        } else {
            pg = Inlines.SHR32(Inlines.frac_div32(best_xy, best_yy + 1), 16)
        }

        k = 0
        while (k < 3) {
            xcorr[k] = Kernels.celt_inner_prod(x, x_ptr, x, x_ptr - (T + k - 1), N)
            k++
        }

        if (xcorr[2] - xcorr[0] > Inlines.MULT16_32_Q15(
                (0.5 + .7f * (1 shl 15)).toShort()/*Inlines.QCONST16(.7f, 15)*/,
                xcorr[1] - xcorr[0]
            )
        ) {
            offset = 1
        } else if (xcorr[0] - xcorr[2] > Inlines.MULT16_32_Q15(
                (0.5 + .7f * (1 shl 15)).toShort()/*Inlines.QCONST16(.7f, 15)*/,
                xcorr[1] - xcorr[2]
            )
        ) {
            offset = -1
        } else {
            offset = 0
        }

        if (pg > g) {
            pg = g
        }

        T0_.Val = 2 * T + offset

        if (T0_.Val < minperiod0) {
            T0_.Val = minperiod0
        }

        return pg
    }
}
