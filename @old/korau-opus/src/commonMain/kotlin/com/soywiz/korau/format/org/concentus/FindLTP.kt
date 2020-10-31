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

internal object FindLTP {

    /* Head room for correlations */
    private val LTP_CORRS_HEAD_ROOM = 2

    /// <summary>
    /// Finds linear prediction coeffecients and weights
    /// </summary>
    /// <param name="b_Q14"></param>
    /// <param name="WLTP"></param>
    /// <param name="LTPredCodGain_Q7"></param>
    /// <param name="r_lpc"></param>
    /// <param name="lag"></param>
    /// <param name="Wght_Q15"></param>
    /// <param name="subfr_length"></param>
    /// <param name="nb_subfr"></param>
    /// <param name="mem_offset"></param>
    /// <param name="corr_rshifts"></param>
    /// <param name="arch"></param>
    fun silk_find_LTP(
        b_Q14: ShortArray, /* O    LTP coefs [SilkConstants.MAX_NB_SUBFR * SilkConstants.LTP_ORDER]                                                                  */
        WLTP: IntArray, /* O    Weight for LTP quantization [SilkConstants.MAX_NB_SUBFR * SilkConstants.LTP_ORDER * SilkConstants.LTP_ORDER]                                          */
        LTPredCodGain_Q7: BoxedValueInt?, /* O    LTP coding gain                                                             */
        r_lpc: ShortArray, /* I    residual signal after LPC signal + state for first 10 ms                    */
        lag: IntArray, /* I    LTP lags   [SilkConstants.MAX_NB_SUBFR]                                                                 */
        Wght_Q15: IntArray, /* I    weights [SilkConstants.MAX_NB_SUBFR]                                                                    */
        subfr_length: Int, /* I    subframe length                                                             */
        nb_subfr: Int, /* I    number of subframes                                                         */
        mem_offset: Int, /* I    number of samples in LTP memory                                             */
        corr_rshifts: IntArray /* O    right shifts applied to correlations  [SilkConstants.MAX_NB_SUBFR]                                      */
    ) {
        var i: Int
        var k: Int
        var lshift: Int
        var r_ptr: Int
        var lag_ptr: Int
        var b_Q14_ptr: Int

        var regu: Int
        var WLTP_ptr: Int
        val b_Q16 = IntArray(SilkConstants.LTP_ORDER)
        val delta_b_Q14 = IntArray(SilkConstants.LTP_ORDER)
        val d_Q14 = IntArray(SilkConstants.MAX_NB_SUBFR)
        val nrg = IntArray(SilkConstants.MAX_NB_SUBFR)
        var g_Q26: Int
        val w = IntArray(SilkConstants.MAX_NB_SUBFR)
        var WLTP_max: Int
        var max_abs_d_Q14: Int
        var max_w_bits: Int

        var temp32: Int
        var denom32: Int
        var extra_shifts: Int
        var rr_shifts: Int
        var maxRshifts: Int
        val maxRshifts_wxtra: Int
        var LZs: Int
        var LPC_res_nrg: Int
        var LPC_LTP_res_nrg: Int
        val div_Q16: Int
        val Rr = IntArray(SilkConstants.LTP_ORDER)
        val rr = IntArray(SilkConstants.MAX_NB_SUBFR)
        var wd: Int
        val m_Q12: Int

        b_Q14_ptr = 0
        WLTP_ptr = 0
        r_ptr = mem_offset
        k = 0
        while (k < nb_subfr) {
            lag_ptr = r_ptr - (lag[k] + SilkConstants.LTP_ORDER / 2)
            val boxed_rr = BoxedValueInt(0)
            val boxed_rr_shift = BoxedValueInt(0)
            SumSqrShift.silk_sum_sqr_shift(boxed_rr, boxed_rr_shift, r_lpc, r_ptr, subfr_length)
            /* rr[ k ] in Q( -rr_shifts ) */
            rr[k] = boxed_rr.Val
            rr_shifts = boxed_rr_shift.Val

            /* Assure headroom */
            LZs = Inlines.silk_CLZ32(rr[k])
            if (LZs < LTP_CORRS_HEAD_ROOM) {
                rr[k] = Inlines.silk_RSHIFT_ROUND(rr[k], LTP_CORRS_HEAD_ROOM - LZs)
                rr_shifts += LTP_CORRS_HEAD_ROOM - LZs
            }
            corr_rshifts[k] = rr_shifts
            val boxed_shifts = BoxedValueInt(corr_rshifts[k])
            CorrelateMatrix.silk_corrMatrix(
                r_lpc,
                lag_ptr,
                subfr_length,
                SilkConstants.LTP_ORDER,
                LTP_CORRS_HEAD_ROOM,
                WLTP,
                WLTP_ptr,
                boxed_shifts
            )
            /* WLTP_ptr in Q( -corr_rshifts[ k ] ) */
            corr_rshifts[k] = boxed_shifts.Val

            /* The correlation vector always has lower max abs value than rr and/or RR so head room is assured */
            CorrelateMatrix.silk_corrVector(
                r_lpc,
                lag_ptr,
                r_lpc,
                r_ptr,
                subfr_length,
                SilkConstants.LTP_ORDER,
                Rr,
                corr_rshifts[k]
            )
            /* Rr_ptr   in Q( -corr_rshifts[ k ] ) */
            if (corr_rshifts[k] > rr_shifts) {
                rr[k] = Inlines.silk_RSHIFT(rr[k], corr_rshifts[k] - rr_shifts)
                /* rr[ k ] in Q( -corr_rshifts[ k ] ) */
            }
            Inlines.OpusAssert(rr[k] >= 0)

            regu = 1
            regu = Inlines.silk_SMLAWB(
                regu,
                rr[k],
                (TuningParameters.LTP_DAMPING / 3 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LTP_DAMPING / 3, 16)*/
            )
            regu = Inlines.silk_SMLAWB(
                regu,
                Inlines.MatrixGet(WLTP, WLTP_ptr, 0, 0, SilkConstants.LTP_ORDER),
                (TuningParameters.LTP_DAMPING / 3 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LTP_DAMPING / 3, 16)*/
            )
            regu = Inlines.silk_SMLAWB(
                regu,
                Inlines.MatrixGet(
                    WLTP,
                    WLTP_ptr,
                    SilkConstants.LTP_ORDER - 1,
                    SilkConstants.LTP_ORDER - 1,
                    SilkConstants.LTP_ORDER
                ),
                (TuningParameters.LTP_DAMPING / 3 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LTP_DAMPING / 3, 16)*/
            )
            RegularizeCorrelations.silk_regularize_correlations(WLTP, WLTP_ptr, rr, k, regu, SilkConstants.LTP_ORDER)

            LinearAlgebra.silk_solve_LDL(WLTP, WLTP_ptr, SilkConstants.LTP_ORDER, Rr, b_Q16)
            /* WLTP_ptr and Rr_ptr both in Q(-corr_rshifts[k]) */

            /* Limit and store in Q14 */
            silk_fit_LTP(b_Q16, b_Q14, b_Q14_ptr)

            /* Calculate residual energy */
            nrg[k] = ResidualEnergy.silk_residual_energy16_covar(
                b_Q14,
                b_Q14_ptr,
                WLTP,
                WLTP_ptr,
                Rr,
                rr[k],
                SilkConstants.LTP_ORDER,
                14
            )
            /* nrg in Q( -corr_rshifts[ k ] ) */

            /* temp = Wght[ k ] / ( nrg[ k ] * Wght[ k ] + 0.01f * subfr_length ); */
            extra_shifts = Inlines.silk_min_int(corr_rshifts[k], LTP_CORRS_HEAD_ROOM)
            denom32 = Inlines.silk_LSHIFT_SAT32(Inlines.silk_SMULWB(nrg[k], Wght_Q15[k]), 1 + extra_shifts) +
                    /* Q( -corr_rshifts[ k ] + extra_shifts ) */
                    Inlines.silk_RSHIFT(Inlines.silk_SMULWB(subfr_length, 655), corr_rshifts[k] - extra_shifts)
            /* Q( -corr_rshifts[ k ] + extra_shifts ) */
            denom32 = Inlines.silk_max(denom32, 1)
            Inlines.OpusAssert(Wght_Q15[k].toLong() shl 16 < Int.MAX_VALUE)
            /* Wght always < 0.5 in Q0 */
            temp32 = Inlines.silk_DIV32(Inlines.silk_LSHIFT(Wght_Q15[k], 16), denom32)
            /* Q( 15 + 16 + corr_rshifts[k] - extra_shifts ) */
            temp32 = Inlines.silk_RSHIFT(temp32, 31 + corr_rshifts[k] - extra_shifts - 26)
            /* Q26 */

            /* Limit temp such that the below scaling never wraps around */
            WLTP_max = 0
            i = WLTP_ptr
            while (i < WLTP_ptr + SilkConstants.LTP_ORDER * SilkConstants.LTP_ORDER) {
                WLTP_max = Inlines.silk_max(WLTP[i], WLTP_max)
                i++
            }
            lshift = Inlines.silk_CLZ32(WLTP_max) - 1 - 3
            /* keep 3 bits free for vq_nearest_neighbor */
            Inlines.OpusAssert(26 - 18 + lshift >= 0)
            if (26 - 18 + lshift < 31) {
                temp32 = Inlines.silk_min_32(temp32, Inlines.silk_LSHIFT(1, 26 - 18 + lshift))
            }

            Inlines.silk_scale_vector32_Q26_lshift_18(
                WLTP,
                WLTP_ptr,
                temp32,
                SilkConstants.LTP_ORDER * SilkConstants.LTP_ORDER
            )
            /* WLTP_ptr in Q( 18 - corr_rshifts[ k ] ) */

            w[k] = Inlines.MatrixGet(
                WLTP,
                WLTP_ptr,
                SilkConstants.LTP_ORDER / 2,
                SilkConstants.LTP_ORDER / 2,
                SilkConstants.LTP_ORDER
            )
            /* w in Q( 18 - corr_rshifts[ k ] ) */
            Inlines.OpusAssert(w[k] >= 0)

            r_ptr += subfr_length
            b_Q14_ptr += SilkConstants.LTP_ORDER
            WLTP_ptr += SilkConstants.LTP_ORDER * SilkConstants.LTP_ORDER
            k++
        }

        maxRshifts = 0
        k = 0
        while (k < nb_subfr) {
            maxRshifts = Inlines.silk_max_int(corr_rshifts[k], maxRshifts)
            k++
        }

        /* Compute LTP coding gain */
        if (LTPredCodGain_Q7 != null) {
            LPC_LTP_res_nrg = 0
            LPC_res_nrg = 0
            Inlines.OpusAssert(LTP_CORRS_HEAD_ROOM >= 2)
            /* Check that no overflow will happen when adding */
            k = 0
            while (k < nb_subfr) {
                LPC_res_nrg = Inlines.silk_ADD32(
                    LPC_res_nrg,
                    Inlines.silk_RSHIFT(
                        Inlines.silk_ADD32(Inlines.silk_SMULWB(rr[k], Wght_Q15[k]), 1),
                        1 + (maxRshifts - corr_rshifts[k])
                    )
                )
                /* Q( -maxRshifts ) */
                LPC_LTP_res_nrg = Inlines.silk_ADD32(
                    LPC_LTP_res_nrg,
                    Inlines.silk_RSHIFT(
                        Inlines.silk_ADD32(Inlines.silk_SMULWB(nrg[k], Wght_Q15[k]), 1),
                        1 + (maxRshifts - corr_rshifts[k])
                    )
                )
                k++
                /* Q( -maxRshifts ) */
            }
            LPC_LTP_res_nrg = Inlines.silk_max(LPC_LTP_res_nrg, 1)
            /* avoid division by zero */

            div_Q16 = Inlines.silk_DIV32_varQ(LPC_res_nrg, LPC_LTP_res_nrg, 16)
            LTPredCodGain_Q7!!.Val = Inlines.silk_SMULBB(3, Inlines.silk_lin2log(div_Q16) - (16 shl 7)).toInt()

            Inlines.OpusAssert(
                LTPredCodGain_Q7!!.Val == Inlines.silk_SAT16(
                    Inlines.silk_MUL(
                        3,
                        Inlines.silk_lin2log(div_Q16) - (16 shl 7)
                    )
                ).toInt()
            )
        }

        /* smoothing */
        /* d = sum( B, 1 ); */
        b_Q14_ptr = 0
        k = 0
        while (k < nb_subfr) {
            d_Q14[k] = 0
            i = b_Q14_ptr
            while (i < b_Q14_ptr + SilkConstants.LTP_ORDER) {
                d_Q14[k] += b_Q14[i].toInt()
                i++
            }
            b_Q14_ptr += SilkConstants.LTP_ORDER
            k++
        }

        /* m = ( w * d' ) / ( sum( w ) + 1e-3 ); */

        /* Find maximum absolute value of d_Q14 and the bits used by w in Q0 */
        max_abs_d_Q14 = 0
        max_w_bits = 0
        k = 0
        while (k < nb_subfr) {
            max_abs_d_Q14 = Inlines.silk_max_32(max_abs_d_Q14, Inlines.silk_abs(d_Q14[k]))
            /* w[ k ] is in Q( 18 - corr_rshifts[ k ] ) */
            /* Find bits needed in Q( 18 - maxRshifts ) */
            max_w_bits = Inlines.silk_max_32(max_w_bits, 32 - Inlines.silk_CLZ32(w[k]) + corr_rshifts[k] - maxRshifts)
            k++
        }

        /* max_abs_d_Q14 = (5 << 15); worst case, i.e. SilkConstants.LTP_ORDER * -silk_int16_MIN */
        Inlines.OpusAssert(max_abs_d_Q14 <= 5 shl 15)

        /* How many bits is needed for w*d' in Q( 18 - maxRshifts ) in the worst case, of all d_Q14's being equal to max_abs_d_Q14 */
        extra_shifts = max_w_bits + 32 - Inlines.silk_CLZ32(max_abs_d_Q14) - 14

        /* Subtract what we got available; bits in output var plus maxRshifts */
        extra_shifts -= 32 - 1 - 2 + maxRshifts
        /* Keep sign bit free as well as 2 bits for accumulation */
        extra_shifts = Inlines.silk_max_int(extra_shifts, 0)

        maxRshifts_wxtra = maxRshifts + extra_shifts

        temp32 = Inlines.silk_RSHIFT(262, maxRshifts + extra_shifts) + 1
        /* 1e-3f in Q( 18 - (maxRshifts + extra_shifts) ) */
        wd = 0
        k = 0
        while (k < nb_subfr) {
            /* w has at least 2 bits of headroom so no overflow should happen */
            temp32 = Inlines.silk_ADD32(temp32, Inlines.silk_RSHIFT(w[k], maxRshifts_wxtra - corr_rshifts[k]))
            /* Q( 18 - maxRshifts_wxtra ) */
            wd = Inlines.silk_ADD32(
                wd,
                Inlines.silk_LSHIFT(
                    Inlines.silk_SMULWW(
                        Inlines.silk_RSHIFT(w[k], maxRshifts_wxtra - corr_rshifts[k]),
                        d_Q14[k]
                    ), 2
                )
            )
            k++
            /* Q( 18 - maxRshifts_wxtra ) */
        }
        m_Q12 = Inlines.silk_DIV32_varQ(wd, temp32, 12)

        b_Q14_ptr = 0
        k = 0
        while (k < nb_subfr) {
            /* w[ k ] from Q( 18 - corr_rshifts[ k ] ) to Q( 16 ) */
            if (2 - corr_rshifts[k] > 0) {
                temp32 = Inlines.silk_RSHIFT(w[k], 2 - corr_rshifts[k])
            } else {
                temp32 = Inlines.silk_LSHIFT_SAT32(w[k], corr_rshifts[k] - 2)
            }

            g_Q26 = Inlines.silk_MUL(
                Inlines.silk_DIV32(
                    (TuningParameters.LTP_SMOOTHING * (1.toLong() shl 26) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LTP_SMOOTHING, 26)*/,
                    Inlines.silk_RSHIFT(
                        (TuningParameters.LTP_SMOOTHING * (1.toLong() shl 26) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LTP_SMOOTHING, 26)*/,
                        10
                    ) + temp32
                ), /* Q10 */
                Inlines.silk_LSHIFT_SAT32(Inlines.silk_SUB_SAT32(m_Q12, Inlines.silk_RSHIFT(d_Q14[k], 2)), 4)
            )
            /* Q16 */

            temp32 = 0
            i = 0
            while (i < SilkConstants.LTP_ORDER) {
                delta_b_Q14[i] = Inlines.silk_max_16(b_Q14[b_Q14_ptr + i], 1638.toShort()).toInt()
                /* 1638_Q14 = 0.1_Q0 */
                temp32 += delta_b_Q14[i]
                i++
                /* Q14 */
            }
            temp32 = Inlines.silk_DIV32(g_Q26, temp32)
            /* Q14 . Q12 */
            i = 0
            while (i < SilkConstants.LTP_ORDER) {
                b_Q14[b_Q14_ptr + i] = Inlines.silk_LIMIT_32(
                    b_Q14[b_Q14_ptr + i].toInt() + Inlines.silk_SMULWB(
                        Inlines.silk_LSHIFT_SAT32(
                            temp32,
                            4
                        ), delta_b_Q14[i]
                    ), -16000, 28000
                ).toShort()
                i++
            }
            b_Q14_ptr += SilkConstants.LTP_ORDER
            k++
        }
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="LTP_coefs_Q16">[SilkConstants.LTP_ORDER]</param>
    /// <param name="LTP_coefs_Q14">[SilkConstants.LTP_ORDER]</param>
    /// <param name=""></param>
    fun silk_fit_LTP(
        LTP_coefs_Q16: IntArray,
        LTP_coefs_Q14: ShortArray,
        LTP_coefs_Q14_ptr: Int
    ) {
        var i: Int

        i = 0
        while (i < SilkConstants.LTP_ORDER) {
            LTP_coefs_Q14[LTP_coefs_Q14_ptr + i] =
                    Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(LTP_coefs_Q16[i], 2)).toShort()
            i++
        }
    }
}
