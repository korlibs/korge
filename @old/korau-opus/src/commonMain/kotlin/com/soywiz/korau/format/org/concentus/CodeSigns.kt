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

internal object CodeSigns {

    private fun silk_enc_map(a: Int): Int {
        return Inlines.silk_RSHIFT(a, 15) + 1
    }

    private fun silk_dec_map(a: Int): Int {
        return Inlines.silk_LSHIFT(a, 1) - 1
    }

    /// <summary>
    /// Encodes signs of excitation
    /// </summary>
    /// <param name="psRangeEnc">I/O  Compressor data structure</param>
    /// <param name="pulses">I    pulse signal</param>
    /// <param name="length">I    length of input</param>
    /// <param name="signalType">I    Signal type</param>
    /// <param name="quantOffsetType">I    Quantization offset type</param>
    /// <param name="sum_pulses">I    Sum of absolute pulses per block [MAX_NB_SHELL_BLOCKS]</param>
    fun silk_encode_signs(
        psRangeEnc: EntropyCoder,
        pulses: ByteArray,
        length: Int,
        signalType: Int,
        quantOffsetType: Int,
        sum_pulses: IntArray
    ) {
        var length = length
        var i: Int
        var j: Int
        var p: Int
        val icdf = ShortArray(2)
        var q_ptr: Int
        val sign_icdf = SilkTables.silk_sign_iCDF
        val icdf_ptr: Int

        icdf[1] = 0
        q_ptr = 0
        i = Inlines.silk_SMULBB(7, Inlines.silk_ADD_LSHIFT(quantOffsetType, signalType, 1))
        icdf_ptr = i
        length = Inlines.silk_RSHIFT(
            length + SilkConstants.SHELL_CODEC_FRAME_LENGTH / 2,
            SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH
        )
        i = 0
        while (i < length) {
            p = sum_pulses[i]
            if (p > 0) {
                icdf[0] = sign_icdf[icdf_ptr + Inlines.silk_min(p and 0x1F, 6)]
                j = q_ptr
                while (j < q_ptr + SilkConstants.SHELL_CODEC_FRAME_LENGTH) {
                    if (pulses[j].toInt() != 0) {
                        psRangeEnc.enc_icdf(silk_enc_map(pulses[j].toInt()), icdf, 8)
                    }
                    j++
                }
            }

            q_ptr += SilkConstants.SHELL_CODEC_FRAME_LENGTH
            i++
        }
    }

    /// <summary>
    /// Decodes signs of excitation
    /// </summary>
    /// <param name="psRangeDec">I/O  Compressor data structure</param>
    /// <param name="pulses">I/O  pulse signal</param>
    /// <param name="length">I    length of input</param>
    /// <param name="signalType">I    Signal type</param>
    /// <param name="quantOffsetType">I    Quantization offset type</param>
    /// <param name="sum_pulses">I    Sum of absolute pulses per block [MAX_NB_SHELL_BLOCKS]</param>
    fun silk_decode_signs(
        psRangeDec: EntropyCoder,
        pulses: ShortArray,
        length: Int,
        signalType: Int,
        quantOffsetType: Int,
        sum_pulses: IntArray
    ) {
        var length = length
        var i: Int
        var j: Int
        var p: Int
        val icdf = ShortArray(2)
        var q_ptr: Int
        val icdf_table = SilkTables.silk_sign_iCDF
        val icdf_ptr: Int

        icdf[1] = 0
        q_ptr = 0
        i = Inlines.silk_SMULBB(7, Inlines.silk_ADD_LSHIFT(quantOffsetType, signalType, 1))
        icdf_ptr = i
        length = Inlines.silk_RSHIFT(
            length + SilkConstants.SHELL_CODEC_FRAME_LENGTH / 2,
            SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH
        )

        i = 0
        while (i < length) {
            p = sum_pulses[i]

            if (p > 0) {
                icdf[0] = icdf_table[icdf_ptr + Inlines.silk_min(p and 0x1F, 6)]
                j = 0
                while (j < SilkConstants.SHELL_CODEC_FRAME_LENGTH) {
                    if (pulses[q_ptr + j] > 0) {
                        /* attach sign */
                        pulses[q_ptr + j] = (pulses[q_ptr + j] * silk_dec_map(psRangeDec.dec_icdf(icdf, 8)).toShort()).toShort()
                    }
                    j++
                }
            }

            q_ptr += SilkConstants.SHELL_CODEC_FRAME_LENGTH
            i++
        }
    }
}
