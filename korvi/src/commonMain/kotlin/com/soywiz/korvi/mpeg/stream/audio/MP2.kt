package com.soywiz.korvi.mpeg.stream.audio

import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.arraycopy
import com.soywiz.korvi.mpeg.stream.AudioDestination
import com.soywiz.korvi.mpeg.util.BitBuffer
import com.soywiz.korvi.mpeg.stream.DecoderBase
import com.soywiz.korvi.mpeg.JSMpeg
import com.soywiz.korvi.mpeg.length

class MP2(
    streaming: Boolean,
    val bufferSize: Int = 128 * 1024,
    val onDecodeCallback: ((mp2: MP2, elapsedTime: Double) -> Unit)? = null,
) : DecoderBase<AudioDestination>(streaming) {
    // Based on kjmp2 by Martin J. Fiedler
    // http://keyj.emphy.de/kjmp2/

    var bufferMode = if (streaming) BitBuffer.MODE.EVICT else BitBuffer.MODE.EXPAND

    override val bits = BitBuffer(bufferSize, bufferMode)

    val left = FloatArray(1152)
    val right = FloatArray(1152)
    var sampleRate = 44100

    val D = DoubleArray(1024).also {
        arraycopy(SYNTHESIS_WINDOW, 0, it, 0, SYNTHESIS_WINDOW.size)
        arraycopy(SYNTHESIS_WINDOW, 0, it, 512, SYNTHESIS_WINDOW.size)
    }
    val V = arrayOf(DoubleArray(1024), DoubleArray(1024))
    val U = IntArray(32)
    var VPos = 0
    val allocation = Array(2) { arrayOfNulls<Quant>(32) }
    val scaleFactorInfo = Array(2) { Uint8Buffer(32) }
    val scaleFactor = Array(2) { Array(32) { IntArray(3) } }
    val sample = Array(2) { Array(32) { DoubleArray(3) } }

    override fun decode(): Boolean {
        val startTime = JSMpeg.Now()

        val pos = this.bits.index shr 3
        if (pos >= this.bits.byteLength) {
            return false
        }

        val decoded = this.decodeFrame(this.left, this.right)
        this.bits.index = (pos + decoded) shl 3
        if (decoded == 0) {
            return false
        }

        this.destination?.play(this.sampleRate, this.left, this.right)

        this.advanceDecodedTime(this.left.length / this.sampleRate.toDouble())

        val elapsedTime = JSMpeg.Now() - startTime
        this.onDecodeCallback?.invoke(this, elapsedTime)
        return true
    }

    override val currentTime: Double get() {
        val enqueuedTime = this.destination?.enqueuedTime ?: 0.0
        return this.decodedTime - enqueuedTime
    }

    fun decodeFrame(left: FloatArray, right: FloatArray): Int {
        // Check for valid header: syncword OK, MPEG-Audio Layer 2
        val sync = this.bits.read(11)
        val version = this.bits.read(2)
        val layer = this.bits.read(2)
        val hasCRC = this.bits.read(1) == 0

        if (
            sync != FRAME_SYNC ||
            version != VERSION.MPEG_1 ||
            layer != LAYER.II
        ) {
            return 0 // Invalid header or unsupported version
        }

        var bitrateIndex = this.bits.read(4) - 1
        if (bitrateIndex > 13) {
            return 0  // Invalid bit rate or 'free format'
        }

        var sampleRateIndex = this.bits.read(2)
        if (sampleRateIndex == 3) {
            return 0 // Invalid sample rate
        }
        if (version == VERSION.MPEG_2) {
            sampleRateIndex += 4
            bitrateIndex += 14
        }
        val padding = this.bits.read(1)
        var privat = this.bits.read(1)
        val mode = this.bits.read(2)

        // Parse the mode_extension, set up the stereo bound
        var bound = when (mode) {
            MODE.JOINT_STEREO -> (this.bits.read(2) + 1) shl 2
            else -> {
                this.bits.skip(2)
                if (mode == MODE.MONO) 0 else 32
            }
        }

        // Discard the last 4 bits of the header and the CRC value, if present
        this.bits.skip(4)
        if (hasCRC) {
            this.bits.skip(16)
        }

        // Compute the frame size
        val bitrate = BIT_RATE[bitrateIndex]
        val sampleRate = SAMPLE_RATE[sampleRateIndex]
        val frameSize = ((144000 * bitrate / sampleRate) + padding).toInt()

        // Prepare the quantizer table lookups
        var tab3 = 0
        var sblimit = 0
        if (version == VERSION.MPEG_2) {
            // MPEG-2 (LSR)
            tab3 = 2
            sblimit = 30
        } else {
            // MPEG-1
            val tab1 = if (mode == MODE.MONO) 0 else 1
            val tab2 = QUANT_LUT_STEP_1[tab1][bitrateIndex]
            tab3 = QUANT_LUT_STEP_2[tab2][sampleRateIndex]
            sblimit = tab3 and 63
            tab3 = tab3 shr 6
        }

        if (bound > sblimit) {
            bound = sblimit
        }

        // Read the allocation information
        for (sb in 0 until bound) {
            this.allocation[0][sb] = this.readAllocation(sb, tab3)
            this.allocation[1][sb] = this.readAllocation(sb, tab3)
        }

        for (sb in bound until sblimit) {
            val v = this.readAllocation(sb, tab3)
            this.allocation[0][sb] = v
            this.allocation[1][sb] = v
        }

        // Read scale factor selector information
        val channels = if (mode == MODE.MONO) 1 else 2
        for (sb in 0 until sblimit) {
            for (ch in 0 until channels) {
                if (this.allocation[ch][sb] != null) {
                    this.scaleFactorInfo[ch][sb] = this.bits.read(2)
                }
            }
            if (mode == MODE.MONO) {
                this.scaleFactorInfo[1][sb] = this.scaleFactorInfo[0][sb]
            }
        }

        // Read scale factors
        for (sb in 0 until sblimit) {
            for (ch in 0 until channels) {
                if (this.allocation[ch][sb] != null) {
                    val sf = this.scaleFactor[ch][sb]
                    when (this.scaleFactorInfo[ch][sb]) {
                        0 -> {
                            sf[0] = this.bits.read(6)
                            sf[1] = this.bits.read(6)
                            sf[2] = this.bits.read(6)
                        }

                        1 -> {
                            val v = this.bits.read(6)
                            sf[0] = v
                            sf[1] = v
                            sf[2] = this.bits.read(6)
                        }

                        2 -> {
                            val v = this.bits.read(6)
                            sf[0] = v
                            sf[1] = v
                            sf[2] = v
                        }
                        3 -> {
                            sf[0] = this.bits.read(6)
                            val v = this.bits.read(6)
                            sf[1] = v
                            sf[2] = v
                        }
                    }
                }
            }
            if (mode == MODE.MONO) {
                this.scaleFactor[1][sb][0] = this.scaleFactor[0][sb][0]
                this.scaleFactor[1][sb][1] = this.scaleFactor[0][sb][1]
                this.scaleFactor[1][sb][2] = this.scaleFactor[0][sb][2]
            }
        }

        // Coefficient input and reconstruction
        var outPos = 0
        for (part in 0 until 3) {
            for (granule in 0 until 4) {

                // Read the samples
                for (sb in 0 until bound) {
                    this.readSamples(0, sb, part)
                    this.readSamples(1, sb, part)
                }
                for (sb in bound until sblimit) {
                    this.readSamples(0, sb, part)
                    this.sample[1][sb][0] = this.sample[0][sb][0]
                    this.sample[1][sb][1] = this.sample[0][sb][1]
                    this.sample[1][sb][2] = this.sample[0][sb][2]
                }
                for (sb in sblimit until 32) {
                    this.sample[0][sb][0] = 0.0
                    this.sample[0][sb][1] = 0.0
                    this.sample[0][sb][2] = 0.0
                    this.sample[1][sb][0] = 0.0
                    this.sample[1][sb][1] = 0.0
                    this.sample[1][sb][2] = 0.0
                }

                // Synthesis loop
                for (p in 0 until 3) {
                    // Shifting step
                    this.VPos = (this.VPos - 64) and 1023

                    for (ch in 0 until 2) {
                        MatrixTransform(this.sample[ch], p, this.V[ch], this.VPos)

                        // Build U, windowing, calculate output
                        this.U.fill(0)

                        var dIndex = 512 - (this.VPos shr 1)
                        var vIndex = (this.VPos % 128) shr 1
                        while (vIndex < 1024) {
                            for (i in 0 until 32) {
                                this.U[i] += (this.D[dIndex++] * this.V[ch][vIndex++]).toInt()
                            }

                            vIndex += 128 - 32
                            dIndex += 64 - 32
                        }

                        vIndex = (128 - 32 + 1024) - vIndex
                        dIndex -= (512 - 32)
                        while (vIndex < 1024) {
                            for (i in 0 until 32) {
                                this.U[i] += (this.D[dIndex++] * this.V[ch][vIndex++]).toInt()
                            }

                            vIndex += 128 - 32
                            dIndex += 64 - 32
                        }

                        // Output samples
                        val outChannel = if (ch == 0) left else right

                        for (j in 0 until 32) {
                            outChannel[outPos + j] = (this.U[j] / 2147418112.0).toFloat()
                        }
                    } // End of synthesis channel loop
                    outPos += 32
                } // End of synthesis sub-block loop
            } // Decoding of the granule finished
        }

        this.sampleRate = sampleRate
        return frameSize
    }

    fun readAllocation(sb: Int, tab3: Int): Quant? {
        val tab4 = QUANT_LUT_STEP_3[tab3][sb]
        val qtab = QUANT_LUT_STEP4[tab4 and 15][this.bits.read(tab4 shr 4)]
        return if (qtab != 0) (QUANT_TAB_TAB[qtab - 1]) else null
    }

    fun readSamples(ch: Int, sb: Int, part: Int) {
        val q = this.allocation[ch][sb]
        var sf = this.scaleFactor[ch][sb][part]
        val sample = this.sample[ch][sb]
        var `val` = 0

        if (q == null) {
            // No bits allocated for this subband
            sample[0] = 0.0
            sample[1] = 0.0
            sample[2] = 0.0
            return
        }

        // Resolve scalefactor
        sf = when (sf) {
            63 -> 0
            else -> {
                val shift = (sf / 3)
                (SCALEFACTOR_BASE[sf % 3] + ((1 shl shift) shr 1)) shr shift
            }
        }

        // Decode samples
        var adj = q.levels
        if (q.group != 0) {
            // Decode grouped samples
            `val` = this.bits.read(q.bits)
            sample[0] = (`val` % adj).toDouble()
            `val` = (`val` / adj.toDouble()).toInt()
            sample[1] = (`val` % adj).toDouble()
            sample[2] = (`val` / adj.toDouble()).toInt().toDouble()
        } else {
            // Decode direct samples
            sample[0] = this.bits.read(q.bits).toDouble()
            sample[1] = this.bits.read(q.bits).toDouble()
            sample[2] = this.bits.read(q.bits).toDouble()
        }

        // Postmultiply samples
        val scale = (65536.0 / (adj + 1)).toInt()
        adj = ((adj + 1) shr 1) - 1

        `val` = ((adj - sample[0]) * scale).toInt()
        sample[0] = ((`val` * (sf shr 12) + ((`val` * (sf and 4095) + 2048) shr 12)) shr 12).toDouble()

        `val` = ((adj - sample[1]) * scale).toInt()
        sample[1] = ((`val` * (sf shr 12) + ((`val` * (sf and 4095) + 2048) shr 12)) shr 12).toDouble()

        `val` = ((adj - sample[2]) * scale).toInt()
        sample[2] = ((`val` * (sf shr 12) + ((`val` * (sf and 4095) + 2048) shr 12)) shr 12).toDouble()
    }

    companion object {
        fun MatrixTransform(s: Array<DoubleArray>, ss: Int, d: DoubleArray, dp: Int) {
            var t01 = s[0][ss] + s[31][ss]
            var t02 = (s[0][ss] - s[31][ss]) * 0.500602998235
            var t03 = s[1][ss] + s[30][ss]
            var t04 = (s[1][ss] - s[30][ss]) * 0.505470959898
            var t05 = s[2][ss] + s[29][ss]
            var t06 = (s[2][ss] - s[29][ss]) * 0.515447309923
            var t07 = s[3][ss] + s[28][ss]
            var t08 = (s[3][ss] - s[28][ss]) * 0.53104259109
            var t09 = s[4][ss] + s[27][ss]
            var t10 = (s[4][ss] - s[27][ss]) * 0.553103896034
            var t11 = s[5][ss] + s[26][ss]
            var t12 = (s[5][ss] - s[26][ss]) * 0.582934968206
            var t13 = s[6][ss] + s[25][ss]
            var t14 = (s[6][ss] - s[25][ss]) * 0.622504123036
            var t15 = s[7][ss] + s[24][ss]
            var t16 = (s[7][ss] - s[24][ss]) * 0.674808341455
            var t17 = s[8][ss] + s[23][ss]
            var t18 = (s[8][ss] - s[23][ss]) * 0.744536271002
            var t19 = s[9][ss] + s[22][ss]
            var t20 = (s[9][ss] - s[22][ss]) * 0.839349645416
            var t21 = s[10][ss] + s[21][ss]
            var t22 = (s[10][ss] - s[21][ss]) * 0.972568237862
            var t23 = s[11][ss] + s[20][ss]
            var t24 = (s[11][ss] - s[20][ss]) * 1.16943993343
            var t25 = s[12][ss] + s[19][ss]
            var t26 = (s[12][ss] - s[19][ss]) * 1.48416461631
            var t27 = s[13][ss] + s[18][ss]
            var t28 = (s[13][ss] - s[18][ss]) * 2.05778100995
            var t29 = s[14][ss] + s[17][ss]
            var t30 = (s[14][ss] - s[17][ss]) * 3.40760841847
            var t31 = s[15][ss] + s[16][ss]
            var t32 = (s[15][ss] - s[16][ss]) * 10.1900081235

            var t33 = t01 + t31; t31 = (t01 - t31) * 0.502419286188
            t01 = t03 + t29; t29 = (t03 - t29) * 0.52249861494
            t03 = t05 + t27; t27 = (t05 - t27) * 0.566944034816
            t05 = t07 + t25; t25 = (t07 - t25) * 0.64682178336
            t07 = t09 + t23; t23 = (t09 - t23) * 0.788154623451
            t09 = t11 + t21; t21 = (t11 - t21) * 1.06067768599
            t11 = t13 + t19; t19 = (t13 - t19) * 1.72244709824
            t13 = t15 + t17; t17 = (t15 - t17) * 5.10114861869
            t15 = t33 + t13; t13 = (t33 - t13) * 0.509795579104
            t33 = t01 + t11; t01 = (t01 - t11) * 0.601344886935
            t11 = t03 + t09; t09 = (t03 - t09) * 0.899976223136
            t03 = t05 + t07; t07 = (t05 - t07) * 2.56291544774
            t05 = t15 + t03; t15 = (t15 - t03) * 0.541196100146
            t03 = t33 + t11; t11 = (t33 - t11) * 1.30656296488
            t33 = t05 + t03; t05 = (t05 - t03) * 0.707106781187
            t03 = t15 + t11; t15 = (t15 - t11) * 0.707106781187
            t03 += t15
            t11 = t13 + t07; t13 = (t13 - t07) * 0.541196100146
            t07 = t01 + t09; t09 = (t01 - t09) * 1.30656296488
            t01 = t11 + t07; t07 = (t11 - t07) * 0.707106781187
            t11 = t13 + t09; t13 = (t13 - t09) * 0.707106781187
            t11 += t13; t01 += t11
            t11 += t07; t07 += t13
            t09 = t31 + t17; t31 = (t31 - t17) * 0.509795579104
            t17 = t29 + t19; t29 = (t29 - t19) * 0.601344886935
            t19 = t27 + t21; t21 = (t27 - t21) * 0.899976223136
            t27 = t25 + t23; t23 = (t25 - t23) * 2.56291544774
            t25 = t09 + t27; t09 = (t09 - t27) * 0.541196100146
            t27 = t17 + t19; t19 = (t17 - t19) * 1.30656296488
            t17 = t25 + t27; t27 = (t25 - t27) * 0.707106781187
            t25 = t09 + t19; t19 = (t09 - t19) * 0.707106781187
            t25 += t19
            t09 = t31 + t23; t31 = (t31 - t23) * 0.541196100146
            t23 = t29 + t21; t21 = (t29 - t21) * 1.30656296488
            t29 = t09 + t23; t23 = (t09 - t23) * 0.707106781187
            t09 = t31 + t21; t31 = (t31 - t21) * 0.707106781187
            t09 += t31; t29 += t09; t09 += t23; t23 += t31
            t17 += t29; t29 += t25; t25 += t09; t09 += t27
            t27 += t23; t23 += t19; t19 += t31
            t21 = t02 + t32; t02 = (t02 - t32) * 0.502419286188
            t32 = t04 + t30; t04 = (t04 - t30) * 0.52249861494
            t30 = t06 + t28; t28 = (t06 - t28) * 0.566944034816
            t06 = t08 + t26; t08 = (t08 - t26) * 0.64682178336
            t26 = t10 + t24; t10 = (t10 - t24) * 0.788154623451
            t24 = t12 + t22; t22 = (t12 - t22) * 1.06067768599
            t12 = t14 + t20; t20 = (t14 - t20) * 1.72244709824
            t14 = t16 + t18; t16 = (t16 - t18) * 5.10114861869
            t18 = t21 + t14; t14 = (t21 - t14) * 0.509795579104
            t21 = t32 + t12; t32 = (t32 - t12) * 0.601344886935
            t12 = t30 + t24; t24 = (t30 - t24) * 0.899976223136
            t30 = t06 + t26; t26 = (t06 - t26) * 2.56291544774
            t06 = t18 + t30; t18 = (t18 - t30) * 0.541196100146
            t30 = t21 + t12; t12 = (t21 - t12) * 1.30656296488
            t21 = t06 + t30; t30 = (t06 - t30) * 0.707106781187
            t06 = t18 + t12; t12 = (t18 - t12) * 0.707106781187
            t06 += t12
            t18 = t14 + t26; t26 = (t14 - t26) * 0.541196100146
            t14 = t32 + t24; t24 = (t32 - t24) * 1.30656296488
            t32 = t18 + t14; t14 = (t18 - t14) * 0.707106781187
            t18 = t26 + t24; t24 = (t26 - t24) * 0.707106781187
            t18 += t24; t32 += t18
            t18 += t14; t26 = t14 + t24
            t14 = t02 + t16; t02 = (t02 - t16) * 0.509795579104
            t16 = t04 + t20; t04 = (t04 - t20) * 0.601344886935
            t20 = t28 + t22; t22 = (t28 - t22) * 0.899976223136
            t28 = t08 + t10; t10 = (t08 - t10) * 2.56291544774
            t08 = t14 + t28; t14 = (t14 - t28) * 0.541196100146
            t28 = t16 + t20; t20 = (t16 - t20) * 1.30656296488
            t16 = t08 + t28; t28 = (t08 - t28) * 0.707106781187
            t08 = t14 + t20; t20 = (t14 - t20) * 0.707106781187
            t08 += t20
            t14 = t02 + t10; t02 = (t02 - t10) * 0.541196100146
            t10 = t04 + t22; t22 = (t04 - t22) * 1.30656296488
            t04 = t14 + t10; t10 = (t14 - t10) * 0.707106781187
            t14 = t02 + t22; t02 = (t02 - t22) * 0.707106781187
            t14 += t02; t04 += t14; t14 += t10; t10 += t02
            t16 += t04; t04 += t08; t08 += t14; t14 += t28
            t28 += t10; t10 += t20; t20 += t02; t21 += t16
            t16 += t32; t32 += t04; t04 += t06; t06 += t08
            t08 += t18; t18 += t14; t14 += t30; t30 += t28
            t28 += t26; t26 += t10; t10 += t12; t12 += t20
            t20 += t24; t24 += t02

            d[dp + 48] = -t33
            d[dp + 49] = -t21; d[dp + 47] = -t21
            d[dp + 50] = -t17; d[dp + 46] = -t17
            d[dp + 51] = -t16; d[dp + 45] = -t16
            d[dp + 52] = -t01; d[dp + 44] = -t01
            d[dp + 53] = -t32; d[dp + 43] = -t32
            d[dp + 54] = -t29; d[dp + 42] = -t29
            d[dp + 55] = -t04; d[dp + 41] = -t04
            d[dp + 56] = -t03; d[dp + 40] = -t03
            d[dp + 57] = -t06; d[dp + 39] = -t06
            d[dp + 58] = -t25; d[dp + 38] = -t25
            d[dp + 59] = -t08; d[dp + 37] = -t08
            d[dp + 60] = -t11; d[dp + 36] = -t11
            d[dp + 61] = -t18; d[dp + 35] = -t18
            d[dp + 62] = -t09; d[dp + 34] = -t09
            d[dp + 63] = -t14; d[dp + 33] = -t14
            d[dp + 32] = -t05
            d[dp + 0] = t05; d[dp + 31] = -t30
            d[dp + 1] = t30; d[dp + 30] = -t27
            d[dp + 2] = t27; d[dp + 29] = -t28
            d[dp + 3] = t28; d[dp + 28] = -t07
            d[dp + 4] = t07; d[dp + 27] = -t26
            d[dp + 5] = t26; d[dp + 26] = -t23
            d[dp + 6] = t23; d[dp + 25] = -t10
            d[dp + 7] = t10; d[dp + 24] = -t15
            d[dp + 8] = t15; d[dp + 23] = -t12
            d[dp + 9] = t12; d[dp + 22] = -t19
            d[dp + 10] = t19; d[dp + 21] = -t20
            d[dp + 11] = t20; d[dp + 20] = -t13
            d[dp + 12] = t13; d[dp + 19] = -t24
            d[dp + 13] = t24; d[dp + 18] = -t31
            d[dp + 14] = t31; d[dp + 17] = -t02
            d[dp + 15] = t02; d[dp + 16] = 0.0
        }

        val FRAME_SYNC = 0x7ff

        object VERSION {
            const val MPEG_2_5 = 0x0
            const val MPEG_2 = 0x2
            const val MPEG_1 = 0x3
        }

        object LAYER {
            const val III = 0x1
            const val II = 0x2
            const val I = 0x3
        }

        object MODE {
            const val STEREO = 0x0
            const val JOINT_STEREO = 0x1
            const val DUAL_CHANNEL = 0x2
            const val MONO = 0x3
        }

        val SAMPLE_RATE = intArrayOf(
            44100, 48000, 32000, 0, // MPEG-1
            22050, 24000, 16000, 0  // MPEG-2
        )

        val BIT_RATE = intArrayOf(
            32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, // MPEG-1
            8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160  // MPEG-2
        )

        val SCALEFACTOR_BASE = intArrayOf(
            0x02000000, 0x01965FEA, 0x01428A30
        )

        val SYNTHESIS_WINDOW = doubleArrayOf(
            0.0, -0.5, -0.5, -0.5, -0.5, -0.5,
            -0.5, -1.0, -1.0, -1.0, -1.0, -1.5,
            -1.5, -2.0, -2.0, -2.5, -2.5, -3.0,
            -3.5, -3.5, -4.0, -4.5, -5.0, -5.5,
            -6.5, -7.0, -8.0, -8.5, -9.5, -10.5,
            -12.0, -13.0, -14.5, -15.5, -17.5, -19.0,
            -20.5, -22.5, -24.5, -26.5, -29.0, -31.5,
            -34.0, -36.5, -39.5, -42.5, -45.5, -48.5,
            -52.0, -55.5, -58.5, -62.5, -66.0, -69.5,
            -73.5, -77.0, -80.5, -84.5, -88.0, -91.5,
            -95.0, -98.0, -101.0, -104.0, 106.5, 109.0,
            111.0, 112.5, 113.5, 114.0, 114.0, 113.5,
            112.0, 110.5, 107.5, 104.0, 100.0, 94.5,
            88.5, 81.5, 73.0, 63.5, 53.0, 41.5,
            28.5, 14.5, -1.0, -18.0, -36.0, -55.5,
            -76.5, -98.5, -122.0, -147.0, -173.5, -200.5,
            -229.5, -259.5, -290.5, -322.5, -355.5, -389.5,
            -424.0, -459.5, -495.5, -532.0, -568.5, -605.0,
            -641.5, -678.0, -714.0, -749.0, -783.5, -817.0,
            -849.0, -879.5, -908.5, -935.0, -959.5, -981.0,
            -1000.5, -1016.0, -1028.5, -1037.5, -1042.5, -1043.5,
            -1040.0, -1031.5, 1018.5, 1000.0, 976.0, 946.5,
            911.0, 869.5, 822.0, 767.5, 707.0, 640.0,
            565.5, 485.0, 397.0, 302.5, 201.0, 92.5,
            -22.5, -144.0, -272.5, -407.0, -547.5, -694.0,
            -846.0, -1003.0, -1165.0, -1331.5, -1502.0, -1675.5,
            -1852.5, -2031.5, -2212.5, -2394.0, -2576.5, -2758.5,
            -2939.5, -3118.5, -3294.5, -3467.5, -3635.5, -3798.5,
            -3955.0, -4104.5, -4245.5, -4377.5, -4499.0, -4609.5,
            -4708.0, -4792.5, -4863.5, -4919.0, -4958.0, -4979.5,
            -4983.0, -4967.5, -4931.5, -4875.0, -4796.0, -4694.5,
            -4569.5, -4420.0, -4246.0, -4046.0, -3820.0, -3567.0,
            3287.0, 2979.5, 2644.0, 2280.5, 1888.0, 1467.5,
            1018.5, 541.0, 35.0, -499.0, -1061.0, -1650.0,
            -2266.5, -2909.0, -3577.0, -4270.0, -4987.5, -5727.5,
            -6490.0, -7274.0, -8077.5, -8899.5, -9739.0, -10594.5,
            -11464.5, -12347.0, -13241.0, -14144.5, -15056.0, -15973.5,
            -16895.5, -17820.0, -18744.5, -19668.0, -20588.0, -21503.0,
            -22410.5, -23308.5, -24195.0, -25068.5, -25926.5, -26767.0,
            -27589.0, -28389.0, -29166.5, -29919.0, -30644.5, -31342.0,
            -32009.5, -32645.0, -33247.0, -33814.5, -34346.0, -34839.5,
            -35295.0, -35710.0, -36084.5, -36417.5, -36707.5, -36954.0,
            -37156.5, -37315.0, -37428.0, -37496.0, 37519.0, 37496.0,
            37428.0, 37315.0, 37156.5, 36954.0, 36707.5, 36417.5,
            36084.5, 35710.0, 35295.0, 34839.5, 34346.0, 33814.5,
            33247.0, 32645.0, 32009.5, 31342.0, 30644.5, 29919.0,
            29166.5, 28389.0, 27589.0, 26767.0, 25926.5, 25068.5,
            24195.0, 23308.5, 22410.5, 21503.0, 20588.0, 19668.0,
            18744.5, 17820.0, 16895.5, 15973.5, 15056.0, 14144.5,
            13241.0, 12347.0, 11464.5, 10594.5, 9739.0, 8899.5,
            8077.5, 7274.0, 6490.0, 5727.5, 4987.5, 4270.0,
            3577.0, 2909.0, 2266.5, 1650.0, 1061.0, 499.0,
            -35.0, -541.0, -1018.5, -1467.5, -1888.0, -2280.5,
            -2644.0, -2979.5, 3287.0, 3567.0, 3820.0, 4046.0,
            4246.0, 4420.0, 4569.5, 4694.5, 4796.0, 4875.0,
            4931.5, 4967.5, 4983.0, 4979.5, 4958.0, 4919.0,
            4863.5, 4792.5, 4708.0, 4609.5, 4499.0, 4377.5,
            4245.5, 4104.5, 3955.0, 3798.5, 3635.5, 3467.5,
            3294.5, 3118.5, 2939.5, 2758.5, 2576.5, 2394.0,
            2212.5, 2031.5, 1852.5, 1675.5, 1502.0, 1331.5,
            1165.0, 1003.0, 846.0, 694.0, 547.5, 407.0,
            272.5, 144.0, 22.5, -92.5, -201.0, -302.5,
            -397.0, -485.0, -565.5, -640.0, -707.0, -767.5,
            -822.0, -869.5, -911.0, -946.5, -976.0, -1000.0,
            1018.5, 1031.5, 1040.0, 1043.5, 1042.5, 1037.5,
            1028.5, 1016.0, 1000.5, 981.0, 959.5, 935.0,
            908.5, 879.5, 849.0, 817.0, 783.5, 749.0,
            714.0, 678.0, 641.5, 605.0, 568.5, 532.0,
            495.5, 459.5, 424.0, 389.5, 355.5, 322.5,
            290.5, 259.5, 229.5, 200.5, 173.5, 147.0,
            122.0, 98.5, 76.5, 55.5, 36.0, 18.0,
            1.0, -14.5, -28.5, -41.5, -53.0, -63.5,
            -73.0, -81.5, -88.5, -94.5, -100.0, -104.0,
            -107.5, -110.5, -112.0, -113.5, -114.0, -114.0,
            -113.5, -112.5, -111.0, -109.0, 106.5, 104.0,
            101.0, 98.0, 95.0, 91.5, 88.0, 84.5,
            80.5, 77.0, 73.5, 69.5, 66.0, 62.5,
            58.5, 55.5, 52.0, 48.5, 45.5, 42.5,
            39.5, 36.5, 34.0, 31.5, 29.0, 26.5,
            24.5, 22.5, 20.5, 19.0, 17.5, 15.5,
            14.5, 13.0, 12.0, 10.5, 9.5, 8.5,
            8.0, 7.0, 6.5, 5.5, 5.0, 4.5,
            4.0, 3.5, 3.5, 3.0, 2.5, 2.5,
            2.0, 2.0, 1.5, 1.5, 1.0, 1.0,
            1.0, 1.0, 0.5, 0.5, 0.5, 0.5,
            0.5, 0.5
        )

        // Quantizer lookup, step 1: bitrate classes
        val QUANT_LUT_STEP_1 = arrayOf(
            // 32, 48, 56, 64, 80, 96,112,128,160,192,224,256,320,384 <- bitrate
            intArrayOf(0, 0, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2), // mono
            // 16, 24, 28, 32, 40, 48, 56, 64, 80, 96,112,128,160,192 <- bitrate / chan
            intArrayOf(0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 2, 2, 2, 2) // stereo
        )

        // Quantizer lookup, step 2: bitrate class, sample rate -> B2 table idx, sblimit
        object QUANT_TAB {
            const val A = (27 or 64) // Table 3-B.2a: high-rate, sblimit = 27
            const val B = (30 or 64) // Table 3-B.2b: high-rate, sblimit = 30
            const val C = 8       // Table 3-B.2c:  low-rate, sblimit =  8
            const val D = 12        // Table 3-B.2d:  low-rate, sblimit = 12
        }

        val QUANT_LUT_STEP_2 = arrayOf(
            //   44.1 kHz,        48 kHz,          32 kHz
            arrayOf(QUANT_TAB.C, QUANT_TAB.C, QUANT_TAB.D), // 32 - 48 kbit/sec/ch
            arrayOf(QUANT_TAB.A, QUANT_TAB.A, QUANT_TAB.A), // 56 - 80 kbit/sec/ch
            arrayOf(QUANT_TAB.B, QUANT_TAB.A, QUANT_TAB.B)  // 96+	 kbit/sec/ch
        )

        // Quantizer lookup, step 3: B2 table, subband -> nbal, row index
        // (upper 4 bits: nbal, lower 4 bits: row index)
        val QUANT_LUT_STEP_3 = arrayOf(
            // Low-rate table (3-B.2c and 3-B.2d)
            intArrayOf(
                0x44, 0x44,
                0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34
            ),
            // High-rate table (3-B.2a and 3-B.2b)
            intArrayOf(
                0x43, 0x43, 0x43,
                0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
                0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31, 0x31,
                0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20
            ),
            // MPEG-2 LSR table (B.2 in ISO 13818-3)
            intArrayOf(
                0x45, 0x45, 0x45, 0x45,
                0x34, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34,
                0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24,
                0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24, 0x24
            )
        )

        // Quantizer lookup, step 4: table row, allocation[] value -> quant table index
        val QUANT_LUT_STEP4 = arrayOf(
            intArrayOf(0, 1, 2, 17),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 17),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 17),
            intArrayOf(0, 1, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            intArrayOf(0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17),
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        )

        data class Quant(val levels: Int, val group: Int, val bits: Int)

        val QUANT_TAB_TAB = arrayOf(
            Quant(levels = 3, group = 1, bits = 5),  //  1
            Quant(levels = 5, group = 1, bits = 7),  //  2
            Quant(levels = 7, group = 0, bits = 3),  //  3
            Quant(levels = 9, group = 1, bits = 10),  //  4
            Quant(levels = 15, group = 0, bits = 4),  //  5
            Quant(levels = 31, group = 0, bits = 5),  //  6
            Quant(levels = 63, group = 0, bits = 6),  //  7
            Quant(levels = 127, group = 0, bits = 7),  //  8
            Quant(levels = 255, group = 0, bits = 8),  //  9
            Quant(levels = 511, group = 0, bits = 9),  // 10
            Quant(levels = 1023, group = 0, bits = 10),  // 11
            Quant(levels = 2047, group = 0, bits = 11),  // 12
            Quant(levels = 4095, group = 0, bits = 12),  // 13
            Quant(levels = 8191, group = 0, bits = 13),  // 14
            Quant(levels = 16383, group = 0, bits = 14),  // 15
            Quant(levels = 32767, group = 0, bits = 15),  // 16
            Quant(levels = 65535, group = 0, bits = 16)   // 17
        )
    }
}
