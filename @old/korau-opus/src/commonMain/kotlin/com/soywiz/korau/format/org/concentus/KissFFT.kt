/* Copyright (c) 2003-2004, Mark Borgerding
   Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Modified from KISS-FFT by Jean-Marc Valin
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

/* This code is originally from Mark Borgerding's KISS-FFT but has been
   heavily modified to better suit Opus */
package com.soywiz.korau.format.org.concentus

internal object KissFFT {
    //public final int SAMP_MAX = 2147483647;
    //public final int SAMP_MIN = 0 - SAMP_MAX;
    //public final int TWID_MAX = 32767;
    //public final int TRIG_UPSCALE = 1;

    val MAXFACTORS = 8

    fun S_MUL(a: Int, b: Int): Int {
        return Inlines.MULT16_32_Q15(b, a)
    }

    fun S_MUL(a: Int, b: Short): Int {
        return Inlines.MULT16_32_Q15(b, a)
    }

    fun HALF_OF(x: Int): Int {
        return x shr 1
    }

    fun kf_bfly2(Fout: IntArray, fout_ptr: Int, m: Int, N: Int) {
        var fout_ptr = fout_ptr
        var Fout2: Int
        var i: Int
        run {
            val tw: Short
            tw = (0.5 + 0.7071067812f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.7071067812f, 15)*/
            /* We know that m==4 here because the radix-2 is just after a radix-4 */
            Inlines.OpusAssert(m == 4)
            i = 0
            while (i < N) {
                var t_r: Int
                var t_i: Int
                Fout2 = fout_ptr + 8
                t_r = Fout[Fout2 + 0]
                t_i = Fout[Fout2 + 1]
                Fout[Fout2 + 0] = Fout[fout_ptr + 0] - t_r
                Fout[Fout2 + 1] = Fout[fout_ptr + 1] - t_i
                Fout[fout_ptr + 0] += t_r
                Fout[fout_ptr + 1] += t_i

                t_r = S_MUL(Fout[Fout2 + 2] + Fout[Fout2 + 3], tw)
                t_i = S_MUL(Fout[Fout2 + 3] - Fout[Fout2 + 2], tw)
                Fout[Fout2 + 2] = Fout[fout_ptr + 2] - t_r
                Fout[Fout2 + 3] = Fout[fout_ptr + 3] - t_i
                Fout[fout_ptr + 2] += t_r
                Fout[fout_ptr + 3] += t_i

                t_r = Fout[Fout2 + 5]
                t_i = 0 - Fout[Fout2 + 4]
                Fout[Fout2 + 4] = Fout[fout_ptr + 4] - t_r
                Fout[Fout2 + 5] = Fout[fout_ptr + 5] - t_i
                Fout[fout_ptr + 4] += t_r
                Fout[fout_ptr + 5] += t_i

                t_r = S_MUL(Fout[Fout2 + 7] - Fout[Fout2 + 6], tw)
                t_i = S_MUL(0 - Fout[Fout2 + 7] - Fout[Fout2 + 6], tw)
                Fout[Fout2 + 6] = Fout[fout_ptr + 6] - t_r
                Fout[Fout2 + 7] = Fout[fout_ptr + 7] - t_i
                Fout[fout_ptr + 6] += t_r
                Fout[fout_ptr + 7] += t_i

                fout_ptr += 16
                i++
            }
        }
    }

    fun kf_bfly4(
        Fout: IntArray,
        fout_ptr: Int,
        fstride: Int,
        st: FFTState,
        m: Int,
        N: Int,
        mm: Int
    ) {
        var fout_ptr = fout_ptr
        var i: Int

        if (m == 1) {
            /* Degenerate case where all the twiddles are 1. */
            var scratch0: Int
            var scratch1: Int
            var scratch2: Int
            var scratch3: Int
            i = 0
            while (i < N) {
                scratch0 = Fout[fout_ptr + 0] - Fout[fout_ptr + 4]
                scratch1 = Fout[fout_ptr + 1] - Fout[fout_ptr + 5]
                Fout[fout_ptr + 0] += Fout[fout_ptr + 4]
                Fout[fout_ptr + 1] += Fout[fout_ptr + 5]
                scratch2 = Fout[fout_ptr + 2] + Fout[fout_ptr + 6]
                scratch3 = Fout[fout_ptr + 3] + Fout[fout_ptr + 7]
                Fout[fout_ptr + 4] = Fout[fout_ptr + 0] - scratch2
                Fout[fout_ptr + 5] = Fout[fout_ptr + 1] - scratch3
                Fout[fout_ptr + 0] += scratch2
                Fout[fout_ptr + 1] += scratch3
                scratch2 = Fout[fout_ptr + 2] - Fout[fout_ptr + 6]
                scratch3 = Fout[fout_ptr + 3] - Fout[fout_ptr + 7]
                Fout[fout_ptr + 2] = scratch0 + scratch3
                Fout[fout_ptr + 3] = scratch1 - scratch2
                Fout[fout_ptr + 6] = scratch0 - scratch3
                Fout[fout_ptr + 7] = scratch1 + scratch2
                fout_ptr += 8
                i++
            }
        } else {
            var j: Int
            var scratch0: Int
            var scratch1: Int
            var scratch2: Int
            var scratch3: Int
            var scratch4: Int
            var scratch5: Int
            var scratch6: Int
            var scratch7: Int
            var scratch8: Int
            var scratch9: Int
            var scratch10: Int
            var scratch11: Int
            var tw1: Int
            var tw2: Int
            var tw3: Int
            val Fout_beg = fout_ptr
            i = 0
            while (i < N) {
                fout_ptr = Fout_beg + 2 * i * mm
                var m1 = fout_ptr + 2 * m
                var m2 = fout_ptr + 4 * m
                var m3 = fout_ptr + 6 * m
                tw1 = 0
                tw2 = tw1
                tw3 = tw2
                /* m is guaranteed to be a multiple of 4. */
                j = 0
                while (j < m) {
                    scratch0 = S_MUL(Fout[m1], st.twiddles!![tw1]) - S_MUL(Fout[m1 + 1], st.twiddles!![tw1 + 1])
                    scratch1 = S_MUL(Fout[m1], st.twiddles!![tw1 + 1]) + S_MUL(Fout[m1 + 1], st.twiddles!![tw1])
                    scratch2 = S_MUL(Fout[m2], st.twiddles!![tw2]) - S_MUL(Fout[m2 + 1], st.twiddles!![tw2 + 1])
                    scratch3 = S_MUL(Fout[m2], st.twiddles!![tw2 + 1]) + S_MUL(Fout[m2 + 1], st.twiddles!![tw2])
                    scratch4 = S_MUL(Fout[m3], st.twiddles!![tw3]) - S_MUL(Fout[m3 + 1], st.twiddles!![tw3 + 1])
                    scratch5 = S_MUL(Fout[m3], st.twiddles!![tw3 + 1]) + S_MUL(Fout[m3 + 1], st.twiddles!![tw3])
                    scratch10 = Fout[fout_ptr] - scratch2
                    scratch11 = Fout[fout_ptr + 1] - scratch3
                    Fout[fout_ptr] += scratch2
                    Fout[fout_ptr + 1] += scratch3
                    scratch6 = scratch0 + scratch4
                    scratch7 = scratch1 + scratch5
                    scratch8 = scratch0 - scratch4
                    scratch9 = scratch1 - scratch5
                    Fout[m2] = Fout[fout_ptr] - scratch6
                    Fout[m2 + 1] = Fout[fout_ptr + 1] - scratch7
                    tw1 += fstride * 2
                    tw2 += fstride * 4
                    tw3 += fstride * 6
                    Fout[fout_ptr] += scratch6
                    Fout[fout_ptr + 1] += scratch7
                    Fout[m1] = scratch10 + scratch9
                    Fout[m1 + 1] = scratch11 - scratch8
                    Fout[m3] = scratch10 - scratch9
                    Fout[m3 + 1] = scratch11 + scratch8
                    fout_ptr += 2
                    m1 += 2
                    m2 += 2
                    m3 += 2
                    j++
                }
                i++
            }
        }
    }

    fun kf_bfly3(
        Fout: IntArray,
        fout_ptr: Int,
        fstride: Int,
        st: FFTState,
        m: Int,
        N: Int,
        mm: Int
    ) {
        var fout_ptr = fout_ptr
        var i: Int
        var k: Int
        val m1 = 2 * m
        val m2 = 4 * m
        var tw1: Int
        var tw2: Int
        var scratch0: Int
        var scratch1: Int
        var scratch2: Int
        var scratch3: Int
        var scratch4: Int
        var scratch5: Int
        var scratch6: Int
        var scratch7: Int

        val Fout_beg = fout_ptr

        i = 0
        while (i < N) {
            fout_ptr = Fout_beg + 2 * i * mm
            tw2 = 0
            tw1 = tw2
            /* For non-custom modes, m is guaranteed to be a multiple of 4. */
            k = m
            do {
                scratch2 = S_MUL(Fout[fout_ptr + m1], st.twiddles!![tw1]) -
                        S_MUL(Fout[fout_ptr + m1 + 1], st.twiddles!![tw1 + 1])
                scratch3 = S_MUL(Fout[fout_ptr + m1], st.twiddles!![tw1 + 1]) +
                        S_MUL(Fout[fout_ptr + m1 + 1], st.twiddles!![tw1])
                scratch4 = S_MUL(Fout[fout_ptr + m2], st.twiddles!![tw2]) -
                        S_MUL(Fout[fout_ptr + m2 + 1], st.twiddles!![tw2 + 1])
                scratch5 = S_MUL(Fout[fout_ptr + m2], st.twiddles!![tw2 + 1]) +
                        S_MUL(Fout[fout_ptr + m2 + 1], st.twiddles!![tw2])

                scratch6 = scratch2 + scratch4
                scratch7 = scratch3 + scratch5
                scratch0 = scratch2 - scratch4
                scratch1 = scratch3 - scratch5

                tw1 += fstride * 2
                tw2 += fstride * 4

                Fout[fout_ptr + m1] = Fout[fout_ptr + 0] - HALF_OF(scratch6)
                Fout[fout_ptr + m1 + 1] = Fout[fout_ptr + 1] - HALF_OF(scratch7)

                scratch0 = S_MUL(scratch0, -28378)
                scratch1 = S_MUL(scratch1, -28378)

                Fout[fout_ptr + 0] += scratch6
                Fout[fout_ptr + 1] += scratch7

                Fout[fout_ptr + m2] = Fout[fout_ptr + m1] + scratch1
                Fout[fout_ptr + m2 + 1] = Fout[fout_ptr + m1 + 1] - scratch0

                Fout[fout_ptr + m1] -= scratch1
                Fout[fout_ptr + m1 + 1] += scratch0

                fout_ptr += 2
            } while (--k != 0)
            i++
        }
    }

    fun kf_bfly5(
        Fout: IntArray,
        fout_ptr: Int,
        fstride: Int,
        st: FFTState,
        m: Int,
        N: Int,
        mm: Int
    ) {
        var fout_ptr = fout_ptr
        var Fout0: Int
        var Fout1: Int
        var Fout2: Int
        var Fout3: Int
        var Fout4: Int
        var i: Int
        var u: Int
        var scratch0: Int
        var scratch1: Int
        var scratch2: Int
        var scratch3: Int
        var scratch4: Int
        var scratch5: Int
        var scratch6: Int
        var scratch7: Int
        var scratch8: Int
        var scratch9: Int
        var scratch10: Int
        var scratch11: Int
        var scratch12: Int
        var scratch13: Int
        var scratch14: Int
        var scratch15: Int
        var scratch16: Int
        var scratch17: Int
        var scratch18: Int
        var scratch19: Int
        var scratch20: Int
        var scratch21: Int
        var scratch22: Int
        var scratch23: Int
        var scratch24: Int
        var scratch25: Int

        val Fout_beg = fout_ptr

        val ya_r: Short = 10126
        val ya_i: Short = -31164
        val yb_r: Short = -26510
        val yb_i: Short = -19261
        var tw1: Int
        var tw2: Int
        var tw3: Int
        var tw4: Int

        i = 0
        while (i < N) {
            tw4 = 0
            tw3 = tw4
            tw2 = tw3
            tw1 = tw2
            fout_ptr = Fout_beg + 2 * i * mm
            Fout0 = fout_ptr
            Fout1 = fout_ptr + 2 * m
            Fout2 = fout_ptr + 4 * m
            Fout3 = fout_ptr + 6 * m
            Fout4 = fout_ptr + 8 * m

            /* For non-custom modes, m is guaranteed to be a multiple of 4. */
            u = 0
            while (u < m) {
                scratch0 = Fout[Fout0 + 0]
                scratch1 = Fout[Fout0 + 1]

                scratch2 = S_MUL(Fout[Fout1 + 0], st.twiddles!![tw1]) - S_MUL(Fout[Fout1 + 1], st.twiddles!![tw1 + 1])
                scratch3 = S_MUL(Fout[Fout1 + 0], st.twiddles!![tw1 + 1]) + S_MUL(Fout[Fout1 + 1], st.twiddles!![tw1])
                scratch4 = S_MUL(Fout[Fout2 + 0], st.twiddles!![tw2]) - S_MUL(Fout[Fout2 + 1], st.twiddles!![tw2 + 1])
                scratch5 = S_MUL(Fout[Fout2 + 0], st.twiddles!![tw2 + 1]) + S_MUL(Fout[Fout2 + 1], st.twiddles!![tw2])
                scratch6 = S_MUL(Fout[Fout3 + 0], st.twiddles!![tw3]) - S_MUL(Fout[Fout3 + 1], st.twiddles!![tw3 + 1])
                scratch7 = S_MUL(Fout[Fout3 + 0], st.twiddles!![tw3 + 1]) + S_MUL(Fout[Fout3 + 1], st.twiddles!![tw3])
                scratch8 = S_MUL(Fout[Fout4 + 0], st.twiddles!![tw4]) - S_MUL(Fout[Fout4 + 1], st.twiddles!![tw4 + 1])
                scratch9 = S_MUL(Fout[Fout4 + 0], st.twiddles!![tw4 + 1]) + S_MUL(Fout[Fout4 + 1], st.twiddles!![tw4])

                tw1 += 2 * fstride
                tw2 += 4 * fstride
                tw3 += 6 * fstride
                tw4 += 8 * fstride

                scratch14 = scratch2 + scratch8
                scratch15 = scratch3 + scratch9
                scratch20 = scratch2 - scratch8
                scratch21 = scratch3 - scratch9
                scratch16 = scratch4 + scratch6
                scratch17 = scratch5 + scratch7
                scratch18 = scratch4 - scratch6
                scratch19 = scratch5 - scratch7

                Fout[Fout0 + 0] += scratch14 + scratch16
                Fout[Fout0 + 1] += scratch15 + scratch17

                scratch10 = scratch0 + S_MUL(scratch14, ya_r) + S_MUL(scratch16, yb_r)
                scratch11 = scratch1 + S_MUL(scratch15, ya_r) + S_MUL(scratch17, yb_r)

                scratch12 = S_MUL(scratch21, ya_i) + S_MUL(scratch19, yb_i)
                scratch13 = 0 - S_MUL(scratch20, ya_i) - S_MUL(scratch18, yb_i)

                Fout[Fout1 + 0] = scratch10 - scratch12
                Fout[Fout1 + 1] = scratch11 - scratch13
                Fout[Fout4 + 0] = scratch10 + scratch12
                Fout[Fout4 + 1] = scratch11 + scratch13

                scratch22 = scratch0 + S_MUL(scratch14, yb_r) + S_MUL(scratch16, ya_r)
                scratch23 = scratch1 + S_MUL(scratch15, yb_r) + S_MUL(scratch17, ya_r)
                scratch24 = 0 - S_MUL(scratch21, yb_i) + S_MUL(scratch19, ya_i)
                scratch25 = S_MUL(scratch20, yb_i) - S_MUL(scratch18, ya_i)

                Fout[Fout2 + 0] = scratch22 + scratch24
                Fout[Fout2 + 1] = scratch23 + scratch25
                Fout[Fout3 + 0] = scratch22 - scratch24
                Fout[Fout3 + 1] = scratch23 - scratch25

                Fout0 += 2
                Fout1 += 2
                Fout2 += 2
                Fout3 += 2
                Fout4 += 2
                ++u
            }
            i++
        }
    }

    fun opus_fft_impl(st: FFTState, fout: IntArray, fout_ptr: Int) {
        var m2: Int
        var m: Int
        var p: Int
        var L: Int
        val fstride = IntArray(MAXFACTORS)
        var i: Int
        val shift: Int

        /* st.shift can be -1 */
        shift = if (st.shift > 0) st.shift else 0

        fstride[0] = 1
        L = 0
        do {
            p = st.factors[2 * L].toInt()
            m = st.factors[2 * L + 1].toInt()
            fstride[L + 1] = fstride[L] * p
            L++
        } while (m != 1)

        m = st.factors[2 * L - 1].toInt()
        i = L - 1
        while (i >= 0) {
            if (i != 0) {
                m2 = st.factors[2 * i - 1].toInt()
            } else {
                m2 = 1
            }
            when (st.factors[2 * i].toInt()) {
                2 -> kf_bfly2(fout, fout_ptr, m, fstride[i])
                4 -> kf_bfly4(fout, fout_ptr, fstride[i] shl shift, st, m, fstride[i], m2)
                3 -> kf_bfly3(fout, fout_ptr, fstride[i] shl shift, st, m, fstride[i], m2)
                5 -> kf_bfly5(fout, fout_ptr, fstride[i] shl shift, st, m, fstride[i], m2)
            }
            m = m2
            i--
        }
    }

    fun opus_fft(st: FFTState, fin: IntArray, fout: IntArray) {
        var i: Int
        /* Allows us to scale with MULT16_32_Q16() */
        val scale_shift = st.scale_shift - 1
        val scale = st.scale

        Inlines.OpusAssert(fin != fout, "In-place FFT not supported")

        /* Bit-reverse the input */
        i = 0
        while (i < st.nfft) {
            fout[2 * st.bitrev!![i]] = Inlines.SHR32(Inlines.MULT16_32_Q16(scale, fin[2 * i]), scale_shift)
            fout[2 * st.bitrev!![i] + 1] = Inlines.SHR32(Inlines.MULT16_32_Q16(scale, fin[2 * i + 1]), scale_shift)
            i++
        }

        opus_fft_impl(st, fout, 0)
    }

    //static void opus_ifft(FFTState st, Pointer<int> fin, Pointer<int> fout)
    //{
    //    int i;
    //    Inlines.OpusAssert(fin != fout, "In-place iFFT not supported");
    //    /* Bit-reverse the input */
    //    for (i = 0; i < st.nfft * 2; i++)
    //    {
    //        fout[st.bitrev[i]] = fin[i];
    //    }
    //    for (i = 1; i < st.nfft * 2; i += 2)
    //    {
    //        fout[i] = -fout[i];
    //    }
    //    opus_fft_impl(st, fout.Data, fout.Offset);
    //    for (i = 1; i < st.nfft * 2; i += 2)
    //        fout[i] = -fout[i];
    //}
}
