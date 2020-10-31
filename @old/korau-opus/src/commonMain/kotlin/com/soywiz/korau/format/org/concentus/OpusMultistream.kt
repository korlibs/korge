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

internal object OpusMultistream {

    fun validate_layout(layout: ChannelLayout): Int {
        var i: Int
        val max_channel: Int

        max_channel = layout.nb_streams + layout.nb_coupled_streams
        if (max_channel > 255) {
            return 0
        }
        i = 0
        while (i < layout.nb_channels) {
            if (layout.mapping[i] >= max_channel && layout.mapping[i].toInt() != 255) {
                return 0
            }
            i++
        }
        return 1
    }

    fun get_left_channel(layout: ChannelLayout, stream_id: Int, prev: Int): Int {
        var i: Int
        i = if (prev < 0) 0 else prev + 1
        while (i < layout.nb_channels) {
            if (layout.mapping[i].toInt() == stream_id * 2) {
                return i
            }
            i++
        }
        return -1
    }

    fun get_right_channel(layout: ChannelLayout, stream_id: Int, prev: Int): Int {
        var i: Int
        i = if (prev < 0) 0 else prev + 1
        while (i < layout.nb_channels) {
            if (layout.mapping[i].toInt() == stream_id * 2 + 1) {
                return i
            }
            i++
        }
        return -1
    }

    fun get_mono_channel(layout: ChannelLayout, stream_id: Int, prev: Int): Int {
        var i: Int
        i = if (prev < 0) 0 else prev + 1
        while (i < layout.nb_channels) {
            if (layout.mapping[i].toInt() == stream_id + layout.nb_coupled_streams) {
                return i
            }
            i++
        }
        return -1
    }
}
