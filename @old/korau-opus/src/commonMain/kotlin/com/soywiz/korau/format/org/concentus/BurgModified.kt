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

import com.soywiz.kmem.*

internal object BurgModified {

    /* subfr_length * nb_subfr = ( 0.005 * 16000 + 16 ) * 4 = 384 */
    private val MAX_FRAME_SIZE = 384
    private val QA = 25
    private val N_BITS_HEAD_ROOM = 2
    private val MIN_RSHIFTS = -16
    private val MAX_RSHIFTS = 32 - QA

    /* Compute reflection coefficients from input signal */
    fun silk_burg_modified(
        res_nrg: BoxedValueInt, /* O    Residual energy                                             */
        res_nrg_Q: BoxedValueInt, /* O    Residual energy Q value                                     */
        A_Q16: IntArray, /* O    Prediction coefficients (length order)                      */
        x: ShortArray, /* I    Input signal, length: nb_subfr * ( D + subfr_length )       */
        x_ptr: Int,
        minInvGain_Q30: Int, /* I    Inverse of max prediction gain                              */
        subfr_length: Int, /* I    Input signal subframe length (incl. D preceding samples)    */
        nb_subfr: Int, /* I    Number of subframes stacked in x                            */
        D: Int /* I    Order                                                       */
    ) {
        var k: Int
        var n: Int
        var s: Int
        var lz: Int
        var rshifts: Int
        var reached_max_gain: Int
        var C0: Int = 0
        var num: Int
        var nrg: Int
        var rc_Q31: Int
        var invGain_Q30: Int
        var Atmp_QA: Int
        var Atmp1: Int
        var tmp1: Int
        var tmp2: Int
        var x1: Int
        var x2: Int
        var x_offset: Int
        val C_first_row = IntArray(SilkConstants.SILK_MAX_ORDER_LPC)
        val C_last_row = IntArray(SilkConstants.SILK_MAX_ORDER_LPC)
        val Af_QA = IntArray(SilkConstants.SILK_MAX_ORDER_LPC)
        val CAf = IntArray(SilkConstants.SILK_MAX_ORDER_LPC + 1)
        val CAb = IntArray(SilkConstants.SILK_MAX_ORDER_LPC + 1)
        val xcorr = IntArray(SilkConstants.SILK_MAX_ORDER_LPC)
        val C0_64: Long

        Inlines.OpusAssert(subfr_length * nb_subfr <= MAX_FRAME_SIZE)

        /* Compute autocorrelations, added over subframes */
        C0_64 = Inlines.silk_inner_prod16_aligned_64(x, x_ptr, x, x_ptr, subfr_length * nb_subfr)
        lz = Inlines.silk_CLZ64(C0_64)
        rshifts = 32 + 1 + N_BITS_HEAD_ROOM - lz
        if (rshifts > MAX_RSHIFTS) {
            rshifts = MAX_RSHIFTS
        }
        if (rshifts < MIN_RSHIFTS) {
            rshifts = MIN_RSHIFTS
        }

        if (rshifts > 0) {
            C0 = Inlines.silk_RSHIFT64(C0_64, rshifts).toInt()
        } else {
            C0 = Inlines.silk_LSHIFT32(C0_64.toInt(), -rshifts)
        }

        CAf[0] = C0 + Inlines.silk_SMMUL(
            (TuningParameters.FIND_LPC_COND_FAC * (1.toLong() shl 32) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.FIND_LPC_COND_FAC, 32)*/,
            C0
        ) + 1
        CAb[0] = CAf[0]
        /* Q(-rshifts) */
        Arrays.MemSet(C_first_row, 0, SilkConstants.SILK_MAX_ORDER_LPC)
        if (rshifts > 0) {
            s = 0
            while (s < nb_subfr) {
                x_offset = x_ptr + s * subfr_length
                n = 1
                while (n < D + 1) {
                    C_first_row[n - 1] += Inlines.silk_RSHIFT64(
                        Inlines.silk_inner_prod16_aligned_64(x, x_offset, x, x_offset + n, subfr_length - n), rshifts
                    ).toInt()
                    n++
                }
                s++
            }
        } else {
            s = 0
            while (s < nb_subfr) {
                var i: Int
                var d: Int
                x_offset = x_ptr + s * subfr_length
                CeltPitchXCorr.pitch_xcorr(x, x_offset, x, x_offset + 1, xcorr, subfr_length - D, D)
                n = 1
                while (n < D + 1) {
                    i = n + subfr_length - D
                    d = 0
                    while (i < subfr_length) {
                        d = Inlines.MAC16_16(d, x[x_offset + i], x[x_offset + i - n])
                        i++
                    }
                    xcorr[n - 1] += d
                    n++
                }
                n = 1
                while (n < D + 1) {
                    C_first_row[n - 1] += Inlines.silk_LSHIFT32(xcorr[n - 1], -rshifts)
                    n++
                }
                s++
            }
        }
        arraycopy(C_first_row, 0, C_last_row, 0, SilkConstants.SILK_MAX_ORDER_LPC)

        /* Initialize */
        CAf[0] = C0 + Inlines.silk_SMMUL(
            (TuningParameters.FIND_LPC_COND_FAC * (1.toLong() shl 32) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.FIND_LPC_COND_FAC, 32)*/,
            C0
        ) + 1
        CAb[0] = CAf[0]
        /* Q(-rshifts) */

        invGain_Q30 = 1 shl 30
        reached_max_gain = 0
        n = 0
        while (n < D) {
            /* Update first row of correlation matrix (without first element) */
            /* Update last row of correlation matrix (without last element, stored in reversed order) */
            /* Update C * Af */
            /* Update C * flipud(Af) (stored in reversed order) */
            if (rshifts > -2) {
                s = 0
                while (s < nb_subfr) {
                    x_offset = x_ptr + s * subfr_length
                    x1 = -Inlines.silk_LSHIFT32(x[x_offset + n].toInt(), 16 - rshifts)
                    /* Q(16-rshifts) */
                    x2 = -Inlines.silk_LSHIFT32(x[x_offset + subfr_length - n - 1].toInt(), 16 - rshifts)
                    /* Q(16-rshifts) */
                    tmp1 = Inlines.silk_LSHIFT32(x[x_offset + n].toInt(), QA - 16)
                    /* Q(QA-16) */
                    tmp2 = Inlines.silk_LSHIFT32(x[x_offset + subfr_length - n - 1].toInt(), QA - 16)
                    /* Q(QA-16) */
                    k = 0
                    while (k < n) {
                        C_first_row[k] = Inlines.silk_SMLAWB(C_first_row[k], x1, x[x_offset + n - k - 1].toInt())
                        /* Q( -rshifts ) */
                        C_last_row[k] =
                                Inlines.silk_SMLAWB(C_last_row[k], x2, x[x_offset + subfr_length - n + k].toInt())
                        /* Q( -rshifts ) */
                        Atmp_QA = Af_QA[k]
                        tmp1 = Inlines.silk_SMLAWB(tmp1, Atmp_QA, x[x_offset + n - k - 1].toInt())
                        /* Q(QA-16) */
                        tmp2 = Inlines.silk_SMLAWB(tmp2, Atmp_QA, x[x_offset + subfr_length - n + k].toInt())
                        k++
                        /* Q(QA-16) */
                    }
                    tmp1 = Inlines.silk_LSHIFT32(-tmp1, 32 - QA - rshifts)
                    /* Q(16-rshifts) */
                    tmp2 = Inlines.silk_LSHIFT32(-tmp2, 32 - QA - rshifts)
                    /* Q(16-rshifts) */
                    k = 0
                    while (k <= n) {
                        CAf[k] = Inlines.silk_SMLAWB(CAf[k], tmp1, x[x_offset + n - k].toInt())
                        /* Q( -rshift ) */
                        CAb[k] = Inlines.silk_SMLAWB(CAb[k], tmp2, x[x_offset + subfr_length - n + k - 1].toInt())
                        k++
                        /* Q( -rshift ) */
                    }
                    s++
                }
            } else {
                s = 0
                while (s < nb_subfr) {
                    x_offset = x_ptr + s * subfr_length
                    x1 = -Inlines.silk_LSHIFT32(x[x_offset + n].toInt(), -rshifts)
                    /* Q( -rshifts ) */
                    x2 = -Inlines.silk_LSHIFT32(x[x_offset + subfr_length - n - 1].toInt(), -rshifts)
                    /* Q( -rshifts ) */
                    tmp1 = Inlines.silk_LSHIFT32(x[x_offset + n].toInt(), 17)
                    /* Q17 */
                    tmp2 = Inlines.silk_LSHIFT32(x[x_offset + subfr_length - n - 1].toInt(), 17)
                    /* Q17 */
                    k = 0
                    while (k < n) {
                        C_first_row[k] = Inlines.silk_MLA(C_first_row[k], x1, x[x_offset + n - k - 1].toInt())
                        /* Q( -rshifts ) */
                        C_last_row[k] = Inlines.silk_MLA(C_last_row[k], x2, x[x_offset + subfr_length - n + k].toInt())
                        /* Q( -rshifts ) */
                        Atmp1 = Inlines.silk_RSHIFT_ROUND(Af_QA[k], QA - 17)
                        /* Q17 */
                        tmp1 = Inlines.silk_MLA(tmp1, x[x_offset + n - k - 1].toInt(), Atmp1)
                        /* Q17 */
                        tmp2 = Inlines.silk_MLA(tmp2, x[x_offset + subfr_length - n + k].toInt(), Atmp1)
                        k++
                        /* Q17 */
                    }
                    tmp1 = -tmp1
                    /* Q17 */
                    tmp2 = -tmp2
                    /* Q17 */
                    k = 0
                    while (k <= n) {
                        CAf[k] = Inlines.silk_SMLAWW(
                            CAf[k], tmp1,
                            Inlines.silk_LSHIFT32(x[x_offset + n - k].toInt(), -rshifts - 1)
                        )
                        /* Q( -rshift ) */
                        CAb[k] = Inlines.silk_SMLAWW(
                            CAb[k], tmp2,
                            Inlines.silk_LSHIFT32(x[x_offset + subfr_length - n + k - 1].toInt(), -rshifts - 1)
                        )
                        k++
                        /* Q( -rshift ) */
                    }
                    s++
                }
            }

            /* Calculate nominator and denominator for the next order reflection (parcor) coefficient */
            tmp1 = C_first_row[n]
            /* Q( -rshifts ) */
            tmp2 = C_last_row[n]
            /* Q( -rshifts ) */
            num = 0
            /* Q( -rshifts ) */
            nrg = Inlines.silk_ADD32(CAb[0], CAf[0])
            /* Q( 1-rshifts ) */
            k = 0
            while (k < n) {
                Atmp_QA = Af_QA[k]
                lz = Inlines.silk_CLZ32(Inlines.silk_abs(Atmp_QA)) - 1
                lz = Inlines.silk_min(32 - QA, lz)
                Atmp1 = Inlines.silk_LSHIFT32(Atmp_QA, lz)
                /* Q( QA + lz ) */

                tmp1 = Inlines.silk_ADD_LSHIFT32(tmp1, Inlines.silk_SMMUL(C_last_row[n - k - 1], Atmp1), 32 - QA - lz)
                /* Q( -rshifts ) */
                tmp2 = Inlines.silk_ADD_LSHIFT32(tmp2, Inlines.silk_SMMUL(C_first_row[n - k - 1], Atmp1), 32 - QA - lz)
                /* Q( -rshifts ) */
                num = Inlines.silk_ADD_LSHIFT32(num, Inlines.silk_SMMUL(CAb[n - k], Atmp1), 32 - QA - lz)
                /* Q( -rshifts ) */
                nrg = Inlines.silk_ADD_LSHIFT32(
                    nrg, Inlines.silk_SMMUL(
                        Inlines.silk_ADD32(CAb[k + 1], CAf[k + 1]),
                        Atmp1
                    ), 32 - QA - lz
                )
                k++
                /* Q( 1-rshifts ) */
            }
            CAf[n + 1] = tmp1
            /* Q( -rshifts ) */
            CAb[n + 1] = tmp2
            /* Q( -rshifts ) */
            num = Inlines.silk_ADD32(num, tmp2)
            /* Q( -rshifts ) */
            num = Inlines.silk_LSHIFT32(-num, 1)
            /* Q( 1-rshifts ) */

            /* Calculate the next order reflection (parcor) coefficient */
            if (Inlines.silk_abs(num) < nrg) {
                rc_Q31 = Inlines.silk_DIV32_varQ(num, nrg, 31)
            } else {
                rc_Q31 = if (num > 0) Int.MAX_VALUE else Int.MIN_VALUE
            }

            /* Update inverse prediction gain */
            tmp1 = (1 shl 30) - Inlines.silk_SMMUL(rc_Q31, rc_Q31)
            tmp1 = Inlines.silk_LSHIFT(Inlines.silk_SMMUL(invGain_Q30, tmp1), 2)
            if (tmp1 <= minInvGain_Q30) {
                /* Max prediction gain exceeded; set reflection coefficient such that max prediction gain is exactly hit */
                tmp2 = (1 shl 30) - Inlines.silk_DIV32_varQ(minInvGain_Q30, invGain_Q30, 30)
                /* Q30 */
                rc_Q31 = Inlines.silk_SQRT_APPROX(tmp2)
                /* Q15 */
                /* Newton-Raphson iteration */
                rc_Q31 = Inlines.silk_RSHIFT32(rc_Q31 + Inlines.silk_DIV32(tmp2, rc_Q31), 1)
                /* Q15 */
                rc_Q31 = Inlines.silk_LSHIFT32(rc_Q31, 16)
                /* Q31 */
                if (num < 0) {
                    /* Ensure adjusted reflection coefficients has the original sign */
                    rc_Q31 = -rc_Q31
                }
                invGain_Q30 = minInvGain_Q30
                reached_max_gain = 1
            } else {
                invGain_Q30 = tmp1
            }

            /* Update the AR coefficients */
            k = 0
            while (k < n + 1 shr 1) {
                tmp1 = Af_QA[k]
                /* QA */
                tmp2 = Af_QA[n - k - 1]
                /* QA */
                Af_QA[k] = Inlines.silk_ADD_LSHIFT32(tmp1, Inlines.silk_SMMUL(tmp2, rc_Q31), 1)
                /* QA */
                Af_QA[n - k - 1] = Inlines.silk_ADD_LSHIFT32(tmp2, Inlines.silk_SMMUL(tmp1, rc_Q31), 1)
                k++
                /* QA */
            }
            Af_QA[n] = Inlines.silk_RSHIFT32(rc_Q31, 31 - QA)
            /* QA */

            if (reached_max_gain != 0) {
                /* Reached max prediction gain; set remaining coefficients to zero and exit loop */
                k = n + 1
                while (k < D) {
                    Af_QA[k] = 0
                    k++
                }
                break
            }

            /* Update C * Af and C * Ab */
            k = 0
            while (k <= n + 1) {
                tmp1 = CAf[k]
                /* Q( -rshifts ) */
                tmp2 = CAb[n - k + 1]
                /* Q( -rshifts ) */
                CAf[k] = Inlines.silk_ADD_LSHIFT32(tmp1, Inlines.silk_SMMUL(tmp2, rc_Q31), 1)
                /* Q( -rshifts ) */
                CAb[n - k + 1] = Inlines.silk_ADD_LSHIFT32(tmp2, Inlines.silk_SMMUL(tmp1, rc_Q31), 1)
                k++
                /* Q( -rshifts ) */
            }
            n++
        }

        if (reached_max_gain != 0) {
            k = 0
            while (k < D) {
                /* Scale coefficients */
                A_Q16[k] = -Inlines.silk_RSHIFT_ROUND(Af_QA[k], QA - 16)
                k++
            }
            /* Subtract energy of preceding samples from C0 */
            if (rshifts > 0) {
                s = 0
                while (s < nb_subfr) {
                    x_offset = x_ptr + s * subfr_length
                    C0 -= Inlines.silk_RSHIFT64(
                        Inlines.silk_inner_prod16_aligned_64(x, x_offset, x, x_offset, D),
                        rshifts
                    ).toInt()
                    s++
                }
            } else {
                s = 0
                while (s < nb_subfr) {
                    x_offset = x_ptr + s * subfr_length
                    C0 -= Inlines.silk_LSHIFT32(Inlines.silk_inner_prod_self(x, x_offset, D), -rshifts)
                    s++
                }
            }
            /* Approximate residual energy */
            res_nrg.Val = Inlines.silk_LSHIFT(Inlines.silk_SMMUL(invGain_Q30, C0), 2)
            res_nrg_Q.Val = 0 - rshifts
        } else {
            /* Return residual energy */
            nrg = CAf[0]
            /* Q( -rshifts ) */
            tmp1 = 1 shl 16
            /* Q16 */
            k = 0
            while (k < D) {
                Atmp1 = Inlines.silk_RSHIFT_ROUND(Af_QA[k], QA - 16)
                /* Q16 */
                nrg = Inlines.silk_SMLAWW(nrg, CAf[k + 1], Atmp1)
                /* Q( -rshifts ) */
                tmp1 = Inlines.silk_SMLAWW(tmp1, Atmp1, Atmp1)
                /* Q16 */
                A_Q16[k] = -Atmp1
                k++
            }
            res_nrg.Val = Inlines.silk_SMLAWW(
                nrg,
                Inlines.silk_SMMUL(
                    (TuningParameters.FIND_LPC_COND_FAC * (1.toLong() shl 32) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.FIND_LPC_COND_FAC, 32)*/,
                    C0
                ),
                -tmp1
            )/* Q( -rshifts ) */
            res_nrg_Q.Val = -rshifts
        }
    }
}
