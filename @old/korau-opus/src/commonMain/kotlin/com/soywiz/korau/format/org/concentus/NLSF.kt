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

import com.soywiz.kmem.*
import com.soywiz.korau.format.org.concentus.internal.*
import com.soywiz.korio.lang.*

/// <summary>
/// Normalized line spectrum frequency processor
/// </summary>
internal object NLSF {

    private val MAX_STABILIZE_LOOPS = 20

    private val QA = 16

    /// <summary>
    /// Number of binary divisions, when not in low complexity mode
    /// </summary>
    private val BIN_DIV_STEPS_A2NLSF = 3
    /* must be no higher than 16 - log2( LSF_COS_TAB_SZ ) */

    private val MAX_ITERATIONS_A2NLSF = 30

    /* This ordering was found to maximize quality. It improves numerical accuracy of
           silk_NLSF2A_find_poly() compared to "standard" ordering. */
    private val ordering16 = byteArrayOf(0, 15, 8, 7, 4, 11, 12, 3, 2, 13, 10, 5, 6, 9, 14, 1)
    private val ordering10 = byteArrayOf(0, 9, 6, 3, 4, 5, 8, 1, 2, 7)

    /// <summary>
    /// Compute quantization errors for an LPC_order element input vector for a VQ codebook
    /// </summary>
    /// <param name="err_Q26">(O) Quantization errors [K]</param>
    /// <param name="in_Q15">(I) Input vectors to be quantized [LPC_order]</param>
    /// <param name="pCB_Q8">(I) Codebook vectors [K*LPC_order]</param>
    /// <param name="K">(I) Number of codebook vectors</param>
    /// <param name="LPC_order">(I) Number of LPCs</param>
    fun silk_NLSF_VQ(err_Q26: IntArray?, in_Q15: ShortArray, pCB_Q8: ShortArray?, K: Int, LPC_order: Int) {
        var diff_Q15: Int
        var sum_error_Q30: Int
        var sum_error_Q26: Int
        var pCB_idx = 0

        Inlines.OpusAssert(err_Q26 != null)
        Inlines.OpusAssert(LPC_order <= 16)
        Inlines.OpusAssert(LPC_order and 1 == 0)

        // Loop over codebook
        for (i in 0 until K) {
            sum_error_Q26 = 0

            var m = 0
            while (m < LPC_order) {
                // Compute weighted squared quantization error for index m
                diff_Q15 = Inlines.silk_SUB_LSHIFT32(
                    in_Q15[m].toInt(),
                    pCB_Q8!![pCB_idx++].toInt(),
                    7
                ) // range: [ -32767 : 32767 ]
                sum_error_Q30 = Inlines.silk_SMULBB(diff_Q15, diff_Q15)

                // Compute weighted squared quantization error for index m + 1
                diff_Q15 = Inlines.silk_SUB_LSHIFT32(
                    in_Q15[m + 1].toInt(),
                    pCB_Q8[pCB_idx++].toInt(),
                    7
                ) // range: [ -32767 : 32767 ]
                sum_error_Q30 = Inlines.silk_SMLABB(sum_error_Q30, diff_Q15, diff_Q15)

                sum_error_Q26 = Inlines.silk_ADD_RSHIFT32(sum_error_Q26, sum_error_Q30, 4)

                Inlines.OpusAssert(sum_error_Q26 >= 0)
                Inlines.OpusAssert(sum_error_Q30 >= 0)
                m += 2
            }

            err_Q26!![i] = sum_error_Q26
        }
    }

    /// <summary>
    /// Laroia low complexity NLSF weights
    /// </summary>
    /// <param name="pNLSFW_Q_OUT">(O) Pointer to input vector weights [D]</param>
    /// <param name="pNLSF_Q15">(I) Pointer to input vector [D]</param>
    /// <param param name="D">(I) Input vector dimension (even)</param>
    fun silk_NLSF_VQ_weights_laroia(pNLSFW_Q_OUT: ShortArray, pNLSF_Q15: ShortArray, D: Int) {
        var k: Int
        var tmp1_int: Int
        var tmp2_int: Int

        Inlines.OpusAssert(pNLSFW_Q_OUT != null)
        Inlines.OpusAssert(D > 0)
        Inlines.OpusAssert(D and 1 == 0)

        // First value
        tmp1_int = Inlines.silk_max_int(pNLSF_Q15[0].toInt(), 1)
        tmp1_int = Inlines.silk_DIV32(1 shl 15 + SilkConstants.NLSF_W_Q, tmp1_int)
        tmp2_int = Inlines.silk_max_int(pNLSF_Q15[1] - pNLSF_Q15[0], 1)
        tmp2_int = Inlines.silk_DIV32(1 shl 15 + SilkConstants.NLSF_W_Q, tmp2_int)
        pNLSFW_Q_OUT[0] = Inlines.silk_min_int(tmp1_int + tmp2_int, Short.MAX_VALUE.toInt()).toShort()

        Inlines.OpusAssert(pNLSFW_Q_OUT[0] > 0)

        // Main loop
        k = 1
        while (k < D - 1) {
            tmp1_int = Inlines.silk_max_int(pNLSF_Q15[k + 1] - pNLSF_Q15[k], 1)
            tmp1_int = Inlines.silk_DIV32(1 shl 15 + SilkConstants.NLSF_W_Q, tmp1_int)
            pNLSFW_Q_OUT[k] = Inlines.silk_min_int(tmp1_int + tmp2_int, Short.MAX_VALUE.toInt()).toShort()
            Inlines.OpusAssert(pNLSFW_Q_OUT[k] > 0)

            tmp2_int = Inlines.silk_max_int(pNLSF_Q15[k + 2] - pNLSF_Q15[k + 1], 1)
            tmp2_int = Inlines.silk_DIV32(1 shl 15 + SilkConstants.NLSF_W_Q, tmp2_int)
            pNLSFW_Q_OUT[k + 1] = Inlines.silk_min_int(tmp1_int + tmp2_int, Short.MAX_VALUE.toInt()).toShort()
            Inlines.OpusAssert(pNLSFW_Q_OUT[k + 1] > 0)
            k += 2
        }

        // Last value
        tmp1_int = Inlines.silk_max_int((1 shl 15) - pNLSF_Q15[D - 1], 1)
        tmp1_int = Inlines.silk_DIV32(1 shl 15 + SilkConstants.NLSF_W_Q, tmp1_int)
        pNLSFW_Q_OUT[D - 1] = Inlines.silk_min_int(tmp1_int + tmp2_int, Short.MAX_VALUE.toInt()).toShort()

        Inlines.OpusAssert(pNLSFW_Q_OUT[D - 1] > 0)
    }

    /// <summary>
    /// Returns RD value in Q30
    /// </summary>
    /// <param name="x_Q10">(O) Output [ order ]</param>
    /// <param name="indices">(I) Quantization indices [ order ]</param>
    /// <param name="pred_coef_Q8">(I) Backward predictor coefs [ order ]</param>
    /// <param name="quant_step_size_Q16">(I) Quantization step size</param>
    /// <param name="order">(I) Number of input values</param>
    fun silk_NLSF_residual_dequant(
        x_Q10: ShortArray,
        indices: ByteArray,
        indices_ptr: Int,
        pred_coef_Q8: ShortArray,
        quant_step_size_Q16: Int,
        order: Short
    ) {
        var i: Int
        var pred_Q10: Int
        var out_Q10: Short

        out_Q10 = 0
        i = order - 1
        while (i >= 0) {
            pred_Q10 = Inlines.silk_RSHIFT(Inlines.silk_SMULBB(out_Q10.toInt(), pred_coef_Q8[i].toInt()), 8)
            out_Q10 = Inlines.silk_LSHIFT16(indices[indices_ptr + i].toShort(), 10)
            if (out_Q10 > 0) {
                out_Q10 = Inlines.silk_SUB16(
                    out_Q10,
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                )
            } else if (out_Q10 < 0) {
                out_Q10 = Inlines.silk_ADD16(
                    out_Q10,
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                )
            }
            out_Q10 = Inlines.silk_SMLAWB(pred_Q10, out_Q10.toInt(), quant_step_size_Q16).toShort()
            x_Q10[i] = out_Q10
            i--
        }
    }

    /// <summary>
    /// Unpack predictor values and indices for entropy coding tables
    /// </summary>
    /// <param name="ec_ix">(O) Indices to entropy tables [ LPC_ORDER ]</param>
    /// <param name="pred_Q8">(O) LSF predictor [ LPC_ORDER ]</param>
    /// <param name="psNLSF_CB">(I) Codebook object</param>
    /// <param name="CB1_index">(I) Index of vector in first LSF codebook</param>
    fun silk_NLSF_unpack(ec_ix: ShortArray, pred_Q8: ShortArray, psNLSF_CB: NLSFCodebook, CB1_index: Int) {
        var i: Int
        var entry: Short
        val ec_sel = psNLSF_CB.ec_sel
        var ec_sel_ptr = CB1_index * psNLSF_CB.order / 2

        i = 0
        while (i < psNLSF_CB.order) {
            entry = ec_sel!![ec_sel_ptr]
            ec_sel_ptr++
            ec_ix[i] = Inlines.silk_SMULBB(
                Inlines.silk_RSHIFT(entry.toInt(), 1) and 7,
                2 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE + 1
            ).toShort()
            pred_Q8[i] = psNLSF_CB.pred_Q8!![i + (entry and 1) * (psNLSF_CB.order - 1)]
            ec_ix[i + 1] = Inlines.silk_SMULBB(
                Inlines.silk_RSHIFT(entry.toInt(), 5) and 7,
                2 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE + 1
            ).toShort()
            pred_Q8[i + 1] =
                    psNLSF_CB.pred_Q8!![i + (Inlines.silk_RSHIFT(entry.toInt(), 4) and 1) * (psNLSF_CB.order - 1) + 1]
            i += 2
        }
    }

    /// <summary>
    /// NLSF stabilizer, for a single input data vector
    /// </summary>
    /// <param name="NLSF_Q15">(I/O) Unstable/stabilized normalized LSF vector in Q15 [L]</param>
    /// <param name="NDeltaMin_Q15">(I) Min distance vector, NDeltaMin_Q15[L] must be >= 1 [L+1]</param>
    /// <param name="L">(I) Number of NLSF parameters in the input vector</param>
    fun silk_NLSF_stabilize(NLSF_Q15: ShortArray, NDeltaMin_Q15: ShortArray, L: Int) {
        var i: Int
        var I = 0
        var k: Int
        var loops: Int
        var center_freq_Q15: Short
        var diff_Q15: Int
        var min_diff_Q15: Int
        var min_center_Q15: Int
        var max_center_Q15: Int

        // This is necessary to ensure an output within range of a short
        Inlines.OpusAssert(NDeltaMin_Q15[L] >= 1)

        loops = 0
        while (loops < MAX_STABILIZE_LOOPS) {
            /**
             * ***********************
             */
            /* Find smallest distance */
            /**
             * ***********************
             */
            // First element
            min_diff_Q15 = NLSF_Q15[0] - NDeltaMin_Q15[0]
            I = 0

            // Middle elements
            i = 1
            while (i <= L - 1) {
                diff_Q15 = NLSF_Q15[i] - (NLSF_Q15[i - 1] + NDeltaMin_Q15[i])
                if (diff_Q15 < min_diff_Q15) {
                    min_diff_Q15 = diff_Q15
                    I = i
                }
                i++
            }

            // Last element
            diff_Q15 = (1 shl 15) - (NLSF_Q15[L - 1] + NDeltaMin_Q15[L])
            if (diff_Q15 < min_diff_Q15) {
                min_diff_Q15 = diff_Q15
                I = L
            }

            /**
             * ************************************************
             */
            /* Now check if the smallest distance non-negative */
            /**
             * ************************************************
             */
            if (min_diff_Q15 >= 0) {
                return
            }

            if (I == 0) {
                // Move away from lower limit
                NLSF_Q15[0] = NDeltaMin_Q15[0]
            } else if (I == L) {
                // Move away from higher limit
                NLSF_Q15[L - 1] = ((1 shl 15) - NDeltaMin_Q15[L]).toShort()
            } else {
                // Find the lower extreme for the location of the current center frequency
                min_center_Q15 = 0
                k = 0
                while (k < I) {
                    min_center_Q15 += NDeltaMin_Q15[k].toInt()
                    k++
                }

                min_center_Q15 += Inlines.silk_RSHIFT(NDeltaMin_Q15[I].toInt(), 1)

                // Find the upper extreme for the location of the current center frequency
                max_center_Q15 = 1 shl 15
                k = L
                while (k > I) {
                    max_center_Q15 -= NDeltaMin_Q15[k].toInt()
                    k--
                }

                max_center_Q15 -= Inlines.silk_RSHIFT(NDeltaMin_Q15[I].toInt(), 1)

                // Move apart, sorted by value, keeping the same center frequency
                center_freq_Q15 = Inlines.silk_LIMIT_32(
                    Inlines.silk_RSHIFT_ROUND(NLSF_Q15[I - 1].toInt() + NLSF_Q15[I].toInt(), 1),
                    min_center_Q15, max_center_Q15
                ).toShort()
                NLSF_Q15[I - 1] = (center_freq_Q15 - Inlines.silk_RSHIFT(NDeltaMin_Q15[I].toInt(), 1)).toShort()
                NLSF_Q15[I] = (NLSF_Q15[I - 1] + NDeltaMin_Q15[I]).toShort()
            }
            loops++
        }

        // Safe and simple fall back method, which is less ideal than the above
        if (loops == MAX_STABILIZE_LOOPS) {
            Sort.silk_insertion_sort_increasing_all_values_int16(NLSF_Q15, L)

            // First NLSF should be no less than NDeltaMin[0]
            NLSF_Q15[0] = Inlines.silk_max_int(NLSF_Q15[0].toInt(), NDeltaMin_Q15[0].toInt()).toShort()

            // Keep delta_min distance between the NLSFs
            i = 1
            while (i < L) {
                NLSF_Q15[i] = Inlines.silk_max_int(NLSF_Q15[i].toInt(), NLSF_Q15[i - 1] + NDeltaMin_Q15[i]).toShort()
                i++
            }

            // Last NLSF should be no higher than 1 - NDeltaMin[L]
            NLSF_Q15[L - 1] = Inlines.silk_min_int(NLSF_Q15[L - 1].toInt(), (1 shl 15) - NDeltaMin_Q15[L]).toShort()

            // Keep NDeltaMin distance between the NLSFs
            i = L - 2
            while (i >= 0) {
                NLSF_Q15[i] =
                        Inlines.silk_min_int(NLSF_Q15[i].toInt(), NLSF_Q15[i + 1] - NDeltaMin_Q15[i + 1]).toShort()
                i--
            }
        }
    }

    /// <summary>
    /// NLSF vector decoder
    /// </summary>
    /// <param name="pNLSF_Q15">(O) Quantized NLSF vector [ LPC_ORDER ]</param>
    /// <param name="NLSFIndices">(I) Codebook path vector [ LPC_ORDER + 1 ]</param>
    /// <param name="psNLSF_CB">(I) Codebook object</param>
    fun silk_NLSF_decode(pNLSF_Q15: ShortArray, NLSFIndices: ByteArray, psNLSF_CB: NLSFCodebook) {
        var i: Int
        val pred_Q8 = ShortArray(psNLSF_CB.order.toInt())
        val ec_ix = ShortArray(psNLSF_CB.order.toInt())
        val res_Q10 = ShortArray(psNLSF_CB.order.toInt())
        val W_tmp_QW = ShortArray(psNLSF_CB.order.toInt())
        var W_tmp_Q9: Int
        var NLSF_Q15_tmp: Int

        // Decode first stage
        val pCB = psNLSF_CB.CB1_NLSF_Q8
        val pCB_element = NLSFIndices[0] * psNLSF_CB.order

        i = 0
        while (i < psNLSF_CB.order) {
            pNLSF_Q15[i] = Inlines.silk_LSHIFT16(pCB!![pCB_element + i].toShort(), 7)
            i++
        }

        // Unpack entropy table indices and predictor for current CB1 index
        silk_NLSF_unpack(ec_ix, pred_Q8, psNLSF_CB, NLSFIndices[0].toInt())

        // Predictive residual dequantizer
        silk_NLSF_residual_dequant(
            res_Q10,
            NLSFIndices,
            1,
            pred_Q8,
            psNLSF_CB.quantStepSize_Q16.toInt(),
            psNLSF_CB.order
        )

        // Weights from codebook vector
        silk_NLSF_VQ_weights_laroia(W_tmp_QW, pNLSF_Q15, psNLSF_CB.order.toInt())

        // Apply inverse square-rooted weights and add to output
        i = 0
        while (i < psNLSF_CB.order) {
            W_tmp_Q9 = Inlines.silk_SQRT_APPROX(Inlines.silk_LSHIFT(W_tmp_QW[i].toInt(), 18 - SilkConstants.NLSF_W_Q))
            NLSF_Q15_tmp = Inlines.silk_ADD32(
                pNLSF_Q15[i].toInt(),
                Inlines.silk_DIV32_16(Inlines.silk_LSHIFT(res_Q10[i].toInt(), 14), W_tmp_Q9.toShort().toInt())
            )
            pNLSF_Q15[i] = Inlines.silk_LIMIT(NLSF_Q15_tmp, 0, 32767).toShort()
            i++
        }

        // NLSF stabilization
        silk_NLSF_stabilize(pNLSF_Q15, psNLSF_CB.deltaMin_Q15!!, psNLSF_CB.order.toInt())
    }

    /// <summary>
    /// Delayed-decision quantizer for NLSF residuals
    /// </summary>
    /// <param name="indices">(O) Quantization indices [ order ]</param>
    /// <param name="x_Q10">(O) Input [ order ]</param>
    /// <param name="w_Q5">(I) Weights [ order ] </param>
    /// <param name="pred_coef_Q8">(I) Backward predictor coefs [ order ]</param>
    /// <param name="ec_ix">(I) Indices to entropy coding tables [ order ]</param>
    /// <param name="ec_rates_Q5">(I) Rates []</param>
    /// <param name="quant_step_size_Q16">(I) Quantization step size</param>
    /// <param name="inv_quant_step_size_Q6">(I) Inverse quantization step size</param>
    /// <param name="mu_Q20">(I) R/D tradeoff</param>
    /// <param name="order">(I) Number of input values</param>
    /// <returns>RD value in Q25</returns>
    /// Fixme: Optimize this method!
    fun silk_NLSF_del_dec_quant(
        indices: ByteArray,
        x_Q10: ShortArray,
        w_Q5: ShortArray,
        pred_coef_Q8: ShortArray,
        ec_ix: ShortArray,
        ec_rates_Q5: ShortArray?,
        quant_step_size_Q16: Int,
        inv_quant_step_size_Q6: Short,
        mu_Q20: Int,
        order: Short
    ): Int {
        var i: Int
        var j: Int
        var nStates: Int
        var ind_tmp: Int
        var ind_min_max: Int
        var ind_max_min: Int
        var in_Q10: Int
        var res_Q10: Int
        var pred_Q10: Int
        var diff_Q10: Int
        var out0_Q10: Int
        var out1_Q10: Int
        var rate0_Q5: Int
        var rate1_Q5: Int
        var RD_tmp_Q25: Int
        var min_Q25: Int
        var min_max_Q25: Int
        var max_min_Q25: Int
        var pred_coef_Q16: Int
        val ind_sort = IntArray(SilkConstants.NLSF_QUANT_DEL_DEC_STATES)
        val ind = Array<ByteArray>(SilkConstants.NLSF_QUANT_DEL_DEC_STATES) { ByteArray(SilkConstants.MAX_LPC_ORDER) }

        val prev_out_Q10 = ShortArray(2 * SilkConstants.NLSF_QUANT_DEL_DEC_STATES)
        val RD_Q25 = IntArray(2 * SilkConstants.NLSF_QUANT_DEL_DEC_STATES)
        val RD_min_Q25 = IntArray(SilkConstants.NLSF_QUANT_DEL_DEC_STATES)
        val RD_max_Q25 = IntArray(SilkConstants.NLSF_QUANT_DEL_DEC_STATES)
        var rates_Q5: Int

        val out0_Q10_table = IntArray(2 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT)
        val out1_Q10_table = IntArray(2 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT)

        i = 0 - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT
        while (i <= SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT - 1) {
            out0_Q10 = Inlines.silk_LSHIFT(i, 10)
            out1_Q10 = Inlines.silk_ADD16(out0_Q10.toShort(), 1024.toShort()).toInt()

            if (i > 0) {
                out0_Q10 = Inlines.silk_SUB16(
                    out0_Q10.toShort(),
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                ).toInt()
                out1_Q10 = Inlines.silk_SUB16(
                    out1_Q10.toShort(),
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                ).toInt()
            } else if (i == 0) {
                out1_Q10 = Inlines.silk_SUB16(
                    out1_Q10.toShort(),
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                ).toInt()
            } else if (i == -1) {
                out0_Q10 = Inlines.silk_ADD16(
                    out0_Q10.toShort(),
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                ).toInt()
            } else {
                out0_Q10 = Inlines.silk_ADD16(
                    out0_Q10.toShort(),
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                ).toInt()
                out1_Q10 = Inlines.silk_ADD16(
                    out1_Q10.toShort(),
                    (SilkConstants.NLSF_QUANT_LEVEL_ADJ * (1.toLong() shl 10) + 0.5).toInt().toShort()
                ).toInt()
            }

            out0_Q10_table[i + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT] =
                    Inlines.silk_SMULWB(out0_Q10, quant_step_size_Q16)
            out1_Q10_table[i + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT] =
                    Inlines.silk_SMULWB(out1_Q10, quant_step_size_Q16)
            i++
        }

        Inlines.OpusAssert(SilkConstants.NLSF_QUANT_DEL_DEC_STATES and SilkConstants.NLSF_QUANT_DEL_DEC_STATES - 1 == 0) // must be power of two

        nStates = 1
        RD_Q25[0] = 0
        prev_out_Q10[0] = 0

        i = order - 1
        while (true) {
            pred_coef_Q16 = Inlines.silk_LSHIFT(pred_coef_Q8[i].toInt(), 8)
            in_Q10 = x_Q10[i].toInt()

            j = 0
            while (j < nStates) {
                pred_Q10 = Inlines.silk_SMULWB(pred_coef_Q16, prev_out_Q10[j].toInt())
                res_Q10 = Inlines.silk_SUB16(in_Q10.toShort(), pred_Q10.toShort()).toInt()
                ind_tmp = Inlines.silk_SMULWB(inv_quant_step_size_Q6.toInt(), res_Q10)
                ind_tmp = Inlines.silk_LIMIT(
                    ind_tmp,
                    0 - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT,
                    SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT - 1
                )
                ind[j]!![i] = ind_tmp.toByte()
                rates_Q5 = ec_ix[i] + ind_tmp

                // compute outputs for ind_tmp and ind_tmp + 1
                out0_Q10 = out0_Q10_table[ind_tmp + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT]
                out1_Q10 = out1_Q10_table[ind_tmp + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT]

                out0_Q10 = Inlines.silk_ADD16(out0_Q10.toShort(), pred_Q10.toShort()).toInt()
                out1_Q10 = Inlines.silk_ADD16(out1_Q10.toShort(), pred_Q10.toShort()).toInt()
                prev_out_Q10[j] = out0_Q10.toShort()
                prev_out_Q10[j + nStates] = out1_Q10.toShort()

                // compute RD for ind_tmp and ind_tmp + 1
                if (ind_tmp + 1 >= SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
                    if (ind_tmp + 1 == SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
                        rate0_Q5 = ec_rates_Q5!![rates_Q5 + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE].toInt()
                        rate1_Q5 = 280
                    } else {
                        rate0_Q5 = Inlines.silk_SMLABB(280 - 43 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE, 43, ind_tmp)
                        rate1_Q5 = Inlines.silk_ADD16(rate0_Q5.toShort(), 43.toShort()).toInt()
                    }
                } else if (ind_tmp <= 0 - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
                    if (ind_tmp == 0 - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
                        rate0_Q5 = 280
                        rate1_Q5 = ec_rates_Q5!![rates_Q5 + 1 + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE].toInt()
                    } else {
                        rate0_Q5 = Inlines.silk_SMLABB(280 - 43 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE, -43, ind_tmp)
                        rate1_Q5 = Inlines.silk_SUB16(rate0_Q5.toShort(), 43.toShort()).toInt()
                    }
                } else {
                    rate0_Q5 = ec_rates_Q5!![rates_Q5 + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE].toInt()
                    rate1_Q5 = ec_rates_Q5[rates_Q5 + 1 + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE].toInt()
                }

                RD_tmp_Q25 = RD_Q25[j]
                diff_Q10 = Inlines.silk_SUB16(in_Q10.toShort(), out0_Q10.toShort()).toInt()
                RD_Q25[j] = Inlines.silk_SMLABB(
                    Inlines.silk_MLA(
                        RD_tmp_Q25,
                        Inlines.silk_SMULBB(diff_Q10, diff_Q10),
                        w_Q5[i].toInt()
                    ), mu_Q20, rate0_Q5
                )
                diff_Q10 = Inlines.silk_SUB16(in_Q10.toShort(), out1_Q10.toShort()).toInt()
                RD_Q25[j + nStates] = Inlines.silk_SMLABB(
                    Inlines.silk_MLA(
                        RD_tmp_Q25,
                        Inlines.silk_SMULBB(diff_Q10, diff_Q10),
                        w_Q5[i].toInt()
                    ), mu_Q20, rate1_Q5
                )
                j++
            }

            if (nStates <= SilkConstants.NLSF_QUANT_DEL_DEC_STATES shr 1) {
                // double number of states and copy
                j = 0
                while (j < nStates) {
                    ind[j + nStates]!![i] = (ind[j]!![i] + 1).toByte()
                    j++
                }
                nStates = Inlines.silk_LSHIFT(nStates, 1)

                j = nStates
                while (j < SilkConstants.NLSF_QUANT_DEL_DEC_STATES) {
                    ind[j]!![i] = ind[j - nStates]!![i]
                    j++
                }
            } else if (i > 0) {
                // sort lower and upper half of RD_Q25, pairwise
                j = 0
                while (j < SilkConstants.NLSF_QUANT_DEL_DEC_STATES) {
                    if (RD_Q25[j] > RD_Q25[j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES]) {
                        RD_max_Q25[j] = RD_Q25[j]
                        RD_min_Q25[j] = RD_Q25[j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES]
                        RD_Q25[j] = RD_min_Q25[j]
                        RD_Q25[j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES] = RD_max_Q25[j]

                        // swap prev_out values
                        out0_Q10 = prev_out_Q10[j].toInt()
                        prev_out_Q10[j] = prev_out_Q10[j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES]
                        prev_out_Q10[j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES] = out0_Q10.toShort()
                        ind_sort[j] = j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES
                    } else {
                        RD_min_Q25[j] = RD_Q25[j]
                        RD_max_Q25[j] = RD_Q25[j + SilkConstants.NLSF_QUANT_DEL_DEC_STATES]
                        ind_sort[j] = j
                    }
                    j++
                }

                // compare the highest RD values of the winning half with the lowest one in the losing half, and copy if necessary
                // afterwards ind_sort[] will contain the indices of the NLSF_QUANT_DEL_DEC_STATES winning RD values
                while (true) {
                    min_max_Q25 = Int.MAX_VALUE
                    max_min_Q25 = 0
                    ind_min_max = 0
                    ind_max_min = 0

                    j = 0
                    while (j < SilkConstants.NLSF_QUANT_DEL_DEC_STATES) {
                        if (min_max_Q25 > RD_max_Q25[j]) {
                            min_max_Q25 = RD_max_Q25[j]
                            ind_min_max = j
                        }
                        if (max_min_Q25 < RD_min_Q25[j]) {
                            max_min_Q25 = RD_min_Q25[j]
                            ind_max_min = j
                        }
                        j++
                    }

                    if (min_max_Q25 >= max_min_Q25) {
                        break
                    }

                    // copy ind_min_max to ind_max_min
                    ind_sort[ind_max_min] = ind_sort[ind_min_max] xor SilkConstants.NLSF_QUANT_DEL_DEC_STATES
                    RD_Q25[ind_max_min] = RD_Q25[ind_min_max + SilkConstants.NLSF_QUANT_DEL_DEC_STATES]
                    prev_out_Q10[ind_max_min] = prev_out_Q10[ind_min_max + SilkConstants.NLSF_QUANT_DEL_DEC_STATES]
                    RD_min_Q25[ind_max_min] = 0
                    RD_max_Q25[ind_min_max] = Int.MAX_VALUE
                    arraycopy(ind[ind_min_max], 0, ind[ind_max_min], 0, order.toInt())
                }

                // increment index if it comes from the upper half
                j = 0
                while (j < SilkConstants.NLSF_QUANT_DEL_DEC_STATES) {
                    val x = Inlines.silk_RSHIFT(ind_sort[j], SilkConstants.NLSF_QUANT_DEL_DEC_STATES_LOG2).toByte()
                    ind[j]!![i] = (ind[j]!![i] + x).toByte()
                    j++
                }
            } else {
                // i == 0
                break
            }
            i--
        }

        // last sample: find winner, copy indices and return RD value
        ind_tmp = 0
        min_Q25 = Int.MAX_VALUE
        j = 0
        while (j < 2 * SilkConstants.NLSF_QUANT_DEL_DEC_STATES) {
            if (min_Q25 > RD_Q25[j]) {
                min_Q25 = RD_Q25[j]
                ind_tmp = j
            }
            j++
        }

        j = 0
        while (j < order) {
            indices[j] = ind[ind_tmp and SilkConstants.NLSF_QUANT_DEL_DEC_STATES - 1]!![j]
            Inlines.OpusAssert(indices[j] >= 0 - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT)
            Inlines.OpusAssert(indices[j] <= SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT)
            j++
        }

        indices[0] = (indices[0] + Inlines.silk_RSHIFT(ind_tmp, SilkConstants.NLSF_QUANT_DEL_DEC_STATES_LOG2)).toByte()
        Inlines.OpusAssert(indices[0] <= SilkConstants.NLSF_QUANT_MAX_AMPLITUDE_EXT)
        Inlines.OpusAssert(min_Q25 >= 0)
        return min_Q25
    }

    /// <summary>
    /// NLSF vector encoder
    /// </summary>
    /// <param name="NLSFIndices">(I) Codebook path vector [ LPC_ORDER + 1 ]</param>
    /// <param name="pNLSF_Q15">(I/O) Quantized NLSF vector [ LPC_ORDER ]</param>
    /// <param name="psNLSF_CB">(I) Codebook object</param>
    /// <param name="pW_QW">(I) NLSF weight vector [ LPC_ORDER ]</param>
    /// <param name="NLSF_mu_Q20">(I) Rate weight for the RD optimization</param>
    /// <param name="nSurvivors">(I) Max survivors after first stage</param>
    /// <param name="signalType">(I) Signal type: 0/1/2</param>
    /// <returns>RD value in Q25</returns>
    fun silk_NLSF_encode(
        NLSFIndices: ByteArray,
        pNLSF_Q15: ShortArray,
        psNLSF_CB: NLSFCodebook,
        pW_QW: ShortArray,
        NLSF_mu_Q20: Int,
        nSurvivors: Int,
        signalType: Int
    ): Int {
        var i: Int
        var s: Int
        var ind1: Int
        var prob_Q8: Int
        var bits_q7: Int
        var W_tmp_Q9: Int
        val err_Q26: IntArray
        val RD_Q25: IntArray
        val tempIndices1: IntArray
        val tempIndices2: Array<ByteArray>
        val res_Q15 = ShortArray(psNLSF_CB.order.toInt())
        val res_Q10 = ShortArray(psNLSF_CB.order.toInt())
        val NLSF_tmp_Q15 = ShortArray(psNLSF_CB.order.toInt())
        val W_tmp_QW = ShortArray(psNLSF_CB.order.toInt())
        val W_adj_Q5 = ShortArray(psNLSF_CB.order.toInt())
        val pred_Q8 = ShortArray(psNLSF_CB.order.toInt())
        val ec_ix = ShortArray(psNLSF_CB.order.toInt())
        val pCB = psNLSF_CB.CB1_NLSF_Q8
        var iCDF_ptr: Int
        var pCB_element: Int

        Inlines.OpusAssert(nSurvivors <= SilkConstants.NLSF_VQ_MAX_SURVIVORS)
        Inlines.OpusAssert(signalType >= 0 && signalType <= 2)
        Inlines.OpusAssert(NLSF_mu_Q20 <= 32767 && NLSF_mu_Q20 >= 0)

        // NLSF stabilization
        silk_NLSF_stabilize(pNLSF_Q15, psNLSF_CB.deltaMin_Q15!!, psNLSF_CB.order.toInt())

        // First stage: VQ
        err_Q26 = IntArray(psNLSF_CB.nVectors.toInt())
        silk_NLSF_VQ(err_Q26, pNLSF_Q15, psNLSF_CB.CB1_NLSF_Q8, psNLSF_CB.nVectors.toInt(), psNLSF_CB.order.toInt())

        // Sort the quantization errors
        tempIndices1 = IntArray(nSurvivors)
        Sort.silk_insertion_sort_increasing(err_Q26, tempIndices1, psNLSF_CB.nVectors.toInt(), nSurvivors)

        RD_Q25 = IntArray(nSurvivors)
        tempIndices2 = Arrays.InitTwoDimensionalArrayByte(nSurvivors, SilkConstants.MAX_LPC_ORDER)

        // Loop over survivors
        s = 0
        while (s < nSurvivors) {
            ind1 = tempIndices1[s]

            // Residual after first stage
            pCB_element = ind1 * psNLSF_CB.order // opt: potential 1:2 partitioned buffer
            i = 0
            while (i < psNLSF_CB.order) {
                NLSF_tmp_Q15[i] = Inlines.silk_LSHIFT16(pCB!![pCB_element + i].toShort(), 7)
                res_Q15[i] = (pNLSF_Q15[i] - NLSF_tmp_Q15[i]).toShort()
                i++
            }

            // Weights from codebook vector
            silk_NLSF_VQ_weights_laroia(W_tmp_QW, NLSF_tmp_Q15, psNLSF_CB.order.toInt())

            // Apply square-rooted weights
            i = 0
            while (i < psNLSF_CB.order) {
                W_tmp_Q9 =
                        Inlines.silk_SQRT_APPROX(Inlines.silk_LSHIFT(W_tmp_QW[i].toInt(), 18 - SilkConstants.NLSF_W_Q))
                res_Q10[i] = Inlines.silk_RSHIFT(Inlines.silk_SMULBB(res_Q15[i].toInt(), W_tmp_Q9), 14).toShort()
                i++
            }

            // Modify input weights accordingly
            i = 0
            while (i < psNLSF_CB.order) {
                W_adj_Q5[i] =
                        Inlines.silk_DIV32_16(Inlines.silk_LSHIFT(pW_QW[i].toInt(), 5), W_tmp_QW[i].toInt()).toShort()
                i++
            }

            // Unpack entropy table indices and predictor for current CB1 index
            silk_NLSF_unpack(ec_ix, pred_Q8, psNLSF_CB, ind1)

            // Trellis quantizer
            RD_Q25[s] = silk_NLSF_del_dec_quant(
                tempIndices2[s],
                res_Q10,
                W_adj_Q5,
                pred_Q8,
                ec_ix,
                psNLSF_CB.ec_Rates_Q5,
                psNLSF_CB.quantStepSize_Q16.toInt(),
                psNLSF_CB.invQuantStepSize_Q6,
                NLSF_mu_Q20,
                psNLSF_CB.order
            )

            // Add rate for first stage
            iCDF_ptr = (signalType shr 1) * psNLSF_CB.nVectors

            if (ind1 == 0) {
                prob_Q8 = 256 - psNLSF_CB.CB1_iCDF!![iCDF_ptr + ind1]
            } else {
                prob_Q8 = psNLSF_CB.CB1_iCDF!![iCDF_ptr + ind1 - 1] - psNLSF_CB.CB1_iCDF!![iCDF_ptr + ind1]
            }

            bits_q7 = (8 shl 7) - Inlines.silk_lin2log(prob_Q8)
            RD_Q25[s] = Inlines.silk_SMLABB(RD_Q25[s], bits_q7, Inlines.silk_RSHIFT(NLSF_mu_Q20, 2))
            s++
        }

        // Find the lowest rate-distortion error
        val bestIndex = IntArray(1)
        Sort.silk_insertion_sort_increasing(RD_Q25, bestIndex, nSurvivors, 1)

        NLSFIndices[0] = tempIndices1[bestIndex[0]].toByte()
        arraycopy(tempIndices2[bestIndex[0]], 0, NLSFIndices, 1, psNLSF_CB.order.toInt())

        // Decode
        silk_NLSF_decode(pNLSF_Q15, NLSFIndices, psNLSF_CB)

        return RD_Q25[0]
    }

    /// <summary>
    /// helper function for NLSF2A(..)
    /// </summary>
    /// <param name="o">(O) intermediate polynomial, QA [dd+1]</param>
    /// <param name="cLSF">(I) vector of interleaved 2*cos(LSFs), QA [d]</param>
    /// <param name="dd">(I) polynomial order (= 1/2 * filter order)</param>
    fun silk_NLSF2A_find_poly(
        o: IntArray,
        cLSF: IntArray,
        cLSF_ptr: Int,
        dd: Int
    ) {
        var k: Int
        var n: Int
        var ftmp: Int

        o[0] = Inlines.silk_LSHIFT(1, QA)
        o[1] = 0 - cLSF[cLSF_ptr]
        k = 1
        while (k < dd) {
            ftmp = cLSF[cLSF_ptr + 2 * k]
            /* QA*/
            o[k + 1] = Inlines.silk_LSHIFT(o[k - 1], 1) -
                    Inlines.silk_RSHIFT_ROUND64(Inlines.silk_SMULL(ftmp, o[k]), QA).toInt()
            n = k
            while (n > 1) {
                o[n] += o[n - 2] - Inlines.silk_RSHIFT_ROUND64(Inlines.silk_SMULL(ftmp, o[n - 1]), QA).toInt()
                n--
            }
            o[1] -= ftmp
            k++
        }
    }

    /// <summary>
    /// compute whitening filter coefficients from normalized line spectral frequencies
    /// </summary>
    /// <param name="a_Q12">(O) monic whitening filter coefficients in Q12,  [ d ]</param>
    /// <param name="NLSF">(I) normalized line spectral frequencies in Q15, [ d ]</param>
    /// <param name="d">(I) filter order (should be even)</param>
    fun silk_NLSF2A(
        a_Q12: ShortArray,
        NLSF: ShortArray,
        d: Int
    ) {

        val ordering: ByteArray
        var k: Int
        var i: Int
        val dd: Int
        val cos_LSF_QA = IntArray(d)
        val P = IntArray(d / 2 + 1)
        val Q = IntArray(d / 2 + 1)
        val a32_QA1 = IntArray(d)

        var Ptmp: Int
        var Qtmp: Int
        var f_int: Int
        var f_frac: Int
        var cos_val: Int
        var delta: Int
        var maxabs: Int
        var absval: Int
        var idx = 0
        var sc_Q16: Int

        Inlines.OpusAssert(SilkConstants.LSF_COS_TAB_SZ == 128)
        Inlines.OpusAssert(d == 10 || d == 16)

        /* convert LSFs to 2*cos(LSF), using piecewise linear curve from table */
        ordering = if (d == 16) ordering16 else ordering10

        k = 0
        while (k < d) {
            Inlines.OpusAssert(NLSF[k] >= 0)

            /* f_int on a scale 0-127 (rounded down) */
            f_int = Inlines.silk_RSHIFT(NLSF[k].toInt(), 15 - 7)

            /* f_frac, range: 0..255 */
            f_frac = NLSF[k] - Inlines.silk_LSHIFT(f_int, 15 - 7)

            Inlines.OpusAssert(f_int >= 0)
            Inlines.OpusAssert(f_int < SilkConstants.LSF_COS_TAB_SZ)

            /* Read start and end value from table */
            cos_val = SilkTables.silk_LSFCosTab_Q12[f_int].toInt()
            /* Q12 */
            delta = SilkTables.silk_LSFCosTab_Q12[f_int + 1] - cos_val
            /* Q12, with a range of 0..200 */

            /* Linear interpolation */
            cos_LSF_QA[ordering[k].toInt()] = Inlines.silk_RSHIFT_ROUND(
                Inlines.silk_LSHIFT(cos_val, 8) + Inlines.silk_MUL(delta, f_frac),
                20 - QA
            )
            k++
            /* QA */
        }

        dd = Inlines.silk_RSHIFT(d, 1)

        /* generate even and odd polynomials using convolution */
        silk_NLSF2A_find_poly(P, cos_LSF_QA, 0, dd)
        silk_NLSF2A_find_poly(Q, cos_LSF_QA, 1, dd)

        /* convert even and odd polynomials to opus_int32 Q12 filter coefs */
        k = 0
        while (k < dd) {
            Ptmp = P[k + 1] + P[k]
            Qtmp = Q[k + 1] - Q[k]

            /* the Ptmp and Qtmp values at this stage need to fit in int32 */
            a32_QA1[k] = -Qtmp - Ptmp
            /* QA+1 */
            a32_QA1[d - k - 1] = Qtmp - Ptmp
            k++
            /* QA+1 */
        }

        /* Limit the maximum absolute value of the prediction coefficients, so that they'll fit in int16 */
        i = 0
        while (i < 10) {
            /* Find maximum absolute value and its index */
            maxabs = 0
            k = 0
            while (k < d) {
                absval = Inlines.silk_abs(a32_QA1[k])
                if (absval > maxabs) {
                    maxabs = absval
                    idx = k
                }
                k++
            }

            maxabs = Inlines.silk_RSHIFT_ROUND(maxabs, QA + 1 - 12)
            /* QA+1 . Q12 */

            if (maxabs > Short.MAX_VALUE) {
                /* Reduce magnitude of prediction coefficients */
                maxabs = Inlines.silk_min(maxabs, 163838)
                /* ( silk_int32_MAX >> 14 ) + silk_int16_MAX = 163838 */
                sc_Q16 = (0.999f * (1.toLong() shl 16) + 0.5).toInt()/*Inlines.SILK_CONST(0.999f, 16)*/ -
                        Inlines.silk_DIV32(
                            Inlines.silk_LSHIFT(maxabs - Short.MAX_VALUE, 14),
                            Inlines.silk_RSHIFT32(Inlines.silk_MUL(maxabs, idx + 1), 2)
                        )
                Filters.silk_bwexpander_32(a32_QA1, d, sc_Q16)
            } else {
                break
            }
            i++
        }

        if (i == 10) {
            /* Reached the last iteration, clip the coefficients */
            k = 0
            while (k < d) {
                a_Q12[k] = Inlines.silk_SAT16(Inlines.silk_RSHIFT_ROUND(a32_QA1[k], QA + 1 - 12)).toShort()
                /* QA+1 . Q12 */
                a32_QA1[k] = Inlines.silk_LSHIFT(a_Q12[k].toInt(), QA + 1 - 12)
                k++
            }
        } else {
            k = 0
            while (k < d) {
                a_Q12[k] = Inlines.silk_RSHIFT_ROUND(a32_QA1[k], QA + 1 - 12).toShort()
                k++
                /* QA+1 . Q12 */
            }
        }

        i = 0
        while (i < SilkConstants.MAX_LPC_STABILIZE_ITERATIONS) {
            if (Filters.silk_LPC_inverse_pred_gain(
                    a_Q12,
                    d
                ) < (1.0f / SilkConstants.MAX_PREDICTION_POWER_GAIN * (1.toLong() shl 30) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f / SilkConstants.MAX_PREDICTION_POWER_GAIN, 30)*/) {
                /* Prediction coefficients are (too close to) unstable; apply bandwidth expansion   */
                /* on the unscaled coefficients, convert to Q12 and measure again                   */
                Filters.silk_bwexpander_32(a32_QA1, d, 65536 - Inlines.silk_LSHIFT(2, i))

                k = 0
                while (k < d) {
                    a_Q12[k] = Inlines.silk_RSHIFT_ROUND(a32_QA1[k], QA + 1 - 12).toShort()
                    k++
                    /* QA+1 . Q12 */
                }
            } else {
                break
            }
            i++
        }
    }

    /// <summary>
    /// Helper function for A2NLSF(..) Transforms polynomials from cos(n*f) to cos(f)^n
    /// </summary>
    /// <param name="p">(I/O) Polynomial</param>
    /// <param name="dd">(I) Polynomial order (= filter order / 2 )</param>
    fun silk_A2NLSF_trans_poly(p: IntArray, dd: Int) {
        var k: Int
        var n: Int

        k = 2
        while (k <= dd) {
            n = dd
            while (n > k) {
                p[n - 2] -= p[n]
                n--
            }
            p[k - 2] -= Inlines.silk_LSHIFT(p[k], 1)
            k++
        }
    }

    /// <summary>
    /// Helper function for A2NLSF(..) Polynomial evaluation
    /// </summary>
    /// <param name="p">(I) Polynomial, Q16</param>
    /// <param name="x">(I) Evaluation point, Q12</param>
    /// <param name="dd">(I) Order</param>
    /// <returns>the polynomial evaluation, in Q16</returns>
    fun silk_A2NLSF_eval_poly(p: IntArray, x: Int, dd: Int): Int {
        var n: Int
        val x_Q16: Int
        var y32: Int

        y32 = p[dd]
        /* Q16 */
        x_Q16 = Inlines.silk_LSHIFT(x, 4)

        if (8 == dd) {
            y32 = Inlines.silk_SMLAWW(p[7], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[6], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[5], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[4], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[3], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[2], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[1], y32, x_Q16)
            y32 = Inlines.silk_SMLAWW(p[0], y32, x_Q16)
        } else {
            n = dd - 1
            while (n >= 0) {
                y32 = Inlines.silk_SMLAWW(p[n], y32, x_Q16)
                n--
                /* Q16 */
            }
        }

        return y32
    }

    fun silk_A2NLSF_init(
        a_Q16: IntArray,
        P: IntArray,
        Q: IntArray,
        dd: Int
    ) {
        var k: Int

        /* Convert filter coefs to even and odd polynomials */
        P[dd] = Inlines.silk_LSHIFT(1, 16)
        Q[dd] = Inlines.silk_LSHIFT(1, 16)
        k = 0
        while (k < dd) {
            P[k] = -a_Q16[dd - k - 1] - a_Q16[dd + k]
            /* Q16 */
            Q[k] = -a_Q16[dd - k - 1] + a_Q16[dd + k]
            k++
            /* Q16 */
        }

        /* Divide out zeros as we have that for even filter orders, */
        /* z =  1 is always a root in Q, and                        */
        /* z = -1 is always a root in P                             */
        k = dd
        while (k > 0) {
            P[k - 1] -= P[k]
            Q[k - 1] += Q[k]
            k--
        }

        /* Transform polynomials from cos(n*f) to cos(f)^n */
        silk_A2NLSF_trans_poly(P, dd)
        silk_A2NLSF_trans_poly(Q, dd)
    }

    /// <summary>
    /// Compute Normalized Line Spectral Frequencies (NLSFs) from whitening filter coefficients
    /// If not all roots are found, the a_Q16 coefficients are bandwidth expanded until convergence.
    /// </summary>
    /// <param name="NLSF">(O) Normalized Line Spectral Frequencies in Q15 (0..2^15-1) [d]</param>
    /// <param name="a_Q16">(I/O) Monic whitening filter coefficients in Q16 [d]</param>
    /// <param name="d">(I) Filter order (must be even)</param>
    fun silk_A2NLSF(NLSF: ShortArray, a_Q16: IntArray, d: Int) {
        var i: Int
        var k: Int
        var m: Int
        val dd: Int
        var root_ix: Int
        var ffrac: Int
        var xlo: Int
        var xhi: Int
        var xmid: Int
        var ylo: Int
        var yhi: Int
        var ymid: Int
        var thr: Int
        var nom: Int
        var den: Int
        val P = IntArray(SilkConstants.SILK_MAX_ORDER_LPC / 2 + 1)
        val Q = IntArray(SilkConstants.SILK_MAX_ORDER_LPC / 2 + 1)
        val PQ = arrayOfNulls<IntArray>(2)
        var p: IntArray

        /* Store pointers to array */
        PQ[0] = P
        PQ[1] = Q

        dd = Inlines.silk_RSHIFT(d, 1)

        silk_A2NLSF_init(a_Q16, P, Q, dd)

        /* Find roots, alternating between P and Q */
        p = P
        /* Pointer to polynomial */

        xlo = SilkTables.silk_LSFCosTab_Q12[0].toInt()
        /* Q12*/
        ylo = silk_A2NLSF_eval_poly(p, xlo, dd)

        if (ylo < 0) {
            /* Set the first NLSF to zero and move on to the next */
            NLSF[0] = 0
            p = Q
            /* Pointer to polynomial */
            ylo = silk_A2NLSF_eval_poly(p, xlo, dd)
            root_ix = 1
            /* Index of current root */
        } else {
            root_ix = 0
            /* Index of current root */
        }
        k = 1
        /* Loop counter */
        i = 0
        /* Counter for bandwidth expansions applied */
        thr = 0
        while (true) {
            /* Evaluate polynomial */
            xhi = SilkTables.silk_LSFCosTab_Q12[k].toInt()
            /* Q12 */
            yhi = silk_A2NLSF_eval_poly(p, xhi, dd)

            /* Detect zero crossing */
            if (ylo <= 0 && yhi >= thr || ylo >= 0 && yhi <= -thr) {
                if (yhi == 0) {
                    /* If the root lies exactly at the end of the current       */
                    /* interval, look for the next root in the next interval    */
                    thr = 1
                } else {
                    thr = 0
                }
                /* Binary division */
                ffrac = -256
                m = 0
                while (m < BIN_DIV_STEPS_A2NLSF) {
                    /* Evaluate polynomial */
                    xmid = Inlines.silk_RSHIFT_ROUND(xlo + xhi, 1)
                    ymid = silk_A2NLSF_eval_poly(p, xmid, dd)

                    /* Detect zero crossing */
                    if (ylo <= 0 && ymid >= 0 || ylo >= 0 && ymid <= 0) {
                        /* Reduce frequency */
                        xhi = xmid
                        yhi = ymid
                    } else {
                        /* Increase frequency */
                        xlo = xmid
                        ylo = ymid
                        ffrac = Inlines.silk_ADD_RSHIFT(ffrac, 128, m)
                    }
                    m++
                }

                /* Interpolate */
                if (Inlines.silk_abs(ylo) < 65536) {
                    /* Avoid dividing by zero */
                    den = ylo - yhi
                    nom = Inlines.silk_LSHIFT(ylo, 8 - BIN_DIV_STEPS_A2NLSF) + Inlines.silk_RSHIFT(den, 1)
                    if (den != 0) {
                        ffrac += Inlines.silk_DIV32(nom, den)
                    }
                } else {
                    /* No risk of dividing by zero because abs(ylo - yhi) >= abs(ylo) >= 65536 */
                    ffrac += Inlines.silk_DIV32(ylo, Inlines.silk_RSHIFT(ylo - yhi, 8 - BIN_DIV_STEPS_A2NLSF))
                }
                NLSF[root_ix] =
                        Inlines.silk_min_32(Inlines.silk_LSHIFT(k, 8) + ffrac, Short.MAX_VALUE.toInt())
                            .toShort()

                Inlines.OpusAssert(NLSF[root_ix] >= 0)

                root_ix++
                /* Next root */
                if (root_ix >= d) {
                    /* Found all roots */
                    break
                }

                /* Alternate pointer to polynomial */
                p = PQ!![root_ix and 1]!!

                /* Evaluate polynomial */
                xlo = SilkTables.silk_LSFCosTab_Q12[k - 1].toInt()
                /* Q12*/
                ylo = Inlines.silk_LSHIFT(1 - (root_ix and 2), 12)
            } else {
                /* Increment loop counter */
                k++
                xlo = xhi
                ylo = yhi
                thr = 0

                if (k > SilkConstants.LSF_COS_TAB_SZ) {
                    i++
                    if (i > MAX_ITERATIONS_A2NLSF) {
                        /* Set NLSFs to white spectrum and exit */
                        NLSF[0] = Inlines.silk_DIV32_16(1 shl 15, (d + 1).toShort().toInt()).toShort()
                        k = 1
                        while (k < d) {
                            NLSF[k] = Inlines.silk_SMULBB(k + 1, NLSF[0].toInt()).toShort()
                            k++
                        }
                        return
                    }

                    /* Error: Apply progressively more bandwidth expansion and run again */
                    Filters.silk_bwexpander_32(a_Q16, d, 65536 - Inlines.silk_SMULBB(10 + i, i))
                    /* 10_Q16 = 0.00015*/

                    silk_A2NLSF_init(a_Q16, P, Q, dd)
                    p = P
                    /* Pointer to polynomial */
                    xlo = SilkTables.silk_LSFCosTab_Q12[0].toInt()
                    /* Q12*/
                    ylo = silk_A2NLSF_eval_poly(p, xlo, dd)
                    if (ylo < 0) {
                        /* Set the first NLSF to zero and move on to the next */
                        NLSF[0] = 0
                        p = Q
                        /* Pointer to polynomial */
                        ylo = silk_A2NLSF_eval_poly(p, xlo, dd)
                        root_ix = 1
                        /* Index of current root */
                    } else {
                        root_ix = 0
                        /* Index of current root */
                    }
                    k = 1
                    /* Reset loop counter */
                }
            }
        }
    }

    /// <summary>
    /// Limit, stabilize, convert and quantize NLSFs
    /// </summary>
    /// <param name="psEncC">I/O  Encoder state</param>
    /// <param name="PredCoef_Q12">O    Prediction coefficients [ 2 ][MAX_LPC_ORDER]</param>
    /// <param name="pNLSF_Q15">I/O  Normalized LSFs (quant out) (0 - (2^15-1)) [MAX_LPC_ORDER]</param>
    /// <param name="prev_NLSFq_Q15">I    Previous Normalized LSFs (0 - (2^15-1)) [MAX_LPC_ORDER]</param>
    fun silk_process_NLSFs(
        psEncC: SilkChannelEncoder,
        PredCoef_Q12: Array<ShortArray>,
        pNLSF_Q15: ShortArray,
        prev_NLSFq_Q15: ShortArray
    ) {
        var i: Int
        val doInterpolate: Boolean
        var NLSF_mu_Q20: Int
        val i_sqr_Q15: Int
        val pNLSF0_temp_Q15 = ShortArray(SilkConstants.MAX_LPC_ORDER)
        val pNLSFW_QW = ShortArray(SilkConstants.MAX_LPC_ORDER)
        val pNLSFW0_temp_QW = ShortArray(SilkConstants.MAX_LPC_ORDER)

        Inlines.OpusAssert(psEncC.speech_activity_Q8 >= 0)
        Inlines.OpusAssert(psEncC.speech_activity_Q8 <= (1.0f * (1.toLong() shl 8) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 8)*/)
        Inlines.OpusAssert(psEncC.useInterpolatedNLSFs == 1 || psEncC.indices.NLSFInterpCoef_Q2.toInt() == 1 shl 2)

        /**
         * ********************
         */
        /* Calculate mu values */
        /**
         * ********************
         */
        /* NLSF_mu  = 0.003 - 0.0015 * psEnc.speech_activity; */
        NLSF_mu_Q20 = Inlines.silk_SMLAWB(
            (0.003f * (1.toLong() shl 20) + 0.5).toInt()/*Inlines.SILK_CONST(0.003f, 20)*/,
            (-0.001f * (1.toLong() shl 28) + 0.5).toInt()/*Inlines.SILK_CONST(-0.001f, 28)*/,
            psEncC.speech_activity_Q8
        )
        if (psEncC.nb_subfr == 2) {
            /* Multiply by 1.5 for 10 ms packets */
            NLSF_mu_Q20 = Inlines.silk_ADD_RSHIFT(NLSF_mu_Q20, NLSF_mu_Q20, 1)
        }

        Inlines.OpusAssert(NLSF_mu_Q20 > 0)
        Inlines.OpusAssert(NLSF_mu_Q20 <= (0.005f * (1.toLong() shl 20) + 0.5).toInt()/*Inlines.SILK_CONST(0.005f, 20)*/)

        /* Calculate NLSF weights */
        silk_NLSF_VQ_weights_laroia(pNLSFW_QW, pNLSF_Q15, psEncC.predictLPCOrder)

        /* Update NLSF weights for interpolated NLSFs */
        doInterpolate = psEncC.useInterpolatedNLSFs == 1 && psEncC.indices.NLSFInterpCoef_Q2 < 4
        if (doInterpolate) {
            /* Calculate the interpolated NLSF vector for the first half */
            Inlines.silk_interpolate(
                pNLSF0_temp_Q15, prev_NLSFq_Q15, pNLSF_Q15,
                psEncC.indices.NLSFInterpCoef_Q2.toInt(), psEncC.predictLPCOrder
            )

            /* Calculate first half NLSF weights for the interpolated NLSFs */
            silk_NLSF_VQ_weights_laroia(pNLSFW0_temp_QW, pNLSF0_temp_Q15, psEncC.predictLPCOrder)

            /* Update NLSF weights with contribution from first half */
            i_sqr_Q15 = Inlines.silk_LSHIFT(
                Inlines.silk_SMULBB(
                    psEncC.indices.NLSFInterpCoef_Q2.toInt(),
                    psEncC.indices.NLSFInterpCoef_Q2.toInt()
                ), 11
            )

            i = 0
            while (i < psEncC.predictLPCOrder) {
                pNLSFW_QW[i] = Inlines.silk_SMLAWB(
                    Inlines.silk_RSHIFT(pNLSFW_QW[i].toInt(), 1),
                    pNLSFW0_temp_QW[i].toInt(),
                    i_sqr_Q15
                ).toShort()
                Inlines.OpusAssert(pNLSFW_QW[i] >= 1)
                i++
            }
        }

        //////////////////////////////////////////////////////////////////////////
        silk_NLSF_encode(
            psEncC.indices.NLSFIndices, pNLSF_Q15, psEncC.psNLSF_CB!!, pNLSFW_QW,
            NLSF_mu_Q20, psEncC.NLSF_MSVQ_Survivors, psEncC.indices.signalType.toInt()
        )

        /* Convert quantized NLSFs back to LPC coefficients */
        silk_NLSF2A(PredCoef_Q12[1], pNLSF_Q15, psEncC.predictLPCOrder)

        if (doInterpolate) {
            /* Calculate the interpolated, quantized LSF vector for the first half */
            Inlines.silk_interpolate(
                pNLSF0_temp_Q15, prev_NLSFq_Q15, pNLSF_Q15,
                psEncC.indices.NLSFInterpCoef_Q2.toInt(), psEncC.predictLPCOrder
            )

            /* Convert back to LPC coefficients */
            silk_NLSF2A(PredCoef_Q12[0], pNLSF0_temp_Q15, psEncC.predictLPCOrder)

        } else {
            /* Copy LPC coefficients for first half from second half */
            arraycopy(PredCoef_Q12[1], 0, PredCoef_Q12[0], 0, psEncC.predictLPCOrder)
        }
    }
}
