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
/// Variable cut-off low-pass filter state
/// </summary>
internal class SilkLPState {

    /// <summary>
    /// Low pass filter state
    /// </summary>
    val In_LP_State = IntArray(2)

    /// <summary>
    /// Counter which is mapped to a cut-off frequency
    /// </summary>
    var transition_frame_no = 0

    /// <summary>
    /// Operating mode, <0: switch down, >0: switch up; 0: do nothing
    /// </summary>
    var mode = 0

    fun Reset() {
        In_LP_State[0] = 0
        In_LP_State[1] = 0
        transition_frame_no = 0
        mode = 0
    }

    /* Low-pass filter with variable cutoff frequency based on  */
    /* piece-wise linear interpolation between elliptic filters */
    /* Start by setting psEncC.mode <> 0;                      */
    /* Deactivate by setting psEncC.mode = 0;                  */
    fun silk_LP_variable_cutoff(
        frame: ShortArray, /* I/O  Low-pass filtered output signal             */
        frame_ptr: Int,
        frame_length: Int /* I    Frame length                                */
    ) {
        val B_Q28 = IntArray(SilkConstants.TRANSITION_NB)
        val A_Q28 = IntArray(SilkConstants.TRANSITION_NA)
        var fac_Q16 = 0
        var ind = 0

        Inlines.OpusAssert(this.transition_frame_no >= 0 && this.transition_frame_no <= SilkConstants.TRANSITION_FRAMES)

        /* Run filter if needed */
        if (this.mode != 0) {
            /* Calculate index and interpolation factor for interpolation */
            fac_Q16 = Inlines.silk_LSHIFT(SilkConstants.TRANSITION_FRAMES - this.transition_frame_no, 16 - 6)

            ind = Inlines.silk_RSHIFT(fac_Q16, 16)
            fac_Q16 -= Inlines.silk_LSHIFT(ind, 16)

            Inlines.OpusAssert(ind >= 0)
            Inlines.OpusAssert(ind < SilkConstants.TRANSITION_INT_NUM)

            /* Interpolate filter coefficients */
            Filters.silk_LP_interpolate_filter_taps(B_Q28, A_Q28, ind, fac_Q16)

            /* Update transition frame number for next frame */
            this.transition_frame_no =
                    Inlines.silk_LIMIT(this.transition_frame_no + this.mode, 0, SilkConstants.TRANSITION_FRAMES)

            /* ARMA low-pass filtering */
            Inlines.OpusAssert(SilkConstants.TRANSITION_NB == 3 && SilkConstants.TRANSITION_NA == 2)
            Filters.silk_biquad_alt(frame, frame_ptr, B_Q28, A_Q28, this.In_LP_State, frame, frame_ptr, frame_length, 1)
        }
    }
}
