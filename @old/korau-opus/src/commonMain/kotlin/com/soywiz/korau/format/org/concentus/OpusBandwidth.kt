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

enum class OpusBandwidth {
    OPUS_BANDWIDTH_UNKNOWN,
    OPUS_BANDWIDTH_AUTO,
    OPUS_BANDWIDTH_NARROWBAND,
    OPUS_BANDWIDTH_MEDIUMBAND,
    OPUS_BANDWIDTH_WIDEBAND,
    OPUS_BANDWIDTH_SUPERWIDEBAND,
    OPUS_BANDWIDTH_FULLBAND
}

// Helpers to port over uses of OpusBandwidth as an integer
internal object OpusBandwidthHelpers {

    fun GetOrdinal(bw: OpusBandwidth): Int {
        when (bw) {
            OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND -> return 1
            OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND -> return 2
            OpusBandwidth.OPUS_BANDWIDTH_WIDEBAND -> return 3
            OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND -> return 4
            OpusBandwidth.OPUS_BANDWIDTH_FULLBAND -> return 5
        }

        return -1
    }

    fun GetBandwidth(ordinal: Int): OpusBandwidth {
        when (ordinal) {
            1 -> return OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND
            2 -> return OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND
            3 -> return OpusBandwidth.OPUS_BANDWIDTH_WIDEBAND
            4 -> return OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND
            5 -> return OpusBandwidth.OPUS_BANDWIDTH_FULLBAND
        }

        return OpusBandwidth.OPUS_BANDWIDTH_AUTO
    }

    fun MIN(a: OpusBandwidth, b: OpusBandwidth): OpusBandwidth {
        return if (GetOrdinal(a) < GetOrdinal(b)) {
            a
        } else b
    }

    fun MAX(a: OpusBandwidth, b: OpusBandwidth): OpusBandwidth {
        return if (GetOrdinal(a) > GetOrdinal(b)) {
            a
        } else b
    }

    fun SUBTRACT(a: OpusBandwidth, b: Int): OpusBandwidth {
        return GetBandwidth(GetOrdinal(a) - b)
    }
}
