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

internal class VorbisLayout(var nb_streams: Int, var nb_coupled_streams: Int, var mapping: ShortArray) {
    companion object {

        /* Index is nb_channel-1*/
        val vorbis_mappings = arrayOf(
            VorbisLayout(1, 0, shortArrayOf(0)), /* 1: mono */
            VorbisLayout(1, 1, shortArrayOf(0, 1)), /* 2: stereo */
            VorbisLayout(2, 1, shortArrayOf(0, 2, 1)), /* 3: 1-d surround */
            VorbisLayout(2, 2, shortArrayOf(0, 1, 2, 3)), /* 4: quadraphonic surround */
            VorbisLayout(3, 2, shortArrayOf(0, 4, 1, 2, 3)), /* 5: 5-channel surround */
            VorbisLayout(4, 2, shortArrayOf(0, 4, 1, 2, 3, 5)), /* 6: 5.1 surround */
            VorbisLayout(4, 3, shortArrayOf(0, 4, 1, 2, 3, 5, 6)), /* 7: 6.1 surround */
            VorbisLayout(5, 3, shortArrayOf(0, 6, 1, 2, 3, 4, 5, 7))
        )/* 8: 7.1 surround */
    }
}
