/* Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Originally written by Jean-Marc Valin, Gregory Maxwell, Koen Vos,
   Timothy B. Terriberry, and the Opus open-source contributors
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
import kotlin.math.*

/**
 * The Opus decoder structure.
 *
 * Opus is a stateful codec with overlapping blocks and as a result Opus
 * packets are not coded independently of each other. Packets must be
 * passed into the decoder serially and in the correct order for a correct
 * decode. Lost packets can be replaced with loss concealment by calling
 * the decoder with a null reference and zero length for the missing packet.
 *
 * A single codec state may only be accessed from a single thread at
 * a time and any required locking must be performed by the caller. Separate
 * streams must be decoded with separate decoder states and can be decoded
 * in parallel.
 */
class OpusDecoder {

    internal var channels: Int = 0
    var sampleRate: Int = 0
        internal set
    /**
     * Sampling rate (at the API level)
     */
    internal val DecControl = DecControlState()
    internal var decode_gain: Int = 0

    /* Everything beyond this point gets cleared on a reset */
    internal var stream_channels: Int = 0
    lateinit var bandwidth: OpusBandwidth internal set
	lateinit internal var mode: OpusMode
	lateinit internal var prev_mode: OpusMode
    internal var frame_size: Int = 0
    internal var prev_redundancy: Int = 0
    var lastPacketDuration: Int = 0
        internal set
    var finalRange: Int = 0
        internal set
    internal var SilkDecoder = SilkDecoder()
    internal var Celt_Decoder = CeltDecoder()

    val pitch: Int
        get() = if (prev_mode == OpusMode.MODE_CELT_ONLY) {
            Celt_Decoder.GetPitch()
        } else {
            DecControl.prevPitchLag
        }

    var gain: Int
        get() = decode_gain
        set(value) {
            if (value < -32768 || value > 32767) {
                throw IllegalArgumentException("Gain must be within the range of a signed int16")
            }

            decode_gain = value
        }

    internal constructor() {} // used internally

    internal fun reset() {
        channels = 0
        sampleRate = 0
        /**
         * Sampling rate (at the API level)
         */
        DecControl.Reset()
        decode_gain = 0
        partialReset()
    }

    /// <summary>
    /// OPUS_DECODER_RESET_START
    /// </summary>
    internal fun partialReset() {
        stream_channels = 0
        bandwidth = OpusBandwidth.OPUS_BANDWIDTH_UNKNOWN
        mode = OpusMode.MODE_UNKNOWN
        prev_mode = OpusMode.MODE_UNKNOWN
        frame_size = 0
        prev_redundancy = 0
        lastPacketDuration = 0
        finalRange = 0
        // fixme: do these get reset here? I don't think they do because init_celt and init_silk should both call RESET_STATE on their respective states
        //SilkDecoder.Reset();
        //CeltDecoder.Reset();
    }

    internal fun opus_decoder_init(Fs: Int, channels: Int): Int {
        val silk_dec: SilkDecoder
        val celt_dec: CeltDecoder
        var ret: Int

        if (Fs != 48000 && Fs != 24000 && Fs != 16000 && Fs != 12000 && Fs != 8000 || channels != 1 && channels != 2) {
            return OpusError.OPUS_BAD_ARG
        }
        this.reset()

        /* Initialize SILK encoder */
        silk_dec = this.SilkDecoder
        celt_dec = this.Celt_Decoder
        this.channels = channels
        this.stream_channels = this.channels

        this.sampleRate = Fs
        this.DecControl.API_sampleRate = this.sampleRate
        this.DecControl.nChannelsAPI = this.channels

        /* Reset decoder */
        ret = DecodeAPI.silk_InitDecoder(silk_dec)
        if (ret != 0) {
            return OpusError.OPUS_INTERNAL_ERROR
        }

        /* Initialize CELT decoder */
        ret = celt_dec.celt_decoder_init(Fs, channels)
        if (ret != OpusError.OPUS_OK) {
            return OpusError.OPUS_INTERNAL_ERROR
        }

        celt_dec.SetSignalling(0)

        this.prev_mode = OpusMode.MODE_UNKNOWN
        this.frame_size = Fs / 400
        return OpusError.OPUS_OK
    }

    /**
     * Allocates and initializes a decoder state. Internally Opus stores data at
     * 48000 Hz, so that should be the default value for Fs. However, the
     * decoder can efficiently decode to buffers at 8, 12, 16, and 24 kHz so if
     * for some reason the caller cannot use data at the full sample rate, or
     * knows the compressed data doesn't use the full frequency range, it can
     * request decoding at a reduced rate. Likewise, the decoder is capable of
     * filling in either mono or interleaved stereo pcm buffers, at the caller's
     * request.
     *
     * @param Fs Sample rate to decode at (Hz). This must be one of 8000, 12000,
     * 16000, 24000, or 48000.
     * @param channels Number of channels (1 or 2) to decode.
     * @throws OpusException
     */
    constructor(Fs: Int, channels: Int) {
        val ret: Int
        if (Fs != 48000 && Fs != 24000 && Fs != 16000 && Fs != 12000 && Fs != 8000) {
            throw IllegalArgumentException("Sample rate is invalid (must be 8/12/16/24/48 Khz)")
        }
        if (channels != 1 && channels != 2) {
            throw IllegalArgumentException("Number of channels must be 1 or 2")
        }

        ret = this.opus_decoder_init(Fs, channels)
        if (ret != OpusError.OPUS_OK) {
            if (ret == OpusError.OPUS_BAD_ARG) {
                throw IllegalArgumentException("OPUS_BAD_ARG when creating decoder")
            }
            throw OpusException("Error while initializing decoder", ret)
        }
    }

    internal fun opus_decode_frame(
        data: ByteArray?, data_ptr: Int,
        len: Int, pcm: ShortArray?, pcm_ptr: Int, frame_size: Int, decode_fec: Int
    ): Int {
        var data = data
        var len = len
        var pcm_ptr = pcm_ptr
        var frame_size = frame_size
        val silk_dec: SilkDecoder
        val celt_dec: CeltDecoder
        var i: Int
        var silk_ret = 0
        var celt_ret = 0
        val dec = EntropyCoder() // porting note: stack var
        var silk_frame_size: Int
        val pcm_silk_size: Int
        val pcm_silk: ShortArray
        var pcm_transition_silk_size: Int
        val pcm_transition_silk: ShortArray
        var pcm_transition_celt_size: Int
        val pcm_transition_celt: ShortArray
        var pcm_transition: ShortArray? = null
        val redundant_audio_size: Int
        val redundant_audio: ShortArray

        var audiosize: Int
        val mode: OpusMode
        var transition = 0
        var start_band: Int
        var redundancy = 0
        var redundancy_bytes = 0
        var celt_to_silk = 0
        var c: Int
        val F2_5: Int
        val F5: Int
        val F10: Int
        val F20: Int
        val window: IntArray?
        var redundant_rng = 0
        val celt_accum: Int

        silk_dec = this.SilkDecoder
        celt_dec = this.Celt_Decoder
        F20 = this.sampleRate / 50
        F10 = F20 shr 1
        F5 = F10 shr 1
        F2_5 = F5 shr 1
        if (frame_size < F2_5) {

            return OpusError.OPUS_BUFFER_TOO_SMALL
        }
        /* Limit frame_size to avoid excessive stack allocations. */
        frame_size = Inlines.IMIN(frame_size, this.sampleRate / 25 * 3)
        /* Payloads of 1 (2 including ToC) or 0 trigger the PLC/DTX */
        if (len <= 1) {
            data = null
            /* In that case, don't conceal more than what the ToC says */
            frame_size = Inlines.IMIN(frame_size, this.frame_size)
        }
        if (data != null) {
            audiosize = this.frame_size
            mode = this.mode
            dec.dec_init(data, data_ptr, len)
        } else {
            audiosize = frame_size
            mode = this.prev_mode

            if (mode == OpusMode.MODE_UNKNOWN) {
                /* If we haven't got any packet yet, all we can do is return zeros */
                i = pcm_ptr
                while (i < pcm_ptr + audiosize * this.channels) {
                    pcm!![i] = 0
                    i++
                }

                return audiosize
            }

            /* Avoids trying to run the PLC on sizes other than 2.5 (CELT), 5 (CELT),
               10, or 20 (e.g. 12.5 or 30 ms). */
            if (audiosize > F20) {
                do {
                    val ret = opus_decode_frame(null, 0, 0, pcm, pcm_ptr, Inlines.IMIN(audiosize, F20), 0)
                    if (ret < 0) {

                        return ret
                    }
                    pcm_ptr += ret * this.channels
                    audiosize -= ret
                } while (audiosize > 0)

                return frame_size
            } else if (audiosize < F20) {
                if (audiosize > F10) {
                    audiosize = F10
                } else if (mode != OpusMode.MODE_SILK_ONLY && audiosize > F5 && audiosize < F10) {
                    audiosize = F5
                }
            }
        }

        /* In fixed-point, we can tell CELT to do the accumulation on top of the
           SILK PCM buffer. This saves some stack space. */
        celt_accum = if (mode != OpusMode.MODE_CELT_ONLY && frame_size >= F10) 1 else 0

        pcm_transition_silk_size = 0
        pcm_transition_celt_size = 0
        if (data != null && this.prev_mode != OpusMode.MODE_UNKNOWN && this.prev_mode != OpusMode.MODE_AUTO && (mode == OpusMode.MODE_CELT_ONLY && this.prev_mode != OpusMode.MODE_CELT_ONLY && this.prev_redundancy == 0 || mode != OpusMode.MODE_CELT_ONLY && this.prev_mode == OpusMode.MODE_CELT_ONLY)) {
            transition = 1
            /* Decide where to allocate the stack memory for pcm_transition */
            if (mode == OpusMode.MODE_CELT_ONLY) {
                pcm_transition_celt_size = F5 * this.channels
            } else {
                pcm_transition_silk_size = F5 * this.channels
            }
        }
        pcm_transition_celt = ShortArray(pcm_transition_celt_size)
        if (transition != 0 && mode == OpusMode.MODE_CELT_ONLY) {
            pcm_transition = pcm_transition_celt
            opus_decode_frame(null, 0, 0, pcm_transition, 0, Inlines.IMIN(F5, audiosize), 0)
        }
        if (audiosize > frame_size) {
            /*fprintf(stderr, "PCM buffer too small: %d vs %d (mode = %d)\n", audiosize, frame_size, mode);*/

            return OpusError.OPUS_BAD_ARG
        } else {
            frame_size = audiosize
        }

        /* Don't allocate any memory when in CELT-only mode */
        pcm_silk_size = if (mode != OpusMode.MODE_CELT_ONLY && celt_accum == 0) Inlines.IMAX(
            F10,
            frame_size
        ) * this.channels else 0
        pcm_silk = ShortArray(pcm_silk_size)

        /* SILK processing */
        if (mode != OpusMode.MODE_CELT_ONLY) {
            val lost_flag: Int
            var decoded_samples: Int
            val pcm_ptr2: ShortArray?
            var pcm_ptr2_ptr = 0

            if (celt_accum != 0) {
                pcm_ptr2 = pcm
                pcm_ptr2_ptr = pcm_ptr
            } else {
                pcm_ptr2 = pcm_silk
                pcm_ptr2_ptr = 0
            }

            if (this.prev_mode == OpusMode.MODE_CELT_ONLY) {
                DecodeAPI.silk_InitDecoder(silk_dec)
            }

            /* The SILK PLC cannot produce frames of less than 10 ms */
            this.DecControl.payloadSize_ms = Inlines.IMAX(10, 1000 * audiosize / this.sampleRate)

            if (data != null) {
                this.DecControl.nChannelsInternal = this.stream_channels
                if (mode == OpusMode.MODE_SILK_ONLY) {
                    if (this.bandwidth == OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND) {
                        this.DecControl.internalSampleRate = 8000
                    } else if (this.bandwidth == OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND) {
                        this.DecControl.internalSampleRate = 12000
                    } else if (this.bandwidth == OpusBandwidth.OPUS_BANDWIDTH_WIDEBAND) {
                        this.DecControl.internalSampleRate = 16000
                    } else {
                        this.DecControl.internalSampleRate = 16000
                        Inlines.OpusAssert(false)
                    }
                } else {
                    /* Hybrid mode */
                    this.DecControl.internalSampleRate = 16000
                }
            }

            lost_flag = if (data == null) 1 else 2 * decode_fec
            decoded_samples = 0
            do {
                /* Call SILK decoder */
                val first_frame = if (decoded_samples == 0) 1 else 0
                val boxed_silk_frame_size = BoxedValueInt(0)
                silk_ret = DecodeAPI.silk_Decode(
                    silk_dec, this.DecControl,
                    lost_flag, first_frame, dec, pcm_ptr2!!, pcm_ptr2_ptr, boxed_silk_frame_size
                )
                silk_frame_size = boxed_silk_frame_size.Val

                if (silk_ret != 0) {
                    if (lost_flag != 0) {
                        /* PLC failure should not be fatal */
                        silk_frame_size = frame_size
                        Arrays.MemSetWithOffset(pcm_ptr2, 0.toShort(), pcm_ptr2_ptr, frame_size * this.channels)
                    } else {

                        return OpusError.OPUS_INTERNAL_ERROR
                    }
                }
                pcm_ptr2_ptr += silk_frame_size * this.channels
                decoded_samples += silk_frame_size
            } while (decoded_samples < frame_size)
        }

        start_band = 0
        if (decode_fec == 0 && mode != OpusMode.MODE_CELT_ONLY && data != null
            && dec.tell() + 17 + 20 * (if (this.mode == OpusMode.MODE_HYBRID) 1 else 0) <= 8 * len
        ) {
            /* Check if we have a redundant 0-8 kHz band */
            if (mode == OpusMode.MODE_HYBRID) {
                redundancy = dec.dec_bit_logp(12)
            } else {
                redundancy = 1
            }
            if (redundancy != 0) {
                celt_to_silk = dec.dec_bit_logp(1)
                /* redundancy_bytes will be at least two, in the non-hybrid
                   case due to the ec_tell() check above */
                redundancy_bytes = if (mode == OpusMode.MODE_HYBRID)
                    dec.dec_uint(256).toInt() + 2
                else
                    len - (dec.tell() + 7 shr 3)
                len -= redundancy_bytes
                /* This is a sanity check. It should never happen for a valid
                   packet, so the exact behaviour is not normative. */
                if (len * 8 < dec.tell()) {
                    len = 0
                    redundancy_bytes = 0
                    redundancy = 0
                }
                /* Shrink decoder because of raw bits */
                dec.storage = dec.storage - redundancy_bytes
            }
        }
        if (mode != OpusMode.MODE_CELT_ONLY) {
            start_band = 17
        }

        run {
            var endband = 21

            when (this.bandwidth) {
                OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND -> endband = 13
                OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND, OpusBandwidth.OPUS_BANDWIDTH_WIDEBAND -> endband = 17
                OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND -> endband = 19
                OpusBandwidth.OPUS_BANDWIDTH_FULLBAND -> endband = 21
            }
            celt_dec.SetEndBand(endband)
            celt_dec.SetChannels(this.stream_channels)
        }

        if (redundancy != 0) {
            transition = 0
            pcm_transition_silk_size = 0
        }

        pcm_transition_silk = ShortArray(pcm_transition_silk_size)

        if (transition != 0 && mode != OpusMode.MODE_CELT_ONLY) {
            pcm_transition = pcm_transition_silk
            opus_decode_frame(null, 0, 0, pcm_transition, 0, Inlines.IMIN(F5, audiosize), 0)
        }

        /* Only allocation memory for redundancy if/when needed */
        redundant_audio_size = if (redundancy != 0) F5 * this.channels else 0
        redundant_audio = ShortArray(redundant_audio_size)

        /* 5 ms redundant frame for CELT->SILK*/
        if (redundancy != 0 && celt_to_silk != 0) {
            celt_dec.SetStartBand(0)
            celt_dec.celt_decode_with_ec(
                data, data_ptr + len, redundancy_bytes,
                redundant_audio, 0, F5, null, 0
            )
            redundant_rng = celt_dec.GetFinalRange()
        }

        /* MUST be after PLC */
        celt_dec.SetStartBand(start_band)

        if (mode != OpusMode.MODE_SILK_ONLY) {
            val celt_frame_size = Inlines.IMIN(F20, frame_size)
            /* Make sure to discard any previous CELT state */
            if (mode != this.prev_mode && this.prev_mode != OpusMode.MODE_AUTO && this.prev_mode != OpusMode.MODE_UNKNOWN && this.prev_redundancy == 0) {
                celt_dec.ResetState()
            }
            /* Decode CELT */
            celt_ret = celt_dec.celt_decode_with_ec(
                if (decode_fec != 0) null else data, data_ptr,
                len, pcm, pcm_ptr, celt_frame_size, dec, celt_accum
            )
        } else {
            if (celt_accum == 0) {
                i = pcm_ptr
                while (i < frame_size * this.channels + pcm_ptr) {
                    pcm!![i] = 0
                    i++
                }
            }
            /* For hybrid -> SILK transitions, we let the CELT MDCT
               do a fade-out by decoding a silence frame */
            if (this.prev_mode == OpusMode.MODE_HYBRID && !(redundancy != 0 && celt_to_silk != 0 && this.prev_redundancy != 0)) {
                celt_dec.SetStartBand(0)
                celt_dec.celt_decode_with_ec(SILENCE, 0, 2, pcm, pcm_ptr, F2_5, null, celt_accum)
            }
        }

        if (mode != OpusMode.MODE_CELT_ONLY && celt_accum == 0) {
            i = 0
            while (i < frame_size * this.channels) {
                pcm!![pcm_ptr + i] = Inlines.SAT16(Inlines.ADD32(pcm!![pcm_ptr + i].toInt(), pcm_silk[i].toInt()))
                i++
            }
        }

        window = celt_dec.GetMode()!!.window

        /* 5 ms redundant frame for SILK->CELT */
        if (redundancy != 0 && celt_to_silk == 0) {
            celt_dec.ResetState()
            celt_dec.SetStartBand(0)

            celt_dec.celt_decode_with_ec(data, data_ptr + len, redundancy_bytes, redundant_audio, 0, F5, null, 0)
            redundant_rng = celt_dec.GetFinalRange()
            CodecHelpers.smooth_fade(
				pcm!!, pcm_ptr + this.channels * (frame_size - F2_5), redundant_audio, this.channels * F2_5,
                pcm, pcm_ptr + this.channels * (frame_size - F2_5), F2_5, this.channels, window!!, this.sampleRate
            )
        }
        if (redundancy != 0 && celt_to_silk != 0) {
            c = 0
            while (c < this.channels) {
                i = 0
                while (i < F2_5) {
                    pcm!![this.channels * i + c + pcm_ptr] = redundant_audio[this.channels * i + c]
                    i++
                }
                c++
            }
            CodecHelpers.smooth_fade(
                redundant_audio, this.channels * F2_5, pcm!!, pcm_ptr + this.channels * F2_5,
				pcm!!, pcm_ptr + this.channels * F2_5, F2_5, this.channels, window!!, this.sampleRate
            )
        }
        if (transition != 0) {
            if (audiosize >= F5) {
                i = 0
                while (i < this.channels * F2_5) {
                    pcm!![i] = pcm_transition!![i]
                    i++
                }
                CodecHelpers.smooth_fade(
					pcm_transition!!, this.channels * F2_5, pcm!!, pcm_ptr + this.channels * F2_5,
					pcm!!, pcm_ptr + this.channels * F2_5, F2_5,
                    this.channels, window!!, this.sampleRate
                )
            } else {
                /* Not enough time to do a clean transition, but we do it anyway
                   This will not preserve amplitude perfectly and may introduce
                   a bit of temporal aliasing, but it shouldn't be too bad and
                   that's pretty much the best we can do. In any case, generating this
                   transition is pretty silly in the first place */
                CodecHelpers.smooth_fade(
					pcm_transition!!, 0, pcm!!, pcm_ptr,
					pcm!!, pcm_ptr, F2_5,
                    this.channels, window!!, this.sampleRate
                )
            }
        }

        if (this.decode_gain != 0) {
            val gain: Int
            gain = Inlines.celt_exp2(
                Inlines.MULT16_16_P15(
                    (0.5 + 6.48814081e-4f * (1 shl 25)).toShort().toInt()/*Inlines.QCONST16(6.48814081e-4f, 25)*/,
                    this.decode_gain
                )
            )
            i = pcm_ptr
            while (i < pcm_ptr + frame_size * this.channels) {
                val x: Int
                x = Inlines.MULT16_32_P16(pcm!![i], gain)
                pcm[i] = Inlines.SATURATE(x, 32767).toShort()
                i++
            }
        }

        if (len <= 1) {
            this.finalRange = 0
        } else {
            this.finalRange = dec.rng.toInt() xor redundant_rng
        }

        this.prev_mode = mode
        this.prev_redundancy = if (redundancy != 0 && celt_to_silk == 0) 1 else 0

        return if (celt_ret < 0) celt_ret else audiosize
    }

    internal fun opus_decode_native(
        data: ByteArray?, data_ptr: Int,
        len: Int, pcm_out: ShortArray, pcm_out_ptr: Int, frame_size: Int, decode_fec: Int,
        self_delimited: Int, packet_offset: BoxedValueInt, soft_clip: Int
    ): Int {
        var data_ptr = data_ptr
        var i: Int
        var nb_samples: Int
        val count: Int
        val offset: Int
        val packet_frame_size: Int
        val packet_stream_channels: Int
        packet_offset.Val = 0
        val packet_bandwidth: OpusBandwidth
        val packet_mode: OpusMode
        /* 48 x 2.5 ms = 120 ms */
        // fixme: make sure these values can fit in an int16
        val size = ShortArray(48)
        if (decode_fec < 0 || decode_fec > 1) {
            return OpusError.OPUS_BAD_ARG
        }
        /* For FEC/PLC, frame_size has to be to have a multiple of 2.5 ms */
        if ((decode_fec != 0 || len == 0 || data == null) && frame_size % (this.sampleRate / 400) != 0) {
            return OpusError.OPUS_BAD_ARG
        }
        if (len == 0 || data == null) {
            var pcm_count = 0
            do {
                val ret: Int
                ret = opus_decode_frame(
                    null,
                    0,
                    0,
                    pcm_out,
                    pcm_out_ptr + pcm_count * this.channels,
                    frame_size - pcm_count,
                    0
                )
                if (ret < 0) {
                    return ret
                }
                pcm_count += ret
            } while (pcm_count < frame_size)
            Inlines.OpusAssert(pcm_count == frame_size)
            this.lastPacketDuration = pcm_count
            return pcm_count
        } else if (len < 0) {
            return OpusError.OPUS_BAD_ARG
        }

        packet_mode = OpusPacketInfo.getEncoderMode(data, data_ptr)
        packet_bandwidth = OpusPacketInfo.getBandwidth(data, data_ptr)
        packet_frame_size = OpusPacketInfo.getNumSamplesPerFrame(data, data_ptr, this.sampleRate)
        packet_stream_channels = OpusPacketInfo.getNumEncodedChannels(data, data_ptr)

        val boxed_toc = BoxedValueByte(0.toByte())
        val boxed_offset = BoxedValueInt(0)
        count = OpusPacketInfo.opus_packet_parse_impl(
            data, data_ptr, len, self_delimited, boxed_toc, null, 0,
            size, 0, boxed_offset, packet_offset
        )
        offset = boxed_offset.Val

        if (count < 0) {
            return count
        }

        data_ptr += offset

        if (decode_fec != 0) {
            val dummy = BoxedValueInt(0)
            val duration_copy: Int
            var ret: Int
            /* If no FEC can be present, run the PLC (recursive call) */
            if (frame_size < packet_frame_size || packet_mode == OpusMode.MODE_CELT_ONLY || this.mode == OpusMode.MODE_CELT_ONLY) {
                return opus_decode_native(null, 0, 0, pcm_out, pcm_out_ptr, frame_size, 0, 0, dummy, soft_clip)
            }
            /* Otherwise, run the PLC on everything except the size for which we might have FEC */
            duration_copy = this.lastPacketDuration
            if (frame_size - packet_frame_size != 0) {
                ret = opus_decode_native(
                    null,
                    0,
                    0,
                    pcm_out,
                    pcm_out_ptr,
                    frame_size - packet_frame_size,
                    0,
                    0,
                    dummy,
                    soft_clip
                )
                if (ret < 0) {
                    this.lastPacketDuration = duration_copy
                    return ret
                }
                Inlines.OpusAssert(ret == frame_size - packet_frame_size)
            }
            /* Complete with FEC */
            this.mode = packet_mode
            this.bandwidth = packet_bandwidth
            this.frame_size = packet_frame_size
            this.stream_channels = packet_stream_channels
            ret = opus_decode_frame(
                data,
                data_ptr,
                size[0].toInt(),
                pcm_out,
                pcm_out_ptr + this.channels * (frame_size - packet_frame_size),
                packet_frame_size,
                1
            )
            if (ret < 0) {
                return ret
            } else {
                this.lastPacketDuration = frame_size
                return frame_size
            }
        }

        if (count * packet_frame_size > frame_size) {
            return OpusError.OPUS_BUFFER_TOO_SMALL
        }

        /* Update the state as the last step to avoid updating it on an invalid packet */
        this.mode = packet_mode
        this.bandwidth = packet_bandwidth
        this.frame_size = packet_frame_size
        this.stream_channels = packet_stream_channels

        nb_samples = 0
        i = 0
        while (i < count) {
            val ret: Int
            ret = opus_decode_frame(
                data,
                data_ptr,
                size[i].toInt(),
                pcm_out,
                pcm_out_ptr + nb_samples * this.channels,
                frame_size - nb_samples,
                0
            )
            if (ret < 0) {
                return ret
            }
            Inlines.OpusAssert(ret == packet_frame_size)
            data_ptr += size[i].toInt()
            nb_samples += ret
            i++
        }
        this.lastPacketDuration = nb_samples

        return nb_samples
    }

    /// <summary>
    /// Decodes an Opus packet.
    /// </summary>
    /// <param name="in_data"></param>
    /// <param name="in_data_offset"></param>
    /// <param name="len"></param>
    /// <param name="out_pcm">
    ///
    /// exact sizing.</param>
    /// <param name="out_pcm_offset"></param>
    /// <param name="frame_size"></param>
    /// <param name="decode_fec">Flag to request that any in-band forward error correction data be
    /// decoded. If no such data is available, the frame is decoded as if it were lost.</param>
    /// <returns>The number of decoded samples</returns>
    /**
     * Decodes an Opus packet.
     * @param in_data The input payload. This may be NULL if that previous packet was lost in transit (when PLC is enabled)
     * @param in_data_offset The offset to use when reading the input payload. Usually 0
     * @param len The number of bytes in the payload (the packet size)
     * @param out_pcm A buffer to put the output PCM, in a short array. The output size is (# of samples) * (# of channels).
     * You can use the OpusPacketInfo helpers to get a hint of the frame size before you decode the packet if you need exact sizing.
     * @param out_pcm_offset The offset to use when writing to the output buffer
     * @param frame_size The number of samples (per channel) of available space in the output PCM buf.
     * If this is less than the maximum packet duration (120ms; 5760 for 48khz), this function will
     * not be capable of decoding some packets. In the case of PLC (data == NULL) or FEC (decode_fec == true),
     * then frame_size needs to be exactly the duration of the audio that is missing, otherwise the decoder will
     * not be in an optimal state to decode the next incoming packet. For the PLC and FEC cases, frame_size *must*
     * be a multiple of 2.5 ms.
     * @param decode_fec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data from THIS packet. Using this packet
     * recovery scheme, you will actually decode this packet twice, first with decode_fec TRUE and then again with FALSE. If FEC data is not
     * available in this packet, the decoder will simply generate a best-effort recreation of the lost packet. In that case,
     * the length of frame_size must be EXACTLY the length of the audio that was lost, or else the decoder will be in an inconsistent state.
     * @return The number of decoded samples (per channel)
     * @throws OpusException
     */
    fun decode(
        in_data: ByteArray, in_data_offset: Int,
        len: Int, out_pcm: ShortArray, out_pcm_offset: Int, frame_size: Int, decode_fec: Boolean
    ): Int {
        if (frame_size <= 0) {
            throw IllegalArgumentException("Frame size must be > 0")
        }

        //try {
            val dummy = BoxedValueInt(0)
            val ret = opus_decode_native(
                in_data,
                in_data_offset,
                len,
                out_pcm,
                out_pcm_offset,
                frame_size,
                if (decode_fec) 1 else 0,
                0,
                dummy,
                0
            )

            if (ret < 0) {
                // An error happened; report it
                if (ret == OpusError.OPUS_BAD_ARG) {
                    throw IllegalArgumentException("OPUS_BAD_ARG while decoding")
                }
                throw OpusException("An error occurred during decoding", ret)
            }

            return ret
        //} catch (e: Throwable) {
        //    throw OpusException("Internal error during decoding: " + e.message)
        //}

    }

    /**
     * Decodes an Opus packet.
     * @param in_data The input payload. This may be NULL if that previous packet was lost in transit (when PLC is enabled)
     * @param in_data_offset The offset to use when reading the input payload. Usually 0
     * @param len The number of bytes in the payload (the packet size)
     * @param out_pcm A buffer to put the output PCM, in a byte array. The output size is (# of samples) * (# of channels) * 2.
     * You can use the OpusPacketInfo helpers to get a hint of the frame size before you decode the packet if you need exact sizing.
     * @param out_pcm_offset The offset to use when writing to the output buffer
     * @param frame_size The number of samples (per channel) of available space in the output PCM buf.
     * If this is less than the maximum packet duration (120ms; 5760 for 48khz), this function will
     * not be capable of decoding some packets. In the case of PLC (data == NULL) or FEC (decode_fec == true),
     * then frame_size needs to be exactly the duration of the audio that is missing, otherwise the decoder will
     * not be in an optimal state to decode the next incoming packet. For the PLC and FEC cases, frame_size *must*
     * be a multiple of 2.5 ms.
     * @param decode_fec Indicates that we want to recreate the PREVIOUS (lost) packet using FEC data from THIS packet. Using this packet
     * recovery scheme, you will actually decode this packet twice, first with decode_fec TRUE and then again with FALSE. If FEC data is not
     * available in this packet, the decoder will simply generate a best-effort recreation of the lost packet. In that case, the
     * length of frame_size must be EXACTLY the length of the audio that was lost, or else the decoder will be in an inconsistent state.
     * @return The number of decoded samples (per channel)
     * @throws OpusException
     */
    fun decode(
        in_data: ByteArray, in_data_offset: Int, len: Int, out_pcm: ByteArray,
        out_pcm_offset: Int, frame_size: Int, decode_fec: Boolean
    ): Int {
        val spcm = ShortArray(min(frame_size, 5760) * channels)
        val decSamples = decode(in_data, in_data_offset, len, spcm, 0, frame_size, decode_fec)
        //Convert short array to byte array
        var c = 0
        var idx = out_pcm_offset
        while (c < spcm.size) {
            out_pcm[idx++] = (spcm[c] and 0xff).toByte()
            out_pcm[idx++] = (spcm[c] shr 8 and 0xff).toByte()
            c++
        }
        return decSamples
    }

    fun resetState() {
        partialReset()
        Celt_Decoder.ResetState()
        DecodeAPI.silk_InitDecoder(SilkDecoder)
        stream_channels = channels
        frame_size = sampleRate / 400
    }

    companion object {

        private val SILENCE = byteArrayOf(-1, -1)
    }
}
