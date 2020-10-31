/* Copyright (c) 2006-2011 Skype Limited. All Rights Reserved
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

internal object SumSqrShift {

    /// <summary>
    /// Compute number of bits to right shift the sum of squares of a vector
    /// of int16s to make it fit in an int32
    /// </summary>
    /// <param name="energy">O   Energy of x, after shifting to the right</param>
    /// <param name="shift">O   Number of bits right shift applied to energy</param>
    /// <param name="x">I   Input vector</param>
    /// <param name="len">I   Length of input vector</param>
    fun silk_sum_sqr_shift(
        energy: BoxedValueInt,
        shift: BoxedValueInt,
        x: ShortArray,
        x_ptr: Int,
        len: Int
    ) {
        var len = len
        var i: Int
        var shft: Int
        var nrg_tmp: Int
        var nrg: Int

        nrg = 0
        shft = 0
        len--

        i = 0
        while (i < len) {
            nrg = Inlines.silk_SMLABB_ovflw(nrg, x[x_ptr + i].toInt(), x[x_ptr + i].toInt())
            nrg = Inlines.silk_SMLABB_ovflw(nrg, x[x_ptr + i + 1].toInt(), x[x_ptr + i + 1].toInt())
            if (nrg < 0) {
                /* Scale down */
                nrg = Inlines.silk_RSHIFT_uint(nrg.toLong(), 2).toInt()
                shft = 2
                i += 2
                break
            }
            i += 2
        }

        while (i < len) {
            nrg_tmp = Inlines.silk_SMULBB(x[x_ptr + i].toInt(), x[x_ptr + i].toInt())
            nrg_tmp = Inlines.silk_SMLABB_ovflw(nrg_tmp, x[x_ptr + i + 1].toInt(), x[x_ptr + i + 1].toInt())
            nrg = Inlines.silk_ADD_RSHIFT_uint(nrg.toLong(), nrg_tmp.toLong(), shft).toInt()
            if (nrg < 0) {
                /* Scale down */
                nrg = Inlines.silk_RSHIFT_uint(nrg.toLong(), 2).toInt()
                shft += 2
            }
            i += 2
        }

        if (i == len) {
            /* One sample left to process */
            nrg_tmp = Inlines.silk_SMULBB(x[x_ptr + i].toInt(), x[x_ptr + i].toInt())
            nrg = Inlines.silk_ADD_RSHIFT_uint(nrg.toLong(), nrg_tmp.toLong(), shft).toInt()
        }

        /* Make sure to have at least one extra leading zero (two leading zeros in total) */
        if (nrg and -0x40000000 != 0) {
            nrg = Inlines.silk_RSHIFT_uint(nrg.toLong(), 2).toInt()
            shft += 2
        }

        /* Output arguments */
        shift.Val = shft
        energy.Val = nrg
    }

    /// <summary>
    /// Zero-index variant
    /// Compute number of bits to right shift the sum of squares of a vector
    /// of int16s to make it fit in an int32
    /// </summary>
    /// <param name="energy">O   Energy of x, after shifting to the right</param>
    /// <param name="shift">O   Number of bits right shift applied to energy</param>
    /// <param name="x">I   Input vector</param>
    /// <param name="len">I   Length of input vector</param>
    fun silk_sum_sqr_shift(
        energy: BoxedValueInt,
        shift: BoxedValueInt,
        x: ShortArray,
        len: Int
    ) {
        var len = len
        var i: Int
        var shft: Int
        var nrg_tmp: Int
        var nrg: Int

        nrg = 0
        shft = 0
        len--

        i = 0
        while (i < len) {
            nrg = Inlines.silk_SMLABB_ovflw(nrg, x[i].toInt(), x[i].toInt())
            nrg = Inlines.silk_SMLABB_ovflw(nrg, x[i + 1].toInt(), x[i + 1].toInt())
            if (nrg < 0) {
                /* Scale down */
                nrg = Inlines.silk_RSHIFT_uint(nrg.toLong(), 2).toInt()
                shft = 2
                i += 2
                break
            }
            i += 2
        }

        while (i < len) {
            nrg_tmp = Inlines.silk_SMULBB(x[i].toInt(), x[i].toInt())
            nrg_tmp = Inlines.silk_SMLABB_ovflw(nrg_tmp, x[i + 1].toInt(), x[i + 1].toInt())
            nrg = Inlines.silk_ADD_RSHIFT_uint(nrg.toLong(), nrg_tmp.toLong(), shft).toInt()
            if (nrg < 0) {
                /* Scale down */
                nrg = Inlines.silk_RSHIFT_uint(nrg.toLong(), 2).toInt()
                shft += 2
            }
            i += 2
        }

        if (i == len) {
            /* One sample left to process */
            nrg_tmp = Inlines.silk_SMULBB(x[i].toInt(), x[i].toInt())
            nrg = Inlines.silk_ADD_RSHIFT_uint(nrg.toLong(), nrg_tmp.toLong(), shft).toInt()
        }

        /* Make sure to have at least one extra leading zero (two leading zeros in total) */
        if (nrg and -0x40000000 != 0) {
            nrg = Inlines.silk_RSHIFT_uint(nrg.toLong(), 2).toInt()
            shft += 2
        }

        /* Output arguments */
        shift.Val = shft
        energy.Val = nrg
    }
}
