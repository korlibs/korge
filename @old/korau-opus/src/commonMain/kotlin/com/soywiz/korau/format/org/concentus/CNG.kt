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

/// <summary>
/// Comfort noise generation and estimation
/// </summary>
internal object CNG {

    /// <summary>
    /// Generates excitation for CNG LPC synthesis
    /// </summary>
    /// <param name="exc_Q10">O    CNG excitation signal Q10</param>
    /// <param name="exc_buf_Q14">I    Random samples buffer Q10</param>
    /// <param name="Gain_Q16">I    Gain to apply</param>
    /// <param name="length">I    Length</param>
    /// <param name="rand_seed">I/O  Seed to random index generator</param>
    fun silk_CNG_exc(
        exc_Q10: IntArray,
        exc_Q10_ptr: Int,
        exc_buf_Q14: IntArray,
        Gain_Q16: Int,
        length: Int,
        rand_seed: BoxedValueInt
    ) {
        var seed: Int
        var i: Int
        var idx: Int
        var exc_mask: Int

        exc_mask = SilkConstants.CNG_BUF_MASK_MAX

        while (exc_mask > length) {
            exc_mask = Inlines.silk_RSHIFT(exc_mask, 1)
        }

        seed = rand_seed.Val
        i = exc_Q10_ptr
        while (i < exc_Q10_ptr + length) {
            seed = Inlines.silk_RAND(seed)
            idx = (Inlines.silk_RSHIFT(seed, 24) and exc_mask).toInt()
            Inlines.OpusAssert(idx >= 0)
            Inlines.OpusAssert(idx <= SilkConstants.CNG_BUF_MASK_MAX)
            exc_Q10[i] = Inlines.silk_SAT16(Inlines.silk_SMULWW(exc_buf_Q14[idx], Gain_Q16 shr 4)).toShort().toInt()
            i++
        }

        rand_seed.Val = seed
    }

    /// <summary>
    /// Resets CNG state
    /// </summary>
    /// <param name="psDec">I/O  Decoder state</param>
    fun silk_CNG_Reset(psDec: SilkChannelDecoder) {
        var i: Int
        val NLSF_step_Q15: Int
        var NLSF_acc_Q15: Int

        NLSF_step_Q15 =
                Inlines.silk_DIV32_16(Short.MAX_VALUE.toInt(), (psDec.LPC_order + 1).toShort().toInt())
        NLSF_acc_Q15 = 0
        i = 0
        while (i < psDec.LPC_order) {
            NLSF_acc_Q15 += NLSF_step_Q15
            psDec.sCNG.CNG_smth_NLSF_Q15[i] = NLSF_acc_Q15.toShort()
            i++
        }
        psDec.sCNG.CNG_smth_Gain_Q16 = 0
        psDec.sCNG.rand_seed = 3176576
    }

    /// <summary>
    /// Updates CNG estimate, and applies the CNG when packet was lost
    /// </summary>
    /// <param name="psDec">I/O  Decoder state</param>
    /// <param name="psDecCtrl">I/O  Decoder control</param>
    /// <param name="frame">I/O  Signal</param>
    /// <param name="length">I    Length of residual</param>
    fun silk_CNG(
        psDec: SilkChannelDecoder,
        psDecCtrl: SilkDecoderControl,
        frame: ShortArray,
        frame_ptr: Int,
        length: Int
    ) {
        var i: Int
        var subfr: Int
        var sum_Q6: Int
        var max_Gain_Q16: Int
        var gain_Q16: Int
        val A_Q12 = ShortArray(psDec.LPC_order)
        val psCNG = psDec.sCNG

        if (psDec.fs_kHz != psCNG.fs_kHz) {
            /* Reset state */
            silk_CNG_Reset(psDec)

            psCNG.fs_kHz = psDec.fs_kHz
        }

        if (psDec.lossCnt == 0 && psDec.prevSignalType == SilkConstants.TYPE_NO_VOICE_ACTIVITY) {
            /* Update CNG parameters */

            /* Smoothing of LSF's  */
            i = 0
            while (i < psDec.LPC_order) {
                psCNG.CNG_smth_NLSF_Q15[i] = (psCNG.CNG_smth_NLSF_Q15[i].toInt() + Inlines.silk_SMULWB(
					psDec.prevNLSF_Q15[i].toInt() - psCNG.CNG_smth_NLSF_Q15[i].toInt(),
					SilkConstants.CNG_NLSF_SMTH_Q16
				).toShort().toInt()).toShort()
                i++
            }

            /* Find the subframe with the highest gain */
            max_Gain_Q16 = 0
            subfr = 0
            i = 0
            while (i < psDec.nb_subfr) {
                if (psDecCtrl.Gains_Q16[i] > max_Gain_Q16) {
                    max_Gain_Q16 = psDecCtrl.Gains_Q16[i]
                    subfr = i
                }
                i++
            }

            /* Update CNG excitation buffer with excitation from this subframe */
            Arrays.MemMove(psCNG.CNG_exc_buf_Q14, 0, psDec.subfr_length, (psDec.nb_subfr - 1) * psDec.subfr_length)

            /* Smooth gains */
            i = 0
            while (i < psDec.nb_subfr) {
                psCNG.CNG_smth_Gain_Q16 += Inlines.silk_SMULWB(
                    psDecCtrl.Gains_Q16[i] - psCNG.CNG_smth_Gain_Q16,
                    SilkConstants.CNG_GAIN_SMTH_Q16
                )
                i++
            }
        }

        /* Add CNG when packet is lost or during DTX */
        if (psDec.lossCnt != 0) {
            val CNG_sig_Q10 = IntArray(length + SilkConstants.MAX_LPC_ORDER)

            /* Generate CNG excitation */
            gain_Q16 = Inlines.silk_SMULWW(psDec.sPLC.randScale_Q14.toInt(), psDec.sPLC.prevGain_Q16[1])
            if (gain_Q16 >= 1 shl 21 || psCNG.CNG_smth_Gain_Q16 > 1 shl 23) {
                gain_Q16 = Inlines.silk_SMULTT(gain_Q16, gain_Q16)
                gain_Q16 = Inlines.silk_SUB_LSHIFT32(
                    Inlines.silk_SMULTT(psCNG.CNG_smth_Gain_Q16, psCNG.CNG_smth_Gain_Q16),
                    gain_Q16,
                    5
                )
                gain_Q16 = Inlines.silk_LSHIFT32(Inlines.silk_SQRT_APPROX(gain_Q16), 16)
            } else {
                gain_Q16 = Inlines.silk_SMULWW(gain_Q16, gain_Q16)
                gain_Q16 = Inlines.silk_SUB_LSHIFT32(
                    Inlines.silk_SMULWW(psCNG.CNG_smth_Gain_Q16, psCNG.CNG_smth_Gain_Q16),
                    gain_Q16,
                    5
                )
                gain_Q16 = Inlines.silk_LSHIFT32(Inlines.silk_SQRT_APPROX(gain_Q16), 8)
            }

            val boxed_rand_seed = BoxedValueInt(psCNG.rand_seed)
            silk_CNG_exc(
                CNG_sig_Q10,
                SilkConstants.MAX_LPC_ORDER,
                psCNG.CNG_exc_buf_Q14,
                gain_Q16,
                length,
                boxed_rand_seed
            )
            psCNG.rand_seed = boxed_rand_seed.Val

            /* Convert CNG NLSF to filter representation */
            NLSF.silk_NLSF2A(A_Q12, psCNG.CNG_smth_NLSF_Q15, psDec.LPC_order)

            /* Generate CNG signal, by synthesis filtering */
            arraycopy(psCNG.CNG_synth_state, 0, CNG_sig_Q10, 0, SilkConstants.MAX_LPC_ORDER)

            i = 0
            while (i < length) {
                val lpci = SilkConstants.MAX_LPC_ORDER + i
                Inlines.OpusAssert(psDec.LPC_order == 10 || psDec.LPC_order == 16)
                /* Avoids introducing a bias because silk_SMLAWB() always rounds to -inf */
                sum_Q6 = Inlines.silk_RSHIFT(psDec.LPC_order, 1)
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 1], A_Q12[0].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 2], A_Q12[1].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 3], A_Q12[2].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 4], A_Q12[3].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 5], A_Q12[4].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 6], A_Q12[5].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 7], A_Q12[6].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 8], A_Q12[7].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 9], A_Q12[8].toInt())
                sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 10], A_Q12[9].toInt())

                if (psDec.LPC_order == 16) {
                    sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 11], A_Q12[10].toInt())
                    sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 12], A_Q12[11].toInt())
                    sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 13], A_Q12[12].toInt())
                    sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 14], A_Q12[13].toInt())
                    sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 15], A_Q12[14].toInt())
                    sum_Q6 = Inlines.silk_SMLAWB(sum_Q6, CNG_sig_Q10[lpci - 16], A_Q12[15].toInt())
                }

                /* Update states */
                CNG_sig_Q10[lpci] = Inlines.silk_ADD_LSHIFT(CNG_sig_Q10[lpci], sum_Q6, 4)

                frame[frame_ptr + i] = Inlines.silk_ADD_SAT16(
                    frame[frame_ptr + i],
                    Inlines.silk_RSHIFT_ROUND(CNG_sig_Q10[lpci], 10).toShort()
                )
                i++
            }

            arraycopy(CNG_sig_Q10, length, psCNG.CNG_synth_state, 0, SilkConstants.MAX_LPC_ORDER)
        } else {
            Arrays.MemSet(psCNG.CNG_synth_state, 0, psDec.LPC_order)
        }
    }
}
