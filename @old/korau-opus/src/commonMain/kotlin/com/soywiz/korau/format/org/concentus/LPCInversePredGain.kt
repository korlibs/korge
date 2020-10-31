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

internal object LPCInversePredGain {

    private val QA = 24
    private val A_LIMIT = (0.99975f * (1.toLong() shl QA) + 0.5).toInt()/*Inlines.SILK_CONST(0.99975f, QA)*/

    /* Compute inverse of LPC prediction gain, and                          */
    /* test if LPC coefficients are stable (all poles within unit circle)   */
    fun LPC_inverse_pred_gain_QA( /* O   Returns inverse prediction gain in energy domain, Q30    */
        A_QA: Array<IntArray>, /* I   Prediction coefficients [ 2 ][SILK_MAX_ORDER_LPC]                                 */
        order: Int /* I   Prediction order                                         */
    ): Int {
        var k: Int
        var n: Int
        var mult2Q: Int
        var invGain_Q30: Int
        var rc_Q31: Int
        var rc_mult1_Q30: Int
        var rc_mult2: Int
        var tmp_QA: Int
        var Aold_QA: IntArray
        var Anew_QA: IntArray

        Anew_QA = A_QA[order and 1]

        invGain_Q30 = 1 shl 30
        k = order - 1
        while (k > 0) {
            /* Check for stability */
            if (Anew_QA[k] > A_LIMIT || Anew_QA[k] < -A_LIMIT) {
                return 0
            }

            /* Set RC equal to negated AR coef */
            rc_Q31 = 0 - Inlines.silk_LSHIFT(Anew_QA[k], 31 - QA)

            /* rc_mult1_Q30 range: [ 1 : 2^30 ] */
            rc_mult1_Q30 = (1 shl 30) - Inlines.silk_SMMUL(rc_Q31, rc_Q31)
            Inlines.OpusAssert(rc_mult1_Q30 > 1 shl 15)
            /* reduce A_LIMIT if fails */
            Inlines.OpusAssert(rc_mult1_Q30 <= 1 shl 30)

            /* rc_mult2 range: [ 2^30 : silk_int32_MAX ] */
            mult2Q = 32 - Inlines.silk_CLZ32(Inlines.silk_abs(rc_mult1_Q30))
            rc_mult2 = Inlines.silk_INVERSE32_varQ(rc_mult1_Q30, mult2Q + 30)

            /* Update inverse gain */
            /* invGain_Q30 range: [ 0 : 2^30 ] */
            invGain_Q30 = Inlines.silk_LSHIFT(Inlines.silk_SMMUL(invGain_Q30, rc_mult1_Q30), 2)
            Inlines.OpusAssert(invGain_Q30 >= 0)
            Inlines.OpusAssert(invGain_Q30 <= 1 shl 30)

            /* Swap pointers */
            Aold_QA = Anew_QA
            Anew_QA = A_QA[k and 1]

            /* Update AR coefficient */
            n = 0
            while (n < k) {
                tmp_QA = Aold_QA[n] - Inlines.MUL32_FRAC_Q(Aold_QA[k - n - 1], rc_Q31, 31)
                Anew_QA[n] = Inlines.MUL32_FRAC_Q(tmp_QA, rc_mult2, mult2Q)
                n++
            }
            k--
        }

        /* Check for stability */
        if (Anew_QA[0] > A_LIMIT || Anew_QA[0] < -A_LIMIT) {
            return 0
        }

        /* Set RC equal to negated AR coef */
        rc_Q31 = 0 - Inlines.silk_LSHIFT(Anew_QA[0], 31 - QA)

        /* Range: [ 1 : 2^30 ] */
        rc_mult1_Q30 = (1 shl 30) - Inlines.silk_SMMUL(rc_Q31, rc_Q31)

        /* Update inverse gain */
        /* Range: [ 0 : 2^30 ] */
        invGain_Q30 = Inlines.silk_LSHIFT(Inlines.silk_SMMUL(invGain_Q30, rc_mult1_Q30), 2)
        Inlines.OpusAssert(invGain_Q30 >= 0)
        Inlines.OpusAssert(invGain_Q30 <= 1 shl 30)

        return invGain_Q30
    }

    /* For input in Q12 domain */
    fun silk_LPC_inverse_pred_gain( /* O   Returns inverse prediction gain in energy domain, Q30        */
        A_Q12: ShortArray, /* I   Prediction coefficients, Q12 [order]                         */
        order: Int /* I   Prediction order                                             */
    ): Int {
        var k: Int
        val Atmp_QA = Arrays.InitTwoDimensionalArrayInt(2, SilkConstants.SILK_MAX_ORDER_LPC)
        val Anew_QA: IntArray
        var DC_resp = 0

        Anew_QA = Atmp_QA[order and 1]

        /* Increase Q domain of the AR coefficients */
        k = 0
        while (k < order) {
            DC_resp += A_Q12[k].toInt()
            Anew_QA[k] = Inlines.silk_LSHIFT32(A_Q12[k].toInt(), QA - 12)
            k++
        }
        /* If the DC is unstable, we don't even need to do the full calculations */
        return if (DC_resp >= 4096) {
            0
        } else LPC_inverse_pred_gain_QA(Atmp_QA, order)
    }

    fun silk_LPC_inverse_pred_gain_Q24( /* O    Returns inverse prediction gain in energy domain, Q30       */
        A_Q24: IntArray, /* I    Prediction coefficients [order]                             */
        order: Int /* I    Prediction order                                            */
    ): Int {
        var k: Int
        val Atmp_QA = Arrays.InitTwoDimensionalArrayInt(2, SilkConstants.SILK_MAX_ORDER_LPC)
        val Anew_QA: IntArray

        Anew_QA = Atmp_QA[order and 1]

        /* Increase Q domain of the AR coefficients */
        k = 0
        while (k < order) {
            Anew_QA[k] = Inlines.silk_RSHIFT32(A_Q24[k], 24 - QA)
            k++
        }

        return LPC_inverse_pred_gain_QA(Atmp_QA, order)
    }
}
