/* Copyright (c) 2007-2008 CSIRO
   Copyright (c) 2007-2011 Xiph.Org Foundation
   Originally written by Jean-Marc Valin, Gregory Maxwell, Koen Vos,
   Timothy B. Terriberry, and the Opus open-source contributors
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

internal object QuantizeBands {

    /* prediction coefficients: 0.9, 0.8, 0.65, 0.5 */
    private val pred_coef = intArrayOf(29440, 26112, 21248, 16384)
    private val beta_coef = intArrayOf(30147, 22282, 12124, 6554)
    private val beta_intra = 4915
    private val small_energy_icdf = shortArrayOf(2, 1, 0)

    fun loss_distortion(
        eBands: Array<IntArray>,
        oldEBands: Array<IntArray>,
        start: Int,
        end: Int,
        len: Int,
        C: Int
    ): Int {
        var c: Int
        var i: Int
        var dist = 0
        c = 0
        do {
            i = start
            while (i < end) {
                val d = Inlines.SUB16(Inlines.SHR16(eBands[c][i], 3), Inlines.SHR16(oldEBands[c][i], 3))
                dist = Inlines.MAC16_16(dist, d, d)
                i++
            }
        } while (++c < C)

        return Inlines.MIN32(200, Inlines.SHR32(dist, 2 * CeltConstants.DB_SHIFT - 6))
    }

    fun quant_coarse_energy_impl(
        m: CeltMode, start: Int, end: Int,
        eBands: Array<IntArray>, oldEBands: Array<IntArray>,
        budget: Int, tell: Int,
        prob_model: ShortArray, error: Array<IntArray>, enc: EntropyCoder,
        C: Int, LM: Int, intra: Int, max_decay: Int, lfe: Int
    ): Int {
        var tell = tell
        var i: Int
        var c: Int
        var badness = 0
        val prev = intArrayOf(0, 0)
        val coef: Int
        val beta: Int

        if (tell + 3 <= budget) {
            enc.enc_bit_logp(intra, 3)
        }

        if (intra != 0) {
            coef = 0
            beta = beta_intra
        } else {
            beta = beta_coef[LM]
            coef = pred_coef[LM]
        }

        /* Encode at a fixed coarse resolution */
        i = start
        while (i < end) {
            c = 0
            do {
                val bits_left: Int
                var qi: Int
                val qi0: Int
                val q: Int
                val x: Int
                val f: Int
                var tmp: Int
                val oldE: Int
                val decay_bound: Int
                x = eBands[c][i]
                oldE = Inlines.MAX16(
                    -(0.5 + 9.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(9.0f, CeltConstants.DB_SHIFT)*/,
                    oldEBands[c][i]
                )
                f = Inlines.SHL32(Inlines.EXTEND32(x), 7) - Inlines.PSHR32(Inlines.MULT16_16(coef, oldE), 8) - prev[c]
                /* Rounding to nearest integer here is really important! */
                qi = f + (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT + 7)).toInt() shr CeltConstants.DB_SHIFT + 7
                decay_bound = Inlines.EXTRACT16(
                    Inlines.MAX32(
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/,
                        Inlines.SUB32(oldEBands[c][i], max_decay)
                    )
                ).toInt()
                /* Prevent the energy from going down too quickly (e.g. for bands
                   that have just one bin) */
                if (qi < 0 && x < decay_bound) {
                    qi += Inlines.SHR16(Inlines.SUB16(decay_bound, x), CeltConstants.DB_SHIFT).toInt()
                    if (qi > 0) {
                        qi = 0
                    }
                }
                qi0 = qi
                /* If we don't have enough bits to encode all the energy, just assume
                    something safe. */
                tell = enc.tell()
                bits_left = budget - tell - 3 * C * (end - i)
                if (i != start && bits_left < 30) {
                    if (bits_left < 24) {
                        qi = Inlines.IMIN(1, qi)
                    }
                    if (bits_left < 16) {
                        qi = Inlines.IMAX(-1, qi)
                    }
                }
                if (lfe != 0 && i >= 2) {
                    qi = Inlines.IMIN(qi, 0)
                }
                if (budget - tell >= 15) {
                    val pi: Int
                    pi = 2 * Inlines.IMIN(i, 20)
                    val boxed_qi = BoxedValueInt(qi)
                    Laplace.ec_laplace_encode(
                        enc,
                        boxed_qi,
                        (prob_model[pi] shl 7).toLong(),
                        prob_model[pi + 1].toInt() shl 6
                    )
                    qi = boxed_qi.Val
                } else if (budget - tell >= 2) {
                    qi = Inlines.IMAX(-1, Inlines.IMIN(qi, 1))
                    enc.enc_icdf(2 * qi xor 0 - if (qi < 0) 1 else 0, small_energy_icdf, 2)
                } else if (budget - tell >= 1) {
                    qi = Inlines.IMIN(0, qi)
                    enc.enc_bit_logp(-qi, 1)
                } else {
                    qi = -1
                }
                error[c][i] = Inlines.PSHR32(f, 7) - Inlines.SHL16(qi, CeltConstants.DB_SHIFT)
                badness += Inlines.abs(qi0 - qi)
                q = Inlines.SHL32(qi, CeltConstants.DB_SHIFT).toInt()

                tmp = Inlines.PSHR32(Inlines.MULT16_16(coef, oldE), 8) + prev[c] + Inlines.SHL32(q, 7)
                tmp = Inlines.MAX32(
                    -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT + 7)).toInt()/*Inlines.QCONST32(28.0f, CeltConstants.DB_SHIFT + 7)*/,
                    tmp
                )
                oldEBands[c][i] = Inlines.PSHR32(tmp, 7)
                prev[c] = prev[c] + Inlines.SHL32(q, 7) - Inlines.MULT16_16(beta, Inlines.PSHR32(q, 8))
            } while (++c < C)
            i++
        }
        return if (lfe != 0) 0 else badness
    }

    fun quant_coarse_energy(
        m: CeltMode, start: Int, end: Int, effEnd: Int,
        eBands: Array<IntArray>, oldEBands: Array<IntArray>, budget: Int,
        error: Array<IntArray>, enc: EntropyCoder, C: Int, LM: Int, nbAvailableBytes: Int,
        force_intra: Int, delayedIntra: BoxedValueInt, two_pass: Int, loss_rate: Int, lfe: Int
    ) {
        var two_pass = two_pass
        var intra: Int
        var max_decay: Int
        val oldEBands_intra: Array<IntArray>
        val error_intra: Array<IntArray>
        val enc_start_state = EntropyCoder() // [porting note] stack variable
        val tell: Int
        var badness1 = 0
        val intra_bias: Int
        val new_distortion: Int

        intra =
                if (force_intra != 0 || two_pass == 0 && delayedIntra.Val > 2 * C * (end - start) && nbAvailableBytes > (end - start) * C) 1 else 0
        intra_bias = budget * delayedIntra.Val * loss_rate / (C * 512)
        new_distortion = loss_distortion(eBands, oldEBands, start, effEnd, m.nbEBands, C)

        tell = enc.tell()
        if (tell + 3 > budget) {
            intra = 0
            two_pass = intra
        }

        max_decay = (0.5 + 16.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
            .toInt()/*Inlines.QCONST16(16.0f, CeltConstants.DB_SHIFT)*/
        if (end - start > 10) {
            max_decay = Inlines.MIN32(
                max_decay,
                Inlines.SHL32(nbAvailableBytes, CeltConstants.DB_SHIFT - 3)
            ) // opus bug: useless extend32
        }
        if (lfe != 0) {
            max_decay = (0.5 + 3.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                .toInt()/*Inlines.QCONST16(3.0f, CeltConstants.DB_SHIFT)*/
        }
        enc_start_state.Assign(enc)

        oldEBands_intra = Arrays.InitTwoDimensionalArrayInt(C, m.nbEBands)
        error_intra = Arrays.InitTwoDimensionalArrayInt(C, m.nbEBands)
        arraycopy(oldEBands[0], 0, oldEBands_intra[0], 0, m.nbEBands)
        if (C == 2) {
            arraycopy(oldEBands[1], 0, oldEBands_intra[1], 0, m.nbEBands)
        }

        if (two_pass != 0 || intra != 0) {
            badness1 = quant_coarse_energy_impl(
                m, start, end, eBands, oldEBands_intra, budget,
                tell, CeltTables.e_prob_model[LM][1], error_intra, enc, C, LM, 1, max_decay, lfe
            )
        }

        if (intra == 0) {
            val intra_buf: Int
            val enc_intra_state = EntropyCoder() // [porting note] stack variable
            val tell_intra: Int
            val nstart_bytes: Int
            val nintra_bytes: Int
            val save_bytes: Int
            val badness2: Int
            var intra_bits: ByteArray? = null

            tell_intra = enc.tell_frac().toInt()

            enc_intra_state.Assign(enc)

            nstart_bytes = enc_start_state.range_bytes()
            nintra_bytes = enc_intra_state.range_bytes()
            intra_buf = nstart_bytes
            save_bytes = nintra_bytes - nstart_bytes

            if (save_bytes != 0) {
                intra_bits = ByteArray(save_bytes)
                /* Copy bits from intra bit-stream */
                arraycopy(enc_intra_state._buffer, intra_buf, intra_bits, 0, save_bytes)
            }

            enc.Assign(enc_start_state)

            badness2 = quant_coarse_energy_impl(
                m, start, end, eBands, oldEBands, budget,
                tell, CeltTables.e_prob_model[LM][intra], error, enc, C, LM, 0, max_decay, lfe
            )

            if (two_pass != 0 && (badness1 < badness2 || badness1 == badness2 && enc.tell_frac() + intra_bias > tell_intra)) {
                enc.Assign(enc_intra_state)
                /* Copy intra bits to bit-stream */
                if (intra_bits != null) {
                    enc_intra_state.write_buffer(intra_bits, 0, intra_buf, nintra_bytes - nstart_bytes)
                }
                arraycopy(oldEBands_intra[0], 0, oldEBands[0], 0, m.nbEBands)
                arraycopy(error_intra[0], 0, error[0], 0, m.nbEBands)
                if (C == 2) {
                    arraycopy(oldEBands_intra[1], 0, oldEBands[1], 0, m.nbEBands)
                    arraycopy(error_intra[1], 0, error[1], 0, m.nbEBands)
                }
                intra = 1
            }
        } else {
            arraycopy(oldEBands_intra[0], 0, oldEBands[0], 0, m.nbEBands)
            arraycopy(error_intra[0], 0, error[0], 0, m.nbEBands)
            if (C == 2) {
                arraycopy(oldEBands_intra[1], 0, oldEBands[1], 0, m.nbEBands)
                arraycopy(error_intra[1], 0, error[1], 0, m.nbEBands)
            }
        }

        if (intra != 0) {
            delayedIntra.Val = new_distortion
        } else {
            delayedIntra.Val = Inlines.ADD32(
                Inlines.MULT16_32_Q15(Inlines.MULT16_16_Q15(pred_coef[LM], pred_coef[LM]), delayedIntra.Val),
                new_distortion
            )
        }
    }

    fun quant_fine_energy(
        m: CeltMode,
        start: Int,
        end: Int,
        oldEBands: Array<IntArray>,
        error: Array<IntArray>,
        fine_quant: IntArray,
        enc: EntropyCoder,
        C: Int
    ) {
        var i: Int
        var c: Int

        /* Encode finer resolution */
        i = start
        while (i < end) {
            val frac = 1 shl fine_quant[i]
            if (fine_quant[i] <= 0) {
                i++
                continue
            }
            c = 0
            do {
                var q2: Int
                val offset: Int
                /* Has to be without rounding */
                q2 = error[c][i] + (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort() shr CeltConstants.DB_SHIFT -
                        fine_quant[i]
                if (q2 > frac - 1) {
                    q2 = frac - 1
                }
                if (q2 < 0) {
                    q2 = 0
                }
                enc.enc_bits(q2.toLong(), fine_quant[i])
                offset = Inlines.SUB16(
                    Inlines.SHR32(
                        Inlines.SHL32(
                            q2,
                            CeltConstants.DB_SHIFT
                        ) + (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/,
                        fine_quant[i]
                    ),
                    (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/
                )
                oldEBands[c][i] += offset
                error[c][i] -= offset
            } while (++c < C)
            i++
        }
    }

    fun quant_energy_finalise(
        m: CeltMode,
        start: Int,
        end: Int,
        oldEBands: Array<IntArray>,
        error: Array<IntArray>,
        fine_quant: IntArray,
        fine_priority: IntArray,
        bits_left: Int,
        enc: EntropyCoder,
        C: Int
    ) {
        var bits_left = bits_left
        var i: Int
        var prio: Int
        var c: Int

        /* Use up the remaining bits */
        prio = 0
        while (prio < 2) {
            i = start
            while (i < end && bits_left >= C) {
                if (fine_quant[i] >= CeltConstants.MAX_FINE_BITS || fine_priority[i] != prio) {
                    i++
                    continue
                }

                c = 0
                do {
                    val q2: Int
                    val offset: Int
                    q2 = if (error[c][i] < 0) 0 else 1
                    enc.enc_bits(q2.toLong(), 1)
                    offset = Inlines.SHR16(
                        Inlines.SHL16(
                            q2,
                            CeltConstants.DB_SHIFT
                        ) - (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort(), fine_quant[i] + 1
                    )
                    oldEBands[c][i] += offset
                    bits_left--
                } while (++c < C)
                i++
            }
            prio++
        }
    }

    fun unquant_coarse_energy(
        m: CeltMode,
        start: Int,
        end: Int,
        oldEBands: IntArray,
        intra: Int,
        dec: EntropyCoder,
        C: Int,
        LM: Int
    ) {
        val prob_model = CeltTables.e_prob_model[LM][intra]
        var i: Int
        var c: Int
        val prev = intArrayOf(0, 0)
        val coef: Int
        val beta: Int
        val budget: Int
        var tell: Int

        if (intra != 0) {
            coef = 0
            beta = beta_intra
        } else {
            beta = beta_coef[LM]
            coef = pred_coef[LM]
        }

        budget = dec.storage.toInt() * 8

        /* Decode at a fixed coarse resolution */
        i = start
        while (i < end) {
            c = 0
            do {
                var qi: Int
                val q: Int
                var tmp: Int
                /* It would be better to express this invariant as a
                   test on C at function entry, but that isn't enough
                   to make the static analyzer happy. */
                Inlines.OpusAssert(c < 2)
                tell = dec.tell()
                if (budget - tell >= 15) {
                    val pi: Int
                    pi = 2 * Inlines.IMIN(i, 20)
                    qi = Laplace.ec_laplace_decode(
                        dec,
                        (prob_model[pi] shl 7).toLong(), prob_model[pi + 1] shl 6
                    )
                } else if (budget - tell >= 2) {
                    qi = dec.dec_icdf(small_energy_icdf, 2)
                    qi = qi shr 1 xor -(qi and 1)
                } else if (budget - tell >= 1) {
                    qi = 0 - dec.dec_bit_logp(1)
                } else {
                    qi = -1
                }
                q = Inlines.SHL32(qi, CeltConstants.DB_SHIFT).toInt() // opus bug: useless extend32

                oldEBands[i + c * m.nbEBands] = Inlines.MAX16(
                    0 - (0.5 + 9.0f * (1 shl CeltConstants.DB_SHIFT)).toShort(),
                    oldEBands[i + c * m.nbEBands]
                )
                tmp = Inlines.PSHR32(Inlines.MULT16_16(coef, oldEBands[i + c * m.nbEBands]), 8) + prev[c] +
                        Inlines.SHL32(q, 7)
                tmp = Inlines.MAX32(
                    -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT + 7)).toInt()/*Inlines.QCONST32(28.0f, CeltConstants.DB_SHIFT + 7)*/,
                    tmp
                )
                oldEBands[i + c * m.nbEBands] = Inlines.PSHR32(tmp, 7)
                prev[c] = prev[c] + Inlines.SHL32(q, 7) - Inlines.MULT16_16(beta, Inlines.PSHR32(q, 8))
            } while (++c < C)
            i++
        }
    }

    fun unquant_fine_energy(
        m: CeltMode,
        start: Int,
        end: Int,
        oldEBands: IntArray,
        fine_quant: IntArray,
        dec: EntropyCoder,
        C: Int
    ) {
        var i: Int
        var c: Int
        /* Decode finer resolution */
        i = start
        while (i < end) {
            if (fine_quant[i] <= 0) {
                i++
                continue
            }
            c = 0
            do {
                val q2: Int
                val offset: Int
                q2 = dec.dec_bits(fine_quant[i])
                offset = Inlines.SUB16(
                    Inlines.SHR32(
                        Inlines.SHL32(
                            q2,
                            CeltConstants.DB_SHIFT
                        ) + (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/,
                        fine_quant[i]
                    ),
                    (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(.5f, CeltConstants.DB_SHIFT)*/
                ) // opus bug: unnecessary extend32
                oldEBands[i + c * m.nbEBands] += offset
            } while (++c < C)
            i++
        }
    }

    fun unquant_energy_finalise(
        m: CeltMode,
        start: Int,
        end: Int,
        oldEBands: IntArray,
        fine_quant: IntArray,
        fine_priority: IntArray,
        bits_left: Int,
        dec: EntropyCoder,
        C: Int
    ) {
        var bits_left = bits_left
        var i: Int
        var prio: Int
        var c: Int

        /* Use up the remaining bits */
        prio = 0
        while (prio < 2) {
            i = start
            while (i < end && bits_left >= C) {
                if (fine_quant[i] >= CeltConstants.MAX_FINE_BITS || fine_priority[i] != prio) {
                    i++
                    continue
                }
                c = 0
                do {
                    val q2: Int
                    val offset: Int
                    q2 = dec.dec_bits(1)
                    offset = Inlines.SHR16(
                        Inlines.SHL16(
                            q2,
                            CeltConstants.DB_SHIFT
                        ) - (0.5 + .5f * (1 shl CeltConstants.DB_SHIFT)).toShort(), fine_quant[i] + 1
                    )
                    oldEBands[i + c * m.nbEBands] += offset
                    bits_left--
                } while (++c < C)
                i++
            }
            prio++
        }
    }

    /// <summary>
    /// non-pointer case
    /// </summary>
    /// <param name="m"></param>
    /// <param name="effEnd"></param>
    /// <param name="end"></param>
    /// <param name="bandE"></param>
    /// <param name="bandLogE"></param>
    /// <param name="C"></param>
    fun amp2Log2(
        m: CeltMode, effEnd: Int, end: Int,
        bandE: Array<IntArray>, bandLogE: Array<IntArray>, C: Int
    ) {
        var c: Int
        var i: Int
        c = 0
        do {
            i = 0
            while (i < effEnd) {
                bandLogE[c][i] = Inlines.celt_log2(Inlines.SHL32(bandE[c][i], 2)) -
                        Inlines.SHL16(CeltTables.eMeans[i].toInt(), 6)
                i++
            }
            i = effEnd
            while (i < end) {
                bandLogE[c][i] = 0 - (0.5 + 14.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                i++
            }
        } while (++c < C)
    }

    /// <summary>
    /// only needed in one place
    /// </summary>
    /// <param name="m"></param>
    /// <param name="effEnd"></param>
    /// <param name="end"></param>
    /// <param name="bandE"></param>
    /// <param name="bandLogE"></param>
    /// <param name="C"></param>
    fun amp2Log2(
        m: CeltMode, effEnd: Int, end: Int,
        bandE: IntArray, bandLogE: IntArray, bandLogE_ptr: Int, C: Int
    ) {
        var c: Int
        var i: Int
        c = 0
        do {
            i = 0
            while (i < effEnd) {
                bandLogE[bandLogE_ptr + c * m.nbEBands + i] = Inlines.celt_log2(
                    Inlines.SHL32(
                        bandE[i + c * m.nbEBands],
                        2
                    )
                ) - Inlines.SHL16(CeltTables.eMeans[i].toInt(), 6)
                i++
            }
            i = effEnd
            while (i < end) {
                bandLogE[bandLogE_ptr + c * m.nbEBands + i] = 0 -
                        (0.5 + 14.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                i++
            }
        } while (++c < C)
    }
}
