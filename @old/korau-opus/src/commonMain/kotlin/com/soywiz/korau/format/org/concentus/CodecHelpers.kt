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
import kotlin.math.*

object CodecHelpers {

	/* Don't use more than 60 ms for the frame size analysis */
	private val MAX_DYNAMIC_FRAMESIZE = 24

	internal fun gen_toc(mode: OpusMode, framerate: Int, bandwidth: OpusBandwidth, channels: Int): Byte {
		var framerate = framerate
		var period: Int
		var toc: Short
		period = 0
		while (framerate < 400) {
			framerate = framerate shl 1
			period++
		}
		if (mode == OpusMode.MODE_SILK_ONLY) {
			toc =
					(OpusBandwidthHelpers.GetOrdinal(bandwidth) - OpusBandwidthHelpers.GetOrdinal(OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND) shl 5).toShort()
			toc = (toc or (period - 2 shl 3).toShort()).toShort()
		} else if (mode == OpusMode.MODE_CELT_ONLY) {
			var tmp =
				OpusBandwidthHelpers.GetOrdinal(bandwidth) - OpusBandwidthHelpers.GetOrdinal(OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND)
			if (tmp < 0) {
				tmp = 0
			}
			toc = 0x80
			toc = (toc or (tmp shl 5).toShort()).toShort()
			toc = (toc or (period shl 3).toShort()).toShort()
		} else
		/* Hybrid */ {
			toc = 0x60
			toc = (toc or
					(OpusBandwidthHelpers.GetOrdinal(bandwidth) - OpusBandwidthHelpers.GetOrdinal(OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND) shl 4).toShort()).toShort()
			toc = (toc or (period - 2 shl 3).toShort()).toShort()
		}
		toc = (toc or ((if (channels == 2) 1 else 0) shl 2).toShort()).toShort()
		return (0xFF and toc.toInt()).toByte()
	}

	internal fun hp_cutoff(
		input: ShortArray,
		input_ptr: Int,
		cutoff_Hz: Int,
		output: ShortArray,
		output_ptr: Int,
		hp_mem: IntArray,
		len: Int,
		channels: Int,
		Fs: Int
	) {
		val B_Q28 = IntArray(3)
		val A_Q28 = IntArray(2)
		val Fc_Q19: Int
		val r_Q28: Int
		val r_Q22: Int

		Inlines.OpusAssert(cutoff_Hz <= Int.MAX_VALUE / (1.5f * 3.14159f / 1000 * (1.toLong() shl 19) + 0.5).toInt()/*Inlines.SILK_CONST(1.5f * 3.14159f / 1000, 19)*/)
		Fc_Q19 = Inlines.silk_DIV32_16(
			Inlines.silk_SMULBB(
				(1.5f * 3.14159f / 1000 * (1.toLong() shl 19) + 0.5).toInt()/*Inlines.SILK_CONST(1.5f * 3.14159f / 1000, 19)*/,
				cutoff_Hz
			), Fs / 1000
		)
		Inlines.OpusAssert(Fc_Q19 > 0 && Fc_Q19 < 32768)

		r_Q28 = (1.0f * (1.toLong() shl 28) + 0.5).toInt()/*Inlines.SILK_CONST(1.0f, 28)*/ -
				Inlines.silk_MUL((0.92f * (1.toLong() shl 9) + 0.5).toInt()/*Inlines.SILK_CONST(0.92f, 9)*/, Fc_Q19)

		/* b = r * [ 1; -2; 1 ]; */
		/* a = [ 1; -2 * r * ( 1 - 0.5 * Fc^2 ); r^2 ]; */
		B_Q28[0] = r_Q28
		B_Q28[1] = Inlines.silk_LSHIFT(-r_Q28, 1)
		B_Q28[2] = r_Q28

		/* -r * ( 2 - Fc * Fc ); */
		r_Q22 = Inlines.silk_RSHIFT(r_Q28, 6)
		A_Q28[0] = Inlines.silk_SMULWW(
			r_Q22,
			Inlines.silk_SMULWW(
				Fc_Q19,
				Fc_Q19
			) - (2.0f * (1.toLong() shl 22) + 0.5).toInt()/*Inlines.SILK_CONST(2.0f, 22)*/
		)
		A_Q28[1] = Inlines.silk_SMULWW(r_Q22, r_Q22)

		Filters.silk_biquad_alt(input, input_ptr, B_Q28, A_Q28, hp_mem, 0, output, output_ptr, len, channels)
		if (channels == 2) {
			Filters.silk_biquad_alt(
				input,
				input_ptr + 1,
				B_Q28,
				A_Q28,
				hp_mem,
				2,
				output,
				output_ptr + 1,
				len,
				channels
			)
		}
	}

	internal fun dc_reject(
		input: ShortArray,
		input_ptr: Int,
		cutoff_Hz: Int,
		output: ShortArray,
		output_ptr: Int,
		hp_mem: IntArray,
		len: Int,
		channels: Int,
		Fs: Int
	) {
		var c: Int
		var i: Int
		val shift: Int

		/* Approximates -round(log2(4.*cutoff_Hz/Fs)) */
		shift = Inlines.celt_ilog2(Fs / (cutoff_Hz * 3))
		c = 0
		while (c < channels) {
			i = 0
			while (i < len) {
				val x: Int
				val tmp: Int
				val y: Int
				x = Inlines.SHL32(Inlines.EXTEND32(input[channels * i + c + input_ptr]), 15)
				/* First stage */
				tmp = x - hp_mem[2 * c]
				hp_mem[2 * c] = hp_mem[2 * c] + Inlines.PSHR32(x - hp_mem[2 * c], shift)
				/* Second stage */
				y = tmp - hp_mem[2 * c + 1]
				hp_mem[2 * c + 1] = hp_mem[2 * c + 1] + Inlines.PSHR32(tmp - hp_mem[2 * c + 1], shift)
				output[channels * i + c + output_ptr] =
						Inlines.EXTRACT16(Inlines.SATURATE(Inlines.PSHR32(y, 15), 32767))
				i++
			}
			c++
		}
	}

	internal fun stereo_fade(
		pcm_buf: ShortArray,
		g1: Int,
		g2: Int,
		overlap48: Int,
		frame_size: Int,
		channels: Int,
		window: IntArray,
		Fs: Int
	) {
		var g1 = g1
		var g2 = g2
		var i: Int
		val overlap: Int
		val inc: Int
		inc = 48000 / Fs
		overlap = overlap48 / inc
		g1 = CeltConstants.Q15ONE - g1
		g2 = CeltConstants.Q15ONE - g2
		i = 0
		while (i < overlap) {
			var diff: Int
			val g: Int
			val w: Int
			w = Inlines.MULT16_16_Q15(window[i * inc], window[i * inc])
			g = Inlines.SHR32(
				Inlines.MAC16_16(
					Inlines.MULT16_16(w, g2),
					CeltConstants.Q15ONE - w, g1
				), 15
			)
			diff = Inlines.EXTRACT16(Inlines.HALF32(pcm_buf[i * channels].toInt() - pcm_buf[i * channels + 1].toInt()))
				.toInt()
			diff = Inlines.MULT16_16_Q15(g, diff)
			pcm_buf[i * channels] = (pcm_buf[i * channels] - diff).toShort()
			pcm_buf[i * channels + 1] = (pcm_buf[i * channels + 1] + diff).toShort()
			i++
		}
		while (i < frame_size) {
			var diff: Int
			diff = Inlines.EXTRACT16(Inlines.HALF32(pcm_buf[i * channels].toInt() - pcm_buf[i * channels + 1].toInt()))
				.toInt()
			diff = Inlines.MULT16_16_Q15(g2, diff)
			pcm_buf[i * channels] = (pcm_buf[i * channels] - diff).toShort()
			pcm_buf[i * channels + 1] = (pcm_buf[i * channels + 1] + diff).toShort()
			i++
		}
	}

	internal fun gain_fade(
		buffer: ShortArray, buf_ptr: Int, g1: Int, g2: Int,
		overlap48: Int, frame_size: Int, channels: Int, window: IntArray, Fs: Int
	) {
		var i: Int
		val inc: Int
		val overlap: Int
		var c: Int
		inc = 48000 / Fs
		overlap = overlap48 / inc
		if (channels == 1) {
			i = 0
			while (i < overlap) {
				val g: Int
				val w: Int
				w = Inlines.MULT16_16_Q15(window[i * inc], window[i * inc])
				g = Inlines.SHR32(
					Inlines.MAC16_16(
						Inlines.MULT16_16(w, g2),
						CeltConstants.Q15ONE - w, g1
					), 15
				)
				buffer[buf_ptr + i] = Inlines.MULT16_16_Q15(g, buffer[buf_ptr + i].toInt()).toShort()
				i++
			}
		} else {
			i = 0
			while (i < overlap) {
				val g: Int
				val w: Int
				w = Inlines.MULT16_16_Q15(window[i * inc], window[i * inc])
				g = Inlines.SHR32(
					Inlines.MAC16_16(
						Inlines.MULT16_16(w, g2),
						CeltConstants.Q15ONE - w, g1
					), 15
				)
				buffer[buf_ptr + i * 2] = Inlines.MULT16_16_Q15(g, buffer[buf_ptr + i * 2].toInt()).toShort()
				buffer[buf_ptr + i * 2 + 1] = Inlines.MULT16_16_Q15(g, buffer[buf_ptr + i * 2 + 1].toInt()).toShort()
				i++
			}
		}
		c = 0
		do {
			i = overlap
			while (i < frame_size) {
				buffer[buf_ptr + i * channels + c] =
						Inlines.MULT16_16_Q15(g2, buffer[buf_ptr + i * channels + c].toInt()).toShort()
				i++
			}
		} while (++c < channels)
	}

	/* Estimates how much the bitrate will be boosted based on the sub-frame energy */
	internal fun transient_boost(E: FloatArray, E_ptr: Int, E_1: FloatArray, LM: Int, maxM: Int): Float {
		var i: Int
		val M: Int
		var sumE = 0f
		var sumE_1 = 0f
		val metric: Float

		M = Inlines.IMIN(maxM, (1 shl LM) + 1)
		i = E_ptr
		while (i < M + E_ptr) {
			sumE += E[i]
			sumE_1 += E_1[i]
			i++
		}
		metric = sumE * sumE_1 / (M * M)
		/*if (LM==3)
           printf("%f\n", metric);*/
		/*return metric>10 ? 1 : 0;*/
		/*return Inlines.MAX16(0,1-exp(-.25*(metric-2.)));*/
		return Inlines.MIN16(1f, sqrt(Inlines.MAX16(0f, .05f * (metric - 2)).toDouble()).toFloat())
	}

	/* Viterbi decoding trying to find the best frame size combination using look-ahead

       State numbering:
        0: unused
        1:  2.5 ms
        2:  5 ms (#1)
        3:  5 ms (#2)
        4: 10 ms (#1)
        5: 10 ms (#2)
        6: 10 ms (#3)
        7: 10 ms (#4)
        8: 20 ms (#1)
        9: 20 ms (#2)
       10: 20 ms (#3)
       11: 20 ms (#4)
       12: 20 ms (#5)
       13: 20 ms (#6)
       14: 20 ms (#7)
       15: 20 ms (#8)
     */
	internal fun transient_viterbi(E: FloatArray, E_1: FloatArray, N: Int, frame_cost: Int, rate: Int): Int {
		var i: Int
		val cost = Arrays.InitTwoDimensionalArrayFloat(MAX_DYNAMIC_FRAMESIZE, 16)
		val states = Arrays.InitTwoDimensionalArrayInt(MAX_DYNAMIC_FRAMESIZE, 16)
		var best_cost: Float
		var best_state: Int
		val factor: Float
		/* Take into account that we damp VBR in the 32 kb/s to 64 kb/s range. */
		if (rate < 80) {
			factor = 0f
		} else if (rate > 160) {
			factor = 1f
		} else {
			factor = (rate - 80.0f) / 80.0f
		}
		/* Makes variable framesize less aggressive at lower bitrates, but I can't
           find any valid theoretical justification for this (other than it seems
           to help) */
		i = 0
		while (i < 16) {
			/* Impossible state */
			states[0][i] = -1
			cost[0][i] = 1e10f
			i++
		}
		i = 0
		while (i < 4) {
			cost[0][1 shl i] = (frame_cost + rate * (1 shl i)) * (1 + factor * transient_boost(E, 0, E_1, i, N + 1))
			states[0][1 shl i] = i
			i++
		}
		i = 1
		while (i < N) {
			var j: Int

			/* Follow continuations */
			j = 2
			while (j < 16) {
				cost[i][j] = cost[i - 1][j - 1]
				states[i][j] = j - 1
				j++
			}

			/* New frames */
			j = 0
			while (j < 4) {
				var k: Int
				var min_cost: Float
				val curr_cost: Float
				states[i][1 shl j] = 1
				min_cost = cost[i - 1][1]
				k = 1
				while (k < 4) {
					val tmp = cost[i - 1][(1 shl k + 1) - 1]
					if (tmp < min_cost) {
						states[i][1 shl j] = (1 shl k + 1) - 1
						min_cost = tmp
					}
					k++
				}
				curr_cost = (frame_cost + rate * (1 shl j)) * (1 + factor * transient_boost(E, i, E_1, j, N - i + 1))
				cost[i][1 shl j] = min_cost
				/* If part of the frame is outside the analysis window, only count part of the cost */
				if (N - i < 1 shl j) {
					cost[i][1 shl j] += curr_cost * (N - i).toFloat() / (1 shl j)
				} else {
					cost[i][1 shl j] += curr_cost
				}
				j++
			}
			i++
		}

		best_state = 1
		best_cost = cost[N - 1][1]
		/* Find best end state (doesn't force a frame to end at N-1) */
		i = 2
		while (i < 16) {
			if (cost[N - 1][i] < best_cost) {
				best_cost = cost[N - 1][i]
				best_state = i
			}
			i++
		}

		/* Follow transitions back */
		i = N - 1
		while (i >= 0) {
			/*printf("%d ", best_state);*/
			best_state = states[i][best_state]
			i--
		}
		/*printf("%d\n", best_state);*/
		return best_state
	}

	internal fun optimize_framesize(
		x: ShortArray, x_ptr: Int, len: Int, C: Int, Fs: Int,
		bitrate: Int, tonality: Int, mem: FloatArray, buffering: Int
	): Int {
		var len = len
		var N: Int
		var i: Int
		val e = FloatArray(MAX_DYNAMIC_FRAMESIZE + 4)
		val e_1 = FloatArray(MAX_DYNAMIC_FRAMESIZE + 3)
		var memx: Int
		var bestLM = 0
		val subframe: Int
		val pos: Int
		val offset: Int
		val sub: IntArray

		subframe = Fs / 400
		sub = IntArray(subframe)
		e[0] = mem[0]
		e_1[0] = 1.0f / (CeltConstants.EPSILON + mem[0])
		if (buffering != 0) {
			/* Consider the CELT delay when not in restricted-lowdelay */
			/* We assume the buffering is between 2.5 and 5 ms */
			offset = 2 * subframe - buffering
			Inlines.OpusAssert(offset >= 0 && offset <= subframe)
			len -= offset
			e[1] = mem[1]
			e_1[1] = 1.0f / (CeltConstants.EPSILON + mem[1])
			e[2] = mem[2]
			e_1[2] = 1.0f / (CeltConstants.EPSILON + mem[2])
			pos = 3
		} else {
			pos = 1
			offset = 0
		}
		N = Inlines.IMIN(len / subframe, MAX_DYNAMIC_FRAMESIZE)
		/* Just silencing a warning, it's really initialized later */
		memx = 0
		i = 0
		while (i < N) {
			var tmp: Float
			var tmpx: Int
			var j: Int
			tmp = CeltConstants.EPSILON.toFloat()

			Downmix.downmix_int(x, x_ptr, sub, 0, subframe, i * subframe + offset, 0, -2, C)
			if (i == 0) {
				memx = sub[0]
			}
			j = 0
			while (j < subframe) {
				tmpx = sub[j]
				tmp += (tmpx - memx) * (tmpx - memx).toFloat()
				memx = tmpx
				j++
			}
			e[i + pos] = tmp
			e_1[i + pos] = 1.0f / tmp
			i++
		}
		/* Hack to get 20 ms working with APPLICATION_AUDIO
           The real problem is that the corresponding memory needs to use 1.5 ms
           from this frame and 1 ms from the next frame */
		e[i + pos] = e[i + pos - 1]
		if (buffering != 0) {
			N = Inlines.IMIN(MAX_DYNAMIC_FRAMESIZE, N + 2)
		}
		bestLM = transient_viterbi(e, e_1, N, ((1.0f + .5f * tonality) * (60 * C + 40)).toInt(), bitrate / 400)
		mem[0] = e[1 shl bestLM]
		if (buffering != 0) {
			mem[1] = e[(1 shl bestLM) + 1]
			mem[2] = e[(1 shl bestLM) + 2]
		}
		return bestLM
	}

	internal fun frame_size_select(frame_size: Int, variable_duration: OpusFramesize, Fs: Int): Int {
		val new_size: Int
		if (frame_size < Fs / 400) {
			return -1
		}
		if (variable_duration == OpusFramesize.OPUS_FRAMESIZE_ARG) {
			new_size = frame_size
		} else if (variable_duration == OpusFramesize.OPUS_FRAMESIZE_VARIABLE) {
			new_size = Fs / 50
		} else if (OpusFramesizeHelpers.GetOrdinal(variable_duration) >= OpusFramesizeHelpers.GetOrdinal(OpusFramesize.OPUS_FRAMESIZE_2_5_MS) && OpusFramesizeHelpers.GetOrdinal(
				variable_duration
			) <= OpusFramesizeHelpers.GetOrdinal(OpusFramesize.OPUS_FRAMESIZE_60_MS)
		) {
			new_size = Inlines.IMIN(
				3 * Fs / 50,
				Fs / 400 shl OpusFramesizeHelpers.GetOrdinal(variable_duration) - OpusFramesizeHelpers.GetOrdinal(
					OpusFramesize.OPUS_FRAMESIZE_2_5_MS
				)
			)
		} else {
			return -1
		}
		if (new_size > frame_size) {
			return -1
		}
		return if (400 * new_size != Fs && 200 * new_size != Fs && 100 * new_size != Fs
			&& 50 * new_size != Fs && 25 * new_size != Fs && 50 * new_size != 3 * Fs
		) {
			-1
		} else new_size
	}

	internal fun compute_frame_size(
		analysis_pcm: ShortArray, analysis_pcm_ptr: Int, frame_size: Int,
		variable_duration: OpusFramesize, C: Int, Fs: Int, bitrate_bps: Int,
		delay_compensation: Int, subframe_mem: FloatArray, analysis_enabled: Boolean
	): Int {
		var frame_size = frame_size

		if (analysis_enabled && variable_duration == OpusFramesize.OPUS_FRAMESIZE_VARIABLE && frame_size >= Fs / 200) {
			var LM = 3
			LM = optimize_framesize(
				analysis_pcm, analysis_pcm_ptr, frame_size, C, Fs, bitrate_bps,
				0, subframe_mem, delay_compensation
			)
			while (Fs / 400 shl LM > frame_size) {
				LM--
			}
			frame_size = Fs / 400 shl LM
		} else {
			frame_size = frame_size_select(frame_size, variable_duration, Fs)
		}

		return if (frame_size < 0) {
			-1
		} else frame_size
	}

	internal fun compute_stereo_width(
		pcm: ShortArray,
		pcm_ptr: Int,
		frame_size: Int,
		Fs: Int,
		mem: StereoWidthState
	): Int {
		val corr: Int
		val ldiff: Int
		val width: Int
		var xx: Int
		var xy: Int
		var yy: Int
		val sqrt_xx: Int
		val sqrt_yy: Int
		val qrrt_xx: Int
		val qrrt_yy: Int
		val frame_rate: Int
		var i: Int
		val short_alpha: Int

		frame_rate = Fs / frame_size
		short_alpha = CeltConstants.Q15ONE - 25 * CeltConstants.Q15ONE / Inlines.IMAX(50, frame_rate)
		yy = 0
		xy = yy
		xx = xy
		i = 0
		while (i < frame_size - 3) {
			var pxx = 0
			var pxy = 0
			var pyy = 0
			var x: Int
			var y: Int
			val p2i = pcm_ptr + 2 * i
			x = pcm[p2i].toInt()
			y = pcm[p2i + 1].toInt()
			pxx = Inlines.SHR32(Inlines.MULT16_16(x, x), 2)
			pxy = Inlines.SHR32(Inlines.MULT16_16(x, y), 2)
			pyy = Inlines.SHR32(Inlines.MULT16_16(y, y), 2)
			x = pcm[p2i + 2].toInt()
			y = pcm[p2i + 3].toInt()
			pxx += Inlines.SHR32(Inlines.MULT16_16(x, x), 2)
			pxy += Inlines.SHR32(Inlines.MULT16_16(x, y), 2)
			pyy += Inlines.SHR32(Inlines.MULT16_16(y, y), 2)
			x = pcm[p2i + 4].toInt()
			y = pcm[p2i + 5].toInt()
			pxx += Inlines.SHR32(Inlines.MULT16_16(x, x), 2)
			pxy += Inlines.SHR32(Inlines.MULT16_16(x, y), 2)
			pyy += Inlines.SHR32(Inlines.MULT16_16(y, y), 2)
			x = pcm[p2i + 6].toInt()
			y = pcm[p2i + 7].toInt()
			pxx += Inlines.SHR32(Inlines.MULT16_16(x, x), 2)
			pxy += Inlines.SHR32(Inlines.MULT16_16(x, y), 2)
			pyy += Inlines.SHR32(Inlines.MULT16_16(y, y), 2)

			xx += Inlines.SHR32(pxx, 10)
			xy += Inlines.SHR32(pxy, 10)
			yy += Inlines.SHR32(pyy, 10)
			i += 4
		}

		mem.XX += Inlines.MULT16_32_Q15(short_alpha, xx - mem.XX)
		mem.XY += Inlines.MULT16_32_Q15(short_alpha, xy - mem.XY)
		mem.YY += Inlines.MULT16_32_Q15(short_alpha, yy - mem.YY)
		mem.XX = Inlines.MAX32(0, mem.XX)
		mem.XY = Inlines.MAX32(0, mem.XY)
		mem.YY = Inlines.MAX32(0, mem.YY)
		if (Inlines.MAX32(mem.XX, mem.YY) > (0.5 + 8e-4f * (1 shl 18)).toShort()/*Inlines.QCONST16(8e-4f, 18)*/) {
			sqrt_xx = Inlines.celt_sqrt(mem.XX)
			sqrt_yy = Inlines.celt_sqrt(mem.YY)
			qrrt_xx = Inlines.celt_sqrt(sqrt_xx)
			qrrt_yy = Inlines.celt_sqrt(sqrt_yy)
			/* Inter-channel correlation */
			mem.XY = Inlines.MIN32(mem.XY, sqrt_xx * sqrt_yy)
			corr = Inlines.SHR32(
				Inlines.frac_div32(
					mem.XY,
					CeltConstants.EPSILON + Inlines.MULT16_16(sqrt_xx, sqrt_yy)
				), 16
			)
			/* Approximate loudness difference */
			ldiff = CeltConstants.Q15ONE * Inlines.ABS16(qrrt_xx - qrrt_yy) /
					(CeltConstants.EPSILON + qrrt_xx + qrrt_yy)
			width = Inlines.MULT16_16_Q15(
				Inlines.celt_sqrt(
					(0.5 + 1.0f * (1 shl 30)).toInt()/*Inlines.QCONST32(1.0f, 30)*/ - Inlines.MULT16_16(
						corr,
						corr
					)
				), ldiff
			)
			/* Smoothing over one second */
			mem.smoothed_width += (width - mem.smoothed_width) / frame_rate
			/* Peak follower */
			mem.max_follower = Inlines.MAX16(
				mem.max_follower - (0.5 + .02f * (1 shl 15)).toShort()/*Inlines.QCONST16(.02f, 15)*/ / frame_rate,
				mem.smoothed_width
			)
		} else {
			width = 0
			corr = CeltConstants.Q15ONE
			ldiff = 0
		}
		/*printf("%f %f %f %f %f ", corr/(float)1.0f, ldiff/(float)1.0f, width/(float)1.0f, mem.smoothed_width/(float)1.0f, mem.max_follower/(float)1.0f);*/
		return Inlines.EXTRACT16(Inlines.MIN32(CeltConstants.Q15ONE, 20 * mem.max_follower)).toInt()
	}

	internal fun smooth_fade(
		in1: ShortArray, in1_ptr: Int, in2: ShortArray, in2_ptr: Int,
		output: ShortArray, output_ptr: Int, overlap: Int, channels: Int,
		window: IntArray, Fs: Int
	) {
		var i: Int
		var c: Int
		val inc = 48000 / Fs
		c = 0
		while (c < channels) {
			i = 0
			while (i < overlap) {
				val w = Inlines.MULT16_16_Q15(window[i * inc], window[i * inc])
				output[output_ptr + i * channels + c] = Inlines.SHR32(
					Inlines.MAC16_16(
						Inlines.MULT16_16(w, in2[in2_ptr + i * channels + c].toInt()),
						CeltConstants.Q15ONE - w, in1[in1_ptr + i * channels + c].toInt()
					), 15
				).toShort()
				i++
			}
			c++
		}
	}

	//static void opus_pcm_soft_clip(Pointer<float> _x, int N, int C, Pointer<float> declip_mem)
	//{
	//    int c;
	//    int i;
	//    Pointer<float> x;
	//    if (C < 1 || N < 1 || _x == null || declip_mem == null) return;
	//    /* First thing: saturate everything to +/- 2 which is the highest level our
	//       non-linearity can handle. At the point where the signal reaches +/-2,
	//       the derivative will be zero anyway, so this doesn't introduce any
	//       discontinuity in the derivative. */
	//    for (i = 0; i < N * C; i++)
	//        _x[i] = Inlines.MAX16(-2.0f, Inlines.MIN16(2.0f, _x[i]));
	//    for (c = 0; c < C; c++)
	//    {
	//        float a;
	//        float x0;
	//        int curr;
	//        x = _x.Point(c);
	//        a = declip_mem[c];
	//        /* Continue applying the non-linearity from the previous frame to avoid
	//           any discontinuity. */
	//        for (i = 0; i < N; i++)
	//        {
	//            if (x[i * C] * a >= 0)
	//                break;
	//            x[i * C] = x[i * C] + a * x[i * C] * x[i * C];
	//        }
	//        curr = 0;
	//        x0 = x[0];
	//        while (true)
	//        {
	//            int start, end;
	//            float maxval;
	//            int special = 0;
	//            int peak_pos;
	//            for (i = curr; i < N; i++)
	//            {
	//                if (x[i * C] > 1 || x[i * C] < -1)
	//                    break;
	//            }
	//            if (i == N)
	//            {
	//                a = 0;
	//                break;
	//            }
	//            peak_pos = i;
	//            start = end = i;
	//            maxval = Inlines.ABS16(x[i * C]);
	//            /* Look for first zero crossing before clipping */
	//            while (start > 0 && x[i * C] * x[(start - 1) * C] >= 0)
	//                start--;
	//            /* Look for first zero crossing after clipping */
	//            while (end < N && x[i * C] * x[end * C] >= 0)
	//            {
	//                /* Look for other peaks until the next zero-crossing. */
	//                if (Inlines.ABS16(x[end * C]) > maxval)
	//                {
	//                    maxval = Inlines.ABS16(x[end * C]);
	//                    peak_pos = end;
	//                }
	//                end++;
	//            }
	//            /* Detect the special case where we clip before the first zero crossing */
	//            special = (start == 0 && x[i * C] * x[0] >= 0) ? 1 : 0;
	//            /* Compute a such that maxval + a*maxval^2 = 1 */
	//            a = (maxval - 1) / (maxval * maxval);
	//            if (x[i * C] > 0)
	//                a = -a;
	//            /* Apply soft clipping */
	//            for (i = start; i < end; i++)
	//                x[i * C] = x[i * C] + a * x[i * C] * x[i * C];
	//            if (special != 0 && peak_pos >= 2)
	//            {
	//                /* Add a linear ramp from the first sample to the signal peak.
	//                   This avoids a discontinuity at the beginning of the frame. */
	//                float delta;
	//                float offset = x0 - x[0];
	//                delta = offset / peak_pos;
	//                for (i = curr; i < peak_pos; i++)
	//                {
	//                    offset -= delta;
	//                    x[i * C] += offset;
	//                    x[i * C] = Inlines.MAX16(-1.0f, Inlines.MIN16(1.0f, x[i * C]));
	//                }
	//            }
	//            curr = end;
	//            if (curr == N)
	//            {
	//                break;
	//            }
	//        }
	//        declip_mem[c] = a;
	//    }
	//}
	fun opus_strerror(error: Int): String {
		val error_strings = arrayOf(
			"success",
			"invalid argument",
			"buffer too small",
			"error",
			"corrupted stream",
			"request not implemented",
			"invalid state",
			"memory allocation failed"
		)
		return if (error > 0 || error < -7) {
			"unknown error"
		} else {
			error_strings[-error]
		}
	}

	fun GetVersionString(): String {
		return "concentus 1.0a-java-fixed"
	}
}
