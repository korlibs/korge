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

internal object CeltLPC {

    fun celt_lpc(
        _lpc: IntArray, /* out: [0...p-1] LPC coefficients      */
        ac: IntArray, /* in:  [0...p] autocorrelation values  */
        p: Int
    ) {
        var i: Int
        var j: Int
        var r: Int
        var error = ac[0]
        val lpc = IntArray(p)

        //Arrays.MemSet(lpc, 0, p); strictly, this is not necessary since the runtime zeroes memory for us
        if (ac[0] != 0) {
            i = 0
            while (i < p) {
                /* Sum up this iteration's reflection coefficient */
                var rr = 0
                j = 0
                while (j < i) {
                    rr += Inlines.MULT32_32_Q31(lpc[j], ac[i - j])
                    j++
                }
                rr += Inlines.SHR32(ac[i + 1], 3)
                r = 0 - Inlines.frac_div32(Inlines.SHL32(rr, 3), error)
                /*  Update LPC coefficients and total error */
                lpc[i] = Inlines.SHR32(r, 3)

                j = 0
                while (j < i + 1 shr 1) {
                    val tmp1: Int
                    val tmp2: Int
                    tmp1 = lpc[j]
                    tmp2 = lpc[i - 1 - j]
                    lpc[j] = tmp1 + Inlines.MULT32_32_Q31(r, tmp2)
                    lpc[i - 1 - j] = tmp2 + Inlines.MULT32_32_Q31(r, tmp1)
                    j++
                }

                error = error - Inlines.MULT32_32_Q31(Inlines.MULT32_32_Q31(r, r), error)

                /* Bail out once we get 30 dB gain */
                if (error < Inlines.SHR32(ac[0], 10)) {
                    break
                }
                i++
            }
        }

        i = 0
        while (i < p) {
            _lpc[i] = Inlines.ROUND16(lpc[i], 16)
            i++
        }
    }

    fun celt_iir(
        _x: IntArray,
        _x_ptr: Int,
        den: IntArray,
        _y: IntArray,
        _y_ptr: Int,
        N: Int,
        ord: Int,
        mem: IntArray
    ) {
        var i: Int
        var j: Int
        val rden = IntArray(ord)
        val y = IntArray(N + ord)
        Inlines.OpusAssert(ord and 3 == 0)

        val _sum0 = BoxedValueInt(0)
        val _sum1 = BoxedValueInt(0)
        val _sum2 = BoxedValueInt(0)
        val _sum3 = BoxedValueInt(0)
        var sum0: Int
        var sum1: Int
        var sum2: Int
        var sum3: Int

        i = 0
        while (i < ord) {
            rden[i] = den[ord - i - 1]
            i++
        }
        i = 0
        while (i < ord) {
            y[i] = 0 - mem[ord - i - 1]
            i++
        }
        while (i < N + ord) {
            y[i] = 0
            i++
        }
        i = 0
        while (i < N - 3) {
            /* Unroll by 4 as if it were an FIR filter */
            _sum0.Val = _x[_x_ptr + i]
            _sum1.Val = _x[_x_ptr + i + 1]
            _sum2.Val = _x[_x_ptr + i + 2]
            _sum3.Val = _x[_x_ptr + i + 3]
            Kernels.xcorr_kernel(rden, y, i, _sum0, _sum1, _sum2, _sum3, ord)
            sum0 = _sum0.Val
            sum1 = _sum1.Val
            sum2 = _sum2.Val
            sum3 = _sum3.Val

            /* Patch up the result to compensate for the fact that this is an IIR */
            y[i + ord] = 0 - Inlines.ROUND16(sum0, CeltConstants.SIG_SHIFT)
            _y[_y_ptr + i] = sum0
            sum1 = Inlines.MAC16_16(sum1, y[i + ord], den[0])
            y[i + ord + 1] = 0 - Inlines.ROUND16(sum1, CeltConstants.SIG_SHIFT)
            _y[_y_ptr + i + 1] = sum1
            sum2 = Inlines.MAC16_16(sum2, y[i + ord + 1], den[0])
            sum2 = Inlines.MAC16_16(sum2, y[i + ord], den[1])
            y[i + ord + 2] = 0 - Inlines.ROUND16(sum2, CeltConstants.SIG_SHIFT)
            _y[_y_ptr + i + 2] = sum2

            sum3 = Inlines.MAC16_16(sum3, y[i + ord + 2], den[0])
            sum3 = Inlines.MAC16_16(sum3, y[i + ord + 1], den[1])
            sum3 = Inlines.MAC16_16(sum3, y[i + ord], den[2])
            y[i + ord + 3] = 0 - Inlines.ROUND16(sum3, CeltConstants.SIG_SHIFT)
            _y[_y_ptr + i + 3] = sum3
            i += 4
        }
        while (i < N) {
            var sum = _x[_x_ptr + i]
            j = 0
            while (j < ord) {
                sum -= Inlines.MULT16_16(rden[j], y[i + j])
                j++
            }
            y[i + ord] = Inlines.ROUND16(sum, CeltConstants.SIG_SHIFT)
            _y[_y_ptr + i] = sum
            i++
        }
        i = 0
        while (i < ord) {
            mem[i] = _y[_y_ptr + N - i - 1]
            i++
        }
    }
}
