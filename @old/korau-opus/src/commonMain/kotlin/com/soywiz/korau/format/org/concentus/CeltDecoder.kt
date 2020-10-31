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

/// <summary>
/// Decoder state
/// </summary>
internal class CeltDecoder {

    var mode: CeltMode? = null
    var overlap = 0
    var channels = 0
    var stream_channels = 0

    var downsample = 0
    var start = 0
    var end = 0
    var signalling = 0

    /* Everything beyond this point gets cleared on a reset */
    var rng = 0
    var error = 0
    var last_pitch_index = 0
    var loss_count = 0
    var postfilter_period = 0
    var postfilter_period_old = 0
    var postfilter_gain = 0
    var postfilter_gain_old = 0
    var postfilter_tapset = 0
    var postfilter_tapset_old = 0

    val preemph_memD = IntArray(2)

    /// <summary>
    /// Scratch space used by the decoder. It is actually a variable-sized
    /// field that resulted in a variable-sized struct. There are 6 distinct regions inside.
    /// I have laid them out into separate variables here,
    /// but these were the original definitions:
    /// val32 decode_mem[],     Size = channels*(DECODE_BUFFER_SIZE+mode.overlap)
    /// val16 lpc[],            Size = channels*LPC_ORDER
    /// val16 oldEBands[],      Size = 2*mode.nbEBands
    /// val16 oldLogE[],        Size = 2*mode.nbEBands
    /// val16 oldLogE2[],       Size = 2*mode.nbEBands
    /// val16 backgroundLogE[], Size = 2*mode.nbEBands
    /// </summary>
    var decode_mem: Array<IntArray>? = null
    var lpc: Array<IntArray>? = null // Porting note: Split two-part array into separate arrays (one per channel)
    var oldEBands: IntArray? = null
    var oldLogE: IntArray? = null
    var oldLogE2: IntArray? = null
    var backgroundLogE: IntArray? = null

    private fun Reset() {
        mode =
                null
        overlap = 0
        channels = 0
        stream_channels = 0
        downsample = 0
        start = 0
        end = 0
        signalling = 0
        PartialReset()
    }

    private fun PartialReset() {
        rng = 0
        error = 0
        last_pitch_index = 0
        loss_count = 0
        postfilter_period = 0
        postfilter_period_old = 0
        postfilter_gain = 0
        postfilter_gain_old = 0
        postfilter_tapset = 0
        postfilter_tapset_old = 0
        Arrays.MemSet(preemph_memD, 0, 2)
        decode_mem = null
        lpc = null
        oldEBands = null
        oldLogE = null
        oldLogE2 = null
        backgroundLogE = null
    }

    fun ResetState() {
        var i: Int

        this.PartialReset()

        // We have to reconstitute the dynamic buffers here. fixme: this could be better implemented
        this.decode_mem = Array(this.channels) { IntArray(CeltConstants.DECODE_BUFFER_SIZE + this.mode!!.overlap) }
        this.lpc = Array(this.channels) { IntArray(CeltConstants.LPC_ORDER) }
        this.oldEBands = IntArray(2 * this.mode!!.nbEBands)
        this.oldLogE = IntArray(2 * this.mode!!.nbEBands)
        this.oldLogE2 = IntArray(2 * this.mode!!.nbEBands)
        this.backgroundLogE = IntArray(2 * this.mode!!.nbEBands)

        i = 0
        while (i < 2 * this.mode!!.nbEBands) {
            this.oldLogE2!![i] =
                    -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
            this.oldLogE!![i] = this.oldLogE2!![i]
            i++
        }
    }

    fun celt_decoder_init(sampling_rate: Int, channels: Int): Int {
        val ret: Int
        ret = this.opus_custom_decoder_init(CeltMode.mode48000_960_120, channels)
        if (ret != OpusError.OPUS_OK) {
            return ret
        }
        this.downsample = CeltCommon.resampling_factor(sampling_rate)
        return if (this.downsample == 0) {
            OpusError.OPUS_BAD_ARG
        } else {
            OpusError.OPUS_OK
        }
    }

    private fun opus_custom_decoder_init(mode: CeltMode, channels: Int): Int {
        if (channels < 0 || channels > 2) {
            return OpusError.OPUS_BAD_ARG
        }

        if (this == null) {
            return OpusError.OPUS_ALLOC_FAIL
        }

        this.Reset()

        this.mode = mode
        this.overlap = mode.overlap
        this.channels = channels
        this.stream_channels = this.channels

        this.downsample = 1
        this.start = 0
        this.end = this.mode!!.effEBands
        this.signalling = 1

        this.loss_count = 0

        //this.decode_mem = new int[channels * (CeltConstants.DECODE_BUFFER_SIZE + mode.overlap));
        //this.lpc = new int[channels * CeltConstants.LPC_ORDER);
        //this.oldEBands = new int[2 * mode.nbEBands);
        //this.oldLogE = new int[2 * mode.nbEBands);
        //this.oldLogE2 = new int[2 * mode.nbEBands);
        //this.backgroundLogE = new int[2 * mode.nbEBands);
        this.ResetState()

        return OpusError.OPUS_OK
    }

    fun celt_decode_lost(N: Int, LM: Int) {
        var c: Int
        var i: Int
        val C = this.channels
        val out_syn = arrayOfNulls<IntArray>(2)
        val out_syn_ptrs = IntArray(2)
        val mode: CeltMode?
        val nbEBands: Int
        val overlap: Int
        val noise_based: Int
        val eBands: ShortArray?

        mode = this.mode
        nbEBands = mode!!.nbEBands
        overlap = mode!!.overlap
        eBands = mode!!.eBands

        c = 0
        do {
            out_syn[c] = this.decode_mem!![c]
            out_syn_ptrs[c] = CeltConstants.DECODE_BUFFER_SIZE - N
        } while (++c < C)

        noise_based = if (loss_count >= 5 || start != 0) 1 else 0
        if (noise_based != 0) {
            /* Noise-based PLC/CNG */
            val X: Array<IntArray>
            var seed: Int
            val end: Int
            val effEnd: Int
            val decay: Int
            end = this.end
            effEnd = Inlines.IMAX(start, Inlines.IMIN(end, mode!!.effEBands))

            X = Arrays.InitTwoDimensionalArrayInt(C, N)
            /**
             * < Interleaved normalised MDCTs
             */

            /* Energy decay */
            decay =
                    (if (loss_count == 0) (0.5 + 1.5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(1.5f, CeltConstants.DB_SHIFT)*/ else (0.5 + 0.5f * (1 shl CeltConstants.DB_SHIFT)).toShort()).toInt()/*Inlines.QCONST16(0.5f, CeltConstants.DB_SHIFT)*/
            c = 0
            do {
                i = start
                while (i < end) {
                    this.oldEBands!![c * nbEBands + i] = Inlines.MAX16(
                        backgroundLogE!![c * nbEBands + i],
                        this.oldEBands!![c * nbEBands + i] - decay
                    )
                    i++
                }
            } while (++c < C)
            seed = this.rng
            c = 0
            while (c < C) {
                i = start
                while (i < effEnd) {
                    var j: Int
                    val boffs: Int
                    val blen: Int
                    boffs = eBands!![i] shl LM
                    blen = eBands[i + 1] - eBands[i] shl LM
                    j = 0
                    while (j < blen) {
                        seed = Bands.celt_lcg_rand(seed)
                        X[c][boffs + j] = seed shr 20
                        j++
                    }

                    VQ.renormalise_vector(X[c], 0, blen, CeltConstants.Q15ONE)
                    i++
                }
                c++
            }
            this.rng = seed

            c = 0
            do {
                Arrays.MemMove(this.decode_mem!![c]!!, N, 0, CeltConstants.DECODE_BUFFER_SIZE - N + (overlap shr 1))
            } while (++c < C)

            CeltCommon.celt_synthesis(
                mode!!,
                X,
                out_syn,
                out_syn_ptrs,
				this.oldEBands!!,
                start,
                effEnd,
                C,
                C,
                0,
                LM,
                this.downsample,
                0
            )
        } else {
            /* Pitch-based PLC */
            val window: IntArray?
            var fade = CeltConstants.Q15ONE
            val pitch_index: Int
            val etmp: IntArray
            val exc: IntArray

            if (loss_count == 0) {
                pitch_index = CeltCommon.celt_plc_pitch_search(this.decode_mem!!, C)
                this.last_pitch_index = pitch_index
            } else {
                pitch_index = this.last_pitch_index
                fade = (0.5 + .8f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.8f, 15)*/
            }

            etmp = IntArray(overlap)
            exc = IntArray(CeltConstants.MAX_PERIOD)
            window = mode!!.window
            c = 0
            do {
                var decay: Int = 0
                var attenuation: Int
                var S1 = 0
                val buf: IntArray
                val extrapolation_offset: Int
                val extrapolation_len: Int
                val exc_length: Int
                var j: Int

                buf = this.decode_mem!![c]
                i = 0
                while (i < CeltConstants.MAX_PERIOD) {
                    exc[i] = Inlines.ROUND16(
                        buf[CeltConstants.DECODE_BUFFER_SIZE - CeltConstants.MAX_PERIOD + i],
                        CeltConstants.SIG_SHIFT
                    )
                    i++
                }

                if (loss_count == 0) {
                    val ac = IntArray(CeltConstants.LPC_ORDER + 1)
                    /* Compute LPC coefficients for the last MAX_PERIOD samples before
                       the first loss so we can work in the excitation-filter domain. */
                    Autocorrelation._celt_autocorr(
                        exc, ac, window!!, overlap,
                        CeltConstants.LPC_ORDER, CeltConstants.MAX_PERIOD
                    )
                    /* Add a noise floor of -40 dB. */
                    ac[0] += Inlines.SHR32(ac[0], 13)
                    /* Use lag windowing to stabilize the Levinson-Durbin recursion. */
                    i = 1
                    while (i <= CeltConstants.LPC_ORDER) {
                        /*ac[i] *= exp(-.5*(2*M_PI*.002*i)*(2*M_PI*.002*i));*/
                        ac[i] -= Inlines.MULT16_32_Q15(2 * i * i, ac[i])
                        i++
                    }
                    CeltLPC.celt_lpc(this.lpc!![c], ac, CeltConstants.LPC_ORDER)
                }
                /* We want the excitation for 2 pitch periods in order to look for a
                   decaying signal, but we can't get more than MAX_PERIOD. */
                exc_length = Inlines.IMIN(2 * pitch_index, CeltConstants.MAX_PERIOD)
                /* Initialize the LPC history with the samples just before the start
                   of the region for which we're computing the excitation. */
                run {
                    val lpc_mem = IntArray(CeltConstants.LPC_ORDER)
                    i = 0
                    while (i < CeltConstants.LPC_ORDER) {
                        lpc_mem[i] =
                                Inlines.ROUND16(
                                    buf[CeltConstants.DECODE_BUFFER_SIZE - exc_length - 1 - i],
                                    CeltConstants.SIG_SHIFT
                                )
                        i++
                    }

                    /* Compute the excitation for exc_length samples before the loss. */
                    Kernels.celt_fir(
                        exc, CeltConstants.MAX_PERIOD - exc_length, this.lpc!![c], 0,
                        exc, CeltConstants.MAX_PERIOD - exc_length, exc_length, CeltConstants.LPC_ORDER, lpc_mem
                    )
                }

                /* Check if the waveform is decaying, and if so how fast.
                   We do this to avoid adding energy when concealing in a segment
                   with decaying energy. */
                run {
                    var E1 = 1
                    var E2 = 1
                    val decay_length: Int
                    val shift = Inlines.IMAX(
                        0,
                        2 * Inlines.celt_zlog2(
                            Inlines.celt_maxabs16(
                                exc,
                                CeltConstants.MAX_PERIOD - exc_length,
                                exc_length
                            )
                        ) - 20
                    )
                    decay_length = exc_length shr 1
                    i = 0
                    while (i < decay_length) {
                        var e: Int
                        e = exc[CeltConstants.MAX_PERIOD - decay_length + i]
                        E1 += Inlines.SHR32(Inlines.MULT16_16(e, e), shift)
                        e = exc[CeltConstants.MAX_PERIOD - 2 * decay_length + i]
                        E2 += Inlines.SHR32(Inlines.MULT16_16(e, e), shift)
                        i++
                    }
                    E1 = Inlines.MIN32(E1, E2)
                    decay = Inlines.celt_sqrt(Inlines.frac_div32(Inlines.SHR32(E1, 1), E2))
                }

                /* Move the decoder memory one frame to the left to give us room to
                   add the data for the new frame. We ignore the overlap that extends
                   past the end of the buffer, because we aren't going to use it. */
                Arrays.MemMove(buf, N, 0, CeltConstants.DECODE_BUFFER_SIZE - N)

                /* Extrapolate from the end of the excitation with a period of
                   "pitch_index", scaling down each period by an additional factor of
                   "decay". */
                extrapolation_offset = CeltConstants.MAX_PERIOD - pitch_index
                /* We need to extrapolate enough samples to cover a complete MDCT
                   window (including overlap/2 samples on both sides). */
                extrapolation_len = N + overlap
                /* We also apply fading if this is not the first loss. */
                attenuation = Inlines.MULT16_16_Q15(fade, decay)
                j = 0
                i = j
                while (i < extrapolation_len) {
                    val tmp: Int
                    if (j >= pitch_index) {
                        j -= pitch_index
                        attenuation = Inlines.MULT16_16_Q15(attenuation, decay)
                    }
                    buf[CeltConstants.DECODE_BUFFER_SIZE - N + i] = Inlines.SHL32(
                        Inlines.MULT16_16_Q15(
                            attenuation,
                            exc[extrapolation_offset + j]
                        ), CeltConstants.SIG_SHIFT
                    )
                    /* Compute the energy of the previously decoded signal whose
                       excitation we're copying. */
                    tmp = Inlines.ROUND16(
                        buf[CeltConstants.DECODE_BUFFER_SIZE - CeltConstants.MAX_PERIOD - N + extrapolation_offset + j],
                        CeltConstants.SIG_SHIFT
                    )
                    S1 += Inlines.SHR32(Inlines.MULT16_16(tmp, tmp), 8)
                    i++
                    j++
                }

                run {
                    val lpc_mem = IntArray(CeltConstants.LPC_ORDER)
                    /* Copy the last decoded samples (prior to the overlap region) to
                       synthesis filter memory so we can have a continuous signal. */
                    i = 0
                    while (i < CeltConstants.LPC_ORDER) {
                        lpc_mem[i] = Inlines.ROUND16(
                            buf[CeltConstants.DECODE_BUFFER_SIZE - N - 1 - i],
                            CeltConstants.SIG_SHIFT
                        )
                        i++
                    }
                    /* Apply the synthesis filter to convert the excitation back into
                       the signal domain. */
                    CeltLPC.celt_iir(
                        buf, CeltConstants.DECODE_BUFFER_SIZE - N, this.lpc!![c],
                        buf, CeltConstants.DECODE_BUFFER_SIZE - N, extrapolation_len, CeltConstants.LPC_ORDER,
                        lpc_mem
                    )
                }

                /* Check if the synthesis energy is higher than expected, which can
                   happen with the signal changes during our window. If so,
                   attenuate. */
                run {
                    var S2 = 0
                    i = 0
                    while (i < extrapolation_len) {
                        val tmp =
                            Inlines.ROUND16(buf[CeltConstants.DECODE_BUFFER_SIZE - N + i], CeltConstants.SIG_SHIFT)
                        S2 += Inlines.SHR32(Inlines.MULT16_16(tmp, tmp), 8)
                        i++
                    }
                    /* This checks for an "explosion" in the synthesis. */
                    if (S1 <= Inlines.SHR32(S2, 2)) {
                        i = 0
                        while (i < extrapolation_len) {
                            buf[CeltConstants.DECODE_BUFFER_SIZE - N + i] = 0
                            i++
                        }
                    } else if (S1 < S2) {
                        val ratio = Inlines.celt_sqrt(Inlines.frac_div32(Inlines.SHR32(S1, 1) + 1, S2 + 1))
                        i = 0
                        while (i < overlap) {
                            val tmp_g =
                                CeltConstants.Q15ONE - Inlines.MULT16_16_Q15(window!![i], CeltConstants.Q15ONE - ratio)
                            buf[CeltConstants.DECODE_BUFFER_SIZE - N + i] =
                                    Inlines.MULT16_32_Q15(tmp_g, buf[CeltConstants.DECODE_BUFFER_SIZE - N + i])
                            i++
                        }
                        i = overlap
                        while (i < extrapolation_len) {
                            buf[CeltConstants.DECODE_BUFFER_SIZE - N + i] =
                                    Inlines.MULT16_32_Q15(ratio, buf[CeltConstants.DECODE_BUFFER_SIZE - N + i])
                            i++
                        }
                    }
                }

                /* Apply the pre-filter to the MDCT overlap for the next frame because
                   the post-filter will be re-applied in the decoder after the MDCT
                   overlap. */
                CeltCommon.comb_filter(
                    etmp, 0, buf, CeltConstants.DECODE_BUFFER_SIZE,
                    this.postfilter_period, this.postfilter_period, overlap,
                    -this.postfilter_gain, -this.postfilter_gain,
                    this.postfilter_tapset, this.postfilter_tapset, null, 0
                )

                /* Simulate TDAC on the concealed audio so that it blends with the
                   MDCT of the next frame. */
                i = 0
                while (i < overlap / 2) {
                    buf[CeltConstants.DECODE_BUFFER_SIZE + i] = Inlines.MULT16_32_Q15(
                        window!![i],
                        etmp[overlap - 1 - i]
                    ) + Inlines.MULT16_32_Q15(window!![overlap - i - 1], etmp[i])
                    i++
                }
            } while (++c < C)
        }

        this.loss_count = loss_count + 1
    }

    fun celt_decode_with_ec(
        data: ByteArray?, data_ptr: Int,
        len: Int, pcm: ShortArray?, pcm_ptr: Int, frame_size: Int, dec: EntropyCoder?, accum: Int
    ): Int {
        var frame_size = frame_size
        var dec = dec
        var c: Int
        var i: Int
        val N: Int
        var spread_decision: Int
        var bits: Int
        val X: Array<IntArray>
        val fine_quant: IntArray
        val pulses: IntArray
        val cap: IntArray
        val offsets: IntArray
        val fine_priority: IntArray
        val tf_res: IntArray
        val collapse_masks: ShortArray
        val out_syn = arrayOfNulls<IntArray>(2)
        val out_syn_ptrs = IntArray(2)
        val oldBandE: IntArray?
        val oldLogE: IntArray?
        val oldLogE2: IntArray?
        val backgroundLogE: IntArray?

        val shortBlocks: Int
        val isTransient: Int
        val intra_ener: Int
        val CC = this.channels
        var LM: Int = 0
        val M: Int
        val start: Int
        val end: Int
        var effEnd: Int
        val codedBands: Int
        val alloc_trim: Int
        var postfilter_pitch: Int
        var postfilter_gain: Int
        var intensity = 0
        var dual_stereo = 0
        var total_bits: Int
        val balance: Int
        var tell: Int
        var dynalloc_logp: Int
        var postfilter_tapset: Int
        val anti_collapse_rsv: Int
        var anti_collapse_on = 0
        val silence: Int
        val C = this.stream_channels
        val mode: CeltMode? // porting note: pointer
        val nbEBands: Int
        val overlap: Int
        val eBands: ShortArray?

        mode = this.mode
        nbEBands = mode!!.nbEBands
        overlap = mode!!.overlap
        eBands = mode!!.eBands
        start = this.start
        end = this.end
        frame_size *= this.downsample

        oldBandE = this.oldEBands
        oldLogE = this.oldLogE
        oldLogE2 = this.oldLogE2
        backgroundLogE = this.backgroundLogE

        run {
            LM = 0
            while (LM <= mode!!.maxLM) {
                if (mode!!.shortMdctSize shl LM == frame_size) {
                    break
                }
                LM++
            }
            if (LM > mode!!.maxLM) {
                return OpusError.OPUS_BAD_ARG
            }
        }
        M = 1 shl LM

        if (len < 0 || len > 1275 || pcm == null) {
            return OpusError.OPUS_BAD_ARG
        }

        N = M * mode!!.shortMdctSize
        c = 0
        do {
            out_syn[c] = this.decode_mem!![c]
            out_syn_ptrs[c] = CeltConstants.DECODE_BUFFER_SIZE - N
        } while (++c < CC)

        effEnd = end
        if (effEnd > mode!!.effEBands) {
            effEnd = mode!!.effEBands
        }

        if (data == null || len <= 1) {
            this.celt_decode_lost(N, LM)
            CeltCommon.deemphasis(
                out_syn,
                out_syn_ptrs,
                pcm,
                pcm_ptr,
                N,
                CC,
                this.downsample,
                mode!!.preemph,
                this.preemph_memD,
                accum
            )

            return frame_size / this.downsample
        }

        if (dec == null) {
            // If no entropy decoder was passed into this function, we need to create
            // a new one here for local use only. It only exists in this function scope.
            dec = EntropyCoder()
            dec!!.dec_init(data, data_ptr, len)
        }

        if (C == 1) {
            i = 0
            while (i < nbEBands) {
                oldBandE!![i] = Inlines.MAX16(oldBandE!![i], oldBandE[nbEBands + i])
                i++
            }
        }

        total_bits = len * 8
        tell = dec!!.tell()

        if (tell >= total_bits) {
            silence = 1
        } else if (tell == 1) {
            silence = dec!!.dec_bit_logp(15)
        } else {
            silence = 0
        }

        if (silence != 0) {
            /* Pretend we've read all the remaining bits */
            tell = len * 8
            dec!!.nbits_total += tell - dec!!.tell()
        }

        postfilter_gain = 0
        postfilter_pitch = 0
        postfilter_tapset = 0
        if (start == 0 && tell + 16 <= total_bits) {
            if (dec!!.dec_bit_logp(1) != 0) {
                val qg: Int
                val octave: Int
                octave = dec!!.dec_uint(6).toInt()
                postfilter_pitch = (16 shl octave) + dec!!.dec_bits(4 + octave) - 1
                qg = dec!!.dec_bits(3)
                if (dec!!.tell() + 2 <= total_bits) {
                    postfilter_tapset = dec!!.dec_icdf(CeltTables.tapset_icdf, 2)
                }
                postfilter_gain = (0.5 + .09375f * (1 shl 15)).toShort()/*Inlines.QCONST16(.09375f, 15)*/ * (qg + 1)
            }
            tell = dec!!.tell()
        }

        if (LM > 0 && tell + 3 <= total_bits) {
            isTransient = dec!!.dec_bit_logp(3)
            tell = dec!!.tell()
        } else {
            isTransient = 0
        }

        if (isTransient != 0) {
            shortBlocks = M
        } else {
            shortBlocks = 0
        }

        /* Decode the global flags (first symbols in the stream) */
        intra_ener = if (tell + 3 <= total_bits) dec!!.dec_bit_logp(3) else 0
        /* Get band energies */
        QuantizeBands.unquant_coarse_energy(
            mode, start, end, oldBandE!!,
            intra_ener, dec, C, LM
        )

        tf_res = IntArray(nbEBands)
        CeltCommon.tf_decode(start, end, isTransient, tf_res, LM, dec!!)

        tell = dec!!.tell()
        spread_decision = Spread.SPREAD_NORMAL
        if (tell + 4 <= total_bits) {
            spread_decision = dec!!.dec_icdf(CeltTables.spread_icdf, 5)
        }

        cap = IntArray(nbEBands)

        CeltCommon.init_caps(mode!!, cap, LM, C)

        offsets = IntArray(nbEBands)

        dynalloc_logp = 6
        total_bits = total_bits shl EntropyCoder.BITRES
        tell = dec!!.tell_frac()
        i = start
        while (i < end) {
            val width: Int
            val quanta: Int
            var dynalloc_loop_logp: Int
            var boost: Int
            width = C * (eBands!![i + 1] - eBands[i]) shl LM
            /* quanta is 6 bits, but no more than 1 bit/sample
               and no less than 1/8 bit/sample */
            quanta = Inlines.IMIN(width shl EntropyCoder.BITRES, Inlines.IMAX(6 shl EntropyCoder.BITRES, width))
            dynalloc_loop_logp = dynalloc_logp
            boost = 0
            while (tell + (dynalloc_loop_logp shl EntropyCoder.BITRES) < total_bits && boost < cap[i]) {
                val flag: Int
                flag = dec!!.dec_bit_logp(dynalloc_loop_logp.toLong())
                tell = dec!!.tell_frac()
                if (flag == 0) {
                    break
                }
                boost += quanta
                total_bits -= quanta
                dynalloc_loop_logp = 1
            }
            offsets[i] = boost
            /* Making dynalloc more likely */
            if (boost > 0) {
                dynalloc_logp = Inlines.IMAX(2, dynalloc_logp - 1)
            }
            i++
        }

        fine_quant = IntArray(nbEBands)
        alloc_trim = if (tell + (6 shl EntropyCoder.BITRES) <= total_bits)
            dec!!.dec_icdf(CeltTables.trim_icdf, 7)
        else
            5

        bits = (len * 8 shl EntropyCoder.BITRES) - dec!!.tell_frac() - 1
        anti_collapse_rsv =
                if (isTransient != 0 && LM >= 2 && bits >= LM + 2 shl EntropyCoder.BITRES) 1 shl EntropyCoder.BITRES else 0
        bits -= anti_collapse_rsv

        pulses = IntArray(nbEBands)
        fine_priority = IntArray(nbEBands)

        val boxed_intensity = BoxedValueInt(intensity)
        val boxed_dual_stereo = BoxedValueInt(dual_stereo)
        val boxed_balance = BoxedValueInt(0)
        codedBands = Rate.compute_allocation(
            mode!!, start, end, offsets, cap,
            alloc_trim, boxed_intensity, boxed_dual_stereo, bits, boxed_balance, pulses,
            fine_quant, fine_priority, C, LM, dec, 0, 0, 0
        )
        intensity = boxed_intensity.Val
        dual_stereo = boxed_dual_stereo.Val
        balance = boxed_balance.Val

        QuantizeBands.unquant_fine_energy(mode, start, end, oldBandE, fine_quant, dec, C)

        c = 0
        do {
            Arrays.MemMove(decode_mem!![c], N, 0, CeltConstants.DECODE_BUFFER_SIZE - N + overlap / 2)
        } while (++c < CC)

        /* Decode fixed codebook */
        collapse_masks = ShortArray(C * nbEBands)

        X = Arrays.InitTwoDimensionalArrayInt(C, N)
        /**
         * < Interleaved normalised MDCTs
         */

        val boxed_rng = BoxedValueInt(this.rng)
        Bands.quant_all_bands(
            0,
            mode!!,
            start,
            end,
            X[0],
            if (C == 2) X[1] else null,
            collapse_masks,
            null,
            pulses,
            shortBlocks,
            spread_decision,
            dual_stereo,
            intensity,
            tf_res,
            len * (8 shl EntropyCoder.BITRES) - anti_collapse_rsv,
            balance,
            dec,
            LM,
            codedBands,
            boxed_rng
        )
        this.rng = boxed_rng.Val

        if (anti_collapse_rsv > 0) {
            anti_collapse_on = dec!!.dec_bits(1)
        }

        QuantizeBands.unquant_energy_finalise(
            mode, start, end, oldBandE,
            fine_quant, fine_priority, len * 8 - dec!!.tell(), dec, C
        )

        if (anti_collapse_on != 0) {
            Bands.anti_collapse(
                mode, X, collapse_masks, LM, C, N,
                start, end, oldBandE, oldLogE!!, oldLogE2!!, pulses, this.rng
            )
        }

        if (silence != 0) {
            i = 0
            while (i < C * nbEBands) {
                oldBandE[i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                i++
            }
        }

        CeltCommon.celt_synthesis(
            mode!!, X, out_syn, out_syn_ptrs, oldBandE, start, effEnd,
            C, CC, isTransient, LM, this.downsample, silence
        )

        c = 0
        do {
            this.postfilter_period = Inlines.IMAX(this.postfilter_period, CeltConstants.COMBFILTER_MINPERIOD)
            this.postfilter_period_old = Inlines.IMAX(this.postfilter_period_old, CeltConstants.COMBFILTER_MINPERIOD)
            CeltCommon.comb_filter(
                out_syn!![c]!!,
                out_syn_ptrs[c],
                out_syn!![c]!!,
                out_syn_ptrs[c],
                this.postfilter_period_old,
                this.postfilter_period,
                mode!!.shortMdctSize,
                this.postfilter_gain_old,
                this.postfilter_gain,
                this.postfilter_tapset_old,
                this.postfilter_tapset,
                mode!!.window,
                overlap
            )
            if (LM != 0) {
                CeltCommon.comb_filter(
                    out_syn!![c]!!, out_syn_ptrs[c] + mode!!.shortMdctSize,
                    out_syn!![c]!!, out_syn_ptrs[c] + mode!!.shortMdctSize,
                    this.postfilter_period, postfilter_pitch, N - mode!!.shortMdctSize,
                    this.postfilter_gain, postfilter_gain, this.postfilter_tapset, postfilter_tapset,
                    mode!!.window, overlap
                )
            }

        } while (++c < CC)
        this.postfilter_period_old = this.postfilter_period
        this.postfilter_gain_old = this.postfilter_gain
        this.postfilter_tapset_old = this.postfilter_tapset
        this.postfilter_period = postfilter_pitch
        this.postfilter_gain = postfilter_gain
        this.postfilter_tapset = postfilter_tapset
        if (LM != 0) {
            this.postfilter_period_old = this.postfilter_period
            this.postfilter_gain_old = this.postfilter_gain
            this.postfilter_tapset_old = this.postfilter_tapset
        }

        if (C == 1) {
            arraycopy(oldBandE!!, 0, oldBandE, nbEBands, nbEBands)
        }

        /* In case start or end were to change */
        if (isTransient == 0) {
            val max_background_increase: Int
            arraycopy(oldLogE!!, 0, oldLogE2!!, 0, 2 * nbEBands)
            arraycopy(oldBandE!!, 0, oldLogE, 0, 2 * nbEBands)
            /* In normal circumstances, we only allow the noise floor to increase by
               up to 2.4 dB/second, but when we're in DTX, we allow up to 6 dB
               increase for each update.*/
            if (this.loss_count < 10) {
                max_background_increase = M *
                        (0.5 + 0.001f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(0.001f, CeltConstants.DB_SHIFT)*/
            } else {
                max_background_increase = (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                    .toInt()/*Inlines.QCONST16(1.0f, CeltConstants.DB_SHIFT)*/
            }
            i = 0
            while (i < 2 * nbEBands) {
                backgroundLogE!![i] = Inlines.MIN16(backgroundLogE!![i] + max_background_increase, oldBandE[i])
                i++
            }
        } else {
            i = 0
            while (i < 2 * nbEBands) {
                oldLogE!![i] = Inlines.MIN16(oldLogE!![i], oldBandE!![i])
                i++
            }
        }
        c = 0
        do {
            i = 0
            while (i < start) {
                oldBandE[c * nbEBands + i] = 0
                oldLogE2!![c * nbEBands + i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                oldLogE!![c * nbEBands + i] = oldLogE2[c * nbEBands + i]
                i++
            }
            i = end
            while (i < nbEBands) {
                oldBandE[c * nbEBands + i] = 0
                oldLogE2!![c * nbEBands + i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                oldLogE!![c * nbEBands + i] = oldLogE2[c * nbEBands + i]
                i++
            }
        } while (++c < 2)
        this.rng = dec!!.rng.toInt()

        CeltCommon.deemphasis(
            out_syn,
            out_syn_ptrs,
            pcm,
            pcm_ptr,
            N,
            CC,
            this.downsample,
            mode!!.preemph,
            this.preemph_memD,
            accum
        )
        this.loss_count = 0

        if (dec!!.tell() > 8 * len) {
            return OpusError.OPUS_INTERNAL_ERROR
        }
        if (dec!!._error != 0) {
            this.error = 1
        }
        return frame_size / this.downsample
    }

    fun SetStartBand(value: Int) {
        if (value < 0 || value >= this.mode!!.nbEBands) {
            throw IllegalArgumentException("Start band above max number of ebands (or negative)")
        }
        this.start = value
    }

    fun SetEndBand(value: Int) {
        if (value < 1 || value > this.mode!!.nbEBands) {
            throw IllegalArgumentException("End band above max number of ebands (or less than 1)")
        }
        this.end = value
    }

    fun SetChannels(value: Int) {
        if (value < 1 || value > 2) {
            throw IllegalArgumentException("Channel count must be 1 or 2")
        }
        this.stream_channels = value
    }

    fun GetAndClearError(): Int {
        val returnVal = this.error
        this.error = 0
        return returnVal
    }

    fun GetLookahead(): Int {
        return this.overlap / this.downsample
    }

    fun GetPitch(): Int {
        return this.postfilter_period
    }

    fun GetMode(): CeltMode? {
        return this.mode
    }

    fun SetSignalling(value: Int) {
        this.signalling = value
    }

    fun GetFinalRange(): Int {
        return this.rng
    }
}
