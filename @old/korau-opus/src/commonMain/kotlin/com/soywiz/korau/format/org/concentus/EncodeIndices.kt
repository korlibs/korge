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

internal object EncodeIndices {

	/// <summary>
	/// Encode side-information parameters to payload
	/// </summary>
	/// <param name="psEncC">I/O  Encoder state</param>
	/// <param name="psRangeEnc">I/O  Compressor data structure</param>
	/// <param name="FrameIndex">I    Frame number</param>
	/// <param name="encode_LBRR">I    Flag indicating LBRR data is being encoded</param>
	/// <param name="condCoding">I    The type of conditional coding to use</param>
	fun silk_encode_indices(
		psEncC: SilkChannelEncoder,
		psRangeEnc: EntropyCoder,
		FrameIndex: Int,
		encode_LBRR: Int,
		condCoding: Int
	) {
		var i: Int
		var k: Int
		val typeOffset: Int
		var encode_absolute_lagIndex: Int
		var delta_lagIndex: Int
		val ec_ix = ShortArray(SilkConstants.MAX_LPC_ORDER)
		val pred_Q8 = ShortArray(SilkConstants.MAX_LPC_ORDER)
		val psIndices: SideInfoIndices

		if (encode_LBRR != 0) {
			psIndices = psEncC.indices_LBRR!![FrameIndex]!!
		} else {
			psIndices = psEncC.indices
		}

		/**
		 * ****************************************
		 */
		/* Encode signal type and quantizer offset */
		/**
		 * ****************************************
		 */
		typeOffset = 2 * psIndices.signalType + psIndices.quantOffsetType
		Inlines.OpusAssert(typeOffset >= 0 && typeOffset < 6)
		Inlines.OpusAssert(encode_LBRR == 0 || typeOffset >= 2)
		if (encode_LBRR != 0 || typeOffset >= 2) {
			psRangeEnc.enc_icdf(typeOffset - 2, SilkTables.silk_type_offset_VAD_iCDF, 8)
		} else {
			psRangeEnc.enc_icdf(typeOffset, SilkTables.silk_type_offset_no_VAD_iCDF, 8)
		}

		/**
		 * *************
		 */
		/* Encode gains */
		/**
		 * *************
		 */
		/* first subframe */
		if (condCoding == SilkConstants.CODE_CONDITIONALLY) {
			/* conditional coding */
			Inlines.OpusAssert(psIndices.GainsIndices[0] >= 0 && psIndices.GainsIndices[0] < SilkConstants.MAX_DELTA_GAIN_QUANT - SilkConstants.MIN_DELTA_GAIN_QUANT + 1)
			psRangeEnc.enc_icdf(psIndices.GainsIndices[0].toInt(), SilkTables.silk_delta_gain_iCDF, 8)
		} else {
			/* independent coding, in two stages: MSB bits followed by 3 LSBs */
			Inlines.OpusAssert(psIndices.GainsIndices[0] >= 0 && psIndices.GainsIndices[0] < SilkConstants.N_LEVELS_QGAIN)
			psRangeEnc.enc_icdf(
				Inlines.silk_RSHIFT(psIndices.GainsIndices[0].toInt(), 3),
				SilkTables.silk_gain_iCDF[psIndices.signalType.toInt()],
				8
			)
			psRangeEnc.enc_icdf(psIndices.GainsIndices[0] and 7, SilkTables.silk_uniform8_iCDF, 8)
		}

		/* remaining subframes */
		i = 1
		while (i < psEncC.nb_subfr) {
			Inlines.OpusAssert(psIndices.GainsIndices[i] >= 0 && psIndices.GainsIndices[i] < SilkConstants.MAX_DELTA_GAIN_QUANT - SilkConstants.MIN_DELTA_GAIN_QUANT + 1)
			psRangeEnc.enc_icdf(psIndices.GainsIndices[i].toInt(), SilkTables.silk_delta_gain_iCDF, 8)
			i++
		}

		/**
		 * *************
		 */
		/* Encode NLSFs */
		/**
		 * *************
		 */
		psRangeEnc.enc_icdf(
			psIndices.NLSFIndices[0].toInt(),
			psEncC.psNLSF_CB!!.CB1_iCDF!!,
			(psIndices.signalType shr 1) * psEncC.psNLSF_CB!!.nVectors,
			8
		)
		NLSF.silk_NLSF_unpack(ec_ix, pred_Q8, psEncC.psNLSF_CB!!, psIndices.NLSFIndices[0].toInt())
		Inlines.OpusAssert(psEncC.psNLSF_CB!!.order.toInt() == psEncC.predictLPCOrder)

		i = 0
		while (i < psEncC.psNLSF_CB!!.order) {
			if (psIndices.NLSFIndices[i + 1] >= SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
				psRangeEnc.enc_icdf(
					2 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE,
					psEncC.psNLSF_CB!!.ec_iCDF!!,
					ec_ix[i].toInt(),
					8
				)
				psRangeEnc.enc_icdf(
					psIndices.NLSFIndices[i + 1] - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE,
					SilkTables.silk_NLSF_EXT_iCDF,
					8
				)
			} else if (psIndices.NLSFIndices[i + 1] <= 0 - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
				psRangeEnc.enc_icdf(0, psEncC.psNLSF_CB!!.ec_iCDF!!, ec_ix[i].toInt(), 8)
				psRangeEnc.enc_icdf(
					-psIndices.NLSFIndices[i + 1] - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE,
					SilkTables.silk_NLSF_EXT_iCDF,
					8
				)
			} else {
				psRangeEnc.enc_icdf(
					psIndices.NLSFIndices[i + 1] + SilkConstants.NLSF_QUANT_MAX_AMPLITUDE,
					psEncC.psNLSF_CB!!.ec_iCDF!!,
					ec_ix[i].toInt(),
					8
				)
			}
			i++
		}

		/* Encode NLSF interpolation factor */
		if (psEncC.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
			Inlines.OpusAssert(psIndices.NLSFInterpCoef_Q2 >= 0 && psIndices.NLSFInterpCoef_Q2 < 5)
			psRangeEnc.enc_icdf(psIndices.NLSFInterpCoef_Q2.toInt(), SilkTables.silk_NLSF_interpolation_factor_iCDF, 8)
		}

		if (psIndices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
			/**
			 * ******************
			 */
			/* Encode pitch lags */
			/**
			 * ******************
			 */
			/* lag index */
			encode_absolute_lagIndex = 1
			if (condCoding == SilkConstants.CODE_CONDITIONALLY && psEncC.ec_prevSignalType == SilkConstants.TYPE_VOICED) {
				/* Delta Encoding */
				delta_lagIndex = psIndices.lagIndex - psEncC.ec_prevLagIndex

				if (delta_lagIndex < -8 || delta_lagIndex > 11) {
					delta_lagIndex = 0
				} else {
					delta_lagIndex = delta_lagIndex + 9
					encode_absolute_lagIndex = 0
					/* Only use delta */
				}

				Inlines.OpusAssert(delta_lagIndex >= 0 && delta_lagIndex < 21)
				psRangeEnc.enc_icdf(delta_lagIndex, SilkTables.silk_pitch_delta_iCDF, 8)
			}

			if (encode_absolute_lagIndex != 0) {
				/* Absolute encoding */
				val pitch_high_bits: Int
				val pitch_low_bits: Int
				pitch_high_bits =
						Inlines.silk_DIV32_16(psIndices.lagIndex.toInt(), Inlines.silk_RSHIFT(psEncC.fs_kHz, 1))
				pitch_low_bits = psIndices.lagIndex -
						Inlines.silk_SMULBB(pitch_high_bits, Inlines.silk_RSHIFT(psEncC.fs_kHz, 1))
				Inlines.OpusAssert(pitch_low_bits < psEncC.fs_kHz / 2)
				Inlines.OpusAssert(pitch_high_bits < 32)
				psRangeEnc.enc_icdf(pitch_high_bits, SilkTables.silk_pitch_lag_iCDF, 8)
				psRangeEnc.enc_icdf(pitch_low_bits, psEncC.pitch_lag_low_bits_iCDF!!, 8)
			}
			psEncC.ec_prevLagIndex = psIndices.lagIndex

			/* Countour index */
			Inlines.OpusAssert(psIndices.contourIndex >= 0)
			Inlines.OpusAssert(psIndices.contourIndex < 34 && psEncC.fs_kHz > 8 && psEncC.nb_subfr == 4 || psIndices.contourIndex < 11 && psEncC.fs_kHz == 8 && psEncC.nb_subfr == 4 || psIndices.contourIndex < 12 && psEncC.fs_kHz > 8 && psEncC.nb_subfr == 2 || psIndices.contourIndex < 3 && psEncC.fs_kHz == 8 && psEncC.nb_subfr == 2)
			psRangeEnc.enc_icdf(psIndices.contourIndex.toInt(), psEncC.pitch_contour_iCDF!!, 8)

			/**
			 * *****************
			 */
			/* Encode LTP gains */
			/**
			 * *****************
			 */
			/* PERIndex value */
			Inlines.OpusAssert(psIndices.PERIndex >= 0 && psIndices.PERIndex < 3)
			psRangeEnc.enc_icdf(psIndices.PERIndex.toInt(), SilkTables.silk_LTP_per_index_iCDF, 8)

			/* Codebook Indices */
			k = 0
			while (k < psEncC.nb_subfr) {
				Inlines.OpusAssert(psIndices.LTPIndex[k] >= 0 && psIndices.LTPIndex[k] < 8 shl psIndices.PERIndex.toInt())
				psRangeEnc.enc_icdf(
					psIndices.LTPIndex[k].toInt(),
					SilkTables.silk_LTP_gain_iCDF_ptrs[psIndices.PERIndex.toInt()],
					8
				)
				k++
			}

			/**
			 * *******************
			 */
			/* Encode LTP scaling */
			/**
			 * *******************
			 */
			if (condCoding == SilkConstants.CODE_INDEPENDENTLY) {
				Inlines.OpusAssert(psIndices.LTP_scaleIndex >= 0 && psIndices.LTP_scaleIndex < 3)
				psRangeEnc.enc_icdf(psIndices.LTP_scaleIndex.toInt(), SilkTables.silk_LTPscale_iCDF, 8)
			}

			Inlines.OpusAssert(condCoding == 0 || psIndices.LTP_scaleIndex.toInt() == 0)
		}

		psEncC.ec_prevSignalType = psIndices.signalType.toInt()

		/**
		 * ************
		 */
		/* Encode seed */
		/**
		 * ************
		 */
		Inlines.OpusAssert(psIndices.Seed >= 0 && psIndices.Seed < 4)
		psRangeEnc.enc_icdf(psIndices.Seed.toInt(), SilkTables.silk_uniform4_iCDF, 8)
	}
}
