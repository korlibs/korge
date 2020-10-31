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
/// Prefilter state
/// </summary>
internal class SilkPrefilterState {

    val sLTP_shp = ShortArray(SilkConstants.LTP_BUF_LENGTH)
    val sAR_shp = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER + 1)
    var sLTP_shp_buf_idx = 0
    var sLF_AR_shp_Q12 = 0
    var sLF_MA_shp_Q12 = 0
    var sHarmHP_Q2 = 0
    var rand_seed = 0
    var lagPrev = 0

    fun Reset() {
        Arrays.MemSet(sLTP_shp, 0.toShort(), SilkConstants.LTP_BUF_LENGTH)
        Arrays.MemSet(sAR_shp, 0, SilkConstants.MAX_SHAPE_LPC_ORDER + 1)
        sLTP_shp_buf_idx = 0
        sLF_AR_shp_Q12 = 0
        sLF_MA_shp_Q12 = 0
        sHarmHP_Q2 = 0
        rand_seed = 0
        lagPrev = 0
    }
}
