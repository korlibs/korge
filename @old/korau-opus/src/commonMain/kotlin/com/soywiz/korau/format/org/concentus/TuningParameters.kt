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

internal object TuningParameters {

    /* Decay time for EntropyCoder.BITREServoir */
    val BITRESERVOIR_DECAY_TIME_MS = 500

    /**
     * ****************
     */
    /* Pitch estimator */
    /**
     * ****************
     */

    /* Level of noise floor for whitening filter LPC analysis in pitch analysis */
    val FIND_PITCH_WHITE_NOISE_FRACTION = 1e-3f

    /* Bandwidth expansion for whitening filter in pitch analysis */
    val FIND_PITCH_BANDWIDTH_EXPANSION = 0.99f

    /**
     * ******************
     */
    /* Linear prediction */
    /**
     * ******************
     */

    /* LPC analysis regularization */
    val FIND_LPC_COND_FAC = 1e-5f

    /* LTP analysis defines */
    val FIND_LTP_COND_FAC = 1e-5f
    val LTP_DAMPING = 0.05f
    val LTP_SMOOTHING = 0.1f

    /* LTP quantization settings */
    val MU_LTP_QUANT_NB = 0.03f
    val MU_LTP_QUANT_MB = 0.025f
    val MU_LTP_QUANT_WB = 0.02f

    /* Max cumulative LTP gain */
    val MAX_SUM_LOG_GAIN_DB = 250.0f

    /**
     * ********************
     */
    /* High pass filtering */
    /**
     * ********************
     */

    /* Smoothing parameters for low end of pitch frequency range estimation */
    val VARIABLE_HP_SMTH_COEF1 = 0.1f
    val VARIABLE_HP_SMTH_COEF2 = 0.015f
    val VARIABLE_HP_MAX_DELTA_FREQ = 0.4f

    /* Min and max cut-off frequency values (-3 dB points) */
    val VARIABLE_HP_MIN_CUTOFF_HZ = 60
    val VARIABLE_HP_MAX_CUTOFF_HZ = 100

    /**
     * ********
     */
    /* Various */
    /**
     * ********
     */

    /* VAD threshold */
    val SPEECH_ACTIVITY_DTX_THRES = 0.05f

    /* Speech Activity LBRR enable threshold */
    val LBRR_SPEECH_ACTIVITY_THRES = 0.3f

    /**
     * **********************
     */
    /* Perceptual parameters */
    /**
     * **********************
     */

    /* reduction in coding SNR during low speech activity */
    val BG_SNR_DECR_dB = 2.0f

    /* factor for reducing quantization noise during voiced speech */
    val HARM_SNR_INCR_dB = 2.0f

    /* factor for reducing quantization noise for unvoiced sparse signals */
    val SPARSE_SNR_INCR_dB = 2.0f

    /* threshold for sparseness measure above which to use lower quantization offset during unvoiced */
    val SPARSENESS_THRESHOLD_QNT_OFFSET = 0.75f

    /* warping control */
    val WARPING_MULTIPLIER = 0.015f

    /* fraction added to first autocorrelation value */
    val SHAPE_WHITE_NOISE_FRACTION = 5e-5f

    /* noise shaping filter chirp factor */
    val BANDWIDTH_EXPANSION = 0.95f

    /* difference between chirp factors for analysis and synthesis noise shaping filters at low bitrates */
    val LOW_RATE_BANDWIDTH_EXPANSION_DELTA = 0.01f

    /* extra harmonic boosting (signal shaping) at low bitrates */
    val LOW_RATE_HARMONIC_BOOST = 0.1f

    /* extra harmonic boosting (signal shaping) for noisy input signals */
    val LOW_INPUT_QUALITY_HARMONIC_BOOST = 0.1f

    /* harmonic noise shaping */
    val HARMONIC_SHAPING = 0.3f

    /* extra harmonic noise shaping for high bitrates or noisy input */
    val HIGH_RATE_OR_LOW_QUALITY_HARMONIC_SHAPING = 0.2f

    /* parameter for shaping noise towards higher frequencies */
    val HP_NOISE_COEF = 0.25f

    /* parameter for shaping noise even more towards higher frequencies during voiced speech */
    val HARM_HP_NOISE_COEF = 0.35f

    /* parameter for applying a high-pass tilt to the input signal */
    val INPUT_TILT = 0.05f

    /* parameter for extra high-pass tilt to the input signal at high rates */
    val HIGH_RATE_INPUT_TILT = 0.1f

    /* parameter for reducing noise at the very low frequencies */
    val LOW_FREQ_SHAPING = 4.0f

    /* less reduction of noise at the very low frequencies for signals with low SNR at low frequencies */
    val LOW_QUALITY_LOW_FREQ_SHAPING_DECR = 0.5f

    /* subframe smoothing coefficient for HarmBoost, HarmShapeGain, Tilt (lower . more smoothing) */
    val SUBFR_SMTH_COEF = 0.4f

    /* parameters defining the R/D tradeoff in the residual quantizer */
    val LAMBDA_OFFSET = 1.2f
    val LAMBDA_SPEECH_ACT = -0.2f
    val LAMBDA_DELAYED_DECISIONS = -0.05f
    val LAMBDA_INPUT_QUALITY = -0.1f
    val LAMBDA_CODING_QUALITY = -0.2f
    val LAMBDA_QUANT_OFFSET = 0.8f

    /* Compensation in bitrate calculations for 10 ms modes */
    val REDUCE_BITRATE_10_MS_BPS = 2200

    /* Maximum time before allowing a bandwidth transition */
    val MAX_BANDWIDTH_SWITCH_DELAY_MS = 5000
}
