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
package com.soywiz.korau.format.org.concentus

internal object MDCT {

    /* Forward MDCT trashes the input array */
    fun clt_mdct_forward(
        l: MDCTLookup, input: IntArray, input_ptr: Int, output: IntArray, output_ptr: Int,
        window: IntArray, overlap: Int, shift: Int, stride: Int
    ) {
        var i: Int
        var N: Int
        val N2: Int
        val N4: Int
        val f: IntArray
        val f2: IntArray
        val st = l.kfft[shift]
        val trig: ShortArray?
        var trig_ptr = 0
        val scale: Int

        val scale_shift = st!!.scale_shift - 1
        scale = st.scale.toInt()

        N = l.n
        trig = l.trig
        i = 0
        while (i < shift) {
            N = N shr 1
            trig_ptr += N
            i++
        }
        N2 = N shr 1
        N4 = N shr 2

        f = IntArray(N2)
        f2 = IntArray(N4 * 2)

        /* Consider the input to be composed of four blocks: [a, b, c, d] */
        /* Window, shuffle, fold */
        run {
            /* Temp pointers to make it really clear to the compiler what we're doing */
            var xp1 = input_ptr + (overlap shr 1)
            var xp2 = input_ptr + N2 - 1 + (overlap shr 1)
            var yp = 0
            var wp1 = overlap shr 1
            var wp2 = (overlap shr 1) - 1
            i = 0
            while (i < overlap + 3 shr 2) {
                /* Real part arranged as -d-cR, Imag part arranged as -b+aR*/
                f[yp++] = Inlines.MULT16_32_Q15(window[wp2], input[xp1 + N2]) +
                        Inlines.MULT16_32_Q15(window[wp1], input[xp2])
                f[yp++] = Inlines.MULT16_32_Q15(window[wp1], input[xp1]) -
                        Inlines.MULT16_32_Q15(window[wp2], input[xp2 - N2])
                xp1 += 2
                xp2 -= 2
                wp1 += 2
                wp2 -= 2
                i++
            }
            wp1 = 0
            wp2 = overlap - 1
            while (i < N4 - (overlap + 3 shr 2)) {
                /* Real part arranged as a-bR, Imag part arranged as -c-dR */
                f[yp++] = input[xp2]
                f[yp++] = input[xp1]
                xp1 += 2
                xp2 -= 2
                i++
            }
            while (i < N4) {
                /* Real part arranged as a-bR, Imag part arranged as -c-dR */
                f[yp++] = Inlines.MULT16_32_Q15(window[wp2], input[xp2]) -
                        Inlines.MULT16_32_Q15(window[wp1], input[xp1 - N2])
                f[yp++] = Inlines.MULT16_32_Q15(window[wp2], input[xp1]) +
                        Inlines.MULT16_32_Q15(window[wp1], input[xp2 + N2])
                xp1 += 2
                xp2 -= 2
                wp1 += 2
                wp2 -= 2
                i++
            }
        }
        /* Pre-rotation */
        run {
            var yp = 0
            val t = trig_ptr
            i = 0
            while (i < N4) {
                val t0: Short
                val t1: Short
                val re: Int
                val im: Int
                val yr: Int
                val yi: Int
                t0 = trig!![t + i]
                t1 = trig[t + N4 + i]
                re = f[yp++]
                im = f[yp++]
                yr = KissFFT.S_MUL(re, t0) - KissFFT.S_MUL(im, t1)
                yi = KissFFT.S_MUL(im, t0) + KissFFT.S_MUL(re, t1)
                f2[2 * st.bitrev!![i]] = Inlines.PSHR32(Inlines.MULT16_32_Q16(scale, yr), scale_shift)
                f2[2 * st.bitrev!![i] + 1] = Inlines.PSHR32(Inlines.MULT16_32_Q16(scale, yi), scale_shift)
                i++
            }
        }

        /* N/4 complex FFT, does not downscale anymore */
        KissFFT.opus_fft_impl(st, f2, 0)

        /* Post-rotate */
        run {
            /* Temp pointers to make it really clear to the compiler what we're doing */
            var fp = 0
            var yp1 = output_ptr
            var yp2 = output_ptr + stride * (N2 - 1)
            val t = trig_ptr
            i = 0
            while (i < N4) {
                val yr: Int
                val yi: Int
                yr = KissFFT.S_MUL(f2[fp + 1], trig!![t + N4 + i]) - KissFFT.S_MUL(f2[fp], trig!![t + i])
                yi = KissFFT.S_MUL(f2[fp], trig!![t + N4 + i]) + KissFFT.S_MUL(f2[fp + 1], trig!![t + i])
                output[yp1] = yr
                output[yp2] = yi
                fp += 2
                yp1 += 2 * stride
                yp2 -= 2 * stride
                i++
            }
        }
    }

    fun clt_mdct_backward(
        l: MDCTLookup, input: IntArray, input_ptr: Int, output: IntArray, output_ptr: Int,
        window: IntArray, overlap: Int, shift: Int, stride: Int
    ) {
        var input_ptr = input_ptr
        var i: Int
        var N: Int
        val N2: Int
        val N4: Int
        var trig = 0
        var xp1: Int
        var xp2: Int
        val yp: Int
        var yp0: Int
        var yp1: Int

        N = l.n
        i = 0
        while (i < shift) {
            N = N shr 1
            trig += N
            i++
        }
        N2 = N shr 1
        N4 = N shr 2

        /* Pre-rotate */
        /* Temp pointers to make it really clear to the compiler what we're doing */
        xp2 = input_ptr + stride * (N2 - 1)
        yp = output_ptr + (overlap shr 1)
        val bitrev = l.kfft[shift]!!.bitrev
        var bitrav_ptr = 0
        i = 0
        while (i < N4) {
            val rev = bitrev!![bitrav_ptr++].toInt()
            val ypr = yp + 2 * rev
            /* We swap real and imag because we use an FFT instead of an IFFT. */
            output[ypr + 1] = KissFFT.S_MUL(input[xp2], l.trig!![trig + i]) +
                    KissFFT.S_MUL(input[input_ptr], l.trig!![trig + N4 + i]) //yr
            output[ypr] = KissFFT.S_MUL(input[input_ptr], l.trig!![trig + i]) -
                    KissFFT.S_MUL(input[xp2], l.trig!![trig + N4 + i]) //yi
            /* Storing the pre-rotation directly in the bitrev order. */
            input_ptr += 2 * stride
            xp2 -= 2 * stride
            i++
        }

        KissFFT.opus_fft_impl(l.kfft!![shift]!!, output, output_ptr + (overlap shr 1))

        /* Post-rotate and de-shuffle from both ends of the buffer at once to make
            it in-place. */
        yp0 = output_ptr + (overlap shr 1)
        yp1 = output_ptr + (overlap shr 1) + N2 - 2
        val t = trig

        /* Loop to (N4+1)>>1 to handle odd N4. When N4 is odd, the
            middle pair will be computed twice. */
        val tN4m1 = t + N4 - 1
        val tN2m1 = t + N2 - 1
        i = 0
        while (i < N4 + 1 shr 1) {
            var re: Int
            var im: Int
            var yr: Int
            var yi: Int
            var t0: Short
            var t1: Short
            /* We swap real and imag because we're using an FFT instead of an IFFT. */
            re = output[yp0 + 1]
            im = output[yp0]
            t0 = l.trig!![t + i]
            t1 = l.trig!![t + N4 + i]
            /* We'd scale up by 2 here, but instead it's done when mixing the windows */
            yr = KissFFT.S_MUL(re, t0) + KissFFT.S_MUL(im, t1)
            yi = KissFFT.S_MUL(re, t1) - KissFFT.S_MUL(im, t0)
            /* We swap real and imag because we're using an FFT instead of an IFFT. */
            re = output[yp1 + 1]
            im = output[yp1]
            output[yp0] = yr
            output[yp1 + 1] = yi
            t0 = l.trig!![tN4m1 - i]
            t1 = l.trig!![tN2m1 - i]
            /* We'd scale up by 2 here, but instead it's done when mixing the windows */
            yr = KissFFT.S_MUL(re, t0) + KissFFT.S_MUL(im, t1)
            yi = KissFFT.S_MUL(re, t1) - KissFFT.S_MUL(im, t0)
            output[yp1] = yr
            output[yp0 + 1] = yi
            yp0 += 2
            yp1 -= 2
            i++
        }

        /* Mirror on both sides for TDAC */
        xp1 = output_ptr + overlap - 1
        yp1 = output_ptr
        var wp1 = 0
        var wp2 = overlap - 1

        i = 0
        while (i < overlap / 2) {
            val x1 = output[xp1]
            val x2 = output[yp1]
            output[yp1++] = Inlines.MULT16_32_Q15(window[wp2], x2) - Inlines.MULT16_32_Q15(window[wp1], x1)
            output[xp1--] = Inlines.MULT16_32_Q15(window[wp1], x2) + Inlines.MULT16_32_Q15(window[wp2], x1)
            wp1++
            wp2--
            i++
        }
    }
}
