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

internal object CeltPitchXCorr {

    fun pitch_xcorr(
        _x: IntArray,
        _y: IntArray,
        xcorr: IntArray,
        len: Int,
        max_pitch: Int
    ): Int {
        var i: Int
        var maxcorr = 1
        Inlines.OpusAssert(max_pitch > 0)
        val sum0 = BoxedValueInt(0)
        val sum1 = BoxedValueInt(0)
        val sum2 = BoxedValueInt(0)
        val sum3 = BoxedValueInt(0)
        i = 0
        while (i < max_pitch - 3) {
            sum0.Val = 0
            sum1.Val = 0
            sum2.Val = 0
            sum3.Val = 0
            Kernels.xcorr_kernel(_x, _y, i, sum0, sum1, sum2, sum3, len)
            xcorr[i] = sum0.Val
            xcorr[i + 1] = sum1.Val
            xcorr[i + 2] = sum2.Val
            xcorr[i + 3] = sum3.Val
            sum0.Val = Inlines.MAX32(sum0.Val, sum1.Val)
            sum2.Val = Inlines.MAX32(sum2.Val, sum3.Val)
            sum0.Val = Inlines.MAX32(sum0.Val, sum2.Val)
            maxcorr = Inlines.MAX32(maxcorr, sum0.Val)
            i += 4
        }
        /* In case max_pitch isn't a multiple of 4, do non-unrolled version. */
        while (i < max_pitch) {
            val inner_sum = Kernels.celt_inner_prod(_x, 0, _y, i, len)
            xcorr[i] = inner_sum
            maxcorr = Inlines.MAX32(maxcorr, inner_sum)
            i++
        }
        return maxcorr
    }

    fun pitch_xcorr(
        _x: ShortArray,
        _x_ptr: Int,
        _y: ShortArray,
        _y_ptr: Int,
        xcorr: IntArray,
        len: Int,
        max_pitch: Int
    ): Int {
        var i: Int
        var maxcorr = 1
        Inlines.OpusAssert(max_pitch > 0)
        val sum0 = BoxedValueInt(0)
        val sum1 = BoxedValueInt(0)
        val sum2 = BoxedValueInt(0)
        val sum3 = BoxedValueInt(0)
        i = 0
        while (i < max_pitch - 3) {
            sum0.Val = 0
            sum1.Val = 0
            sum2.Val = 0
            sum3.Val = 0
            Kernels.xcorr_kernel(_x, _x_ptr, _y, _y_ptr + i, sum0, sum1, sum2, sum3, len)

            xcorr[i] = sum0.Val
            xcorr[i + 1] = sum1.Val
            xcorr[i + 2] = sum2.Val
            xcorr[i + 3] = sum3.Val
            sum0.Val = Inlines.MAX32(sum0.Val, sum1.Val)
            sum2.Val = Inlines.MAX32(sum2.Val, sum3.Val)
            sum0.Val = Inlines.MAX32(sum0.Val, sum2.Val)
            maxcorr = Inlines.MAX32(maxcorr, sum0.Val)
            i += 4
        }
        /* In case max_pitch isn't a multiple of 4, do non-unrolled version. */
        while (i < max_pitch) {
            val inner_sum = Kernels.celt_inner_prod(_x, _x_ptr, _y, _y_ptr + i, len)
            xcorr[i] = inner_sum
            maxcorr = Inlines.MAX32(maxcorr, inner_sum)
            i++
        }
        return maxcorr
    }

    fun pitch_xcorr(
        _x: ShortArray,
        _y: ShortArray,
        xcorr: IntArray,
        len: Int,
        max_pitch: Int
    ): Int {
        var i: Int
        var maxcorr = 1
        Inlines.OpusAssert(max_pitch > 0)
        val sum0 = BoxedValueInt(0)
        val sum1 = BoxedValueInt(0)
        val sum2 = BoxedValueInt(0)
        val sum3 = BoxedValueInt(0)
        i = 0
        while (i < max_pitch - 3) {
            sum0.Val = 0
            sum1.Val = 0
            sum2.Val = 0
            sum3.Val = 0
            Kernels.xcorr_kernel(_x, 0, _y, i, sum0, sum1, sum2, sum3, len)

            xcorr[i] = sum0.Val
            xcorr[i + 1] = sum1.Val
            xcorr[i + 2] = sum2.Val
            xcorr[i + 3] = sum3.Val
            sum0.Val = Inlines.MAX32(sum0.Val, sum1.Val)
            sum2.Val = Inlines.MAX32(sum2.Val, sum3.Val)
            sum0.Val = Inlines.MAX32(sum0.Val, sum2.Val)
            maxcorr = Inlines.MAX32(maxcorr, sum0.Val)
            i += 4
        }
        /* In case max_pitch isn't a multiple of 4, do non-unrolled version. */
        while (i < max_pitch) {
            val inner_sum = Kernels.celt_inner_prod(_x, _y, i, len)
            xcorr[i] = inner_sum
            maxcorr = Inlines.MAX32(maxcorr, inner_sum)
            i++
        }
        return maxcorr
    }
}
