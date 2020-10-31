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

internal object DecodeAPI {

    /// <summary>
    /// Reset decoder state
    /// </summary>
    /// <param name="decState">I/O  Stat</param>
    /// <returns>Returns error code</returns>
    fun silk_InitDecoder(decState: SilkDecoder): Int {
        /* Reset decoder */
        decState.Reset()

        var n: Int
        var ret = SilkError.SILK_NO_ERROR
        val channel_states = decState.channel_state

        n = 0
        while (n < SilkConstants.DECODER_NUM_CHANNELS) {
            ret = channel_states[n].silk_init_decoder()
            n++
        }

        decState.sStereo.Reset()

        /* Not strictly needed, but it's cleaner that way */
        decState.prev_decode_only_middle = 0

        return ret
    }

    /* Decode a frame */
    fun silk_Decode( /* O    Returns error code                              */
        psDec: SilkDecoder, /* I/O  State                                           */
        decControl: DecControlState, /* I/O  Control Structure                               */
        lostFlag: Int, /* I    0: no loss, 1 loss, 2 decode fec                */
        newPacketFlag: Int, /* I    Indicates first decoder call for this packet    */
        psRangeDec: EntropyCoder, /* I/O  Compressor data structure                       */
        samplesOut: ShortArray, /* O    Decoded output speech vector                    */
        samplesOut_ptr: Int,
        nSamplesOut: BoxedValueInt /* O    Number of samples decoded                       */
    ): Int {
        var i: Int
        var n: Int
        var decode_only_middle = 0
        var ret = SilkError.SILK_NO_ERROR
        var LBRR_symbol: Int
        val nSamplesOutDec = BoxedValueInt(0)
        var samplesOut_tmp: ShortArray
        val samplesOut_tmp_ptrs = IntArray(2)
        val samplesOut1_tmp_storage1: ShortArray
        val samplesOut1_tmp_storage2: ShortArray
        val samplesOut2_tmp: ShortArray
        val MS_pred_Q13 = intArrayOf(0, 0)
        val resample_out: ShortArray
        val resample_out_ptr: Int
        val channel_state = psDec.channel_state
        val has_side: Int
        val stereo_to_mono: Int
        val delay_stack_alloc: Int
        nSamplesOut.Val = 0

        Inlines.OpusAssert(decControl.nChannelsInternal == 1 || decControl.nChannelsInternal == 2)

        /**
         * *******************************
         */
        /* Test if first frame in payload */
        /**
         * *******************************
         */
        if (newPacketFlag != 0) {
            n = 0
            while (n < decControl.nChannelsInternal) {
                channel_state[n].nFramesDecoded = 0
                n++
                /* Used to count frames in packet */
            }
        }

        /* If Mono . Stereo transition in bitstream: init state of second channel */
        if (decControl.nChannelsInternal > psDec.nChannelsInternal) {
            ret += channel_state[1].silk_init_decoder()
        }

        stereo_to_mono = if (decControl.nChannelsInternal == 1 && psDec.nChannelsInternal == 2
            && decControl.internalSampleRate == 1000 * channel_state[0].fs_kHz
        )
            1
        else
            0

        if (channel_state[0].nFramesDecoded == 0) {
            n = 0
            while (n < decControl.nChannelsInternal) {
                val fs_kHz_dec: Int
                if (decControl.payloadSize_ms == 0) {
                    /* Assuming packet loss, use 10 ms */
                    channel_state[n].nFramesPerPacket = 1
                    channel_state[n].nb_subfr = 2
                } else if (decControl.payloadSize_ms == 10) {
                    channel_state[n].nFramesPerPacket = 1
                    channel_state[n].nb_subfr = 2
                } else if (decControl.payloadSize_ms == 20) {
                    channel_state[n].nFramesPerPacket = 1
                    channel_state[n].nb_subfr = 4
                } else if (decControl.payloadSize_ms == 40) {
                    channel_state[n].nFramesPerPacket = 2
                    channel_state[n].nb_subfr = 4
                } else if (decControl.payloadSize_ms == 60) {
                    channel_state[n].nFramesPerPacket = 3
                    channel_state[n].nb_subfr = 4
                } else {
                    Inlines.OpusAssert(false)
                    return SilkError.SILK_DEC_INVALID_FRAME_SIZE
                }
                fs_kHz_dec = (decControl.internalSampleRate shr 10) + 1
                if (fs_kHz_dec != 8 && fs_kHz_dec != 12 && fs_kHz_dec != 16) {
                    Inlines.OpusAssert(false)
                    return SilkError.SILK_DEC_INVALID_SAMPLING_FREQUENCY
                }
                ret += channel_state[n].silk_decoder_set_fs(fs_kHz_dec, decControl.API_sampleRate)
                n++
            }
        }

        if (decControl.nChannelsAPI == 2 && decControl.nChannelsInternal == 2 && (psDec.nChannelsAPI == 1 || psDec.nChannelsInternal == 1)) {
            Arrays.MemSet(psDec.sStereo.pred_prev_Q13, 0.toShort(), 2)
            Arrays.MemSet(psDec.sStereo.sSide, 0.toShort(), 2)
            channel_state[1].resampler_state.Assign(channel_state[0].resampler_state)
        }
        psDec.nChannelsAPI = decControl.nChannelsAPI
        psDec.nChannelsInternal = decControl.nChannelsInternal

        if (decControl.API_sampleRate > SilkConstants.MAX_API_FS_KHZ.toInt() * 1000 || decControl.API_sampleRate < 8000) {
            ret = SilkError.SILK_DEC_INVALID_SAMPLING_FREQUENCY
            return ret
        }

        if (lostFlag != DecoderAPIFlag.FLAG_PACKET_LOST && channel_state[0].nFramesDecoded == 0) {
            /* First decoder call for this payload */
            /* Decode VAD flags and LBRR flag */
            n = 0
            while (n < decControl.nChannelsInternal) {
                i = 0
                while (i < channel_state[n].nFramesPerPacket) {
                    channel_state[n].VAD_flags[i] = psRangeDec.dec_bit_logp(1)
                    i++
                }
                channel_state[n].LBRR_flag = psRangeDec.dec_bit_logp(1)
                n++
            }
            /* Decode LBRR flags */
            n = 0
            while (n < decControl.nChannelsInternal) {
                Arrays.MemSet(channel_state[n].LBRR_flags, 0, SilkConstants.MAX_FRAMES_PER_PACKET)
                if (channel_state[n].LBRR_flag != 0) {
                    if (channel_state[n].nFramesPerPacket == 1) {
                        channel_state[n].LBRR_flags[0] = 1
                    } else {
                        LBRR_symbol = psRangeDec.dec_icdf(
                            SilkTables.silk_LBRR_flags_iCDF_ptr[channel_state[n].nFramesPerPacket - 2],
                            8
                        ) + 1
                        i = 0
                        while (i < channel_state[n].nFramesPerPacket) {
                            channel_state[n].LBRR_flags[i] = Inlines.silk_RSHIFT(LBRR_symbol, i) and 1
                            i++
                        }
                    }
                }
                n++
            }

            if (lostFlag == DecoderAPIFlag.FLAG_DECODE_NORMAL) {
                /* Regular decoding: skip all LBRR data */
                i = 0
                while (i < channel_state[0].nFramesPerPacket) {
                    n = 0
                    while (n < decControl.nChannelsInternal) {
                        if (channel_state[n].LBRR_flags[i] != 0) {
                            val pulses = ShortArray(SilkConstants.MAX_FRAME_LENGTH)
                            val condCoding: Int

                            if (decControl.nChannelsInternal == 2 && n == 0) {
                                Stereo.silk_stereo_decode_pred(psRangeDec, MS_pred_Q13)
                                if (channel_state[1].LBRR_flags[i] == 0) {
                                    val decodeOnlyMiddleBoxed = BoxedValueInt(decode_only_middle)
                                    Stereo.silk_stereo_decode_mid_only(psRangeDec, decodeOnlyMiddleBoxed)
                                    decode_only_middle = decodeOnlyMiddleBoxed.Val
                                }
                            }
                            /* Use conditional coding if previous frame available */
                            if (i > 0 && channel_state[n].LBRR_flags[i - 1] != 0) {
                                condCoding = SilkConstants.CODE_CONDITIONALLY
                            } else {
                                condCoding = SilkConstants.CODE_INDEPENDENTLY
                            }
                            DecodeIndices.silk_decode_indices(channel_state[n], psRangeDec, i, 1, condCoding)
                            DecodePulses.silk_decode_pulses(
                                psRangeDec, pulses, channel_state[n].indices.signalType.toInt(),
                                channel_state[n].indices.quantOffsetType.toInt(), channel_state[n].frame_length
                            )
                        }
                        n++
                    }
                    i++
                }
            }
        }

        /* Get MS predictor index */
        if (decControl.nChannelsInternal == 2) {
            if (lostFlag == DecoderAPIFlag.FLAG_DECODE_NORMAL || lostFlag == DecoderAPIFlag.FLAG_DECODE_LBRR && channel_state[0].LBRR_flags[channel_state[0].nFramesDecoded] == 1) {
                Stereo.silk_stereo_decode_pred(psRangeDec, MS_pred_Q13)
                /* For LBRR data, decode mid-only flag only if side-channel's LBRR flag is false */
                if (lostFlag == DecoderAPIFlag.FLAG_DECODE_NORMAL && channel_state[1].VAD_flags[channel_state[0].nFramesDecoded] == 0 || lostFlag == DecoderAPIFlag.FLAG_DECODE_LBRR && channel_state[1].LBRR_flags[channel_state[0].nFramesDecoded] == 0) {
                    val decodeOnlyMiddleBoxed = BoxedValueInt(decode_only_middle)
                    Stereo.silk_stereo_decode_mid_only(psRangeDec, decodeOnlyMiddleBoxed)
                    decode_only_middle = decodeOnlyMiddleBoxed.Val
                } else {
                    decode_only_middle = 0
                }
            } else {
                n = 0
                while (n < 2) {
                    MS_pred_Q13[n] = psDec.sStereo.pred_prev_Q13[n].toInt()
                    n++
                }
            }
        }

        /* Reset side channel decoder prediction memory for first frame with side coding */
        if (decControl.nChannelsInternal == 2 && decode_only_middle == 0 && psDec.prev_decode_only_middle == 1) {
            Arrays.MemSet(
                psDec.channel_state[1].outBuf,
                0.toShort(),
                SilkConstants.MAX_FRAME_LENGTH + 2 * SilkConstants.MAX_SUB_FRAME_LENGTH
            )
            Arrays.MemSet(psDec.channel_state[1].sLPC_Q14_buf, 0, SilkConstants.MAX_LPC_ORDER)
            psDec.channel_state[1].lagPrev = 100
            psDec.channel_state[1].LastGainIndex = 10
            psDec.channel_state[1].prevSignalType = SilkConstants.TYPE_NO_VOICE_ACTIVITY
            psDec.channel_state[1].first_frame_after_reset = 1
        }

        /* Check if the temp buffer fits into the output PCM buffer. If it fits,
           we can delay allocating the temp buffer until after the SILK peak stack
           usage. We need to use a < and not a <= because of the two extra samples. */
        delay_stack_alloc =
                if (decControl.internalSampleRate * decControl.nChannelsInternal < decControl.API_sampleRate * decControl.nChannelsAPI)
                    1
                else
                    0

        if (delay_stack_alloc != 0) {
            samplesOut_tmp = samplesOut
            samplesOut_tmp_ptrs[0] = samplesOut_ptr
            samplesOut_tmp_ptrs[1] = samplesOut_ptr + channel_state[0].frame_length + 2
        } else {
            samplesOut1_tmp_storage1 = ShortArray(decControl.nChannelsInternal * (channel_state[0].frame_length + 2))
            samplesOut_tmp = samplesOut1_tmp_storage1
            samplesOut_tmp_ptrs[0] = 0
            samplesOut_tmp_ptrs[1] = channel_state[0].frame_length + 2
        }

        if (lostFlag == DecoderAPIFlag.FLAG_DECODE_NORMAL) {
            has_side = if (decode_only_middle == 0) 1 else 0
        } else {
            has_side = if (psDec.prev_decode_only_middle == 0 || (decControl.nChannelsInternal == 2
                        && lostFlag == DecoderAPIFlag.FLAG_DECODE_LBRR
                        && channel_state[1].LBRR_flags[channel_state[1].nFramesDecoded] == 1)
            )
                1
            else
                0
        }
        /* Call decoder for one frame */
        n = 0
        while (n < decControl.nChannelsInternal) {
            if (n == 0 || has_side != 0) {
                val FrameIndex: Int
                val condCoding: Int

                FrameIndex = channel_state[0].nFramesDecoded - n
                /* Use independent coding if no previous frame available */
                if (FrameIndex <= 0) {
                    condCoding = SilkConstants.CODE_INDEPENDENTLY
                } else if (lostFlag == DecoderAPIFlag.FLAG_DECODE_LBRR) {
                    condCoding =
                            if (channel_state[n].LBRR_flags[FrameIndex - 1] != 0) SilkConstants.CODE_CONDITIONALLY else SilkConstants.CODE_INDEPENDENTLY
                } else if (n > 0 && psDec.prev_decode_only_middle != 0) {
                    /* If we skipped a side frame in this packet, we don't
                       need LTP scaling; the LTP state is well-defined. */
                    condCoding = SilkConstants.CODE_INDEPENDENTLY_NO_LTP_SCALING
                } else {
                    condCoding = SilkConstants.CODE_CONDITIONALLY
                }
                ret += channel_state[n].silk_decode_frame(
                    psRangeDec,
                    samplesOut_tmp,
                    samplesOut_tmp_ptrs[n] + 2,
                    nSamplesOutDec,
                    lostFlag,
                    condCoding
                )
            } else {
                Arrays.MemSetWithOffset(samplesOut_tmp, 0.toShort(), samplesOut_tmp_ptrs[n] + 2, nSamplesOutDec.Val)
            }
            channel_state[n].nFramesDecoded++
            n++
        }

        if (decControl.nChannelsAPI == 2 && decControl.nChannelsInternal == 2) {
            /* Convert Mid/Side to Left/Right */
            Stereo.silk_stereo_MS_to_LR(
                psDec.sStereo,
                samplesOut_tmp,
                samplesOut_tmp_ptrs[0],
                samplesOut_tmp,
                samplesOut_tmp_ptrs[1],
                MS_pred_Q13,
                channel_state[0].fs_kHz,
                nSamplesOutDec.Val
            )
        } else {
            /* Buffering */
            arraycopy(psDec.sStereo.sMid, 0, samplesOut_tmp, samplesOut_tmp_ptrs[0], 2)
            arraycopy(samplesOut_tmp, samplesOut_tmp_ptrs[0] + nSamplesOutDec.Val, psDec.sStereo.sMid, 0, 2)
        }

        /* Number of output samples */
        nSamplesOut.Val = Inlines.silk_DIV32(
            nSamplesOutDec.Val * decControl.API_sampleRate,
            Inlines.silk_SMULBB(channel_state[0].fs_kHz, 1000)
        )

        /* Set up pointers to temp buffers */
        if (decControl.nChannelsAPI == 2) {
            samplesOut2_tmp = ShortArray(nSamplesOut.Val)
            resample_out = samplesOut2_tmp
            resample_out_ptr = 0
        } else {
            resample_out = samplesOut
            resample_out_ptr = samplesOut_ptr
        }

        if (delay_stack_alloc != 0) {
            samplesOut1_tmp_storage2 = ShortArray(decControl.nChannelsInternal * (channel_state[0].frame_length + 2))
            arraycopy(
                samplesOut,
                samplesOut_ptr,
                samplesOut1_tmp_storage2,
                0,
                decControl.nChannelsInternal * (channel_state[0].frame_length + 2)
            )
            samplesOut_tmp = samplesOut1_tmp_storage2
            samplesOut_tmp_ptrs[0] = 0
            samplesOut_tmp_ptrs[1] = channel_state[0].frame_length + 2
        }
        n = 0
        while (n < Inlines.silk_min(decControl.nChannelsAPI, decControl.nChannelsInternal)) {

            /* Resample decoded signal to API_sampleRate */
            ret += Resampler.silk_resampler(
                channel_state[n].resampler_state,
                resample_out,
                resample_out_ptr,
                samplesOut_tmp,
                samplesOut_tmp_ptrs[n] + 1,
                nSamplesOutDec.Val
            )

            /* Interleave if stereo output and stereo stream */
            if (decControl.nChannelsAPI == 2) {
                val nptr = samplesOut_ptr + n
                i = 0
                while (i < nSamplesOut.Val) {
                    samplesOut[nptr + 2 * i] = resample_out[resample_out_ptr + i]
                    i++
                }
            }
            n++
        }

        /* Create two channel output from mono stream */
        if (decControl.nChannelsAPI == 2 && decControl.nChannelsInternal == 1) {
            if (stereo_to_mono != 0) {
                /* Resample right channel for newly collapsed stereo just in case
                   we weren't doing collapsing when switching to mono */
                ret += Resampler.silk_resampler(
                    channel_state[1].resampler_state,
                    resample_out,
                    resample_out_ptr,
                    samplesOut_tmp,
                    samplesOut_tmp_ptrs[0] + 1,
                    nSamplesOutDec.Val
                )

                i = 0
                while (i < nSamplesOut.Val) {
                    samplesOut[samplesOut_ptr + 1 + 2 * i] = resample_out[resample_out_ptr + i]
                    i++
                }
            } else {
                i = 0
                while (i < nSamplesOut.Val) {
                    samplesOut[samplesOut_ptr + 1 + 2 * i] = samplesOut[samplesOut_ptr + 2 * i]
                    i++
                }
            }
        }

        /* Export pitch lag, measured at 48 kHz sampling rate */
        if (channel_state[0].prevSignalType == SilkConstants.TYPE_VOICED) {
            val mult_tab = intArrayOf(6, 4, 3)
            decControl.prevPitchLag = channel_state[0].lagPrev * mult_tab[channel_state[0].fs_kHz - 8 shr 2]
        } else {
            decControl.prevPitchLag = 0
        }

        if (lostFlag == DecoderAPIFlag.FLAG_PACKET_LOST) {
            /* On packet loss, remove the gain clamping to prevent having the energy "bounce back"
               if we lose packets when the energy is going down */
            i = 0
            while (i < psDec.nChannelsInternal) {
                psDec.channel_state[i].LastGainIndex = 10
                i++
            }
        } else {
            psDec.prev_decode_only_middle = decode_only_middle
        }

        return ret
    }
}
