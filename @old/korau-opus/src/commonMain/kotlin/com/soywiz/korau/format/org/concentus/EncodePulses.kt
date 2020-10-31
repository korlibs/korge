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

internal object EncodePulses {

    /// <summary>
    ///
    /// </summary>
    /// <param name="pulses_comb">(O)</param>
    /// <param name="pulses_in">(I)</param>
    /// <param name="max_pulses"> I    max value for sum of pulses</param>
    /// <param name="len">I    number of output values</param>
    /// <returns>return ok</returns>
    fun combine_and_check(
        pulses_comb: IntArray,
        pulses_comb_ptr: Int,
        pulses_in: IntArray,
        pulses_in_ptr: Int,
        max_pulses: Int,
        len: Int
    ): Int {
        for (k in 0 until len) {
            val k2p = 2 * k + pulses_in_ptr
            val sum = pulses_in[k2p] + pulses_in[k2p + 1]
            if (sum > max_pulses) {
                return 1
            }
            pulses_comb[pulses_comb_ptr + k] = sum
        }
        return 0
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="pulses_comb">(O)</param>
    /// <param name="pulses_in">(I)</param>
    /// <param name="max_pulses"> I    max value for sum of pulses</param>
    /// <param name="len">I    number of output values</param>
    /// <returns>return ok</returns>
    fun combine_and_check(
        pulses_comb: IntArray,
        pulses_in: IntArray,
        max_pulses: Int,
        len: Int
    ): Int {
        for (k in 0 until len) {
            val sum = pulses_in[2 * k] + pulses_in[2 * k + 1]
            if (sum > max_pulses) {
                return 1
            }
            pulses_comb[k] = sum
        }
        return 0
    }

    /// <summary>
    /// Encode quantization indices of excitation
    /// </summary>
    /// <param name="psRangeEnc">I/O  compressor data structure</param>
    /// <param name="signalType">I    Signal type</param>
    /// <param name="quantOffsetType">I    quantOffsetType</param>
    /// <param name="pulses">I    quantization indices</param>
    /// <param name="frame_length">I    Frame length</param>
    fun silk_encode_pulses(
        psRangeEnc: EntropyCoder,
        signalType: Int,
        quantOffsetType: Int,
        pulses: ByteArray,
        frame_length: Int
    ) {
        var i: Int
        var k: Int
        var j: Int
        var iter: Int
        var bit: Int
        var nLS: Int
        var scale_down: Int
        var RateLevelIndex = 0
        var abs_q: Int
        var minSumBits_Q5: Int
        var sumBits_Q5: Int
        val abs_pulses: IntArray
        val sum_pulses: IntArray
        val nRshifts: IntArray
        val pulses_comb = IntArray(8)
        var abs_pulses_ptr: Int
        var pulses_ptr: Int
        var nBits_ptr: ShortArray

        Arrays.MemSet(pulses_comb, 0, 8)

        /**
         * *************************
         */
        /* Prepare for shell coding */
        /**
         * *************************
         */
        /* Calculate number of shell blocks */
        Inlines.OpusAssert(1 shl SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH == SilkConstants.SHELL_CODEC_FRAME_LENGTH)
        iter = Inlines.silk_RSHIFT(frame_length, SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH)
        if (iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH < frame_length) {
            Inlines.OpusAssert(frame_length == 12 * 10)
            /* Make sure only happens for 10 ms @ 12 kHz */
            iter++
            Arrays.MemSetWithOffset(pulses, 0.toByte(), frame_length, SilkConstants.SHELL_CODEC_FRAME_LENGTH)
        }

        /* Take the absolute value of the pulses */
        abs_pulses = IntArray(iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH)
        Inlines.OpusAssert(SilkConstants.SHELL_CODEC_FRAME_LENGTH and 3 == 0)

        // unrolled loop
        i = 0
        while (i < iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH) {
            abs_pulses[i + 0] = Inlines.silk_abs(pulses[i + 0].toInt()).toInt()
            abs_pulses[i + 1] = Inlines.silk_abs(pulses[i + 1].toInt()).toInt()
            abs_pulses[i + 2] = Inlines.silk_abs(pulses[i + 2].toInt()).toInt()
            abs_pulses[i + 3] = Inlines.silk_abs(pulses[i + 3].toInt()).toInt()
            i += 4
        }

        /* Calc sum pulses per shell code frame */
        sum_pulses = IntArray(iter)
        nRshifts = IntArray(iter)
        abs_pulses_ptr = 0
        i = 0
        while (i < iter) {
            nRshifts[i] = 0

            while (true) {
                /* 1+1 . 2 */
                scale_down = combine_and_check(
                    pulses_comb,
                    0,
                    abs_pulses,
                    abs_pulses_ptr,
                    SilkTables.silk_max_pulses_table[0].toInt(),
                    8
                )
                /* 2+2 . 4 */
                scale_down += combine_and_check(
                    pulses_comb,
                    pulses_comb,
                    SilkTables.silk_max_pulses_table[1].toInt(),
                    4
                )
                /* 4+4 . 8 */
                scale_down += combine_and_check(
                    pulses_comb,
                    pulses_comb,
                    SilkTables.silk_max_pulses_table[2].toInt(),
                    2
                )
                /* 8+8 . 16 */
                scale_down += combine_and_check(
                    sum_pulses,
                    i,
                    pulses_comb,
                    0,
                    SilkTables.silk_max_pulses_table[3].toInt(),
                    1
                )

                if (scale_down != 0) {
                    /* We need to downscale the quantization signal */
                    nRshifts[i]++
                    k = abs_pulses_ptr
                    while (k < abs_pulses_ptr + SilkConstants.SHELL_CODEC_FRAME_LENGTH) {
                        abs_pulses[k] = Inlines.silk_RSHIFT(abs_pulses[k], 1)
                        k++
                    }
                } else {
                    /* Jump out of while(1) loop and go to next shell coding frame */
                    break
                }
            }

            abs_pulses_ptr += SilkConstants.SHELL_CODEC_FRAME_LENGTH
            i++
        }

        /**
         * ***********
         */
        /* Rate level */
        /**
         * ***********
         */
        /* find rate level that leads to fewest bits for coding of pulses per block info */
        minSumBits_Q5 = Int.MAX_VALUE
        k = 0
        while (k < SilkConstants.N_RATE_LEVELS - 1) {
            nBits_ptr = SilkTables.silk_pulses_per_block_BITS_Q5[k]
            sumBits_Q5 = SilkTables.silk_rate_levels_BITS_Q5[signalType shr 1][k].toInt()
            i = 0
            while (i < iter) {
                if (nRshifts[i] > 0) {
                    sumBits_Q5 += nBits_ptr[SilkConstants.SILK_MAX_PULSES + 1].toInt()
                } else {
                    sumBits_Q5 += nBits_ptr[sum_pulses[i]].toInt()
                }
                i++
            }
            if (sumBits_Q5 < minSumBits_Q5) {
                minSumBits_Q5 = sumBits_Q5
                RateLevelIndex = k
            }
            k++
        }

        psRangeEnc.enc_icdf(RateLevelIndex, SilkTables.silk_rate_levels_iCDF[signalType shr 1], 8)

        /**
         * ************************************************
         */
        /* Sum-Weighted-Pulses Encoding                    */
        /**
         * ************************************************
         */
        i = 0
        while (i < iter) {
            if (nRshifts[i] == 0) {
                psRangeEnc.enc_icdf(sum_pulses[i], SilkTables.silk_pulses_per_block_iCDF[RateLevelIndex], 8)
            } else {
                psRangeEnc.enc_icdf(
                    SilkConstants.SILK_MAX_PULSES + 1,
                    SilkTables.silk_pulses_per_block_iCDF[RateLevelIndex],
                    8
                )
                k = 0
                while (k < nRshifts[i] - 1) {
                    psRangeEnc.enc_icdf(
                        SilkConstants.SILK_MAX_PULSES + 1,
                        SilkTables.silk_pulses_per_block_iCDF[SilkConstants.N_RATE_LEVELS - 1],
                        8
                    )
                    k++
                }

                psRangeEnc.enc_icdf(
                    sum_pulses[i],
                    SilkTables.silk_pulses_per_block_iCDF[SilkConstants.N_RATE_LEVELS - 1],
                    8
                )
            }
            i++
        }

        /**
         * ***************
         */
        /* Shell Encoding */
        /**
         * ***************
         */
        i = 0
        while (i < iter) {
            if (sum_pulses[i] > 0) {
                ShellCoder.silk_shell_encoder(psRangeEnc, abs_pulses, i * SilkConstants.SHELL_CODEC_FRAME_LENGTH)
            }
            i++
        }

        /**
         * *************
         */
        /* LSB Encoding */
        /**
         * *************
         */
        i = 0
        while (i < iter) {
            if (nRshifts[i] > 0) {
                pulses_ptr = i * SilkConstants.SHELL_CODEC_FRAME_LENGTH
                nLS = nRshifts[i] - 1
                k = 0
                while (k < SilkConstants.SHELL_CODEC_FRAME_LENGTH) {
                    abs_q = Inlines.silk_abs(pulses[pulses_ptr + k].toInt()).toByte().toInt()
                    j = nLS
                    while (j > 0) {
                        bit = Inlines.silk_RSHIFT(abs_q, j) and 1
                        psRangeEnc.enc_icdf(bit, SilkTables.silk_lsb_iCDF, 8)
                        j--
                    }
                    bit = abs_q and 1
                    psRangeEnc.enc_icdf(bit, SilkTables.silk_lsb_iCDF, 8)
                    k++
                }
            }
            i++
        }

        /**
         * *************
         */
        /* Encode signs */
        /**
         * *************
         */
        CodeSigns.silk_encode_signs(psRangeEnc, pulses, frame_length, signalType, quantOffsetType, sum_pulses)
    }
}
