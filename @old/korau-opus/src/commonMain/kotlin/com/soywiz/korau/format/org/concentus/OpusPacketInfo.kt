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

class OpusPacketInfo private constructor(
    /**
     * The Table of Contents byte for this packet. Contains info about modes, frame length, etc.
     */
    var TOCByte: Byte,
    /**
     * The list of subframes in this packet
     */
    var Frames: List<Array<Byte>>,
    /**
     * The index of the start of the payload within the packet
     */
    var PayloadOffset: Int
) {
    companion object {

        /**
         * Parse an opus packet into a packetinfo object containing one or more frames.
         * Opus_decode will perform this operation internally so most applications do
         * not need to use this function.
         * @param packet The packet data to be parsed
         * @param packet_offset The index of the beginning of the packet in the data array (usually 0)
         * @param len The packet's length
         * @return A parsed packet info struct
         * @throws OpusException
         */
        fun parseOpusPacket(packet: ByteArray, packet_offset: Int, len: Int): OpusPacketInfo {
            // Find the number of frames first
            val numFrames = getNumFrames(packet, packet_offset, len)

            val payload_offset = BoxedValueInt(0)
            val out_toc = BoxedValueByte(0.toByte())
            val frames = Array<ByteArray>(numFrames) { byteArrayOf() }
            val size = ShortArray(numFrames)
            val packetOffset = BoxedValueInt(0)
            val error = opus_packet_parse_impl(
                packet,
                packet_offset,
                len,
                0,
                out_toc,
                frames as Array<ByteArray?>,
                0,
                size,
                0,
                payload_offset,
                packetOffset
            )
            if (error < 0) {
                throw OpusException("An error occurred while parsing the packet", error)
            }

            // Since packet_parse_impl has created deep copies of each frame, we can return them safely from this function without
            // worrying about variable scoping or side effects
            val copiedFrames = ArrayList<Array<Byte>>()
            for (c in frames.indices) {
                // Java does not let us unbox an array (?) so we have to copy the bytes _again
                val convertedFrame = Array<Byte>(frames[c].size) { frames[c]!![it] }
                copiedFrames.add(convertedFrame)
            }

            return OpusPacketInfo(out_toc.Val, copiedFrames, payload_offset.Val)
        }

        fun getNumSamplesPerFrame(packet: ByteArray, packet_offset: Int, Fs: Int): Int {
            var audiosize: Int
            if (packet[packet_offset] and 0x80 != 0) {
                audiosize = packet[packet_offset] shr 3 and 0x3
                audiosize = (Fs shl audiosize) / 400
            } else if (packet[packet_offset] and 0x60 == 0x60) {
                audiosize = if (packet[packet_offset] and 0x08 != 0) Fs / 50 else Fs / 100
            } else {
                audiosize = packet[packet_offset] shr 3 and 0x3
                if (audiosize == 3) {
                    audiosize = Fs * 60 / 1000
                } else {
                    audiosize = (Fs shl audiosize) / 100
                }
            }
            return audiosize
        }

        /// <summary>
        /// Gets the encoded bandwidth of an Opus packet. Note that you are not forced to decode at this bandwidth
        /// </summary>
        /// <param name="data">An Opus packet (must be at least 1 byte)</param>
        /// <returns>An OpusBandwidth value</returns>
        fun getBandwidth(packet: ByteArray, packet_offset: Int): OpusBandwidth {
            var bandwidth: OpusBandwidth
            if (packet[packet_offset] and 0x80 != 0) {
                bandwidth =
                        OpusBandwidthHelpers.GetBandwidth(OpusBandwidthHelpers.GetOrdinal(OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND) + (packet[packet_offset] shr 5 and 0x3))
                if (bandwidth == OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND) {
                    bandwidth = OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND
                }
            } else if (packet[packet_offset] and 0x60 == 0x60) {
                bandwidth = if (packet[packet_offset] and 0x10 != 0)
                    OpusBandwidth.OPUS_BANDWIDTH_FULLBAND
                else
                    OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND
            } else {
                bandwidth =
                        OpusBandwidthHelpers.GetBandwidth(OpusBandwidthHelpers.GetOrdinal(OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND) + (packet[packet_offset] shr 5 and 0x3))
            }
            return bandwidth
        }

        fun getNumEncodedChannels(packet: ByteArray, packet_offset: Int): Int {
            return if (packet[packet_offset] and 0x4 != 0) 2 else 1
        }

        /// <summary>
        /// Gets the number of frames in an Opus packet.
        /// </summary>
        /// <param name="packet">An Opus packet</param>
        /// <param name="len">The packet's length (must be at least 1)</param>
        /// <returns>The number of frames in the packet</returns>
        fun getNumFrames(packet: ByteArray, packet_offset: Int, len: Int): Int {
            val count: Int
            if (len < 1) {
                return OpusError.OPUS_BAD_ARG
            }
            count = packet[packet_offset] and 0x3
            return if (count == 0) {
                1
            } else if (count != 3) {
                2
            } else if (len < 2) {
                OpusError.OPUS_INVALID_PACKET
            } else {
                packet[packet_offset + 1] and 0x3F
            }
        }

        fun getNumSamples(
            packet: ByteArray, packet_offset: Int, len: Int,
            Fs: Int
        ): Int {
            val samples: Int
            val count = getNumFrames(packet, packet_offset, len)

            if (count < 0) {
                return count
            }

            samples = count * getNumSamplesPerFrame(packet, packet_offset, Fs)
            /* Can't have more than 120 ms */
            return if (samples * 25 > Fs * 3) {
                OpusError.OPUS_INVALID_PACKET
            } else {
                samples
            }
        }

        /// <summary>
        /// Gets the number of samples of an Opus packet.
        /// </summary>
        /// <param name="dec">Your current decoder state</param>
        /// <param name="packet">An Opus packet</param>
        /// <param name="len">The packet's length</param>
        /// <returns>The size of the PCM samples that this packet will be decoded to by the specified decoder</returns>
        fun getNumSamples(
            dec: OpusDecoder,
            packet: ByteArray, packet_offset: Int, len: Int
        ): Int {
            return getNumSamples(packet, packet_offset, len, dec.sampleRate)
        }

        fun getEncoderMode(packet: ByteArray, packet_offset: Int): OpusMode {
            val mode: OpusMode
            if (packet[packet_offset] and 0x80 != 0) {
                mode = OpusMode.MODE_CELT_ONLY
            } else if (packet[packet_offset] and 0x60 == 0x60) {
                mode = OpusMode.MODE_HYBRID
            } else {
                mode = OpusMode.MODE_SILK_ONLY
            }
            return mode
        }

        internal fun encode_size(size: Int, data: ByteArray, data_ptr: Int): Int {
            if (size < 252) {
                data[data_ptr] = (size and 0xFF).toByte()
                return 1
            } else {
                val dp1 = 252 + (size and 0x3)
                data[data_ptr] = (dp1 and 0xFF).toByte()
                data[data_ptr + 1] = (size - dp1 shr 2).toByte()
                return 2
            }
        }

        internal fun parse_size(data: ByteArray, data_ptr: Int, len: Int, size: BoxedValueShort): Int {
            if (len < 1) {
                size.Val = -1
                return -1
            } else if (Inlines.SignedByteToUnsignedInt(data[data_ptr]) < 252) {
                size.Val = Inlines.SignedByteToUnsignedInt(data[data_ptr]).toShort()
                return 1
            } else if (len < 2) {
                size.Val = -1
                return -1
            } else {
                size.Val =
                        (4 * Inlines.SignedByteToUnsignedInt(data[data_ptr + 1]) + Inlines.SignedByteToUnsignedInt(data[data_ptr])).toShort()
                return 2
            }
        }

        internal fun opus_packet_parse_impl(
            data: ByteArray, data_ptr: Int, len: Int,
            self_delimited: Int, out_toc: BoxedValueByte,
            frames: Array<ByteArray?>?, frames_ptr: Int, sizes: ShortArray?, sizes_ptr: Int,
            payload_offset: BoxedValueInt, packet_offset: BoxedValueInt
        ): Int {
            var data_ptr = data_ptr
            var len = len
            var i: Int
            var bytes: Int
            val count: Int
            var cbr: Int
            val toc: Byte
            val ch: Int
            val framesize: Int
            var last_size: Int
            var pad = 0
            val data0 = data_ptr
			var boxed_size: BoxedValueShort
            out_toc.Val = 0
            payload_offset.Val = 0
            packet_offset.Val = 0

            if (sizes == null || len < 0) {
                return OpusError.OPUS_BAD_ARG
            }
            if (len == 0) {
                return OpusError.OPUS_INVALID_PACKET
            }

            framesize = getNumSamplesPerFrame(data, data_ptr, 48000)

            cbr = 0
            toc = data[data_ptr++]
            len--
            last_size = len
            when (toc and 0x3) {
            /* One frame */
                0 -> count = 1
            /* Two CBR frames */
                1 -> {
                    count = 2
                    cbr = 1
                    if (self_delimited == 0) {
                        if (len and 0x1 != 0) {
                            return OpusError.OPUS_INVALID_PACKET
                        }
                        last_size = len / 2
                        /* If last_size doesn't fit in size[0], we'll catch it later */
                        sizes[sizes_ptr] = last_size.toShort()
                    }
                }
            /* Two VBR frames */
                2 -> {
                    count = 2
                    boxed_size = BoxedValueShort(sizes[sizes_ptr])
                    bytes = parse_size(data, data_ptr, len, boxed_size)
                    sizes[sizes_ptr] = boxed_size.Val
                    len -= bytes
                    if (sizes[sizes_ptr] < 0 || sizes[sizes_ptr] > len) {
                        return OpusError.OPUS_INVALID_PACKET
                    }
                    data_ptr += bytes
                    last_size = len - sizes[sizes_ptr]
                }
            /* Multiple CBR/VBR frames (from 0 to 120 ms) */
                else -> {
                    /*case 3:*/
                    if (len < 1) {
                        return OpusError.OPUS_INVALID_PACKET
                    }
                    /* Number of frames encoded in bits 0 to 5 */
                    ch = Inlines.SignedByteToUnsignedInt(data[data_ptr++])
                    count = ch and 0x3F
                    if (count <= 0 || framesize * count > 5760) {
                        return OpusError.OPUS_INVALID_PACKET
                    }
                    len--
                    /* Padding flag is bit 6 */
                    if (ch and 0x40 != 0) {
                        var p: Int
                        do {
                            val tmp: Int
                            if (len <= 0) {
                                return OpusError.OPUS_INVALID_PACKET
                            }
                            p = Inlines.SignedByteToUnsignedInt(data[data_ptr++])
                            len--
                            tmp = if (p == 255) 254 else p
                            len -= tmp
                            pad += tmp
                        } while (p == 255)
                    }
                    if (len < 0) {
                        return OpusError.OPUS_INVALID_PACKET
                    }
                    /* VBR flag is bit 7 */
                    cbr = if (ch and 0x80 != 0) 0 else 1
                    if (cbr == 0) {
                        /* VBR case */
                        last_size = len
                        i = 0
                        while (i < count - 1) {
                            boxed_size = BoxedValueShort(sizes[sizes_ptr + i])
                            bytes = parse_size(data, data_ptr, len, boxed_size)
                            sizes[sizes_ptr + i] = boxed_size.Val
                            len -= bytes
                            if (sizes[sizes_ptr + i] < 0 || sizes[sizes_ptr + i] > len) {
                                return OpusError.OPUS_INVALID_PACKET
                            }
                            data_ptr += bytes
                            last_size -= bytes + sizes[sizes_ptr + i]
                            i++
                        }
                        if (last_size < 0) {
                            return OpusError.OPUS_INVALID_PACKET
                        }
                    } else if (self_delimited == 0) {
                        /* CBR case */
                        last_size = len / count
                        if (last_size * count != len) {
                            return OpusError.OPUS_INVALID_PACKET
                        }
                        i = 0
                        while (i < count - 1) {
                            sizes[sizes_ptr + i] = last_size.toShort()
                            i++
                        }
                    }
                }
            }

            /* Self-delimited framing has an extra size for the last frame. */
            if (self_delimited != 0) {
                val boxed_size = BoxedValueShort(sizes[sizes_ptr + count - 1])
                bytes = parse_size(data, data_ptr, len, boxed_size)
                sizes[sizes_ptr + count - 1] = boxed_size.Val
                len -= bytes
                if (sizes[sizes_ptr + count - 1] < 0 || sizes[sizes_ptr + count - 1] > len) {
                    return OpusError.OPUS_INVALID_PACKET
                }
                data_ptr += bytes
                /* For CBR packets, apply the size to all the frames. */
                if (cbr != 0) {
                    if (sizes[sizes_ptr + count - 1] * count > len) {
                        return OpusError.OPUS_INVALID_PACKET
                    }
                    i = 0
                    while (i < count - 1) {
                        sizes[sizes_ptr + i] = sizes[sizes_ptr + count - 1]
                        i++
                    }
                } else if (bytes + sizes[sizes_ptr + count - 1] > last_size) {
                    return OpusError.OPUS_INVALID_PACKET
                }
            } else {
                /* Because it's not encoded explicitly, it's possible the size of the
               last packet (or all the packets, for the CBR case) is larger than
               1275. Reject them here.*/
                if (last_size > 1275) {
                    return OpusError.OPUS_INVALID_PACKET
                }
                sizes[sizes_ptr + count - 1] = last_size.toShort()
            }

            payload_offset.Val = data_ptr - data0

            i = 0
            while (i < count) {
                if (frames != null) {
                    // The old code returned pointers to the single data array, but that can cause unwanted side effects.
                    // So I have replaced it with this code that creates a new copy of each frame. Slower, but more robust
                    frames[frames_ptr + i] = ByteArray(data.size - data_ptr)
                    arraycopy(data, data_ptr, frames[frames_ptr + i]!!, 0, data.size - data_ptr)
                }
                data_ptr += sizes[sizes_ptr + i].toInt()
                i++
            }

            packet_offset.Val = pad + (data_ptr - data0)

            out_toc.Val = toc

            return count
        }
    }
}
