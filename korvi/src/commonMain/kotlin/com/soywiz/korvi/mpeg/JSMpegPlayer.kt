package com.soywiz.korvi.mpeg

import com.soywiz.kds.algo.Historiogram
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8ClampedBuffer
import com.soywiz.kmem.subarray
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korvi.mpeg.mux.TS
import com.soywiz.korvi.mpeg.stream.AudioDestination
import com.soywiz.korvi.mpeg.stream.VideoDestination
import com.soywiz.korvi.mpeg.stream.audio.MP2
import com.soywiz.korvi.mpeg.stream.video.MPEG1

class JSMpegPlayer {
    var bitmap: Bitmap32 = Bitmap32(0, 0)

    val video = MPEG1(false, onDecodeCallback = { video, time ->
        //println("mpeg1.onDecodeCallback")
    }).also {
        it.connect(object : VideoDestination {
            val width: Int get() = bitmap.width
            val height: Int get() = bitmap.height
            var frameN = 0

            override fun render(Y: Uint8ClampedBuffer, Cr: Uint8ClampedBuffer, Cb: Uint8ClampedBuffer, v: Boolean) {
                // Chroma values are the same for each block of 4 pixels, so we proccess
                // 2 lines at a time, 2 neighboring pixels each.
                // I wish we could use 32bit writes to the RGBA buffer instead of writing
                // each byte separately, but we need the automatic clamping of the RGBA
                // buffer.

                // Chroma values are the same for each block of 4 pixels, so we proccess
                // 2 lines at a time, 2 neighboring pixels each.
                // I wish we could use 32bit writes to the RGBA buffer instead of writing
                // each byte separately, but we need the automatic clamping of the RGBA
                // buffer.
                val w: Int = ((this.width + 15) shr 4) shl 4
                val w2 = w shr 1

                var yIndex1 = 0
                var yIndex2: Int = w
                val yNext2Lines: Int = w + (w - this.width)

                var cIndex = 0
                val cNextLine: Int = w2 - (this.width ushr 1)

                var rgbaIndex1 = 0
                var rgbaIndex2: Int = this.width
                val rgbaNext2Lines: Int = this.width

                val cols: Int = this.width ushr 1
                val rows: Int = this.height ushr 1

                var ccb: Int
                var ccr: Int
                var r: Int
                var g: Int
                var b: Int

                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        ccb = Cb[cIndex]
                        ccr = Cr[cIndex]
                        cIndex++
                        b = ccb + ((ccb * 103) ushr 8) - 179
                        g = ((ccr * 88) ushr 8) - 44 + ((ccb * 183) ushr 8) - 91
                        r = ccr + ((ccr * 198) ushr 8) - 227

                        // Line 1
                        val y1 = Y[yIndex1++]
                        val y2 = Y[yIndex1++]
                        val col1 = RGBA(y1 + r, y1 - g, y1 + b, 0xFF)
                        val col2 = RGBA(y2 + r, y2 - g, y2 + b, 0xFF)
                        //println("$rgbaIndex1: ${col1.r}, ${col1.g}, ${col1.b}, ${col1.a}")
                        if (rgbaIndex1 + 1 >= bitmap.area) break
                        bitmap.setRgbaAtIndex(rgbaIndex1 + 0, col1)
                        bitmap.setRgbaAtIndex(rgbaIndex1 + 1, col2)
                        rgbaIndex1 += 2

                        // Line 2
                        val y3 = Y[yIndex2++]
                        val y4 = Y[yIndex2++]
                        val col3 = RGBA(y3 + r, y3 - g, y3 + b, 0xFF)
                        val col4 = RGBA(y4 + r, y4 - g, y4 + b, 0xFF)
                        if (rgbaIndex2 + 1 >= bitmap.area) break
                        bitmap.setRgbaAtIndex(rgbaIndex2 + 0, col3)
                        bitmap.setRgbaAtIndex(rgbaIndex2 + 1, col4)
                        rgbaIndex2 += 2
                    }
                    yIndex1 += yNext2Lines
                    yIndex2 += yNext2Lines
                    rgbaIndex1 += rgbaNext2Lines
                    rgbaIndex2 += rgbaNext2Lines
                    cIndex += cNextLine
                }

                println("frame[${frameN++}] ${hashArray(Y)} ${hashArray(Cr)} ${hashArray(Cb)}")
            }

            override fun resize(width: Int, height: Int) {
                println("video.resize=$width,$height")
                bitmap = Bitmap32(width, height, premultiplied = true)
            }
        })
    }
    val audio = MP2(false, onDecodeCallback = { audio, time ->
        //println("mp2.onDecodeCallback")
    }).also {
        it.connect(object : AudioDestination {
            override fun play(rate: Int, left: FloatArray, right: FloatArray) {
                println("audio.play:rate=$rate")
            }

            override var enqueuedTime: Double = 0.0
        })
    }
    val demuxer = TS().also {
        it.connect(TS.STREAM.VIDEO_1, video)
        it.connect(TS.STREAM.AUDIO_1, audio)
    }

    fun write(data: Uint8Buffer) {
        demuxer.write(data)
    }

    fun frame() {
        //println(demuxer.parsePacket())
        //println(video.decode())
        //for (n in 0 until 7) println(video.decode())
        for (n in 0 until 7) {
            println(video.decode())
        }
        //println(audio.decode())
    }
}
