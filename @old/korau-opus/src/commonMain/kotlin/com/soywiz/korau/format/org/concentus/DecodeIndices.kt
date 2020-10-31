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

internal object DecodeIndices {

	/* Decode side-information parameters from payload */
	fun silk_decode_indices(
		psDec: SilkChannelDecoder, /* I/O  State                                       */
		psRangeDec: EntropyCoder, /* I/O  Compressor data structure                   */
		FrameIndex: Int, /* I    Frame number                                */
		decode_LBRR: Int, /* I    Flag indicating LBRR data is being decoded  */
		condCoding: Int /* I    The type of conditional coding to use       */
	) {
		var i: Int
		var k: Int
		var Ix: Int
		var decode_absolute_lagIndex: Int
		var delta_lagIndex: Int
		val ec_ix = ShortArray(psDec.LPC_order)
		val pred_Q8 = ShortArray(psDec.LPC_order)

		/**
		 * ****************************************
		 */
		/* Decode signal type and quantizer offset */
		/**
		 * ****************************************
		 */
		if (decode_LBRR != 0 || psDec.VAD_flags[FrameIndex] != 0) {
			Ix = psRangeDec.dec_icdf(SilkTables.silk_type_offset_VAD_iCDF, 8) + 2
		} else {
			Ix = psRangeDec.dec_icdf(SilkTables.silk_type_offset_no_VAD_iCDF, 8)
		}
		psDec.indices.signalType = Inlines.silk_RSHIFT(Ix, 1).toByte()
		psDec.indices.quantOffsetType = (Ix and 1).toByte()

		/**
		 * *************
		 */
		/* Decode gains */
		/**
		 * *************
		 */
		/* First subframe */
		if (condCoding == SilkConstants.CODE_CONDITIONALLY) {
			/* Conditional coding */
			psDec.indices.GainsIndices[0] = psRangeDec.dec_icdf(SilkTables.silk_delta_gain_iCDF, 8).toByte()
		} else {
			/* Independent coding, in two stages: MSB bits followed by 3 LSBs */
			psDec.indices.GainsIndices[0] =
					Inlines.silk_LSHIFT(
						psRangeDec.dec_icdf(
							SilkTables.silk_gain_iCDF[psDec.indices.signalType.toInt()],
							8
						), 3
					)
						.toByte()
			psDec.indices.GainsIndices[0] =
					(psDec.indices.GainsIndices[0] + psRangeDec.dec_icdf(
						SilkTables.silk_uniform8_iCDF,
						8
					).toByte()).toByte()
		}

		/* Remaining subframes */
		i = 1
		while (i < psDec.nb_subfr) {
			psDec.indices.GainsIndices[i] = psRangeDec.dec_icdf(SilkTables.silk_delta_gain_iCDF, 8).toByte()
			i++
		}

		/**
		 * *******************
		 */
		/* Decode LSF Indices */
		/**
		 * *******************
		 */
		psDec.indices.NLSFIndices[0] = psRangeDec.dec_icdf(
			psDec.psNLSF_CB!!.CB1_iCDF!!,
			(psDec.indices.signalType shr 1) * psDec.psNLSF_CB!!.nVectors,
			8
		).toByte()
		NLSF.silk_NLSF_unpack(ec_ix, pred_Q8, psDec.psNLSF_CB!!, psDec.indices.NLSFIndices[0].toInt())
		Inlines.OpusAssert(psDec.psNLSF_CB!!.order.toInt() == psDec.LPC_order)
		i = 0
		while (i < psDec.psNLSF_CB!!.order) {
			Ix = psRangeDec.dec_icdf(psDec.psNLSF_CB!!.ec_iCDF!!, ec_ix[i].toInt(), 8)
			if (Ix == 0) {
				Ix -= psRangeDec.dec_icdf(SilkTables.silk_NLSF_EXT_iCDF, 8)
			} else if (Ix == 2 * SilkConstants.NLSF_QUANT_MAX_AMPLITUDE) {
				Ix += psRangeDec.dec_icdf(SilkTables.silk_NLSF_EXT_iCDF, 8)
			}
			psDec.indices.NLSFIndices[i + 1] = (Ix - SilkConstants.NLSF_QUANT_MAX_AMPLITUDE).toByte()
			i++
		}

		/* Decode LSF interpolation factor */
		if (psDec.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
			psDec.indices.NLSFInterpCoef_Q2 =
					psRangeDec.dec_icdf(SilkTables.silk_NLSF_interpolation_factor_iCDF, 8).toByte()
		} else {
			psDec.indices.NLSFInterpCoef_Q2 = 4
		}

		if (psDec.indices.signalType.toInt() == SilkConstants.TYPE_VOICED) {
			/**
			 * ******************
			 */
			/* Decode pitch lags */
			/**
			 * ******************
			 */
			/* Get lag index */
			decode_absolute_lagIndex = 1
			if (condCoding == SilkConstants.CODE_CONDITIONALLY && psDec.ec_prevSignalType == SilkConstants.TYPE_VOICED) {
				/* Decode Delta index */
				delta_lagIndex = psRangeDec.dec_icdf(SilkTables.silk_pitch_delta_iCDF, 8).toShort().toInt()
				if (delta_lagIndex > 0) {
					delta_lagIndex = delta_lagIndex - 9
					psDec.indices.lagIndex = (psDec.ec_prevLagIndex + delta_lagIndex).toShort()
					decode_absolute_lagIndex = 0
				}
			}
			if (decode_absolute_lagIndex != 0) {
				/* Absolute decoding */
				psDec.indices.lagIndex = (psRangeDec.dec_icdf(SilkTables.silk_pitch_lag_iCDF, 8) * Inlines.silk_RSHIFT(
					psDec.fs_kHz,
					1
				)).toShort()
				psDec.indices.lagIndex = (psDec.indices.lagIndex + psRangeDec.dec_icdf(
					psDec.pitch_lag_low_bits_iCDF!!,
					8
				).toShort()).toShort()
			}
			psDec.ec_prevLagIndex = psDec.indices.lagIndex

			/* Get countour index */
			psDec.indices.contourIndex = psRangeDec.dec_icdf(psDec.pitch_contour_iCDF!!, 8).toByte()

			/**
			 * *****************
			 */
			/* Decode LTP gains */
			/**
			 * *****************
			 */
			/* Decode PERIndex value */
			psDec.indices.PERIndex = psRangeDec.dec_icdf(SilkTables.silk_LTP_per_index_iCDF, 8).toByte()

			k = 0
			while (k < psDec.nb_subfr) {
				psDec.indices.LTPIndex[k] =
						psRangeDec.dec_icdf(SilkTables.silk_LTP_gain_iCDF_ptrs[psDec.indices.PERIndex.toInt()], 8)
							.toByte()
				k++
			}

			/**
			 * *******************
			 */
			/* Decode LTP scaling */
			/**
			 * *******************
			 */
			if (condCoding == SilkConstants.CODE_INDEPENDENTLY) {
				psDec.indices.LTP_scaleIndex = psRangeDec.dec_icdf(SilkTables.silk_LTPscale_iCDF, 8).toByte()
			} else {
				psDec.indices.LTP_scaleIndex = 0
			}
		}
		psDec.ec_prevSignalType = psDec.indices.signalType.toInt()

		/**
		 * ************
		 */
		/* Decode seed */
		/**
		 * ************
		 */
		psDec.indices.Seed = psRangeDec.dec_icdf(SilkTables.silk_uniform4_iCDF, 8).toByte()
	}
}
