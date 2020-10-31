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

class OpusMSEncoder private constructor(nb_streams: Int, nb_coupled_streams: Int) {
	init {
		if (nb_streams < 1 || nb_coupled_streams > nb_streams || nb_coupled_streams < 0) {
			throw IllegalArgumentException("Invalid channel count in MS encoder")
		}
	}

    internal val layout = ChannelLayout()
    internal var lfe_stream = 0
    internal var application = OpusApplication.OPUS_APPLICATION_AUDIO
    var expertFrameDuration = OpusFramesize.OPUS_FRAMESIZE_UNKNOWN
    internal var surround = 0
    internal var bitrate_bps = 0
    internal val subframe_mem = FloatArray(3)
    internal var encoders: Array<OpusEncoder> = Array(nb_streams) { OpusEncoder() }

	// fixme is this nb_streams or nb_channels?
	internal var window_mem: IntArray = IntArray(nb_streams * 120)
	internal var preemph_mem: IntArray = IntArray(nb_streams)

    /* Max size in case the encoder decides to return three frames */
    private val MS_FRAME_TMP = 3 * 1275 + 7

    var bitrate: Int
        get() {
            var s: Int
            var value = 0
            var encoder_ptr = 0
            s = 0
            while (s < layout.nb_streams) {
                val enc = encoders!![encoder_ptr++]
                value += enc.bitrate
                s++
            }
            return value
        }
        set(value) {
            if (value < 0 && value != OpusConstants.OPUS_AUTO && value != OpusConstants.OPUS_BITRATE_MAX) {
                throw IllegalArgumentException("Invalid bitrate")
            }
            bitrate_bps = value
        }

    var forceChannels: Int
        get() = encoders!![0].forceChannels
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].forceChannels = value
            }
        }

    var maxBandwidth: OpusBandwidth
        get() = encoders!![0].maxBandwidth
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].maxBandwidth = value
            }
        }

    var bandwidth: OpusBandwidth
        get() = encoders!![0].getBandwidth()
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].setBandwidth(value)
            }
        }

    var useDTX: Boolean
        get() = encoders!![0].useDTX
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].useDTX = value
            }
        }

    var complexity: Int
        get() = encoders!![0].complexity
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].complexity = value
            }
        }

    var forceMode: OpusMode
        get() = encoders!![0].forceMode
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].forceMode = value
            }
        }

    var useInbandFEC: Boolean
        get() = encoders!![0].useInbandFEC
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].useInbandFEC = value
            }
        }

    var packetLossPercent: Int
        get() = encoders!![0].packetLossPercent
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].packetLossPercent = value
            }
        }

    var useVBR: Boolean
        get() = encoders!![0].useVBR
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].useVBR = value
            }
        }

    var useConstrainedVBR: Boolean
        get() = encoders!![0].useConstrainedVBR
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].useConstrainedVBR = value
            }
        }

    var signalType: OpusSignal
        get() = encoders!![0].signalType
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].signalType = value
            }
        }

    val lookahead: Int
        get() = encoders!![0].lookahead

    val sampleRate: Int
        get() = encoders!![0].sampleRate

    val finalRange: Int
        get() {
            var s: Int
            var value = 0
            var encoder_ptr = 0
            s = 0
            while (s < layout.nb_streams) {
                value = value xor encoders!![encoder_ptr++].finalRange
                s++
            }
            return value
        }

    var lsbDepth: Int
        get() = encoders!![0].lsbDepth
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].lsbDepth = value
            }
        }

    var predictionDisabled: Boolean
        get() = encoders!![0].predictionDisabled
        set(value) {
            for (encoder_ptr in 0 until layout.nb_streams) {
                encoders!![encoder_ptr].predictionDisabled = value
            }
        }

    fun resetState() {
        var s: Int
        subframe_mem[2] = 0f
        subframe_mem[1] = subframe_mem[2]
        subframe_mem[0] = subframe_mem[1]
        if (surround != 0) {
            Arrays.MemSet(preemph_mem!!, 0, layout.nb_channels)
            Arrays.MemSet(window_mem!!, 0, layout.nb_channels * 120)
        }
        var encoder_ptr = 0
        s = 0
        while (s < layout.nb_streams) {
            val enc = encoders!![encoder_ptr++]
            enc.resetState()
            s++
        }
    }

    internal fun opus_multistream_encoder_init(
        Fs: Int,
        channels: Int,
        streams: Int,
        coupled_streams: Int,
        mapping: ShortArray,
        application: OpusApplication,
        surround: Int
    ): Int {
        var i: Int
        var ret: Int
        var encoder_ptr: Int

        if (channels > 255 || channels < 1 || coupled_streams > streams
            || streams < 1 || coupled_streams < 0 || streams > 255 - coupled_streams
        ) {
            return OpusError.OPUS_BAD_ARG
        }

        this.layout.nb_channels = channels
        this.layout.nb_streams = streams
        this.layout.nb_coupled_streams = coupled_streams
        this.subframe_mem[2] = 0f
        this.subframe_mem[1] = this.subframe_mem[2]
        this.subframe_mem[0] = this.subframe_mem[1]
        if (surround == 0) {
            this.lfe_stream = -1
        }
        this.bitrate_bps = OpusConstants.OPUS_AUTO
        this.application = application
        this.expertFrameDuration = OpusFramesize.OPUS_FRAMESIZE_ARG
        i = 0
        while (i < this.layout.nb_channels) {
            this.layout.mapping[i] = mapping[i]
            i++
        }
        if (OpusMultistream.validate_layout(this.layout) == 0 || validate_encoder_layout(this.layout) == 0) {
            return OpusError.OPUS_BAD_ARG
        }

        encoder_ptr = 0

        i = 0
        while (i < this.layout.nb_coupled_streams) {
            ret = this.encoders!![encoder_ptr].opus_init_encoder(Fs, 2, application)
            if (ret != OpusError.OPUS_OK) {
                return ret
            }
            if (i == this.lfe_stream) {
                this.encoders!![encoder_ptr].isLFE = true
            }
            encoder_ptr += 1
            i++
        }
        while (i < this.layout.nb_streams) {
            ret = this.encoders!![encoder_ptr].opus_init_encoder(Fs, 1, application)
            if (i == this.lfe_stream) {
                this.encoders!![encoder_ptr].isLFE = true
            }
            if (ret != OpusError.OPUS_OK) {
                return ret
            }
            encoder_ptr += 1
            i++
        }
        if (surround != 0) {
            Arrays.MemSet(preemph_mem!!, 0, channels)
            Arrays.MemSet(window_mem!!, 0, channels * 120)
        }
        this.surround = surround
        return OpusError.OPUS_OK
    }

    internal fun opus_multistream_surround_encoder_init(
        Fs: Int,
        channels: Int,
        mapping_family: Int,
        streams: BoxedValueInt,
        coupled_streams: BoxedValueInt,
        mapping: ShortArray,
        application: OpusApplication
    ): Int {
        streams.Val = 0
        coupled_streams.Val = 0
        if (channels > 255 || channels < 1) {
            return OpusError.OPUS_BAD_ARG
        }
        this.lfe_stream = -1
        if (mapping_family == 0) {
            if (channels == 1) {
                streams.Val = 1
                coupled_streams.Val = 0
                mapping[0] = 0
            } else if (channels == 2) {
                streams.Val = 1
                coupled_streams.Val = 1
                mapping[0] = 0
                mapping[1] = 1
            } else {
                return OpusError.OPUS_UNIMPLEMENTED
            }
        } else if (mapping_family == 1 && channels <= 8 && channels >= 1) {
            var i: Int
            streams.Val = VorbisLayout.vorbis_mappings[channels - 1].nb_streams
            coupled_streams.Val = VorbisLayout.vorbis_mappings[channels - 1].nb_coupled_streams
            i = 0
            while (i < channels) {
                mapping[i] = VorbisLayout.vorbis_mappings[channels - 1].mapping[i]
                i++
            }
            if (channels >= 6) {
                this.lfe_stream = streams.Val - 1
            }
        } else if (mapping_family == 255) {
            var i: Byte
            streams.Val = channels
            coupled_streams.Val = 0
            i = 0
            while (i < channels) {
                mapping[i.toInt()] = i.toShort()
                i++
            }
        } else {
            return OpusError.OPUS_UNIMPLEMENTED
        }
        return opus_multistream_encoder_init(
            Fs, channels, streams.Val, coupled_streams.Val,
            mapping, application, if (channels > 2 && mapping_family == 1) 1 else 0
        )
    }

    internal fun surround_rate_allocation(
        out_rates: IntArray,
        frame_size: Int
    ): Int {
        var i: Int
        val channel_rate: Int
        val Fs: Int
        val ptr: OpusEncoder
        var stream_offset: Int
        val lfe_offset: Int
        val coupled_ratio: Int
        /* Q8 */
        val lfe_ratio: Int
        /* Q8 */
        var rate_sum = 0

        ptr = this.encoders!![0]
        Fs = ptr.sampleRate

        if (this.bitrate_bps > this.layout.nb_channels * 40000) {
            stream_offset = 20000
        } else {
            stream_offset = this.bitrate_bps / this.layout.nb_channels / 2
        }
        stream_offset += 60 * (Fs / frame_size - 50)
        /* We start by giving each stream (coupled or uncoupled) the same bitrate.
           This models the main saving of coupled channels over uncoupled. */
        /* The LFE stream is an exception to the above and gets fewer bits. */
        lfe_offset = 3500 + 60 * (Fs / frame_size - 50)
        /* Coupled streams get twice the mono rate after the first 20 kb/s. */
        coupled_ratio = 512
        /* Should depend on the bitrate, for now we assume LFE gets 1/8 the bits of mono */
        lfe_ratio = 32

        /* Compute bitrate allocation between streams */
        if (this.bitrate_bps == OpusConstants.OPUS_AUTO) {
            channel_rate = Fs + 60 * Fs / frame_size
        } else if (this.bitrate_bps == OpusConstants.OPUS_BITRATE_MAX) {
            channel_rate = 300000
        } else {
            val nb_lfe: Int
            val nb_uncoupled: Int
            val nb_coupled: Int
            val total: Int
            nb_lfe = if (this.lfe_stream != -1) 1 else 0
            nb_coupled = this.layout.nb_coupled_streams
            nb_uncoupled = this.layout.nb_streams - nb_coupled - nb_lfe
            total = ((nb_uncoupled shl 8) /* mono */
                    + coupled_ratio * nb_coupled /* stereo */
                    + nb_lfe * lfe_ratio)
            channel_rate = 256 *
                    (this.bitrate_bps - lfe_offset * nb_lfe - stream_offset * (nb_coupled + nb_uncoupled)) / total
        }

        i = 0
        while (i < this.layout.nb_streams) {
            if (i < this.layout.nb_coupled_streams) {
                out_rates[i] = stream_offset + (channel_rate * coupled_ratio shr 8)
            } else if (i != this.lfe_stream) {
                out_rates[i] = stream_offset + channel_rate
            } else {
                out_rates[i] = lfe_offset + (channel_rate * lfe_ratio shr 8)
            }
            out_rates[i] = Inlines.IMAX(out_rates[i], 500)
            rate_sum += out_rates[i]
            i++
        }
        return rate_sum
    }

    internal fun opus_multistream_encode_native(
        pcm: ShortArray,
        pcm_ptr: Int,
        analysis_frame_size: Int,
        data: ByteArray,
        data_ptr: Int,
        max_data_bytes: Int,
        lsb_depth: Int,
        float_api: Int
    ): Int {
        var data_ptr = data_ptr
        var max_data_bytes = max_data_bytes
        val Fs: Int
        var s: Int
        var encoder_ptr: Int
        var tot_size: Int
        val buf: ShortArray
        val bandSMR: IntArray
        val tmp_data = ByteArray(MS_FRAME_TMP)
        val rp = OpusRepacketizer()
        val vbr: Int
        val celt_mode: CeltMode?
        val bitrates = IntArray(256)
        val bandLogE = IntArray(42)
        var mem: IntArray? = null
        var preemph_mem: IntArray? = null
        var frame_size: Int = 0
        val rate_sum: Int
        val smallest_packet: Int

        if (this.surround != 0) {
            preemph_mem = this.preemph_mem
            mem = this.window_mem
        }

        encoder_ptr = 0
        Fs = this.encoders!![encoder_ptr].sampleRate
        vbr = if (this.encoders!![encoder_ptr].useVBR) 1 else 0
        celt_mode = this.encoders!![encoder_ptr].GetCeltMode()

        run {
            var delay_compensation: Int
            val channels: Int

            channels = this.layout.nb_streams + this.layout.nb_coupled_streams
            delay_compensation = this.encoders!![encoder_ptr].lookahead
            delay_compensation -= Fs / 400
            frame_size = CodecHelpers.compute_frame_size(
                pcm, pcm_ptr, analysis_frame_size,
                this.expertFrameDuration, channels, Fs, this.bitrate_bps,
                delay_compensation, this.subframe_mem, this.encoders!![encoder_ptr].analysis.enabled
            )
        }

        if (400 * frame_size < Fs) {
            return OpusError.OPUS_BAD_ARG
        }
        /* Validate frame_size before using it to allocate stack space.
           This mirrors the checks in opus_encode[_float](). */
        if (400 * frame_size != Fs && 200 * frame_size != Fs
            && 100 * frame_size != Fs && 50 * frame_size != Fs
            && 25 * frame_size != Fs && 50 * frame_size != 3 * Fs
        ) {
            return OpusError.OPUS_BAD_ARG
        }

        /* Smallest packet the encoder can produce. */
        smallest_packet = this.layout.nb_streams * 2 - 1
        if (max_data_bytes < smallest_packet) {
            return OpusError.OPUS_BUFFER_TOO_SMALL
        }
        buf = ShortArray(2 * frame_size)

        bandSMR = IntArray(21 * this.layout.nb_channels)
        if (this.surround != 0) {
            surround_analysis(
                celt_mode!!,
                pcm,
                pcm_ptr,
                bandSMR,
                mem,
                preemph_mem,
                frame_size,
                120,
                this.layout.nb_channels,
                Fs
            )
        }

        /* Compute bitrate allocation between streams (this could be a lot better) */
        rate_sum = surround_rate_allocation(bitrates, frame_size)

        if (vbr == 0) {
            if (this.bitrate_bps == OpusConstants.OPUS_AUTO) {
                max_data_bytes = Inlines.IMIN(max_data_bytes, 3 * rate_sum / (3 * 8 * Fs / frame_size))
            } else if (this.bitrate_bps != OpusConstants.OPUS_BITRATE_MAX) {
                max_data_bytes = Inlines.IMIN(
                    max_data_bytes, Inlines.IMAX(
                        smallest_packet,
                        3 * this.bitrate_bps / (3 * 8 * Fs / frame_size)
                    )
                )
            }
        }

        s = 0
        while (s < this.layout.nb_streams) {
            val enc = this.encoders!![encoder_ptr]
            encoder_ptr += 1
            enc.bitrate = bitrates[s]
            if (this.surround != 0) {
                var equiv_rate: Int
                equiv_rate = this.bitrate_bps
                if (frame_size * 50 < Fs) {
                    equiv_rate -= 60 * (Fs / frame_size - 50) * this.layout.nb_channels
                }
                if (equiv_rate > 10000 * this.layout.nb_channels) {
                    enc.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_FULLBAND)
                } else if (equiv_rate > 7000 * this.layout.nb_channels) {
                    enc.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND)
                } else if (equiv_rate > 5000 * this.layout.nb_channels) {
                    enc.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_WIDEBAND)
                } else {
                    enc.setBandwidth(OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND)
                }
                if (s < this.layout.nb_coupled_streams) {
                    /* To preserve the spatial image, force stereo CELT on coupled streams */
                    enc.forceMode = OpusMode.MODE_CELT_ONLY
                    enc.forceChannels = 2
                }
            }
            s++
        }

        encoder_ptr = 0
        /* Counting ToC */
        tot_size = 0
        s = 0
        while (s < this.layout.nb_streams) {
            val enc: OpusEncoder
            var len: Int
            var curr_max: Int
            val c1: Int
            val c2: Int

            rp.Reset()
            enc = this.encoders!![encoder_ptr]
            if (s < this.layout.nb_coupled_streams) {
                var i: Int
                val left: Int
                val right: Int
                left = OpusMultistream.get_left_channel(this.layout, s, -1)
                right = OpusMultistream.get_right_channel(this.layout, s, -1)
                opus_copy_channel_in_short(
                    buf, 0, 2,
                    pcm, pcm_ptr, this.layout.nb_channels, left, frame_size
                )
                opus_copy_channel_in_short(
                    buf, 1, 2,
                    pcm, pcm_ptr, this.layout.nb_channels, right, frame_size
                )
                encoder_ptr += 1
                if (this.surround != 0) {
                    i = 0
                    while (i < 21) {
                        bandLogE[i] = bandSMR[21 * left + i]
                        bandLogE[21 + i] = bandSMR[21 * right + i]
                        i++
                    }
                }
                c1 = left
                c2 = right
            } else {
                var i: Int
                val chan = OpusMultistream.get_mono_channel(this.layout, s, -1)
                opus_copy_channel_in_short(
                    buf, 0, 1,
                    pcm, pcm_ptr, this.layout.nb_channels, chan, frame_size
                )
                encoder_ptr += 1
                if (this.surround != 0) {
                    i = 0
                    while (i < 21) {
                        bandLogE[i] = bandSMR[21 * chan + i]
                        i++
                    }
                }
                c1 = chan
                c2 = -1
            }
            if (this.surround != 0) {
                enc.SetEnergyMask(bandLogE)
            }

            /* number of bytes left (+Toc) */
            curr_max = max_data_bytes - tot_size
            /* Reserve one byte for the last stream and two for the others */
            curr_max -= Inlines.IMAX(0, 2 * (this.layout.nb_streams - s - 1) - 1)
            curr_max = Inlines.IMIN(curr_max, MS_FRAME_TMP)
            /* Repacketizer will add one or two bytes for self-delimited frames */
            if (s != this.layout.nb_streams - 1) {
                curr_max -= if (curr_max > 253) 2 else 1
            }
            if (vbr == 0 && s == this.layout.nb_streams - 1) {
                enc.bitrate = curr_max * (8 * Fs / frame_size)
            }
            len = enc.opus_encode_native(
                buf, 0, frame_size, tmp_data, 0, curr_max, lsb_depth,
                pcm, pcm_ptr, analysis_frame_size, c1, c2, this.layout.nb_channels, float_api
            )
            if (len < 0) {
                return len
            }
            /* We need to use the repacketizer to add the self-delimiting lengths
               while taking into account the fact that the encoder can now return
               more than one frame at a time (e.g. 60 ms CELT-only) */
            rp.addPacket(tmp_data, 0, len)
            len = rp.opus_repacketizer_out_range_impl(
                0,
                rp.numFrames,
                data,
                data_ptr,
                max_data_bytes - tot_size,
                if (s != this.layout.nb_streams - 1) 1 else 0,
                if (vbr == 0 && s == this.layout.nb_streams - 1) 1 else 0
            )
            data_ptr += len
            tot_size += len
            s++
        }

        return tot_size
    }

    fun encodeMultistream(
        pcm: ShortArray,
        pcm_offset: Int,
        frame_size: Int,
        outputBuffer: ByteArray,
        outputBuffer_offset: Int,
        max_data_bytes: Int
    ): Int {
        // todo: catch error codes here
        return opus_multistream_encode_native(
            pcm,
            pcm_offset,
            frame_size,
            outputBuffer,
            outputBuffer_offset,
            max_data_bytes,
            16,
            0
        )
    }

    fun getApplication(): OpusApplication {
        return encoders!![0].getApplication()
    }

    fun setApplication(value: OpusApplication) {
        for (encoder_ptr in 0 until layout.nb_streams) {
            encoders!![encoder_ptr].setApplication(value)
        }
    }

    fun getMultistreamEncoderState(streamId: Int): OpusEncoder {
        if (streamId >= layout.nb_streams) {
            throw IllegalArgumentException("Requested stream doesn't exist")
        }
        return encoders!![streamId]
    }

    companion object {

        internal fun validate_encoder_layout(layout: ChannelLayout): Int {
            var s: Int
            s = 0
            while (s < layout.nb_streams) {
                if (s < layout.nb_coupled_streams) {
                    if (OpusMultistream.get_left_channel(layout, s, -1) == -1) {
                        return 0
                    }
                    if (OpusMultistream.get_right_channel(layout, s, -1) == -1) {
                        return 0
                    }
                } else if (OpusMultistream.get_mono_channel(layout, s, -1) == -1) {
                    return 0
                }
                s++
            }
            return 1
        }

        internal fun channel_pos(channels: Int, pos: IntArray/*[8]*/) {
            /* Position in the mix: 0 don't mix, 1: left, 2: center, 3:right */
            if (channels == 4) {
                pos[0] = 1
                pos[1] = 3
                pos[2] = 1
                pos[3] = 3
            } else if (channels == 3 || channels == 5 || channels == 6) {
                pos[0] = 1
                pos[1] = 2
                pos[2] = 3
                pos[3] = 1
                pos[4] = 3
                pos[5] = 0
            } else if (channels == 7) {
                pos[0] = 1
                pos[1] = 2
                pos[2] = 3
                pos[3] = 1
                pos[4] = 3
                pos[5] = 2
                pos[6] = 0
            } else if (channels == 8) {
                pos[0] = 1
                pos[1] = 2
                pos[2] = 3
                pos[3] = 1
                pos[4] = 3
                pos[5] = 1
                pos[6] = 3
                pos[7] = 0
            }
        }

        private val diff_table/*[17]*/ = intArrayOf(
            (0.5 + 0.5000000f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.5000000f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.2924813f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.2924813f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.1609640f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.1609640f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.0849625f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0849625f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.0437314f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0437314f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.0221971f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0221971f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.0111839f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0111839f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.0056136f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0056136f, CeltConstants.DB_SHIFT)*/,
            (0.5 + 0.0028123f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(0.0028123f, CeltConstants.DB_SHIFT)*/
        )

        /* Computes a rough approximation of log2(2^a + 2^b) */
        internal fun logSum(a: Int, b: Int): Int {
            val max: Int
            val diff: Int
            val frac: Int

            val low: Int
            if (a > b) {
                max = a
                diff = Inlines.SUB32(Inlines.EXTEND32(a), Inlines.EXTEND32(b))
            } else {
                max = b
                diff = Inlines.SUB32(Inlines.EXTEND32(b), Inlines.EXTEND32(a))
            }
            if (diff >= (0.5 + 8.0f * (1 shl CeltConstants.DB_SHIFT)).toShort())
            /* inverted to catch NaNs */ {
                return max
            }
            low = Inlines.SHR32(diff, CeltConstants.DB_SHIFT - 1)
            frac = Inlines.SHL16(diff - Inlines.SHL16(low, CeltConstants.DB_SHIFT - 1), 16 - CeltConstants.DB_SHIFT)
            return max + diff_table[low] + Inlines.MULT16_16_Q15(
                frac,
                Inlines.SUB16(diff_table[low + 1], diff_table[low])
            )
        }

        // fixme: test the perf of this alternate implementation
        //int logSum(int a, int b)
        //{
        //    return log2(pow(4, a) + pow(4, b)) / 2;
        //}
        internal fun surround_analysis(
            celt_mode: CeltMode, pcm: ShortArray, pcm_ptr: Int,
            bandLogE: IntArray, mem: IntArray?, preemph_mem: IntArray?,
            len: Int, overlap: Int, channels: Int, rate: Int
        ) {
            var c: Int
            var i: Int
            var LM: Int
            val pos = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
            val upsample: Int
            val frame_size: Int
            val channel_offset: Int
            val bandE = Arrays.InitTwoDimensionalArrayInt(1, 21)
            val maskLogE = Arrays.InitTwoDimensionalArrayInt(3, 21)
            val input: IntArray
            val x: ShortArray
            val freq: Array<IntArray>

            upsample = CeltCommon.resampling_factor(rate)
            frame_size = len * upsample

            LM = 0
            while (LM < celt_mode.maxLM) {
                if (celt_mode.shortMdctSize shl LM == frame_size) {
                    break
                }
                LM++
            }

            input = IntArray(frame_size + overlap)
            x = ShortArray(len)
            freq = Arrays.InitTwoDimensionalArrayInt(1, frame_size)

            channel_pos(channels, pos)

            c = 0
            while (c < 3) {
                i = 0
                while (i < 21) {
                    maskLogE[c][i] =
                            -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                    i++
                }
                c++
            }

            c = 0
            while (c < channels) {
                arraycopy(mem!!, c * overlap, input, 0, overlap)
                opus_copy_channel_in_short(x, 0, 1, pcm, pcm_ptr, channels, c, len)
                val boxed_preemph = BoxedValueInt(preemph_mem!![c])
                CeltCommon.celt_preemphasis(
                    x,
                    input,
                    overlap,
                    frame_size,
                    1,
                    upsample,
                    celt_mode.preemph,
                    boxed_preemph,
                    0
                )
                preemph_mem[c] = boxed_preemph.Val

                MDCT.clt_mdct_forward(
                    celt_mode.mdct,
                    input,
                    0,
                    freq[0],
                    0,
					celt_mode.window!!,
                    overlap,
                    celt_mode.maxLM - LM,
                    1
                )
                if (upsample != 1) {
                    i = 0
                    while (i < len) {
                        freq[0][i] *= upsample
                        i++
                    }
                    while (i < frame_size) {
                        freq[0][i] = 0
                        i++
                    }
                }

                Bands.compute_band_energies(celt_mode, freq, bandE, 21, 1, LM)
                QuantizeBands.amp2Log2(celt_mode, 21, 21, bandE[0], bandLogE, 21 * c, 1)
                /* Apply spreading function with -6 dB/band going up and -12 dB/band going down. */
                i = 1
                while (i < 21) {
                    bandLogE[21 * c + i] = Inlines.MAX16(
                        bandLogE[21 * c + i],
                        bandLogE[21 * c + i - 1] - (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(1.0f, CeltConstants.DB_SHIFT)*/
                    )
                    i++
                }
                i = 19
                while (i >= 0) {
                    bandLogE[21 * c + i] = Inlines.MAX16(
                        bandLogE[21 * c + i],
                        bandLogE[21 * c + i + 1] - (0.5 + 2.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(2.0f, CeltConstants.DB_SHIFT)*/
                    )
                    i--
                }
                if (pos[c] == 1) {
                    i = 0
                    while (i < 21) {
                        maskLogE[0][i] = logSum(maskLogE[0][i], bandLogE[21 * c + i])
                        i++
                    }
                } else if (pos[c] == 3) {
                    i = 0
                    while (i < 21) {
                        maskLogE[2][i] = logSum(maskLogE[2][i], bandLogE[21 * c + i])
                        i++
                    }
                } else if (pos[c] == 2) {
                    i = 0
                    while (i < 21) {
                        maskLogE[0][i] = logSum(
                            maskLogE[0][i],
                            bandLogE[21 * c + i] - (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/
                        )
                        maskLogE[2][i] = logSum(
                            maskLogE[2][i],
                            bandLogE[21 * c + i] - (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/
                        )
                        i++
                    }
                }
                arraycopy(input, frame_size, mem, c * overlap, overlap)
                c++
            }
            i = 0
            while (i < 21) {
                maskLogE[1][i] = Inlines.MIN32(maskLogE[0][i], maskLogE[2][i])
                i++
            }
            channel_offset =
                    Inlines.HALF16(Inlines.celt_log2((0.5 + 2.0f * (1 shl 14)).toInt()/*Inlines.QCONST32(2.0f, 14)*/ / (channels - 1)))
            c = 0
            while (c < 3) {
                i = 0
                while (i < 21) {
                    maskLogE[c][i] += channel_offset
                    i++
                }
                c++
            }

            c = 0
            while (c < channels) {
                val mask: IntArray
                if (pos[c] != 0) {
                    mask = maskLogE[pos[c] - 1]
                    i = 0
                    while (i < 21) {
                        bandLogE[21 * c + i] = bandLogE[21 * c + i] - mask[i]
                        i++
                    }
                } else {
                    i = 0
                    while (i < 21) {
                        bandLogE[21 * c + i] = 0
                        i++
                    }
                }
                c++
            }
        }

        fun Create(
            Fs: Int,
            channels: Int,
            streams: Int,
            coupled_streams: Int,
            mapping: ShortArray,
            application: OpusApplication
        ): OpusMSEncoder {
            val ret: Int
            val st: OpusMSEncoder
            if (channels > 255 || channels < 1 || coupled_streams > streams
                || streams < 1 || coupled_streams < 0 || streams > 255 - coupled_streams
            ) {
                throw IllegalArgumentException("Invalid channel / stream configuration")
            }
            st = OpusMSEncoder(streams, coupled_streams)
            ret = st.opus_multistream_encoder_init(Fs, channels, streams, coupled_streams, mapping, application, 0)
            if (ret != OpusError.OPUS_OK) {
                if (ret == OpusError.OPUS_BAD_ARG) {
                    throw IllegalArgumentException("OPUS_BAD_ARG when creating MS encoder")
                }
                throw OpusException("Could not create MS encoder", ret)
            }
            return st
        }

        internal fun GetStreamCount(
            channels: Int,
            mapping_family: Int,
            nb_streams: BoxedValueInt,
            nb_coupled_streams: BoxedValueInt
        ) {
            if (mapping_family == 0) {
                if (channels == 1) {
                    nb_streams.Val = 1
                    nb_coupled_streams.Val = 0
                } else if (channels == 2) {
                    nb_streams.Val = 1
                    nb_coupled_streams.Val = 1
                } else {
                    throw IllegalArgumentException("More than 2 channels requires custom mappings")
                }
            } else if (mapping_family == 1 && channels <= 8 && channels >= 1) {
                nb_streams.Val = VorbisLayout.vorbis_mappings[channels - 1].nb_streams
                nb_coupled_streams.Val = VorbisLayout.vorbis_mappings[channels - 1].nb_coupled_streams
            } else if (mapping_family == 255) {
                nb_streams.Val = channels
                nb_coupled_streams.Val = 0
            } else {
                throw IllegalArgumentException("Invalid mapping family")
            }
        }

        internal fun CreateSurround(
            Fs: Int,
            channels: Int,
            mapping_family: Int,
            streams: BoxedValueInt,
            coupled_streams: BoxedValueInt,
            mapping: ShortArray,
            application: OpusApplication
        ): OpusMSEncoder {
            val ret: Int
            val st: OpusMSEncoder
            if (channels > 255 || channels < 1 || application == OpusApplication.OPUS_APPLICATION_UNIMPLEMENTED) {
                throw IllegalArgumentException("Invalid channel count or application")
            }
            val nb_streams = BoxedValueInt(0)
            val nb_coupled_streams = BoxedValueInt(0)
            GetStreamCount(channels, mapping_family, nb_streams, nb_coupled_streams)

            st = OpusMSEncoder(nb_streams.Val, nb_coupled_streams.Val)
            ret = st.opus_multistream_surround_encoder_init(
                Fs,
                channels,
                mapping_family,
                streams,
                coupled_streams,
                mapping,
                application
            )
            if (ret != OpusError.OPUS_OK) {
                if (ret == OpusError.OPUS_BAD_ARG) {
                    throw IllegalArgumentException("Bad argument passed to CreateSurround")
                }
                throw OpusException("Could not create multistream encoder", ret)
            }
            return st
        }

        internal fun opus_copy_channel_in_short(
            dst: ShortArray,
            dst_ptr: Int,
            dst_stride: Int,
            src: ShortArray,
            src_ptr: Int,
            src_stride: Int,
            src_channel: Int,
            frame_size: Int
        ) {
            var i: Int
            i = 0
            while (i < frame_size) {
                dst[dst_ptr + i * dst_stride] = src[i * src_stride + src_channel + src_ptr]
                i++
            }
        }
    }
}
