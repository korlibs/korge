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

class OpusMSDecoder private constructor(nb_streams: Int, nb_coupled_streams: Int) {

    internal var layout = ChannelLayout()
    internal var decoders: Array<OpusDecoder>? = null

    val bandwidth: OpusBandwidth
        get() {
            if (decoders == null || decoders!!.size == 0) {
                throw IllegalStateException("Decoder not initialized")
            }
            return decoders!![0].bandwidth
        }

    val sampleRate: Int
        get() {
            if (decoders == null || decoders!!.size == 0) {
                throw IllegalStateException("Decoder not initialized")
            }
            return decoders!![0].sampleRate
        }

    var gain: Int
        get() {
            if (decoders == null || decoders!!.size == 0) {
                throw IllegalStateException("Decoder not initialized")
            }
            return decoders!![0].gain
        }
        set(value) {
            for (s in 0 until layout.nb_streams) {
                decoders!![s].gain = value
            }
        }

    val lastPacketDuration: Int
        get() = if (decoders == null || decoders!!.size == 0) {
            OpusError.OPUS_INVALID_STATE
        } else decoders!![0].lastPacketDuration

    val finalRange: Int
        get() {
            var value = 0
            for (s in 0 until layout.nb_streams) {
                value = value xor decoders!![s].finalRange
            }
            return value
        }

    init {
        decoders = Array(nb_streams) { OpusDecoder() }
    }

    internal fun opus_multistream_decoder_init(
        Fs: Int,
        channels: Int,
        streams: Int,
        coupled_streams: Int,
        mapping: ShortArray
    ): Int {
        var i: Int
        var ret: Int
        var decoder_ptr = 0

        if (channels > 255 || channels < 1 || coupled_streams > streams
            || streams < 1 || coupled_streams < 0 || streams > 255 - coupled_streams
        ) {
            throw IllegalArgumentException("Invalid channel or coupled stream count")
        }

        this.layout.nb_channels = channels
        this.layout.nb_streams = streams
        this.layout.nb_coupled_streams = coupled_streams

        i = 0
        while (i < this.layout.nb_channels) {
            this.layout.mapping[i] = mapping[i]
            i++
        }
        if (OpusMultistream.validate_layout(this.layout) == 0) {
            throw IllegalArgumentException("Invalid surround channel layout")
        }

        i = 0
        while (i < this.layout.nb_coupled_streams) {
            ret = this.decoders!![decoder_ptr].opus_decoder_init(Fs, 2)
            if (ret != OpusError.OPUS_OK) {
                return ret
            }
            decoder_ptr++
            i++
        }
        while (i < this.layout.nb_streams) {
            ret = this.decoders!![decoder_ptr].opus_decoder_init(Fs, 1)
            if (ret != OpusError.OPUS_OK) {
                return ret
            }
            decoder_ptr++
            i++
        }
        return OpusError.OPUS_OK
    }

    internal fun opus_multistream_decode_native(
        data: ByteArray,
        data_ptr: Int,
        len: Int,
        pcm: ShortArray,
        pcm_ptr: Int,
        frame_size: Int,
        decode_fec: Int,
        soft_clip: Int
    ): Int {
        var data_ptr = data_ptr
        var len = len
        var frame_size = frame_size
        val Fs: Int
        var s: Int
        var c: Int
        var decoder_ptr: Int
        var do_plc = 0
        val buf: ShortArray

        /* Limit frame_size to avoid excessive stack allocations. */
        Fs = this.sampleRate
        frame_size = Inlines.IMIN(frame_size, Fs / 25 * 3)
        buf = ShortArray(2 * frame_size)
        decoder_ptr = 0

        if (len == 0) {
            do_plc = 1
        }
        if (len < 0) {
            return OpusError.OPUS_BAD_ARG
        }
        if (do_plc == 0 && len < 2 * this.layout.nb_streams - 1) {
            return OpusError.OPUS_INVALID_PACKET
        }
        if (do_plc == 0) {
            val ret = opus_multistream_packet_validate(data, data_ptr, len, this.layout.nb_streams, Fs)
            if (ret < 0) {
                return ret
            } else if (ret > frame_size) {
                return OpusError.OPUS_BUFFER_TOO_SMALL
            }
        }
        s = 0
        while (s < this.layout.nb_streams) {
            val dec: OpusDecoder
            val ret: Int

            dec = this.decoders!![decoder_ptr++]

            if (do_plc == 0 && len <= 0) {
                return OpusError.OPUS_INTERNAL_ERROR
            }
            val packet_offset = BoxedValueInt(0)
            ret = dec.opus_decode_native(
                data, data_ptr, len, buf, 0, frame_size, decode_fec,
                if (s != this.layout.nb_streams - 1) 1 else 0, packet_offset, soft_clip
            )
            data_ptr += packet_offset.Val
            len -= packet_offset.Val
            if (ret <= 0) {
                return ret
            }
            frame_size = ret
            if (s < this.layout.nb_coupled_streams) {
                var chan: Int
                var prev: Int
                prev = -1
                /* Copy "left" audio to the channel(s) where it belongs */
                while (true) {
					chan = OpusMultistream.get_left_channel(this.layout, s, prev)
					if (chan == -1) break
                    opus_copy_channel_out_short(
                        pcm, pcm_ptr, this.layout.nb_channels, chan,
                        buf, 0, 2, frame_size
                    )
                    prev = chan
                }
                prev = -1
                /* Copy "right" audio to the channel(s) where it belongs */
                while (true) {
					chan = OpusMultistream.get_right_channel(this.layout, s, prev)
					if (chan == -1) break
                    opus_copy_channel_out_short(
                        pcm, pcm_ptr, this.layout.nb_channels, chan,
                        buf, 1, 2, frame_size
                    )
                    prev = chan
                }
            } else {
                var chan: Int
                var prev: Int
                prev = -1
                /* Copy audio to the channel(s) where it belongs */
                while (true) {
					chan = OpusMultistream.get_mono_channel(this.layout, s, prev)
					if (chan == -1) break
                    opus_copy_channel_out_short(
                        pcm, pcm_ptr, this.layout.nb_channels, chan,
                        buf, 0, 1, frame_size
                    )
                    prev = chan
                }
            }
            s++
        }
        /* Handle muted channels */
        c = 0
        while (c < this.layout.nb_channels) {
            if (this.layout.mapping[c].toInt() == 255) {
                opus_copy_channel_out_short(pcm, pcm_ptr, this.layout.nb_channels, c, null, 0, 0, frame_size)
            }
            c++
        }

        return frame_size
    }

    fun decodeMultistream(
        data: ByteArray,
        data_offset: Int,
        len: Int,
        out_pcm: ShortArray,
        out_pcm_offset: Int,
        frame_size: Int,
        decode_fec: Int
    ): Int {
        return opus_multistream_decode_native(
            data, data_offset, len,
            out_pcm, out_pcm_offset, frame_size, decode_fec, 0
        )
    }

    fun ResetState() {
        for (s in 0 until layout.nb_streams) {
            decoders!![s].resetState()
        }
    }

    fun GetMultistreamDecoderState(streamId: Int): OpusDecoder {
        return decoders!![streamId]
    }

    companion object {

        /// <summary>
        /// Creates a new MS decoder
        /// </summary>
        /// <param name="Fs"></param>
        /// <param name="channels"></param>
        /// <param name="streams"></param>
        /// <param name="coupled_streams"></param>
        /// <param name="mapping">A mapping family (just use { 0, 1, 255 })</param>
        /// <returns></returns>
        fun create(
            Fs: Int,
            channels: Int,
            streams: Int,
            coupled_streams: Int,
            mapping: ShortArray
        ): OpusMSDecoder {
            val ret: Int
            val st: OpusMSDecoder
            if (channels > 255 || channels < 1 || coupled_streams > streams
                || streams < 1 || coupled_streams < 0 || streams > 255 - coupled_streams
            ) {
                throw IllegalArgumentException("Invalid channel / stream configuration")
            }
            st = OpusMSDecoder(streams, coupled_streams)
            ret = st.opus_multistream_decoder_init(Fs, channels, streams, coupled_streams, mapping)
            if (ret != OpusError.OPUS_OK) {
                if (ret == OpusError.OPUS_BAD_ARG) {
                    throw IllegalArgumentException("Bad argument while creating MS decoder")
                }
                throw OpusException("Could not create MS decoder", ret)
            }
            return st
        }

        internal fun opus_multistream_packet_validate(
            data: ByteArray, data_ptr: Int,
            len: Int, nb_streams: Int, Fs: Int
        ): Int {
            var data_ptr = data_ptr
            var len = len
            var s: Int
            var count: Int
            val toc = BoxedValueByte(0.toByte())
            val size = ShortArray(48)
            var samples = 0
            val packet_offset = BoxedValueInt(0)
            val dummy = BoxedValueInt(0)

            s = 0
            while (s < nb_streams) {
                val tmp_samples: Int
                if (len <= 0) {
                    return OpusError.OPUS_INVALID_PACKET
                }

                count = OpusPacketInfo.opus_packet_parse_impl(
                    data, data_ptr, len, if (s != nb_streams - 1) 1 else 0, toc, null, 0,
                    size, 0, dummy, packet_offset
                )
                if (count < 0) {
                    return count
                }

                tmp_samples = OpusPacketInfo.getNumSamples(data, data_ptr, packet_offset.Val, Fs)
                if (s != 0 && samples != tmp_samples) {
                    return OpusError.OPUS_INVALID_PACKET
                }
                samples = tmp_samples
                data_ptr += packet_offset.Val
                len -= packet_offset.Val
                s++
            }

            return samples
        }

        internal fun opus_copy_channel_out_short(
            dst: ShortArray,
            dst_ptr: Int,
            dst_stride: Int,
            dst_channel: Int,
            src: ShortArray?,
            src_ptr: Int,
            src_stride: Int,
            frame_size: Int
        ) {
            var i: Int
            if (src != null) {
                i = 0
                while (i < frame_size) {
                    dst[i * dst_stride + dst_channel + dst_ptr] = src[i * src_stride + src_ptr]
                    i++
                }
            } else {
                i = 0
                while (i < frame_size) {
                    dst[i * dst_stride + dst_channel + dst_ptr] = 0
                    i++
                }
            }
        }
    }
}
