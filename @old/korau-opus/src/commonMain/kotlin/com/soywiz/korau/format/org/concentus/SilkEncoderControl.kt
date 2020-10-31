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

/**
 * *********************
 */
/* Encoder control FIX  */
/**
 * *********************
 */
internal class SilkEncoderControl {

    /* Prediction and coding parameters */
    val Gains_Q16 = IntArray(SilkConstants.MAX_NB_SUBFR)
    val PredCoef_Q12 = Arrays.InitTwoDimensionalArrayShort(2, SilkConstants.MAX_LPC_ORDER)
    /* holds interpolated and final coefficients */
    val LTPCoef_Q14 = ShortArray(SilkConstants.LTP_ORDER * SilkConstants.MAX_NB_SUBFR)
    var LTP_scale_Q14 = 0
    val pitchL = IntArray(SilkConstants.MAX_NB_SUBFR)

    /* Noise shaping parameters */
    val AR1_Q13 = ShortArray(SilkConstants.MAX_NB_SUBFR * SilkConstants.MAX_SHAPE_LPC_ORDER)
    val AR2_Q13 = ShortArray(SilkConstants.MAX_NB_SUBFR * SilkConstants.MAX_SHAPE_LPC_ORDER)
    val LF_shp_Q14 = IntArray(SilkConstants.MAX_NB_SUBFR)
    /* Packs two int16 coefficients per int32 value         */
    val GainsPre_Q14 = IntArray(SilkConstants.MAX_NB_SUBFR)
    val HarmBoost_Q14 = IntArray(SilkConstants.MAX_NB_SUBFR)
    val Tilt_Q14 = IntArray(SilkConstants.MAX_NB_SUBFR)
    val HarmShapeGain_Q14 = IntArray(SilkConstants.MAX_NB_SUBFR)
    var Lambda_Q10 = 0
    var input_quality_Q14 = 0
    var coding_quality_Q14 = 0

    /* Measures */
    var sparseness_Q8 = 0
    var predGain_Q16 = 0
    var LTPredCodGain_Q7 = 0

    /* Residual energy per subframe */
    val ResNrg = IntArray(SilkConstants.MAX_NB_SUBFR)

    /* Q domain for the residual energy > 0                 */
    val ResNrgQ = IntArray(SilkConstants.MAX_NB_SUBFR)

    /* Parameters for CBR mode */
    val GainsUnq_Q16 = IntArray(SilkConstants.MAX_NB_SUBFR)
    var lastGainIndexPrev: Byte = 0

    fun Reset() {
        Arrays.MemSet(Gains_Q16, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(PredCoef_Q12[0], 0.toShort(), SilkConstants.MAX_LPC_ORDER)
        Arrays.MemSet(PredCoef_Q12[1], 0.toShort(), SilkConstants.MAX_LPC_ORDER)
        Arrays.MemSet(LTPCoef_Q14, 0.toShort(), SilkConstants.LTP_ORDER * SilkConstants.MAX_NB_SUBFR)
        LTP_scale_Q14 = 0
        Arrays.MemSet(pitchL, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(AR1_Q13, 0.toShort(), SilkConstants.MAX_NB_SUBFR * SilkConstants.MAX_SHAPE_LPC_ORDER)
        Arrays.MemSet(AR2_Q13, 0.toShort(), SilkConstants.MAX_NB_SUBFR * SilkConstants.MAX_SHAPE_LPC_ORDER)
        Arrays.MemSet(LF_shp_Q14, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(GainsPre_Q14, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(HarmBoost_Q14, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(Tilt_Q14, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(HarmShapeGain_Q14, 0, SilkConstants.MAX_NB_SUBFR)
        Lambda_Q10 = 0
        input_quality_Q14 = 0
        coding_quality_Q14 = 0
        sparseness_Q8 = 0
        predGain_Q16 = 0
        LTPredCodGain_Q7 = 0
        Arrays.MemSet(ResNrg, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(ResNrgQ, 0, SilkConstants.MAX_NB_SUBFR)
        Arrays.MemSet(GainsUnq_Q16, 0, SilkConstants.MAX_NB_SUBFR)
        lastGainIndexPrev = 0
    }
}
