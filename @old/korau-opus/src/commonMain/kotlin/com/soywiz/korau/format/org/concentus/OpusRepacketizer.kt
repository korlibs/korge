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

class OpusRepacketizer {

    internal var toc: Byte = 0
    /**
     * Return the total number of frames contained in packet data submitted to
     * the repacketizer state so far via opus_repacketizer_cat() since the last
     * call to opus_repacketizer_init() or opus_repacketizer_create(). This
     * defines the valid range of packets that can be extracted with
     * opus_repacketizer_out_range() or opus_repacketizer_out().
     *
     * @param rp <tt>OpusRepacketizer*</tt>: The repacketizer state containing
     * the frames.
     * @returns The total number of frames contained in the packet data
     * submitted to the repacketizer state.
     */
    var numFrames = 0
        internal set
    internal val frames = arrayOfNulls<ByteArray>(48)
    internal val len = ShortArray(48)
    internal var framesize = 0

    /**
     * (Re)initializes a previously allocated repacketizer state. The state must
     * be at least the size returned by opus_repacketizer_get_size(). This can
     * be used for applications which use their own allocator instead of
     * malloc(). It must also be called to reset the queue of packets waiting to
     * be repacketized, which is necessary if the maximum packet duration of 120
     * ms is reached or if you wish to submit packets with a different Opus
     * configuration (coding mode, audio bandwidth, frame size, or channel
     * count). Failure to do so will prevent a new packet from being added with
     * opus_repacketizer_cat().
     *
     * @see opus_repacketizer_create
     *
     * @see opus_repacketizer_get_size
     *
     * @see opus_repacketizer_cat
     *
     * @param rp <tt>OpusRepacketizer*</tt>: The repacketizer state to
     * (re)initialize.
     */
    fun Reset() {
        this.numFrames = 0
    }

    /**
     * Allocates memory and initializes the new repacketizer with
     * opus_repacketizer_init().
     */
    init {
        this.Reset()
    }

    internal fun opus_repacketizer_cat_impl(data: ByteArray, data_ptr: Int, len: Int, self_delimited: Int): Int {
        val dummy_toc = BoxedValueByte(0.toByte())
        val dummy_offset = BoxedValueInt(0)
        val curr_nb_frames: Int
        val ret: Int
        /* Set of check ToC */
        if (len < 1) {
            return OpusError.OPUS_INVALID_PACKET
        }

        if (this.numFrames == 0) {
            this.toc = data[data_ptr]
            this.framesize = OpusPacketInfo.getNumSamplesPerFrame(data, data_ptr, 8000)
        } else if (this.toc and 0xFC != data[data_ptr] and 0xFC) {
            /*fprintf(stderr, "toc mismatch: 0x%x vs 0x%x\n", rp.toc, data[0]);*/
            return OpusError.OPUS_INVALID_PACKET
        }
        curr_nb_frames = OpusPacketInfo.getNumFrames(data, data_ptr, len)
        if (curr_nb_frames < 1) {
            return OpusError.OPUS_INVALID_PACKET
        }

        /* Check the 120 ms maximum packet size */
        if ((curr_nb_frames + this.numFrames) * this.framesize > 960) {
            return OpusError.OPUS_INVALID_PACKET
        }

        ret = OpusPacketInfo.opus_packet_parse_impl(
            data,
            data_ptr,
            len,
            self_delimited,
            dummy_toc,
            this.frames,
            this.numFrames,
            this.len,
            this.numFrames,
            dummy_offset,
            dummy_offset
        )
        if (ret < 1) {
            return ret
        }

        this.numFrames += curr_nb_frames
        return OpusError.OPUS_OK
    }

    /**
     * opus_repacketizer_cat. Add a packet to the current repacketizer state.
     * This packet must match the configuration of any packets already submitted
     * for repacketization since the last call to opus_repacketizer_init(). This
     * means that it must have the same coding mode, audio bandwidth, frame
     * size, and channel count. This can be checked in advance by examining the
     * top 6 bits of the first byte of the packet, and ensuring they match the
     * top 6 bits of the first byte of any previously submitted packet. The
     * total duration of audio in the repacketizer state also must not exceed
     * 120 ms, the maximum duration of a single packet, after adding this
     * packet.
     *
     * The contents of the current repacketizer state can be extracted into new
     * packets using opus_repacketizer_out() or opus_repacketizer_out_range().
     *
     * In order to add a packet with a different configuration or to add more
     * audio beyond 120 ms, you must clear the repacketizer state by calling
     * opus_repacketizer_init(). If a packet is too large to add to the current
     * repacketizer state, no part of it is added, even if it contains multiple
     * frames, some of which might fit. If you wish to be able to add parts of
     * such packets, you should first use another repacketizer to split the
     * packet into pieces and add them individually.
     *
     * @see opus_repacketizer_out_range
     *
     * @see opus_repacketizer_out
     *
     * @see opus_repacketizer_init
     *
     * @param data : The packet data. The application must ensure this pointer
     * remains valid until the next call to opus_repacketizer_init() or
     * opus_repacketizer_destroy().
     * @param len: The number of bytes in the packet data.
     * @returns An error code indicating whether or not the operation succeeded.
     * @retval #OPUS_OK The packet's contents have been added to the
     * repacketizer state.
     * @retval #OPUS_INVALID_PACKET The packet did not have a valid TOC
     * sequence, the packet's TOC sequence was not compatible with previously
     * submitted packets (because the coding mode, audio bandwidth, frame size,
     * or channel count did not match), or adding this packet would increase the
     * total amount of audio stored in the repacketizer state to more than 120
     * ms.
     */
    fun addPacket(data: ByteArray, data_offset: Int, len: Int): Int {
        return opus_repacketizer_cat_impl(data, data_offset, len, 0)
    }

    internal fun opus_repacketizer_out_range_impl(
        begin: Int, end: Int,
        data: ByteArray, data_ptr: Int, maxlen: Int, self_delimited: Int, pad: Int
    ): Int {
        var i: Int
        val count: Int
        var tot_size: Int
        var ptr: Int

        if (begin < 0 || begin >= end || end > this.numFrames) {
            /*fprintf(stderr, "%d %d %d\n", begin, end, rp.nb_frames);*/
            return OpusError.OPUS_BAD_ARG
        }
        count = end - begin

        if (self_delimited != 0) {
            tot_size = 1 + if (this.len[count - 1] >= 252) 1 else 0
        } else {
            tot_size = 0
        }

        ptr = data_ptr
        if (count == 1) {
            /* Code 0 */
            tot_size += this.len[0] + 1
            if (tot_size > maxlen) {
                return OpusError.OPUS_BUFFER_TOO_SMALL
            }
            data[ptr++] = (this.toc and 0xFC).toByte()
        } else if (count == 2) {
            if (this.len[1] == this.len[0]) {
                /* Code 1 */
                tot_size += 2 * this.len[0] + 1
                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL
                }
                data[ptr++] = (this.toc and 0xFC or 0x1).toByte()
            } else {
                /* Code 2 */
                tot_size += this.len[0].toInt() + this.len[1].toInt() + 2 + if (this.len[0] >= 252) 1 else 0
                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL
                }
                data[ptr++] = (this.toc and 0xFC or 0x2).toByte()
                ptr += OpusPacketInfo.encode_size(this.len[0].toInt(), data, ptr)
            }
        }
        if (count > 2 || pad != 0 && tot_size < maxlen) {
            /* Code 3 */
            var vbr: Int
            var pad_amount = 0

            /* Restart the process for the padding case */
            ptr = data_ptr
            if (self_delimited != 0) {
                tot_size = 1 + if (this.len[count - 1] >= 252) 1 else 0
            } else {
                tot_size = 0
            }
            vbr = 0
            i = 1
            while (i < count) {
                if (this.len[i] != this.len[0]) {
                    vbr = 1
                    break
                }
                i++
            }
            if (vbr != 0) {
                tot_size += 2
                i = 0
                while (i < count - 1) {
                    tot_size += 1 + (if (this.len[i] >= 252) 1 else 0) + this.len[i].toInt()
                    i++
                }
                tot_size += this.len[count - 1].toInt()

                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL
                }
                data[ptr++] = (this.toc and 0xFC or 0x3).toByte()
                data[ptr++] = (count or 0x80).toByte()
            } else {
                tot_size += count * this.len[0] + 2
                if (tot_size > maxlen) {
                    return OpusError.OPUS_BUFFER_TOO_SMALL
                }
                data[ptr++] = (this.toc and 0xFC or 0x3).toByte()
                data[ptr++] = count.toByte()
            }

            pad_amount = if (pad != 0) maxlen - tot_size else 0

            if (pad_amount != 0) {
                val nb_255s: Int
                data[data_ptr + 1] = (data[data_ptr + 1] or 0x40).toByte()
                nb_255s = (pad_amount - 1) / 255
                i = 0
                while (i < nb_255s) {
                    data[ptr++] = -1
                    i++
                }

                data[ptr++] = (pad_amount - 255 * nb_255s - 1).toByte()
                tot_size += pad_amount
            }

            if (vbr != 0) {
                i = 0
                while (i < count - 1) {
                    ptr += OpusPacketInfo.encode_size(this.len[i].toInt(), data, ptr)
                    i++
                }
            }
        }

        if (self_delimited != 0) {
            val sdlen = OpusPacketInfo.encode_size(this.len[count - 1].toInt(), data, ptr)
            ptr += sdlen
        }

        /* Copy the actual data */
        i = begin
        while (i < count + begin) {

            if (data == this.frames[i]) {
                /* Using OPUS_MOVE() instead of OPUS_COPY() in case we're doing in-place
                   padding from opus_packet_pad or opus_packet_unpad(). */
                Arrays.MemMove(data, 0, ptr, this.len[i].toInt())
            } else {
                arraycopy(this.frames!![i]!!, 0, data, ptr, this.len[i].toInt())
            }
            ptr += this.len[i].toInt()
            i++
        }

        if (pad != 0) {
            /* Fill padding with zeros. */
            Arrays.MemSetWithOffset(data, 0.toByte(), ptr, data_ptr + maxlen - ptr)
        }

        return tot_size
    }

    /**
     * Construct a new packet from data previously submitted to the repacketizer
     * state via opus_repacketizer_cat().
     *
     * @param rp <tt>OpusRepacketizer*</tt>: The repacketizer state from which
     * to construct the new packet.
     * @param begin <tt>int</tt>: The index of the first frame in the current
     * repacketizer state to include in the output.
     * @param end <tt>int</tt>: One past the index of the last frame in the
     * current repacketizer state to include in the output.
     * @param[out] data <tt>final unsigned char*</tt>: The buffer in which to
     * store the output packet.
     * @param maxlen <tt>opus_int32</tt>: The maximum number of bytes to store
     * in the output buffer. In order to guarantee success, this should be at
     * least `1276` for a single frame, or for multiple frames,
     * `1277*(end-begin)`. However, `1*(end-begin)` plus
     * the size of all packet data submitted to the repacketizer since the last
     * call to opus_repacketizer_init() or opus_repacketizer_create() is also
     * sufficient, and possibly much smaller.
     * @returns The total size of the output packet on success, or an error code
     * on failure.
     * @retval #OPUS_BAD_ARG `[begin,end)` was an invalid range of
     * frames (begin < 0, begin >= end, or end >
     * opus_repacketizer_get_nb_frames()).
     * @retval #OPUS_BUFFER_TOO_SMALL \a maxlen was insufficient to contain the
     * complete output packet.
     */
    fun createPacket(begin: Int, end: Int, data: ByteArray, data_offset: Int, maxlen: Int): Int {
        return opus_repacketizer_out_range_impl(begin, end, data, data_offset, maxlen, 0, 0)
    }

    /**
     * Construct a new packet from data previously submitted to the repacketizer
     * state via opus_repacketizer_cat(). This is a convenience routine that
     * returns all the data submitted so far in a single packet. It is
     * equivalent to calling
     * @code
     * opus_repacketizer_out_range(rp, 0, opus_repacketizer_get_nb_frames(rp),
     * data, maxlen)
     * @endcode
     * @param rp <tt>OpusRepacketizer*</tt>: The repacketizer state from which to
     * construct the new packet.
     * @param[out] data <tt>final unsigned char*</tt>: The buffer in which to
     * store the output packet.
     * @param maxlen <tt>opus_int32</tt>: The maximum number of bytes to store in
     * the output buffer. In order to guarantee
     * success, this should be at least
     * `1277*opus_repacketizer_get_nb_frames(rp)`.
     * However,
     * `1*opus_repacketizer_get_nb_frames(rp)`
     * plus the size of all packet data
     * submitted to the repacketizer since the
     * last call to opus_repacketizer_init() or
     * opus_repacketizer_create() is also
     * sufficient, and possibly much smaller.
     * @returns The total size of the output packet on success, or an error code
     * on failure.
     * @retval #OPUS_BUFFER_TOO_SMALL \a maxlen was insufficient to contain the
     * complete output packet.
     */
    fun createPacket(data: ByteArray, data_offset: Int, maxlen: Int): Int {
        return opus_repacketizer_out_range_impl(0, this.numFrames, data, data_offset, maxlen, 0, 0)
    }

    companion object {

        /**
         * Pads a given Opus packet to a larger size (possibly changing the TOC
         * sequence).
         *
         * @param[in,out] data <tt>final unsigned char*</tt>: The buffer containing
         * the packet to pad.
         * @param len <tt>opus_int32</tt>: The size of the packet. This must be at
         * least 1.
         * @param new_len <tt>opus_int32</tt>: The desired size of the packet after
         * padding. This must be at least as large as len.
         * @returns an error code
         * @retval #OPUS_OK \a on success.
         * @retval #OPUS_BAD_ARG \a len was less than 1 or new_len was less than
         * len.
         * @retval #OPUS_INVALID_PACKET \a data did not contain a valid Opus packet.
         */
        fun padPacket(data: ByteArray, data_offset: Int, len: Int, new_len: Int): Int {
            val rp = OpusRepacketizer()
            val ret: Int
            if (len < 1) {
                return OpusError.OPUS_BAD_ARG
            }
            if (len == new_len) {
                return OpusError.OPUS_OK
            } else if (len > new_len) {
                return OpusError.OPUS_BAD_ARG
            }
            rp.Reset()
            /* Moving payload to the end of the packet so we can do in-place padding */
            Arrays.MemMove(data, data_offset, data_offset + new_len - len, len)
            //data.MemMoveTo(data.Point(new_len - len), len);
            rp.addPacket(data, data_offset + new_len - len, len)
            ret = rp.opus_repacketizer_out_range_impl(0, rp.numFrames, data, data_offset, new_len, 0, 1)
            return if (ret > 0) {
                OpusError.OPUS_OK
            } else {
                ret
            }
        }

        /**
         * Remove all padding from a given Opus packet and rewrite the TOC sequence
         * to minimize space usage.
         *
         * @param[in,out] data <tt>final unsigned char*</tt>: The buffer containing
         * the packet to strip.
         * @param len <tt>opus_int32</tt>: The size of the packet. This must be at
         * least 1.
         * @returns The new size of the output packet on success, or an error code
         * on failure.
         * @retval #OPUS_BAD_ARG \a len was less than 1.
         * @retval #OPUS_INVALID_PACKET \a data did not contain a valid Opus packet.
         */
        fun unpadPacket(data: ByteArray, data_offset: Int, len: Int): Int {
            var ret: Int
            if (len < 1) {
                return OpusError.OPUS_BAD_ARG
            }

            val rp = OpusRepacketizer()
            rp.Reset()
            ret = rp.addPacket(data, data_offset, len)
            if (ret < 0) {
                return ret
            }
            ret = rp.opus_repacketizer_out_range_impl(0, rp.numFrames, data, data_offset, len, 0, 0)
            Inlines.OpusAssert(ret > 0 && ret <= len)

            return ret
        }

        /**
         * Pads a given Opus multi-stream packet to a larger size (possibly changing
         * the TOC sequence).
         *
         * @param[in,out] data <tt>final unsigned char*</tt>: The buffer containing
         * the packet to pad.
         * @param len <tt>opus_int32</tt>: The size of the packet. This must be at
         * least 1.
         * @param new_len <tt>opus_int32</tt>: The desired size of the packet after
         * padding. This must be at least 1.
         * @param nb_streams <tt>opus_int32</tt>: The number of streams (not
         * channels) in the packet. This must be at least as large as len.
         * @returns an error code
         * @retval #OPUS_OK \a on success.
         * @retval #OPUS_BAD_ARG \a len was less than 1.
         * @retval #OPUS_INVALID_PACKET \a data did not contain a valid Opus packet.
         */
        fun padMultistreamPacket(data: ByteArray, data_offset: Int, len: Int, new_len: Int, nb_streams: Int): Int {
            var data_offset = data_offset
            var len = len
            var s: Int
            var count: Int
            val dummy_toc = BoxedValueByte(0.toByte())
            val size = ShortArray(48)
            val packet_offset = BoxedValueInt(0)
            val dummy_offset = BoxedValueInt(0)
            val amount: Int

            if (len < 1) {
                return OpusError.OPUS_BAD_ARG
            }
            if (len == new_len) {
                return OpusError.OPUS_OK
            } else if (len > new_len) {
                return OpusError.OPUS_BAD_ARG
            }
            amount = new_len - len
            /* Seek to last stream */
            s = 0
            while (s < nb_streams - 1) {
                if (len <= 0) {
                    return OpusError.OPUS_INVALID_PACKET
                }
                count = OpusPacketInfo.opus_packet_parse_impl(
                    data, data_offset, len, 1, dummy_toc, null, 0,
                    size, 0, dummy_offset, packet_offset
                )
                if (count < 0) {
                    return count
                }
                data_offset += packet_offset.Val
                len -= packet_offset.Val
                s++
            }
            return padPacket(data, data_offset, len, len + amount)
        }

        // FIXME THIS METHOD FAILS IN TEST_OPUS_ENCODE
        /**
         * Remove all padding from a given Opus multi-stream packet and rewrite the
         * TOC sequence to minimize space usage.
         *
         * @param[in,out] data <tt>final unsigned char*</tt>: The buffer containing
         * the packet to strip.
         * @param len <tt>opus_int32</tt>: The size of the packet. This must be at
         * least 1.
         * @param nb_streams <tt>opus_int32</tt>: The number of streams (not
         * channels) in the packet. This must be at least 1.
         * @returns The new size of the output packet on success, or an error code
         * on failure.
         * @retval #OPUS_BAD_ARG \a len was less than 1 or new_len was less than
         * len.
         * @retval #OPUS_INVALID_PACKET \a data did not contain a valid Opus packet.
         */
        fun unpadMultistreamPacket(data: ByteArray, data_offset: Int, len: Int, nb_streams: Int): Int {
            var data_offset = data_offset
            var len = len
            var s: Int
            val dummy_toc = BoxedValueByte(0.toByte())
            val size = ShortArray(48)
            val packet_offset = BoxedValueInt(0)
            val dummy_offset = BoxedValueInt(0)
            val rp = OpusRepacketizer()
            var dst: Int
            var dst_len: Int

            if (len < 1) {
                return OpusError.OPUS_BAD_ARG
            }
            dst = data_offset
            dst_len = 0
            /* Unpad all frames */
            s = 0
            while (s < nb_streams) {
                var ret: Int
                val self_delimited = (if (s != nb_streams) 1 else 0) - 1
                if (len <= 0) {
                    return OpusError.OPUS_INVALID_PACKET
                }
                rp.Reset()
                ret = OpusPacketInfo.opus_packet_parse_impl(
                    data, data_offset, len, self_delimited, dummy_toc, null, 0,
                    size, 0, dummy_offset, packet_offset
                )
                if (ret < 0) {
                    return ret
                }
                ret = rp.opus_repacketizer_cat_impl(data, data_offset, packet_offset.Val, self_delimited)
                if (ret < 0) {
                    return ret
                }
                ret = rp.opus_repacketizer_out_range_impl(0, rp.numFrames, data, dst, len, self_delimited, 0)
                if (ret < 0) {
                    return ret
                } else {
                    dst_len += ret
                }
                dst += ret
                data_offset += packet_offset.Val
                len -= packet_offset.Val
                s++
            }
            return dst_len
        }
    }
}
