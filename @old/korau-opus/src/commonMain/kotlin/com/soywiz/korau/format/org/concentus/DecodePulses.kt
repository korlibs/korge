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

internal object DecodePulses {

    /**
     * ******************************************
     */
    /* Decode quantization indices of excitation */
    /**
     * ******************************************
     */
    fun silk_decode_pulses(
        psRangeDec: EntropyCoder, /* I/O  Compressor data structure                   */
        pulses: ShortArray, /* O    Excitation signal                           */
        signalType: Int, /* I    Sigtype                                     */
        quantOffsetType: Int, /* I    quantOffsetType                             */
        frame_length: Int /* I    Frame length                                */
    ) {
        var i: Int
        var j: Int
        var k: Int
        var iter: Int
        var abs_q: Int
        var nLS: Int
        val RateLevelIndex: Int
        val sum_pulses = IntArray(SilkConstants.MAX_NB_SHELL_BLOCKS)
        val nLshifts = IntArray(SilkConstants.MAX_NB_SHELL_BLOCKS)
        var pulses_ptr: Int

        /**
         * ******************
         */
        /* Decode rate level */
        /**
         * ******************
         */
        RateLevelIndex = psRangeDec.dec_icdf(SilkTables.silk_rate_levels_iCDF[signalType shr 1], 8)

        /* Calculate number of shell blocks */
        Inlines.OpusAssert(1 shl SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH == SilkConstants.SHELL_CODEC_FRAME_LENGTH)
        iter = Inlines.silk_RSHIFT(frame_length, SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH)
        if (iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH < frame_length) {
            Inlines.OpusAssert(frame_length == 12 * 10)
            /* Make sure only happens for 10 ms @ 12 kHz */
            iter++
        }

        /**
         * ************************************************
         */
        /* Sum-Weighted-Pulses Decoding                    */
        /**
         * ************************************************
         */
        i = 0
        while (i < iter) {
            nLshifts[i] = 0
            sum_pulses[i] = psRangeDec.dec_icdf(SilkTables.silk_pulses_per_block_iCDF[RateLevelIndex], 8)

            /* LSB indication */
            while (sum_pulses[i] == SilkConstants.SILK_MAX_PULSES + 1) {
                nLshifts[i]++
                /* When we've already got 10 LSBs, we shift the table to not allow (SILK_MAX_PULSES + 1) */
                sum_pulses[i] = psRangeDec.dec_icdf(
                    SilkTables.silk_pulses_per_block_iCDF[SilkConstants.N_RATE_LEVELS - 1],
                    if (nLshifts[i] == 10) 1 else 0,
                    8
                )
            }
            i++
        }

        /**
         * ************************************************
         */
        /* Shell decoding                                  */
        /**
         * ************************************************
         */
        i = 0
        while (i < iter) {
            if (sum_pulses[i] > 0) {
                ShellCoder.silk_shell_decoder(
                    pulses,
                    Inlines.silk_SMULBB(i, SilkConstants.SHELL_CODEC_FRAME_LENGTH),
                    psRangeDec,
                    sum_pulses[i]
                )
            } else {
                Arrays.MemSetWithOffset(
                    pulses,
                    0.toByte().toShort(),
                    Inlines.silk_SMULBB(i, SilkConstants.SHELL_CODEC_FRAME_LENGTH),
                    SilkConstants.SHELL_CODEC_FRAME_LENGTH
                )
            }
            i++
        }

        /**
         * ************************************************
         */
        /* LSB Decoding                                    */
        /**
         * ************************************************
         */
        i = 0
        while (i < iter) {
            if (nLshifts[i] > 0) {
                nLS = nLshifts[i]
                pulses_ptr = Inlines.silk_SMULBB(i, SilkConstants.SHELL_CODEC_FRAME_LENGTH)
                k = 0
                while (k < SilkConstants.SHELL_CODEC_FRAME_LENGTH) {
                    abs_q = pulses[pulses_ptr + k].toInt()
                    j = 0
                    while (j < nLS) {
                        abs_q = Inlines.silk_LSHIFT(abs_q, 1)
                        abs_q += psRangeDec.dec_icdf(SilkTables.silk_lsb_iCDF, 8)
                        j++
                    }
                    pulses[pulses_ptr + k] = abs_q.toShort()
                    k++
                }
                /* Mark the number of pulses non-zero for sign decoding. */
                sum_pulses[i] = sum_pulses[i] or (nLS shl 5)
            }
            i++
        }

        /**
         * *************************************
         */
        /* Decode and add signs to pulse signal */
        /**
         * *************************************
         */
        CodeSigns.silk_decode_signs(psRangeDec, pulses, frame_length, signalType, quantOffsetType, sum_pulses)
    }
}
