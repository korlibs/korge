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

internal object VQ_WMat_EC {

    /* Entropy constrained matrix-weighted VQ, hard-coded to 5-element vectors, for a single input data vector */
    fun silk_VQ_WMat_EC(
        ind: BoxedValueByte, /* O    index of best codebook vector               */
        rate_dist_Q14: BoxedValueInt, /* O    best weighted quant error + mu * rate       */
        gain_Q7: BoxedValueInt, /* O    sum of absolute LTP coefficients            */
        in_Q14: ShortArray, /* I    input vector to be quantized                */
        in_Q14_ptr: Int,
        W_Q18: IntArray, /* I    weighting matrix                            */
        W_Q18_ptr: Int,
        cb_Q7: Array<ByteArray>, /* I    codebook                                    */
        cb_gain_Q7: ShortArray, /* I    codebook effective gain                     */
        cl_Q5: ShortArray, /* I    code length for each codebook vector        */
        mu_Q9: Int, /* I    tradeoff betw. weighted error and rate      */
        max_gain_Q7: Int, /* I    maximum sum of absolute LTP coefficients    */
        L: Int /* I    number of vectors in codebook               */
    ) {
        var k: Int
        var gain_tmp_Q7: Int
        var cb_row_Q7: ByteArray
        var cb_row_Q7_ptr = 0
        val diff_Q14 = ShortArray(5)
        var sum1_Q14: Int
        var sum2_Q16: Int

        /* Loop over codebook */
        rate_dist_Q14.Val = Int.MAX_VALUE
        k = 0
        while (k < L) {
            /* Go to next cbk vector */
            cb_row_Q7 = cb_Q7[cb_row_Q7_ptr++]
            gain_tmp_Q7 = cb_gain_Q7[k].toInt()

            diff_Q14[0] = (in_Q14[in_Q14_ptr] - Inlines.silk_LSHIFT(cb_row_Q7[0].toInt(), 7)).toShort()
            diff_Q14[1] = (in_Q14[in_Q14_ptr + 1] - Inlines.silk_LSHIFT(cb_row_Q7[1].toInt(), 7)).toShort()
            diff_Q14[2] = (in_Q14[in_Q14_ptr + 2] - Inlines.silk_LSHIFT(cb_row_Q7[2].toInt(), 7)).toShort()
            diff_Q14[3] = (in_Q14[in_Q14_ptr + 3] - Inlines.silk_LSHIFT(cb_row_Q7[3].toInt(), 7)).toShort()
            diff_Q14[4] = (in_Q14[in_Q14_ptr + 4] - Inlines.silk_LSHIFT(cb_row_Q7[4].toInt(), 7)).toShort()

            /* Weighted rate */
            sum1_Q14 = Inlines.silk_SMULBB(mu_Q9, cl_Q5[k].toInt())

            /* Penalty for too large gain */
            sum1_Q14 = Inlines.silk_ADD_LSHIFT32(
                sum1_Q14,
                Inlines.silk_max(Inlines.silk_SUB32(gain_tmp_Q7, max_gain_Q7), 0),
                10
            )

            Inlines.OpusAssert(sum1_Q14 >= 0)

            /* first row of W_Q18 */
            sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 1], diff_Q14[1].toInt())
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 2], diff_Q14[2].toInt())
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 3], diff_Q14[3].toInt())
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 4], diff_Q14[4].toInt())
            sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1)
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr], diff_Q14[0].toInt())
            sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[0].toInt())

            /* second row of W_Q18 */
            sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 7], diff_Q14[2].toInt())
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 8], diff_Q14[3].toInt())
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 9], diff_Q14[4].toInt())
            sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1)
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 6], diff_Q14[1].toInt())
            sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[1].toInt())

            /* third row of W_Q18 */
            sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 13], diff_Q14[3].toInt())
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 14], diff_Q14[4].toInt())
            sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1)
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 12], diff_Q14[2].toInt())
            sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[2].toInt())

            /* fourth row of W_Q18 */
            sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 19], diff_Q14[4].toInt())
            sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1)
            sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 18], diff_Q14[3].toInt())
            sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[3].toInt())

            /* last row of W_Q18 */
            sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 24], diff_Q14[4].toInt())
            sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[4].toInt())

            Inlines.OpusAssert(sum1_Q14 >= 0)

            /* find best */
            if (sum1_Q14 < rate_dist_Q14.Val) {
                rate_dist_Q14.Val = sum1_Q14
                ind.Val = k.toByte()
                gain_Q7.Val = gain_tmp_Q7
            }
            k++
        }
    }
}
