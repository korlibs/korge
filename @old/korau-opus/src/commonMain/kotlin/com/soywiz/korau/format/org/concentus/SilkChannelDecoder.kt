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
/// Decoder state
/// </summary>
internal class SilkChannelDecoder {

    var prev_gain_Q16 = 0
    val exc_Q14 = IntArray(SilkConstants.MAX_FRAME_LENGTH)
    val sLPC_Q14_buf = IntArray(SilkConstants.MAX_LPC_ORDER)
    val outBuf = ShortArray(SilkConstants.MAX_FRAME_LENGTH + 2 * SilkConstants.MAX_SUB_FRAME_LENGTH)
    /* Buffer for output signal                     */
    var lagPrev = 0
    /* Previous Lag                                                     */
    var LastGainIndex: Byte = 0
    /* Previous gain index                                              */
    var fs_kHz = 0
    /* Sampling frequency in kHz                                        */
    var fs_API_hz = 0
    /* API sample frequency (Hz)                                        */
    var nb_subfr = 0
    /* Number of 5 ms subframes in a frame                              */
    var frame_length = 0
    /* Frame length (samples)                                           */
    var subfr_length = 0
    /* Subframe length (samples)                                        */
    var ltp_mem_length = 0
    /* Length of LTP memory                                             */
    var LPC_order = 0
    /* LPC order                                                        */
    val prevNLSF_Q15 = ShortArray(SilkConstants.MAX_LPC_ORDER)
    /* Used to interpolate LSFs                                         */
    var first_frame_after_reset = 0
    /* Flag for deactivating NLSF interpolation                         */
    var pitch_lag_low_bits_iCDF: ShortArray? = null
    /* Pointer to iCDF table for low bits of pitch lag index            */
    var pitch_contour_iCDF: ShortArray? = null
    /* Pointer to iCDF table for pitch contour index                    */

    /* For buffering payload in case of more frames per packet */
    var nFramesDecoded = 0
    var nFramesPerPacket = 0

    /* Specifically for entropy coding */
    var ec_prevSignalType = 0
    var ec_prevLagIndex: Short = 0

    val VAD_flags = IntArray(SilkConstants.MAX_FRAMES_PER_PACKET)
    var LBRR_flag = 0
    val LBRR_flags = IntArray(SilkConstants.MAX_FRAMES_PER_PACKET)

    val resampler_state = SilkResamplerState()

    var psNLSF_CB: NLSFCodebook? = null
    /* Pointer to NLSF codebook                                         */

    /* Quantization indices */
    val indices = SideInfoIndices()

    /* CNG state */
    val sCNG = CNGState()

    /* Stuff used for PLC */
    var lossCnt = 0
    var prevSignalType = 0

    val sPLC = PLCStruct()

    fun Reset() {
        prev_gain_Q16 = 0
        Arrays.MemSet(exc_Q14, 0, SilkConstants.MAX_FRAME_LENGTH)
        Arrays.MemSet(sLPC_Q14_buf, 0, SilkConstants.MAX_LPC_ORDER)
        Arrays.MemSet(outBuf, 0.toShort(), SilkConstants.MAX_FRAME_LENGTH + 2 * SilkConstants.MAX_SUB_FRAME_LENGTH)
        lagPrev = 0
        LastGainIndex = 0
        fs_kHz = 0
        fs_API_hz = 0
        nb_subfr = 0
        frame_length = 0
        subfr_length = 0
        ltp_mem_length = 0
        LPC_order = 0
        Arrays.MemSet(prevNLSF_Q15, 0.toShort(), SilkConstants.MAX_LPC_ORDER)
        first_frame_after_reset = 0
        pitch_lag_low_bits_iCDF = null
        pitch_contour_iCDF = null
        nFramesDecoded = 0
        nFramesPerPacket = 0
        ec_prevSignalType = 0
        ec_prevLagIndex = 0
        Arrays.MemSet(VAD_flags, 0, SilkConstants.MAX_FRAMES_PER_PACKET)
        LBRR_flag = 0
        Arrays.MemSet(LBRR_flags, 0, SilkConstants.MAX_FRAMES_PER_PACKET)
        resampler_state.Reset()
        psNLSF_CB = null
        indices.Reset()
        sCNG.Reset()
        lossCnt = 0
        prevSignalType = 0
        sPLC.Reset()
    }

    /// <summary>
    /// Init Decoder State
    /// </summary>
    /// <param name="this">I/O  Decoder state pointer</param>
    /// <returns></returns>
    fun silk_init_decoder(): Int {
        /* Clear the entire encoder state, except anything copied */
        this.Reset()

        /* Used to deactivate LSF interpolation */
        this.first_frame_after_reset = 1
        this.prev_gain_Q16 = 65536

        /* Reset CNG state */
        silk_CNG_Reset()

        /* Reset PLC state */
        silk_PLC_Reset()

        return 0
    }

    /// <summary>
    /// Resets CNG state
    /// </summary>
    /// <param name="this">I/O  Decoder state</param>
    private fun silk_CNG_Reset() {
        var i: Int
        val NLSF_step_Q15: Int
        var NLSF_acc_Q15: Int

        NLSF_step_Q15 = Inlines.silk_DIV32_16(Short.MAX_VALUE.toInt(), this.LPC_order + 1)
        NLSF_acc_Q15 = 0
        i = 0
        while (i < this.LPC_order) {
            NLSF_acc_Q15 += NLSF_step_Q15
            this.sCNG.CNG_smth_NLSF_Q15[i] = NLSF_acc_Q15.toShort()
            i++
        }
        this.sCNG.CNG_smth_Gain_Q16 = 0
        this.sCNG.rand_seed = 3176576
    }

    /// <summary>
    /// Resets PLC state
    /// </summary>
    /// <param name="this">I/O Decoder state</param>
    private fun silk_PLC_Reset() {
        this.sPLC.pitchL_Q8 = Inlines.silk_LSHIFT(this.frame_length, 8 - 1)
        this.sPLC.prevGain_Q16[0] = (1 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1, 16)*/
        this.sPLC.prevGain_Q16[1] = (1 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1, 16)*/
        this.sPLC.subfr_length = 20
        this.sPLC.nb_subfr = 2
    }

    /* Set decoder sampling rate */
    fun silk_decoder_set_fs(
        fs_kHz: Int, /* I    Sampling frequency (kHz)                    */
        fs_API_Hz: Int /* I    API Sampling frequency (Hz)                 */
    ): Int {
        val frame_length: Int
        var ret = 0

        Inlines.OpusAssert(fs_kHz == 8 || fs_kHz == 12 || fs_kHz == 16)
        Inlines.OpusAssert(this.nb_subfr == SilkConstants.MAX_NB_SUBFR || this.nb_subfr == SilkConstants.MAX_NB_SUBFR / 2)

        /* New (sub)frame length */
        this.subfr_length = Inlines.silk_SMULBB(SilkConstants.SUB_FRAME_LENGTH_MS, fs_kHz)
        frame_length = Inlines.silk_SMULBB(this.nb_subfr, this.subfr_length)

        /* Initialize resampler when switching or external sampling frequency */
        if (this.fs_kHz != fs_kHz || this.fs_API_hz != fs_API_Hz) {
            /* Initialize the resampler for dec_API.c preparing resampling from fs_kHz to API_fs_Hz */
            ret += Resampler.silk_resampler_init(this.resampler_state, Inlines.silk_SMULBB(fs_kHz, 1000), fs_API_Hz, 0)

            this.fs_API_hz = fs_API_Hz
        }

        if (this.fs_kHz != fs_kHz || frame_length != this.frame_length) {
            if (fs_kHz == 8) {
                if (this.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
                    this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_NB_iCDF
                } else {
                    this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_NB_iCDF
                }
            } else if (this.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
                this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_iCDF
            } else {
                this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_iCDF
            }
            if (this.fs_kHz != fs_kHz) {
                this.ltp_mem_length = Inlines.silk_SMULBB(SilkConstants.LTP_MEM_LENGTH_MS, fs_kHz)
                if (fs_kHz == 8 || fs_kHz == 12) {
                    this.LPC_order = SilkConstants.MIN_LPC_ORDER
                    this.psNLSF_CB = SilkTables.silk_NLSF_CB_NB_MB
                } else {
                    this.LPC_order = SilkConstants.MAX_LPC_ORDER
                    this.psNLSF_CB = SilkTables.silk_NLSF_CB_WB
                }
                if (fs_kHz == 16) {
                    this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform8_iCDF
                } else if (fs_kHz == 12) {
                    this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform6_iCDF
                } else if (fs_kHz == 8) {
                    this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform4_iCDF
                } else {
                    /* unsupported sampling rate */
                    Inlines.OpusAssert(false)
                }
                this.first_frame_after_reset = 1
                this.lagPrev = 100
                this.LastGainIndex = 10
                this.prevSignalType = SilkConstants.TYPE_NO_VOICE_ACTIVITY
                Arrays.MemSet(
                    this.outBuf,
                    0.toShort(),
                    SilkConstants.MAX_FRAME_LENGTH + 2 * SilkConstants.MAX_SUB_FRAME_LENGTH
                )
                Arrays.MemSet(this.sLPC_Q14_buf, 0, SilkConstants.MAX_LPC_ORDER)
            }

            this.fs_kHz = fs_kHz
            this.frame_length = frame_length
        }

        /* Check that settings are valid */
        Inlines.OpusAssert(this.frame_length > 0 && this.frame_length <= SilkConstants.MAX_FRAME_LENGTH)

        return ret
    }

    /**
     * *************
     */
    /* Decode frame */
    /**
     * *************
     */
    fun silk_decode_frame(
        psRangeDec: EntropyCoder, /* I/O  Compressor data structure                   */
        pOut: ShortArray, /* O    Pointer to output speech frame              */
        pOut_ptr: Int,
        pN: BoxedValueInt, /* O    Pointer to size of output frame             */
        lostFlag: Int, /* I    0: no loss, 1 loss, 2 decode fec            */
        condCoding: Int /* I    The type of conditional coding to use       */
    ): Int {
        val thisCtrl = SilkDecoderControl()
        val L: Int
        val mv_len: Int
        val ret = 0

        L = this.frame_length
        thisCtrl.LTP_scale_Q14 = 0

        /* Safety checks */
        Inlines.OpusAssert(L > 0 && L <= SilkConstants.MAX_FRAME_LENGTH)

        if (lostFlag == DecoderAPIFlag.FLAG_DECODE_NORMAL || lostFlag == DecoderAPIFlag.FLAG_DECODE_LBRR && this.LBRR_flags[this.nFramesDecoded] == 1) {
            val pulses =
                ShortArray(L + SilkConstants.SHELL_CODEC_FRAME_LENGTH - 1 and (SilkConstants.SHELL_CODEC_FRAME_LENGTH - 1).inv())
            /**
             * ******************************************
             */
            /* Decode quantization indices of side info  */
            /**
             * ******************************************
             */
            DecodeIndices.silk_decode_indices(this, psRangeDec, this.nFramesDecoded, lostFlag, condCoding)

            /**
             * ******************************************
             */
            /* Decode quantization indices of excitation */
            /**
             * ******************************************
             */
            DecodePulses.silk_decode_pulses(
                psRangeDec, pulses, this.indices.signalType.toInt(),
                this.indices.quantOffsetType.toInt(), this.frame_length
            )

            /**
             * *****************************************
             */
            /* Decode parameters and pulse signal       */
            /**
             * *****************************************
             */
            DecodeParameters.silk_decode_parameters(this, thisCtrl, condCoding)

            /**
             * *****************************************************
             */
            /* Run inverse NSQ                                      */
            /**
             * *****************************************************
             */
            DecodeCore.silk_decode_core(this, thisCtrl, pOut, pOut_ptr, pulses)

            /**
             * *****************************************************
             */
            /* Update PLC state                                     */
            /**
             * *****************************************************
             */
            PLC.silk_PLC(this, thisCtrl, pOut, pOut_ptr, 0)

            this.lossCnt = 0
            this.prevSignalType = this.indices.signalType.toInt()
            Inlines.OpusAssert(this.prevSignalType >= 0 && this.prevSignalType <= 2)

            /* A frame has been decoded without errors */
            this.first_frame_after_reset = 0
        } else {
            /* Handle packet loss by extrapolation */
            PLC.silk_PLC(this, thisCtrl, pOut, pOut_ptr, 1)
        }

        /**
         * **********************
         */
        /* Update output buffer. */
        /**
         * **********************
         */
        Inlines.OpusAssert(this.ltp_mem_length >= this.frame_length)
        mv_len = this.ltp_mem_length - this.frame_length
        Arrays.MemMove(this.outBuf, this.frame_length, 0, mv_len)
        arraycopy(pOut, pOut_ptr, this.outBuf, mv_len, this.frame_length)

        /**
         * *********************************************
         */
        /* Comfort noise generation / estimation        */
        /**
         * *********************************************
         */
        CNG.silk_CNG(this, thisCtrl, pOut, pOut_ptr, L)

        /**
         * *************************************************************
         */
        /* Ensure smooth connection of extrapolated and good frames     */
        /**
         * *************************************************************
         */
        PLC.silk_PLC_glue_frames(this, pOut, pOut_ptr, L)

        /* Update some decoder state variables */
        this.lagPrev = thisCtrl.pitchL[this.nb_subfr - 1]

        /* Set output frame length */
        pN.Val = L

        return ret
    }
}
