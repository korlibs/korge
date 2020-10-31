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
/// Struct for Packet Loss Concealment
/// </summary>
internal class PLCStruct {

    var pitchL_Q8 = 0
    /* Pitch lag to use for voiced concealment                          */
    val LTPCoef_Q14 = ShortArray(SilkConstants.LTP_ORDER)
    /* LTP coeficients to use for voiced concealment                    */
    val prevLPC_Q12 = ShortArray(SilkConstants.MAX_LPC_ORDER)
    var last_frame_lost = 0
    /* Was previous frame lost                                          */
    var rand_seed = 0
    /* Seed for unvoiced signal generation                              */
    var randScale_Q14: Short = 0
    /* Scaling of unvoiced random signal                                */
    var conc_energy = 0
    var conc_energy_shift = 0
    var prevLTP_scale_Q14: Short = 0
    val prevGain_Q16 = IntArray(2)
    var fs_kHz = 0
    var nb_subfr = 0
    var subfr_length = 0

    fun Reset() {
        pitchL_Q8 = 0
        Arrays.MemSet(LTPCoef_Q14, 0.toShort(), SilkConstants.LTP_ORDER)
        Arrays.MemSet(prevLPC_Q12, 0.toShort(), SilkConstants.MAX_LPC_ORDER)
        last_frame_lost = 0
        rand_seed = 0
        randScale_Q14 = 0
        conc_energy = 0
        conc_energy_shift = 0
        prevLTP_scale_Q14 = 0
        Arrays.MemSet(prevGain_Q16, 0, 2)
        fs_kHz = 0
        nb_subfr = 0
        subfr_length = 0
    }
}
