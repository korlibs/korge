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

internal class CeltEncoder {

    var mode: CeltMode? = null
    /**
     * < Mode used by the encoder. Without custom modes, this always refers to
     * the same predefined struct
     */
    var channels = 0
    var stream_channels = 0

    var force_intra = 0
    var clip = 0
    var disable_pf = 0
    var complexity = 0
    var upsample = 0
    var start = 0
    var end = 0

    var bitrate = 0
    var vbr = 0
    var signalling = 0

    /* If zero, VBR can do whatever it likes with the rate */
    var constrained_vbr = 0
    var loss_rate = 0
    var lsb_depth = 0
    var variable_duration = OpusFramesize.OPUS_FRAMESIZE_UNKNOWN
    var lfe = 0

    /* Everything beyond this point gets cleared on a reset */
    var rng = 0
    var spread_decision = 0
    var delayedIntra = 0
    var tonal_average = 0
    var lastCodedBands = 0
    var hf_average = 0
    var tapset_decision = 0

    var prefilter_period = 0
    var prefilter_gain = 0
    var prefilter_tapset = 0
    var consec_transient = 0
    var analysis = AnalysisInfo()

    val preemph_memE = IntArray(2)
    val preemph_memD = IntArray(2)

    /* VBR-related parameters */
    var vbr_reservoir = 0
    var vbr_drift = 0
    var vbr_offset = 0
    var vbr_count = 0
    var overlap_max = 0
    var stereo_saving = 0
    var intensity = 0
    var energy_mask: IntArray? = null
    var spec_avg = 0

    /// <summary>
    /// The original C++ defined in_mem as a single float[1] which was the "caboose"
    /// to the overall encoder struct, containing 5 separate variable-sized buffer
    /// spaces of heterogeneous datatypes. I have laid them out into separate variables here,
    /// but these were the original definitions:
    /// val32 in_mem[],        Size = channels*mode.overlap
    /// val32 prefilter_mem[], Size = channels*COMBFILTER_MAXPERIOD
    /// val16 oldBandE[],      Size = channels*mode.nbEBands
    /// val16 oldLogE[],       Size = channels*mode.nbEBands
    /// val16 oldLogE2[],      Size = channels*mode.nbEBands
    /// </summary>
    var in_mem: Array<IntArray>? = null
    var prefilter_mem: Array<IntArray>? = null
    var oldBandE: Array<IntArray>? = null
    var oldLogE: Array<IntArray>? = null
    var oldLogE2: Array<IntArray>? = null

    private fun Reset() {
        mode = null
        channels = 0
        stream_channels = 0
        force_intra = 0
        clip = 0
        disable_pf = 0
        complexity = 0
        upsample = 0
        start = 0
        end = 0
        bitrate = 0
        vbr = 0
        signalling = 0
        constrained_vbr = 0
        loss_rate = 0
        lsb_depth = 0
        variable_duration = OpusFramesize.OPUS_FRAMESIZE_UNKNOWN
        lfe = 0
        PartialReset()
    }

    private fun PartialReset() {
        rng = 0
        spread_decision = 0
        delayedIntra = 0
        tonal_average = 0
        lastCodedBands = 0
        hf_average = 0
        tapset_decision = 0
        prefilter_period = 0
        prefilter_gain = 0
        prefilter_tapset = 0
        consec_transient = 0
        analysis.Reset()
        preemph_memE[0] = 0
        preemph_memE[1] = 0
        preemph_memD[0] = 0
        preemph_memD[1] = 0
        vbr_reservoir = 0
        vbr_drift = 0
        vbr_offset = 0
        vbr_count = 0
        overlap_max = 0
        stereo_saving = 0
        intensity = 0
        energy_mask = null
        spec_avg = 0
        in_mem = null
        prefilter_mem = null
        oldBandE = null
        oldLogE = null
        oldLogE2 = null
    }

    fun ResetState() {
        var i: Int

        this.PartialReset()

        // We have to reconstitute the dynamic buffers here.
        this.in_mem = Arrays.InitTwoDimensionalArrayInt(this.channels, this.mode!!.overlap)
        this.prefilter_mem = Arrays.InitTwoDimensionalArrayInt(this.channels, CeltConstants.COMBFILTER_MAXPERIOD)
        this.oldBandE = Arrays.InitTwoDimensionalArrayInt(this.channels, this.mode!!.nbEBands)
        this.oldLogE = Arrays.InitTwoDimensionalArrayInt(this.channels, this.mode!!.nbEBands)
        this.oldLogE2 = Arrays.InitTwoDimensionalArrayInt(this.channels, this.mode!!.nbEBands)

        i = 0
        while (i < this.mode!!.nbEBands) {
            this.oldLogE2!![0][i] =
                    -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
            this.oldLogE!![0][i] = this.oldLogE2!![0][i]
            i++
        }
        if (this.channels == 2) {
            i = 0
            while (i < this.mode!!.nbEBands) {
                this.oldLogE2!![1][i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                this.oldLogE!![1][i] = this.oldLogE2!![1][i]
                i++
            }
        }
        this.vbr_offset = 0
        this.delayedIntra = 1
        this.spread_decision = Spread.SPREAD_NORMAL
        this.tonal_average = 256
        this.hf_average = 0
        this.tapset_decision = 0
    }

    fun opus_custom_encoder_init_arch(
        mode: CeltMode?,
        channels: Int
    ): Int {
        if (channels < 0 || channels > 2) {
            return OpusError.OPUS_BAD_ARG
        }

        if (this == null || mode == null) {
            return OpusError.OPUS_ALLOC_FAIL
        }

        this.Reset()

        this.mode = mode
        this.channels = channels
        this.stream_channels = this.channels

        this.upsample = 1
        this.start = 0
        this.end = this.mode!!.effEBands
        this.signalling = 1

        this.constrained_vbr = 1
        this.clip = 1

        this.bitrate = OpusConstants.OPUS_BITRATE_MAX
        this.vbr = 0
        this.force_intra = 0
        this.complexity = 5
        this.lsb_depth = 24

        //this.in_mem = new int[channels * mode.overlap);
        //this.prefilter_mem = new int[channels * CeltConstants.COMBFILTER_MAXPERIOD);
        //this.oldBandE = new int[channels * mode.nbEBands);
        //this.oldLogE = new int[channels * mode.nbEBands);
        //this.oldLogE2 = new int[channels * mode.nbEBands);
        this.ResetState()

        return OpusError.OPUS_OK
    }

    fun celt_encoder_init(sampling_rate: Int, channels: Int): Int {
        val ret: Int
        ret = this.opus_custom_encoder_init_arch(CeltMode.mode48000_960_120, channels)
        if (ret != OpusError.OPUS_OK) {
            return ret
        }
        this.upsample = CeltCommon.resampling_factor(sampling_rate)
        return OpusError.OPUS_OK
    }

    fun run_prefilter(
        input: Array<IntArray>,
        prefilter_mem: Array<IntArray>?,
        CC: Int,
        N: Int,
        prefilter_tapset: Int,
        pitch: BoxedValueInt,
        gain: BoxedValueInt,
        qgain: BoxedValueInt,
        enabled: Int,
        nbAvailableBytes: Int
    ): Int {
        var c: Int
        val pre = arrayOfNulls<IntArray>(CC)
        val mode: CeltMode? // [porting note] pointer
        val pitch_index = BoxedValueInt(0)
        var gain1: Int
        var pf_threshold: Int
        val pf_on: Int
        var qg: Int
        val overlap: Int

        mode = this.mode
        overlap = mode!!.overlap
        for (z in 0 until CC) {
            pre[z] = IntArray(N + CeltConstants.COMBFILTER_MAXPERIOD)
        }

        c = 0
        do {
            arraycopy(prefilter_mem!![c], 0, pre[c]!!, 0, CeltConstants.COMBFILTER_MAXPERIOD)
            arraycopy(input[c]!!, overlap, pre[c]!!, CeltConstants.COMBFILTER_MAXPERIOD, N)
        } while (++c < CC)

        if (enabled != 0) {
            val pitch_buf = IntArray(CeltConstants.COMBFILTER_MAXPERIOD + N shr 1)

            Pitch.pitch_downsample(pre as Array<IntArray>, pitch_buf, CeltConstants.COMBFILTER_MAXPERIOD + N, CC)
            /* Don't search for the fir last 1.5 octave of the range because
               there's too many false-positives due to short-term correlation */
            Pitch.pitch_search(
                pitch_buf, CeltConstants.COMBFILTER_MAXPERIOD shr 1, pitch_buf, N,
                CeltConstants.COMBFILTER_MAXPERIOD - 3 * CeltConstants.COMBFILTER_MINPERIOD, pitch_index
            )
            pitch_index.Val = CeltConstants.COMBFILTER_MAXPERIOD - pitch_index.Val
            gain1 = Pitch.remove_doubling(
                pitch_buf, CeltConstants.COMBFILTER_MAXPERIOD, CeltConstants.COMBFILTER_MINPERIOD,
                N, pitch_index, this.prefilter_period, this.prefilter_gain
            )
            if (pitch_index.Val > CeltConstants.COMBFILTER_MAXPERIOD - 2) {
                pitch_index.Val = CeltConstants.COMBFILTER_MAXPERIOD - 2
            }
            gain1 = Inlines.MULT16_16_Q15(
                (0.5 + .7f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.7f, 15)*/,
                gain1
            )
            /*printf("%d %d %f %f\n", pitch_change, pitch_index, gain1, st.analysis.tonality);*/
            if (this.loss_rate > 2) {
                gain1 = Inlines.HALF32(gain1)
            }
            if (this.loss_rate > 4) {
                gain1 = Inlines.HALF32(gain1)
            }
            if (this.loss_rate > 8) {
                gain1 = 0
            }
        } else {
            gain1 = 0
            pitch_index.Val = CeltConstants.COMBFILTER_MINPERIOD
        }

        /* Gain threshold for enabling the prefilter/postfilter */
        pf_threshold = (0.5 + .2f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.2f, 15)*/

        /* Adjusting the threshold based on rate and continuity */
        if (Inlines.abs(pitch_index.Val - this.prefilter_period) * 10 > pitch_index.Val) {
            pf_threshold += (0.5 + .2f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.2f, 15)*/
        }
        if (nbAvailableBytes < 25) {
            pf_threshold += (0.5 + .1f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.1f, 15)*/
        }
        if (nbAvailableBytes < 35) {
            pf_threshold += (0.5 + .1f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.1f, 15)*/
        }
        if (this.prefilter_gain > (0.5 + .4f * (1 shl 15)).toShort()/*Inlines.QCONST16(.4f, 15)*/) {
            pf_threshold -= (0.5 + .1f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.1f, 15)*/
        }
        if (this.prefilter_gain > (0.5 + .55f * (1 shl 15)).toShort()/*Inlines.QCONST16(.55f, 15)*/) {
            pf_threshold -= (0.5 + .1f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.1f, 15)*/
        }

        /* Hard threshold at 0.2 */
        pf_threshold =
                Inlines.MAX16(pf_threshold, (0.5 + .2f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.2f, 15)*/)

        if (gain1 < pf_threshold) {
            gain1 = 0
            pf_on = 0
            qg = 0
        } else {
            /*This block is not gated by a total bits check only because
              of the nbAvailableBytes check above.*/
            if (Inlines.ABS32(gain1 - this.prefilter_gain) < (0.5 + .1f * (1 shl 15)).toShort()/*Inlines.QCONST16(.1f, 15)*/) {
                gain1 = this.prefilter_gain
            }

            qg = (gain1 + 1536 shr 10) / 3 - 1
            qg = Inlines.IMAX(0, Inlines.IMIN(7, qg))
            gain1 = (0.5 + 0.09375f * (1 shl 15)).toShort()/*Inlines.QCONST16(0.09375f, 15)*/ * (qg + 1)
            pf_on = 1
        }
        /*printf("%d %f\n", pitch_index, gain1);*/

        c = 0
        do {
            val offset = mode!!.shortMdctSize - overlap
            this.prefilter_period = Inlines.IMAX(this.prefilter_period, CeltConstants.COMBFILTER_MINPERIOD)
            arraycopy(this.in_mem!![c], 0, input[c], 0, overlap)
            if (offset != 0) {
                CeltCommon.comb_filter(
                    input[c], overlap, pre!![c]!!, CeltConstants.COMBFILTER_MAXPERIOD,
                    this.prefilter_period, this.prefilter_period, offset, -this.prefilter_gain, -this.prefilter_gain,
                    this.prefilter_tapset, this.prefilter_tapset, null, 0
                ) // opt: lots of pointer allocations here
            }

            CeltCommon.comb_filter(
                input[c], overlap + offset, pre!![c]!!, CeltConstants.COMBFILTER_MAXPERIOD + offset,
                this.prefilter_period, pitch_index.Val, N - offset, -this.prefilter_gain, -gain1,
                this.prefilter_tapset, prefilter_tapset, mode!!.window, overlap
            )
            arraycopy(input[c], N, this.in_mem!![c], 0, overlap)

            if (N > CeltConstants.COMBFILTER_MAXPERIOD) {
                arraycopy(pre!![c]!!, N, prefilter_mem[c], 0, CeltConstants.COMBFILTER_MAXPERIOD)
            } else {
                Arrays.MemMove(prefilter_mem[c], N, 0, CeltConstants.COMBFILTER_MAXPERIOD - N)
                arraycopy(
                    pre!![c]!!,
                    CeltConstants.COMBFILTER_MAXPERIOD,
                    prefilter_mem[c],
                    CeltConstants.COMBFILTER_MAXPERIOD - N,
                    N
                )
            }
        } while (++c < CC)

        gain.Val = gain1
        pitch.Val = pitch_index.Val
        qgain.Val = qg
        return pf_on
    }

    fun celt_encode_with_ec(
		pcm: ShortArray?,
		pcm_ptr: Int,
		frame_size: Int,
		compressed: ByteArray?,
		compressed_ptr: Int,
		nbCompressedBytes: Int,
		enc: EntropyCoder?
    ): Int {
        var frame_size = frame_size
        var nbCompressedBytes = nbCompressedBytes
        var enc = enc
        var i: Int
        var c: Int
        val N: Int
        var bits: Int
        val input: Array<IntArray>
        val freq: Array<IntArray>
        val X: Array<IntArray>
        val bandE: Array<IntArray>
        val bandLogE: Array<IntArray>
        val bandLogE2: Array<IntArray>
        val fine_quant: IntArray
        val error: Array<IntArray>
        val pulses: IntArray
        val cap: IntArray
        val offsets: IntArray
        val fine_priority: IntArray
        val tf_res: IntArray
        val collapse_masks: ShortArray
        var shortBlocks = 0
        var isTransient = 0
        val CC = this.channels
        val C = this.stream_channels
        var LM: Int
        val M: Int
        val tf_select: Int
        val nbFilledBytes: Int
        var nbAvailableBytes: Int
        val start: Int
        val end: Int
        var effEnd: Int
        val codedBands: Int
        val tf_sum: Int
        var alloc_trim: Int
        var pitch_index = CeltConstants.COMBFILTER_MINPERIOD
        var gain1 = 0
        var dual_stereo = 0
        var effectiveBytes: Int
        var dynalloc_logp: Int
        val vbr_rate: Int
        var total_bits: Int
        var total_boost: Int
        val balance: Int
        var tell: Int
        var prefilter_tapset = 0
        var pf_on: Int = 0
        val anti_collapse_rsv: Int
        var anti_collapse_on = 0
        var silence = 0
        var tf_chan = 0
        var tf_estimate: Int
        var pitch_change = 0
        val tot_boost: Int
        var sample_max: Int
        val maxDepth: Int
        val mode: CeltMode?
        val nbEBands: Int
        val overlap: Int
        val eBands: ShortArray?
        val secondMdct: Int
        var signalBandwidth: Int
        var transient_got_disabled = 0
        var surround_masking = 0
        var temporal_vbr = 0
        var surround_trim = 0
        var equiv_rate = 510000
        val surround_dynalloc: IntArray

        mode = this.mode
        nbEBands = mode!!.nbEBands
        overlap = mode!!.overlap
        eBands = mode!!.eBands
        start = this.start
        end = this.end
        tf_estimate = 0
        if (nbCompressedBytes < 2 || pcm == null) {
            return OpusError.OPUS_BAD_ARG
        }

        frame_size *= this.upsample
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
        M = 1 shl LM
        N = M * mode!!.shortMdctSize

        if (enc == null) {
            tell = 1
            nbFilledBytes = 0
        } else {
            tell = enc!!.tell()
            nbFilledBytes = tell + 4 shr 3
        }

        Inlines.OpusAssert(this.signalling == 0)

        /* Can't produce more than 1275 output bytes */
        nbCompressedBytes = Inlines.IMIN(nbCompressedBytes, 1275)
        nbAvailableBytes = nbCompressedBytes - nbFilledBytes

        if (this.vbr != 0 && this.bitrate != OpusConstants.OPUS_BITRATE_MAX) {
            val den = mode!!.Fs shr EntropyCoder.BITRES
            vbr_rate = (this.bitrate * frame_size + (den shr 1)) / den
            effectiveBytes = vbr_rate shr 3 + EntropyCoder.BITRES
        } else {
            var tmp: Int
            vbr_rate = 0
            tmp = this.bitrate * frame_size
            if (tell > 1) {
                tmp += tell
            }
            if (this.bitrate != OpusConstants.OPUS_BITRATE_MAX) {
                nbCompressedBytes = Inlines.IMAX(
                    2, Inlines.IMIN(
                        nbCompressedBytes,
                        (tmp + 4 * mode!!.Fs) / (8 * mode!!.Fs) - if (this.signalling != 0) 1 else 0
                    )
                )
            }
            effectiveBytes = nbCompressedBytes
        }
        if (this.bitrate != OpusConstants.OPUS_BITRATE_MAX) {
            equiv_rate = this.bitrate - (40 * C + 20) * ((400 shr LM) - 50)
        }

        if (enc == null) {
            enc = EntropyCoder()
            enc!!.enc_init(compressed!!, compressed_ptr, nbCompressedBytes)
        }

        if (vbr_rate > 0) {
            /* Computes the max bit-rate allowed in VBR mode to avoid violating the
                target rate and buffering.
               We must do this up front so that bust-prevention logic triggers
                correctly if we don't have enough bits. */
            if (this.constrained_vbr != 0) {
                val vbr_bound: Int
                val max_allowed: Int
                /* We could use any multiple of vbr_rate as bound (depending on the
                    delay).
                   This is clamped to ensure we use at least two bytes if the encoder
                    was entirely empty, but to allow 0 in hybrid mode. */
                vbr_bound = vbr_rate
                max_allowed = Inlines.IMIN(
                    Inlines.IMAX(
                        if (tell == 1) 2 else 0,
                        vbr_rate + vbr_bound - this.vbr_reservoir shr EntropyCoder.BITRES + 3
                    ),
                    nbAvailableBytes
                )
                if (max_allowed < nbAvailableBytes) {
                    nbCompressedBytes = nbFilledBytes + max_allowed
                    nbAvailableBytes = max_allowed
                    enc!!.enc_shrink(nbCompressedBytes)
                }
            }
        }
        total_bits = nbCompressedBytes * 8

        effEnd = end
        if (effEnd > mode!!.effEBands) {
            effEnd = mode!!.effEBands
        }

        input = Arrays.InitTwoDimensionalArrayInt(CC, N + overlap)

        sample_max = Inlines.MAX32(
            this.overlap_max,
            Inlines.celt_maxabs32(pcm, pcm_ptr, C * (N - overlap) / this.upsample).toInt()
        )
        this.overlap_max =
                Inlines.celt_maxabs32(pcm, pcm_ptr + C * (N - overlap) / this.upsample, C * overlap / this.upsample)
                    .toInt()
        sample_max = Inlines.MAX32(sample_max, this.overlap_max)
        silence = if (sample_max == 0) 1 else 0
        if (tell == 1) {
            enc!!.enc_bit_logp(silence, 15)
        } else {
            silence = 0
        }
        if (silence != 0) {
            /*In VBR mode there is no need to send more than the minimum. */
            if (vbr_rate > 0) {
                nbCompressedBytes = Inlines.IMIN(nbCompressedBytes, nbFilledBytes + 2)
                effectiveBytes = nbCompressedBytes
                total_bits = nbCompressedBytes * 8
                nbAvailableBytes = 2
                enc!!.enc_shrink(nbCompressedBytes)
            }
            /* Pretend we've filled all the remaining bits with zeros
                  (that's what the initialiser did anyway) */
            tell = nbCompressedBytes * 8
            enc!!.nbits_total += tell - enc!!.tell()
        }
        c = 0
        val boxed_memE = BoxedValueInt(0)
        do {
            val need_clip = 0
            boxed_memE.Val = this.preemph_memE[c]
            CeltCommon.celt_preemphasis(
                pcm, pcm_ptr + c, input[c], overlap, N, CC, this.upsample,
                mode!!.preemph, boxed_memE, need_clip
            )
            this.preemph_memE[c] = boxed_memE.Val
        } while (++c < CC)

        /* Find pitch period and gain */
        run {
            val enabled: Int
            val qg: Int
            enabled =
                    if ((this.lfe != 0 && nbAvailableBytes > 3 || nbAvailableBytes > 12 * C) && start == 0 && silence == 0 && this.disable_pf == 0
                        && this.complexity >= 5 && !(this.consec_transient != 0 && LM != 3 && this.variable_duration == OpusFramesize.OPUS_FRAMESIZE_VARIABLE)
                    )
                        1
                    else
                        0

            prefilter_tapset = this.tapset_decision
            val boxed_pitch_index = BoxedValueInt(0)
            val boxed_gain1 = BoxedValueInt(0)
            val boxed_qg = BoxedValueInt(0)
            pf_on = this.run_prefilter(
                input,
                this.prefilter_mem,
                CC,
                N,
                prefilter_tapset,
                boxed_pitch_index,
                boxed_gain1,
                boxed_qg,
                enabled,
                nbAvailableBytes
            )
            pitch_index = boxed_pitch_index.Val
            gain1 = boxed_gain1.Val
            qg = boxed_qg.Val

            if ((gain1 > (0.5 + .4f * (1 shl 15)).toShort()/*Inlines.QCONST16(.4f, 15)*/ || this.prefilter_gain > (0.5 + .4f * (1 shl 15)).toShort()/*Inlines.QCONST16(.4f, 15)*/) && (this.analysis.valid == 0 || this.analysis.tonality > .3)
                && (pitch_index > 1.26 * this.prefilter_period || pitch_index < .79 * this.prefilter_period)
            ) {
                pitch_change = 1
            }
            if (pf_on == 0) {
                if (start == 0 && tell + 16 <= total_bits) {
                    enc!!.enc_bit_logp(0, 1)
                }
            } else {
                /*This block is not gated by a total bits check only because
                  of the nbAvailableBytes check above.*/
                val octave: Int
                enc!!.enc_bit_logp(1, 1)
                pitch_index += 1
                octave = Inlines.EC_ILOG(pitch_index.toLong()) - 5
                enc!!.enc_uint(octave.toLong(), 6)
                enc!!.enc_bits((pitch_index - (16 shl octave)).toLong(), 4 + octave)
                pitch_index -= 1
                enc!!.enc_bits(qg.toLong(), 3)
                enc!!.enc_icdf(prefilter_tapset, CeltTables.tapset_icdf, 2)
            }
        }

        isTransient = 0
        shortBlocks = 0
        if (this.complexity >= 1 && this.lfe == 0) {
            val boxed_tf_estimate = BoxedValueInt(0)
            val boxed_tf_chan = BoxedValueInt(0)
            isTransient = CeltCommon.transient_analysis(
                input, N + overlap, CC,
                boxed_tf_estimate, boxed_tf_chan
            )
            tf_estimate = boxed_tf_estimate.Val
            tf_chan = boxed_tf_chan.Val
        }

        if (LM > 0 && enc!!.tell() + 3 <= total_bits) {
            if (isTransient != 0) {
                shortBlocks = M
            }
        } else {
            isTransient = 0
            transient_got_disabled = 1
        }

        freq = Arrays.InitTwoDimensionalArrayInt(CC, N)
        /**
         * < Interleaved signal MDCTs
         */
        bandE = Arrays.InitTwoDimensionalArrayInt(CC, nbEBands)
        bandLogE = Arrays.InitTwoDimensionalArrayInt(CC, nbEBands)

        secondMdct = if (shortBlocks != 0 && this.complexity >= 8) 1 else 0
        bandLogE2 = Arrays.InitTwoDimensionalArrayInt(CC, nbEBands)
        //Arrays.MemSet(bandLogE2, 0, C * nbEBands); // not explicitly needed
        if (secondMdct != 0) {
            CeltCommon.compute_mdcts(mode!!, 0, input, freq, C, CC, LM, this.upsample)
            Bands.compute_band_energies(mode!!, freq, bandE, effEnd, C, LM)
            QuantizeBands.amp2Log2(mode, effEnd, end, bandE, bandLogE2, C)
            i = 0
            while (i < nbEBands) {
                bandLogE2[0][i] += Inlines.HALF16(Inlines.SHL16(LM, CeltConstants.DB_SHIFT))
                i++
            }
            if (C == 2) {
                i = 0
                while (i < nbEBands) {
                    bandLogE2[1][i] += Inlines.HALF16(Inlines.SHL16(LM, CeltConstants.DB_SHIFT))
                    i++
                }
            }
        }

        CeltCommon.compute_mdcts(mode!!, shortBlocks, input, freq, C, CC, LM, this.upsample)
        if (CC == 2 && C == 1) {
            tf_chan = 0
        }
        Bands.compute_band_energies(mode!!, freq, bandE, effEnd, C, LM)

        if (this.lfe != 0) {
            i = 2
            while (i < end) {
                bandE[0][i] = Inlines.IMIN(
                    bandE[0][i],
                    Inlines.MULT16_32_Q15(
                        (0.5 + 1e-4f * (1 shl 15)).toShort()/*Inlines.QCONST16(1e-4f, 15)*/,
                        bandE[0][0]
                    )
                )
                bandE[0][i] = Inlines.MAX32(bandE[0][i], CeltConstants.EPSILON)
                i++
            }
        }

        QuantizeBands.amp2Log2(mode, effEnd, end, bandE, bandLogE, C)

        surround_dynalloc = IntArray(C * nbEBands)
        //Arrays.MemSet(surround_dynalloc, 0, end); // not strictly needed
        /* This computes how much masking takes place between surround channels */
        if (start == 0 && this.energy_mask != null && this.lfe == 0) {
            val mask_end: Int
            var midband: Int
            var count_dynalloc: Int
            var mask_avg = 0
            var diff = 0
            var count = 0
            mask_end = Inlines.IMAX(2, this.lastCodedBands)
            c = 0
            while (c < C) {
                i = 0
                while (i < mask_end) {
                    var mask: Int
                    mask = Inlines.MAX16(
                        Inlines.MIN16(
                            this.energy_mask!![nbEBands * c + i],
                            (0.5 + .25f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(.25f, CeltConstants.DB_SHIFT)*/
                        ),
                        -(0.5 + 2.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(2.0f, CeltConstants.DB_SHIFT)*/
                    )
                    if (mask > 0) {
                        mask = Inlines.HALF16(mask)
                    }
                    mask_avg += Inlines.MULT16_16(mask, eBands!![i + 1] - eBands[i])
                    count += eBands!![i + 1] - eBands[i]
                    diff += Inlines.MULT16_16(mask, 1 + 2 * i - mask_end)
                    i++
                }
                c++
            }
            Inlines.OpusAssert(count > 0)
            mask_avg = Inlines.DIV32_16(mask_avg, count)
            mask_avg += (0.5 + .2f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                .toInt()/*Inlines.QCONST16(.2f, CeltConstants.DB_SHIFT)*/
            diff = diff * 6 / (C * (mask_end - 1) * (mask_end + 1) * mask_end)
            /* Again, being conservative */
            diff = Inlines.HALF32(diff)
            diff = Inlines.MAX32(
                Inlines.MIN32(
                    diff,
                    (0.5 + .031f * (1 shl CeltConstants.DB_SHIFT)).toInt()/*Inlines.QCONST32(.031f, CeltConstants.DB_SHIFT)*/
                ),
                0 - (0.5 + .031f * (1 shl CeltConstants.DB_SHIFT)).toInt()/*Inlines.QCONST32(.031f, CeltConstants.DB_SHIFT)*/
            )
            /* Find the band that's in the middle of the coded spectrum */
            midband = 0
            while (eBands!![midband + 1] < eBands[mask_end] / 2) {
                midband++
            }
            count_dynalloc = 0
            i = 0
            while (i < mask_end) {
                val lin: Int
                var unmask: Int
                lin = mask_avg + diff * (i - midband)
                if (C == 2) {
                    unmask = Inlines.MAX16(this.energy_mask!![i], this.energy_mask!![nbEBands + i])
                } else {
                    unmask = this.energy_mask!![i]
                }
                unmask = Inlines.MIN16(
                    unmask,
                    (0.5 + .0f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(.0f, CeltConstants.DB_SHIFT)*/
                )
                unmask -= lin
                if (unmask > (0.5 + .25f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.25f, CeltConstants.DB_SHIFT)*/) {
                    surround_dynalloc[i] = unmask -
                            (0.5 + .25f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.25f, CeltConstants.DB_SHIFT)*/
                    count_dynalloc++
                }
                i++
            }
            if (count_dynalloc >= 3) {
                /* If we need dynalloc in many bands, it's probably because our
                   initial masking rate was too low. */
                mask_avg += (0.5 + .25f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                    .toInt()/*Inlines.QCONST16(.25f, CeltConstants.DB_SHIFT)*/
                if (mask_avg > 0) {
                    /* Something went really wrong in the original calculations,
                       disabling masking. */
                    mask_avg = 0
                    diff = 0
                    Arrays.MemSet(surround_dynalloc, 0, mask_end)
                } else {
                    i = 0
                    while (i < mask_end) {
                        surround_dynalloc[i] = Inlines.MAX16(
                            0,
                            surround_dynalloc[i] - (0.5 + .25f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(.25f, CeltConstants.DB_SHIFT)*/
                        )
                        i++
                    }
                }
            }
            mask_avg += (0.5 + .2f * (1 shl CeltConstants.DB_SHIFT)).toShort()
                .toInt()/*Inlines.QCONST16(.2f, CeltConstants.DB_SHIFT)*/
            /* Convert to 1/64th units used for the trim */
            surround_trim = 64 * diff
            /*printf("%d %d ", mask_avg, surround_trim);*/
            surround_masking = mask_avg
        }
        /* Temporal VBR (but not for LFE) */
        if (this.lfe == 0) {
            var follow =
                -(0.5 + 10.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(10.0f, CeltConstants.DB_SHIFT)*/
            var frame_avg = 0
            val offset = if (shortBlocks != 0) Inlines.HALF16(Inlines.SHL16(LM, CeltConstants.DB_SHIFT)) else 0
            i = start
            while (i < end) {
                follow = Inlines.MAX16(
                    follow - (0.5 + 1.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(1.0f, CeltConstants.DB_SHIFT)*/,
                    bandLogE[0][i] - offset
                )
                if (C == 2) {
                    follow = Inlines.MAX16(follow, bandLogE[1][i] - offset)
                }
                frame_avg += follow
                i++
            }
            frame_avg /= end - start
            temporal_vbr = Inlines.SUB16(frame_avg, this.spec_avg)
            temporal_vbr = Inlines.MIN16(
                (0.5 + 3.0f * (1 shl CeltConstants.DB_SHIFT)).toShort().toInt()/*Inlines.QCONST16(3.0f, CeltConstants.DB_SHIFT)*/,
                Inlines.MAX16(
                    -(0.5 + 1.5f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(1.5f, CeltConstants.DB_SHIFT)*/,
                    temporal_vbr
                )
            )
            this.spec_avg += Inlines.MULT16_16_Q15(
                (0.5 + .02f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.02f, 15)*/,
                temporal_vbr
            ).toShort().toInt()
        }
        /*for (i=0;i<21;i++)
           printf("%f ", bandLogE[i]);
        printf("\n");*/

        if (secondMdct == 0) {
            arraycopy(bandLogE[0], 0, bandLogE2[0], 0, nbEBands)
            if (C == 2) {
                arraycopy(bandLogE[1], 0, bandLogE2[1], 0, nbEBands)
            }
        }

        /* Last chance to catch any transient we might have missed in the
           time-domain analysis */
        if (LM > 0 && enc!!.tell() + 3 <= total_bits && isTransient == 0 && this.complexity >= 5 && this.lfe == 0) {
            if (CeltCommon.patch_transient_decision(bandLogE, this.oldBandE!!, nbEBands, start, end, C) != 0) {
                isTransient = 1
                shortBlocks = M
                CeltCommon.compute_mdcts(mode!!, shortBlocks, input, freq, C, CC, LM, this.upsample)
                Bands.compute_band_energies(mode!!, freq, bandE, effEnd, C, LM)
                QuantizeBands.amp2Log2(mode, effEnd, end, bandE, bandLogE, C)
                /* Compensate for the scaling of short vs long mdcts */
                i = 0
                while (i < nbEBands) {
                    bandLogE2[0][i] += Inlines.HALF16(Inlines.SHL16(LM, CeltConstants.DB_SHIFT))
                    i++
                }
                if (C == 2) {
                    i = 0
                    while (i < nbEBands) {
                        bandLogE2[1][i] += Inlines.HALF16(Inlines.SHL16(LM, CeltConstants.DB_SHIFT))
                        i++
                    }
                }
                tf_estimate = (0.5 + .2f * (1 shl 14)).toShort().toInt()/*Inlines.QCONST16(.2f, 14)*/
            }
        }

        if (LM > 0 && enc!!.tell() + 3 <= total_bits) {
            enc!!.enc_bit_logp(isTransient, 3)
        }

        X = Arrays.InitTwoDimensionalArrayInt(C, N)
        /**
         * < Interleaved normalised MDCTs
         */

        /* Band normalisation */
        Bands.normalise_bands(mode!!, freq, X, bandE, effEnd, C, M)

        tf_res = IntArray(nbEBands)
        /* Disable variable tf resolution for hybrid and at very low bitrate */
        if (effectiveBytes >= 15 * C && start == 0 && this.complexity >= 2 && this.lfe == 0) {
            var lambda: Int
            if (effectiveBytes < 40) {
                lambda = 12
            } else if (effectiveBytes < 60) {
                lambda = 6
            } else if (effectiveBytes < 100) {
                lambda = 4
            } else {
                lambda = 3
            }
            lambda *= 2
            val boxed_tf_sum = BoxedValueInt(0)
            tf_select = CeltCommon.tf_analysis(
                mode!!,
                effEnd,
                isTransient,
                tf_res,
                lambda,
                X,
                N,
                LM,
                boxed_tf_sum,
                tf_estimate,
                tf_chan
            )
            tf_sum = boxed_tf_sum.Val

            i = effEnd
            while (i < end) {
                tf_res[i] = tf_res[effEnd - 1]
                i++
            }
        } else {
            tf_sum = 0
            i = 0
            while (i < end) {
                tf_res[i] = isTransient
                i++
            }
            tf_select = 0
        }

        error = Arrays.InitTwoDimensionalArrayInt(C, nbEBands)
        val boxed_delayedintra = BoxedValueInt(this.delayedIntra)
        QuantizeBands.quant_coarse_energy(
            mode!!, start, end, effEnd, bandLogE,
			this.oldBandE!!, total_bits, error, enc!!,
            C, LM, nbAvailableBytes, this.force_intra,
            boxed_delayedintra, if (this.complexity >= 4) 1 else 0, this.loss_rate, this.lfe
        )
        this.delayedIntra = boxed_delayedintra.Val

        CeltCommon.tf_encode(start, end, isTransient, tf_res, LM, tf_select, enc!!)

        if (enc!!.tell() + 4 <= total_bits) {
            if (this.lfe != 0) {
                this.tapset_decision = 0
                this.spread_decision = Spread.SPREAD_NORMAL
            } else if (shortBlocks != 0 || this.complexity < 3 || nbAvailableBytes < 10 * C || start != 0) {
                if (this.complexity == 0) {
                    this.spread_decision = Spread.SPREAD_NONE
                } else {
                    this.spread_decision = Spread.SPREAD_NORMAL
                }
            } else {
                val boxed_tonal_average = BoxedValueInt(this.tonal_average)
                val boxed_tapset_decision = BoxedValueInt(this.tapset_decision)
                val boxed_hf_average = BoxedValueInt(this.hf_average)
                this.spread_decision = Bands.spreading_decision(
                    mode!!, X,
                    boxed_tonal_average, this.spread_decision, boxed_hf_average,
                    boxed_tapset_decision, if (pf_on != 0 && shortBlocks == 0) 1 else 0, effEnd, C, M
                )
                this.tonal_average = boxed_tonal_average.Val
                this.tapset_decision = boxed_tapset_decision.Val
                this.hf_average = boxed_hf_average.Val

                /*printf("%d %d\n", st.tapset_decision, st.spread_decision);*/
                /*printf("%f %d %f %d\n\n", st.analysis.tonality, st.spread_decision, st.analysis.tonality_slope, st.tapset_decision);*/
            }
            enc!!.enc_icdf(this.spread_decision, CeltTables.spread_icdf, 5)
        }

        offsets = IntArray(nbEBands)

        val boxed_tot_boost = BoxedValueInt(0)
        maxDepth = CeltCommon.dynalloc_analysis(
            bandLogE, bandLogE2, nbEBands, start, end, C, offsets,
            this.lsb_depth, mode!!.logN!!, isTransient, this.vbr, this.constrained_vbr,
			eBands!!, LM, effectiveBytes, boxed_tot_boost, this.lfe, surround_dynalloc
        )
        tot_boost = boxed_tot_boost.Val

        /* For LFE, everything interesting is in the first band */
        if (this.lfe != 0) {
            offsets[0] = Inlines.IMIN(8, effectiveBytes / 3)
        }
        cap = IntArray(nbEBands)
        CeltCommon.init_caps(mode!!, cap, LM, C)

        dynalloc_logp = 6
        total_bits = total_bits shl EntropyCoder.BITRES
        total_boost = 0
        tell = enc!!.tell_frac().toInt()
        i = start
        while (i < end) {
            val width: Int
            val quanta: Int
            var dynalloc_loop_logp: Int
            var boost: Int
            var j: Int
            width = C * (eBands!![i + 1] - eBands[i]) shl LM
            /* quanta is 6 bits, but no more than 1 bit/sample
               and no less than 1/8 bit/sample */
            quanta = Inlines.IMIN(width shl EntropyCoder.BITRES, Inlines.IMAX(6 shl EntropyCoder.BITRES, width))
            dynalloc_loop_logp = dynalloc_logp
            boost = 0
            j = 0
            while (tell + (dynalloc_loop_logp shl EntropyCoder.BITRES) < total_bits - total_boost && boost < cap[i]) {
                val flag: Int
                flag = if (j < offsets[i]) 1 else 0
                enc!!.enc_bit_logp(flag, dynalloc_loop_logp)
                tell = enc!!.tell_frac().toInt()
                if (flag == 0) {
                    break
                }
                boost += quanta
                total_boost += quanta
                dynalloc_loop_logp = 1
                j++
            }
            /* Making dynalloc more likely */
            if (j != 0) {
                dynalloc_logp = Inlines.IMAX(2, dynalloc_logp - 1)
            }
            offsets[i] = boost
            i++
        }

        if (C == 2) {
            /* Always use MS for 2.5 ms frames until we can do a better analysis */
            if (LM != 0) {
                dual_stereo = CeltCommon.stereo_analysis(mode, X, LM)
            }

            this.intensity = Bands.hysteresis_decision(
                equiv_rate / 1000,
                CeltTables.intensity_thresholds, CeltTables.intensity_histeresis, 21, this.intensity
            )
            this.intensity = Inlines.IMIN(end, Inlines.IMAX(start, this.intensity))
        }

        alloc_trim = 5
        if (tell + (6 shl EntropyCoder.BITRES) <= total_bits - total_boost) {
            if (this.lfe != 0) {
                alloc_trim = 5
            } else {
                val boxed_stereo_saving = BoxedValueInt(this.stereo_saving)
                alloc_trim = CeltCommon.alloc_trim_analysis(
                    mode, X, bandLogE,
                    end, LM, C, this.analysis, boxed_stereo_saving, tf_estimate,
                    this.intensity, surround_trim
                )
                this.stereo_saving = boxed_stereo_saving.Val
            }
            enc!!.enc_icdf(alloc_trim, CeltTables.trim_icdf, 7)
            tell = enc!!.tell_frac().toInt()
        }

        /* Variable bitrate */
        if (vbr_rate > 0) {
            val alpha: Int
            var delta: Int
            /* The target rate in 8th bits per frame */
            var target: Int
            var base_target: Int
            val min_allowed: Int
            val lm_diff = mode!!.maxLM - LM

            /* Don't attempt to use more than 510 kb/s, even for frames smaller than 20 ms.
               The CELT allocator will just not be able to use more than that anyway. */
            nbCompressedBytes = Inlines.IMIN(nbCompressedBytes, 1275 shr 3 - LM)
            base_target = vbr_rate - (40 * C + 20 shl EntropyCoder.BITRES)

            if (this.constrained_vbr != 0) {
                base_target += this.vbr_offset shr lm_diff
            }

            target = CeltCommon.compute_vbr(
                mode!!, this.analysis, base_target, LM, equiv_rate,
                this.lastCodedBands, C, this.intensity, this.constrained_vbr,
                this.stereo_saving, tot_boost, tf_estimate, pitch_change, maxDepth,
                this.variable_duration, this.lfe, if (this.energy_mask != null) 1 else 0, surround_masking,
                temporal_vbr
            )

            /* The current offset is removed from the target and the space used
               so far is added*/
            target = target + tell
            /* In VBR mode the frame size must not be reduced so much that it would
                result in the encoder running out of bits.
               The margin of 2 bytes ensures that none of the bust-prevention logic
                in the decoder will have triggered so far. */
            min_allowed = (tell + total_boost + (1 shl EntropyCoder.BITRES + 3) - 1 shr EntropyCoder.BITRES + 3) + 2 -
                    nbFilledBytes

            nbAvailableBytes = target + (1 shl EntropyCoder.BITRES + 2) shr EntropyCoder.BITRES + 3
            nbAvailableBytes = Inlines.IMAX(min_allowed, nbAvailableBytes)
            nbAvailableBytes = Inlines.IMIN(nbCompressedBytes, nbAvailableBytes + nbFilledBytes) - nbFilledBytes

            /* By how much did we "miss" the target on that frame */
            delta = target - vbr_rate

            target = nbAvailableBytes shl EntropyCoder.BITRES + 3

            /*If the frame is silent we don't adjust our drift, otherwise
              the encoder will shoot to very high rates after hitting a
              span of silence, but we do allow the EntropyCoder.BITRES to refill.
              This means that we'll undershoot our target in CVBR/VBR modes
              on files with lots of silence. */
            if (silence != 0) {
                nbAvailableBytes = 2
                target = 2 * 8 shl EntropyCoder.BITRES
                delta = 0
            }

            if (this.vbr_count < 970) {
                this.vbr_count++
                alpha = Inlines.celt_rcp(Inlines.SHL32(this.vbr_count + 20, 16))
            } else {
                alpha = (0.5 + .001f * (1 shl 15)).toShort().toInt()/*Inlines.QCONST16(.001f, 15)*/
            }
            /* How many bits have we used in excess of what we're allowed */
            if (this.constrained_vbr != 0) {
                this.vbr_reservoir += target - vbr_rate
            }
            /*printf ("%d\n", st.vbr_reservoir);*/

            /* Compute the offset we need to apply in order to reach the target */
            if (this.constrained_vbr != 0) {
                this.vbr_drift += Inlines.MULT16_32_Q15(
                    alpha,
                    delta * (1 shl lm_diff) - this.vbr_offset - this.vbr_drift
                ).toInt()
                this.vbr_offset = -this.vbr_drift
            }
            /*printf ("%d\n", st.vbr_drift);*/

            if (this.constrained_vbr != 0 && this.vbr_reservoir < 0) {
                /* We're under the min value -- increase rate */
                val adjust = -this.vbr_reservoir / (8 shl EntropyCoder.BITRES)
                /* Unless we're just coding silence */
                nbAvailableBytes += if (silence != 0) 0 else adjust
                this.vbr_reservoir = 0
                /*printf ("+%d\n", adjust);*/
            }
            nbCompressedBytes = Inlines.IMIN(nbCompressedBytes, nbAvailableBytes + nbFilledBytes)
            /*printf("%d\n", nbCompressedBytes*50*8);*/
            /* This moves the raw bits to take into account the new compressed size */
            enc!!.enc_shrink(nbCompressedBytes)
        }

        /* Bit allocation */
        fine_quant = IntArray(nbEBands)
        pulses = IntArray(nbEBands)
        fine_priority = IntArray(nbEBands)

        /* bits =    packet size                                     - where we are                        - safety*/
        bits = (nbCompressedBytes * 8 shl EntropyCoder.BITRES) - enc!!.tell_frac().toInt() - 1
        anti_collapse_rsv =
                if (isTransient != 0 && LM >= 2 && bits >= LM + 2 shl EntropyCoder.BITRES) 1 shl EntropyCoder.BITRES else 0
        bits -= anti_collapse_rsv
        signalBandwidth = end - 1

        if (this.analysis.enabled && this.analysis.valid != 0) {
            val min_bandwidth: Int
            if (equiv_rate < 32000 * C) {
                min_bandwidth = 13
            } else if (equiv_rate < 48000 * C) {
                min_bandwidth = 16
            } else if (equiv_rate < 60000 * C) {
                min_bandwidth = 18
            } else if (equiv_rate < 80000 * C) {
                min_bandwidth = 19
            } else {
                min_bandwidth = 20
            }
            signalBandwidth = Inlines.IMAX(this.analysis.bandwidth, min_bandwidth)
        }

        if (this.lfe != 0) {
            signalBandwidth = 1
        }

        val boxed_intensity = BoxedValueInt(this.intensity)
        val boxed_balance = BoxedValueInt(0)
        val boxed_dual_stereo = BoxedValueInt(dual_stereo)
        codedBands = Rate.compute_allocation(
            mode!!, start, end, offsets, cap,
            alloc_trim, boxed_intensity, boxed_dual_stereo, bits, boxed_balance, pulses,
            fine_quant, fine_priority, C, LM, enc, 1, this.lastCodedBands, signalBandwidth
        )
        this.intensity = boxed_intensity.Val
        balance = boxed_balance.Val
        dual_stereo = boxed_dual_stereo.Val

        if (this.lastCodedBands != 0) {
            this.lastCodedBands =
                    Inlines.IMIN(this.lastCodedBands + 1, Inlines.IMAX(this.lastCodedBands - 1, codedBands))
        } else {
            this.lastCodedBands = codedBands
        }

        QuantizeBands.quant_fine_energy(mode, start, end, this.oldBandE!!, error, fine_quant, enc, C)

        /* Residual quantisation */
        collapse_masks = ShortArray(C * nbEBands)
        val boxed_rng = BoxedValueInt(this.rng)
        Bands.quant_all_bands(
            1, mode!!, start, end, X[0], if (C == 2) X[1] else null, collapse_masks,
            bandE, pulses, shortBlocks, this.spread_decision,
            dual_stereo, this.intensity, tf_res, nbCompressedBytes * (8 shl EntropyCoder.BITRES) - anti_collapse_rsv,
            balance, enc, LM, codedBands, boxed_rng
        )
        this.rng = boxed_rng.Val

        if (anti_collapse_rsv > 0) {
            anti_collapse_on = if (this.consec_transient < 2) 1 else 0
            enc!!.enc_bits(anti_collapse_on.toLong(), 1)
        }

        QuantizeBands.quant_energy_finalise(
            mode,
            start,
            end,
			this.oldBandE!!,
            error,
            fine_quant,
            fine_priority,
            nbCompressedBytes * 8 - enc!!.tell().toInt(),
            enc,
            C
        )

        if (silence != 0) {
            i = 0
            while (i < nbEBands) {
                this.oldBandE!![0][i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                i++
            }
            if (C == 2) {
                i = 0
                while (i < nbEBands) {
                    this.oldBandE!![1][i] =
                            -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                    i++
                }
            }
        }

        this.prefilter_period = pitch_index
        this.prefilter_gain = gain1
        this.prefilter_tapset = prefilter_tapset

        if (CC == 2 && C == 1) {
            arraycopy(oldBandE!![0], 0, oldBandE!![1], 0, nbEBands)
        }

        if (isTransient == 0) {
            arraycopy(oldLogE!![0], 0, oldLogE2!![0], 0, nbEBands)
            arraycopy(oldBandE!![0], 0, oldLogE!![0], 0, nbEBands)
            if (CC == 2) {
                arraycopy(oldLogE!![1], 0, oldLogE2!![1], 0, nbEBands)
                arraycopy(oldBandE!![1], 0, oldLogE!![1], 0, nbEBands)
            }
        } else {
            i = 0
            while (i < nbEBands) {
                oldLogE!![0][i] = Inlines.MIN16(oldLogE!![0][i], oldBandE!![0][i])
                i++
            }
            if (CC == 2) {
                i = 0
                while (i < nbEBands) {
                    oldLogE!![1][i] = Inlines.MIN16(oldLogE!![1][i], oldBandE!![1][i])
                    i++
                }
            }
        }

        /* In case start or end were to change */
        c = 0
        do {
            i = 0
            while (i < start) {
                oldBandE!![c][i] = 0
                oldLogE2!![c][i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                oldLogE!![c][i] = oldLogE2!![c][i]
                i++
            }
            i = end
            while (i < nbEBands) {
                oldBandE!![c][i] = 0
                oldLogE2!![c][i] =
                        -(0.5 + 28.0f * (1 shl CeltConstants.DB_SHIFT)).toShort()/*Inlines.QCONST16(28.0f, CeltConstants.DB_SHIFT)*/
                oldLogE!![c][i] = oldLogE2!![c][i]
                i++
            }
        } while (++c < CC)

        if (isTransient != 0 || transient_got_disabled != 0) {
            this.consec_transient++
        } else {
            this.consec_transient = 0
        }
        this.rng = enc!!.rng.toInt()

        /* If there's any room left (can only happen for very high rates),
           it's already filled with zeros */
        enc!!.enc_done()

        return if (enc!!._error != 0) {
            OpusError.OPUS_INTERNAL_ERROR
        } else {
            nbCompressedBytes
        }
    }

    fun SetComplexity(value: Int) {
        if (value < 0 || value > 10) {
            throw IllegalArgumentException("Complexity must be between 0 and 10 inclusive")
        }
        this.complexity = value
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

    fun SetPacketLossPercent(value: Int) {
        if (value < 0 || value > 100) {
            throw IllegalArgumentException("Packet loss must be between 0 and 100")
        }
        this.loss_rate = value
    }

    fun SetPrediction(value: Int) {
        if (value < 0 || value > 2) {
            throw IllegalArgumentException("CELT prediction mode must be 0, 1, or 2")
        }
        this.disable_pf = if (value <= 1) 1 else 0
        this.force_intra = if (value == 0) 1 else 0
    }

    fun SetVBRConstraint(value: Boolean) {
        this.constrained_vbr = if (value) 1 else 0
    }

    fun SetVBR(value: Boolean) {
        this.vbr = if (value) 1 else 0
    }

    fun SetBitrate(value: Int) {
        var value = value
        if (value <= 500 && value != OpusConstants.OPUS_BITRATE_MAX) {
            throw IllegalArgumentException("Bitrate out of range")
        }
        value = Inlines.IMIN(value, 260000 * this.channels)
        this.bitrate = value
    }

    fun SetChannels(value: Int) {
        if (value < 1 || value > 2) {
            throw IllegalArgumentException("Channel count must be 1 or 2")
        }
        this.stream_channels = value
    }

    fun SetLSBDepth(value: Int) {
        if (value < 8 || value > 24) {
            throw IllegalArgumentException("Bit depth must be between 8 and 24")
        }
        this.lsb_depth = value
    }

    fun GetLSBDepth(): Int {
        return this.lsb_depth
    }

    fun SetExpertFrameDuration(value: OpusFramesize) {
        this.variable_duration = value
    }

    fun SetSignalling(value: Int) {
        this.signalling = value
    }

    fun SetAnalysis(value: AnalysisInfo?) {
        if (value == null) {
            throw IllegalArgumentException("AnalysisInfo")
        }
        this.analysis.Assign(value)
    }

    fun GetMode(): CeltMode? {
        return this.mode
    }

    fun GetFinalRange(): Int {
        return this.rng
    }

    fun SetLFE(value: Int) {
        this.lfe = value
    }

    fun SetEnergyMask(value: IntArray) {
        this.energy_mask = value
    }

}
