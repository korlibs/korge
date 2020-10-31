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

internal object FindPredCoefs {

    fun silk_find_pred_coefs(
        psEnc: SilkChannelEncoder, /* I/O  encoder state                                                               */
        psEncCtrl: SilkEncoderControl, /* I/O  encoder control                                                             */
        res_pitch: ShortArray, /* I    Residual from pitch analysis                                                */
        x: ShortArray, /* I    Speech signal                                                               */
        x_ptr: Int,
        condCoding: Int /* I    The type of conditional coding to use                                       */
    ) {
        var i: Int
        val invGains_Q16 = IntArray(SilkConstants.MAX_NB_SUBFR)
        val local_gains = IntArray(SilkConstants.MAX_NB_SUBFR)
        val Wght_Q15 = IntArray(SilkConstants.MAX_NB_SUBFR)
        val NLSF_Q15 = ShortArray(SilkConstants.MAX_LPC_ORDER)
        var x_ptr2: Int
        var x_pre_ptr: Int
        val LPC_in_pre: ShortArray
        var tmp: Int
        var min_gain_Q16: Int
        var minInvGain_Q30: Int
        val LTP_corrs_rshift = IntArray(SilkConstants.MAX_NB_SUBFR)

        /* weighting for weighted least squares */
        min_gain_Q16 = Int.MAX_VALUE shr 6
        i = 0
        while (i < psEnc.nb_subfr) {
            min_gain_Q16 = Inlines.silk_min(min_gain_Q16, psEncCtrl.Gains_Q16[i])
            i++
        }
        i = 0
        while (i < psEnc.nb_subfr) {
            /* Divide to Q16 */
            Inlines.OpusAssert(psEncCtrl.Gains_Q16[i] > 0)
            /* Invert and normalize gains, and ensure that maximum invGains_Q16 is within range of a 16 bit int */
            invGains_Q16[i] = Inlines.silk_DIV32_varQ(min_gain_Q16, psEncCtrl.Gains_Q16[i], 16 - 2)

            /* Ensure Wght_Q15 a minimum value 1 */
            invGains_Q16[i] = Inlines.silk_max(invGains_Q16[i], 363)

            /* Square the inverted gains */
            Inlines.OpusAssert(invGains_Q16[i] == Inlines.silk_SAT16(invGains_Q16[i]))
            tmp = Inlines.silk_SMULWB(invGains_Q16[i], invGains_Q16[i])
            Wght_Q15[i] = Inlines.silk_RSHIFT(tmp, 1)

            /* Invert the inverted and normalized gains */
            local_gains[i] = Inlines.silk_DIV32(1 shl 16, invGains_Q16[i])
            i++
        }

        LPC_in_pre = ShortArray(psEnc.nb_subfr * psEnc.predictLPCOrder + psEnc.frame_length)
        if (psEnc.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            val WLTP: IntArray

            /**
             * *******
             */
            /* VOICED */
            /**
             * *******
             */
            Inlines.OpusAssert(psEnc.ltp_mem_length - psEnc.predictLPCOrder >= psEncCtrl.pitchL[0] + SilkConstants.LTP_ORDER / 2)

            WLTP = IntArray(psEnc.nb_subfr * SilkConstants.LTP_ORDER * SilkConstants.LTP_ORDER)

            /* LTP analysis */
            val boxed_codgain = BoxedValueInt(psEncCtrl.LTPredCodGain_Q7)
            FindLTP.silk_find_LTP(
                psEncCtrl.LTPCoef_Q14, WLTP, boxed_codgain,
                res_pitch, psEncCtrl.pitchL, Wght_Q15, psEnc.subfr_length,
                psEnc.nb_subfr, psEnc.ltp_mem_length, LTP_corrs_rshift
            )
            psEncCtrl.LTPredCodGain_Q7 = boxed_codgain.Val

            /* Quantize LTP gain parameters */
            val boxed_periodicity = BoxedValueByte(psEnc.indices.PERIndex)
            val boxed_gain = BoxedValueInt(psEnc.sum_log_gain_Q7)
            QuantizeLTPGains.silk_quant_LTP_gains(
                psEncCtrl.LTPCoef_Q14, psEnc.indices.LTPIndex, boxed_periodicity,
                boxed_gain, WLTP, psEnc.mu_LTP_Q9, psEnc.LTPQuantLowComplexity, psEnc.nb_subfr
            )
            psEnc.indices.PERIndex = boxed_periodicity.Val
            psEnc.sum_log_gain_Q7 = boxed_gain.Val

            /* Control LTP scaling */
            LTPScaleControl.silk_LTP_scale_ctrl(psEnc, psEncCtrl, condCoding)

            /* Create LTP residual */
            LTPAnalysisFilter.silk_LTP_analysis_filter(
                LPC_in_pre, x, x_ptr - psEnc.predictLPCOrder, psEncCtrl.LTPCoef_Q14,
                psEncCtrl.pitchL, invGains_Q16, psEnc.subfr_length, psEnc.nb_subfr, psEnc.predictLPCOrder
            )

        } else {
            /**
             * *********
             */
            /* UNVOICED */
            /**
             * *********
             */
            /* Create signal with prepended subframes, scaled by inverse gains */
            x_ptr2 = x_ptr - psEnc.predictLPCOrder
            x_pre_ptr = 0
            i = 0
            while (i < psEnc.nb_subfr) {
                Inlines.silk_scale_copy_vector16(
                    LPC_in_pre, x_pre_ptr, x, x_ptr2, invGains_Q16[i],
                    psEnc.subfr_length + psEnc.predictLPCOrder
                )
                x_pre_ptr += psEnc.subfr_length + psEnc.predictLPCOrder
                x_ptr2 += psEnc.subfr_length
                i++
            }

            Arrays.MemSet(psEncCtrl.LTPCoef_Q14, 0.toShort(), psEnc.nb_subfr * SilkConstants.LTP_ORDER)
            psEncCtrl.LTPredCodGain_Q7 = 0
            psEnc.sum_log_gain_Q7 = 0
        }

        /* Limit on total predictive coding gain */
        if (psEnc.first_frame_after_reset != 0) {
            minInvGain_Q30 =
                    (1.0f / SilkConstants.MAX_PREDICTION_POWER_GAIN_AFTER_RESET * (1.toLong() shl 30) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f / SilkConstants.MAX_PREDICTION_POWER_GAIN_AFTER_RESET, 30)*/
        } else {
            minInvGain_Q30 = Inlines.silk_log2lin(
                Inlines.silk_SMLAWB(
                    16 shl 7,
                    psEncCtrl.LTPredCodGain_Q7.toInt(),
                    (1.0f / 3f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f / 3f, 16)*/
                )
            )
            /* Q16 */
            minInvGain_Q30 = Inlines.silk_DIV32_varQ(
                minInvGain_Q30,
                Inlines.silk_SMULWW(
                    (SilkConstants.MAX_PREDICTION_POWER_GAIN * (1.toLong() shl 0) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.MAX_PREDICTION_POWER_GAIN, 0)*/,
                    Inlines.silk_SMLAWB(
                        (0.25f * (1.toLong() shl 18) + 0.5).toInt()/*Inlines.SILK_CONST(0.25f, 18)*/,
                        (0.75f * (1.toLong() shl 18) + 0.5).toInt()/*Inlines.SILK_CONST(0.75f, 18)*/,
                        psEncCtrl.coding_quality_Q14
                    )
                ), 14
            )
        }

        /* LPC_in_pre contains the LTP-filtered input for voiced, and the unfiltered input for unvoiced */
        FindLPC.silk_find_LPC(psEnc, NLSF_Q15, LPC_in_pre, minInvGain_Q30)

        /* Quantize LSFs */
        NLSF.silk_process_NLSFs(psEnc, psEncCtrl.PredCoef_Q12, NLSF_Q15, psEnc.prev_NLSFq_Q15)

        /* Calculate residual energy using quantized LPC coefficients */
        ResidualEnergy.silk_residual_energy(
            psEncCtrl.ResNrg, psEncCtrl.ResNrgQ, LPC_in_pre, psEncCtrl.PredCoef_Q12, local_gains,
            psEnc.subfr_length, psEnc.nb_subfr, psEnc.predictLPCOrder
        )

        /* Copy to prediction struct for use in next frame for interpolation */
        arraycopy(NLSF_Q15, 0, psEnc.prev_NLSFq_Q15, 0, SilkConstants.MAX_LPC_ORDER)
    }
}
