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
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korio.lang.*

/// <summary>
/// Noise shaping quantization state
/// </summary>
internal class SilkNSQState {

    /// <summary>
    /// Buffer for quantized output signal
    /// </summary>
    val xq =
        ShortArray(2 * SilkConstants.MAX_FRAME_LENGTH) //opt: make these arrays variable-sized since construction cost is significant here
    val sLTP_shp_Q14 = IntArray(2 * SilkConstants.MAX_FRAME_LENGTH)
    val sLPC_Q14 = IntArray(SilkConstants.MAX_SUB_FRAME_LENGTH + SilkConstants.NSQ_LPC_BUF_LENGTH)
    val sAR2_Q14 = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER)
    var sLF_AR_shp_Q14 = 0
    var lagPrev = 0
    var sLTP_buf_idx = 0
    var sLTP_shp_buf_idx = 0
    var rand_seed = 0
    var prev_gain_Q16 = 0
    var rewhite_flag = 0

    fun Reset() {
        Arrays.MemSet(xq, 0.toShort(), 2 * SilkConstants.MAX_FRAME_LENGTH)
        Arrays.MemSet(sLTP_shp_Q14, 0, 2 * SilkConstants.MAX_FRAME_LENGTH)
        Arrays.MemSet(sLPC_Q14, 0, SilkConstants.MAX_SUB_FRAME_LENGTH + SilkConstants.NSQ_LPC_BUF_LENGTH)
        Arrays.MemSet(sAR2_Q14, 0, SilkConstants.MAX_SHAPE_LPC_ORDER)
        sLF_AR_shp_Q14 = 0
        lagPrev = 0
        sLTP_buf_idx = 0
        sLTP_shp_buf_idx = 0
        rand_seed = 0
        prev_gain_Q16 = 0
        rewhite_flag = 0
    }

    // Copies another nsq state to this one
    fun Assign(other: SilkNSQState) {
        this.sLF_AR_shp_Q14 = other.sLF_AR_shp_Q14
        this.lagPrev = other.lagPrev
        this.sLTP_buf_idx = other.sLTP_buf_idx
        this.sLTP_shp_buf_idx = other.sLTP_shp_buf_idx
        this.rand_seed = other.rand_seed
        this.prev_gain_Q16 = other.prev_gain_Q16
        this.rewhite_flag = other.rewhite_flag
        arraycopy(other.xq, 0, this.xq, 0, 2 * SilkConstants.MAX_FRAME_LENGTH)
        arraycopy(other.sLTP_shp_Q14, 0, this.sLTP_shp_Q14, 0, 2 * SilkConstants.MAX_FRAME_LENGTH)
        arraycopy(
            other.sLPC_Q14,
            0,
            this.sLPC_Q14,
            0,
            SilkConstants.MAX_SUB_FRAME_LENGTH + SilkConstants.NSQ_LPC_BUF_LENGTH
        )
        arraycopy(other.sAR2_Q14, 0, this.sAR2_Q14, 0, SilkConstants.MAX_SHAPE_LPC_ORDER)
    }

    private inner class NSQ_del_dec_struct internal constructor(shapingOrder: Int) {

        internal val sLPC_Q14 = IntArray(SilkConstants.MAX_SUB_FRAME_LENGTH + SilkConstants.NSQ_LPC_BUF_LENGTH)
        internal val RandState = IntArray(SilkConstants.DECISION_DELAY)
        internal val Q_Q10 = IntArray(SilkConstants.DECISION_DELAY)
        internal val Xq_Q14 = IntArray(SilkConstants.DECISION_DELAY)
        internal val Pred_Q15 = IntArray(SilkConstants.DECISION_DELAY)
        internal val Shape_Q14 = IntArray(SilkConstants.DECISION_DELAY)
        internal var sAR2_Q14: IntArray
        internal var LF_AR_Q14 = 0
        internal var Seed = 0
        internal var SeedInit = 0
        internal var RD_Q10 = 0

        init {
            sAR2_Q14 = IntArray(shapingOrder)
        }

        internal fun PartialCopyFrom(other: NSQ_del_dec_struct, q14Offset: Int) {
            arraycopy(
                other.sLPC_Q14,
                q14Offset,
                sLPC_Q14,
                q14Offset,
                SilkConstants.MAX_SUB_FRAME_LENGTH + SilkConstants.NSQ_LPC_BUF_LENGTH - q14Offset
            )
            arraycopy(other.RandState, 0, RandState, 0, SilkConstants.DECISION_DELAY)
            arraycopy(other.Q_Q10, 0, Q_Q10, 0, SilkConstants.DECISION_DELAY)
            arraycopy(other.Xq_Q14, 0, Xq_Q14, 0, SilkConstants.DECISION_DELAY)
            arraycopy(other.Pred_Q15, 0, Pred_Q15, 0, SilkConstants.DECISION_DELAY)
            arraycopy(other.Shape_Q14, 0, Shape_Q14, 0, SilkConstants.DECISION_DELAY)
            arraycopy(other.sAR2_Q14, 0, sAR2_Q14, 0, sAR2_Q14.size)
            LF_AR_Q14 = other.LF_AR_Q14
            Seed = other.Seed
            SeedInit = other.SeedInit
            RD_Q10 = other.RD_Q10
        }

        internal fun Assign(other: NSQ_del_dec_struct) {
            this.PartialCopyFrom(other, 0)
        }
    }

    private inner class NSQ_sample_struct {

        internal var Q_Q10: Int = 0
        internal var RD_Q10: Int = 0
        internal var xq_Q14: Int = 0
        internal var LF_AR_Q14: Int = 0
        internal var sLTP_shp_Q14: Int = 0
        internal var LPC_exc_Q14: Int = 0

        internal fun Assign(other: NSQ_sample_struct) {
            this.Q_Q10 = other.Q_Q10
            this.RD_Q10 = other.RD_Q10
            this.xq_Q14 = other.xq_Q14
            this.LF_AR_Q14 = other.LF_AR_Q14
            this.sLTP_shp_Q14 = other.sLTP_shp_Q14
            this.LPC_exc_Q14 = other.LPC_exc_Q14
        }
    }

    fun silk_NSQ(
        psEncC: SilkChannelEncoder, /* I/O  Encoder State                   */
        psIndices: SideInfoIndices, /* I/O  Quantization Indices            */
        x_Q3: IntArray, /* I    Prefiltered input signal        */
        pulses: ByteArray, /* O    Quantized pulse signal          */
        PredCoef_Q12: Array<ShortArray>, /* I    Short term prediction coefs [2][SilkConstants.MAX_LPC_ORDER]    */
        LTPCoef_Q14: ShortArray, /* I    Long term prediction coefs [SilkConstants.LTP_ORDER * MAX_NB_SUBFR]     */
        AR2_Q13: ShortArray, /* I Noise shaping coefs [MAX_NB_SUBFR * SilkConstants.MAX_SHAPE_LPC_ORDER]            */
        HarmShapeGain_Q14: IntArray, /* I    Long term shaping coefs [MAX_NB_SUBFR]        */
        Tilt_Q14: IntArray, /* I    Spectral tilt [MAX_NB_SUBFR]                  */
        LF_shp_Q14: IntArray, /* I    Low frequency shaping coefs [MAX_NB_SUBFR]    */
        Gains_Q16: IntArray, /* I    Quantization step sizes [MAX_NB_SUBFR]        */
        pitchL: IntArray, /* I    Pitch lags [MAX_NB_SUBFR]                     */
        Lambda_Q10: Int, /* I    Rate/distortion tradeoff        */
        LTP_scale_Q14: Int /* I    LTP state scaling               */
    ) {
        var k: Int
        var lag: Int
        var start_idx: Int
        val LSF_interpolation_flag: Int
        var A_Q12: Int
        var B_Q14: Int
        var AR_shp_Q13: Int
        var pxq: Int
        val sLTP_Q15: IntArray
        val sLTP: ShortArray
        var HarmShapeFIRPacked_Q14: Int
        val offset_Q10: Int
        val x_sc_Q10: IntArray
        var pulses_ptr = 0
        var x_Q3_ptr = 0

        this.rand_seed = psIndices.Seed.toInt()

        /* Set unvoiced lag to the previous one, overwrite later for voiced */
        lag = this.lagPrev

        Inlines.OpusAssert(this.prev_gain_Q16 != 0)

        offset_Q10 =
                SilkTables.silk_Quantization_Offsets_Q10[psIndices.signalType shr 1][psIndices.quantOffsetType.toInt()].toInt()

        if (psIndices.NLSFInterpCoef_Q2.toInt() == 4) {
            LSF_interpolation_flag = 0
        } else {
            LSF_interpolation_flag = 1
        }

        sLTP_Q15 = IntArray(psEncC.ltp_mem_length + psEncC.frame_length)
        sLTP = ShortArray(psEncC.ltp_mem_length + psEncC.frame_length)
        x_sc_Q10 = IntArray(psEncC.subfr_length)
        /* Set up pointers to start of sub frame */
        this.sLTP_shp_buf_idx = psEncC.ltp_mem_length
        this.sLTP_buf_idx = psEncC.ltp_mem_length
        pxq = psEncC.ltp_mem_length
        k = 0
        while (k < psEncC.nb_subfr) {
            A_Q12 = k shr 1 or 1 - LSF_interpolation_flag
            B_Q14 = k * SilkConstants.LTP_ORDER // opt: does this indicate a partitioned array?
            AR_shp_Q13 = k * SilkConstants.MAX_SHAPE_LPC_ORDER // opt: same here

            /* Noise shape parameters */
            Inlines.OpusAssert(HarmShapeGain_Q14[k] >= 0)
            HarmShapeFIRPacked_Q14 = Inlines.silk_RSHIFT(HarmShapeGain_Q14[k], 2)
            HarmShapeFIRPacked_Q14 = HarmShapeFIRPacked_Q14 or
                    Inlines.silk_LSHIFT(Inlines.silk_RSHIFT(HarmShapeGain_Q14[k], 1).toInt(), 16)

            this.rewhite_flag = 0
            if (psIndices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
                /* Voiced */
                lag = pitchL[k]

                /* Re-whitening */
                if (k and 3 - Inlines.silk_LSHIFT(LSF_interpolation_flag, 1) == 0) {
                    /* Rewhiten with new A coefs */
                    start_idx = psEncC.ltp_mem_length - lag - psEncC.predictLPCOrder - SilkConstants.LTP_ORDER / 2
                    Inlines.OpusAssert(start_idx > 0)

                    Filters.silk_LPC_analysis_filter(
                        sLTP, start_idx, this.xq, start_idx + k * psEncC.subfr_length,
                        PredCoef_Q12[A_Q12], 0, psEncC.ltp_mem_length - start_idx, psEncC.predictLPCOrder
                    )

                    this.rewhite_flag = 1
                    this.sLTP_buf_idx = psEncC.ltp_mem_length
                }
            }

            silk_nsq_scale_states(
                psEncC,
                x_Q3,
                x_Q3_ptr,
                x_sc_Q10,
                sLTP,
                sLTP_Q15,
                k,
                LTP_scale_Q14,
                Gains_Q16,
                pitchL,
                psIndices.signalType.toInt()
            )

            silk_noise_shape_quantizer(
                psIndices.signalType.toInt(),
                x_sc_Q10,
                pulses,
                pulses_ptr,
                this.xq,
                pxq,
                sLTP_Q15,
                PredCoef_Q12[A_Q12],
                LTPCoef_Q14,
                B_Q14,
                AR2_Q13,
                AR_shp_Q13,
                lag,
                HarmShapeFIRPacked_Q14,
                Tilt_Q14[k],
                LF_shp_Q14[k],
                Gains_Q16[k],
                Lambda_Q10,
                offset_Q10,
                psEncC.subfr_length,
                psEncC.shapingLPCOrder,
                psEncC.predictLPCOrder
            )

            x_Q3_ptr += psEncC.subfr_length
            pulses_ptr += psEncC.subfr_length
            pxq += psEncC.subfr_length
            k++
        }

        /* Update lagPrev for next frame */
        this.lagPrev = pitchL[psEncC.nb_subfr - 1]

        /* Save quantized speech and noise shaping signals */
        Arrays.MemMove(this.xq, psEncC.frame_length, 0, psEncC.ltp_mem_length)
        Arrays.MemMove(this.sLTP_shp_Q14, psEncC.frame_length, 0, psEncC.ltp_mem_length)
    }

    /**
     * ********************************
     */
    /* silk_noise_shape_quantizer  */
    /**
     * ********************************
     */
    private fun silk_noise_shape_quantizer(
        signalType: Int, /* I    Signal type                     */
        x_sc_Q10: IntArray, /* I [length]                                   */
        pulses: ByteArray, /* O [length]                                    */
        pulses_ptr: Int,
        xq: ShortArray, /* O [length]                                    */
        xq_ptr: Int,
        sLTP_Q15: IntArray, /* I/O  LTP state                       */
        a_Q12: ShortArray, /* I    Short term prediction coefs     */
        b_Q14: ShortArray, /* I    Long term prediction coefs      */
        b_Q14_ptr: Int,
        AR_shp_Q13: ShortArray, /* I    Noise shaping AR coefs          */
        AR_shp_Q13_ptr: Int,
        lag: Int, /* I    Pitch lag                       */
        HarmShapeFIRPacked_Q14: Int, /* I                                    */
        Tilt_Q14: Int, /* I    Spectral tilt                   */
        LF_shp_Q14: Int, /* I                                    */
        Gain_Q16: Int, /* I                                    */
        Lambda_Q10: Int, /* I                                    */
        offset_Q10: Int, /* I                                    */
        length: Int, /* I    Input length                    */
        shapingLPCOrder: Int, /* I    Noise shaping AR filter order   */
        predictLPCOrder: Int /* I    Prediction filter order         */
    ) {
        var i: Int
        var j: Int
        var LTP_pred_Q13: Int
        var LPC_pred_Q10: Int
        var n_AR_Q12: Int
        var n_LTP_Q13: Int
        var n_LF_Q12: Int
        var r_Q10: Int
        var rr_Q10: Int
        var q1_Q0: Int
        var q1_Q10: Int
        var q2_Q10: Int
        var rd1_Q20: Int
        var rd2_Q20: Int
        var exc_Q14: Int
        var LPC_exc_Q14: Int
        var xq_Q14: Int
        val Gain_Q10: Int
        var tmp1: Int
        var tmp2: Int
        var sLF_AR_shp_Q14: Int
        var psLPC_Q14: Int
        var shp_lag_ptr: Int
        var pred_lag_ptr: Int

        shp_lag_ptr = this.sLTP_shp_buf_idx - lag + SilkConstants.HARM_SHAPE_FIR_TAPS / 2
        pred_lag_ptr = this.sLTP_buf_idx - lag + SilkConstants.LTP_ORDER / 2
        Gain_Q10 = Inlines.silk_RSHIFT(Gain_Q16, 6)

        /* Set up short term AR state */
        psLPC_Q14 = SilkConstants.NSQ_LPC_BUF_LENGTH - 1

        i = 0
        while (i < length) {
            /* Generate dither */
            this.rand_seed = Inlines.silk_RAND(this.rand_seed)

            /* Short-term prediction */
            Inlines.OpusAssert(predictLPCOrder == 10 || predictLPCOrder == 16)
            /* Avoids introducing a bias because Inlines.silk_SMLAWB() always rounds to -inf */
            LPC_pred_Q10 = Inlines.silk_RSHIFT(predictLPCOrder, 1)
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 0], a_Q12[0].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 1], a_Q12[1].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 2], a_Q12[2].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 3], a_Q12[3].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 4], a_Q12[4].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 5], a_Q12[5].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 6], a_Q12[6].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 7], a_Q12[7].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 8], a_Q12[8].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 9], a_Q12[9].toInt())
            if (predictLPCOrder == 16) {
                LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 10], a_Q12[10].toInt())
                LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 11], a_Q12[11].toInt())
                LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 12], a_Q12[12].toInt())
                LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 13], a_Q12[13].toInt())
                LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 14], a_Q12[14].toInt())
                LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, this.sLPC_Q14[psLPC_Q14 - 15], a_Q12[15].toInt())
            }

            /* Long-term prediction */
            if (signalType == SilkConstants.TYPE_VOICED) {
                /* Unrolled loop */
                /* Avoids introducing a bias because Inlines.silk_SMLAWB() always rounds to -inf */
                LTP_pred_Q13 = 2
                LTP_pred_Q13 = Inlines.silk_SMLAWB(LTP_pred_Q13, sLTP_Q15[pred_lag_ptr], b_Q14[b_Q14_ptr].toInt())
                LTP_pred_Q13 =
                        Inlines.silk_SMLAWB(LTP_pred_Q13, sLTP_Q15[pred_lag_ptr - 1], b_Q14[b_Q14_ptr + 1].toInt())
                LTP_pred_Q13 =
                        Inlines.silk_SMLAWB(LTP_pred_Q13, sLTP_Q15[pred_lag_ptr - 2], b_Q14[b_Q14_ptr + 2].toInt())
                LTP_pred_Q13 =
                        Inlines.silk_SMLAWB(LTP_pred_Q13, sLTP_Q15[pred_lag_ptr - 3], b_Q14[b_Q14_ptr + 3].toInt())
                LTP_pred_Q13 =
                        Inlines.silk_SMLAWB(LTP_pred_Q13, sLTP_Q15[pred_lag_ptr - 4], b_Q14[b_Q14_ptr + 4].toInt())
                pred_lag_ptr += 1
            } else {
                LTP_pred_Q13 = 0
            }

            /* Noise shape feedback */
            Inlines.OpusAssert(shapingLPCOrder and 1 == 0)
            /* check that order is even */
            tmp2 = this.sLPC_Q14[psLPC_Q14]
            tmp1 = this.sAR2_Q14[0]
            this.sAR2_Q14[0] = tmp2
            n_AR_Q12 = Inlines.silk_RSHIFT(shapingLPCOrder, 1)
            n_AR_Q12 = Inlines.silk_SMLAWB(n_AR_Q12, tmp2, AR_shp_Q13[AR_shp_Q13_ptr].toInt())
            j = 2
            while (j < shapingLPCOrder) {
                tmp2 = this.sAR2_Q14[j - 1]
                this.sAR2_Q14[j - 1] = tmp1
                n_AR_Q12 = Inlines.silk_SMLAWB(n_AR_Q12, tmp1, AR_shp_Q13[AR_shp_Q13_ptr + j - 1].toInt())
                tmp1 = this.sAR2_Q14[j + 0]
                this.sAR2_Q14[j + 0] = tmp2
                n_AR_Q12 = Inlines.silk_SMLAWB(n_AR_Q12, tmp2, AR_shp_Q13[AR_shp_Q13_ptr + j].toInt())
                j += 2
            }
            this.sAR2_Q14[shapingLPCOrder - 1] = tmp1
            n_AR_Q12 = Inlines.silk_SMLAWB(n_AR_Q12, tmp1, AR_shp_Q13[AR_shp_Q13_ptr + shapingLPCOrder - 1].toInt())

            n_AR_Q12 = Inlines.silk_LSHIFT32(n_AR_Q12, 1)
            /* Q11 . Q12 */
            n_AR_Q12 = Inlines.silk_SMLAWB(n_AR_Q12, this.sLF_AR_shp_Q14, Tilt_Q14)

            n_LF_Q12 = Inlines.silk_SMULWB(this.sLTP_shp_Q14[this.sLTP_shp_buf_idx - 1], LF_shp_Q14)
            n_LF_Q12 = Inlines.silk_SMLAWT(n_LF_Q12, this.sLF_AR_shp_Q14, LF_shp_Q14)

            Inlines.OpusAssert(lag > 0 || signalType != SilkConstants.TYPE_VOICED)

            /* Combine prediction and noise shaping signals */
            tmp1 = Inlines.silk_SUB32(Inlines.silk_LSHIFT32(LPC_pred_Q10, 2), n_AR_Q12)
            /* Q12 */
            tmp1 = Inlines.silk_SUB32(tmp1, n_LF_Q12)
            /* Q12 */
            if (lag > 0) {
                /* Symmetric, packed FIR coefficients */
                n_LTP_Q13 = Inlines.silk_SMULWB(
                    Inlines.silk_ADD32(
                        this.sLTP_shp_Q14[shp_lag_ptr],
                        this.sLTP_shp_Q14[shp_lag_ptr - 2]
                    ), HarmShapeFIRPacked_Q14
                )
                n_LTP_Q13 = Inlines.silk_SMLAWT(n_LTP_Q13, this.sLTP_shp_Q14[shp_lag_ptr - 1], HarmShapeFIRPacked_Q14)
                n_LTP_Q13 = Inlines.silk_LSHIFT(n_LTP_Q13, 1)
                shp_lag_ptr += 1

                tmp2 = Inlines.silk_SUB32(LTP_pred_Q13, n_LTP_Q13)
                /* Q13 */
                tmp1 = Inlines.silk_ADD_LSHIFT32(tmp2, tmp1, 1)
                /* Q13 */
                tmp1 = Inlines.silk_RSHIFT_ROUND(tmp1, 3)
                /* Q10 */
            } else {
                tmp1 = Inlines.silk_RSHIFT_ROUND(tmp1, 2)
                /* Q10 */
            }

            r_Q10 = Inlines.silk_SUB32(x_sc_Q10[i], tmp1)
            /* residual error Q10 */

            /* Flip sign depending on dither */
            if (this.rand_seed < 0) {
                r_Q10 = -r_Q10
            }
            r_Q10 = Inlines.silk_LIMIT_32(r_Q10, -(31 shl 10), 30 shl 10)

            /* Find two quantization level candidates and measure their rate-distortion */
            q1_Q10 = Inlines.silk_SUB32(r_Q10, offset_Q10)
            q1_Q0 = Inlines.silk_RSHIFT(q1_Q10, 10)
            if (q1_Q0 > 0) {
                q1_Q10 = Inlines.silk_SUB32(Inlines.silk_LSHIFT(q1_Q0, 10), SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                q1_Q10 = Inlines.silk_ADD32(q1_Q10, offset_Q10)
                q2_Q10 = Inlines.silk_ADD32(q1_Q10, 1024)
                rd1_Q20 = Inlines.silk_SMULBB(q1_Q10, Lambda_Q10)
                rd2_Q20 = Inlines.silk_SMULBB(q2_Q10, Lambda_Q10)
            } else if (q1_Q0 == 0) {
                q1_Q10 = offset_Q10
                q2_Q10 = Inlines.silk_ADD32(q1_Q10, 1024 - SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                rd1_Q20 = Inlines.silk_SMULBB(q1_Q10, Lambda_Q10)
                rd2_Q20 = Inlines.silk_SMULBB(q2_Q10, Lambda_Q10)
            } else if (q1_Q0 == -1) {
                q2_Q10 = offset_Q10
                q1_Q10 = Inlines.silk_SUB32(q2_Q10, 1024 - SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                rd1_Q20 = Inlines.silk_SMULBB(-q1_Q10, Lambda_Q10)
                rd2_Q20 = Inlines.silk_SMULBB(q2_Q10, Lambda_Q10)
            } else {
                /* Q1_Q0 < -1 */
                q1_Q10 = Inlines.silk_ADD32(Inlines.silk_LSHIFT(q1_Q0, 10), SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                q1_Q10 = Inlines.silk_ADD32(q1_Q10, offset_Q10)
                q2_Q10 = Inlines.silk_ADD32(q1_Q10, 1024)
                rd1_Q20 = Inlines.silk_SMULBB(-q1_Q10, Lambda_Q10)
                rd2_Q20 = Inlines.silk_SMULBB(-q2_Q10, Lambda_Q10)
            }
            rr_Q10 = Inlines.silk_SUB32(r_Q10, q1_Q10)
            rd1_Q20 = Inlines.silk_SMLABB(rd1_Q20, rr_Q10, rr_Q10)
            rr_Q10 = Inlines.silk_SUB32(r_Q10, q2_Q10)
            rd2_Q20 = Inlines.silk_SMLABB(rd2_Q20, rr_Q10, rr_Q10)

            if (rd2_Q20 < rd1_Q20) {
                q1_Q10 = q2_Q10
            }

            pulses[pulses_ptr + i] = Inlines.silk_RSHIFT_ROUND(q1_Q10, 10).toByte()

            /* Excitation */
            exc_Q14 = Inlines.silk_LSHIFT(q1_Q10, 4)
            if (this.rand_seed < 0) {
                exc_Q14 = -exc_Q14
            }

            /* Add predictions */
            LPC_exc_Q14 = Inlines.silk_ADD_LSHIFT32(exc_Q14, LTP_pred_Q13, 1)
            xq_Q14 = Inlines.silk_ADD_LSHIFT32(LPC_exc_Q14, LPC_pred_Q10, 4)

            /* Scale XQ back to normal level before saving */
            xq[xq_ptr + i] =
                    Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(Inlines.silk_SMULWW(xq_Q14, Gain_Q10), 8)).toShort()

            /* Update states */
            psLPC_Q14 += 1
            this.sLPC_Q14[psLPC_Q14] = xq_Q14
            sLF_AR_shp_Q14 = Inlines.silk_SUB_LSHIFT32(xq_Q14, n_AR_Q12, 2)
            this.sLF_AR_shp_Q14 = sLF_AR_shp_Q14

            this.sLTP_shp_Q14[this.sLTP_shp_buf_idx] = Inlines.silk_SUB_LSHIFT32(sLF_AR_shp_Q14, n_LF_Q12, 2)
            sLTP_Q15[this.sLTP_buf_idx] = Inlines.silk_LSHIFT(LPC_exc_Q14, 1)
            this.sLTP_shp_buf_idx++
            this.sLTP_buf_idx++

            /* Make dither dependent on quantized signal */
            this.rand_seed = Inlines.silk_ADD32_ovflw(this.rand_seed, pulses[pulses_ptr + i].toInt())
            i++
        }

        /* Update LPC synth buffer */
        arraycopy(this.sLPC_Q14, length, this.sLPC_Q14, 0, SilkConstants.NSQ_LPC_BUF_LENGTH)
    }

    private fun silk_nsq_scale_states(
        psEncC: SilkChannelEncoder, /* I    Encoder State                   */
        x_Q3: IntArray, /* I    input in Q3                     */
        x_Q3_ptr: Int,
        x_sc_Q10: IntArray, /* O    input scaled with 1/Gain        */
        sLTP: ShortArray, /* I    re-whitened LTP state in Q0     */
        sLTP_Q15: IntArray, /* O    LTP state matching scaled input */
        subfr: Int, /* I    subframe number                 */
        LTP_scale_Q14: Int, /* I                                    */
        Gains_Q16: IntArray, /* I [MAX_NB_SUBFR]                                */
        pitchL: IntArray, /* I    Pitch lag [MAX_NB_SUBFR]                      */
        signal_type: Int /* I    Signal type                     */
    ) {
        var i: Int
        val lag: Int
        val gain_adj_Q16: Int
        var inv_gain_Q31: Int
        val inv_gain_Q23: Int

        lag = pitchL[subfr]
        inv_gain_Q31 = Inlines.silk_INVERSE32_varQ(Inlines.silk_max(Gains_Q16[subfr], 1), 47)
        Inlines.OpusAssert(inv_gain_Q31 != 0)

        /* Calculate gain adjustment factor */
        if (Gains_Q16[subfr] != this.prev_gain_Q16) {
            gain_adj_Q16 = Inlines.silk_DIV32_varQ(this.prev_gain_Q16, Gains_Q16[subfr], 16)
        } else {
            gain_adj_Q16 = 1 shl 16
        }

        /* Scale input */
        inv_gain_Q23 = Inlines.silk_RSHIFT_ROUND(inv_gain_Q31, 8)
        i = 0
        while (i < psEncC.subfr_length) {
            x_sc_Q10[i] = Inlines.silk_SMULWW(x_Q3[x_Q3_ptr + i], inv_gain_Q23)
            i++
        }

        /* Save inverse gain */
        this.prev_gain_Q16 = Gains_Q16[subfr]

        /* After rewhitening the LTP state is un-scaled, so scale with inv_gain_Q16 */
        if (this.rewhite_flag != 0) {
            if (subfr == 0) {
                /* Do LTP downscaling */
                inv_gain_Q31 = Inlines.silk_LSHIFT(Inlines.silk_SMULWB(inv_gain_Q31, LTP_scale_Q14), 2)
            }
            i = this.sLTP_buf_idx - lag - SilkConstants.LTP_ORDER / 2
            while (i < this.sLTP_buf_idx) {
                Inlines.OpusAssert(i < SilkConstants.MAX_FRAME_LENGTH)
                sLTP_Q15[i] = Inlines.silk_SMULWB(inv_gain_Q31, sLTP[i].toInt())
                i++
            }
        }

        /* Adjust for changing gain */
        if (gain_adj_Q16 != 1 shl 16) {
            /* Scale long-term shaping state */
            i = this.sLTP_shp_buf_idx - psEncC.ltp_mem_length
            while (i < this.sLTP_shp_buf_idx) {
                this.sLTP_shp_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, this.sLTP_shp_Q14[i])
                i++
            }

            /* Scale long-term prediction state */
            if (signal_type == SilkConstants.TYPE_VOICED && this.rewhite_flag == 0) {
                i = this.sLTP_buf_idx - lag - SilkConstants.LTP_ORDER / 2
                while (i < this.sLTP_buf_idx) {
                    sLTP_Q15[i] = Inlines.silk_SMULWW(gain_adj_Q16, sLTP_Q15[i])
                    i++
                }
            }

            this.sLF_AR_shp_Q14 = Inlines.silk_SMULWW(gain_adj_Q16, this.sLF_AR_shp_Q14)

            /* Scale short-term prediction and shaping states */
            i = 0
            while (i < SilkConstants.NSQ_LPC_BUF_LENGTH) {
                this.sLPC_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, this.sLPC_Q14[i])
                i++
            }
            i = 0
            while (i < SilkConstants.MAX_SHAPE_LPC_ORDER) {
                this.sAR2_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, this.sAR2_Q14[i])
                i++
            }
        }
    }

    fun silk_NSQ_del_dec(
        psEncC: SilkChannelEncoder, /* I  Encoder State                   */
        psIndices: SideInfoIndices, /* I/O  Quantization Indices            */
        x_Q3: IntArray, /* I    Prefiltered input signal        */
        pulses: ByteArray, /* O    Quantized pulse signal          */
        PredCoef_Q12: Array<ShortArray>, /* I    Short term prediction coefs [2 * MAX_LPC_ORDER]    */
        LTPCoef_Q14: ShortArray, /* I    Long term prediction coefs LTP_ORDER * MAX_NB_SUBFR]     */
        AR2_Q13: ShortArray, /* I Noise shaping coefs  [MAX_NB_SUBFR * MAX_SHAPE_LPC_ORDER]           */
        HarmShapeGain_Q14: IntArray, /* I    Long term shaping coefs [MAX_NB_SUBFR]        */
        Tilt_Q14: IntArray, /* I    Spectral tilt [MAX_NB_SUBFR]                  */
        LF_shp_Q14: IntArray, /* I    Low frequency shaping coefs [MAX_NB_SUBFR]    */
        Gains_Q16: IntArray, /* I    Quantization step sizes [MAX_NB_SUBFR]        */
        pitchL: IntArray, /* I    Pitch lags  [MAX_NB_SUBFR]                    */
        Lambda_Q10: Int, /* I    Rate/distortion tradeoff        */
        LTP_scale_Q14: Int /* I    LTP state scaling               */
    ) {
        var i: Int
        var k: Int
        var lag: Int
        var start_idx: Int
        val LSF_interpolation_flag: Int
        var Winner_ind: Int
        var subfr: Int
        var last_smple_idx: Int
        var smpl_buf_idx: Int
        var decisionDelay: Int
        var A_Q12: Int
        var pulses_ptr = 0
        var pxq: Int
        val sLTP_Q15: IntArray
        val sLTP: ShortArray
        var HarmShapeFIRPacked_Q14: Int
        val offset_Q10: Int
        var RDmin_Q10: Int
        val Gain_Q10: Int
        val x_sc_Q10: IntArray
        val delayedGain_Q10: IntArray
        var x_Q3_ptr = 0
        val psDelDec: Array<NSQ_del_dec_struct>
        var psDD: NSQ_del_dec_struct

        /* Set unvoiced lag to the previous one, overwrite later for voiced */
        lag = this.lagPrev

        Inlines.OpusAssert(this.prev_gain_Q16 != 0)

        /* Initialize delayed decision states */
        psDelDec = Array(psEncC.nStatesDelayedDecision) { NSQ_del_dec_struct(psEncC.shapingLPCOrder) }

        k = 0
        while (k < psEncC.nStatesDelayedDecision) {
            psDD = psDelDec[k]
            psDD.Seed = k + psIndices.Seed and 3
            psDD.SeedInit = psDD.Seed
            psDD.RD_Q10 = 0
            psDD.LF_AR_Q14 = this.sLF_AR_shp_Q14
            psDD.Shape_Q14[0] = this.sLTP_shp_Q14[psEncC.ltp_mem_length - 1]
            arraycopy(this.sLPC_Q14, 0, psDD.sLPC_Q14, 0, SilkConstants.NSQ_LPC_BUF_LENGTH)
            arraycopy(this.sAR2_Q14, 0, psDD.sAR2_Q14, 0, psEncC.shapingLPCOrder)
            k++
        }

        offset_Q10 =
                SilkTables.silk_Quantization_Offsets_Q10[psIndices.signalType shr 1][psIndices.quantOffsetType.toInt()].toInt()
        smpl_buf_idx = 0
        /* index of oldest samples */

        decisionDelay = Inlines.silk_min_int(SilkConstants.DECISION_DELAY, psEncC.subfr_length)

        /* For voiced frames limit the decision delay to lower than the pitch lag */
        if (psIndices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            k = 0
            while (k < psEncC.nb_subfr) {
                decisionDelay = Inlines.silk_min_int(decisionDelay, pitchL[k] - SilkConstants.LTP_ORDER / 2 - 1)
                k++
            }
        } else if (lag > 0) {
            decisionDelay = Inlines.silk_min_int(decisionDelay, lag - SilkConstants.LTP_ORDER / 2 - 1)
        }

        if (psIndices.NLSFInterpCoef_Q2.toInt() == 4) {
            LSF_interpolation_flag = 0
        } else {
            LSF_interpolation_flag = 1
        }

        sLTP_Q15 = IntArray(psEncC.ltp_mem_length + psEncC.frame_length)
        sLTP = ShortArray(psEncC.ltp_mem_length + psEncC.frame_length)
        x_sc_Q10 = IntArray(psEncC.subfr_length)
        delayedGain_Q10 = IntArray(SilkConstants.DECISION_DELAY)

        /* Set up pointers to start of sub frame */
        pxq = psEncC.ltp_mem_length
        this.sLTP_shp_buf_idx = psEncC.ltp_mem_length
        this.sLTP_buf_idx = psEncC.ltp_mem_length
        subfr = 0
        k = 0
        while (k < psEncC.nb_subfr) {
            A_Q12 = k shr 1 or 1 - LSF_interpolation_flag

            /* Noise shape parameters */
            Inlines.OpusAssert(HarmShapeGain_Q14[k] >= 0)
            HarmShapeFIRPacked_Q14 = Inlines.silk_RSHIFT(HarmShapeGain_Q14[k], 2)
            HarmShapeFIRPacked_Q14 = HarmShapeFIRPacked_Q14 or
                    Inlines.silk_LSHIFT(Inlines.silk_RSHIFT(HarmShapeGain_Q14[k], 1).toInt(), 16)

            this.rewhite_flag = 0
            if (psIndices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
                /* Voiced */
                lag = pitchL[k]

                /* Re-whitening */
                if (k and 3 - Inlines.silk_LSHIFT(LSF_interpolation_flag, 1) == 0) {
                    if (k == 2) {
                        /* RESET DELAYED DECISIONS */
                        /* Find winner */
                        RDmin_Q10 = psDelDec[0].RD_Q10
                        Winner_ind = 0
                        i = 1
                        while (i < psEncC.nStatesDelayedDecision) {
                            if (psDelDec[i].RD_Q10 < RDmin_Q10) {
                                RDmin_Q10 = psDelDec[i].RD_Q10
                                Winner_ind = i
                            }
                            i++
                        }
                        i = 0
                        while (i < psEncC.nStatesDelayedDecision) {
                            if (i != Winner_ind) {
                                psDelDec[i].RD_Q10 += Int.MAX_VALUE shr 4
                                Inlines.OpusAssert(psDelDec[i].RD_Q10 >= 0)
                            }
                            i++
                        }

                        /* Copy final part of signals from winner state to output and long-term filter states */
                        psDD = psDelDec[Winner_ind]
                        last_smple_idx = smpl_buf_idx + decisionDelay
                        i = 0
                        while (i < decisionDelay) {
                            last_smple_idx = last_smple_idx - 1 and SilkConstants.DECISION_DELAY_MASK
                            pulses[pulses_ptr + i - decisionDelay] =
                                    Inlines.silk_RSHIFT_ROUND(psDD.Q_Q10[last_smple_idx], 10).toByte()
                            this.xq[pxq + i - decisionDelay] = Inlines.silk_SAT16(
                                Inlines.silk_RSHIFT_ROUND(
                                    Inlines.silk_SMULWW(psDD.Xq_Q14[last_smple_idx], Gains_Q16[1]), 14
                                )
                            ).toShort()
                            this.sLTP_shp_Q14[this.sLTP_shp_buf_idx - decisionDelay + i] =
                                    psDD.Shape_Q14[last_smple_idx]
                            i++
                        }

                        subfr = 0
                    }

                    /* Rewhiten with new A coefs */
                    start_idx = psEncC.ltp_mem_length - lag - psEncC.predictLPCOrder - SilkConstants.LTP_ORDER / 2
                    Inlines.OpusAssert(start_idx > 0)

                    Filters.silk_LPC_analysis_filter(
                        sLTP, start_idx, this.xq, start_idx + k * psEncC.subfr_length,
                        PredCoef_Q12[A_Q12], 0, psEncC.ltp_mem_length - start_idx, psEncC.predictLPCOrder
                    )

                    this.sLTP_buf_idx = psEncC.ltp_mem_length
                    this.rewhite_flag = 1
                }
            }

            silk_nsq_del_dec_scale_states(
                psEncC,
                psDelDec,
                x_Q3,
                x_Q3_ptr,
                x_sc_Q10,
                sLTP,
                sLTP_Q15,
                k,
                psEncC.nStatesDelayedDecision,
                LTP_scale_Q14,
                Gains_Q16,
                pitchL,
                psIndices.signalType.toInt(),
                decisionDelay
            )

            val smpl_buf_idx_boxed = BoxedValueInt(smpl_buf_idx)
            silk_noise_shape_quantizer_del_dec(
                psDelDec,
                psIndices.signalType.toInt(),
                x_sc_Q10,
                pulses,
                pulses_ptr,
                this.xq,
                pxq,
                sLTP_Q15,
                delayedGain_Q10,
                PredCoef_Q12[A_Q12],
                LTPCoef_Q14,
                k * SilkConstants.LTP_ORDER,
                AR2_Q13,
                k * SilkConstants.MAX_SHAPE_LPC_ORDER,
                lag,
                HarmShapeFIRPacked_Q14,
                Tilt_Q14[k],
                LF_shp_Q14[k],
                Gains_Q16[k],
                Lambda_Q10,
                offset_Q10,
                psEncC.subfr_length,
                subfr++,
                psEncC.shapingLPCOrder,
                psEncC.predictLPCOrder,
                psEncC.warping_Q16,
                psEncC.nStatesDelayedDecision,
                smpl_buf_idx_boxed,
                decisionDelay
            )
            smpl_buf_idx = smpl_buf_idx_boxed.Val

            x_Q3_ptr += psEncC.subfr_length
            pulses_ptr += psEncC.subfr_length
            pxq += psEncC.subfr_length
            k++
        }

        /* Find winner */
        RDmin_Q10 = psDelDec[0].RD_Q10
        Winner_ind = 0
        k = 1
        while (k < psEncC.nStatesDelayedDecision) {
            if (psDelDec[k].RD_Q10 < RDmin_Q10) {
                RDmin_Q10 = psDelDec[k].RD_Q10
                Winner_ind = k
            }
            k++
        }

        /* Copy final part of signals from winner state to output and long-term filter states */
        psDD = psDelDec[Winner_ind]
        psIndices.Seed = psDD.SeedInit.toByte()
        last_smple_idx = smpl_buf_idx + decisionDelay
        Gain_Q10 = Inlines.silk_RSHIFT32(Gains_Q16[psEncC.nb_subfr - 1], 6)
        i = 0
        while (i < decisionDelay) {
            last_smple_idx = last_smple_idx - 1 and SilkConstants.DECISION_DELAY_MASK
            pulses[pulses_ptr + i - decisionDelay] = Inlines.silk_RSHIFT_ROUND(psDD.Q_Q10[last_smple_idx], 10).toByte()
            this.xq[pxq + i - decisionDelay] = Inlines.silk_SAT16(
                Inlines.silk_RSHIFT_ROUND(
                    Inlines.silk_SMULWW(psDD.Xq_Q14[last_smple_idx], Gain_Q10), 8
                )
            ).toShort()
            this.sLTP_shp_Q14[this.sLTP_shp_buf_idx - decisionDelay + i] = psDD.Shape_Q14[last_smple_idx]
            i++
        }
        arraycopy(psDD.sLPC_Q14, psEncC.subfr_length, this.sLPC_Q14, 0, SilkConstants.NSQ_LPC_BUF_LENGTH)
        arraycopy(psDD.sAR2_Q14, 0, this.sAR2_Q14, 0, psEncC.shapingLPCOrder)

        /* Update states */
        this.sLF_AR_shp_Q14 = psDD.LF_AR_Q14
        this.lagPrev = pitchL[psEncC.nb_subfr - 1]

        /* Save quantized speech signal */
        Arrays.MemMove(this.xq, psEncC.frame_length, 0, psEncC.ltp_mem_length)
        Arrays.MemMove(this.sLTP_shp_Q14, psEncC.frame_length, 0, psEncC.ltp_mem_length)
    }

    /**
     * ***************************************
     */
    /* Noise shape quantizer for one subframe */
    /**
     * ***************************************
     */
    private fun silk_noise_shape_quantizer_del_dec(
        psDelDec: Array<NSQ_del_dec_struct>, /* I/O  Delayed decision states             */
        signalType: Int, /* I    Signal type                         */
        x_Q10: IntArray, /* I                                        */
        pulses: ByteArray, /* O                                        */
        pulses_ptr: Int,
        xq: ShortArray, /* O                                        */
        xq_ptr: Int,
        sLTP_Q15: IntArray, /* I/O  LTP filter state                    */
        delayedGain_Q10: IntArray, /* I/O  Gain delay buffer                   */
        a_Q12: ShortArray, /* I    Short term prediction coefs         */
        b_Q14: ShortArray, /* I    Long term prediction coefs          */
        b_Q14_ptr: Int,
        AR_shp_Q13: ShortArray, /* I    Noise shaping coefs                 */
        AR_shp_Q13_ptr: Int,
        lag: Int, /* I    Pitch lag                           */
        HarmShapeFIRPacked_Q14: Int, /* I                                        */
        Tilt_Q14: Int, /* I    Spectral tilt                       */
        LF_shp_Q14: Int, /* I                                        */
        Gain_Q16: Int, /* I                                        */
        Lambda_Q10: Int, /* I                                        */
        offset_Q10: Int, /* I                                        */
        length: Int, /* I    Input length                        */
        subfr: Int, /* I    Subframe number                     */
        shapingLPCOrder: Int, /* I    Shaping LPC filter order            */
        predictLPCOrder: Int, /* I    Prediction filter order             */
        warping_Q16: Int, /* I                                        */
        nStatesDelayedDecision: Int, /* I    Number of states in decision tree   */
        smpl_buf_idx: BoxedValueInt, /* I    Index to newest samples in buffers  */
        decisionDelay: Int /* I                                        */
    ) {
        var i: Int
        var j: Int
        var k: Int
        var Winner_ind: Int
        var RDmin_ind: Int
        var RDmax_ind: Int
        var last_smple_idx: Int
        var Winner_rand_state: Int
        var LTP_pred_Q14: Int
        var LPC_pred_Q14: Int
        var n_AR_Q14: Int
        var n_LTP_Q14: Int
        var n_LF_Q14: Int
        var r_Q10: Int
        var rr_Q10: Int
        var rd1_Q10: Int
        var rd2_Q10: Int
        var RDmin_Q10: Int
        var RDmax_Q10: Int
        var q1_Q0: Int
        var q1_Q10: Int
        var q2_Q10: Int
        var exc_Q14: Int
        var LPC_exc_Q14: Int
        var xq_Q14: Int
        val Gain_Q10: Int
        var tmp1: Int
        var tmp2: Int
        var sLF_AR_shp_Q14: Int
        var pred_lag_ptr: Int
        var shp_lag_ptr: Int
        var psLPC_Q14: Int
        val sampleStates: Array<NSQ_sample_struct>
        var psDD: NSQ_del_dec_struct
        var SS_left: Int
        var SS_right: Int

        Inlines.OpusAssert(nStatesDelayedDecision > 0)
		// [porting note] structs must be initialized manually here
        sampleStates = Array(2 * nStatesDelayedDecision) { NSQ_sample_struct() }

        shp_lag_ptr = this.sLTP_shp_buf_idx - lag + SilkConstants.HARM_SHAPE_FIR_TAPS / 2
        pred_lag_ptr = this.sLTP_buf_idx - lag + SilkConstants.LTP_ORDER / 2
        Gain_Q10 = Inlines.silk_RSHIFT(Gain_Q16, 6)

        i = 0
        while (i < length) {
            /* Perform common calculations used in all states */

            /* Long-term prediction */
            if (signalType == SilkConstants.TYPE_VOICED) {
                /* Unrolled loop */
                /* Avoids introducing a bias because Inlines.silk_SMLAWB() always rounds to -inf */
                LTP_pred_Q14 = 2
                LTP_pred_Q14 = Inlines.silk_SMLAWB(LTP_pred_Q14, sLTP_Q15[pred_lag_ptr], b_Q14[b_Q14_ptr + 0].toInt())
                LTP_pred_Q14 =
                        Inlines.silk_SMLAWB(LTP_pred_Q14, sLTP_Q15[pred_lag_ptr - 1], b_Q14[b_Q14_ptr + 1].toInt())
                LTP_pred_Q14 =
                        Inlines.silk_SMLAWB(LTP_pred_Q14, sLTP_Q15[pred_lag_ptr - 2], b_Q14[b_Q14_ptr + 2].toInt())
                LTP_pred_Q14 =
                        Inlines.silk_SMLAWB(LTP_pred_Q14, sLTP_Q15[pred_lag_ptr - 3], b_Q14[b_Q14_ptr + 3].toInt())
                LTP_pred_Q14 =
                        Inlines.silk_SMLAWB(LTP_pred_Q14, sLTP_Q15[pred_lag_ptr - 4], b_Q14[b_Q14_ptr + 4].toInt())
                LTP_pred_Q14 = Inlines.silk_LSHIFT(LTP_pred_Q14, 1)
                /* Q13 . Q14 */
                pred_lag_ptr += 1
            } else {
                LTP_pred_Q14 = 0
            }

            /* Long-term shaping */
            if (lag > 0) {
                /* Symmetric, packed FIR coefficients */
                n_LTP_Q14 = Inlines.silk_SMULWB(
                    Inlines.silk_ADD32(
                        this.sLTP_shp_Q14[shp_lag_ptr],
                        this.sLTP_shp_Q14[shp_lag_ptr - 2]
                    ), HarmShapeFIRPacked_Q14
                )
                n_LTP_Q14 = Inlines.silk_SMLAWT(n_LTP_Q14, this.sLTP_shp_Q14[shp_lag_ptr - 1], HarmShapeFIRPacked_Q14)
                n_LTP_Q14 = Inlines.silk_SUB_LSHIFT32(LTP_pred_Q14, n_LTP_Q14, 2)
                /* Q12 . Q14 */
                shp_lag_ptr += 1
            } else {
                n_LTP_Q14 = 0
            }

            k = 0
            while (k < nStatesDelayedDecision) {
                /* Delayed decision state */
                psDD = psDelDec[k]
                val psDD_sAR2 = psDD.sAR2_Q14

                /* Sample state */
                SS_left = 2 * k
                SS_right = SS_left + 1

                /* Generate dither */
                psDD.Seed = Inlines.silk_RAND(psDD.Seed)

                /* Pointer used in short term prediction and shaping */
                psLPC_Q14 = SilkConstants.NSQ_LPC_BUF_LENGTH - 1 + i
                /* Short-term prediction */
                Inlines.OpusAssert(predictLPCOrder == 10 || predictLPCOrder == 16)
                /* Avoids introducing a bias because Inlines.silk_SMLAWB() always rounds to -inf */
                LPC_pred_Q14 = Inlines.silk_RSHIFT(predictLPCOrder, 1)
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14], a_Q12[0].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 1], a_Q12[1].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 2], a_Q12[2].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 3], a_Q12[3].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 4], a_Q12[4].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 5], a_Q12[5].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 6], a_Q12[6].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 7], a_Q12[7].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 8], a_Q12[8].toInt())
                LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 9], a_Q12[9].toInt())
                if (predictLPCOrder == 16) {
                    LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 10], a_Q12[10].toInt())
                    LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 11], a_Q12[11].toInt())
                    LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 12], a_Q12[12].toInt())
                    LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 13], a_Q12[13].toInt())
                    LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 14], a_Q12[14].toInt())
                    LPC_pred_Q14 = Inlines.silk_SMLAWB(LPC_pred_Q14, psDD.sLPC_Q14[psLPC_Q14 - 15], a_Q12[15].toInt())
                }
                LPC_pred_Q14 = Inlines.silk_LSHIFT(LPC_pred_Q14, 4)
                /* Q10 . Q14 */


                /* Noise shape feedback */
                Inlines.OpusAssert(shapingLPCOrder and 1 == 0)
                /* check that order is even */
                /* Output of lowpass section */
                tmp2 = Inlines.silk_SMLAWB(psDD.sLPC_Q14[psLPC_Q14], psDD_sAR2[0], warping_Q16)
                /* Output of allpass section */
                tmp1 = Inlines.silk_SMLAWB(psDD_sAR2[0], psDD_sAR2[1] - tmp2, warping_Q16)
                psDD_sAR2[0] = tmp2
                n_AR_Q14 = Inlines.silk_RSHIFT(shapingLPCOrder, 1)
                n_AR_Q14 = Inlines.silk_SMLAWB(n_AR_Q14, tmp2, AR_shp_Q13[AR_shp_Q13_ptr].toInt())
                /* Loop over allpass sections */
                j = 2
                while (j < shapingLPCOrder) {
                    /* Output of allpass section */
                    tmp2 = Inlines.silk_SMLAWB(psDD_sAR2[j - 1], psDD_sAR2[j + 0] - tmp1, warping_Q16)
                    psDD_sAR2[j - 1] = tmp1
                    n_AR_Q14 = Inlines.silk_SMLAWB(n_AR_Q14, tmp1, AR_shp_Q13[AR_shp_Q13_ptr + j - 1].toInt())
                    /* Output of allpass section */
                    tmp1 = Inlines.silk_SMLAWB(psDD_sAR2[j + 0], psDD_sAR2[j + 1] - tmp2, warping_Q16)
                    psDD_sAR2[j + 0] = tmp2
                    n_AR_Q14 = Inlines.silk_SMLAWB(n_AR_Q14, tmp2, AR_shp_Q13[AR_shp_Q13_ptr + j].toInt())
                    j += 2
                }
                psDD_sAR2[shapingLPCOrder - 1] = tmp1
                n_AR_Q14 = Inlines.silk_SMLAWB(n_AR_Q14, tmp1, AR_shp_Q13[AR_shp_Q13_ptr + shapingLPCOrder - 1].toInt())

                n_AR_Q14 = Inlines.silk_LSHIFT(n_AR_Q14, 1)
                /* Q11 . Q12 */
                n_AR_Q14 = Inlines.silk_SMLAWB(n_AR_Q14, psDD.LF_AR_Q14, Tilt_Q14)
                /* Q12 */
                n_AR_Q14 = Inlines.silk_LSHIFT(n_AR_Q14, 2)
                /* Q12 . Q14 */

                n_LF_Q14 = Inlines.silk_SMULWB(psDD.Shape_Q14[smpl_buf_idx.Val], LF_shp_Q14)
                /* Q12 */
                n_LF_Q14 = Inlines.silk_SMLAWT(n_LF_Q14, psDD.LF_AR_Q14, LF_shp_Q14)
                /* Q12 */
                n_LF_Q14 = Inlines.silk_LSHIFT(n_LF_Q14, 2)
                /* Q12 . Q14 */

                /* Input minus prediction plus noise feedback                       */
                /* r = x[ i ] - LTP_pred - LPC_pred + n_AR + n_Tilt + n_LF + n_LTP  */
                tmp1 = Inlines.silk_ADD32(n_AR_Q14, n_LF_Q14)
                /* Q14 */
                tmp2 = Inlines.silk_ADD32(n_LTP_Q14, LPC_pred_Q14)
                /* Q13 */
                tmp1 = Inlines.silk_SUB32(tmp2, tmp1)
                /* Q13 */
                tmp1 = Inlines.silk_RSHIFT_ROUND(tmp1, 4)
                /* Q10 */

                r_Q10 = Inlines.silk_SUB32(x_Q10[i], tmp1)
                /* residual error Q10 */

                /* Flip sign depending on dither */
                if (psDD.Seed < 0) {
                    r_Q10 = -r_Q10
                }
                r_Q10 = Inlines.silk_LIMIT_32(r_Q10, -(31 shl 10), 30 shl 10)

                /* Find two quantization level candidates and measure their rate-distortion */
                q1_Q10 = Inlines.silk_SUB32(r_Q10, offset_Q10)
                q1_Q0 = Inlines.silk_RSHIFT(q1_Q10, 10)
                if (q1_Q0 > 0) {
                    q1_Q10 = Inlines.silk_SUB32(Inlines.silk_LSHIFT(q1_Q0, 10), SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                    q1_Q10 = Inlines.silk_ADD32(q1_Q10, offset_Q10)
                    q2_Q10 = Inlines.silk_ADD32(q1_Q10, 1024)
                    rd1_Q10 = Inlines.silk_SMULBB(q1_Q10, Lambda_Q10)
                    rd2_Q10 = Inlines.silk_SMULBB(q2_Q10, Lambda_Q10)
                } else if (q1_Q0 == 0) {
                    q1_Q10 = offset_Q10
                    q2_Q10 = Inlines.silk_ADD32(q1_Q10, 1024 - SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                    rd1_Q10 = Inlines.silk_SMULBB(q1_Q10, Lambda_Q10)
                    rd2_Q10 = Inlines.silk_SMULBB(q2_Q10, Lambda_Q10)
                } else if (q1_Q0 == -1) {
                    q2_Q10 = offset_Q10
                    q1_Q10 = Inlines.silk_SUB32(q2_Q10, 1024 - SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                    rd1_Q10 = Inlines.silk_SMULBB(-q1_Q10, Lambda_Q10)
                    rd2_Q10 = Inlines.silk_SMULBB(q2_Q10, Lambda_Q10)
                } else {
                    /* q1_Q0 < -1 */
                    q1_Q10 = Inlines.silk_ADD32(Inlines.silk_LSHIFT(q1_Q0, 10), SilkConstants.QUANT_LEVEL_ADJUST_Q10)
                    q1_Q10 = Inlines.silk_ADD32(q1_Q10, offset_Q10)
                    q2_Q10 = Inlines.silk_ADD32(q1_Q10, 1024)
                    rd1_Q10 = Inlines.silk_SMULBB(-q1_Q10, Lambda_Q10)
                    rd2_Q10 = Inlines.silk_SMULBB(-q2_Q10, Lambda_Q10)
                }
                rr_Q10 = Inlines.silk_SUB32(r_Q10, q1_Q10)
                rd1_Q10 = Inlines.silk_RSHIFT(Inlines.silk_SMLABB(rd1_Q10, rr_Q10, rr_Q10), 10)
                rr_Q10 = Inlines.silk_SUB32(r_Q10, q2_Q10)
                rd2_Q10 = Inlines.silk_RSHIFT(Inlines.silk_SMLABB(rd2_Q10, rr_Q10, rr_Q10), 10)

                if (rd1_Q10 < rd2_Q10) {
                    sampleStates[SS_left].RD_Q10 = Inlines.silk_ADD32(psDD.RD_Q10, rd1_Q10)
                    sampleStates[SS_right].RD_Q10 = Inlines.silk_ADD32(psDD.RD_Q10, rd2_Q10)
                    sampleStates[SS_left].Q_Q10 = q1_Q10
                    sampleStates[SS_right].Q_Q10 = q2_Q10
                } else {
                    sampleStates[SS_left].RD_Q10 = Inlines.silk_ADD32(psDD.RD_Q10, rd2_Q10)
                    sampleStates[SS_right].RD_Q10 = Inlines.silk_ADD32(psDD.RD_Q10, rd1_Q10)
                    sampleStates[SS_left].Q_Q10 = q2_Q10
                    sampleStates[SS_right].Q_Q10 = q1_Q10
                }

                /* Update states for best quantization */

                /* Quantized excitation */
                exc_Q14 = Inlines.silk_LSHIFT32(sampleStates[SS_left].Q_Q10, 4)
                if (psDD.Seed < 0) {
                    exc_Q14 = -exc_Q14
                }

                /* Add predictions */
                LPC_exc_Q14 = Inlines.silk_ADD32(exc_Q14, LTP_pred_Q14)
                xq_Q14 = Inlines.silk_ADD32(LPC_exc_Q14, LPC_pred_Q14)

                /* Update states */
                sLF_AR_shp_Q14 = Inlines.silk_SUB32(xq_Q14, n_AR_Q14)
                sampleStates[SS_left].sLTP_shp_Q14 = Inlines.silk_SUB32(sLF_AR_shp_Q14, n_LF_Q14)
                sampleStates[SS_left].LF_AR_Q14 = sLF_AR_shp_Q14
                sampleStates[SS_left].LPC_exc_Q14 = LPC_exc_Q14
                sampleStates[SS_left].xq_Q14 = xq_Q14

                /* Update states for second best quantization */

                /* Quantized excitation */
                exc_Q14 = Inlines.silk_LSHIFT32(sampleStates[SS_right].Q_Q10, 4)
                if (psDD.Seed < 0) {
                    exc_Q14 = -exc_Q14
                }


                /* Add predictions */
                LPC_exc_Q14 = Inlines.silk_ADD32(exc_Q14, LTP_pred_Q14)
                xq_Q14 = Inlines.silk_ADD32(LPC_exc_Q14, LPC_pred_Q14)

                /* Update states */
                sLF_AR_shp_Q14 = Inlines.silk_SUB32(xq_Q14, n_AR_Q14)
                sampleStates[SS_right].sLTP_shp_Q14 = Inlines.silk_SUB32(sLF_AR_shp_Q14, n_LF_Q14)
                sampleStates[SS_right].LF_AR_Q14 = sLF_AR_shp_Q14
                sampleStates[SS_right].LPC_exc_Q14 = LPC_exc_Q14
                sampleStates[SS_right].xq_Q14 = xq_Q14
                k++
            }

            smpl_buf_idx.Val = smpl_buf_idx.Val - 1 and SilkConstants.DECISION_DELAY_MASK
            /* Index to newest samples              */
            last_smple_idx = smpl_buf_idx.Val + decisionDelay and SilkConstants.DECISION_DELAY_MASK
            /* Index to decisionDelay old samples   */

            /* Find winner */
            RDmin_Q10 = sampleStates[0].RD_Q10
            Winner_ind = 0
            k = 1
            while (k < nStatesDelayedDecision) {
                if (sampleStates[k * 2].RD_Q10 < RDmin_Q10) {
                    RDmin_Q10 = sampleStates[k * 2].RD_Q10
                    Winner_ind = k
                }
                k++
            }

            /* Increase RD values of expired states */
            Winner_rand_state = psDelDec[Winner_ind].RandState[last_smple_idx]
            k = 0
            while (k < nStatesDelayedDecision) {
                if (psDelDec[k].RandState[last_smple_idx] != Winner_rand_state) {
                    val k2 = k * 2
                    sampleStates[k2].RD_Q10 = Inlines.silk_ADD32(sampleStates[k2].RD_Q10, Int.MAX_VALUE shr 4)
                    sampleStates[k2 + 1].RD_Q10 =
                            Inlines.silk_ADD32(sampleStates[k2 + 1].RD_Q10, Int.MAX_VALUE shr 4)
                    Inlines.OpusAssert(sampleStates[k2].RD_Q10 >= 0)
                }
                k++
            }

            /* Find worst in first set and best in second set */
            RDmax_Q10 = sampleStates[0].RD_Q10
            RDmin_Q10 = sampleStates[1].RD_Q10
            RDmax_ind = 0
            RDmin_ind = 0
            k = 1
            while (k < nStatesDelayedDecision) {
                val k2 = k * 2
                /* find worst in first set */
                if (sampleStates[k2].RD_Q10 > RDmax_Q10) {
                    RDmax_Q10 = sampleStates[k2].RD_Q10
                    RDmax_ind = k
                }
                /* find best in second set */
                if (sampleStates[k2 + 1].RD_Q10 < RDmin_Q10) {
                    RDmin_Q10 = sampleStates[k2 + 1].RD_Q10
                    RDmin_ind = k
                }
                k++
            }

            /* Replace a state if best from second set outperforms worst in first set */
            if (RDmin_Q10 < RDmax_Q10) {
                psDelDec[RDmax_ind].PartialCopyFrom(psDelDec[RDmin_ind], i)
                sampleStates[RDmax_ind * 2].Assign(sampleStates[RDmin_ind * 2 + 1])
            }

            /* Write samples from winner to output and long-term filter states */
            psDD = psDelDec[Winner_ind]
            if (subfr > 0 || i >= decisionDelay) {
                pulses[pulses_ptr + i - decisionDelay] =
                        Inlines.silk_RSHIFT_ROUND(psDD.Q_Q10[last_smple_idx], 10).toByte()
                xq[xq_ptr + i - decisionDelay] = Inlines.silk_SAT16(
                    Inlines.silk_RSHIFT_ROUND(
                        Inlines.silk_SMULWW(psDD.Xq_Q14[last_smple_idx], delayedGain_Q10[last_smple_idx]), 8
                    )
                ).toShort()
                this.sLTP_shp_Q14[this.sLTP_shp_buf_idx - decisionDelay] = psDD.Shape_Q14[last_smple_idx]
                sLTP_Q15[this.sLTP_buf_idx - decisionDelay] = psDD.Pred_Q15[last_smple_idx]
            }
            this.sLTP_shp_buf_idx++
            this.sLTP_buf_idx++

            /* Update states */
            k = 0
            while (k < nStatesDelayedDecision) {
                psDD = psDelDec[k]
                SS_left = k * 2
                psDD.LF_AR_Q14 = sampleStates[SS_left].LF_AR_Q14
                psDD.sLPC_Q14[SilkConstants.NSQ_LPC_BUF_LENGTH + i] = sampleStates[SS_left].xq_Q14
                psDD.Xq_Q14[smpl_buf_idx.Val] = sampleStates[SS_left].xq_Q14
                psDD.Q_Q10[smpl_buf_idx.Val] = sampleStates[SS_left].Q_Q10
                psDD.Pred_Q15[smpl_buf_idx.Val] = Inlines.silk_LSHIFT32(sampleStates[SS_left].LPC_exc_Q14, 1)
                psDD.Shape_Q14[smpl_buf_idx.Val] = sampleStates[SS_left].sLTP_shp_Q14
                psDD.Seed =
                        Inlines.silk_ADD32_ovflw(psDD.Seed, Inlines.silk_RSHIFT_ROUND(sampleStates[SS_left].Q_Q10, 10))
                psDD.RandState[smpl_buf_idx.Val] = psDD.Seed
                psDD.RD_Q10 = sampleStates[SS_left].RD_Q10
                k++
            }
            delayedGain_Q10[smpl_buf_idx.Val] = Gain_Q10
            i++
        }

        /* Update LPC states */
        k = 0
        while (k < nStatesDelayedDecision) {
            psDD = psDelDec[k]
            arraycopy(psDD.sLPC_Q14, length, psDD.sLPC_Q14, 0, SilkConstants.NSQ_LPC_BUF_LENGTH)
            k++
        }
    }

    private fun silk_nsq_del_dec_scale_states(
        psEncC: SilkChannelEncoder, /* I    Encoder State                       */
        psDelDec: Array<NSQ_del_dec_struct>, /* I/O  Delayed decision states             */
        x_Q3: IntArray, /* I    Input in Q3                         */
        x_Q3_ptr: Int,
        x_sc_Q10: IntArray, /* O    Input scaled with 1/Gain in Q10     */
        sLTP: ShortArray, /* I    Re-whitened LTP state in Q0         */
        sLTP_Q15: IntArray, /* O    LTP state matching scaled input     */
        subfr: Int, /* I    Subframe number                     */
        nStatesDelayedDecision: Int, /* I    Number of del dec states            */
        LTP_scale_Q14: Int, /* I    LTP state scaling                   */
        Gains_Q16: IntArray, /* I [MAX_NB_SUBFR]                                       */
        pitchL: IntArray, /* I    Pitch lag [MAX_NB_SUBFR]                          */
        signal_type: Int, /* I    Signal type                         */
        decisionDelay: Int /* I    Decision delay                      */
    ) {
        var i: Int
        var k: Int
        val lag: Int
        val gain_adj_Q16: Int
        var inv_gain_Q31: Int
        val inv_gain_Q23: Int
        var psDD: NSQ_del_dec_struct

        lag = pitchL[subfr]
        inv_gain_Q31 = Inlines.silk_INVERSE32_varQ(Inlines.silk_max(Gains_Q16[subfr], 1), 47)
        Inlines.OpusAssert(inv_gain_Q31 != 0)

        /* Calculate gain adjustment factor */
        if (Gains_Q16[subfr] != this.prev_gain_Q16) {
            gain_adj_Q16 = Inlines.silk_DIV32_varQ(this.prev_gain_Q16, Gains_Q16[subfr], 16)
        } else {
            gain_adj_Q16 = 1 shl 16
        }

        /* Scale input */
        inv_gain_Q23 = Inlines.silk_RSHIFT_ROUND(inv_gain_Q31, 8)
        i = 0
        while (i < psEncC.subfr_length) {
            x_sc_Q10[i] = Inlines.silk_SMULWW(x_Q3[x_Q3_ptr + i], inv_gain_Q23)
            i++
        }

        /* Save inverse gain */
        this.prev_gain_Q16 = Gains_Q16[subfr]

        /* After rewhitening the LTP state is un-scaled, so scale with inv_gain_Q16 */
        if (this.rewhite_flag != 0) {
            if (subfr == 0) {
                /* Do LTP downscaling */
                inv_gain_Q31 = Inlines.silk_LSHIFT(Inlines.silk_SMULWB(inv_gain_Q31, LTP_scale_Q14), 2)
            }
            i = this.sLTP_buf_idx - lag - SilkConstants.LTP_ORDER / 2
            while (i < this.sLTP_buf_idx) {
                Inlines.OpusAssert(i < SilkConstants.MAX_FRAME_LENGTH)
                sLTP_Q15[i] = Inlines.silk_SMULWB(inv_gain_Q31, sLTP[i].toInt())
                i++
            }
        }

        /* Adjust for changing gain */
        if (gain_adj_Q16 != 1 shl 16) {
            /* Scale long-term shaping state */
            i = this.sLTP_shp_buf_idx - psEncC.ltp_mem_length
            while (i < this.sLTP_shp_buf_idx) {
                this.sLTP_shp_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, this.sLTP_shp_Q14[i])
                i++
            }

            /* Scale long-term prediction state */
            if (signal_type == SilkConstants.TYPE_VOICED && this.rewhite_flag == 0) {
                i = this.sLTP_buf_idx - lag - SilkConstants.LTP_ORDER / 2
                while (i < this.sLTP_buf_idx - decisionDelay) {
                    sLTP_Q15[i] = Inlines.silk_SMULWW(gain_adj_Q16, sLTP_Q15[i])
                    i++
                }
            }

            k = 0
            while (k < nStatesDelayedDecision) {
                psDD = psDelDec[k]

                /* Scale scalar states */
                psDD.LF_AR_Q14 = Inlines.silk_SMULWW(gain_adj_Q16, psDD.LF_AR_Q14)

                /* Scale short-term prediction and shaping states */
                i = 0
                while (i < SilkConstants.NSQ_LPC_BUF_LENGTH) {
                    psDD.sLPC_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, psDD.sLPC_Q14[i])
                    i++
                }
                i = 0
                while (i < psEncC.shapingLPCOrder) {
                    psDD.sAR2_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, psDD.sAR2_Q14[i])
                    i++
                }
                i = 0
                while (i < SilkConstants.DECISION_DELAY) {
                    psDD.Pred_Q15[i] = Inlines.silk_SMULWW(gain_adj_Q16, psDD.Pred_Q15[i])
                    psDD.Shape_Q14[i] = Inlines.silk_SMULWW(gain_adj_Q16, psDD.Shape_Q14[i])
                    i++
                }
                k++
            }
        }
    }
}
