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
/// shell coder; pulse-subframe length is hardcoded
/// </summary>
internal object ShellCoder {

    /// <summary>
    /// </summary>
    /// <param name="output">O    combined pulses vector [len]</param>
    /// <param name="input">I    input vector       [2 * len]</param>
    /// <param name="len">I    number of OUTPUT samples</param>
    fun combine_pulses(
        output: IntArray,
        input: IntArray,
        input_ptr: Int,
        len: Int
    ) {
        var k: Int
        k = 0
        while (k < len) {
            output[k] = input[input_ptr + 2 * k] + input[input_ptr + 2 * k + 1]
            k++
        }
    }

    /// <summary>
    /// </summary>
    /// <param name="output">O    combined pulses vector [len]</param>
    /// <param name="input">I    input vector       [2 * len]</param>
    /// <param name="len">I    number of OUTPUT samples</param>
    fun combine_pulses(
        output: IntArray,
        input: IntArray,
        len: Int
    ) {
        var k: Int
        k = 0
        while (k < len) {
            output[k] = input[2 * k] + input[2 * k + 1]
            k++
        }
    }

    fun encode_split(
        psRangeEnc: EntropyCoder, /* I/O  compressor data structure                   */
        p_child1: Int, /* I    pulse amplitude of first child subframe     */
        p: Int, /* I    pulse amplitude of current subframe         */
        shell_table: ShortArray /* I    table of shell cdfs                         */
    ) {
        if (p > 0) {
            psRangeEnc.enc_icdf(p_child1, shell_table, SilkTables.silk_shell_code_table_offsets[p].toInt(), 8)
        }
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="p_child1">O    pulse amplitude of first child subframe</param>
    /// <param name="p_child2">O    pulse amplitude of second child subframe</param>
    /// <param name="psRangeDec">I/O  Compressor data structure</param>
    /// <param name="p">I    pulse amplitude of current subframe</param>
    /// <param name="shell_table">I    table of shell cdfs</param>
    fun decode_split(
        p_child1: ShortArray,
        child1_ptr: Int,
        p_child2: ShortArray,
        p_child2_ptr: Int,
        psRangeDec: EntropyCoder,
        p: Int,
        shell_table: ShortArray
    ) {
        if (p > 0) {
            p_child1[child1_ptr] =
                    psRangeDec.dec_icdf(shell_table, SilkTables.silk_shell_code_table_offsets[p].toInt(), 8).toShort()
            p_child2[p_child2_ptr] = (p - p_child1[child1_ptr]).toShort()
        } else {
            p_child1[child1_ptr] = 0
            p_child2[p_child2_ptr] = 0
        }
    }

    /// <summary>
    /// Shell encoder, operates on one shell code frame of 16 pulses
    /// </summary>
    /// <param name="psRangeEnc">I/O  compressor data structure</param>
    /// <param name="pulses0">I    data: nonnegative pulse amplitudes</param>
    fun silk_shell_encoder(psRangeEnc: EntropyCoder, pulses0: IntArray, pulses0_ptr: Int) {
        val pulses1 = IntArray(8)
        val pulses2 = IntArray(4)
        val pulses3 = IntArray(2)
        val pulses4 = IntArray(1)

        /* this function operates on one shell code frame of 16 pulses */
        Inlines.OpusAssert(SilkConstants.SHELL_CODEC_FRAME_LENGTH == 16)

        /* tree representation per pulse-subframe */
        combine_pulses(pulses1, pulses0, pulses0_ptr, 8)
        combine_pulses(pulses2, pulses1, 4)
        combine_pulses(pulses3, pulses2, 2)
        combine_pulses(pulses4, pulses3, 1)

        encode_split(psRangeEnc, pulses3[0], pulses4[0], SilkTables.silk_shell_code_table3)

        encode_split(psRangeEnc, pulses2[0], pulses3[0], SilkTables.silk_shell_code_table2)

        encode_split(psRangeEnc, pulses1[0], pulses2[0], SilkTables.silk_shell_code_table1)
        encode_split(psRangeEnc, pulses0[pulses0_ptr], pulses1[0], SilkTables.silk_shell_code_table0)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 2], pulses1[1], SilkTables.silk_shell_code_table0)

        encode_split(psRangeEnc, pulses1[2], pulses2[1], SilkTables.silk_shell_code_table1)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 4], pulses1[2], SilkTables.silk_shell_code_table0)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 6], pulses1[3], SilkTables.silk_shell_code_table0)

        encode_split(psRangeEnc, pulses2[2], pulses3[1], SilkTables.silk_shell_code_table2)

        encode_split(psRangeEnc, pulses1[4], pulses2[2], SilkTables.silk_shell_code_table1)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 8], pulses1[4], SilkTables.silk_shell_code_table0)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 10], pulses1[5], SilkTables.silk_shell_code_table0)

        encode_split(psRangeEnc, pulses1[6], pulses2[3], SilkTables.silk_shell_code_table1)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 12], pulses1[6], SilkTables.silk_shell_code_table0)
        encode_split(psRangeEnc, pulses0[pulses0_ptr + 14], pulses1[7], SilkTables.silk_shell_code_table0)
    }


    /* Shell decoder, operates on one shell code frame of 16 pulses */
    fun silk_shell_decoder(
        pulses0: ShortArray, /* O    data: nonnegative pulse amplitudes          */
        pulses0_ptr: Int,
        psRangeDec: EntropyCoder, /* I/O  Compressor data structure                   */
        pulses4: Int /* I    number of pulses per pulse-subframe         */
    ) {
        val pulses1 = ShortArray(8)
        val pulses2 = ShortArray(4)
        val pulses3 = ShortArray(2)

        /* this function operates on one shell code frame of 16 pulses */
        Inlines.OpusAssert(SilkConstants.SHELL_CODEC_FRAME_LENGTH == 16)

        decode_split(pulses3, 0, pulses3, 1, psRangeDec, pulses4, SilkTables.silk_shell_code_table3)

        decode_split(pulses2, 0, pulses2, 1, psRangeDec, pulses3[0].toInt(), SilkTables.silk_shell_code_table2)

        decode_split(pulses1, 0, pulses1, 1, psRangeDec, pulses2[0].toInt(), SilkTables.silk_shell_code_table1)
        decode_split(
            pulses0,
            pulses0_ptr,
            pulses0,
            pulses0_ptr + 1,
            psRangeDec,
            pulses1[0].toInt(),
            SilkTables.silk_shell_code_table0
        )
        decode_split(
            pulses0,
            pulses0_ptr + 2,
            pulses0,
            pulses0_ptr + 3,
            psRangeDec,
            pulses1[1].toInt(),
            SilkTables.silk_shell_code_table0
        )

        decode_split(pulses1, 2, pulses1, 3, psRangeDec, pulses2[1].toInt(), SilkTables.silk_shell_code_table1)
        decode_split(
            pulses0,
            pulses0_ptr + 4,
            pulses0,
            pulses0_ptr + 5,
            psRangeDec,
            pulses1[2].toInt(),
            SilkTables.silk_shell_code_table0
        )
        decode_split(
            pulses0,
            pulses0_ptr + 6,
            pulses0,
            pulses0_ptr + 7,
            psRangeDec,
            pulses1[3].toInt(),
            SilkTables.silk_shell_code_table0
        )

        decode_split(pulses2, 2, pulses2, 3, psRangeDec, pulses3[1].toInt(), SilkTables.silk_shell_code_table2)

        decode_split(pulses1, 4, pulses1, 5, psRangeDec, pulses2[2].toInt(), SilkTables.silk_shell_code_table1)
        decode_split(
            pulses0,
            pulses0_ptr + 8,
            pulses0,
            pulses0_ptr + 9,
            psRangeDec,
            pulses1[4].toInt(),
            SilkTables.silk_shell_code_table0
        )
        decode_split(
            pulses0,
            pulses0_ptr + 10,
            pulses0,
            pulses0_ptr + 11,
            psRangeDec,
            pulses1[5].toInt(),
            SilkTables.silk_shell_code_table0
        )

        decode_split(pulses1, 6, pulses1, 7, psRangeDec, pulses2[3].toInt(), SilkTables.silk_shell_code_table1)
        decode_split(
            pulses0,
            pulses0_ptr + 12,
            pulses0,
            pulses0_ptr + 13,
            psRangeDec,
            pulses1[6].toInt(),
            SilkTables.silk_shell_code_table0
        )
        decode_split(
            pulses0,
            pulses0_ptr + 14,
            pulses0,
            pulses0_ptr + 15,
            psRangeDec,
            pulses1[7].toInt(),
            SilkTables.silk_shell_code_table0
        )
    }
}
