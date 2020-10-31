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

internal class StereoEncodeState {

    val pred_prev_Q13 = ShortArray(2)
    val sMid = ShortArray(2)
    val sSide = ShortArray(2)
    val mid_side_amp_Q0 = IntArray(4)
    var smth_width_Q14: Short = 0
    var width_prev_Q14: Short = 0
    var silent_side_len: Short = 0
    val predIx = Arrays.InitThreeDimensionalArrayByte(SilkConstants.MAX_FRAMES_PER_PACKET, 2, 3)
    val mid_only_flags = ByteArray(SilkConstants.MAX_FRAMES_PER_PACKET)

    fun Reset() {
        Arrays.MemSet(pred_prev_Q13, 0.toShort(), 2)
        Arrays.MemSet(sMid, 0.toShort(), 2)
        Arrays.MemSet(sSide, 0.toShort(), 2)
        Arrays.MemSet(mid_side_amp_Q0, 0, 4)
        smth_width_Q14 = 0
        width_prev_Q14 = 0
        silent_side_len = 0
        for (x in 0 until SilkConstants.MAX_FRAMES_PER_PACKET) {
            for (y in 0..1) {
                Arrays.MemSet(predIx[x][y], 0.toByte(), 3)
            }
        }

        Arrays.MemSet(mid_only_flags, 0.toByte(), SilkConstants.MAX_FRAMES_PER_PACKET)
    }
}
