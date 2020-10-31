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

internal object Filters {

    /* Coefficients for 2-band filter bank based on first-order allpass filters */
    private val A_fb1_20 = (5394 shl 1).toShort()
    private val A_fb1_21: Short = -24290

    private val QA = 24
    private val A_LIMIT = (0.99975f * (1.toLong() shl QA) + 0.5).toInt()/*Inlines.SILK_CONST(0.99975f, QA)*/

    fun silk_warped_LPC_analysis_filter(
        state: IntArray, /* I/O  State [order + 1]                   */
        res_Q2: IntArray, /* O    Residual signal [length]            */
        coef_Q13: ShortArray, /* I    Coefficients [order]                */
        coef_Q13_ptr: Int,
        input: ShortArray, /* I    Input signal [length]               */
        input_ptr: Int,
        lambda_Q16: Short, /* I    Warping factor                      */
        length: Int, /* I    Length of input signal              */
        order: Int /* I    Filter order (even)                 */
    ) {
        var n: Int
        var i: Int
        var acc_Q11: Int
        var tmp1: Int
        var tmp2: Int

        /* Order must be even */
        Inlines.OpusAssert(order and 1 == 0)

        n = 0
        while (n < length) {
            /* Output of lowpass section */
            tmp2 = Inlines.silk_SMLAWB(state[0], state[1], lambda_Q16.toInt())
            state[0] = Inlines.silk_LSHIFT(input[input_ptr + n].toInt(), 14)
            /* Output of allpass section */
            tmp1 = Inlines.silk_SMLAWB(state[1], state[2] - tmp2, lambda_Q16.toInt())
            state[1] = tmp2
            acc_Q11 = Inlines.silk_RSHIFT(order, 1)
            acc_Q11 = Inlines.silk_SMLAWB(acc_Q11, tmp2, coef_Q13[coef_Q13_ptr].toInt())
            /* Loop over allpass sections */
            i = 2
            while (i < order) {
                /* Output of allpass section */
                tmp2 = Inlines.silk_SMLAWB(state[i], state[i + 1] - tmp1, lambda_Q16.toInt())
                state[i] = tmp1
                acc_Q11 = Inlines.silk_SMLAWB(acc_Q11, tmp1, coef_Q13[coef_Q13_ptr + i - 1].toInt())
                /* Output of allpass section */
                tmp1 = Inlines.silk_SMLAWB(state[i + 1], state[i + 2] - tmp2, lambda_Q16.toInt())
                state[i + 1] = tmp2
                acc_Q11 = Inlines.silk_SMLAWB(acc_Q11, tmp2, coef_Q13[coef_Q13_ptr + i].toInt())
                i += 2
            }
            state[order] = tmp1
            acc_Q11 = Inlines.silk_SMLAWB(acc_Q11, tmp1, coef_Q13[coef_Q13_ptr + order - 1].toInt())
            res_Q2[n] = Inlines.silk_LSHIFT(input[input_ptr + n].toInt(), 2) - Inlines.silk_RSHIFT_ROUND(acc_Q11, 9)
            n++
        }
    }

    fun silk_prefilter(
        psEnc: SilkChannelEncoder, /* I/O  Encoder state                                                               */
        psEncCtrl: SilkEncoderControl, /* I    Encoder control                                                             */
        xw_Q3: IntArray, /* O    Weighted signal                                                             */
        x: ShortArray, /* I    Speech signal                                                               */
        x_ptr: Int
    ) {
        val P = psEnc.sPrefilt
        var j: Int
        var k: Int
        var lag: Int
        var tmp_32: Int
        var AR1_shp_Q13: Int
        var px: Int
        var pxw_Q3: Int
        var HarmShapeGain_Q12: Int
        var Tilt_Q14: Int
        var HarmShapeFIRPacked_Q12: Int
        var LF_shp_Q14: Int
        val x_filt_Q12: IntArray
        val st_res_Q2: IntArray
        val B_Q10 = ShortArray(2)

        /* Set up pointers */
        px = x_ptr
        pxw_Q3 = 0
        lag = P.lagPrev
        x_filt_Q12 = IntArray(psEnc.subfr_length)
        st_res_Q2 = IntArray(psEnc.subfr_length)
        k = 0
        while (k < psEnc.nb_subfr) {
            /* Update Variables that change per sub frame */
            if (psEnc.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
                lag = psEncCtrl.pitchL[k]
            }

            /* Noise shape parameters */
            HarmShapeGain_Q12 =
                    Inlines.silk_SMULWB(psEncCtrl.HarmShapeGain_Q14[k].toInt(), 16384 - psEncCtrl.HarmBoost_Q14[k])
            Inlines.OpusAssert(HarmShapeGain_Q12 >= 0)
            HarmShapeFIRPacked_Q12 = Inlines.silk_RSHIFT(HarmShapeGain_Q12, 2)
            HarmShapeFIRPacked_Q12 = HarmShapeFIRPacked_Q12 or
                    Inlines.silk_LSHIFT(Inlines.silk_RSHIFT(HarmShapeGain_Q12, 1).toInt(), 16)
            Tilt_Q14 = psEncCtrl.Tilt_Q14[k]
            LF_shp_Q14 = psEncCtrl.LF_shp_Q14[k]
            AR1_shp_Q13 = k * SilkConstants.MAX_SHAPE_LPC_ORDER

            /* Short term FIR filtering*/
            silk_warped_LPC_analysis_filter(
                P.sAR_shp, st_res_Q2, psEncCtrl.AR1_Q13, AR1_shp_Q13, x, px,
                psEnc.warping_Q16.toShort(), psEnc.subfr_length, psEnc.shapingLPCOrder
            )

            /* Reduce (mainly) low frequencies during harmonic emphasis */
            B_Q10[0] = Inlines.silk_RSHIFT_ROUND(psEncCtrl.GainsPre_Q14[k], 4).toShort()
            tmp_32 = Inlines.silk_SMLABB(
                (TuningParameters.INPUT_TILT * (1.toLong() shl 26) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.INPUT_TILT, 26)*/,
                psEncCtrl.HarmBoost_Q14[k],
                HarmShapeGain_Q12
            )
            /* Q26 */
            tmp_32 = Inlines.silk_SMLABB(
                tmp_32,
                psEncCtrl.coding_quality_Q14,
                (TuningParameters.HIGH_RATE_INPUT_TILT * (1.toLong() shl 12) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HIGH_RATE_INPUT_TILT, 12)*/
            )
            /* Q26 */
            tmp_32 = Inlines.silk_SMULWB(tmp_32, -psEncCtrl.GainsPre_Q14[k])
            /* Q24 */
            tmp_32 = Inlines.silk_RSHIFT_ROUND(tmp_32, 14)
            /* Q10 */
            B_Q10[1] = Inlines.silk_SAT16(tmp_32).toShort()
            x_filt_Q12[0] =
                    Inlines.silk_MLA(Inlines.silk_MUL(st_res_Q2[0], B_Q10[0].toInt()), P.sHarmHP_Q2, B_Q10[1].toInt())
            j = 1
            while (j < psEnc.subfr_length) {
                x_filt_Q12[j] = Inlines.silk_MLA(
                    Inlines.silk_MUL(st_res_Q2[j], B_Q10[0].toInt()),
                    st_res_Q2[j - 1],
                    B_Q10[1].toInt()
                )
                j++
            }
            P.sHarmHP_Q2 = st_res_Q2[psEnc.subfr_length - 1]

            silk_prefilt(
                P,
                x_filt_Q12,
                xw_Q3,
                pxw_Q3,
                HarmShapeFIRPacked_Q12,
                Tilt_Q14,
                LF_shp_Q14,
                lag,
                psEnc.subfr_length
            )

            px += psEnc.subfr_length
            pxw_Q3 += psEnc.subfr_length
            k++
        }

        P.lagPrev = psEncCtrl.pitchL[psEnc.nb_subfr - 1]

    }

    /* Prefilter for finding Quantizer input signal */
    fun silk_prefilt(
        P: SilkPrefilterState, /* I/O  state                               */
        st_res_Q12: IntArray, /* I    short term residual signal          */
        xw_Q3: IntArray, /* O    prefiltered signal                  */
        xw_Q3_ptr: Int,
        HarmShapeFIRPacked_Q12: Int, /* I    Harmonic shaping coeficients        */
        Tilt_Q14: Int, /* I    Tilt shaping coeficient             */
        LF_shp_Q14: Int, /* I    Low-frequancy shaping coeficients   */
        lag: Int, /* I    Lag for harmonic shaping            */
        length: Int /* I    Length of signals                   */
    ) {
        var i: Int
        var idx: Int
        var LTP_shp_buf_idx: Int
        var n_LTP_Q12: Int
        var n_Tilt_Q10: Int
        var n_LF_Q10: Int
        var sLF_MA_shp_Q12: Int
        var sLF_AR_shp_Q12: Int
        val LTP_shp_buf: ShortArray

        /* To speed up use temp variables instead of using the struct */
        LTP_shp_buf = P.sLTP_shp
        LTP_shp_buf_idx = P.sLTP_shp_buf_idx
        sLF_AR_shp_Q12 = P.sLF_AR_shp_Q12
        sLF_MA_shp_Q12 = P.sLF_MA_shp_Q12

        i = 0
        while (i < length) {
            if (lag > 0) {
                /* unrolled loop */
                Inlines.OpusAssert(SilkConstants.HARM_SHAPE_FIR_TAPS == 3)
                idx = lag + LTP_shp_buf_idx
                n_LTP_Q12 = Inlines.silk_SMULBB(
                    LTP_shp_buf[idx - SilkConstants.HARM_SHAPE_FIR_TAPS / 2 - 1 and SilkConstants.LTP_MASK].toInt(),
                    HarmShapeFIRPacked_Q12
                )
                n_LTP_Q12 = Inlines.silk_SMLABT(
                    n_LTP_Q12,
                    LTP_shp_buf[idx - SilkConstants.HARM_SHAPE_FIR_TAPS / 2 and SilkConstants.LTP_MASK].toInt(),
                    HarmShapeFIRPacked_Q12
                )
                n_LTP_Q12 = Inlines.silk_SMLABB(
                    n_LTP_Q12,
                    LTP_shp_buf[idx - SilkConstants.HARM_SHAPE_FIR_TAPS / 2 + 1 and SilkConstants.LTP_MASK].toInt(),
                    HarmShapeFIRPacked_Q12
                )
            } else {
                n_LTP_Q12 = 0
            }

            n_Tilt_Q10 = Inlines.silk_SMULWB(sLF_AR_shp_Q12, Tilt_Q14)
            n_LF_Q10 = Inlines.silk_SMLAWB(Inlines.silk_SMULWT(sLF_AR_shp_Q12, LF_shp_Q14), sLF_MA_shp_Q12, LF_shp_Q14)

            sLF_AR_shp_Q12 = Inlines.silk_SUB32(st_res_Q12[i], Inlines.silk_LSHIFT(n_Tilt_Q10, 2))
            sLF_MA_shp_Q12 = Inlines.silk_SUB32(sLF_AR_shp_Q12, Inlines.silk_LSHIFT(n_LF_Q10, 2))

            LTP_shp_buf_idx = LTP_shp_buf_idx - 1 and SilkConstants.LTP_MASK
            LTP_shp_buf[LTP_shp_buf_idx] = Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(sLF_MA_shp_Q12, 12)).toShort()

            xw_Q3[xw_Q3_ptr + i] = Inlines.silk_RSHIFT_ROUND(Inlines.silk_SUB32(sLF_MA_shp_Q12, n_LTP_Q12), 9)
            i++
        }

        /* Copy temp variable back to state */
        P.sLF_AR_shp_Q12 = sLF_AR_shp_Q12
        P.sLF_MA_shp_Q12 = sLF_MA_shp_Q12
        P.sLTP_shp_buf_idx = LTP_shp_buf_idx
    }

    /// <summary>
    /// Second order ARMA filter, alternative implementation
    /// </summary>
    /// <param name="input">I     input signal</param>
    /// <param name="B_Q28">I     MA coefficients [3]</param>
    /// <param name="A_Q28">I     AR coefficients [2]</param>
    /// <param name="S">I/O   State vector [2]</param>
    /// <param name="output">O     output signal</param>
    /// <param name="len">I     signal length (must be even)</param>
    /// <param name="stride">I     Operate on interleaved signal if > 1</param>
    fun silk_biquad_alt(
        input: ShortArray,
        input_ptr: Int,
        B_Q28: IntArray,
        A_Q28: IntArray,
        S: IntArray,
        output: ShortArray,
        output_ptr: Int,
        len: Int,
        stride: Int
    ) {
        /* DIRECT FORM II TRANSPOSED (uses 2 element state vector) */
        var k: Int
        var inval: Int
        val A0_U_Q28: Int
        val A0_L_Q28: Int
        val A1_U_Q28: Int
        val A1_L_Q28: Int
        var out32_Q14: Int

        /* Negate A_Q28 values and split in two parts */
        A0_L_Q28 = -A_Q28[0] and 0x00003FFF
        /* lower part */
        A0_U_Q28 = Inlines.silk_RSHIFT(-A_Q28[0], 14)
        /* upper part */
        A1_L_Q28 = -A_Q28[1] and 0x00003FFF
        /* lower part */
        A1_U_Q28 = Inlines.silk_RSHIFT(-A_Q28[1], 14)
        /* upper part */

        k = 0
        while (k < len) {
            /* S[ 0 ], S[ 1 ]: Q12 */
            inval = input[input_ptr + k * stride].toInt()
            out32_Q14 = Inlines.silk_LSHIFT(Inlines.silk_SMLAWB(S[0], B_Q28[0], inval), 2)

            S[0] = S[1] + Inlines.silk_RSHIFT_ROUND(Inlines.silk_SMULWB(out32_Q14, A0_L_Q28), 14)
            S[0] = Inlines.silk_SMLAWB(S[0], out32_Q14, A0_U_Q28)
            S[0] = Inlines.silk_SMLAWB(S[0], B_Q28[1], inval)

            S[1] = Inlines.silk_RSHIFT_ROUND(Inlines.silk_SMULWB(out32_Q14, A1_L_Q28), 14)
            S[1] = Inlines.silk_SMLAWB(S[1], out32_Q14, A1_U_Q28)
            S[1] = Inlines.silk_SMLAWB(S[1], B_Q28[2], inval)

            /* Scale back to Q0 and saturate */
            output[output_ptr + k * stride] =
                    Inlines.silk_SAT16(Inlines.silk_RSHIFT(out32_Q14 + (1 shl 14) - 1, 14)).toShort()
            k++
        }
    }

    fun silk_biquad_alt(
        input: ShortArray,
        input_ptr: Int,
        B_Q28: IntArray,
        A_Q28: IntArray,
        S: IntArray,
        S_ptr: Int,
        output: ShortArray,
        output_ptr: Int,
        len: Int,
        stride: Int
    ) {
        /* DIRECT FORM II TRANSPOSED (uses 2 element state vector) */
        var k: Int
        var inval: Int
        val A0_U_Q28: Int
        val A0_L_Q28: Int
        val A1_U_Q28: Int
        val A1_L_Q28: Int
        var out32_Q14: Int

        /* Negate A_Q28 values and split in two parts */
        A0_L_Q28 = -A_Q28[0] and 0x00003FFF
        /* lower part */
        A0_U_Q28 = Inlines.silk_RSHIFT(-A_Q28[0], 14)
        /* upper part */
        A1_L_Q28 = -A_Q28[1] and 0x00003FFF
        /* lower part */
        A1_U_Q28 = Inlines.silk_RSHIFT(-A_Q28[1], 14)
        /* upper part */

        k = 0
        while (k < len) {
            val s1 = S_ptr + 1
            /* S[ 0 ], S[ 1 ]: Q12 */
            inval = input[input_ptr + k * stride].toInt()
            out32_Q14 = Inlines.silk_LSHIFT(Inlines.silk_SMLAWB(S[S_ptr], B_Q28[0], inval), 2)

            S[S_ptr] = S[s1] + Inlines.silk_RSHIFT_ROUND(Inlines.silk_SMULWB(out32_Q14, A0_L_Q28), 14)
            S[S_ptr] = Inlines.silk_SMLAWB(S[S_ptr], out32_Q14, A0_U_Q28)
            S[S_ptr] = Inlines.silk_SMLAWB(S[S_ptr], B_Q28[1], inval)

            S[s1] = Inlines.silk_RSHIFT_ROUND(Inlines.silk_SMULWB(out32_Q14, A1_L_Q28), 14)
            S[s1] = Inlines.silk_SMLAWB(S[s1], out32_Q14, A1_U_Q28)
            S[s1] = Inlines.silk_SMLAWB(S[s1], B_Q28[2], inval)

            /* Scale back to Q0 and saturate */
            output[output_ptr + k * stride] =
                    Inlines.silk_SAT16(Inlines.silk_RSHIFT(out32_Q14 + (1 shl 14) - 1, 14)).toShort()
            k++
        }
    }

    /* (opus_int16)(20623 << 1) */

    /// <summary>
    /// Split signal into two decimated bands using first-order allpass filters
    /// </summary>
    /// <param name="input">I    Input signal [N]</param>
    /// <param name="S">I/O  State vector [2]</param>
    /// <param name="outL">O    Low band [N/2]</param>
    /// <param name="outH">O    High band [N/2]</param>
    /// <param name="N">I    Number of input samples</param>
    fun silk_ana_filt_bank_1(
        input: ShortArray,
        input_ptr: Int,
        S: IntArray,
        outL: ShortArray,
        outH: ShortArray,
        outH_ptr: Int,
        N: Int
    ) {
        var k: Int
        val N2 = Inlines.silk_RSHIFT(N, 1)
        var in32: Int
        var X: Int
        var Y: Int
        var out_1: Int
        var out_2: Int

        /* Internal variables and state are in Q10 format */
        k = 0
        while (k < N2) {
            /* Convert to Q10 */
            in32 = Inlines.silk_LSHIFT(input[input_ptr + 2 * k].toInt(), 10)

            /* All-pass section for even input sample */
            Y = Inlines.silk_SUB32(in32, S[0])
            X = Inlines.silk_SMLAWB(Y, Y, A_fb1_21.toInt())
            out_1 = Inlines.silk_ADD32(S[0], X)
            S[0] = Inlines.silk_ADD32(in32, X)

            /* Convert to Q10 */
            in32 = Inlines.silk_LSHIFT(input[input_ptr + 2 * k + 1].toInt(), 10)

            /* All-pass section for odd input sample, and add to output of previous section */
            Y = Inlines.silk_SUB32(in32, S[1])
            X = Inlines.silk_SMULWB(Y, A_fb1_20.toInt())
            out_2 = Inlines.silk_ADD32(S[1], X)
            S[1] = Inlines.silk_ADD32(in32, X)

            /* Add/subtract, convert back to int16 and store to output */
            outL[k] = Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(Inlines.silk_ADD32(out_2, out_1), 11)).toShort()
            outH[outH_ptr + k] =
                    Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(Inlines.silk_SUB32(out_2, out_1), 11)).toShort()
            k++
        }
    }

    /// <summary>
    /// Chirp (bandwidth expand) LP AR filter
    /// </summary>
    /// <param name="ar">I/O  AR filter to be expanded (without leading 1)</param>
    /// <param name="d">I    Length of ar</param>
    /// <param name="chirp_Q16">I    Chirp factor in Q16</param>
    fun silk_bwexpander_32(ar: IntArray, d: Int, chirp_Q16: Int) {
        var chirp_Q16 = chirp_Q16
        var i: Int
        val chirp_minus_one_Q16 = chirp_Q16 - 65536

        i = 0
        while (i < d - 1) {
            ar[i] = Inlines.silk_SMULWW(chirp_Q16, ar[i])
            chirp_Q16 += Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, chirp_minus_one_Q16), 16)
            i++
        }

        ar[d - 1] = Inlines.silk_SMULWW(chirp_Q16, ar[d - 1])
    }

    /// <summary>
    /// Elliptic/Cauer filters designed with 0.1 dB passband ripple,
    /// 80 dB minimum stopband attenuation, and
    /// [0.95 : 0.15 : 0.35] normalized cut off frequencies.
    /// Helper function, interpolates the filter taps
    /// </summary>
    /// <param name="B_Q28">order [TRANSITION_NB]</param>
    /// <param name="A_Q28">order [TRANSITION_NA]</param>
    /// <param name="ind"></param>
    /// <param name="fac_Q16"></param>
    fun silk_LP_interpolate_filter_taps(
        B_Q28: IntArray,
        A_Q28: IntArray,
        ind: Int,
        fac_Q16: Int
    ) {
        var nb: Int
        var na: Int

        if (ind < SilkConstants.TRANSITION_INT_NUM - 1) {
            if (fac_Q16 > 0) {
                if (fac_Q16 < 32768) {
                    /* fac_Q16 is in range of a 16-bit int */
                    /* Piece-wise linear interpolation of B and A */
                    nb = 0
                    while (nb < SilkConstants.TRANSITION_NB) {
                        B_Q28[nb] = Inlines.silk_SMLAWB(
                            SilkTables.silk_Transition_LP_B_Q28[ind][nb],
                            SilkTables.silk_Transition_LP_B_Q28[ind + 1][nb] - SilkTables.silk_Transition_LP_B_Q28[ind][nb],
                            fac_Q16
                        )
                        nb++
                    }

                    na = 0
                    while (na < SilkConstants.TRANSITION_NA) {
                        A_Q28[na] = Inlines.silk_SMLAWB(
                            SilkTables.silk_Transition_LP_A_Q28[ind][na],
                            SilkTables.silk_Transition_LP_A_Q28[ind + 1][na] - SilkTables.silk_Transition_LP_A_Q28[ind][na],
                            fac_Q16
                        )
                        na++
                    }
                } else {
                    /* ( fac_Q16 - ( 1 << 16 ) ) is in range of a 16-bit int */
                    Inlines.OpusAssert(fac_Q16 - (1 shl 16) == Inlines.silk_SAT16(fac_Q16 - (1 shl 16)))

                    /* Piece-wise linear interpolation of B and A */
                    nb = 0
                    while (nb < SilkConstants.TRANSITION_NB) {
                        B_Q28[nb] = Inlines.silk_SMLAWB(
                            SilkTables.silk_Transition_LP_B_Q28[ind + 1][nb],
                            SilkTables.silk_Transition_LP_B_Q28[ind + 1][nb] - SilkTables.silk_Transition_LP_B_Q28[ind][nb],
                            fac_Q16 - (1 shl 16)
                        )
                        nb++
                    }

                    na = 0
                    while (na < SilkConstants.TRANSITION_NA) {
                        A_Q28[na] = Inlines.silk_SMLAWB(
                            SilkTables.silk_Transition_LP_A_Q28[ind + 1][na],
                            SilkTables.silk_Transition_LP_A_Q28[ind + 1][na] - SilkTables.silk_Transition_LP_A_Q28[ind][na],
                            fac_Q16 - (1 shl 16)
                        )
                        na++
                    }
                }
            } else {
                arraycopy(SilkTables.silk_Transition_LP_B_Q28[ind], 0, B_Q28, 0, SilkConstants.TRANSITION_NB)
                arraycopy(SilkTables.silk_Transition_LP_A_Q28[ind], 0, A_Q28, 0, SilkConstants.TRANSITION_NA)
            }
        } else {
            arraycopy(
                SilkTables.silk_Transition_LP_B_Q28[SilkConstants.TRANSITION_INT_NUM - 1],
                0,
                B_Q28,
                0,
                SilkConstants.TRANSITION_NB
            )
            arraycopy(
                SilkTables.silk_Transition_LP_A_Q28[SilkConstants.TRANSITION_INT_NUM - 1],
                0,
                A_Q28,
                0,
                SilkConstants.TRANSITION_NA
            )
        }
    }

    /// <summary>
    /// LPC analysis filter
    /// NB! State is kept internally and the
    /// filter always starts with zero state
    /// first d output samples are set to zero
    /// </summary>
    /// <param name="output">O    Output signal</param>
    /// <param name="input">I    Input signal</param>
    /// <param name="B">I    MA prediction coefficients, Q12 [order]</param>
    /// <param name="len">I    Signal length</param>
    /// <param name="d">I    Filter order</param>
    /// <param name="arch">I    Run-time architecture</param>
    fun silk_LPC_analysis_filter(
        output: ShortArray,
        output_ptr: Int,
        input: ShortArray,
        input_ptr: Int,
        B: ShortArray,
        B_ptr: Int,
        len: Int,
        d: Int
    ) {
        var j: Int

        val mem = ShortArray(SilkConstants.SILK_MAX_ORDER_LPC)
        val num = ShortArray(SilkConstants.SILK_MAX_ORDER_LPC)

        Inlines.OpusAssert(d >= 6)
        Inlines.OpusAssert(d and 1 == 0)
        Inlines.OpusAssert(d <= len)

        Inlines.OpusAssert(d <= SilkConstants.SILK_MAX_ORDER_LPC)
        j = 0
        while (j < d) {
            num[j] = (0 - B[B_ptr + j]).toShort()
            j++
        }
        j = 0
        while (j < d) {
            mem[j] = input[input_ptr + d - j - 1]
            j++
        }
        Kernels.celt_fir(input, input_ptr + d, num, output, output_ptr + d, len - d, d, mem)
        j = output_ptr
        while (j < output_ptr + d) {
            output[j] = 0
            j++
        }
    }

    /// <summary>
    /// Compute inverse of LPC prediction gain, and
    /// test if LPC coefficients are stable (all poles within unit circle)
    /// </summary>
    /// <param name="A_QA">Prediction coefficients, order [2][SILK_MAX_ORDER_LPC]</param>
    /// <param name="order">Prediction order</param>
    /// <returns>inverse prediction gain in energy domain, Q30</returns>
    fun LPC_inverse_pred_gain_QA(
        A_QA: Array<IntArray>,
        order: Int
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

    /// <summary>
    /// For input in Q12 domain
    /// </summary>
    /// <param name="A_Q12">Prediction coefficients, Q12 [order]</param>
    /// <param name="order">I   Prediction order</param>
    /// <returns>inverse prediction gain in energy domain, Q30</returns>
    fun silk_LPC_inverse_pred_gain(A_Q12: ShortArray, order: Int): Int {
        var k: Int
        val Atmp_QA = Array<IntArray>(2) { intArrayOf() }
        Atmp_QA[0] = IntArray(order)
        Atmp_QA[1] = IntArray(order)
        val Anew_QA: IntArray
        var DC_resp = 0

        Anew_QA = Atmp_QA!![order and 1]!!

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
        } else LPC_inverse_pred_gain_QA(Atmp_QA!!, order)

    }
}
