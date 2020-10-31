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

internal object LTPAnalysisFilter {

    fun silk_LTP_analysis_filter(
        LTP_res: ShortArray, /* O    LTP residual signal of length SilkConstants.MAX_NB_SUBFR * ( pre_length + subfr_length )  */
        x: ShortArray, /* I    Pointer to input signal with at least max( pitchL ) preceding samples       */
        x_ptr: Int,
        LTPCoef_Q14: ShortArray, /* I     LTP_ORDER LTP coefficients for each MAX_NB_SUBFR subframe  [SilkConstants.LTP_ORDER * SilkConstants.MAX_NB_SUBFR]                 */
        pitchL: IntArray, /* I    Pitch lag, one for each subframe [SilkConstants.MAX_NB_SUBFR]                                           */
        invGains_Q16: IntArray, /* I    Inverse quantization gains, one for each subframe [SilkConstants.MAX_NB_SUBFR]                           */
        subfr_length: Int, /* I    Length of each subframe                                                     */
        nb_subfr: Int, /* I    Number of subframes                                                         */
        pre_length: Int /* I    Length of the preceding samples starting at &x[0] for each subframe         */
    ) {
        var x_ptr2: Int
        var x_lag_ptr: Int
        val Btmp_Q14 = ShortArray(SilkConstants.LTP_ORDER)
        var LTP_res_ptr: Int
        var k: Int
        var i: Int
        var LTP_est: Int

        x_ptr2 = x_ptr
        LTP_res_ptr = 0
        k = 0
        while (k < nb_subfr) {
            x_lag_ptr = x_ptr2 - pitchL[k]

            Btmp_Q14[0] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER]
            Btmp_Q14[1] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 1]
            Btmp_Q14[2] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 2]
            Btmp_Q14[3] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 3]
            Btmp_Q14[4] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 4]

            /* LTP analysis FIR filter */
            i = 0
            while (i < subfr_length + pre_length) {
                val LTP_res_ptri = LTP_res_ptr + i
                LTP_res[LTP_res_ptri] = x[x_ptr2 + i]

                /* Long-term prediction */
                LTP_est = Inlines.silk_SMULBB(x[x_lag_ptr + SilkConstants.LTP_ORDER / 2].toInt(), Btmp_Q14[0].toInt())
                LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr + 1].toInt(), Btmp_Q14[1].toInt())
                LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr].toInt(), Btmp_Q14[2].toInt())
                LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr - 1].toInt(), Btmp_Q14[3].toInt())
                LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr - 2].toInt(), Btmp_Q14[4].toInt())

                LTP_est = Inlines.silk_RSHIFT_ROUND(LTP_est, 14)
                /* round and . Q0*/

                /* Subtract long-term prediction */
                LTP_res[LTP_res_ptri] = Inlines.silk_SAT16(x[x_ptr2 + i].toInt() - LTP_est).toShort()

                /* Scale residual */
                LTP_res[LTP_res_ptri] = Inlines.silk_SMULWB(invGains_Q16[k], LTP_res[LTP_res_ptri].toInt()).toShort()

                x_lag_ptr++
                i++
            }

            /* Update pointers */
            LTP_res_ptr += subfr_length + pre_length
            x_ptr2 += subfr_length
            k++
        }
    }
}
