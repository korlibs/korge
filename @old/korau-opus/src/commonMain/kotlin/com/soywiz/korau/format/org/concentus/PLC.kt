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

/// <summary>
/// Routines for managing packet loss concealment
/// </summary>
internal object PLC {

    private val NB_ATT = 2
    private val HARM_ATT_Q15 = shortArrayOf(32440, 31130)
    /* 0.99, 0.95 */
    private val PLC_RAND_ATTENUATE_V_Q15 = shortArrayOf(31130, 26214)
    /* 0.95, 0.8 */
    private val PLC_RAND_ATTENUATE_UV_Q15 = shortArrayOf(32440, 29491)

    /* 0.99, 0.9 */

    fun silk_PLC_Reset(
        psDec: SilkChannelDecoder /* I/O Decoder state        */
    ) {
        psDec.sPLC.pitchL_Q8 = Inlines.silk_LSHIFT(psDec.frame_length, 8 - 1)
        psDec.sPLC.prevGain_Q16[0] = (1 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1, 16)*/
        psDec.sPLC.prevGain_Q16[1] = (1 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1, 16)*/
        psDec.sPLC.subfr_length = 20
        psDec.sPLC.nb_subfr = 2
    }

    fun silk_PLC(
        psDec: SilkChannelDecoder, /* I/O Decoder state        */
        psDecCtrl: SilkDecoderControl, /* I/O Decoder control      */
        frame: ShortArray, /* I/O  signal              */
        frame_ptr: Int,
        lost: Int /* I Loss flag              */
    ) {
        /* PLC control function */
        if (psDec.fs_kHz != psDec.sPLC.fs_kHz) {
            silk_PLC_Reset(psDec)
            psDec.sPLC.fs_kHz = psDec.fs_kHz
        }

        if (lost != 0) {
            /**
             * *************************
             */
            /* Generate Signal          */
            /**
             * *************************
             */
            silk_PLC_conceal(psDec, psDecCtrl, frame, frame_ptr)

            psDec.lossCnt++
        } else {
            /**
             * *************************
             */
            /* Update state             */
            /**
             * *************************
             */
            silk_PLC_update(psDec, psDecCtrl)
        }
    }

    /**
     * ***********************************************
     */
    /* Update state of PLC                            */
    /**
     * ***********************************************
     */
    fun silk_PLC_update(
        psDec: SilkChannelDecoder, /* I/O Decoder state        */
        psDecCtrl: SilkDecoderControl /* I/O Decoder control      */
    ) {
        var LTP_Gain_Q14: Int
        var temp_LTP_Gain_Q14: Int
        var i: Int
        var j: Int
        val psPLC = psDec.sPLC // [porting note] pointer on the stack

        /* Update parameters used in case of packet loss */
        psDec.prevSignalType = psDec.indices.signalType.toInt()
        LTP_Gain_Q14 = 0
        if (psDec.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
            /* Find the parameters for the last subframe which contains a pitch pulse */
            j = 0
            while (j * psDec.subfr_length < psDecCtrl.pitchL[psDec.nb_subfr - 1]) {
                if (j == psDec.nb_subfr) {
                    break
                }
                temp_LTP_Gain_Q14 = 0
                i = 0
                while (i < SilkConstants.LTP_ORDER) {
                    temp_LTP_Gain_Q14 += psDecCtrl.LTPCoef_Q14[(psDec.nb_subfr - 1 - j) * SilkConstants.LTP_ORDER + i].toInt()
                    i++
                }
                if (temp_LTP_Gain_Q14 > LTP_Gain_Q14) {
                    LTP_Gain_Q14 = temp_LTP_Gain_Q14

                    arraycopy(
                        psDecCtrl.LTPCoef_Q14,
                        Inlines.silk_SMULBB(psDec.nb_subfr - 1 - j, SilkConstants.LTP_ORDER),
                        psPLC.LTPCoef_Q14,
                        0,
                        SilkConstants.LTP_ORDER
                    )

                    psPLC.pitchL_Q8 = Inlines.silk_LSHIFT(psDecCtrl.pitchL[psDec.nb_subfr - 1 - j], 8)
                }
                j++
            }

            Arrays.MemSet(psPLC.LTPCoef_Q14, 0.toShort(), SilkConstants.LTP_ORDER)
            psPLC.LTPCoef_Q14[SilkConstants.LTP_ORDER / 2] = LTP_Gain_Q14.toShort()

            /* Limit LT coefs */
            if (LTP_Gain_Q14 < SilkConstants.V_PITCH_GAIN_START_MIN_Q14) {
                val scale_Q10: Int
                val tmp: Int

                tmp = Inlines.silk_LSHIFT(SilkConstants.V_PITCH_GAIN_START_MIN_Q14, 10)
                scale_Q10 = Inlines.silk_DIV32(tmp, Inlines.silk_max(LTP_Gain_Q14, 1))
                i = 0
                while (i < SilkConstants.LTP_ORDER) {
                    psPLC.LTPCoef_Q14[i] =
                            Inlines.silk_RSHIFT(Inlines.silk_SMULBB(psPLC.LTPCoef_Q14[i].toInt(), scale_Q10), 10)
                                .toShort()
                    i++
                }
            } else if (LTP_Gain_Q14 > SilkConstants.V_PITCH_GAIN_START_MAX_Q14) {
                val scale_Q14: Int
                val tmp: Int

                tmp = Inlines.silk_LSHIFT(SilkConstants.V_PITCH_GAIN_START_MAX_Q14, 14)
                scale_Q14 = Inlines.silk_DIV32(tmp, Inlines.silk_max(LTP_Gain_Q14, 1))
                i = 0
                while (i < SilkConstants.LTP_ORDER) {
                    psPLC.LTPCoef_Q14[i] =
                            Inlines.silk_RSHIFT(Inlines.silk_SMULBB(psPLC.LTPCoef_Q14[i].toInt(), scale_Q14), 14)
                                .toShort()
                    i++
                }
            }
        } else {
            psPLC.pitchL_Q8 = Inlines.silk_LSHIFT(Inlines.silk_SMULBB(psDec.fs_kHz, 18), 8)
            Arrays.MemSet(psPLC.LTPCoef_Q14, 0.toShort(), SilkConstants.LTP_ORDER)
        }

        /* Save LPC coeficients */
        arraycopy(psDecCtrl.PredCoef_Q12[1], 0, psPLC.prevLPC_Q12, 0, psDec.LPC_order)
        psPLC.prevLTP_scale_Q14 = psDecCtrl.LTP_scale_Q14.toShort()

        /* Save last two gains */
        arraycopy(psDecCtrl.Gains_Q16, psDec.nb_subfr - 2, psPLC.prevGain_Q16, 0, 2)

        psPLC.subfr_length = psDec.subfr_length
        psPLC.nb_subfr = psDec.nb_subfr
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="energy1">O</param>
    /// <param name="shift1">O</param>
    /// <param name="energy2">O</param>
    /// <param name="shift2">O</param>
    /// <param name="exc_Q14">I</param>
    /// <param name="prevGain_Q10">I</param>
    /// <param name="subfr_length">I</param>
    /// <param name="nb_subfr">I</param>
    fun silk_PLC_energy(
        energy1: BoxedValueInt,
        shift1: BoxedValueInt,
        energy2: BoxedValueInt,
        shift2: BoxedValueInt,
        exc_Q14: IntArray,
        prevGain_Q10: IntArray,
        subfr_length: Int,
        nb_subfr: Int
    ) {
        var i: Int
        var k: Int
        var exc_buf_ptr = 0
        val exc_buf = ShortArray(2 * subfr_length)

        /* Find random noise component */
        /* Scale previous excitation signal */
        k = 0
        while (k < 2) {
            i = 0
            while (i < subfr_length) {
                exc_buf[exc_buf_ptr + i] = Inlines.silk_SAT16(
                    Inlines.silk_RSHIFT(
                        Inlines.silk_SMULWW(exc_Q14[i + (k + nb_subfr - 2) * subfr_length], prevGain_Q10[k]), 8
                    )
                ).toShort()
                i++
            }
            exc_buf_ptr += subfr_length
            k++
        }

        /* Find the subframe with lowest energy of the last two and use that as random noise generator */
        SumSqrShift.silk_sum_sqr_shift(energy1, shift1, exc_buf, subfr_length)
        SumSqrShift.silk_sum_sqr_shift(energy2, shift2, exc_buf, subfr_length, subfr_length)
    }

    fun silk_PLC_conceal(
        psDec: SilkChannelDecoder, /* I/O Decoder state        */
        psDecCtrl: SilkDecoderControl, /* I/O Decoder control      */
        frame: ShortArray, /* O LPC residual signal    */
        frame_ptr: Int
    ) {
        var i: Int
        var j: Int
        var k: Int
        var lag: Int
        var idx: Int
        var sLTP_buf_idx: Int
        var rand_seed: Int
        val harm_Gain_Q15: Int
        var rand_Gain_Q15: Int
        var inv_gain_Q30: Int
        val energy1 = BoxedValueInt(0)
        val energy2 = BoxedValueInt(0)
        val shift1 = BoxedValueInt(0)
        val shift2 = BoxedValueInt(0)
        val rand_ptr: Int
        var pred_lag_ptr: Int
        var LPC_pred_Q10: Int
        var LTP_pred_Q12: Int
        var rand_scale_Q14: Short
        val B_Q14: ShortArray
        val sLPC_Q14_ptr: Int
        val sLTP = ShortArray(psDec.ltp_mem_length)
        val sLTP_Q14 = IntArray(psDec.ltp_mem_length + psDec.frame_length)
        val psPLC = psDec.sPLC
        val prevGain_Q10 = IntArray(2)

        prevGain_Q10[0] = Inlines.silk_RSHIFT(psPLC.prevGain_Q16[0], 6)
        prevGain_Q10[1] = Inlines.silk_RSHIFT(psPLC.prevGain_Q16[1], 6)

        if (psDec.first_frame_after_reset != 0) {
            Arrays.MemSet(psPLC.prevLPC_Q12, 0.toShort(), SilkConstants.MAX_LPC_ORDER)
        }

        silk_PLC_energy(
            energy1,
            shift1,
            energy2,
            shift2,
            psDec.exc_Q14,
            prevGain_Q10,
            psDec.subfr_length,
            psDec.nb_subfr
        )

        if (Inlines.silk_RSHIFT(energy1.Val, shift2.Val) < Inlines.silk_RSHIFT(energy2.Val, shift1.Val)) {
            /* First sub-frame has lowest energy */
            rand_ptr = Inlines.silk_max_int(0, (psPLC.nb_subfr - 1) * psPLC.subfr_length - SilkConstants.RAND_BUF_SIZE)
        } else {
            /* Second sub-frame has lowest energy */
            rand_ptr = Inlines.silk_max_int(0, psPLC.nb_subfr * psPLC.subfr_length - SilkConstants.RAND_BUF_SIZE)
        }

        /* Set up Gain to random noise component */
        B_Q14 = psPLC.LTPCoef_Q14
        rand_scale_Q14 = psPLC.randScale_Q14

        /* Set up attenuation gains */
        harm_Gain_Q15 = HARM_ATT_Q15[Inlines.silk_min_int(NB_ATT - 1, psDec.lossCnt)].toInt()
        if (psDec.prevSignalType == SilkConstants.TYPE_VOICED) {
            rand_Gain_Q15 = PLC_RAND_ATTENUATE_V_Q15[Inlines.silk_min_int(NB_ATT - 1, psDec.lossCnt)].toInt()
        } else {
            rand_Gain_Q15 = PLC_RAND_ATTENUATE_UV_Q15[Inlines.silk_min_int(NB_ATT - 1, psDec.lossCnt)].toInt()
        }

        /* LPC concealment. Apply BWE to previous LPC */
        BWExpander.silk_bwexpander(
            psPLC.prevLPC_Q12,
            psDec.LPC_order,
            (SilkConstants.BWE_COEF * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.BWE_COEF, 16)*/
        )

        /* First Lost frame */
        if (psDec.lossCnt == 0) {
            rand_scale_Q14 = (1 shl 14).toShort()

            /* Reduce random noise Gain for voiced frames */
            if (psDec.prevSignalType == SilkConstants.TYPE_VOICED) {
                i = 0
                while (i < SilkConstants.LTP_ORDER) {
                    rand_scale_Q14 = (rand_scale_Q14 - B_Q14[i]).toShort()
                    i++
                }
                rand_scale_Q14 = Inlines.silk_max_16(3277.toShort(), rand_scale_Q14)
                /* 0.2 */
                rand_scale_Q14 = Inlines.silk_RSHIFT(
                    Inlines.silk_SMULBB(
                        rand_scale_Q14.toInt(),
                        psPLC.prevLTP_scale_Q14.toInt()
                    ), 14
                ).toShort()
            } else {
                /* Reduce random noise for unvoiced frames with high LPC gain */
                val invGain_Q30: Int
                var down_scale_Q30: Int

                invGain_Q30 = LPCInversePredGain.silk_LPC_inverse_pred_gain(psPLC.prevLPC_Q12, psDec.LPC_order)

                down_scale_Q30 = Inlines.silk_min_32(
                    Inlines.silk_RSHIFT(1 shl 30, SilkConstants.LOG2_INV_LPC_GAIN_HIGH_THRES),
                    invGain_Q30
                )
                down_scale_Q30 = Inlines.silk_max_32(
                    Inlines.silk_RSHIFT(1 shl 30, SilkConstants.LOG2_INV_LPC_GAIN_LOW_THRES),
                    down_scale_Q30
                )
                down_scale_Q30 = Inlines.silk_LSHIFT(down_scale_Q30, SilkConstants.LOG2_INV_LPC_GAIN_HIGH_THRES)

                rand_Gain_Q15 = Inlines.silk_RSHIFT(Inlines.silk_SMULWB(down_scale_Q30, rand_Gain_Q15), 14)
            }
        }

        rand_seed = psPLC.rand_seed
        lag = Inlines.silk_RSHIFT_ROUND(psPLC.pitchL_Q8, 8)
        sLTP_buf_idx = psDec.ltp_mem_length

        /* Rewhiten LTP state */
        idx = psDec.ltp_mem_length - lag - psDec.LPC_order - SilkConstants.LTP_ORDER / 2
        Inlines.OpusAssert(idx > 0)
        Filters.silk_LPC_analysis_filter(
            sLTP,
            idx,
            psDec.outBuf,
            idx,
            psPLC.prevLPC_Q12,
            0,
            psDec.ltp_mem_length - idx,
            psDec.LPC_order
        )
        /* Scale LTP state */
        inv_gain_Q30 = Inlines.silk_INVERSE32_varQ(psPLC.prevGain_Q16[1], 46)
        inv_gain_Q30 = Inlines.silk_min(inv_gain_Q30, Int.MAX_VALUE shr 1)
        i = idx + psDec.LPC_order
        while (i < psDec.ltp_mem_length) {
            sLTP_Q14[i] = Inlines.silk_SMULWB(inv_gain_Q30, sLTP[i].toInt())
            i++
        }

        /**
         * ************************
         */
        /* LTP synthesis filtering */
        /**
         * ************************
         */
        k = 0
        while (k < psDec.nb_subfr) {
            /* Set up pointer */
            pred_lag_ptr = sLTP_buf_idx - lag + SilkConstants.LTP_ORDER / 2
            i = 0
            while (i < psDec.subfr_length) {
                /* Unrolled loop */
                /* Avoids introducing a bias because Inlines.silk_SMLAWB() always rounds to -inf */
                LTP_pred_Q12 = 2
                LTP_pred_Q12 = Inlines.silk_SMLAWB(LTP_pred_Q12, sLTP_Q14[pred_lag_ptr], B_Q14[0].toInt())
                LTP_pred_Q12 = Inlines.silk_SMLAWB(LTP_pred_Q12, sLTP_Q14[pred_lag_ptr - 1], B_Q14[1].toInt())
                LTP_pred_Q12 = Inlines.silk_SMLAWB(LTP_pred_Q12, sLTP_Q14[pred_lag_ptr - 2], B_Q14[2].toInt())
                LTP_pred_Q12 = Inlines.silk_SMLAWB(LTP_pred_Q12, sLTP_Q14[pred_lag_ptr - 3], B_Q14[3].toInt())
                LTP_pred_Q12 = Inlines.silk_SMLAWB(LTP_pred_Q12, sLTP_Q14[pred_lag_ptr - 4], B_Q14[4].toInt())
                pred_lag_ptr++

                /* Generate LPC excitation */
                rand_seed = Inlines.silk_RAND(rand_seed)
                idx = Inlines.silk_RSHIFT(rand_seed, 25) and SilkConstants.RAND_BUF_MASK
                sLTP_Q14[sLTP_buf_idx] = Inlines.silk_LSHIFT32(
                    Inlines.silk_SMLAWB(
                        LTP_pred_Q12,
                        psDec.exc_Q14[rand_ptr + idx],
                        rand_scale_Q14.toInt()
                    ), 2
                )
                sLTP_buf_idx++
                i++
            }

            /* Gradually reduce LTP gain */
            j = 0
            while (j < SilkConstants.LTP_ORDER) {
                B_Q14[j] = Inlines.silk_RSHIFT(Inlines.silk_SMULBB(harm_Gain_Q15, B_Q14[j].toInt()), 15).toShort()
                j++
            }
            /* Gradually reduce excitation gain */
            rand_scale_Q14 =
                    Inlines.silk_RSHIFT(Inlines.silk_SMULBB(rand_scale_Q14.toInt(), rand_Gain_Q15), 15).toShort()

            /* Slowly increase pitch lag */
            psPLC.pitchL_Q8 = Inlines.silk_SMLAWB(psPLC.pitchL_Q8, psPLC.pitchL_Q8, SilkConstants.PITCH_DRIFT_FAC_Q16)
            psPLC.pitchL_Q8 = Inlines.silk_min_32(
                psPLC.pitchL_Q8,
                Inlines.silk_LSHIFT(Inlines.silk_SMULBB(SilkConstants.MAX_PITCH_LAG_MS, psDec.fs_kHz), 8)
            )
            lag = Inlines.silk_RSHIFT_ROUND(psPLC.pitchL_Q8, 8)
            k++
        }

        /**
         * ************************
         */
        /* LPC synthesis filtering */
        /**
         * ************************
         */
        sLPC_Q14_ptr = psDec.ltp_mem_length - SilkConstants.MAX_LPC_ORDER

        /* Copy LPC state */
        arraycopy(psDec.sLPC_Q14_buf, 0, sLTP_Q14, sLPC_Q14_ptr, SilkConstants.MAX_LPC_ORDER)

        Inlines.OpusAssert(psDec.LPC_order >= 10)
        /* check that unrolling works */
        i = 0
        while (i < psDec.frame_length) {
            /* partly unrolled */
            val sLPCmaxi = sLPC_Q14_ptr + SilkConstants.MAX_LPC_ORDER + i
            /* Avoids introducing a bias because Inlines.silk_SMLAWB() always rounds to -inf */
            LPC_pred_Q10 = Inlines.silk_RSHIFT(psDec.LPC_order, 1)
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 1], psPLC.prevLPC_Q12[0].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 2], psPLC.prevLPC_Q12[1].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 3], psPLC.prevLPC_Q12[2].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 4], psPLC.prevLPC_Q12[3].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 5], psPLC.prevLPC_Q12[4].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 6], psPLC.prevLPC_Q12[5].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 7], psPLC.prevLPC_Q12[6].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 8], psPLC.prevLPC_Q12[7].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 9], psPLC.prevLPC_Q12[8].toInt())
            LPC_pred_Q10 = Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - 10], psPLC.prevLPC_Q12[9].toInt())
            j = 10
            while (j < psDec.LPC_order) {
                LPC_pred_Q10 =
                        Inlines.silk_SMLAWB(LPC_pred_Q10, sLTP_Q14[sLPCmaxi - j - 1], psPLC.prevLPC_Q12[j].toInt())
                j++
            }

            /* Add prediction to LPC excitation */
            sLTP_Q14[sLPCmaxi] = Inlines.silk_ADD_LSHIFT32(sLTP_Q14[sLPCmaxi], LPC_pred_Q10, 4)

            /* Scale with Gain */
            frame[frame_ptr + i] = Inlines.silk_SAT16(
                Inlines.silk_SAT16(
                    Inlines.silk_RSHIFT_ROUND(
                        Inlines.silk_SMULWW(
                            sLTP_Q14[sLPCmaxi],
                            prevGain_Q10[1]
                        ), 8
                    )
                )
            ).toShort()
            i++
        }

        /* Save LPC state */
        arraycopy(
            sLTP_Q14,
            sLPC_Q14_ptr + psDec.frame_length,
            psDec.sLPC_Q14_buf,
            0,
            SilkConstants.MAX_LPC_ORDER
        )

        /**
         * ***********************************
         */
        /* Update states                      */
        /**
         * ***********************************
         */
        psPLC.rand_seed = rand_seed
        psPLC.randScale_Q14 = rand_scale_Q14
        i = 0
        while (i < SilkConstants.MAX_NB_SUBFR) {
            psDecCtrl.pitchL[i] = lag
            i++
        }
    }

    /* Glues concealed frames with new good received frames */
    fun silk_PLC_glue_frames(
        psDec: SilkChannelDecoder, /* I/O decoder state        */
        frame: ShortArray, /* I/O signal               */
        frame_ptr: Int,
        length: Int /* I length of signal       */
    ) {
        var i: Int
        val energy_shift = BoxedValueInt(0)
        val energy = BoxedValueInt(0)
        val psPLC = psDec.sPLC

        if (psDec.lossCnt != 0) {
            /* Calculate energy in concealed residual */
            val boxed_conc_e = BoxedValueInt(0)
            val boxed_conc_shift = BoxedValueInt(0)
            SumSqrShift.silk_sum_sqr_shift(boxed_conc_e, boxed_conc_shift, frame, frame_ptr, length)
            psPLC.conc_energy = boxed_conc_e.Val
            psPLC.conc_energy_shift = boxed_conc_shift.Val

            psPLC.last_frame_lost = 1
        } else {
            if (psDec.sPLC.last_frame_lost != 0) {
                /* Calculate residual in decoded signal if last frame was lost */
                SumSqrShift.silk_sum_sqr_shift(energy, energy_shift, frame, frame_ptr, length)

                /* Normalize energies */
                if (energy_shift.Val > psPLC.conc_energy_shift) {
                    psPLC.conc_energy =
                            Inlines.silk_RSHIFT(psPLC.conc_energy, energy_shift.Val - psPLC.conc_energy_shift)
                } else if (energy_shift.Val < psPLC.conc_energy_shift) {
                    energy.Val = Inlines.silk_RSHIFT(energy.Val, psPLC.conc_energy_shift - energy_shift.Val)
                }

                /* Fade in the energy difference */
                if (energy.Val > psPLC.conc_energy) {
                    val frac_Q24: Int
                    var LZ: Int
                    var gain_Q16: Int
                    var slope_Q16: Int

                    LZ = Inlines.silk_CLZ32(psPLC.conc_energy)
                    LZ = LZ - 1
                    psPLC.conc_energy = Inlines.silk_LSHIFT(psPLC.conc_energy, LZ)
                    energy.Val = Inlines.silk_RSHIFT(energy.Val, Inlines.silk_max_32(24 - LZ, 0))

                    frac_Q24 = Inlines.silk_DIV32(psPLC.conc_energy, Inlines.silk_max(energy.Val, 1))

                    gain_Q16 = Inlines.silk_LSHIFT(Inlines.silk_SQRT_APPROX(frac_Q24), 4)
                    slope_Q16 = Inlines.silk_DIV32_16((1 shl 16) - gain_Q16, length)
                    /* Make slope 4x steeper to avoid missing onsets after DTX */
                    slope_Q16 = Inlines.silk_LSHIFT(slope_Q16, 2)

                    i = frame_ptr
                    while (i < frame_ptr + length) {
                        frame[i] = Inlines.silk_SMULWB(gain_Q16, frame[i].toInt()).toShort()
                        gain_Q16 += slope_Q16
                        if (gain_Q16 > 1 shl 16) {
                            break
                        }
                        i++
                    }
                }
            }
            psPLC.last_frame_lost = 0
        }
    }
}
