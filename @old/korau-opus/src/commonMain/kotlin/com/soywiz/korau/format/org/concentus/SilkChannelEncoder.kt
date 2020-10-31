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
/// Encoder state
/// </summary>
internal class SilkChannelEncoder {

    val In_HP_State = IntArray(2)
    /* High pass filter state                                           */
    var variable_HP_smth1_Q15 = 0
    /* State of first smoother                                          */
    var variable_HP_smth2_Q15 = 0
    /* State of second smoother                                         */
    val sLP = SilkLPState()
    /* Low pass filter state                                            */
    val sVAD = SilkVADState()
    /* Voice activity detector state                                    */
    val sNSQ = SilkNSQState()
    /* Noise Shape Quantizer State                                      */
    val prev_NLSFq_Q15 = ShortArray(SilkConstants.MAX_LPC_ORDER)
    /* Previously quantized NLSF vector                                 */
    var speech_activity_Q8 = 0
    /* Speech activity                                                  */
    var allow_bandwidth_switch = 0
    /* Flag indicating that switching of bandwidth is allowed  */
    var LBRRprevLastGainIndex: Byte = 0
    var prevSignalType: Byte = 0
    var prevLag = 0
    var pitch_LPC_win_length = 0
    var max_pitch_lag = 0
    /* Highest possible pitch lag (samples)                             */
    var API_fs_Hz = 0
    /* API sampling frequency (Hz)                                      */
    var prev_API_fs_Hz = 0
    /* Previous API sampling frequency (Hz)                             */
    var maxInternal_fs_Hz = 0
    /* Maximum sampling frequency (Hz)                         */
    var minInternal_fs_Hz = 0
    /* Minimum sampling frequency (Hz)                         */
    var desiredInternal_fs_Hz = 0
    /* Soft request for sampling frequency (Hz)                */
    var fs_kHz = 0
    /* Internal sampling frequency (kHz)                                */
    var nb_subfr = 0
    /* Number of 5 ms subframes in a frame                              */
    var frame_length = 0
    /* Frame length (samples)                                           */
    var subfr_length = 0
    /* Subframe length (samples)                                        */
    var ltp_mem_length = 0
    /* Length of LTP memory                                             */
    var la_pitch = 0
    /* Look-ahead for pitch analysis (samples)                          */
    var la_shape = 0
    /* Look-ahead for noise shape analysis (samples)                    */
    var shapeWinLength = 0
    /* Window length for noise shape analysis (samples)                 */
    var TargetRate_bps = 0
    /* Target bitrate (bps)                                             */
    var PacketSize_ms = 0
    /* Number of milliseconds to put in each packet                     */
    var PacketLoss_perc = 0
    /* Packet loss rate measured by farend                              */
    var frameCounter = 0
    var Complexity = 0
    /* Complexity setting                                               */
    var nStatesDelayedDecision = 0
    /* Number of states in delayed decision quantization                */
    var useInterpolatedNLSFs = 0
    /* Flag for using NLSF interpolation                                */
    var shapingLPCOrder = 0
    /* Filter order for noise shaping filters                           */
    var predictLPCOrder = 0
    /* Filter order for prediction filters                              */
    var pitchEstimationComplexity = 0
    /* Complexity level for pitch estimator                             */
    var pitchEstimationLPCOrder = 0
    /* Whitening filter order for pitch estimator                       */
    var pitchEstimationThreshold_Q16 = 0
    /* Threshold for pitch estimator                                    */
    var LTPQuantLowComplexity = 0
    /* Flag for low complexity LTP quantization                         */
    var mu_LTP_Q9 = 0
    /* Rate-distortion tradeoff in LTP quantization                     */
    var sum_log_gain_Q7 = 0
    /* Cumulative max prediction gain                                   */
    var NLSF_MSVQ_Survivors = 0
    /* Number of survivors in NLSF MSVQ                                 */
    var first_frame_after_reset = 0
    /* Flag for deactivating NLSF interpolation, pitch prediction       */
    var controlled_since_last_payload = 0
    /* Flag for ensuring codec_control only runs once per packet        */
    var warping_Q16 = 0
    /* Warping parameter for warped noise shaping                       */
    var useCBR = 0
    /* Flag to enable constant bitrate                                  */
    var prefillFlag = 0
    /* Flag to indicate that only buffers are prefilled, no coding      */
    var pitch_lag_low_bits_iCDF: ShortArray? = null
    /* Pointer to iCDF table for low bits of pitch lag index            */
    var pitch_contour_iCDF: ShortArray? = null
    /* Pointer to iCDF table for pitch contour index                    */
    var psNLSF_CB: NLSFCodebook? = null
    /* Pointer to NLSF codebook                                         */
    val input_quality_bands_Q15 = IntArray(SilkConstants.VAD_N_BANDS)
    var input_tilt_Q15 = 0
    var SNR_dB_Q7 = 0
    /* Quality setting                                                  */

    val VAD_flags = ByteArray(SilkConstants.MAX_FRAMES_PER_PACKET)
    var LBRR_flag: Byte = 0
    val LBRR_flags = IntArray(SilkConstants.MAX_FRAMES_PER_PACKET)

    val indices = SideInfoIndices()
    val pulses = ByteArray(SilkConstants.MAX_FRAME_LENGTH)

    /* Input/output buffering */
    val inputBuf = ShortArray(SilkConstants.MAX_FRAME_LENGTH + 2)
    /* Buffer containing input signal                                   */
    var inputBufIx = 0
    var nFramesPerPacket = 0
    var nFramesEncoded = 0
    /* Number of frames analyzed in current packet                      */

    var nChannelsAPI = 0
    var nChannelsInternal = 0
    var channelNb = 0

    /* Parameters For LTP scaling Control */
    var frames_since_onset = 0

    /* Specifically for entropy coding */
    var ec_prevSignalType = 0
    var ec_prevLagIndex: Short = 0

    val resampler_state = SilkResamplerState()

    /* DTX */
    var useDTX = 0
    /* Flag to enable DTX                                               */
    var inDTX = 0
    /* Flag to signal DTX period                                        */
    var noSpeechCounter = 0
    /* Counts concecutive nonactive frames, used by DTX                 */

    /* Inband Low Bitrate Redundancy (LBRR) data */
    var useInBandFEC = 0
    /* Saves the API setting for query                                  */
    var LBRR_enabled = 0
    /* Depends on useInBandFRC, bitrate and packet loss rate            */
    var LBRR_GainIncreases = 0
    /* Gains increment for coding LBRR frames                           */
    val indices_LBRR = Array<SideInfoIndices>(SilkConstants.MAX_FRAMES_PER_PACKET) { SideInfoIndices() }
    val pulses_LBRR =
        Arrays.InitTwoDimensionalArrayByte(SilkConstants.MAX_FRAMES_PER_PACKET, SilkConstants.MAX_FRAME_LENGTH)

    /* Noise shaping state */
    val sShape = SilkShapeState()

    /* Prefilter State */
    val sPrefilt = SilkPrefilterState()

    /* Buffer for find pitch and noise shape analysis */
    val x_buf = ShortArray(2 * SilkConstants.MAX_FRAME_LENGTH + SilkConstants.LA_SHAPE_MAX)

    /* Normalized correlation from pitch lag estimator */
    var LTPCorr_Q15 = 0

    fun Reset() {
        Arrays.MemSet(In_HP_State, 0, 2)
        variable_HP_smth1_Q15 = 0
        variable_HP_smth2_Q15 = 0
        sLP.Reset()
        sVAD.Reset()
        sNSQ.Reset()
        Arrays.MemSet(prev_NLSFq_Q15, 0.toShort(), SilkConstants.MAX_LPC_ORDER)
        speech_activity_Q8 = 0
        allow_bandwidth_switch = 0
        LBRRprevLastGainIndex = 0
        prevSignalType = 0
        prevLag = 0
        pitch_LPC_win_length = 0
        max_pitch_lag = 0
        API_fs_Hz = 0
        prev_API_fs_Hz = 0
        maxInternal_fs_Hz = 0
        minInternal_fs_Hz = 0
        desiredInternal_fs_Hz = 0
        fs_kHz = 0
        nb_subfr = 0
        frame_length = 0
        subfr_length = 0
        ltp_mem_length = 0
        la_pitch = 0
        la_shape = 0
        shapeWinLength = 0
        TargetRate_bps = 0
        PacketSize_ms = 0
        PacketLoss_perc = 0
        frameCounter = 0
        Complexity = 0
        nStatesDelayedDecision = 0
        useInterpolatedNLSFs = 0
        shapingLPCOrder = 0
        predictLPCOrder = 0
        pitchEstimationComplexity = 0
        pitchEstimationLPCOrder = 0
        pitchEstimationThreshold_Q16 = 0
        LTPQuantLowComplexity = 0
        mu_LTP_Q9 = 0
        sum_log_gain_Q7 = 0
        NLSF_MSVQ_Survivors = 0
        first_frame_after_reset = 0
        controlled_since_last_payload = 0
        warping_Q16 = 0
        useCBR = 0
        prefillFlag = 0
        pitch_lag_low_bits_iCDF = null
        pitch_contour_iCDF = null
        psNLSF_CB = null
        Arrays.MemSet(input_quality_bands_Q15, 0, SilkConstants.VAD_N_BANDS)
        input_tilt_Q15 = 0
        SNR_dB_Q7 = 0
        Arrays.MemSet(VAD_flags, 0.toByte(), SilkConstants.MAX_FRAMES_PER_PACKET)
        LBRR_flag = 0
        Arrays.MemSet(LBRR_flags, 0, SilkConstants.MAX_FRAMES_PER_PACKET)
        indices.Reset()
        Arrays.MemSet(pulses, 0.toByte(), SilkConstants.MAX_FRAME_LENGTH)
        Arrays.MemSet(inputBuf, 0.toShort(), SilkConstants.MAX_FRAME_LENGTH + 2)
        inputBufIx = 0
        nFramesPerPacket = 0
        nFramesEncoded = 0
        nChannelsAPI = 0
        nChannelsInternal = 0
        channelNb = 0
        frames_since_onset = 0
        ec_prevSignalType = 0
        ec_prevLagIndex = 0
        resampler_state.Reset()
        useDTX = 0
        inDTX = 0
        noSpeechCounter = 0
        useInBandFEC = 0
        LBRR_enabled = 0
        LBRR_GainIncreases = 0
        for (c in 0 until SilkConstants.MAX_FRAMES_PER_PACKET) {
            indices_LBRR[c]!!.Reset()
            Arrays.MemSet(pulses_LBRR[c], 0.toByte(), SilkConstants.MAX_FRAME_LENGTH)
        }
        sShape.Reset()
        sPrefilt.Reset()
        Arrays.MemSet(x_buf, 0.toShort(), 2 * SilkConstants.MAX_FRAME_LENGTH + SilkConstants.LA_SHAPE_MAX)
        LTPCorr_Q15 = 0
    }

    /// <summary>
    /// Control encoder
    /// </summary>
    /// <param name="this">I/O  Pointer to Silk encoder state</param>
    /// <param name="encControl">I    Control structure</param>
    /// <param name="TargetRate_bps">I    Target max bitrate (bps)</param>
    /// <param name="allow_bw_switch">I    Flag to allow switching audio bandwidth</param>
    /// <param name="channelNb">I    Channel number</param>
    /// <param name="force_fs_kHz"></param>
    /// <returns></returns>
    fun silk_control_encoder(
        encControl: EncControlState,
        TargetRate_bps: Int,
        allow_bw_switch: Int,
        channelNb: Int,
        force_fs_kHz: Int
    ): Int {
        var fs_kHz: Int
        var ret = SilkError.SILK_NO_ERROR

        this.useDTX = encControl.useDTX
        this.useCBR = encControl.useCBR
        this.API_fs_Hz = encControl.API_sampleRate
        this.maxInternal_fs_Hz = encControl.maxInternalSampleRate
        this.minInternal_fs_Hz = encControl.minInternalSampleRate
        this.desiredInternal_fs_Hz = encControl.desiredInternalSampleRate
        this.useInBandFEC = encControl.useInBandFEC
        this.nChannelsAPI = encControl.nChannelsAPI
        this.nChannelsInternal = encControl.nChannelsInternal
        this.allow_bandwidth_switch = allow_bw_switch
        this.channelNb = channelNb

        if (this.controlled_since_last_payload != 0 && this.prefillFlag == 0) {
            if (this.API_fs_Hz != this.prev_API_fs_Hz && this.fs_kHz > 0) {
                /* Change in API sampling rate in the middle of encoding a packet */
                ret = silk_setup_resamplers(this.fs_kHz)
            }
            return ret
        }

        /* Beyond this point we know that there are no previously coded frames in the payload buffer */
        /**
         * *****************************************
         */
        /* Determine sampling rate         */
        /**
         * *****************************************
         */
        fs_kHz = silk_control_audio_bandwidth(encControl)
        if (force_fs_kHz != 0) {
            fs_kHz = force_fs_kHz
        }
        /**
         * *****************************************
         */
        /* Prepare resampler and buffered data      */
        /**
         * *****************************************
         */
        ret = silk_setup_resamplers(fs_kHz)

        /**
         * *****************************************
         */
        /* Set sampling frequency          */
        /**
         * *****************************************
         */
        ret = silk_setup_fs(fs_kHz, encControl.payloadSize_ms)

        /**
         * *****************************************
         */
        /* Set encoding complexity                  */
        /**
         * *****************************************
         */
        ret = silk_setup_complexity(encControl.complexity)

        /**
         * *****************************************
         */
        /* Set packet loss rate measured by farend  */
        /**
         * *****************************************
         */
        this.PacketLoss_perc = encControl.packetLossPercentage

        /**
         * *****************************************
         */
        /* Set LBRR usage                           */
        /**
         * *****************************************
         */
        ret = silk_setup_LBRR(TargetRate_bps)

        this.controlled_since_last_payload = 1

        return ret
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="this">I/O</param>
    /// <param name="fs_kHz">I</param>
    /// <returns></returns>
    private fun silk_setup_resamplers(fs_kHz: Int): Int {
        var ret = 0

        if (this.fs_kHz != fs_kHz || this.prev_API_fs_Hz != this.API_fs_Hz) {
            if (this.fs_kHz == 0) {
                /* Initialize the resampler for enc_API.c preparing resampling from API_fs_Hz to fs_kHz */
                ret += Resampler.silk_resampler_init(this.resampler_state, this.API_fs_Hz, fs_kHz * 1000, 1)
            } else {
                val x_buf_API_fs_Hz: ShortArray
                var temp_resampler_state: SilkResamplerState? = null

                val api_buf_samples: Int
                val old_buf_samples: Int
                val buf_length_ms: Int

                buf_length_ms = Inlines.silk_LSHIFT(this.nb_subfr * 5, 1) + SilkConstants.LA_SHAPE_MS
                old_buf_samples = buf_length_ms * this.fs_kHz

                /* Initialize resampler for temporary resampling of x_buf data to API_fs_Hz */
                temp_resampler_state = SilkResamplerState()
                ret += Resampler.silk_resampler_init(
                    temp_resampler_state!!,
                    Inlines.silk_SMULBB(this.fs_kHz, 1000),
                    this.API_fs_Hz,
                    0
                )

                /* Calculate number of samples to temporarily upsample */
                api_buf_samples = buf_length_ms * Inlines.silk_DIV32_16(this.API_fs_Hz, 1000)

                /* Temporary resampling of x_buf data to API_fs_Hz */
                x_buf_API_fs_Hz = ShortArray(api_buf_samples)
                ret += Resampler.silk_resampler(
                    temp_resampler_state!!,
                    x_buf_API_fs_Hz,
                    0,
                    this.x_buf,
                    0,
                    old_buf_samples
                )

                /* Initialize the resampler for enc_API.c preparing resampling from API_fs_Hz to fs_kHz */
                ret += Resampler.silk_resampler_init(
                    this.resampler_state,
                    this.API_fs_Hz,
                    Inlines.silk_SMULBB(fs_kHz, 1000),
                    1
                )

                /* Correct resampler state by resampling buffered data from API_fs_Hz to fs_kHz */
                ret += Resampler.silk_resampler(
                    this.resampler_state,
                    this.x_buf,
                    0,
                    x_buf_API_fs_Hz,
                    0,
                    api_buf_samples
                )
            }
        }

        this.prev_API_fs_Hz = this.API_fs_Hz

        return ret
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="this">I/O</param>
    /// <param name="fs_kHz">I</param>
    /// <param name="PacketSize_ms">I</param>
    /// <returns></returns>
    private fun silk_setup_fs(
        fs_kHz: Int,
        PacketSize_ms: Int
    ): Int {
        var ret = SilkError.SILK_NO_ERROR

        /* Set packet size */
        if (PacketSize_ms != this.PacketSize_ms) {
            if (PacketSize_ms != 10
                && PacketSize_ms != 20
                && PacketSize_ms != 40
                && PacketSize_ms != 60
            ) {
                ret = SilkError.SILK_ENC_PACKET_SIZE_NOT_SUPPORTED
            }
            if (PacketSize_ms <= 10) {
                this.nFramesPerPacket = 1
                this.nb_subfr = if (PacketSize_ms == 10) 2 else 1
                this.frame_length = Inlines.silk_SMULBB(PacketSize_ms, fs_kHz)
                this.pitch_LPC_win_length = Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS_2_SF, fs_kHz)
                if (this.fs_kHz == 8) {
                    this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_NB_iCDF
                } else {
                    this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_iCDF
                }
            } else {
                this.nFramesPerPacket = Inlines.silk_DIV32_16(PacketSize_ms, SilkConstants.MAX_FRAME_LENGTH_MS)
                this.nb_subfr = SilkConstants.MAX_NB_SUBFR
                this.frame_length = Inlines.silk_SMULBB(20, fs_kHz)
                this.pitch_LPC_win_length = Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS, fs_kHz)
                if (this.fs_kHz == 8) {
                    this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_NB_iCDF
                } else {
                    this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_iCDF
                }
            }
            this.PacketSize_ms = PacketSize_ms
            this.TargetRate_bps = 0
            /* trigger new SNR computation */
        }

        /* Set sampling frequency */
        Inlines.OpusAssert(fs_kHz == 8 || fs_kHz == 12 || fs_kHz == 16)
        Inlines.OpusAssert(this.nb_subfr == 2 || this.nb_subfr == 4)
        if (this.fs_kHz != fs_kHz) {
            /* reset part of the state */
            this.sShape.Reset()
            this.sPrefilt.Reset()
            this.sNSQ.Reset()
            Arrays.MemSet(this.prev_NLSFq_Q15, 0.toShort(), SilkConstants.MAX_LPC_ORDER)
            Arrays.MemSet(this.sLP.In_LP_State, 0, 2)
            this.inputBufIx = 0
            this.nFramesEncoded = 0
            this.TargetRate_bps = 0
            /* trigger new SNR computation */

            /* Initialize non-zero parameters */
            this.prevLag = 100
            this.first_frame_after_reset = 1
            this.sPrefilt.lagPrev = 100
            this.sShape.LastGainIndex = 10
            this.sNSQ.lagPrev = 100
            this.sNSQ.prev_gain_Q16 = 65536
            this.prevSignalType = SilkConstants.TYPE_NO_VOICE_ACTIVITY.toByte()

            this.fs_kHz = fs_kHz
            if (this.fs_kHz == 8) {
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

            if (this.fs_kHz == 8 || this.fs_kHz == 12) {
                this.predictLPCOrder = SilkConstants.MIN_LPC_ORDER
                this.psNLSF_CB = SilkTables.silk_NLSF_CB_NB_MB
            } else {
                this.predictLPCOrder = SilkConstants.MAX_LPC_ORDER
                this.psNLSF_CB = SilkTables.silk_NLSF_CB_WB
            }

            this.subfr_length = SilkConstants.SUB_FRAME_LENGTH_MS * fs_kHz
            this.frame_length = Inlines.silk_SMULBB(this.subfr_length, this.nb_subfr)
            this.ltp_mem_length = Inlines.silk_SMULBB(SilkConstants.LTP_MEM_LENGTH_MS, fs_kHz)
            this.la_pitch = Inlines.silk_SMULBB(SilkConstants.LA_PITCH_MS, fs_kHz)
            this.max_pitch_lag = Inlines.silk_SMULBB(18, fs_kHz)

            if (this.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
                this.pitch_LPC_win_length = Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS, fs_kHz)
            } else {
                this.pitch_LPC_win_length = Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS_2_SF, fs_kHz)
            }

            if (this.fs_kHz == 16) {
                this.mu_LTP_Q9 =
                        (TuningParameters.MU_LTP_QUANT_WB * (1.toLong() shl 9) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.MU_LTP_QUANT_WB, 9)*/
                this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform8_iCDF
            } else if (this.fs_kHz == 12) {
                this.mu_LTP_Q9 =
                        (TuningParameters.MU_LTP_QUANT_MB * (1.toLong() shl 9) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.MU_LTP_QUANT_MB, 9)*/
                this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform6_iCDF
            } else {
                this.mu_LTP_Q9 =
                        (TuningParameters.MU_LTP_QUANT_NB * (1.toLong() shl 9) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.MU_LTP_QUANT_NB, 9)*/
                this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform4_iCDF
            }
        }

        /* Check that settings are valid */
        Inlines.OpusAssert(this.subfr_length * this.nb_subfr == this.frame_length)

        return ret
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="this">I/O</param>
    /// <param name="Complexity">O</param>
    /// <returns></returns>
    private fun silk_setup_complexity(Complexity: Int): Int {
        val ret = 0

        /* Set encoding complexity */
        Inlines.OpusAssert(Complexity >= 0 && Complexity <= 10)
        if (Complexity < 2) {
            this.pitchEstimationComplexity = SilkConstants.SILK_PE_MIN_COMPLEX
            this.pitchEstimationThreshold_Q16 =
                    (0.8f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.8f, 16)*/
            this.pitchEstimationLPCOrder = 6
            this.shapingLPCOrder = 8
            this.la_shape = 3 * this.fs_kHz
            this.nStatesDelayedDecision = 1
            this.useInterpolatedNLSFs = 0
            this.LTPQuantLowComplexity = 1
            this.NLSF_MSVQ_Survivors = 2
            this.warping_Q16 = 0
        } else if (Complexity < 4) {
            this.pitchEstimationComplexity = SilkConstants.SILK_PE_MID_COMPLEX
            this.pitchEstimationThreshold_Q16 =
                    (0.76f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.76f, 16)*/
            this.pitchEstimationLPCOrder = 8
            this.shapingLPCOrder = 10
            this.la_shape = 5 * this.fs_kHz
            this.nStatesDelayedDecision = 1
            this.useInterpolatedNLSFs = 0
            this.LTPQuantLowComplexity = 0
            this.NLSF_MSVQ_Survivors = 4
            this.warping_Q16 = 0
        } else if (Complexity < 6) {
            this.pitchEstimationComplexity = SilkConstants.SILK_PE_MID_COMPLEX
            this.pitchEstimationThreshold_Q16 =
                    (0.74f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.74f, 16)*/
            this.pitchEstimationLPCOrder = 10
            this.shapingLPCOrder = 12
            this.la_shape = 5 * this.fs_kHz
            this.nStatesDelayedDecision = 2
            this.useInterpolatedNLSFs = 1
            this.LTPQuantLowComplexity = 0
            this.NLSF_MSVQ_Survivors = 8
            this.warping_Q16 = this.fs_kHz *
                    (TuningParameters.WARPING_MULTIPLIER * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.WARPING_MULTIPLIER, 16)*/
        } else if (Complexity < 8) {
            this.pitchEstimationComplexity = SilkConstants.SILK_PE_MID_COMPLEX
            this.pitchEstimationThreshold_Q16 =
                    (0.72f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.72f, 16)*/
            this.pitchEstimationLPCOrder = 12
            this.shapingLPCOrder = 14
            this.la_shape = 5 * this.fs_kHz
            this.nStatesDelayedDecision = 3
            this.useInterpolatedNLSFs = 1
            this.LTPQuantLowComplexity = 0
            this.NLSF_MSVQ_Survivors = 16
            this.warping_Q16 = this.fs_kHz *
                    (TuningParameters.WARPING_MULTIPLIER * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.WARPING_MULTIPLIER, 16)*/
        } else {
            this.pitchEstimationComplexity = SilkConstants.SILK_PE_MAX_COMPLEX
            this.pitchEstimationThreshold_Q16 =
                    (0.7f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.7f, 16)*/
            this.pitchEstimationLPCOrder = 16
            this.shapingLPCOrder = 16
            this.la_shape = 5 * this.fs_kHz
            this.nStatesDelayedDecision = SilkConstants.MAX_DEL_DEC_STATES
            this.useInterpolatedNLSFs = 1
            this.LTPQuantLowComplexity = 0
            this.NLSF_MSVQ_Survivors = 32
            this.warping_Q16 = this.fs_kHz *
                    (TuningParameters.WARPING_MULTIPLIER * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.WARPING_MULTIPLIER, 16)*/
        }

        /* Do not allow higher pitch estimation LPC order than predict LPC order */
        this.pitchEstimationLPCOrder = Inlines.silk_min_int(this.pitchEstimationLPCOrder, this.predictLPCOrder)
        this.shapeWinLength = SilkConstants.SUB_FRAME_LENGTH_MS * this.fs_kHz + 2 * this.la_shape
        this.Complexity = Complexity

        Inlines.OpusAssert(this.pitchEstimationLPCOrder <= SilkConstants.MAX_FIND_PITCH_LPC_ORDER)
        Inlines.OpusAssert(this.shapingLPCOrder <= SilkConstants.MAX_SHAPE_LPC_ORDER)
        Inlines.OpusAssert(this.nStatesDelayedDecision <= SilkConstants.MAX_DEL_DEC_STATES)
        Inlines.OpusAssert(this.warping_Q16 <= 32767)
        Inlines.OpusAssert(this.la_shape <= SilkConstants.LA_SHAPE_MAX)
        Inlines.OpusAssert(this.shapeWinLength <= SilkConstants.SHAPE_LPC_WIN_MAX)
        Inlines.OpusAssert(this.NLSF_MSVQ_Survivors <= SilkConstants.NLSF_VQ_MAX_SURVIVORS)

        return ret
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="this">I/O</param>
    /// <param name="TargetRate_bps">I</param>
    /// <returns></returns>
    private fun silk_setup_LBRR(TargetRate_bps: Int): Int {
        val LBRR_in_previous_packet: Int
        val ret = SilkError.SILK_NO_ERROR
        var LBRR_rate_thres_bps: Int

        LBRR_in_previous_packet = this.LBRR_enabled
        this.LBRR_enabled = 0
        if (this.useInBandFEC != 0 && this.PacketLoss_perc > 0) {
            if (this.fs_kHz == 8) {
                LBRR_rate_thres_bps = SilkConstants.LBRR_NB_MIN_RATE_BPS
            } else if (this.fs_kHz == 12) {
                LBRR_rate_thres_bps = SilkConstants.LBRR_MB_MIN_RATE_BPS
            } else {
                LBRR_rate_thres_bps = SilkConstants.LBRR_WB_MIN_RATE_BPS
            }

            LBRR_rate_thres_bps = Inlines.silk_SMULWB(
                Inlines.silk_MUL(
                    LBRR_rate_thres_bps,
                    125 - Inlines.silk_min(this.PacketLoss_perc, 25)
                ), (0.01f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.01f, 16)*/
            )

            if (TargetRate_bps > LBRR_rate_thres_bps) {
                /* Set gain increase for coding LBRR excitation */
                if (LBRR_in_previous_packet == 0) {
                    /* Previous packet did not have LBRR, and was therefore coded at a higher bitrate */
                    this.LBRR_GainIncreases = 7
                } else {
                    this.LBRR_GainIncreases = Inlines.silk_max_int(
                        7 - Inlines.silk_SMULWB(
                            this.PacketLoss_perc,
                            (0.4f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.4f, 16)*/
                        ), 2
                    )
                }
                this.LBRR_enabled = 1
            }
        }

        return ret
    }

    /// <summary>
    /// Control sampling rate
    /// </summary>
    /// <param name="this">I/O  Pointer to Silk encoder state</param>
    /// <param name="encControl">I    Control structure</param>
    /// <returns></returns>
    fun silk_control_audio_bandwidth(encControl: EncControlState): Int {
        var fs_kHz: Int
        var fs_Hz: Int

        fs_kHz = this.fs_kHz
        fs_Hz = Inlines.silk_SMULBB(fs_kHz, 1000)

        if (fs_Hz == 0) {
            /* Encoder has just been initialized */
            fs_Hz = Inlines.silk_min(this.desiredInternal_fs_Hz, this.API_fs_Hz)
            fs_kHz = Inlines.silk_DIV32_16(fs_Hz, 1000)
        } else if (fs_Hz > this.API_fs_Hz || fs_Hz > this.maxInternal_fs_Hz || fs_Hz < this.minInternal_fs_Hz) {
            /* Make sure rate is not higher than external rate or maximum allowed, or lower than minimum allowed */
            fs_Hz = this.API_fs_Hz
            fs_Hz = Inlines.silk_min(fs_Hz, this.maxInternal_fs_Hz)
            fs_Hz = Inlines.silk_max(fs_Hz, this.minInternal_fs_Hz)
            fs_kHz = Inlines.silk_DIV32_16(fs_Hz, 1000)
        } else {
            /* State machine for the sampling rate switching */
            if (this.sLP.transition_frame_no >= SilkConstants.TRANSITION_FRAMES) {
                /* Stop transition phase */
                this.sLP.mode = 0
            }

            if (this.allow_bandwidth_switch != 0 || encControl.opusCanSwitch != 0) {
                /* Check if we should switch down */
                if (Inlines.silk_SMULBB(this.fs_kHz, 1000) > this.desiredInternal_fs_Hz) {
                    /* Switch down */
                    if (this.sLP.mode == 0) {
                        /* New transition */
                        this.sLP.transition_frame_no = SilkConstants.TRANSITION_FRAMES

                        /* Reset transition filter state */
                        Arrays.MemSet(this.sLP.In_LP_State, 0, 2)
                    }

                    if (encControl.opusCanSwitch != 0) {
                        /* Stop transition phase */
                        this.sLP.mode = 0

                        /* Switch to a lower sample frequency */
                        fs_kHz = if (this.fs_kHz == 16) 12 else 8
                    } else if (this.sLP.transition_frame_no <= 0) {
                        encControl.switchReady = 1
                        /* Make room for redundancy */
                        encControl.maxBits -= encControl.maxBits * 5 / (encControl.payloadSize_ms + 5)
                    } else {
                        /* Direction: down (at double speed) */
                        this.sLP.mode = -2
                    }
                } else /* Check if we should switch up */ if (Inlines.silk_SMULBB(
                        this.fs_kHz,
                        1000
                    ) < this.desiredInternal_fs_Hz
                ) {
                    /* Switch up */
                    if (encControl.opusCanSwitch != 0) {
                        /* Switch to a higher sample frequency */
                        fs_kHz = if (this.fs_kHz == 8) 12 else 16

                        /* New transition */
                        this.sLP.transition_frame_no = 0

                        /* Reset transition filter state */
                        Arrays.MemSet(this.sLP.In_LP_State, 0, 2)

                        /* Direction: up */
                        this.sLP.mode = 1
                    } else if (this.sLP.mode == 0) {
                        encControl.switchReady = 1
                        /* Make room for redundancy */
                        encControl.maxBits -= encControl.maxBits * 5 / (encControl.payloadSize_ms + 5)
                    } else {
                        /* Direction: up */
                        this.sLP.mode = 1
                    }
                } else if (this.sLP.mode < 0) {
                    this.sLP.mode = 1
                }
            }
        }

        return fs_kHz
    }

    /* Control SNR of residual quantizer */
    fun silk_control_SNR(
        TargetRate_bps: Int /* I    Target max bitrate (bps)                    */
    ): Int {
        var TargetRate_bps = TargetRate_bps
        var k: Int
        val ret = SilkError.SILK_NO_ERROR
        val frac_Q6: Int
        val rateTable: IntArray

        /* Set bitrate/coding quality */
        TargetRate_bps =
                Inlines.silk_LIMIT(TargetRate_bps, SilkConstants.MIN_TARGET_RATE_BPS, SilkConstants.MAX_TARGET_RATE_BPS)
        if (TargetRate_bps != this.TargetRate_bps) {
            this.TargetRate_bps = TargetRate_bps

            /* If new TargetRate_bps, translate to SNR_dB value */
            if (this.fs_kHz == 8) {
                rateTable = SilkTables.silk_TargetRate_table_NB
            } else if (this.fs_kHz == 12) {
                rateTable = SilkTables.silk_TargetRate_table_MB
            } else {
                rateTable = SilkTables.silk_TargetRate_table_WB
            }

            /* Reduce bitrate for 10 ms modes in these calculations */
            if (this.nb_subfr == 2) {
                TargetRate_bps -= TuningParameters.REDUCE_BITRATE_10_MS_BPS
            }

            /* Find bitrate interval in table and interpolate */
            k = 1
            while (k < SilkConstants.TARGET_RATE_TAB_SZ) {
                if (TargetRate_bps <= rateTable[k]) {
                    frac_Q6 = Inlines.silk_DIV32(
                        Inlines.silk_LSHIFT(TargetRate_bps - rateTable[k - 1], 6),
                        rateTable[k] - rateTable[k - 1]
                    )
                    this.SNR_dB_Q7 = Inlines.silk_LSHIFT(SilkTables.silk_SNR_table_Q1[k - 1].toInt(), 6) +
                            Inlines.silk_MUL(
                                frac_Q6,
                                SilkTables.silk_SNR_table_Q1[k] - SilkTables.silk_SNR_table_Q1[k - 1]
                            )
                    break
                }
                k++
            }
        }

        return ret
    }

    fun silk_encode_do_VAD() {
        /**
         * *************************
         */
        /* Voice Activity Detection */
        /**
         * *************************
         */
        VoiceActivityDetection.silk_VAD_GetSA_Q8(this, this.inputBuf, 1)

        /**
         * ***********************************************
         */
        /* Convert speech activity into VAD and DTX flags */
        /**
         * ***********************************************
         */
        if (this.speech_activity_Q8 < (TuningParameters.SPEECH_ACTIVITY_DTX_THRES * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.SPEECH_ACTIVITY_DTX_THRES, 8)*/) {
            this.indices.signalType = SilkConstants.TYPE_NO_VOICE_ACTIVITY.toByte()
            this.noSpeechCounter++
            if (this.noSpeechCounter < SilkConstants.NB_SPEECH_FRAMES_BEFORE_DTX) {
                this.inDTX = 0
            } else if (this.noSpeechCounter > SilkConstants.MAX_CONSECUTIVE_DTX + SilkConstants.NB_SPEECH_FRAMES_BEFORE_DTX) {
                this.noSpeechCounter = SilkConstants.NB_SPEECH_FRAMES_BEFORE_DTX
                this.inDTX = 0
            }
            this.VAD_flags[this.nFramesEncoded] = 0
        } else {
            this.noSpeechCounter = 0
            this.inDTX = 0
            this.indices.signalType = SilkConstants.TYPE_UNVOICED.toByte()
            this.VAD_flags[this.nFramesEncoded] = 1
        }
    }

    /**
     * *************
     */
    /* Encode frame */
    /**
     * *************
     */
    fun silk_encode_frame(
        pnBytesOut: BoxedValueInt, /* O    Pointer to number of payload bytes;                                         */
        psRangeEnc: EntropyCoder, /* I/O  compressor data structure                                                   */
        condCoding: Int, /* I    The type of conditional coding to use                                       */
        maxBits: Int, /* I    If > 0: maximum number of output bits                                       */
        useCBR: Int /* I    Flag to force constant-bitrate operation                                    */
    ): Int {
        val sEncCtrl = SilkEncoderControl()
        var i: Int
        var iter: Int
        val maxIter: Int
        var found_upper: Int
        var found_lower: Int
        val ret = 0
        val x_frame: Int
        val sRangeEnc_copy = EntropyCoder()
        val sRangeEnc_copy2 = EntropyCoder()
        val sNSQ_copy = SilkNSQState()
        val sNSQ_copy2 = SilkNSQState()
        var nBits: Int
        var nBits_lower: Int
        var nBits_upper: Int
        var gainMult_lower: Int
        var gainMult_upper: Int
        var gainsID: Int
        var gainsID_lower: Int
        var gainsID_upper: Int
        var gainMult_Q8: Short
        val ec_prevLagIndex_copy: Short
        val ec_prevSignalType_copy: Int
        var LastGainIndex_copy2: Byte
        val seed_copy: Byte

        /* This is totally unnecessary but many compilers (including gcc) are too dumb to realise it */
        LastGainIndex_copy2 = 0
        gainMult_upper = 0
        gainMult_lower = gainMult_upper
        nBits_upper = gainMult_lower
        nBits_lower = nBits_upper

        this.indices.Seed = (this.frameCounter++ and 3).toByte()

        /**
         * ***********************************************************
         */
        /* Set up Input Pointers, and insert frame in input buffer   */
        /**
         * **********************************************************
         */
        /* start of frame to encode */
        x_frame = this.ltp_mem_length

        /**
         * ************************************
         */
        /* Ensure smooth bandwidth transitions */
        /**
         * ************************************
         */
        this.sLP.silk_LP_variable_cutoff(this.inputBuf, 1, this.frame_length)

        /**
         * ****************************************
         */
        /* Copy new frame to front of input buffer */
        /**
         * ****************************************
         */
        arraycopy(
            this.inputBuf,
            1,
            this.x_buf,
            x_frame + SilkConstants.LA_SHAPE_MS * this.fs_kHz,
            this.frame_length
        )

        if (this.prefillFlag == 0) {
            val xfw_Q3: IntArray
            val res_pitch: ShortArray
            val ec_buf_copy: ByteArray
            val res_pitch_frame: Int

            res_pitch = ShortArray(this.la_pitch + this.frame_length + this.ltp_mem_length)
            /* start of pitch LPC residual frame */
            res_pitch_frame = this.ltp_mem_length

            /**
             * **************************************
             */
            /* Find pitch lags, initial LPC analysis */
            /**
             * **************************************
             */
            FindPitchLags.silk_find_pitch_lags(this, sEncCtrl, res_pitch, this.x_buf, x_frame)

            /**
             * *********************
             */
            /* Noise shape analysis */
            /**
             * *********************
             */
            NoiseShapeAnalysis.silk_noise_shape_analysis(
                this,
                sEncCtrl,
                res_pitch,
                res_pitch_frame,
                this.x_buf,
                x_frame
            )

            /**
             * ************************************************
             */
            /* Find linear prediction coefficients (LPC + LTP) */
            /**
             * ************************************************
             */
            FindPredCoefs.silk_find_pred_coefs(this, sEncCtrl, res_pitch, this.x_buf, x_frame, condCoding)

            /**
             * *************************************
             */
            /* Process gains                        */
            /**
             * *************************************
             */
            ProcessGains.silk_process_gains(this, sEncCtrl, condCoding)

            /**
             * **************************************
             */
            /* Prefiltering for noise shaper         */
            /**
             * **************************************
             */
            xfw_Q3 = IntArray(this.frame_length)
            Filters.silk_prefilter(this, sEncCtrl, xfw_Q3, this.x_buf, x_frame)

            /**
             * *************************************
             */
            /* Low Bitrate Redundant Encoding       */
            /**
             * *************************************
             */
            silk_LBRR_encode(sEncCtrl, xfw_Q3, condCoding)

            /* Loop over quantizer and entropy coding to control bitrate */
            maxIter = 6
            gainMult_Q8 = (1 * (1.toLong() shl 8) + 0.5).toInt().toShort()
            found_lower = 0
            found_upper = 0
            gainsID = GainQuantization.silk_gains_ID(this.indices.GainsIndices, this.nb_subfr)
            gainsID_lower = -1
            gainsID_upper = -1
            /* Copy part of the input state */
            sRangeEnc_copy.Assign(psRangeEnc)
            sNSQ_copy.Assign(this.sNSQ)
            seed_copy = this.indices.Seed
            ec_prevLagIndex_copy = this.ec_prevLagIndex
            ec_prevSignalType_copy = this.ec_prevSignalType
            ec_buf_copy = ByteArray(1275) // fixme: this size might be optimized to the actual size
            iter = 0
            while (true) {
                if (gainsID == gainsID_lower) {
                    nBits = nBits_lower
                } else if (gainsID == gainsID_upper) {
                    nBits = nBits_upper
                } else {
                    /* Restore part of the input state */
                    if (iter > 0) {
                        psRangeEnc.Assign(sRangeEnc_copy)
                        this.sNSQ.Assign(sNSQ_copy)
                        this.indices.Seed = seed_copy
                        this.ec_prevLagIndex = ec_prevLagIndex_copy
                        this.ec_prevSignalType = ec_prevSignalType_copy
                    }

                    /**
                     * **************************************
                     */
                    /* Noise shaping quantization            */
                    /**
                     * **************************************
                     */
                    if (this.nStatesDelayedDecision > 1 || this.warping_Q16 > 0) {
                        sNSQ.silk_NSQ_del_dec(
                            this,
                            this.indices,
                            xfw_Q3,
                            pulses,
                            sEncCtrl.PredCoef_Q12,
                            sEncCtrl.LTPCoef_Q14,
                            sEncCtrl.AR2_Q13,
                            sEncCtrl.HarmShapeGain_Q14,
                            sEncCtrl.Tilt_Q14,
                            sEncCtrl.LF_shp_Q14,
                            sEncCtrl.Gains_Q16,
                            sEncCtrl.pitchL,
                            sEncCtrl.Lambda_Q10,
                            sEncCtrl.LTP_scale_Q14
                        )
                    } else {
                        sNSQ.silk_NSQ(
                            this,
                            this.indices,
                            xfw_Q3,
                            pulses,
                            sEncCtrl.PredCoef_Q12,
                            sEncCtrl.LTPCoef_Q14,
                            sEncCtrl.AR2_Q13,
                            sEncCtrl.HarmShapeGain_Q14,
                            sEncCtrl.Tilt_Q14,
                            sEncCtrl.LF_shp_Q14,
                            sEncCtrl.Gains_Q16,
                            sEncCtrl.pitchL,
                            sEncCtrl.Lambda_Q10,
                            sEncCtrl.LTP_scale_Q14
                        )
                    }

                    /**
                     * *************************************
                     */
                    /* Encode Parameters                    */
                    /**
                     * *************************************
                     */
                    EncodeIndices.silk_encode_indices(this, psRangeEnc, this.nFramesEncoded, 0, condCoding)

                    /**
                     * *************************************
                     */
                    /* Encode Excitation Signal             */
                    /**
                     * *************************************
                     */
                    EncodePulses.silk_encode_pulses(
                        psRangeEnc, this.indices.signalType.toInt(), this.indices.quantOffsetType.toInt(),
                        this.pulses, this.frame_length
                    )

                    nBits = psRangeEnc.tell()

                    if (useCBR == 0 && iter == 0 && nBits <= maxBits) {
                        break
                    }
                }

                if (iter == maxIter) {
                    if (found_lower != 0 && (gainsID == gainsID_lower || nBits > maxBits)) {
                        /* Restore output state from earlier iteration that did meet the bitrate budget */
                        psRangeEnc.Assign(sRangeEnc_copy2)
                        Inlines.OpusAssert(sRangeEnc_copy2.offs <= 1275)
                        psRangeEnc.write_buffer(ec_buf_copy, 0, 0, sRangeEnc_copy2.offs.toInt())
                        this.sNSQ.Assign(sNSQ_copy2)
                        this.sShape.LastGainIndex = LastGainIndex_copy2
                    }
                    break
                }

                if (nBits > maxBits) {
                    if (found_lower == 0 && iter >= 2) {
                        /* Adjust the quantizer's rate/distortion tradeoff and discard previous "upper" results */
                        sEncCtrl.Lambda_Q10 = Inlines.silk_ADD_RSHIFT32(sEncCtrl.Lambda_Q10, sEncCtrl.Lambda_Q10, 1)
                        found_upper = 0
                        gainsID_upper = -1
                    } else {
                        found_upper = 1
                        nBits_upper = nBits
                        gainMult_upper = gainMult_Q8.toInt()
                        gainsID_upper = gainsID
                    }
                } else if (nBits < maxBits - 5) {
                    found_lower = 1
                    nBits_lower = nBits
                    gainMult_lower = gainMult_Q8.toInt()
                    if (gainsID != gainsID_lower) {
                        gainsID_lower = gainsID
                        /* Copy part of the output state */
                        sRangeEnc_copy2.Assign(psRangeEnc)
                        Inlines.OpusAssert(psRangeEnc.offs <= 1275)
                        arraycopy(psRangeEnc._buffer, 0, ec_buf_copy, 0, psRangeEnc.offs.toInt())
                        sNSQ_copy2.Assign(this.sNSQ)
                        LastGainIndex_copy2 = this.sShape.LastGainIndex
                    }
                } else {
                    /* Within 5 bits of budget: close enough */
                    break
                }

                if (found_lower and found_upper == 0) {
                    /* Adjust gain according to high-rate rate/distortion curve */
                    var gain_factor_Q16: Int
                    gain_factor_Q16 = Inlines.silk_log2lin(
                        Inlines.silk_LSHIFT(
                            nBits - maxBits,
                            7
                        ) / this.frame_length + (16 * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(16, 7)*/
                    )
                    gain_factor_Q16 = Inlines.silk_min_32(
                        gain_factor_Q16,
                        (2 * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(2, 16)*/
                    )
                    if (nBits > maxBits) {
                        gain_factor_Q16 = Inlines.silk_max_32(
                            gain_factor_Q16,
                            (1.3f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(1.3f, 16)*/
                        )
                    }

                    gainMult_Q8 = Inlines.silk_SMULWB(gain_factor_Q16, gainMult_Q8.toInt()).toShort()
                } else {
                    /* Adjust gain by interpolating */
                    gainMult_Q8 = (gainMult_lower + Inlines.silk_DIV32_16(
                        Inlines.silk_MUL(
                            gainMult_upper - gainMult_lower,
                            maxBits - nBits_lower
                        ), nBits_upper - nBits_lower
                    )).toShort()
                    /* New gain multplier must be between 25% and 75% of old range (note that gainMult_upper < gainMult_lower) */
                    if (gainMult_Q8 > Inlines.silk_ADD_RSHIFT32(gainMult_lower, gainMult_upper - gainMult_lower, 2)) {
                        gainMult_Q8 =
                                Inlines.silk_ADD_RSHIFT32(gainMult_lower, gainMult_upper - gainMult_lower, 2).toShort()
                    } else if (gainMult_Q8 < Inlines.silk_SUB_RSHIFT32(
                            gainMult_upper,
                            gainMult_upper - gainMult_lower,
                            2
                        )
                    ) {
                        gainMult_Q8 =
                                Inlines.silk_SUB_RSHIFT32(gainMult_upper, gainMult_upper - gainMult_lower, 2).toShort()
                    }
                }

                i = 0
                while (i < this.nb_subfr) {
                    sEncCtrl.Gains_Q16[i] = Inlines.silk_LSHIFT_SAT32(
                        Inlines.silk_SMULWB(
                            sEncCtrl.GainsUnq_Q16[i],
                            gainMult_Q8.toInt()
                        ), 8
                    )
                    i++
                }

                /* Quantize gains */
                this.sShape.LastGainIndex = sEncCtrl.lastGainIndexPrev
                val boxed_gainIndex = BoxedValueByte(this.sShape.LastGainIndex)
                GainQuantization.silk_gains_quant(
                    this.indices.GainsIndices, sEncCtrl.Gains_Q16,
                    boxed_gainIndex, if (condCoding == SilkConstants.CODE_CONDITIONALLY) 1 else 0, this.nb_subfr
                )
                this.sShape.LastGainIndex = boxed_gainIndex.Val

                /* Unique identifier of gains vector */
                gainsID = GainQuantization.silk_gains_ID(this.indices.GainsIndices, this.nb_subfr)
                iter++
            }
        }

        /* Update input buffer */
        Arrays.MemMove(this.x_buf, this.frame_length, 0, this.ltp_mem_length + SilkConstants.LA_SHAPE_MS * this.fs_kHz)

        /* Exit without entropy coding */
        if (this.prefillFlag != 0) {
            /* No payload */
            pnBytesOut.Val = 0

            return ret
        }

        /* Parameters needed for next frame */
        this.prevLag = sEncCtrl.pitchL[this.nb_subfr - 1]
        this.prevSignalType = this.indices.signalType

        /**
         * *************************************
         */
        /* Finalize payload                     */
        /**
         * *************************************
         */
        this.first_frame_after_reset = 0
        /* Payload size */
        pnBytesOut.Val = Inlines.silk_RSHIFT(psRangeEnc.tell() + 7, 3)

        return ret
    }

    /* Low-Bitrate Redundancy (LBRR) encoding. Reuse all parameters but encode excitation at lower bitrate  */
    fun silk_LBRR_encode(
        thisCtrl: SilkEncoderControl, /* I/O  Pointer to Silk FIX encoder control struct                                  */
        xfw_Q3: IntArray, /* I    Input signal                                                                */
        condCoding: Int /* I    The type of conditional coding used so far for this frame                   */
    ) {
        val TempGains_Q16 = IntArray(/*SilkConstants.MAX_NB_SUBFR*/this.nb_subfr)
        val psIndices_LBRR = this.indices_LBRR[this.nFramesEncoded]
        val sNSQ_LBRR = SilkNSQState()

        /**
         * ****************************************
         */
        /* Control use of inband LBRR              */
        /**
         * ****************************************
         */
        if (this.LBRR_enabled != 0 && this.speech_activity_Q8 > (TuningParameters.LBRR_SPEECH_ACTIVITY_THRES * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.LBRR_SPEECH_ACTIVITY_THRES, 8)*/) {
            this.LBRR_flags[this.nFramesEncoded] = 1

            /* Copy noise shaping quantizer state and quantization indices from regular encoding */
            sNSQ_LBRR.Assign(this.sNSQ)
            psIndices_LBRR!!.Assign(this.indices)

            /* Save original gains */
            arraycopy(thisCtrl.Gains_Q16, 0, TempGains_Q16, 0, this.nb_subfr)

            if (this.nFramesEncoded == 0 || this.LBRR_flags[this.nFramesEncoded - 1] == 0) {
                /* First frame in packet or previous frame not LBRR coded */
                this.LBRRprevLastGainIndex = this.sShape.LastGainIndex

                /* Increase Gains to get target LBRR rate */
                psIndices_LBRR.GainsIndices[0] = (psIndices_LBRR.GainsIndices[0] + this.LBRR_GainIncreases).toByte()
                psIndices_LBRR.GainsIndices[0] =
                        Inlines.silk_min_int(psIndices_LBRR.GainsIndices[0].toInt(), SilkConstants.N_LEVELS_QGAIN - 1)
                            .toByte()
            }

            /* Decode to get gains in sync with decoder         */
            /* Overwrite unquantized gains with quantized gains */
            val boxed_gainIndex = BoxedValueByte(this.LBRRprevLastGainIndex)
            GainQuantization.silk_gains_dequant(
                thisCtrl.Gains_Q16, psIndices_LBRR.GainsIndices,
                boxed_gainIndex, if (condCoding == SilkConstants.CODE_CONDITIONALLY) 1 else 0, this.nb_subfr
            )
            this.LBRRprevLastGainIndex = boxed_gainIndex.Val

            /**
             * **************************************
             */
            /* Noise shaping quantization            */
            /**
             * **************************************
             */
            if (this.nStatesDelayedDecision > 1 || this.warping_Q16 > 0) {
                sNSQ_LBRR.silk_NSQ_del_dec(
                    this,
                    psIndices_LBRR,
                    xfw_Q3,
                    this.pulses_LBRR[this.nFramesEncoded],
                    thisCtrl.PredCoef_Q12,
                    thisCtrl.LTPCoef_Q14,
                    thisCtrl.AR2_Q13,
                    thisCtrl.HarmShapeGain_Q14,
                    thisCtrl.Tilt_Q14,
                    thisCtrl.LF_shp_Q14,
                    thisCtrl.Gains_Q16,
                    thisCtrl.pitchL,
                    thisCtrl.Lambda_Q10,
                    thisCtrl.LTP_scale_Q14
                )
            } else {
                sNSQ_LBRR.silk_NSQ(
                    this,
                    psIndices_LBRR,
                    xfw_Q3,
                    this.pulses_LBRR[this.nFramesEncoded],
                    thisCtrl.PredCoef_Q12,
                    thisCtrl.LTPCoef_Q14,
                    thisCtrl.AR2_Q13,
                    thisCtrl.HarmShapeGain_Q14,
                    thisCtrl.Tilt_Q14,
                    thisCtrl.LF_shp_Q14,
                    thisCtrl.Gains_Q16,
                    thisCtrl.pitchL,
                    thisCtrl.Lambda_Q10,
                    thisCtrl.LTP_scale_Q14
                )
            }

            /* Restore original gains */
            arraycopy(TempGains_Q16, 0, thisCtrl.Gains_Q16, 0, this.nb_subfr)
        }
    }
}
