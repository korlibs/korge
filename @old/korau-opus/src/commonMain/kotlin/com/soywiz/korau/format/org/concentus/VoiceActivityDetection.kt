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
/// Voice Activity Detection module for silk codec
/// </summary>
internal object VoiceActivityDetection {

    /// <summary>
    /// Weighting factors for tilt measure
    /// </summary>
    private val tiltWeights = intArrayOf(30000, 6000, -12000, -12000)

    /// <summary>
    /// Initialization of the Silk VAD
    /// </summary>
    /// <param name="psSilk_VAD">O  Pointer to Silk VAD state. Cannot be nullptr</param>
    /// <returns>0 if success</returns>
    fun silk_VAD_Init(psSilk_VAD: SilkVADState): Int {
        var b: Int
        val ret = 0

        /* reset state memory */
        psSilk_VAD.Reset()

        /* init noise levels */
        /* Initialize array with approx pink noise levels (psd proportional to inverse of frequency) */
        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            psSilk_VAD.NoiseLevelBias[b] = Inlines.silk_max_32(
                Inlines.silk_DIV32_16(
                    SilkConstants.VAD_NOISE_LEVELS_BIAS,
                    (b + 1).toShort().toInt()
                ), 1
            )
            b++
        }

        /* Initialize state */
        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            psSilk_VAD.NL[b] = Inlines.silk_MUL(100, psSilk_VAD.NoiseLevelBias[b])
            psSilk_VAD.inv_NL[b] = Inlines.silk_DIV32(Int.MAX_VALUE, psSilk_VAD.NL[b])
            b++
        }

        psSilk_VAD.counter = 15

        /* init smoothed energy-to-noise ratio*/
        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            psSilk_VAD.NrgRatioSmth_Q8[b] = 100 * 256
            b++
            /* 100 * 256 -. 20 dB SNR */
        }

        return ret
    }

    /// <summary>
    /// Get the speech activity level in Q8
    /// </summary>
    /// <param name="psEncC">I/O  Encoder state</param>
    /// <param name="pIn">I    PCM input</param>
    /// <returns>0 if success</returns>
    fun silk_VAD_GetSA_Q8(
        psEncC: SilkChannelEncoder,
        pIn: ShortArray,
        pIn_ptr: Int
    ): Int {
        var SA_Q15: Int
        val pSNR_dB_Q7: Int
        var input_tilt: Int
        val decimated_framelength1: Int
        val decimated_framelength2: Int
        var decimated_framelength: Int
        var dec_subframe_length: Int
        var dec_subframe_offset: Int
        var SNR_Q7: Int
        var i: Int
        var b: Int
        var s: Int
        var sumSquared = 0
        var smooth_coef_Q16: Int
        val HPstateTmp: Short
        val X: ShortArray
        val Xnrg = IntArray(SilkConstants.VAD_N_BANDS)
        val NrgToNoiseRatio_Q8 = IntArray(SilkConstants.VAD_N_BANDS)
        var speech_nrg: Int
        var x_tmp: Int
        val X_offset = IntArray(SilkConstants.VAD_N_BANDS)
        val ret = 0
        val psSilk_VAD = psEncC.sVAD

        /* Safety checks */
        Inlines.OpusAssert(SilkConstants.VAD_N_BANDS == 4)
        Inlines.OpusAssert(SilkConstants.MAX_FRAME_LENGTH >= psEncC.frame_length)
        Inlines.OpusAssert(psEncC.frame_length <= 512)
        Inlines.OpusAssert(psEncC.frame_length == 8 * Inlines.silk_RSHIFT(psEncC.frame_length, 3))

        /**
         * ********************
         */
        /* Filter and Decimate */
        /**
         * ********************
         */
        decimated_framelength1 = Inlines.silk_RSHIFT(psEncC.frame_length, 1)
        decimated_framelength2 = Inlines.silk_RSHIFT(psEncC.frame_length, 2)
        decimated_framelength = Inlines.silk_RSHIFT(psEncC.frame_length, 3)

        /* Decimate into 4 bands:
           0       L      3L       L              3L                             5L
                   -      --       -              --                             --
                   8       8       2               4                              4

           [0-1 kHz| temp. |1-2 kHz|    2-4 kHz    |            4-8 kHz           |

           They're arranged to allow the minimal ( frame_length / 4 ) extra
           scratch space during the downsampling process */
        X_offset[0] = 0
        X_offset[1] = decimated_framelength + decimated_framelength2
        X_offset[2] = X_offset[1] + decimated_framelength
        X_offset[3] = X_offset[2] + decimated_framelength2
        X = ShortArray(X_offset[3] + decimated_framelength1)

        /* 0-8 kHz to 0-4 kHz and 4-8 kHz */
        Filters.silk_ana_filt_bank_1(
            pIn, pIn_ptr, psSilk_VAD.AnaState,
            X, X, X_offset[3], psEncC.frame_length
        )

        /* 0-4 kHz to 0-2 kHz and 2-4 kHz */
        Filters.silk_ana_filt_bank_1(
            X, 0, psSilk_VAD.AnaState1,
            X, X, X_offset[2], decimated_framelength1
        )

        /* 0-2 kHz to 0-1 kHz and 1-2 kHz */
        Filters.silk_ana_filt_bank_1(
            X, 0, psSilk_VAD.AnaState2,
            X, X, X_offset[1], decimated_framelength2
        )

        /**
         * ******************************************
         */
        /* HP filter on lowest band (differentiator) */
        /**
         * ******************************************
         */
        X[decimated_framelength - 1] = Inlines.silk_RSHIFT(X[decimated_framelength - 1].toInt(), 1).toShort()
        HPstateTmp = X[decimated_framelength - 1]

        i = decimated_framelength - 1
        while (i > 0) {
            X[i - 1] = Inlines.silk_RSHIFT(X[i - 1].toInt(), 1).toShort()
            X[i] = (X[i] - X[i - 1]).toShort()
            i--
        }

        X[0] = (X[0] - psSilk_VAD.HPstate).toShort()
        psSilk_VAD.HPstate = HPstateTmp

        /**
         * **********************************
         */
        /* Calculate the energy in each band */
        /**
         * **********************************
         */
        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            /* Find the decimated framelength in the non-uniformly divided bands */
            decimated_framelength = Inlines.silk_RSHIFT(
                psEncC.frame_length,
                Inlines.silk_min_int(SilkConstants.VAD_N_BANDS - b, SilkConstants.VAD_N_BANDS - 1)
            )

            /* Split length into subframe lengths */
            dec_subframe_length = Inlines.silk_RSHIFT(decimated_framelength, SilkConstants.VAD_INTERNAL_SUBFRAMES_LOG2)
            dec_subframe_offset = 0

            /* Compute energy per sub-frame */
            /* initialize with summed energy of last subframe */
            Xnrg[b] = psSilk_VAD.XnrgSubfr[b]
            s = 0
            while (s < SilkConstants.VAD_INTERNAL_SUBFRAMES) {
                sumSquared = 0

                i = 0
                while (i < dec_subframe_length) {
                    /* The energy will be less than dec_subframe_length * ( silk_int16_MIN / 8 ) ^ 2.            */
                    /* Therefore we can accumulate with no risk of overflow (unless dec_subframe_length > 128)  */
                    x_tmp = Inlines.silk_RSHIFT(
                        X[X_offset[b] + i + dec_subframe_offset].toInt(), 3
                    )
                    sumSquared = Inlines.silk_SMLABB(sumSquared, x_tmp, x_tmp)
                    /* Safety check */
                    Inlines.OpusAssert(sumSquared >= 0)
                    i++
                }

                /* Add/saturate summed energy of current subframe */
                if (s < SilkConstants.VAD_INTERNAL_SUBFRAMES - 1) {
                    Xnrg[b] = Inlines.silk_ADD_POS_SAT32(Xnrg[b], sumSquared)
                } else {
                    /* Look-ahead subframe */
                    Xnrg[b] = Inlines.silk_ADD_POS_SAT32(Xnrg[b], Inlines.silk_RSHIFT(sumSquared, 1))
                }

                dec_subframe_offset += dec_subframe_length
                s++
            }

            psSilk_VAD.XnrgSubfr[b] = sumSquared
            b++
        }

        /**
         * *****************
         */
        /* Noise estimation */
        /**
         * *****************
         */
        silk_VAD_GetNoiseLevels(Xnrg, psSilk_VAD)

        /**
         * ********************************************
         */
        /* Signal-plus-noise to noise ratio estimation */
        /**
         * ********************************************
         */
        sumSquared = 0
        input_tilt = 0
        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            speech_nrg = Xnrg[b] - psSilk_VAD.NL[b]
            if (speech_nrg > 0) {
                /* Divide, with sufficient resolution */
                if (Xnrg[b] and -0x800000 == 0) {
                    NrgToNoiseRatio_Q8[b] = Inlines.silk_DIV32(Inlines.silk_LSHIFT(Xnrg[b], 8), psSilk_VAD.NL[b] + 1)
                } else {
                    NrgToNoiseRatio_Q8[b] = Inlines.silk_DIV32(Xnrg[b], Inlines.silk_RSHIFT(psSilk_VAD.NL[b], 8) + 1)
                }

                /* Convert to log domain */
                SNR_Q7 = Inlines.silk_lin2log(NrgToNoiseRatio_Q8[b]) - 8 * 128

                /* Sum-of-squares */
                sumSquared = Inlines.silk_SMLABB(sumSquared, SNR_Q7, SNR_Q7)
                /* Q14 */

                /* Tilt measure */
                if (speech_nrg < 1 shl 20) {
                    /* Scale down SNR value for small subband speech energies */
                    SNR_Q7 = Inlines.silk_SMULWB(Inlines.silk_LSHIFT(Inlines.silk_SQRT_APPROX(speech_nrg), 6), SNR_Q7)
                }
                input_tilt = Inlines.silk_SMLAWB(input_tilt, tiltWeights[b], SNR_Q7)
            } else {
                NrgToNoiseRatio_Q8[b] = 256
            }
            b++
        }

        /* Mean-of-squares */
        sumSquared = Inlines.silk_DIV32_16(sumSquared, SilkConstants.VAD_N_BANDS)
        /* Q14 */

        /* Root-mean-square approximation, scale to dBs, and write to output pointer */
        pSNR_dB_Q7 = (3 * Inlines.silk_SQRT_APPROX(sumSquared)).toShort().toInt()
        /* Q7 */

        /**
         * ******************************
         */
        /* Speech Probability Estimation */
        /**
         * ******************************
         */
        SA_Q15 = Sigmoid.silk_sigm_Q15(
            Inlines.silk_SMULWB(
                SilkConstants.VAD_SNR_FACTOR_Q16,
                pSNR_dB_Q7
            ) - SilkConstants.VAD_NEGATIVE_OFFSET_Q5
        )

        /**
         * ***********************
         */
        /* Frequency Tilt Measure */
        /**
         * ***********************
         */
        psEncC.input_tilt_Q15 = Inlines.silk_LSHIFT(Sigmoid.silk_sigm_Q15(input_tilt) - 16384, 1)

        /**
         * ***********************************************
         */
        /* Scale the sigmoid output based on power levels */
        /**
         * ***********************************************
         */
        speech_nrg = 0
        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            /* Accumulate signal-without-noise energies, higher frequency bands have more weight */
            speech_nrg += (b + 1) * Inlines.silk_RSHIFT(Xnrg[b] - psSilk_VAD.NL[b], 4)
            b++
        }

        /* Power scaling */
        if (speech_nrg <= 0) {
            SA_Q15 = Inlines.silk_RSHIFT(SA_Q15, 1)
        } else if (speech_nrg < 32768) {
            if (psEncC.frame_length == 10 * psEncC.fs_kHz) {
                speech_nrg = Inlines.silk_LSHIFT_SAT32(speech_nrg, 16)
            } else {
                speech_nrg = Inlines.silk_LSHIFT_SAT32(speech_nrg, 15)
            }

            /* square-root */
            speech_nrg = Inlines.silk_SQRT_APPROX(speech_nrg)
            SA_Q15 = Inlines.silk_SMULWB(32768 + speech_nrg, SA_Q15)
        }

        /* Copy the resulting speech activity in Q8 */
        psEncC.speech_activity_Q8 = Inlines.silk_min_int(Inlines.silk_RSHIFT(SA_Q15, 7), 255)

        /**
         * ********************************
         */
        /* Energy Level and SNR estimation */
        /**
         * ********************************
         */
        /* Smoothing coefficient */
        smooth_coef_Q16 =
                Inlines.silk_SMULWB(SilkConstants.VAD_SNR_SMOOTH_COEF_Q18, Inlines.silk_SMULWB(SA_Q15, SA_Q15))

        if (psEncC.frame_length == 10 * psEncC.fs_kHz) {
            smooth_coef_Q16 = smooth_coef_Q16 shr 1
        }

        b = 0
        while (b < SilkConstants.VAD_N_BANDS) {
            /* compute smoothed energy-to-noise ratio per band */
            psSilk_VAD.NrgRatioSmth_Q8[b] = Inlines.silk_SMLAWB(
                psSilk_VAD.NrgRatioSmth_Q8[b],
                NrgToNoiseRatio_Q8[b] - psSilk_VAD.NrgRatioSmth_Q8[b], smooth_coef_Q16
            )
            /* signal to noise ratio in dB per band */
            SNR_Q7 = 3 * (Inlines.silk_lin2log(psSilk_VAD.NrgRatioSmth_Q8[b]) - 8 * 128)
            /* quality = sigmoid( 0.25 * ( SNR_dB - 16 ) ); */
            psEncC.input_quality_bands_Q15[b] = Sigmoid.silk_sigm_Q15(Inlines.silk_RSHIFT(SNR_Q7 - 16 * 128, 4))
            b++
        }

        return ret
    }

    /// <summary>
    /// Noise level estimation
    /// </summary>
    /// <param name="pX">I    subband energies [VAD_N_BANDS]</param>
    /// <param name="psSilk_VAD">I/O  Pointer to Silk VAD state</param>
    fun silk_VAD_GetNoiseLevels(
        pX: IntArray,
        psSilk_VAD: SilkVADState
    ) {
        var k: Int
        var nl: Int
        var nrg: Int
        var inv_nrg: Int
        var coef: Int
        val min_coef: Int

        /* Initially faster smoothing */
        if (psSilk_VAD.counter < 1000) {
            /* 1000 = 20 sec */
            min_coef = Inlines.silk_DIV32_16(
                Short.MAX_VALUE.toInt(),
                (Inlines.silk_RSHIFT(psSilk_VAD.counter, 4) + 1).toShort().toInt()
            )
        } else {
            min_coef = 0
        }

        k = 0
        while (k < SilkConstants.VAD_N_BANDS) {
            /* Get old noise level estimate for current band */
            nl = psSilk_VAD.NL[k]
            Inlines.OpusAssert(nl >= 0)

            /* Add bias */
            nrg = Inlines.silk_ADD_POS_SAT32(pX[k], psSilk_VAD.NoiseLevelBias[k])
            Inlines.OpusAssert(nrg > 0)

            /* Invert energies */
            inv_nrg = Inlines.silk_DIV32(Int.MAX_VALUE, nrg)
            Inlines.OpusAssert(inv_nrg >= 0)

            /* Less update when subband energy is high */
            if (nrg > Inlines.silk_LSHIFT(nl, 3)) {
                coef = SilkConstants.VAD_NOISE_LEVEL_SMOOTH_COEF_Q16 shr 3
            } else if (nrg < nl) {
                coef = SilkConstants.VAD_NOISE_LEVEL_SMOOTH_COEF_Q16
            } else {
                coef = Inlines.silk_SMULWB(
                    Inlines.silk_SMULWW(inv_nrg, nl),
                    SilkConstants.VAD_NOISE_LEVEL_SMOOTH_COEF_Q16 shl 1
                )
            }

            /* Initially faster smoothing */
            coef = Inlines.silk_max_int(coef, min_coef)

            /* Smooth inverse energies */
            psSilk_VAD.inv_NL[k] = Inlines.silk_SMLAWB(psSilk_VAD.inv_NL[k], inv_nrg - psSilk_VAD.inv_NL[k], coef)
            Inlines.OpusAssert(psSilk_VAD.inv_NL[k] >= 0)

            /* Compute noise level by inverting again */
            nl = Inlines.silk_DIV32(Int.MAX_VALUE, psSilk_VAD.inv_NL[k])
            Inlines.OpusAssert(nl >= 0)

            /* Limit noise levels (guarantee 7 bits of head room) */
            nl = Inlines.silk_min(nl, 0x00FFFFFF)

            /* Store as part of state */
            psSilk_VAD.NL[k] = nl
            k++
        }

        /* Increment frame counter */
        psSilk_VAD.counter++
    }
}
