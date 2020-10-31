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

internal object BWExpander {

    /// <summary>
    /// Chirp (bw expand) LP AR filter (Fixed point implementation)
    /// </summary>
    /// <param name="ar">I/O  AR filter to be expanded (without leading 1)</param>
    /// <param name="d">I length of ar</param>
    /// <param name="chirp">I    chirp factor (typically in range (0..1) )</param>
    fun silk_bwexpander_32(
        ar: IntArray, /* I/O  AR filter to be expanded (without leading 1)                */
        d: Int, /* I    Length of ar                                                */
        chirp_Q16: Int /* I    Chirp factor in Q16                                         */
    ) {
        var chirp_Q16 = chirp_Q16
        var i: Int
        val chirp_minus_one_Q16 = chirp_Q16 - 65536

        i = 0
        while (i < d - 1) {
            ar[i] = Inlines.silk_SMULWW(chirp_Q16, ar[i])
            chirp_Q16 += Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, chirp_minus_one_Q16), 16)
            i++
        }
        ar[d - 1] = Inlines.silk_SMULWW(chirp_Q16, ar[d - 1])
    }

    /// <summary>
    /// Chirp (bw expand) LP AR filter (Fixed point implementation)
    /// </summary>
    /// <param name="ar">I/O  AR filter to be expanded (without leading 1)</param>
    /// <param name="d">I length of ar</param>
    /// <param name="chirp">I    chirp factor (typically in range (0..1) )</param>
    fun silk_bwexpander(
        ar: ShortArray,
        d: Int,
        chirp_Q16: Int
    ) {
        var chirp_Q16 = chirp_Q16
        var i: Int
        val chirp_minus_one_Q16 = chirp_Q16 - 65536

        /* NB: Dont use silk_SMULWB, instead of silk_RSHIFT_ROUND( silk_MUL(), 16 ), below.  */
        /* Bias in silk_SMULWB can lead to unstable filters                                */
        i = 0
        while (i < d - 1) {
            ar[i] = Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, ar[i].toInt()), 16).toShort()
            chirp_Q16 += Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, chirp_minus_one_Q16), 16)
            i++
        }
        ar[d - 1] = Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, ar[d - 1].toInt()), 16).toShort()
    }
}
