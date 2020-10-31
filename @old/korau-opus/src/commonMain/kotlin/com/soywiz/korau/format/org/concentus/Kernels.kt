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

internal object Kernels {

    fun celt_fir(
        x: ShortArray,
        x_ptr: Int,
        num: ShortArray,
        y: ShortArray,
        y_ptr: Int,
        N: Int,
        ord: Int,
        mem: ShortArray
    ) {
        var i: Int
        var j: Int
        val rnum = ShortArray(ord)
        val local_x = ShortArray(N + ord)

        i = 0
        while (i < ord) {
            rnum[i] = num[ord - i - 1]
            i++
        }

        i = 0
        while (i < ord) {
            local_x[i] = mem[ord - i - 1]
            i++
        }

        i = 0
        while (i < N) {
            local_x[i + ord] = x[x_ptr + i]
            i++
        }

        i = 0
        while (i < ord) {
            mem[i] = x[x_ptr + N - i - 1]
            i++
        }

        val sum0 = BoxedValueInt(0)
        val sum1 = BoxedValueInt(0)
        val sum2 = BoxedValueInt(0)
        val sum3 = BoxedValueInt(0)

        i = 0
        while (i < N - 3) {
            sum0.Val = 0
            sum1.Val = 0
            sum2.Val = 0
            sum3.Val = 0
            xcorr_kernel(rnum, 0, local_x, i, sum0, sum1, sum2, sum3, ord)
            y[y_ptr + i] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i]),
                    Inlines.PSHR32(sum0.Val, CeltConstants.SIG_SHIFT)
                )
            )
            y[y_ptr + i + 1] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i + 1]),
                    Inlines.PSHR32(sum1.Val, CeltConstants.SIG_SHIFT)
                )
            )
            y[y_ptr + i + 2] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i + 2]),
                    Inlines.PSHR32(sum2.Val, CeltConstants.SIG_SHIFT)
                )
            )
            y[y_ptr + i + 3] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i + 3]),
                    Inlines.PSHR32(sum3.Val, CeltConstants.SIG_SHIFT)
                )
            )
            i += 4
        }

        while (i < N) {
            var sum = 0

            j = 0
            while (j < ord) {
                sum = Inlines.MAC16_16(sum, rnum[j], local_x[i + j])
                j++
            }

            y[y_ptr + i] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i]),
                    Inlines.PSHR32(sum, CeltConstants.SIG_SHIFT)
                )
            )
            i++
        }
    }

    fun celt_fir(
        x: IntArray,
        x_ptr: Int,
        num: IntArray,
        num_ptr: Int,
        y: IntArray,
        y_ptr: Int,
        N: Int,
        ord: Int,
        mem: IntArray
    ) {
        var i: Int
        var j: Int
        val rnum = IntArray(ord)
        val local_x = IntArray(N + ord)

        i = 0
        while (i < ord) {
            rnum[i] = num[num_ptr + ord - i - 1]
            i++
        }

        i = 0
        while (i < ord) {
            local_x[i] = mem[ord - i - 1]
            i++
        }

        i = 0
        while (i < N) {
            local_x[i + ord] = x[x_ptr + i]
            i++
        }

        i = 0
        while (i < ord) {
            mem[i] = x[x_ptr + N - i - 1]
            i++
        }

        val sum0 = BoxedValueInt(0)
        val sum1 = BoxedValueInt(0)
        val sum2 = BoxedValueInt(0)
        val sum3 = BoxedValueInt(0)

        i = 0
        while (i < N - 3) {
            sum0.Val = 0
            sum1.Val = 0
            sum2.Val = 0
            sum3.Val = 0
            xcorr_kernel(rnum, local_x, i, sum0, sum1, sum2, sum3, ord)
            y[y_ptr + i] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i]),
                    Inlines.PSHR32(sum0.Val, CeltConstants.SIG_SHIFT)
                )
            ).toInt()
            y[y_ptr + i + 1] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i + 1]),
                    Inlines.PSHR32(sum1.Val, CeltConstants.SIG_SHIFT)
                )
            ).toInt()
            y[y_ptr + i + 2] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i + 2]),
                    Inlines.PSHR32(sum2.Val, CeltConstants.SIG_SHIFT)
                )
            ).toInt()
            y[y_ptr + i + 3] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i + 3]),
                    Inlines.PSHR32(sum3.Val, CeltConstants.SIG_SHIFT)
                )
            ).toInt()
            i += 4
        }

        while (i < N) {
            var sum = 0

            j = 0
            while (j < ord) {
                sum = Inlines.MAC16_16(sum, rnum[j], local_x[i + j])
                j++
            }

            y[y_ptr + i] = Inlines.SATURATE16(
                Inlines.ADD32(
                    Inlines.EXTEND32(x[x_ptr + i]),
                    Inlines.PSHR32(sum, CeltConstants.SIG_SHIFT)
                )
            ).toInt()
            i++
        }
    }

    /// <summary>
    /// OPT: This is the kernel you really want to optimize. It gets used a lot by the prefilter and by the PLC.
    /// </summary>
    /// <param name="x"></param>
    /// <param name="y"></param>
    /// <param name="sum"></param>
    /// <param name="len"></param>
    fun xcorr_kernel(
        x: ShortArray,
        x_ptr: Int,
        y: ShortArray,
        y_ptr: Int,
        _sum0: BoxedValueInt,
        _sum1: BoxedValueInt,
        _sum2: BoxedValueInt,
        _sum3: BoxedValueInt,
        len: Int
    ) {
        var x_ptr = x_ptr
        var y_ptr = y_ptr
        var sum0 = _sum0.Val
        var sum1 = _sum1.Val
        var sum2 = _sum2.Val
        var sum3 = _sum3.Val
        var j: Int
        var y_0: Short
        var y_1: Short
        var y_2: Short
        var y_3: Short
        Inlines.OpusAssert(len >= 3)
        y_3 = 0
        /* gcc doesn't realize that y_3 can't be used uninitialized */
        y_0 = y[y_ptr++]
        y_1 = y[y_ptr++]
        y_2 = y[y_ptr++]
        j = 0
        while (j < len - 3) {
            var tmp: Short
            tmp = x[x_ptr++]
            y_3 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_0)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_1)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_2)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_3)
            tmp = x[x_ptr++]
            y_0 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_1)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_2)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_3)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_0)
            tmp = x[x_ptr++]
            y_1 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_2)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_3)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_0)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_1)
            tmp = x[x_ptr++]
            y_2 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_3)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_0)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_1)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_2)
            j += 4
        }
        if (j++ < len) {
            val tmp: Short
            tmp = x[x_ptr++]
            y_3 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_0)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_1)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_2)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_3)
        }
        if (j++ < len) {
            val tmp: Short
            tmp = x[x_ptr++]
            y_0 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_1)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_2)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_3)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_0)
        }
        if (j < len) {
            val tmp: Short
            tmp = x[x_ptr++]
            y_1 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_2)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_3)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_0)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_1)
        }

        _sum0.Val = sum0
        _sum1.Val = sum1
        _sum2.Val = sum2
        _sum3.Val = sum3
    }

    fun xcorr_kernel(
        x: IntArray,
        y: IntArray,
        y_ptr: Int,
        _sum0: BoxedValueInt,
        _sum1: BoxedValueInt,
        _sum2: BoxedValueInt,
        _sum3: BoxedValueInt,
        len: Int
    ) {
        var y_ptr = y_ptr
        var sum0 = _sum0.Val
        var sum1 = _sum1.Val
        var sum2 = _sum2.Val
        var sum3 = _sum3.Val
        var j: Int
        var y_0: Int
        var y_1: Int
        var y_2: Int
        var y_3: Int
        var x_ptr = 0
        Inlines.OpusAssert(len >= 3)
        y_3 = 0
        /* gcc doesn't realize that y_3 can't be used uninitialized */
        y_0 = y[y_ptr++]
        y_1 = y[y_ptr++]
        y_2 = y[y_ptr++]
        j = 0
        while (j < len - 3) {
            var tmp: Int
            tmp = x[x_ptr++]
            y_3 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_0)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_1)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_2)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_3)
            tmp = x[x_ptr++]
            y_0 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_1)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_2)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_3)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_0)
            tmp = x[x_ptr++]
            y_1 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_2)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_3)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_0)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_1)
            tmp = x[x_ptr++]
            y_2 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_3)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_0)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_1)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_2)
            j += 4
        }
        if (j++ < len) {
            val tmp: Int
            tmp = x[x_ptr++]
            y_3 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_0)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_1)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_2)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_3)
        }
        if (j++ < len) {
            val tmp: Int
            tmp = x[x_ptr++]
            y_0 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_1)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_2)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_3)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_0)
        }
        if (j < len) {
            val tmp: Int
            tmp = x[x_ptr++]
            y_1 = y[y_ptr++]
            sum0 = Inlines.MAC16_16(sum0, tmp, y_2)
            sum1 = Inlines.MAC16_16(sum1, tmp, y_3)
            sum2 = Inlines.MAC16_16(sum2, tmp, y_0)
            sum3 = Inlines.MAC16_16(sum3, tmp, y_1)
        }

        _sum0.Val = sum0
        _sum1.Val = sum1
        _sum2.Val = sum2
        _sum3.Val = sum3
    }

    fun celt_inner_prod(x: ShortArray, x_ptr: Int, y: ShortArray, y_ptr: Int, N: Int): Int {
        var i: Int
        var xy = 0
        i = 0
        while (i < N) {
            xy = Inlines.MAC16_16(xy, x[x_ptr + i], y[y_ptr + i])
            i++
        }
        return xy
    }

    fun celt_inner_prod(x: ShortArray, y: ShortArray, y_ptr: Int, N: Int): Int {
        var i: Int
        var xy = 0
        i = 0
        while (i < N) {
            xy = Inlines.MAC16_16(xy, x[i], y[y_ptr + i])
            i++
        }
        return xy
    }

    fun celt_inner_prod(x: IntArray, x_ptr: Int, y: IntArray, y_ptr: Int, N: Int): Int {
        var i: Int
        var xy = 0
        i = 0
        while (i < N) {
            xy = Inlines.MAC16_16(xy, x[x_ptr + i], y[y_ptr + i])
            i++
        }
        return xy
    }

    fun dual_inner_prod(
        x: IntArray,
        x_ptr: Int,
        y01: IntArray,
        y01_ptr: Int,
        y02: IntArray,
        y02_ptr: Int,
        N: Int,
        xy1: BoxedValueInt,
        xy2: BoxedValueInt
    ) {
        var i: Int
        var xy01 = 0
        var xy02 = 0
        i = 0
        while (i < N) {
            xy01 = Inlines.MAC16_16(xy01, x[x_ptr + i], y01[y01_ptr + i])
            xy02 = Inlines.MAC16_16(xy02, x[x_ptr + i], y02[y02_ptr + i])
            i++
        }
        xy1.Val = xy01
        xy2.Val = xy02
    }
}
