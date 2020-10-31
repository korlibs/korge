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

internal object Autocorrelation {

    private val QC = 10
    private val QS = 14

    /* Compute autocorrelation */
    fun silk_autocorr(
        results: IntArray, /* O    Result (length correlationCount)                            */
        scale: BoxedValueInt, /* O    Scaling of the correlation vector                           */
        inputData: ShortArray, /* I    Input data to correlate                                     */
        inputDataSize: Int, /* I    Length of input                                             */
        correlationCount: Int /* I    Number of correlation taps to compute                       */
    ) {
        val corrCount = Inlines.silk_min_int(inputDataSize, correlationCount)
        scale.Val = Autocorrelation._celt_autocorr(inputData, results, corrCount - 1, inputDataSize)
    }

    fun _celt_autocorr(
        x: ShortArray, /*  in: [0...n-1] samples x   */
        ac: IntArray, /* out: [0...lag-1] ac values */
        lag: Int,
        n: Int
    ): Int {
        var d: Int
        var i: Int
        var k: Int
        val fastN = n - lag
        var shift: Int
        var xptr: ShortArray
        val xx = ShortArray(n)
        Inlines.OpusAssert(n > 0)
        xptr = x

        shift = 0
        run {
            var ac0: Int
            ac0 = 1 + (n shl 7)
            if (n and 1 != 0) {
                ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[0], xptr[0]), 9)
            }
            i = n and 1
            while (i < n) {
                ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i], xptr[i]), 9)
                ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i + 1], xptr[i + 1]), 9)
                i += 2
            }
            shift = Inlines.celt_ilog2(ac0) - 30 + 10
            shift = shift / 2
            if (shift > 0) {
                i = 0
                while (i < n) {
                    xx[i] = Inlines.PSHR32(xptr[i].toInt(), shift).toShort()
                    i++
                }
                xptr = xx
            } else {
                shift = 0
            }
        }
        CeltPitchXCorr.pitch_xcorr(xptr, xptr, ac, fastN, lag + 1)
        k = 0
        while (k <= lag) {
            i = k + fastN
            d = 0
            while (i < n) {
                d = Inlines.MAC16_16(d, xptr[i], xptr[i - k])
                i++
            }
            ac[k] += d
            k++
        }
        shift = 2 * shift
        if (shift <= 0) {
            ac[0] += Inlines.SHL32(1, -shift)
        }
        if (ac[0] < 268435456) {
            val shift2 = 29 - Inlines.EC_ILOG(ac[0].toLong())
            i = 0
            while (i <= lag) {
                ac[i] = Inlines.SHL32(ac[i], shift2)
                i++
            }
            shift -= shift2
        } else if (ac[0] >= 536870912) {
            var shift2 = 1
            if (ac[0] >= 1073741824) {
                shift2++
            }
            i = 0
            while (i <= lag) {
                ac[i] = Inlines.SHR32(ac[i], shift2)
                i++
            }
            shift += shift2
        }

        return shift
    }

    fun _celt_autocorr(
		x: IntArray, /*  in: [0...n-1] samples x   */
        ac: IntArray, /* out: [0...lag-1] ac values */
        window: IntArray?,
		overlap: Int,
		lag: Int,
		n: Int
    ): Int {
        var d: Int
        var i: Int
        var k: Int
        val fastN = n - lag
        var shift: Int
        var xptr: IntArray
        val xx = IntArray(n)

        Inlines.OpusAssert(n > 0)
        Inlines.OpusAssert(overlap >= 0)

        if (overlap == 0) {
            xptr = x
        } else {
            i = 0
            while (i < n) {
                xx[i] = x[i]
                i++
            }
            i = 0
            while (i < overlap) {
                xx[i] = Inlines.MULT16_16_Q15(x[i], window!![i])
                xx[n - i - 1] = Inlines.MULT16_16_Q15(x[n - i - 1], window[i])
                i++
            }
            xptr = xx
        }

        shift = 0

        var ac0: Int
        ac0 = 1 + (n shl 7)
        if (n and 1 != 0) {
            ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[0], xptr[0]), 9)
        }

        i = n and 1
        while (i < n) {
            ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i], xptr[i]), 9)
            ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i + 1], xptr[i + 1]), 9)
            i += 2
        }

        shift = Inlines.celt_ilog2(ac0) - 30 + 10
        shift = shift / 2
        if (shift > 0) {
            i = 0
            while (i < n) {
                xx[i] = Inlines.PSHR32(xptr[i], shift)
                i++
            }
            xptr = xx
        } else {
            shift = 0
        }

        CeltPitchXCorr.pitch_xcorr(xptr, xptr, ac, fastN, lag + 1)
        k = 0
        while (k <= lag) {
            i = k + fastN
            d = 0
            while (i < n) {
                d = Inlines.MAC16_16(d, xptr[i], xptr[i - k])
                i++
            }
            ac[k] += d
            k++
        }

        shift = 2 * shift
        if (shift <= 0) {
            ac[0] += Inlines.SHL32(1, -shift)
        }
        if (ac[0] < 268435456) {
            val shift2 = 29 - Inlines.EC_ILOG(ac[0].toLong())
            i = 0
            while (i <= lag) {
                ac[i] = Inlines.SHL32(ac[i], shift2)
                i++
            }
            shift -= shift2
        } else if (ac[0] >= 536870912) {
            var shift2 = 1
            if (ac[0] >= 1073741824) {
                shift2++
            }
            i = 0
            while (i <= lag) {
                ac[i] = Inlines.SHR32(ac[i], shift2)
                i++
            }
            shift += shift2
        }

        return shift
    }

    /* Autocorrelations for a warped frequency axis */
    fun silk_warped_autocorrelation(
        corr: IntArray, /* O    Result [order + 1]                                                          */
        scale: BoxedValueInt, /* O    Scaling of the correlation vector                                           */
        input: ShortArray, /* I    Input data to correlate                                                     */
        warping_Q16: Int, /* I    Warping coefficient                                                         */
        length: Int, /* I    Length of input                                                             */
        order: Int /* I    Correlation order (even)                                                    */
    ) {
        var n: Int
        var i: Int
        var lsh: Int
        var tmp1_QS: Int
        var tmp2_QS: Int
        val state_QS = IntArray(SilkConstants.MAX_SHAPE_LPC_ORDER + 1)// = { 0 };
        val corr_QC = LongArray(SilkConstants.MAX_SHAPE_LPC_ORDER + 1)// = { 0 };

        /* Order must be even */
        Inlines.OpusAssert(order and 1 == 0)
        Inlines.OpusAssert(2 * QS - QC >= 0)

        /* Loop over samples */
        n = 0
        while (n < length) {
            tmp1_QS = Inlines.silk_LSHIFT32(input[n].toInt(), QS)
            /* Loop over allpass sections */
            i = 0
            while (i < order) {
                /* Output of allpass section */
                tmp2_QS = Inlines.silk_SMLAWB(state_QS[i], state_QS[i + 1] - tmp1_QS, warping_Q16)
                state_QS[i] = tmp1_QS
                corr_QC[i] += Inlines.silk_RSHIFT64(Inlines.silk_SMULL(tmp1_QS, state_QS[0]), 2 * QS - QC)
                /* Output of allpass section */
                tmp1_QS = Inlines.silk_SMLAWB(state_QS[i + 1], state_QS[i + 2] - tmp2_QS, warping_Q16)
                state_QS[i + 1] = tmp2_QS
                corr_QC[i + 1] += Inlines.silk_RSHIFT64(Inlines.silk_SMULL(tmp2_QS, state_QS[0]), 2 * QS - QC)
                i += 2
            }
            state_QS[order] = tmp1_QS
            corr_QC[order] += Inlines.silk_RSHIFT64(Inlines.silk_SMULL(tmp1_QS, state_QS[0]), 2 * QS - QC)
            n++
        }

        lsh = Inlines.silk_CLZ64(corr_QC[0]) - 35
        lsh = Inlines.silk_LIMIT(lsh, -12 - QC, 30 - QC)
        scale.Val = -(QC + lsh)
        Inlines.OpusAssert(scale.Val >= -30 && scale.Val <= 12)
        if (lsh >= 0) {
            i = 0
            while (i < order + 1) {
                corr[i] = Inlines.silk_LSHIFT64(corr_QC[i], lsh).toInt()
                i++
            }
        } else {
            i = 0
            while (i < order + 1) {
                corr[i] = Inlines.silk_RSHIFT64(corr_QC[i], -lsh).toInt()
                i++
            }
        }
        Inlines.OpusAssert(corr_QC[0] >= 0)
        /* If breaking, decrease QC*/
    }
}
