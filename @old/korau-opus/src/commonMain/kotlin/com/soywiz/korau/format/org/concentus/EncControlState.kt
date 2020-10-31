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
/// Structure for controlling encoder operation
/// </summary>
internal class EncControlState {

    /* I:   Number of channels; 1/2                                                         */
    var nChannelsAPI = 0

    /* I:   Number of channels; 1/2                                                         */
    var nChannelsInternal = 0

    /* I:   Input signal sampling rate in Hertz; 8000/12000/16000/24000/32000/44100/48000   */
    var API_sampleRate = 0

    /* I:   Maximum sampling rate in Hertz; 8000/12000/16000                       */
    var maxInternalSampleRate = 0

    /* I:   Minimum sampling rate in Hertz; 8000/12000/16000                       */
    var minInternalSampleRate = 0

    /* I:   Soft request for sampling rate in Hertz; 8000/12000/16000              */
    var desiredInternalSampleRate = 0

    /* I:   Number of samples per packet in milliseconds; 10/20/40/60                       */
    var payloadSize_ms = 0

    /* I:   Bitrate during active speech in bits/second; internally limited                 */
    var bitRate = 0

    /* I:   Uplink packet loss in percent (0-100)                                           */
    var packetLossPercentage = 0

    /* I:   Complexity mode; 0 is lowest, 10 is highest complexity                          */
    var complexity = 0

    /* I:   Flag to enable in-band Forward Error Correction (FEC); 0/1                      */
    var useInBandFEC = 0

    /* I:   Flag to enable discontinuous transmission (DTX); 0/1                            */
    var useDTX = 0

    /* I:   Flag to use constant bitrate                                                    */
    var useCBR = 0

    /* I:   Maximum number of bits allowed for the frame                                    */
    var maxBits = 0

    /* I:   Causes a smooth downmix to mono                                                 */
    var toMono = 0

    /* I:   Opus encoder is allowing us to switch bandwidth                                 */
    var opusCanSwitch = 0

    /* I: Make frames as independent as possible (but still use LPC)                        */
    var reducedDependency = 0

    /* O:   Internal sampling rate used, in Hertz; 8000/12000/16000                         */
    var internalSampleRate = 0

    /* O: Flag that bandwidth switching is allowed (because low voice activity)             */
    var allowBandwidthSwitch = 0

    /* O:   Flag that SILK runs in WB mode without variable LP filter (use for switching between WB/SWB/FB) */
    var inWBmodeWithoutVariableLP = 0

    /* O:   Stereo width */
    var stereoWidth_Q14 = 0

    /* O:   Tells the Opus encoder we're ready to switch                                    */
    var switchReady = 0

    fun Reset() {
        nChannelsAPI = 0
        nChannelsInternal = 0
        API_sampleRate = 0
        maxInternalSampleRate = 0
        minInternalSampleRate = 0
        desiredInternalSampleRate = 0
        payloadSize_ms = 0
        bitRate = 0
        packetLossPercentage = 0
        complexity = 0
        useInBandFEC = 0
        useDTX = 0
        useCBR = 0
        maxBits = 0
        toMono = 0
        opusCanSwitch = 0
        reducedDependency = 0
        internalSampleRate = 0
        allowBandwidthSwitch = 0
        inWBmodeWithoutVariableLP = 0
        stereoWidth_Q14 = 0
        switchReady = 0
    }

    /// <summary>
    /// Checks this encoder control struct and returns error code, if any
    /// </summary>
    /// <returns></returns>
    fun check_control_input(): Int {
        if ((API_sampleRate != 8000
                    && API_sampleRate != 12000
                    && API_sampleRate != 16000
                    && API_sampleRate != 24000
                    && API_sampleRate != 32000
                    && API_sampleRate != 44100
                    && API_sampleRate != 48000)
            || (desiredInternalSampleRate != 8000
                    && desiredInternalSampleRate != 12000
                    && desiredInternalSampleRate != 16000)
            || (maxInternalSampleRate != 8000
                    && maxInternalSampleRate != 12000
                    && maxInternalSampleRate != 16000)
            || (minInternalSampleRate != 8000
                    && minInternalSampleRate != 12000
                    && minInternalSampleRate != 16000)
            || minInternalSampleRate > desiredInternalSampleRate
            || maxInternalSampleRate < desiredInternalSampleRate
            || minInternalSampleRate > maxInternalSampleRate
        ) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_FS_NOT_SUPPORTED
        }
        if (payloadSize_ms != 10
            && payloadSize_ms != 20
            && payloadSize_ms != 40
            && payloadSize_ms != 60
        ) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_PACKET_SIZE_NOT_SUPPORTED
        }
        if (packetLossPercentage < 0 || packetLossPercentage > 100) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_LOSS_RATE
        }
        if (useDTX < 0 || useDTX > 1) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_DTX_SETTING
        }
        if (useCBR < 0 || useCBR > 1) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_CBR_SETTING
        }
        if (useInBandFEC < 0 || useInBandFEC > 1) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_INBAND_FEC_SETTING
        }
        if (nChannelsAPI < 1 || nChannelsAPI > SilkConstants.ENCODER_NUM_CHANNELS) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_NUMBER_OF_CHANNELS_ERROR
        }
        if (nChannelsInternal < 1 || nChannelsInternal > SilkConstants.ENCODER_NUM_CHANNELS) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_NUMBER_OF_CHANNELS_ERROR
        }
        if (nChannelsInternal > nChannelsAPI) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_NUMBER_OF_CHANNELS_ERROR
        }
        if (complexity < 0 || complexity > 10) {
            Inlines.OpusAssert(false)
            return SilkError.SILK_ENC_INVALID_COMPLEXITY_SETTING
        }

        return SilkError.SILK_NO_ERROR
    }
}
