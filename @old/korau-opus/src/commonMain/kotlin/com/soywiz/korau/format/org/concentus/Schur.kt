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

internal object Schur {

    /* Faster than schur64(), but much less accurate.                       */
    /* uses SMLAWB(), requiring armv5E and higher.                          */
    fun silk_schur( /* O    Returns residual energy                                     */
        rc_Q15: ShortArray, /* O    reflection coefficients [order] Q15                         */
        c: IntArray, /* I    correlations [order+1]                                      */
        order: Int /* I    prediction order                                            */
    ): Int {
        var k: Int
        var n: Int
        var lz: Int
        val C = Arrays.InitTwoDimensionalArrayInt(SilkConstants.SILK_MAX_ORDER_LPC + 1, 2)
        var Ctmp1: Int
        var Ctmp2: Int
        var rc_tmp_Q15: Int

        Inlines.OpusAssert(order == 6 || order == 8 || order == 10 || order == 12 || order == 14 || order == 16)

        /* Get number of leading zeros */
        lz = Inlines.silk_CLZ32(c[0])

        /* Copy correlations and adjust level to Q30 */
        if (lz < 2) {
            /* lz must be 1, so shift one to the right */
            k = 0
            while (k < order + 1) {
                C[k][1] = Inlines.silk_RSHIFT(c[k], 1)
                C[k][0] = C[k][1]
                k++
            }
        } else if (lz > 2) {
            /* Shift to the left */
            lz -= 2
            k = 0
            while (k < order + 1) {
                C[k][1] = Inlines.silk_LSHIFT(c[k], lz)
                C[k][0] = C[k][1]
                k++
            }
        } else {
            /* No need to shift */
            k = 0
            while (k < order + 1) {
                C[k][1] = c[k]
                C[k][0] = C[k][1]
                k++
            }
        }

        k = 0
        while (k < order) {
            /* Check that we won't be getting an unstable rc, otherwise stop here. */
            if (Inlines.silk_abs_int32(C[k + 1][0]) >= C[0][1]) {
                if (C[k + 1][0] > 0) {
                    rc_Q15[k] =
                            (0 - (.99f * (1.toLong() shl 15) + 0.5).toInt()/*Inlines.SILK_CONST(.99f, 15)*/).toShort()
                } else {
                    rc_Q15[k] = (.99f * (1.toLong() shl 15) + 0.5).toInt().toShort()
                }
                k++
                break
            }

            /* Get reflection coefficient */
            rc_tmp_Q15 = 0 -
                    Inlines.silk_DIV32_16(C[k + 1][0], Inlines.silk_max_32(Inlines.silk_RSHIFT(C[0][1], 15), 1))

            /* Clip (shouldn't happen for properly conditioned inputs) */
            rc_tmp_Q15 = Inlines.silk_SAT16(rc_tmp_Q15)

            /* Store */
            rc_Q15[k] = rc_tmp_Q15.toShort()

            /* Update correlations */
            n = 0
            while (n < order - k) {
                Ctmp1 = C[n + k + 1][0]
                Ctmp2 = C[n][1]
                C[n + k + 1][0] = Inlines.silk_SMLAWB(Ctmp1, Inlines.silk_LSHIFT(Ctmp2, 1), rc_tmp_Q15)
                C[n][1] = Inlines.silk_SMLAWB(Ctmp2, Inlines.silk_LSHIFT(Ctmp1, 1), rc_tmp_Q15)
                n++
            }
            k++
        }

        while (k < order) {
            rc_Q15[k] = 0
            k++
        }

        /* return residual energy */
        return Inlines.silk_max_32(1, C[0][1])
    }

    /* Slower than schur(), but more accurate.                              */
    /* Uses SMULL(), available on armv4                                     */
    fun silk_schur64( /* O    returns residual energy                                     */
        rc_Q16: IntArray, /* O    Reflection coefficients [order] Q16                         */
        c: IntArray, /* I    Correlations [order+1]                                      */
        order: Int /* I    Prediction order                                            */
    ): Int {
        var k: Int
        var n: Int
        val C = Arrays.InitTwoDimensionalArrayInt(SilkConstants.SILK_MAX_ORDER_LPC + 1, 2)
        var Ctmp1_Q30: Int
        var Ctmp2_Q30: Int
        var rc_tmp_Q31: Int

        Inlines.OpusAssert(order == 6 || order == 8 || order == 10 || order == 12 || order == 14 || order == 16)

        /* Check for invalid input */
        if (c[0] <= 0) {
            Arrays.MemSet(rc_Q16, 0, order)
            return 0
        }

        k = 0
        while (k < order + 1) {
            C[k][1] = c[k]
            C[k][0] = C[k][1]
            k++
        }

        k = 0
        while (k < order) {
            /* Check that we won't be getting an unstable rc, otherwise stop here. */
            if (Inlines.silk_abs_int32(C[k + 1][0]) >= C[0][1]) {
                if (C[k + 1][0] > 0) {
                    rc_Q16[k] = -(.99f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(.99f, 16)*/
                } else {
                    rc_Q16[k] = (.99f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(.99f, 16)*/
                }
                k++
                break
            }

            /* Get reflection coefficient: divide two Q30 values and get result in Q31 */
            rc_tmp_Q31 = Inlines.silk_DIV32_varQ(-C[k + 1][0], C[0][1], 31)

            /* Save the output */
            rc_Q16[k] = Inlines.silk_RSHIFT_ROUND(rc_tmp_Q31, 15)

            /* Update correlations */
            n = 0
            while (n < order - k) {
                Ctmp1_Q30 = C[n + k + 1][0]
                Ctmp2_Q30 = C[n][1]

                /* Multiply and add the highest int32 */
                C[n + k + 1][0] = Ctmp1_Q30 + Inlines.silk_SMMUL(Inlines.silk_LSHIFT(Ctmp2_Q30, 1), rc_tmp_Q31)
                C[n][1] = Ctmp2_Q30 + Inlines.silk_SMMUL(Inlines.silk_LSHIFT(Ctmp1_Q30, 1), rc_tmp_Q31)
                n++
            }
            k++
        }

        while (k < order) {
            rc_Q16[k] = 0
            k++
        }

        return Inlines.silk_max_32(1, C[0][1])
    }
}
