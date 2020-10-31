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

internal object NoiseShapeAnalysis {

    /* Compute gain to make warped filter coefficients have a zero mean log frequency response on a   */
    /* non-warped frequency scale. (So that it can be implemented with a minimum-phase monic filter.) */
    /* Note: A monic filter is one with the first coefficient equal to 1.0. In Silk we omit the first */
    /* coefficient in an array of coefficients, for monic filters.                                    */
    fun warped_gain( /* gain in Q16*/
        coefs_Q24: IntArray,
        lambda_Q16: Int,
        order: Int
    ): Int {
        var lambda_Q16 = lambda_Q16
        var i: Int
        var gain_Q24: Int

        lambda_Q16 = -lambda_Q16
        gain_Q24 = coefs_Q24[order - 1]
        i = order - 2
        while (i >= 0) {
            gain_Q24 = Inlines.silk_SMLAWB(coefs_Q24[i], gain_Q24, lambda_Q16)
            i--
        }
        gain_Q24 = Inlines.silk_SMLAWB(
            (1.0f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 24)*/,
            gain_Q24,
            -lambda_Q16
        )
        return Inlines.silk_INVERSE32_varQ(gain_Q24, 40)
    }

    /* Convert warped filter coefficients to monic pseudo-warped coefficients and limit maximum     */
    /* amplitude of monic warped coefficients by using bandwidth expansion on the true coefficients */
    fun limit_warped_coefs(
        coefs_syn_Q24: IntArray,
        coefs_ana_Q24: IntArray,
        lambda_Q16: Int,
        limit_Q24: Int,
        order: Int
    ) {
        var lambda_Q16 = lambda_Q16
        var i: Int
        var iter: Int
        var ind = 0
        var tmp: Int
        var maxabs_Q24: Int
        var chirp_Q16: Int
        var gain_syn_Q16: Int
        var gain_ana_Q16: Int
        var nom_Q16: Int
        var den_Q24: Int

        /* Convert to monic coefficients */
        lambda_Q16 = -lambda_Q16
        i = order - 1
        while (i > 0) {
            coefs_syn_Q24[i - 1] = Inlines.silk_SMLAWB(coefs_syn_Q24[i - 1], coefs_syn_Q24[i], lambda_Q16)
            coefs_ana_Q24[i - 1] = Inlines.silk_SMLAWB(coefs_ana_Q24[i - 1], coefs_ana_Q24[i], lambda_Q16)
            i--
        }
        lambda_Q16 = -lambda_Q16
        nom_Q16 = Inlines.silk_SMLAWB(
            (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/,
            -lambda_Q16,
            lambda_Q16
        )
        den_Q24 = Inlines.silk_SMLAWB(
            (1.0f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 24)*/,
            coefs_syn_Q24[0],
            lambda_Q16
        )
        gain_syn_Q16 = Inlines.silk_DIV32_varQ(nom_Q16, den_Q24, 24)
        den_Q24 = Inlines.silk_SMLAWB(
            (1.0f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 24)*/,
            coefs_ana_Q24[0],
            lambda_Q16
        )
        gain_ana_Q16 = Inlines.silk_DIV32_varQ(nom_Q16, den_Q24, 24)
        i = 0
        while (i < order) {
            coefs_syn_Q24[i] = Inlines.silk_SMULWW(gain_syn_Q16, coefs_syn_Q24[i])
            coefs_ana_Q24[i] = Inlines.silk_SMULWW(gain_ana_Q16, coefs_ana_Q24[i])
            i++
        }

        iter = 0
        while (iter < 10) {
            /* Find maximum absolute value */
            maxabs_Q24 = -1
            i = 0
            while (i < order) {
                tmp = Inlines.silk_max(
                    Inlines.silk_abs_int32(coefs_syn_Q24[i]),
                    Inlines.silk_abs_int32(coefs_ana_Q24[i])
                )
                if (tmp > maxabs_Q24) {
                    maxabs_Q24 = tmp
                    ind = i
                }
                i++
            }
            if (maxabs_Q24 <= limit_Q24) {
                /* Coefficients are within range - done */
                return
            }

            /* Convert back to true warped coefficients */
            i = 1
            while (i < order) {
                coefs_syn_Q24[i - 1] = Inlines.silk_SMLAWB(coefs_syn_Q24[i - 1], coefs_syn_Q24[i], lambda_Q16)
                coefs_ana_Q24[i - 1] = Inlines.silk_SMLAWB(coefs_ana_Q24[i - 1], coefs_ana_Q24[i], lambda_Q16)
                i++
            }
            gain_syn_Q16 = Inlines.silk_INVERSE32_varQ(gain_syn_Q16, 32)
            gain_ana_Q16 = Inlines.silk_INVERSE32_varQ(gain_ana_Q16, 32)
            i = 0
            while (i < order) {
                coefs_syn_Q24[i] = Inlines.silk_SMULWW(gain_syn_Q16, coefs_syn_Q24[i])
                coefs_ana_Q24[i] = Inlines.silk_SMULWW(gain_ana_Q16, coefs_ana_Q24[i])
                i++
            }

            /* Apply bandwidth expansion */
            chirp_Q16 = (0.99f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.99f, 16)*/ -
                    Inlines.silk_DIV32_varQ(
                        Inlines.silk_SMULWB(
                            maxabs_Q24 - limit_Q24,
                            Inlines.silk_SMLABB(
                                (0.8f * (1.toLong() shl 10) + 0.5).toInt()/*Inlines.SILK_CONST(0.8f, 10)*/,
                                (0.1f * (1.toLong() shl 10) + 0.5).toInt()/*Inlines.SILK_CONST(0.1f, 10)*/,
                                iter
                            )
                        ),
                        Inlines.silk_MUL(maxabs_Q24, ind + 1), 22
                    )
            BWExpander.silk_bwexpander_32(coefs_syn_Q24, order, chirp_Q16)
            BWExpander.silk_bwexpander_32(coefs_ana_Q24, order, chirp_Q16)

            /* Convert to monic warped coefficients */
            lambda_Q16 = -lambda_Q16
            i = order - 1
            while (i > 0) {
                coefs_syn_Q24[i - 1] = Inlines.silk_SMLAWB(coefs_syn_Q24[i - 1], coefs_syn_Q24[i], lambda_Q16)
                coefs_ana_Q24[i - 1] = Inlines.silk_SMLAWB(coefs_ana_Q24[i - 1], coefs_ana_Q24[i], lambda_Q16)
                i--
            }
            lambda_Q16 = -lambda_Q16
            nom_Q16 = Inlines.silk_SMLAWB(
                (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/,
                -lambda_Q16,
                lambda_Q16
            )
            den_Q24 = Inlines.silk_SMLAWB(
                (1.0f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 24)*/,
                coefs_syn_Q24[0],
                lambda_Q16
            )
            gain_syn_Q16 = Inlines.silk_DIV32_varQ(nom_Q16, den_Q24, 24)
            den_Q24 = Inlines.silk_SMLAWB(
                (1.0f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 24)*/,
                coefs_ana_Q24[0],
                lambda_Q16
            )
            gain_ana_Q16 = Inlines.silk_DIV32_varQ(nom_Q16, den_Q24, 24)
            i = 0
            while (i < order) {
                coefs_syn_Q24[i] = Inlines.silk_SMULWW(gain_syn_Q16, coefs_syn_Q24[i])
                coefs_ana_Q24[i] = Inlines.silk_SMULWW(gain_ana_Q16, coefs_ana_Q24[i])
                i++
            }
            iter++
        }
        Inlines.OpusAssert(false)
    }

    /**
     * ***********************************************************
     */
    /* Compute noise shaping coefficients and initial gain values */
    /**
     * ***********************************************************
     */
    fun silk_noise_shape_analysis(
        psEnc: SilkChannelEncoder, /* I/O  Encoder state FIX                                                           */
        psEncCtrl: SilkEncoderControl, /* I/O  Encoder control FIX                                                         */
        pitch_res: ShortArray, /* I    LPC residual from pitch analysis                                            */
        pitch_res_ptr: Int,
        x: ShortArray, /* I    Input signal [ frame_length + la_shape ]                                    */
        x_ptr: Int
    ) {
        val psShapeSt = psEnc.sShape
        var k: Int
        var i: Int
        val nSamples: Int
        var Qnrg: Int
        var b_Q14: Int
        val warping_Q16: Int
        var scale = 0
        var SNR_adj_dB_Q7: Int
        var HarmBoost_Q16: Int
        var HarmShapeGain_Q16: Int
        val Tilt_Q16: Int
        var tmp32: Int
        var nrg: Int
        var pre_nrg_Q30: Int
        var log_energy_Q7: Int
        var log_energy_prev_Q7: Int
        var energy_variation_Q7: Int
        val delta_Q16: Int
        var BWExp1_Q16: Int
        var BWExp2_Q16: Int
        var gain_mult_Q16: Int
        val gain_add_Q16: Int
        var strength_Q16: Int
        var b_Q8: Int
        val auto_corr = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER + 1)
        val refl_coef_Q16 = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER)
        val AR1_Q24 = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER)
        val AR2_Q24 = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER)
        val x_windowed: ShortArray
        var pitch_res_ptr2: Int
        var x_ptr2: Int

        /* Point to start of first LPC analysis block */
        x_ptr2 = x_ptr - psEnc.la_shape

        /**
         * *************
         */
        /* GAIN CONTROL */
        /**
         * *************
         */
        SNR_adj_dB_Q7 = psEnc.SNR_dB_Q7

        /* Input quality is the average of the quality in the lowest two VAD bands */
        psEncCtrl.input_quality_Q14 =
                Inlines.silk_RSHIFT(psEnc.input_quality_bands_Q15[0].toInt() + psEnc.input_quality_bands_Q15[1], 2)
                    .toInt()

        /* Coding quality level, between 0.0_Q0 and 1.0_Q0, but in Q14 */
        psEncCtrl.coding_quality_Q14 = Inlines.silk_RSHIFT(
            Sigmoid.silk_sigm_Q15(
                Inlines.silk_RSHIFT_ROUND(
                    SNR_adj_dB_Q7 - (20.0f * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(20.0f, 7)*/,
                    4
                )
            ), 1
        )

        /* Reduce coding SNR during low speech activity */
        if (psEnc.useCBR == 0) {
            b_Q8 = (1.0f * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 8)*/ - psEnc.speech_activity_Q8
            b_Q8 = Inlines.silk_SMULWB(Inlines.silk_LSHIFT(b_Q8, 8), b_Q8)
            SNR_adj_dB_Q7 = Inlines.silk_SMLAWB(
                SNR_adj_dB_Q7,
                Inlines.silk_SMULBB(
                    ((0 - TuningParameters.BG_SNR_DECR_dB) * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(0 - TuningParameters.BG_SNR_DECR_dB, 7)*/ shr 4 + 1,
                    b_Q8
                ), /* Q11*/
                Inlines.silk_SMULWB(
                    (1.0f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 14)*/ + psEncCtrl.input_quality_Q14,
                    psEncCtrl.coding_quality_Q14
                )
            )
            /* Q12*/
        }

        if (psEnc.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            /* Reduce gains for periodic signals */
            SNR_adj_dB_Q7 = Inlines.silk_SMLAWB(
                SNR_adj_dB_Q7,
                (TuningParameters.HARM_SNR_INCR_dB * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HARM_SNR_INCR_dB, 8)*/,
                psEnc.LTPCorr_Q15
            )
        } else {
            /* For unvoiced signals and low-quality input, adjust the quality slower than SNR_dB setting */
            SNR_adj_dB_Q7 = Inlines.silk_SMLAWB(
                SNR_adj_dB_Q7,
                Inlines.silk_SMLAWB(
                    (6.0f * (1.toLong() shl 9) + 0.5).toInt()/*Inlines.SILK_CONST(6.0f, 9)*/,
                    -(0.4f * (1.toLong() shl 18) + 0.5).toInt()/*Inlines.SILK_CONST(0.4f, 18)*/,
                    psEnc.SNR_dB_Q7
                ),
                (1.0f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 14)*/ - psEncCtrl.input_quality_Q14
            )
        }

        /**
         * **********************
         */
        /* SPARSENESS PROCESSING */
        /**
         * **********************
         */
        /* Set quantizer offset */
        if (psEnc.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            /* Initially set to 0; may be overruled in process_gains(..) */
            psEnc.indices.quantOffsetType = 0
            psEncCtrl.sparseness_Q8 = 0
        } else {
            /* Sparseness measure, based on relative fluctuations of energy per 2 milliseconds */
            nSamples = Inlines.silk_LSHIFT(psEnc.fs_kHz, 1)
            energy_variation_Q7 = 0
            log_energy_prev_Q7 = 0
            pitch_res_ptr2 = pitch_res_ptr
            val boxed_nrg = BoxedValueInt(0)
            val boxed_scale = BoxedValueInt(0)
            k = 0
            while (k < Inlines.silk_SMULBB(SilkConstants.SUB_FRAME_LENGTH_MS, psEnc.nb_subfr) / 2) {
                SumSqrShift.silk_sum_sqr_shift(boxed_nrg, boxed_scale, pitch_res, pitch_res_ptr2, nSamples)
                nrg = boxed_nrg.Val
                scale = boxed_scale.Val
                nrg += Inlines.silk_RSHIFT(nSamples, scale)
                /* Q(-scale)*/

                log_energy_Q7 = Inlines.silk_lin2log(nrg)
                if (k > 0) {
                    energy_variation_Q7 += Inlines.silk_abs(log_energy_Q7 - log_energy_prev_Q7)
                }
                log_energy_prev_Q7 = log_energy_Q7
                pitch_res_ptr2 += nSamples
                k++
            }

            psEncCtrl.sparseness_Q8 = Inlines.silk_RSHIFT(
                Sigmoid.silk_sigm_Q15(
                    Inlines.silk_SMULWB(
                        energy_variation_Q7 - (5.0f * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(5.0f, 7)*/,
                        (0.1f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.1f, 16)*/
                    )
                ), 7
            )

            /* Set quantization offset depending on sparseness measure */
            if (psEncCtrl.sparseness_Q8 > (TuningParameters.SPARSENESS_THRESHOLD_QNT_OFFSET * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SPARSENESS_THRESHOLD_QNT_OFFSET, 8)*/) {
                psEnc.indices.quantOffsetType = 0
            } else {
                psEnc.indices.quantOffsetType = 1
            }

            /* Increase coding SNR for sparse signals */
            SNR_adj_dB_Q7 = Inlines.silk_SMLAWB(
                SNR_adj_dB_Q7,
                (TuningParameters.SPARSE_SNR_INCR_dB * (1.toLong() shl 15) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SPARSE_SNR_INCR_dB, 15)*/,
                psEncCtrl.sparseness_Q8 - (0.5f * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(0.5f, 8)*/
            )
        }

        /**
         * ****************************
         */
        /* Control bandwidth expansion */
        /**
         * ****************************
         */
        /* More BWE for signals with high prediction gain */
        strength_Q16 = Inlines.silk_SMULWB(
            psEncCtrl.predGain_Q16,
            (TuningParameters.FIND_PITCH_WHITE_NOISE_FRACTION * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.FIND_PITCH_WHITE_NOISE_FRACTION, 16)*/
        )
        BWExp2_Q16 = Inlines.silk_DIV32_varQ(
            (TuningParameters.BANDWIDTH_EXPANSION * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.BANDWIDTH_EXPANSION, 16)*/,
            Inlines.silk_SMLAWW(
                (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/,
                strength_Q16,
                strength_Q16
            ), 16
        )
        BWExp1_Q16 = BWExp2_Q16
        delta_Q16 = Inlines.silk_SMULWB(
            (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/ - Inlines.silk_SMULBB(
                3,
                psEncCtrl.coding_quality_Q14
            ),
            (TuningParameters.LOW_RATE_BANDWIDTH_EXPANSION_DELTA * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LOW_RATE_BANDWIDTH_EXPANSION_DELTA, 16)*/
        )
        BWExp1_Q16 = Inlines.silk_SUB32(BWExp1_Q16, delta_Q16)
        BWExp2_Q16 = Inlines.silk_ADD32(BWExp2_Q16, delta_Q16)
        /* BWExp1 will be applied after BWExp2, so make it relative */
        BWExp1_Q16 = Inlines.silk_DIV32_16(Inlines.silk_LSHIFT(BWExp1_Q16, 14), Inlines.silk_RSHIFT(BWExp2_Q16, 2))

        if (psEnc.warping_Q16 > 0) {
            /* Slightly more warping in analysis will move quantization noise up in frequency, where it's better masked */
            warping_Q16 = Inlines.silk_SMLAWB(
                psEnc.warping_Q16,
                psEncCtrl.coding_quality_Q14.toInt(),
                (0.01f * (1.toLong() shl 18) + 0.5).toInt()/*Inlines.SILK_CONST(0.01f, 18)*/
            )
        } else {
            warping_Q16 = 0
        }

        /**
         * *****************************************
         */
        /* Compute noise shaping AR coefs and gains */
        /**
         * *****************************************
         */
        x_windowed = ShortArray(psEnc.shapeWinLength)
        k = 0
        while (k < psEnc.nb_subfr) {
            /* Apply window: sine slope followed by flat part followed by cosine slope */
            var shift: Int
            val slope_part: Int
            val flat_part: Int
            flat_part = psEnc.fs_kHz * 3
            slope_part = Inlines.silk_RSHIFT(psEnc.shapeWinLength - flat_part, 1)

            ApplySineWindow.silk_apply_sine_window(x_windowed, 0, x, x_ptr2, 1, slope_part)
            shift = slope_part
            arraycopy(x, x_ptr2 + shift, x_windowed, shift, flat_part)
            shift += flat_part
            ApplySineWindow.silk_apply_sine_window(x_windowed, shift, x, x_ptr2 + shift, 2, slope_part)

            /* Update pointer: next LPC analysis block */
            x_ptr2 += psEnc.subfr_length
            val scale_boxed = BoxedValueInt(scale)
            if (psEnc.warping_Q16 > 0) {
                /* Calculate warped auto correlation */
                Autocorrelation.silk_warped_autocorrelation(
                    auto_corr,
                    scale_boxed,
                    x_windowed,
                    warping_Q16,
                    psEnc.shapeWinLength,
                    psEnc.shapingLPCOrder
                )
            } else {
                /* Calculate regular auto correlation */
                Autocorrelation.silk_autocorr(
                    auto_corr,
                    scale_boxed,
                    x_windowed,
                    psEnc.shapeWinLength,
                    psEnc.shapingLPCOrder + 1
                )
            }
            scale = scale_boxed.Val

            /* Add white noise, as a fraction of energy */
            auto_corr[0] = Inlines.silk_ADD32(
                auto_corr[0], Inlines.silk_max_32(
                    Inlines.silk_SMULWB(
                        Inlines.silk_RSHIFT(auto_corr[0], 4),
                        (TuningParameters.SHAPE_WHITE_NOISE_FRACTION * (1.toLong() shl 20) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SHAPE_WHITE_NOISE_FRACTION, 20)*/
                    ), 1
                )
            )

            /* Calculate the reflection coefficients using schur */
            nrg = Schur.silk_schur64(refl_coef_Q16, auto_corr, psEnc.shapingLPCOrder)
            Inlines.OpusAssert(nrg >= 0)

            /* Convert reflection coefficients to prediction coefficients */
            K2A.silk_k2a_Q16(AR2_Q24, refl_coef_Q16, psEnc.shapingLPCOrder)

            Qnrg = -scale
            /* range: -12...30*/
            Inlines.OpusAssert(Qnrg >= -12)
            Inlines.OpusAssert(Qnrg <= 30)

            /* Make sure that Qnrg is an even number */
            if (Qnrg and 1 != 0) {
                Qnrg -= 1
                nrg = nrg shr 1
            }

            tmp32 = Inlines.silk_SQRT_APPROX(nrg)
            Qnrg = Qnrg shr 1
            /* range: -6...15*/

            psEncCtrl.Gains_Q16[k] = Inlines.silk_LSHIFT_SAT32(tmp32, 16 - Qnrg)

            if (psEnc.warping_Q16 > 0) {
                /* Adjust gain for warping */
                gain_mult_Q16 = warped_gain(AR2_Q24, warping_Q16, psEnc.shapingLPCOrder)
                Inlines.OpusAssert(psEncCtrl.Gains_Q16[k] >= 0)
                if (Inlines.silk_SMULWW(
                        Inlines.silk_RSHIFT_ROUND(psEncCtrl.Gains_Q16[k], 1),
                        gain_mult_Q16
                    ) >= Int.MAX_VALUE shr 1
                ) {
                    psEncCtrl.Gains_Q16[k] = Int.MAX_VALUE
                } else {
                    psEncCtrl.Gains_Q16[k] = Inlines.silk_SMULWW(psEncCtrl.Gains_Q16[k], gain_mult_Q16)
                }
            }

            /* Bandwidth expansion for synthesis filter shaping */
            BWExpander.silk_bwexpander_32(AR2_Q24, psEnc.shapingLPCOrder, BWExp2_Q16)

            /* Compute noise shaping filter coefficients */
            arraycopy(AR2_Q24, 0, AR1_Q24, 0, psEnc.shapingLPCOrder)

            /* Bandwidth expansion for analysis filter shaping */
            Inlines.OpusAssert(BWExp1_Q16 <= (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/)
            BWExpander.silk_bwexpander_32(AR1_Q24, psEnc.shapingLPCOrder, BWExp1_Q16)

            /* Ratio of prediction gains, in energy domain */
            pre_nrg_Q30 = LPCInversePredGain.silk_LPC_inverse_pred_gain_Q24(AR2_Q24, psEnc.shapingLPCOrder)
            nrg = LPCInversePredGain.silk_LPC_inverse_pred_gain_Q24(AR1_Q24, psEnc.shapingLPCOrder)

            /*psEncCtrl.GainsPre[ k ] = 1.0f - 0.7f * ( 1.0f - pre_nrg / nrg ) = 0.3f + 0.7f * pre_nrg / nrg;*/
            pre_nrg_Q30 = Inlines.silk_LSHIFT32(
                Inlines.silk_SMULWB(
                    pre_nrg_Q30,
                    (0.7f * (1.toLong() shl 15) + 0.5).toInt()/*Inlines.SILK_CONST(0.7f, 15)*/
                ), 1
            )
            psEncCtrl.GainsPre_Q14[k] = (0.3f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(0.3f, 14)*/ +
                    Inlines.silk_DIV32_varQ(pre_nrg_Q30, nrg, 14)

            /* Convert to monic warped prediction coefficients and limit absolute values */
            limit_warped_coefs(
                AR2_Q24,
                AR1_Q24,
                warping_Q16,
                (3.999f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(3.999f, 24)*/,
                psEnc.shapingLPCOrder
            )

            /* Convert from Q24 to Q13 and store in int16 */
            i = 0
            while (i < psEnc.shapingLPCOrder) {
                psEncCtrl.AR1_Q13[k * SilkConstants.MAX_SHAPE_LPC_ORDER + i] =
                        Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(AR1_Q24[i], 11)).toShort()
                psEncCtrl.AR2_Q13[k * SilkConstants.MAX_SHAPE_LPC_ORDER + i] =
                        Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(AR2_Q24[i], 11)).toShort()
                i++
            }
            k++
        }

        /**
         * **************
         */
        /* Gain tweaking */
        /**
         * **************
         */
        /* Increase gains during low speech activity and put lower limit on gains */
        gain_mult_Q16 = Inlines.silk_log2lin(
            -/*Inlines.SILK_CONST(16.0f, 7)*//*Inlines.SILK_CONST(0.16f, 16)*/Inlines.silk_SMLAWB(
                -(16.0f * (1.toLong() shl 7) + 0.5).toInt(),
                SNR_adj_dB_Q7,
                (0.16f * (1.toLong() shl 16) + 0.5).toInt()
            )
        )
        gain_add_Q16 = Inlines.silk_log2lin(
            Inlines.silk_SMLAWB(
                (16.0f * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(16.0f, 7)*/,
                (SilkConstants.MIN_QGAIN_DB * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.MIN_QGAIN_DB, 7)*/,
                (0.16f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.16f, 16)*/
            )
        )
        Inlines.OpusAssert(gain_mult_Q16 > 0)
        k = 0
        while (k < psEnc.nb_subfr) {
            psEncCtrl.Gains_Q16[k] = Inlines.silk_SMULWW(psEncCtrl.Gains_Q16[k], gain_mult_Q16)
            Inlines.OpusAssert(psEncCtrl.Gains_Q16[k] >= 0)
            psEncCtrl.Gains_Q16[k] = Inlines.silk_ADD_POS_SAT32(psEncCtrl.Gains_Q16[k], gain_add_Q16)
            k++
        }

        gain_mult_Q16 = (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/ +
                Inlines.silk_RSHIFT_ROUND(
                    Inlines.silk_MLA(
                        (TuningParameters.INPUT_TILT * (1.toLong() shl 26) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.INPUT_TILT, 26)*/,
                        psEncCtrl.coding_quality_Q14,
                        (TuningParameters.HIGH_RATE_INPUT_TILT * (1.toLong() shl 12) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HIGH_RATE_INPUT_TILT, 12)*/
                    ), 10
                )
        k = 0
        while (k < psEnc.nb_subfr) {
            psEncCtrl.GainsPre_Q14[k] = Inlines.silk_SMULWB(gain_mult_Q16, psEncCtrl.GainsPre_Q14[k])
            k++
        }

        /**
         * *********************************************
         */
        /* Control low-frequency shaping and noise tilt */
        /**
         * *********************************************
         */
        /* Less low frequency shaping for noisy inputs */
        strength_Q16 = Inlines.silk_MUL(
            (TuningParameters.LOW_FREQ_SHAPING * (1.toLong() shl 4) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LOW_FREQ_SHAPING, 4)*/,
            Inlines.silk_SMLAWB(
                (1.0f * (1.toLong() shl 12) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 12)*/,
                (TuningParameters.LOW_QUALITY_LOW_FREQ_SHAPING_DECR * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LOW_QUALITY_LOW_FREQ_SHAPING_DECR, 13)*/,
                psEnc.input_quality_bands_Q15[0] - (1.0f * (1.toLong() shl 15) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 15)*/
            )
        )
        strength_Q16 = Inlines.silk_RSHIFT(Inlines.silk_MUL(strength_Q16, psEnc.speech_activity_Q8), 8)
        if (psEnc.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            /* Reduce low frequencies quantization noise for periodic signals, depending on pitch lag */
            /*f = 400; freqz([1, -0.98 + 2e-4 * f], [1, -0.97 + 7e-4 * f], 2^12, Fs); axis([0, 1000, -10, 1])*/
            val fs_kHz_inv = Inlines.silk_DIV32_16(
                (0.2f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(0.2f, 14)*/,
                psEnc.fs_kHz
            )
            k = 0
            while (k < psEnc.nb_subfr) {
                b_Q14 = fs_kHz_inv + Inlines.silk_DIV32_16(
                    (3.0f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(3.0f, 14)*/,
                    psEncCtrl.pitchL[k]
                )
                /* Pack two coefficients in one int32 */
                psEncCtrl.LF_shp_Q14[k] = Inlines.silk_LSHIFT(
                    (1.0f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 14)*/ - b_Q14 - Inlines.silk_SMULWB(
                        strength_Q16,
                        b_Q14
                    ), 16
                )
                psEncCtrl.LF_shp_Q14[k] = psEncCtrl.LF_shp_Q14[k] or
                        (b_Q14 - (1.0f * (1.toLong() shl 14) + 0.5).toInt() and 0xFFFF)
                k++
            }
            Inlines.OpusAssert((TuningParameters.HARM_HP_NOISE_COEF * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HARM_HP_NOISE_COEF, 24)*/ < (0.5f * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(0.5f, 24)*/)
            /* Guarantees that second argument to SMULWB() is within range of an short*/
            Tilt_Q16 = -(TuningParameters.HP_NOISE_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HP_NOISE_COEF, 16)*/ -
                    Inlines.silk_SMULWB(
                        (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/ - (TuningParameters.HP_NOISE_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HP_NOISE_COEF, 16)*/,
                        Inlines.silk_SMULWB(
                            (TuningParameters.HARM_HP_NOISE_COEF * (1.toLong() shl 24) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HARM_HP_NOISE_COEF, 24)*/,
                            psEnc.speech_activity_Q8
                        )
                    )
        } else {
            b_Q14 = Inlines.silk_DIV32_16(21299, psEnc.fs_kHz)
            /* 1.3_Q0 = 21299_Q14*/
            /* Pack two coefficients in one int32 */
            psEncCtrl.LF_shp_Q14[0] = Inlines.silk_LSHIFT(
                (1.0f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 14)*/ - b_Q14
                        - Inlines.silk_SMULWB(
                    strength_Q16,
                    Inlines.silk_SMULWB(
                        (0.6f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.6f, 16)*/,
                        b_Q14
                    )
                ), 16
            )
            psEncCtrl.LF_shp_Q14[0] = psEncCtrl.LF_shp_Q14[0] or
                    (b_Q14 - (1.0f * (1.toLong() shl 14) + 0.5).toInt() and 0xFFFF)
            k = 1
            while (k < psEnc.nb_subfr) {
                psEncCtrl.LF_shp_Q14[k] = psEncCtrl.LF_shp_Q14[0]
                k++
            }
            Tilt_Q16 =
                    -(TuningParameters.HP_NOISE_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HP_NOISE_COEF, 16)*/
        }

        /**
         * *************************
         */
        /* HARMONIC SHAPING CONTROL */
        /**
         * *************************
         */
        /* Control boosting of harmonic frequencies */
        HarmBoost_Q16 = Inlines.silk_SMULWB(
            Inlines.silk_SMULWB(
                (1.0f * (1.toLong() shl 17) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 17)*/ - Inlines.silk_LSHIFT(
                    psEncCtrl.coding_quality_Q14,
                    3
                ),
                psEnc.LTPCorr_Q15
            ),
            (TuningParameters.LOW_RATE_HARMONIC_BOOST * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LOW_RATE_HARMONIC_BOOST, 16)*/
        )

        /* More harmonic boost for noisy input signals */
        HarmBoost_Q16 = Inlines.silk_SMLAWB(
            HarmBoost_Q16,
            (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/ - Inlines.silk_LSHIFT(
                psEncCtrl.input_quality_Q14,
                2
            ),
            (TuningParameters.LOW_INPUT_QUALITY_HARMONIC_BOOST * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LOW_INPUT_QUALITY_HARMONIC_BOOST, 16)*/
        )

        if (SilkConstants.USE_HARM_SHAPING != 0 && psEnc.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            /* More harmonic noise shaping for high bitrates or noisy input */
            HarmShapeGain_Q16 = Inlines.silk_SMLAWB(
                (TuningParameters.HARMONIC_SHAPING * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HARMONIC_SHAPING, 16)*/,
                (1.0f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 16)*/ - Inlines.silk_SMULWB(
                    (1.0f * (1.toLong() shl 18) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 18)*/ - Inlines.silk_LSHIFT(
                        psEncCtrl.coding_quality_Q14,
                        4
                    ),
                    psEncCtrl.input_quality_Q14
                ),
                (TuningParameters.HIGH_RATE_OR_LOW_QUALITY_HARMONIC_SHAPING * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.HIGH_RATE_OR_LOW_QUALITY_HARMONIC_SHAPING, 16)*/
            )

            /* Less harmonic noise shaping for less periodic signals */
            HarmShapeGain_Q16 = Inlines.silk_SMULWB(
                Inlines.silk_LSHIFT(HarmShapeGain_Q16, 1),
                Inlines.silk_SQRT_APPROX(Inlines.silk_LSHIFT(psEnc.LTPCorr_Q15, 15))
            )
        } else {
            HarmShapeGain_Q16 = 0
        }

        /**
         * **********************
         */
        /* Smooth over subframes */
        /**
         * **********************
         */
        k = 0
        while (k < SilkConstants.MAX_NB_SUBFR) {
            psShapeSt.HarmBoost_smth_Q16 = Inlines.silk_SMLAWB(
                psShapeSt.HarmBoost_smth_Q16,
                HarmBoost_Q16 - psShapeSt.HarmBoost_smth_Q16,
                (TuningParameters.SUBFR_SMTH_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SUBFR_SMTH_COEF, 16)*/
            )
            psShapeSt.HarmShapeGain_smth_Q16 = Inlines.silk_SMLAWB(
                psShapeSt.HarmShapeGain_smth_Q16,
                HarmShapeGain_Q16 - psShapeSt.HarmShapeGain_smth_Q16,
                (TuningParameters.SUBFR_SMTH_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SUBFR_SMTH_COEF, 16)*/
            )
            psShapeSt.Tilt_smth_Q16 = Inlines.silk_SMLAWB(
                psShapeSt.Tilt_smth_Q16,
                Tilt_Q16 - psShapeSt.Tilt_smth_Q16,
                (TuningParameters.SUBFR_SMTH_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SUBFR_SMTH_COEF, 16)*/
            )

            psEncCtrl.HarmBoost_Q14[k] = Inlines.silk_RSHIFT_ROUND(psShapeSt.HarmBoost_smth_Q16, 2).toInt()
            psEncCtrl.HarmShapeGain_Q14[k] = Inlines.silk_RSHIFT_ROUND(psShapeSt.HarmShapeGain_smth_Q16, 2).toInt()
            psEncCtrl.Tilt_Q14[k] = Inlines.silk_RSHIFT_ROUND(psShapeSt.Tilt_smth_Q16, 2).toInt()
            k++
        }
    }
}
