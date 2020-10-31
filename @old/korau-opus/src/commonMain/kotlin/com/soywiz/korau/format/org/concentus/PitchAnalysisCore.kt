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

internal object PitchAnalysisCore {

    private val SCRATCH_SIZE = 22
    private val SF_LENGTH_4KHZ = SilkConstants.PE_SUBFR_LENGTH_MS * 4
    private val SF_LENGTH_8KHZ = SilkConstants.PE_SUBFR_LENGTH_MS * 8
    private val MIN_LAG_4KHZ = SilkConstants.PE_MIN_LAG_MS * 4
    private val MIN_LAG_8KHZ = SilkConstants.PE_MIN_LAG_MS * 8
    private val MAX_LAG_4KHZ = SilkConstants.PE_MAX_LAG_MS * 4
    private val MAX_LAG_8KHZ = SilkConstants.PE_MAX_LAG_MS * 8 - 1
    private val CSTRIDE_4KHZ = MAX_LAG_4KHZ + 1 - MIN_LAG_4KHZ
    private val CSTRIDE_8KHZ = MAX_LAG_8KHZ + 3 - (MIN_LAG_8KHZ - 2)
    private val D_COMP_MIN = MIN_LAG_8KHZ - 3
    private val D_COMP_MAX = MAX_LAG_8KHZ + 4
    private val D_COMP_STRIDE = D_COMP_MAX - D_COMP_MIN

    // typedef int silk_pe_stage3_vals[SilkConstants.PE_NB_STAGE3_LAGS];
    // fixme can I linearize this?
    internal class silk_pe_stage3_vals {

        val Values = IntArray(SilkConstants.PE_NB_STAGE3_LAGS)
    }

    /**
     * **********************************************************
     */
    /*      FIXED POINT CORE PITCH ANALYSIS FUNCTION             */
    /**
     * **********************************************************
     */
    fun silk_pitch_analysis_core( /* O    Voicing estimate: 0 voiced, 1 unvoiced                      */
        frame: ShortArray, /* I    Signal of length PE_FRAME_LENGTH_MS*Fs_kHz                  */
        pitch_out: IntArray, /* O    4 pitch lag values                                          */
        lagIndex: BoxedValueShort, /* O    Lag Index                                                   */
        contourIndex: BoxedValueByte, /* O    Pitch contour Index                                         */
        LTPCorr_Q15: BoxedValueInt, /* I/O  Normalized correlation; input: value from previous frame    */
        prevLag: Int, /* I    Last lag of previous frame; set to zero is unvoiced         */
        search_thres1_Q16: Int, /* I    First stage threshold for lag candidates 0 - 1              */
        search_thres2_Q13: Int, /* I    Final threshold for lag candidates 0 - 1                    */
        Fs_kHz: Int, /* I    Sample frequency (kHz)                                      */
        complexity: Int, /* I    Complexity setting, 0-2, where 2 is highest                 */
        nb_subfr: Int /* I    number of 5 ms subframes                                    */
    ): Int {
        var prevLag = prevLag
        val frame_8kHz: ShortArray
        val frame_4kHz: ShortArray
        val filt_state = IntArray(6)
        val input_frame_ptr: ShortArray
        var i: Int
        var k: Int
        var d: Int
        var j: Int
        val C: ShortArray
        val xcorr32: IntArray
        var basis: ShortArray
        var basis_ptr: Int
        var target: ShortArray
        var target_ptr: Int
        var cross_corr: Int
        var normalizer: Int
        var energy: Int
        var shift: Int
        var energy_basis: Int
        var energy_target: Int
        val Cmax: Int
        var length_d_srch: Int
        var length_d_comp: Int
        val d_srch = IntArray(SilkConstants.PE_D_SRCH_LENGTH)
        val d_comp: ShortArray
        var sum: Int
        val threshold: Int
        var lag_counter: Int
        var CBimax: Int
        var CBimax_new: Int
        val CBimax_old: Int
        var lag: Int
        val start_lag: Int
        val end_lag: Int
        var lag_new: Int
        var CCmax: Int
        var CCmax_b: Int
        var CCmax_new_b: Int
        var CCmax_new: Int
        val CC = IntArray(SilkConstants.PE_NB_CBKS_STAGE2_EXT)
        val energies_st3: Array<silk_pe_stage3_vals>
        val cross_corr_st3: Array<silk_pe_stage3_vals>
        val frame_length: Int
        val frame_length_8kHz: Int
        val frame_length_4kHz: Int
        val sf_length: Int
        val min_lag: Int
        val max_lag: Int
        val contour_bias_Q15: Int
        var diff: Int
        var nb_cbk_search: Int
        var delta_lag_log2_sqr_Q7: Int
        var lag_log2_Q7: Int
        val prevLag_log2_Q7: Int
        var prev_lag_bias_Q13: Int
        var Lag_CB_ptr: Array<ByteArray>

        /* Check for valid sampling frequency */
        Inlines.OpusAssert(Fs_kHz == 8 || Fs_kHz == 12 || Fs_kHz == 16)

        /* Check for valid complexity setting */
        Inlines.OpusAssert(complexity >= SilkConstants.SILK_PE_MIN_COMPLEX)
        Inlines.OpusAssert(complexity <= SilkConstants.SILK_PE_MAX_COMPLEX)

        Inlines.OpusAssert(search_thres1_Q16 >= 0 && search_thres1_Q16 <= 1 shl 16)
        Inlines.OpusAssert(search_thres2_Q13 >= 0 && search_thres2_Q13 <= 1 shl 13)

        /* Set up frame lengths max / min lag for the sampling frequency */
        frame_length = (SilkConstants.PE_LTP_MEM_LENGTH_MS + nb_subfr * SilkConstants.PE_SUBFR_LENGTH_MS) * Fs_kHz
        frame_length_4kHz = (SilkConstants.PE_LTP_MEM_LENGTH_MS + nb_subfr * SilkConstants.PE_SUBFR_LENGTH_MS) * 4
        frame_length_8kHz = (SilkConstants.PE_LTP_MEM_LENGTH_MS + nb_subfr * SilkConstants.PE_SUBFR_LENGTH_MS) * 8
        sf_length = SilkConstants.PE_SUBFR_LENGTH_MS * Fs_kHz
        min_lag = SilkConstants.PE_MIN_LAG_MS * Fs_kHz
        max_lag = SilkConstants.PE_MAX_LAG_MS * Fs_kHz - 1

        /* Resample from input sampled at Fs_kHz to 8 kHz */
        frame_8kHz = ShortArray(frame_length_8kHz)
        if (Fs_kHz == 16) {
            Arrays.MemSet(filt_state, 0, 2)
            Resampler.silk_resampler_down2(filt_state, frame_8kHz, frame, frame_length)
        } else if (Fs_kHz == 12) {
            Arrays.MemSet(filt_state, 0, 6)
            Resampler.silk_resampler_down2_3(filt_state, frame_8kHz, frame, frame_length)
        } else {
            Inlines.OpusAssert(Fs_kHz == 8)
            arraycopy(frame, 0, frame_8kHz, 0, frame_length_8kHz)
        }

        /* Decimate again to 4 kHz */
        Arrays.MemSet(filt_state, 0, 2)
        /* Set state to zero */
        frame_4kHz = ShortArray(frame_length_4kHz)
        Resampler.silk_resampler_down2(filt_state, frame_4kHz, frame_8kHz, frame_length_8kHz)

        /* Low-pass filter */
        i = frame_length_4kHz - 1
        while (i > 0) {
            frame_4kHz[i] = Inlines.silk_ADD_SAT16(frame_4kHz[i], frame_4kHz[i - 1])
            i--
        }

        /**
         * *****************************************************************************
         * Scale 4 kHz signal down to prevent correlations measures from
         * overflowing * find scaling as max scaling for each 8kHz(?) subframe
         *
         */

        /* Inner product is calculated with different lengths, so scale for the worst case */
        val boxed_energy = BoxedValueInt(0)
        val boxed_shift = BoxedValueInt(0)
        SumSqrShift.silk_sum_sqr_shift(boxed_energy, boxed_shift, frame_4kHz, frame_length_4kHz)
        energy = boxed_energy.Val
        shift = boxed_shift.Val

        if (shift > 0) {
            shift = Inlines.silk_RSHIFT(shift, 1)
            i = 0
            while (i < frame_length_4kHz) {
                frame_4kHz[i] = Inlines.silk_RSHIFT16(frame_4kHz[i], shift)
                i++
            }
        }

        /**
         * ****************************************************************************
         * FIRST STAGE, operating in 4 khz
         *
         */
        C = ShortArray(nb_subfr * CSTRIDE_8KHZ)
        xcorr32 = IntArray(MAX_LAG_4KHZ - MIN_LAG_4KHZ + 1)
        Arrays.MemSet(C, 0.toShort(), (nb_subfr shr 1) * CSTRIDE_4KHZ)
        target = frame_4kHz
        target_ptr = Inlines.silk_LSHIFT(SF_LENGTH_4KHZ, 2)
        k = 0
        while (k < nb_subfr shr 1) {
            basis = target
            basis_ptr = target_ptr - MIN_LAG_4KHZ

            CeltPitchXCorr.pitch_xcorr(
                target,
                target_ptr,
                target,
                target_ptr - MAX_LAG_4KHZ,
                xcorr32,
                SF_LENGTH_8KHZ,
                MAX_LAG_4KHZ - MIN_LAG_4KHZ + 1
            )

            /* Calculate first vector products before loop */
            cross_corr = xcorr32[MAX_LAG_4KHZ - MIN_LAG_4KHZ]
            normalizer = Inlines.silk_inner_prod_self(target, target_ptr, SF_LENGTH_8KHZ)
            normalizer = Inlines.silk_ADD32(normalizer, Inlines.silk_inner_prod_self(basis, basis_ptr, SF_LENGTH_8KHZ))
            normalizer = Inlines.silk_ADD32(normalizer, Inlines.silk_SMULBB(SF_LENGTH_8KHZ, 4000))

            Inlines.MatrixSet(
                C, k, 0, CSTRIDE_4KHZ,
                Inlines.silk_DIV32_varQ(cross_corr, normalizer, 13 + 1).toShort()
            )
            /* Q13 */

            /* From now on normalizer is computed recursively */
            d = MIN_LAG_4KHZ + 1
            while (d <= MAX_LAG_4KHZ) {
                basis_ptr--

                cross_corr = xcorr32[MAX_LAG_4KHZ - d]

                /* Add contribution of new sample and remove contribution from oldest sample */
                normalizer = Inlines.silk_ADD32(
                    normalizer,
                    Inlines.silk_SMULBB(
                        basis[basis_ptr].toInt(),
                        basis[basis_ptr].toInt()
                    ) - Inlines.silk_SMULBB(
                        basis[basis_ptr + SF_LENGTH_8KHZ].toInt(),
                        basis[basis_ptr + SF_LENGTH_8KHZ].toInt()
                    )
                )

                Inlines.MatrixSet(
                    C, k, d - MIN_LAG_4KHZ, CSTRIDE_4KHZ,
                    Inlines.silk_DIV32_varQ(cross_corr, normalizer, 13 + 1).toShort()
                )
                d++
                /* Q13 */
            }
            /* Update target pointer */
            target_ptr += SF_LENGTH_8KHZ
            k++
        }

        /* Combine two subframes into single correlation measure and apply short-lag bias */
        if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
            i = MAX_LAG_4KHZ
            while (i >= MIN_LAG_4KHZ) {
                sum = Inlines.MatrixGet(C, 0, i - MIN_LAG_4KHZ, CSTRIDE_4KHZ).toInt() +
                        Inlines.MatrixGet(C, 1, i - MIN_LAG_4KHZ, CSTRIDE_4KHZ).toInt()
                /* Q14 */
                sum = Inlines.silk_SMLAWB(sum, sum, Inlines.silk_LSHIFT(-i, 4))
                /* Q14 */
                C[i - MIN_LAG_4KHZ] = sum.toShort()
                i--
                /* Q14 */
            }
        } else {
            /* Only short-lag bias */
            i = MAX_LAG_4KHZ
            while (i >= MIN_LAG_4KHZ) {
                sum = Inlines.silk_LSHIFT(C[i - MIN_LAG_4KHZ].toInt(), 1)
                /* Q14 */
                sum = Inlines.silk_SMLAWB(sum, sum, Inlines.silk_LSHIFT(-i, 4))
                /* Q14 */
                C[i - MIN_LAG_4KHZ] = sum.toShort()
                i--
                /* Q14 */
            }
        }

        /* Sort */
        length_d_srch = Inlines.silk_ADD_LSHIFT32(4, complexity, 1)
        Inlines.OpusAssert(3 * length_d_srch <= SilkConstants.PE_D_SRCH_LENGTH)
        Sort.silk_insertion_sort_decreasing_int16(C, d_srch, CSTRIDE_4KHZ, length_d_srch)

        /* Escape if correlation is very low already here */
        Cmax = C[0].toInt()
        /* Q14 */
        if (Cmax < (0.2f * (1.toLong() shl 14) + 0.5).toInt()/*Inlines.SILK_CONST(0.2f, 14)*/) {
            Arrays.MemSet(pitch_out, 0, nb_subfr)
            LTPCorr_Q15.Val = 0
            lagIndex.Val = 0
            contourIndex.Val = 0

            return 1
        }

        threshold = Inlines.silk_SMULWB(search_thres1_Q16, Cmax)
        i = 0
        while (i < length_d_srch) {
            /* Convert to 8 kHz indices for the sorted correlation that exceeds the threshold */
            if (C[i] > threshold) {
                d_srch[i] = Inlines.silk_LSHIFT(d_srch[i] + MIN_LAG_4KHZ, 1)
            } else {
                length_d_srch = i
                break
            }
            i++
        }
        Inlines.OpusAssert(length_d_srch > 0)

        d_comp = ShortArray(D_COMP_STRIDE)
        i = D_COMP_MIN
        while (i < D_COMP_MAX) {
            d_comp[i - D_COMP_MIN] = 0
            i++
        }
        i = 0
        while (i < length_d_srch) {
            d_comp[d_srch[i] - D_COMP_MIN] = 1
            i++
        }

        /* Convolution */
        i = D_COMP_MAX - 1
        while (i >= MIN_LAG_8KHZ) {
            d_comp[i - D_COMP_MIN] = (d_comp[i - D_COMP_MIN] + (d_comp[i - 1 - D_COMP_MIN] + d_comp[i - 2 - D_COMP_MIN]).toShort()).toShort()
            i--
        }

        length_d_srch = 0
        i = MIN_LAG_8KHZ
        while (i < MAX_LAG_8KHZ + 1) {
            if (d_comp[i + 1 - D_COMP_MIN] > 0) {
                d_srch[length_d_srch] = i
                length_d_srch++
            }
            i++
        }

        /* Convolution */
        i = D_COMP_MAX - 1
        while (i >= MIN_LAG_8KHZ) {
            d_comp[i - D_COMP_MIN] = (d_comp[i - D_COMP_MIN] + (d_comp[i - 1 - D_COMP_MIN].toInt() + d_comp[i - 2 - D_COMP_MIN].toInt() + d_comp[i - 3 - D_COMP_MIN].toInt()).toShort()).toShort()
            i--
        }

        length_d_comp = 0
        i = MIN_LAG_8KHZ
        while (i < D_COMP_MAX) {
            if (d_comp[i - D_COMP_MIN] > 0) {
                d_comp[length_d_comp] = (i - 2).toShort()
                length_d_comp++
            }
            i++
        }

        /**
         * ********************************************************************************
         * SECOND STAGE, operating at 8 kHz, on lag sections with high
         * correlation
         *
         */
        /**
         * ****************************************************************************
         * Scale signal down to avoid correlations measures from overflowing
         *
         */
        /* find scaling as max scaling for each subframe */
        boxed_energy.Val = 0
        boxed_shift.Val = 0
        SumSqrShift.silk_sum_sqr_shift(boxed_energy, boxed_shift, frame_8kHz, frame_length_8kHz)
        energy = boxed_energy.Val
        shift = boxed_shift.Val

        if (shift > 0) {
            shift = Inlines.silk_RSHIFT(shift, 1)
            i = 0
            while (i < frame_length_8kHz) {
                frame_8kHz[i] = Inlines.silk_RSHIFT16(frame_8kHz[i], shift)
                i++
            }
        }

        /**
         * *******************************************************************************
         * Find energy of each subframe projected onto its history, for a range
         * of delays
         *
         */
        Arrays.MemSet(C, 0.toShort(), nb_subfr * CSTRIDE_8KHZ)

        target = frame_8kHz
        target_ptr = SilkConstants.PE_LTP_MEM_LENGTH_MS * 8
        k = 0
        while (k < nb_subfr) {

            energy_target = Inlines.silk_ADD32(
                Inlines.silk_inner_prod(target, target_ptr, target, target_ptr, SF_LENGTH_8KHZ),
                1
            )
            j = 0
            while (j < length_d_comp) {
                d = d_comp[j].toInt()
                basis = target
                basis_ptr = target_ptr - d

                cross_corr = Inlines.silk_inner_prod(target, target_ptr, basis, basis_ptr, SF_LENGTH_8KHZ)
                if (cross_corr > 0) {
                    energy_basis = Inlines.silk_inner_prod_self(basis, basis_ptr, SF_LENGTH_8KHZ)
                    Inlines.MatrixSet(
                        C, k, d - (MIN_LAG_8KHZ - 2), CSTRIDE_8KHZ,
                        Inlines.silk_DIV32_varQ(
                            cross_corr,
                            Inlines.silk_ADD32(
                                energy_target,
                                energy_basis
                            ),
                            13 + 1
                        ).toShort()
                    )
                    /* Q13 */
                } else {
                    Inlines.MatrixSet(C, k, d - (MIN_LAG_8KHZ - 2), CSTRIDE_8KHZ, 0.toShort())
                }
                j++
            }
            target_ptr += SF_LENGTH_8KHZ
            k++
        }

        /* search over lag range and lags codebook */
        /* scale factor for lag codebook, as a function of center lag */
        CCmax = Int.MIN_VALUE
        CCmax_b = Int.MIN_VALUE

        CBimax = 0
        /* To avoid returning undefined lag values */
        lag = -1
        /* To check if lag with strong enough correlation has been found */

        if (prevLag > 0) {
            if (Fs_kHz == 12) {
                prevLag = Inlines.silk_DIV32_16(Inlines.silk_LSHIFT(prevLag, 1), 3)
            } else if (Fs_kHz == 16) {
                prevLag = Inlines.silk_RSHIFT(prevLag, 1)
            }
            prevLag_log2_Q7 = Inlines.silk_lin2log(prevLag)
        } else {
            prevLag_log2_Q7 = 0
        }
        Inlines.OpusAssert(search_thres2_Q13 == Inlines.silk_SAT16(search_thres2_Q13))
        /* Set up stage 2 codebook based on number of subframes */
        if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
            Lag_CB_ptr = SilkTables.silk_CB_lags_stage2
            if (Fs_kHz == 8 && complexity > SilkConstants.SILK_PE_MIN_COMPLEX) {
                /* If input is 8 khz use a larger codebook here because it is last stage */
                nb_cbk_search = SilkConstants.PE_NB_CBKS_STAGE2_EXT
            } else {
                nb_cbk_search = SilkConstants.PE_NB_CBKS_STAGE2
            }
        } else {
            Lag_CB_ptr = SilkTables.silk_CB_lags_stage2_10_ms
            nb_cbk_search = SilkConstants.PE_NB_CBKS_STAGE2_10MS
        }

        k = 0
        while (k < length_d_srch) {
            d = d_srch[k]
            j = 0
            while (j < nb_cbk_search) {
                CC[j] = 0
                i = 0
                while (i < nb_subfr) {
                    val d_subfr: Int
                    /* Try all codebooks */
                    d_subfr = d + Lag_CB_ptr[i][j]
                    CC[j] = CC[j] + Inlines.MatrixGet(
                        C, i,
                        d_subfr - (MIN_LAG_8KHZ - 2),
                        CSTRIDE_8KHZ
                    ).toInt()
                    i++
                }
                j++
            }
            /* Find best codebook */
            CCmax_new = Int.MIN_VALUE
            CBimax_new = 0
            i = 0
            while (i < nb_cbk_search) {
                if (CC[i] > CCmax_new) {
                    CCmax_new = CC[i]
                    CBimax_new = i
                }
                i++
            }

            /* Bias towards shorter lags */
            lag_log2_Q7 = Inlines.silk_lin2log(d)
            /* Q7 */
            Inlines.OpusAssert(lag_log2_Q7 == Inlines.silk_SAT16(lag_log2_Q7))
            Inlines.OpusAssert(
                nb_subfr * (SilkConstants.PE_SHORTLAG_BIAS * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_SHORTLAG_BIAS, 13)*/ == Inlines.silk_SAT16(
                    nb_subfr * (SilkConstants.PE_SHORTLAG_BIAS * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_SHORTLAG_BIAS, 13)*/
                )
            )
            CCmax_new_b = CCmax_new - Inlines.silk_RSHIFT(
                Inlines.silk_SMULBB(
                    nb_subfr * (SilkConstants.PE_SHORTLAG_BIAS * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_SHORTLAG_BIAS, 13)*/,
                    lag_log2_Q7
                ), 7
            )
            /* Q13 */

            /* Bias towards previous lag */
            Inlines.OpusAssert(
                nb_subfr * (SilkConstants.PE_PREVLAG_BIAS * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_PREVLAG_BIAS, 13)*/ == Inlines.silk_SAT16(
                    nb_subfr * (SilkConstants.PE_PREVLAG_BIAS * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_PREVLAG_BIAS, 13)*/
                )
            )
            if (prevLag > 0) {
                delta_lag_log2_sqr_Q7 = lag_log2_Q7 - prevLag_log2_Q7
                Inlines.OpusAssert(delta_lag_log2_sqr_Q7 == Inlines.silk_SAT16(delta_lag_log2_sqr_Q7))
                delta_lag_log2_sqr_Q7 =
                        Inlines.silk_RSHIFT(Inlines.silk_SMULBB(delta_lag_log2_sqr_Q7, delta_lag_log2_sqr_Q7), 7)
                prev_lag_bias_Q13 = Inlines.silk_RSHIFT(
                    Inlines.silk_SMULBB(
                        nb_subfr * (SilkConstants.PE_PREVLAG_BIAS * (1.toLong() shl 13) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_PREVLAG_BIAS, 13)*/,
                        LTPCorr_Q15.Val
                    ), 15
                )
                /* Q13 */
                prev_lag_bias_Q13 = Inlines.silk_DIV32(
                    Inlines.silk_MUL(prev_lag_bias_Q13, delta_lag_log2_sqr_Q7),
                    delta_lag_log2_sqr_Q7 + (0.5f * (1.toLong() shl 7) + 0.5).toInt()/*Inlines.SILK_CONST(0.5f, 7)*/
                )
                CCmax_new_b -= prev_lag_bias_Q13
                /* Q13 */
            }

            if (CCmax_new_b > CCmax_b
                && /* Find maximum biased correlation                  */ CCmax_new > Inlines.silk_SMULBB(
                    nb_subfr,
                    search_thres2_Q13
                )
                && /* Correlation needs to be high enough to be voiced */ SilkTables.silk_CB_lags_stage2[0][CBimax_new] <= MIN_LAG_8KHZ /* Lag must be in range                             */) {
                CCmax_b = CCmax_new_b
                CCmax = CCmax_new
                lag = d
                CBimax = CBimax_new
            }
            k++
        }

        if (lag == -1) {
            /* No suitable candidate found */
            Arrays.MemSet(pitch_out, 0, nb_subfr)
            LTPCorr_Q15.Val = 0
            lagIndex.Val = 0
            contourIndex.Val = 0

            return 1
        }

        /* Output normalized correlation */
        LTPCorr_Q15.Val = Inlines.silk_LSHIFT(Inlines.silk_DIV32_16(CCmax, nb_subfr), 2).toInt()
        Inlines.OpusAssert(LTPCorr_Q15.Val >= 0)

        if (Fs_kHz > 8) {
            val scratch_mem: ShortArray
            /**
             * ************************************************************************
             */
            /* Scale input signal down to avoid correlations measures from overflowing */
            /**
             * ************************************************************************
             */
            /* find scaling as max scaling for each subframe */
            boxed_energy.Val = 0
            boxed_shift.Val = 0
            SumSqrShift.silk_sum_sqr_shift(boxed_energy, boxed_shift, frame, frame_length)
            energy = boxed_energy.Val
            shift = boxed_shift.Val

            if (shift > 0) {
                scratch_mem = ShortArray(frame_length)
                /* Move signal to scratch mem because the input signal should be unchanged */
                shift = Inlines.silk_RSHIFT(shift, 1)
                i = 0
                while (i < frame_length) {
                    scratch_mem[i] = Inlines.silk_RSHIFT16(frame[i], shift)
                    i++
                }
                input_frame_ptr = scratch_mem
            } else {
                input_frame_ptr = frame
            }

            /* Search in original signal */
            CBimax_old = CBimax
            /* Compensate for decimation */
            Inlines.OpusAssert(lag == Inlines.silk_SAT16(lag))
            if (Fs_kHz == 12) {
                lag = Inlines.silk_RSHIFT(Inlines.silk_SMULBB(lag, 3), 1)
            } else if (Fs_kHz == 16) {
                lag = Inlines.silk_LSHIFT(lag, 1)
            } else {
                lag = Inlines.silk_SMULBB(lag, 3)
            }

            lag = Inlines.silk_LIMIT_int(lag, min_lag, max_lag)
            start_lag = Inlines.silk_max_int(lag - 2, min_lag)
            end_lag = Inlines.silk_min_int(lag + 2, max_lag)
            lag_new = lag
            /* to avoid undefined lag */
            CBimax = 0
            /* to avoid undefined lag */

            CCmax = Int.MIN_VALUE
            /* pitch lags according to second stage */
            k = 0
            while (k < nb_subfr) {
                pitch_out[k] = lag + 2 * SilkTables.silk_CB_lags_stage2[k][CBimax_old]
                k++
            }

            /* Set up codebook parameters according to complexity setting and frame length */
            if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
                nb_cbk_search = SilkTables.silk_nb_cbk_searchs_stage3[complexity].toInt()
                Lag_CB_ptr = SilkTables.silk_CB_lags_stage3
            } else {
                nb_cbk_search = SilkConstants.PE_NB_CBKS_STAGE3_10MS
                Lag_CB_ptr = SilkTables.silk_CB_lags_stage3_10_ms
            }

            /* Calculate the correlations and energies needed in stage 3 */
            energies_st3 = Array(nb_subfr * nb_cbk_search) { silk_pe_stage3_vals() } // fixme: these can be replaced with a linearized array probably, or at least a struct
            cross_corr_st3 = Array(nb_subfr * nb_cbk_search) { silk_pe_stage3_vals() }
            silk_P_Ana_calc_corr_st3(cross_corr_st3, input_frame_ptr, start_lag, sf_length, nb_subfr, complexity)
            silk_P_Ana_calc_energy_st3(energies_st3, input_frame_ptr, start_lag, sf_length, nb_subfr, complexity)

            lag_counter = 0
            Inlines.OpusAssert(lag == Inlines.silk_SAT16(lag))
            contour_bias_Q15 = Inlines.silk_DIV32_16(
                (SilkConstants.PE_FLATCONTOUR_BIAS * (1.toLong() shl 15) + 0.5).toInt()/*Inlines.SILK_CONST(SilkConstants.PE_FLATCONTOUR_BIAS, 15)*/,
                lag
            )

            target = input_frame_ptr
            target_ptr = SilkConstants.PE_LTP_MEM_LENGTH_MS * Fs_kHz
            energy_target =
                    Inlines.silk_ADD32(Inlines.silk_inner_prod_self(target, target_ptr, nb_subfr * sf_length), 1)
            d = start_lag
            while (d <= end_lag) {
                j = 0
                while (j < nb_cbk_search) {
                    cross_corr = 0
                    energy = energy_target
                    k = 0
                    while (k < nb_subfr) {
                        cross_corr = Inlines.silk_ADD32(
                            cross_corr,
                            Inlines.MatrixGet(
                                cross_corr_st3, k, j,
                                nb_cbk_search
                            ).Values[lag_counter]
                        )
                        energy = Inlines.silk_ADD32(
                            energy,
                            Inlines.MatrixGet(
                                energies_st3, k, j,
                                nb_cbk_search
                            ).Values[lag_counter]
                        )
                        Inlines.OpusAssert(energy >= 0)
                        k++
                    }
                    if (cross_corr > 0) {
                        CCmax_new = Inlines.silk_DIV32_varQ(cross_corr, energy, 13 + 1)
                        /* Q13 */
                        /* Reduce depending on flatness of contour */
                        diff = Short.MAX_VALUE - Inlines.silk_MUL(contour_bias_Q15, j)
                        /* Q15 */
                        Inlines.OpusAssert(diff == Inlines.silk_SAT16(diff))
                        CCmax_new = Inlines.silk_SMULWB(CCmax_new, diff)
                        /* Q14 */
                    } else {
                        CCmax_new = 0
                    }

                    if (CCmax_new > CCmax && d + SilkTables.silk_CB_lags_stage3[0][j] <= max_lag) {
                        CCmax = CCmax_new
                        lag_new = d
                        CBimax = j
                    }
                    j++
                }
                lag_counter++
                d++
            }

            k = 0
            while (k < nb_subfr) {
                pitch_out[k] = lag_new + Lag_CB_ptr[k][CBimax]
                pitch_out[k] = Inlines.silk_LIMIT(pitch_out[k], min_lag, SilkConstants.PE_MAX_LAG_MS * Fs_kHz)
                k++
            }
            lagIndex.Val = (lag_new - min_lag).toShort()
            contourIndex.Val = CBimax.toByte()
        } else {
            /* Fs_kHz == 8 */
            /* Save Lags */
            k = 0
            while (k < nb_subfr) {
                pitch_out[k] = lag + Lag_CB_ptr[k][CBimax]
                pitch_out[k] = Inlines.silk_LIMIT(pitch_out[k], MIN_LAG_8KHZ, SilkConstants.PE_MAX_LAG_MS * 8)
                k++
            }
            lagIndex.Val = (lag - MIN_LAG_8KHZ).toShort()
            contourIndex.Val = CBimax.toByte()
        }
        Inlines.OpusAssert(lagIndex.Val >= 0)
        /* return as voiced */

        return 0
    }

    /**
     * *********************************************************************
     * Calculates the correlations used in stage 3 search. In order to cover the
     * whole lag codebook for all the searched offset lags (lag +- 2), the
     * following correlations are needed in each sub frame:
     *
     * sf1: lag range [-8,...,7] total 16 correlations sf2: lag range [-4,...,4]
     * total 9 correlations sf3: lag range [-3,....4] total 8 correltions sf4:
     * lag range [-6,....8] total 15 correlations
     *
     * In total 48 correlations. The direct implementation computed in worst
     * case 4*12*5 = 240 correlations, but more likely around 120.
     *
     */
    private fun silk_P_Ana_calc_corr_st3(
        cross_corr_st3: Array<silk_pe_stage3_vals>, /* O 3 DIM correlation array */
        frame: ShortArray, /* I vector to correlate         */
        start_lag: Int, /* I lag offset to search around */
        sf_length: Int, /* I length of a 5 ms subframe   */
        nb_subfr: Int, /* I number of subframes         */
        complexity: Int /* I Complexity setting          */
    ) {
        var target_ptr: Int
        var i: Int
        var j: Int
        var k: Int
        var lag_counter: Int
        var lag_low: Int
        var lag_high: Int
        val nb_cbk_search: Int
        var delta: Int
        var idx: Int
        val scratch_mem: IntArray
        val xcorr32: IntArray
        val Lag_range_ptr: Array<ByteArray>
        val Lag_CB_ptr: Array<ByteArray>

        Inlines.OpusAssert(complexity >= SilkConstants.SILK_PE_MIN_COMPLEX)
        Inlines.OpusAssert(complexity <= SilkConstants.SILK_PE_MAX_COMPLEX)

        if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
            Lag_range_ptr = SilkTables.silk_Lag_range_stage3[complexity]
            Lag_CB_ptr = SilkTables.silk_CB_lags_stage3
            nb_cbk_search = SilkTables.silk_nb_cbk_searchs_stage3[complexity].toInt()
        } else {
            Inlines.OpusAssert(nb_subfr == SilkConstants.PE_MAX_NB_SUBFR shr 1)
            Lag_range_ptr = SilkTables.silk_Lag_range_stage3_10_ms
            Lag_CB_ptr = SilkTables.silk_CB_lags_stage3_10_ms
            nb_cbk_search = SilkConstants.PE_NB_CBKS_STAGE3_10MS
        }
        scratch_mem = IntArray(SCRATCH_SIZE)
        xcorr32 = IntArray(SCRATCH_SIZE)

        target_ptr = Inlines.silk_LSHIFT(sf_length, 2)
        /* Pointer to middle of frame */
        k = 0
        while (k < nb_subfr) {
            lag_counter = 0

            /* Calculate the correlations for each subframe */
            lag_low = Lag_range_ptr[k][0].toInt()
            lag_high = Lag_range_ptr[k][1].toInt()
            Inlines.OpusAssert(lag_high - lag_low + 1 <= SCRATCH_SIZE)
            CeltPitchXCorr.pitch_xcorr(
                frame,
                target_ptr,
                frame,
                target_ptr - start_lag - lag_high,
                xcorr32,
                sf_length,
                lag_high - lag_low + 1
            )
            j = lag_low
            while (j <= lag_high) {
                Inlines.OpusAssert(lag_counter < SCRATCH_SIZE)
                scratch_mem[lag_counter] = xcorr32[lag_high - j]
                lag_counter++
                j++
            }

            delta = Lag_range_ptr[k][0].toInt()
            i = 0
            while (i < nb_cbk_search) {
                /* Fill out the 3 dim array that stores the correlations for */
                /* each code_book vector for each start lag */
                idx = Lag_CB_ptr[k][i] - delta
                j = 0
                while (j < SilkConstants.PE_NB_STAGE3_LAGS) {
                    Inlines.OpusAssert(idx + j < SCRATCH_SIZE)
                    Inlines.OpusAssert(idx + j < lag_counter)
                    Inlines.MatrixGet(cross_corr_st3, k, i, nb_cbk_search).Values[j] = scratch_mem[idx + j]
                    j++
                }
                i++
            }
            target_ptr += sf_length
            k++
        }

    }

    /**
     * *****************************************************************
     */
    /* Calculate the energies for first two subframes. The energies are */
    /* calculated recursively.                                          */
    /**
     * *****************************************************************
     */
    fun silk_P_Ana_calc_energy_st3(
        energies_st3: Array<silk_pe_stage3_vals>, /* O 3 DIM energy array */
        frame: ShortArray, /* I vector to calc energy in    */
        start_lag: Int, /* I lag offset to search around */
        sf_length: Int, /* I length of one 5 ms subframe */
        nb_subfr: Int, /* I number of subframes         */
        complexity: Int /* I Complexity setting          */
    ) {
        var target_ptr: Int
        var basis_ptr: Int
        var energy: Int
        var k: Int
        var i: Int
        var j: Int
        var lag_counter: Int
        val nb_cbk_search: Int
        var delta: Int
        var idx: Int
        var lag_diff: Int
        val scratch_mem: IntArray
        val Lag_range_ptr: Array<ByteArray>
        val Lag_CB_ptr: Array<ByteArray>

        Inlines.OpusAssert(complexity >= SilkConstants.SILK_PE_MIN_COMPLEX)
        Inlines.OpusAssert(complexity <= SilkConstants.SILK_PE_MAX_COMPLEX)

        if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
            Lag_range_ptr = SilkTables.silk_Lag_range_stage3[complexity]
            Lag_CB_ptr = SilkTables.silk_CB_lags_stage3
            nb_cbk_search = SilkTables.silk_nb_cbk_searchs_stage3[complexity].toInt()
        } else {
            Inlines.OpusAssert(nb_subfr == SilkConstants.PE_MAX_NB_SUBFR shr 1)
            Lag_range_ptr = SilkTables.silk_Lag_range_stage3_10_ms
            Lag_CB_ptr = SilkTables.silk_CB_lags_stage3_10_ms
            nb_cbk_search = SilkConstants.PE_NB_CBKS_STAGE3_10MS
        }
        scratch_mem = IntArray(SCRATCH_SIZE)

        target_ptr = Inlines.silk_LSHIFT(sf_length, 2)
        k = 0
        while (k < nb_subfr) {
            lag_counter = 0

            /* Calculate the energy for first lag */
            basis_ptr = target_ptr - (start_lag + Lag_range_ptr[k][0])
            energy = Inlines.silk_inner_prod_self(frame, basis_ptr, sf_length)
            Inlines.OpusAssert(energy >= 0)
            scratch_mem[lag_counter] = energy
            lag_counter++

            lag_diff = Lag_range_ptr[k][1] - Lag_range_ptr[k][0] + 1
            i = 1
            while (i < lag_diff) {
                /* remove part outside new window */
                energy -= Inlines.silk_SMULBB(
                    frame[basis_ptr + sf_length - i].toInt(),
                    frame[basis_ptr + sf_length - i].toInt()
                )
                Inlines.OpusAssert(energy >= 0)

                /* add part that comes into window */
                energy = Inlines.silk_ADD_SAT32(
                    energy,
                    Inlines.silk_SMULBB(frame[basis_ptr - i].toInt(), frame[basis_ptr - i].toInt())
                )
                Inlines.OpusAssert(energy >= 0)
                Inlines.OpusAssert(lag_counter < SCRATCH_SIZE)
                scratch_mem[lag_counter] = energy
                lag_counter++
                i++
            }

            delta = Lag_range_ptr[k][0].toInt()
            i = 0
            while (i < nb_cbk_search) {
                /* Fill out the 3 dim array that stores the correlations for    */
                /* each code_book vector for each start lag                     */
                idx = Lag_CB_ptr[k][i] - delta
                j = 0
                while (j < SilkConstants.PE_NB_STAGE3_LAGS) {
                    Inlines.OpusAssert(idx + j < SCRATCH_SIZE)
                    Inlines.OpusAssert(idx + j < lag_counter)
                    Inlines.MatrixGet(energies_st3, k, i, nb_cbk_search).Values[j] = scratch_mem[idx + j]
                    Inlines.OpusAssert(Inlines.MatrixGet(energies_st3, k, i, nb_cbk_search).Values[j] >= 0)
                    j++
                }
                i++
            }
            target_ptr += sf_length
            k++
        }
    }
}
