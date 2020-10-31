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

internal class TonalityAnalysisState {

    var enabled = false
    val angle = FloatArray(240)
    val d_angle = FloatArray(240)
    val d2_angle = FloatArray(240)
    val inmem = IntArray(OpusConstants.ANALYSIS_BUF_SIZE)
    var mem_fill: Int = 0
    /* number of usable samples in the buffer */
    val prev_band_tonality = FloatArray(OpusConstants.NB_TBANDS)
    var prev_tonality: Float = 0.toFloat()
    val E = Arrays.InitTwoDimensionalArrayFloat(OpusConstants.NB_FRAMES, OpusConstants.NB_TBANDS)
    val lowE = FloatArray(OpusConstants.NB_TBANDS)
    val highE = FloatArray(OpusConstants.NB_TBANDS)
    val meanE = FloatArray(OpusConstants.NB_TOT_BANDS)
    val mem = FloatArray(32)
    val cmean = FloatArray(8)
    val std = FloatArray(9)
    var music_prob: Float = 0.toFloat()
    var Etracker: Float = 0.toFloat()
    var lowECount: Float = 0.toFloat()
    var E_count: Int = 0
    var last_music: Int = 0
    var last_transition: Int = 0
    var count: Int = 0
    val subframe_mem = FloatArray(3)
    var analysis_offset: Int = 0
    /**
     * Probability of having speech for time i to DETECT_SIZE-1 (and music
     * before). pspeech[0] is the probability that all frames in the window are
     * speech.
     */
    val pspeech = FloatArray(OpusConstants.DETECT_SIZE)
    /**
     * Probability of having music for time i to DETECT_SIZE-1 (and speech
     * before). pmusic[0] is the probability that all frames in the window are
     * music.
     */
    val pmusic = FloatArray(OpusConstants.DETECT_SIZE)
    var speech_confidence: Float = 0.toFloat()
    var music_confidence: Float = 0.toFloat()
    var speech_confidence_count: Int = 0
    var music_confidence_count: Int = 0
    var write_pos: Int = 0
    var read_pos: Int = 0
    var read_subframe: Int = 0
    val info = arrayOfNulls<AnalysisInfo>(OpusConstants.DETECT_SIZE)

    init {
        for (c in 0 until OpusConstants.DETECT_SIZE) {
            info[c] = AnalysisInfo()
        }
    }

    fun Reset() {
        Arrays.MemSet(angle, 0f, 240)
        Arrays.MemSet(d_angle, 0f, 240)
        Arrays.MemSet(d2_angle, 0f, 240)
        Arrays.MemSet(inmem, 0, OpusConstants.ANALYSIS_BUF_SIZE)
        mem_fill = 0
        Arrays.MemSet(prev_band_tonality, 0f, OpusConstants.NB_TBANDS)
        prev_tonality = 0f
        for (c in 0 until OpusConstants.NB_FRAMES) {
            Arrays.MemSet(E[c], 0f, OpusConstants.NB_TBANDS)
        }
        Arrays.MemSet(lowE, 0f, OpusConstants.NB_TBANDS)
        Arrays.MemSet(highE, 0f, OpusConstants.NB_TBANDS)
        Arrays.MemSet(meanE, 0f, OpusConstants.NB_TOT_BANDS)
        Arrays.MemSet(mem, 0f, 32)
        Arrays.MemSet(cmean, 0f, 8)
        Arrays.MemSet(std, 0f, 9)
        music_prob = 0f
        Etracker = 0f
        lowECount = 0f
        E_count = 0
        last_music = 0
        last_transition = 0
        count = 0
        Arrays.MemSet(subframe_mem, 0f, 3)
        analysis_offset = 0
        Arrays.MemSet(pspeech, 0f, OpusConstants.DETECT_SIZE)
        Arrays.MemSet(pmusic, 0f, OpusConstants.DETECT_SIZE)
        speech_confidence = 0f
        music_confidence = 0f
        speech_confidence_count = 0
        music_confidence_count = 0
        write_pos = 0
        read_pos = 0
        read_subframe = 0
        for (c in 0 until OpusConstants.DETECT_SIZE) {
            info[c]!!.Reset()
        }
    }
}
