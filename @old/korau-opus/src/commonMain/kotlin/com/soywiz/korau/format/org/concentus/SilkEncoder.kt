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

/// <summary>
/// Encoder Super Struct
/// </summary>
internal class SilkEncoder {

    val state_Fxx = Array<SilkChannelEncoder>(SilkConstants.ENCODER_NUM_CHANNELS) { SilkChannelEncoder() }
    val sStereo = StereoEncodeState()
    var nBitsUsedLBRR = 0
    var nBitsExceeded = 0
    var nChannelsAPI = 0
    var nChannelsInternal = 0
    var nPrevChannelsInternal = 0
    var timeSinceSwitchAllowed_ms = 0
    var allowBandwidthSwitch = 0
    var prev_decode_only_middle = 0

    fun Reset() {
        for (c in 0 until SilkConstants.ENCODER_NUM_CHANNELS) {
            state_Fxx[c]!!.Reset()
        }

        sStereo.Reset()
        nBitsUsedLBRR = 0
        nBitsExceeded = 0
        nChannelsAPI = 0
        nChannelsInternal = 0
        nPrevChannelsInternal = 0
        timeSinceSwitchAllowed_ms = 0
        allowBandwidthSwitch = 0
        prev_decode_only_middle = 0
    }

    companion object {

        /// <summary>
        /// Initialize Silk Encoder state
        /// </summary>
        /// <param name="psEnc">I/O  Pointer to Silk FIX encoder state</param>
        /// <param name="arch">I    Run-time architecture</param>
        /// <returns></returns>
        fun silk_init_encoder(psEnc: SilkChannelEncoder): Int {
            var ret = 0

            // Clear the entire encoder state
            psEnc.Reset()

            psEnc.variable_HP_smth1_Q15 = Inlines.silk_LSHIFT(
                Inlines.silk_lin2log((TuningParameters.VARIABLE_HP_MIN_CUTOFF_HZ * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(TuningParameters.VARIABLE_HP_MIN_CUTOFF_HZ, 16)*/) - (16 shl 7),
                8
            )
            psEnc.variable_HP_smth2_Q15 = psEnc.variable_HP_smth1_Q15

            // Used to deactivate LSF interpolation, pitch prediction
            psEnc.first_frame_after_reset = 1

            // Initialize Silk VAD
            ret += VoiceActivityDetection.silk_VAD_Init(psEnc.sVAD)

            return ret
        }
    }
}
