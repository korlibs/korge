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

internal object ApplySineWindow {

    /* Apply sine window to signal vector.                                      */
    /* Window types:                                                            */
    /*    1 . sine window from 0 to pi/2                                       */
    /*    2 . sine window from pi/2 to pi                                      */
    /* Every other sample is linearly interpolated, for speed.                  */
    /* Window length must be between 16 and 120 (incl) and a multiple of 4.     */

    /* Matlab code for table:
       for k=16:9*4:16+2*9*4, fprintf(' %7.d,', -round(65536*pi ./ (k:4:k+8*4))); fprintf('\n'); end
     */
    private val freq_table_Q16 = shortArrayOf(
        12111,
        9804,
        8235,
        7100,
        6239,
        5565,
        5022,
        4575,
        4202,
        3885,
        3612,
        3375,
        3167,
        2984,
        2820,
        2674,
        2542,
        2422,
        2313,
        2214,
        2123,
        2038,
        1961,
        1889,
        1822,
        1760,
        1702
    )

    fun silk_apply_sine_window(
        px_win: ShortArray, /* O    Pointer to windowed signal                                  */
        px_win_ptr: Int,
        px: ShortArray, /* I    Pointer to input signal                                     */
        px_ptr: Int,
        win_type: Int, /* I    Selects a window type                                       */
        length: Int /* I    Window length, multiple of 4                                */
    ) {
        var k: Int
        val f_Q16: Int
        val c_Q16: Int
        var S0_Q16: Int
        var S1_Q16: Int

        Inlines.OpusAssert(win_type == 1 || win_type == 2)

        /* Length must be in a range from 16 to 120 and a multiple of 4 */
        Inlines.OpusAssert(length >= 16 && length <= 120)
        Inlines.OpusAssert(length and 3 == 0)

        /* Frequency */
        k = (length shr 2) - 4
        Inlines.OpusAssert(k >= 0 && k <= 26)
        f_Q16 = freq_table_Q16[k].toInt()

        /* Factor used for cosine approximation */
        c_Q16 = Inlines.silk_SMULWB(f_Q16, -f_Q16)
        Inlines.OpusAssert(c_Q16 >= -32768)

        /* initialize state */
        if (win_type == 1) {
            /* start from 0 */
            S0_Q16 = 0
            /* approximation of sin(f) */
            S1_Q16 = f_Q16 + Inlines.silk_RSHIFT(length, 3)
        } else {
            /* start from 1 */
            S0_Q16 = 1 shl 16
            /* approximation of cos(f) */
            S1_Q16 = (1 shl 16) + Inlines.silk_RSHIFT(c_Q16, 1) + Inlines.silk_RSHIFT(length, 4)
        }

        /* Uses the recursive equation:   sin(n*f) = 2 * cos(f) * sin((n-1)*f) - sin((n-2)*f)    */
        /* 4 samples at a time */
        k = 0
        while (k < length) {
            val pxwk = px_win_ptr + k
            val pxk = px_ptr + k
            px_win[pxwk] = Inlines.silk_SMULWB(Inlines.silk_RSHIFT(S0_Q16 + S1_Q16, 1), px[pxk].toInt()).toShort()
            px_win[pxwk + 1] = Inlines.silk_SMULWB(S1_Q16, px[pxk + 1].toInt()).toShort()
            S0_Q16 = Inlines.silk_SMULWB(S1_Q16, c_Q16) + Inlines.silk_LSHIFT(S1_Q16, 1) - S0_Q16 + 1
            S0_Q16 = Inlines.silk_min(S0_Q16, 1 shl 16)

            px_win[pxwk + 2] =
                    Inlines.silk_SMULWB(Inlines.silk_RSHIFT(S0_Q16 + S1_Q16, 1), px[pxk + 2].toInt()).toShort()
            px_win[pxwk + 3] = Inlines.silk_SMULWB(S0_Q16, px[pxk + 3].toInt()).toShort()
            S1_Q16 = Inlines.silk_SMULWB(S0_Q16, c_Q16) + Inlines.silk_LSHIFT(S0_Q16, 1) - S1_Q16
            S1_Q16 = Inlines.silk_min(S1_Q16, 1 shl 16)
            k += 4
        }
    }
}
