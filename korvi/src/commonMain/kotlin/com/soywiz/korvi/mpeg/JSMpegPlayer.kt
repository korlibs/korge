package com.soywiz.korvi.mpeg

import com.soywiz.klock.milliseconds
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8ClampedBuffer
import com.soywiz.kmem.toUint8Buffer
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korau.sound.AudioSamplesDeque
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.async.Signal
import com.soywiz.korio.async.delay
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readBytesUpTo
import com.soywiz.korvi.mpeg.mux.TS
import com.soywiz.korvi.mpeg.stream.AudioDestination
import com.soywiz.korvi.mpeg.stream.VideoDestination
import com.soywiz.korvi.mpeg.stream.audio.MP2
import com.soywiz.korvi.mpeg.stream.video.MPEG1
import kotlin.coroutines.CoroutineContext

class JSMpegPlayer(val coroutineContext: CoroutineContext) {
    var bitmap: Bitmap32 = Bitmap32(0, 0)

    var lastVideoTime: Double = 0.0
    var lastAudioTime: Double = 0.0
    val onDecodedVideoFrame: Signal<JSMpegPlayer> = Signal()
    val onDecodedAudioFrame: Signal<JSMpegPlayer> = Signal()

    val streaming = false

    val video = MPEG1(streaming = streaming, onDecodeCallback = { video, time ->
        //println("mpeg1.onDecodeCallback")
        lastVideoTime = time
        onDecodedVideoFrame(this)
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
                val cNextLine: Int = w2 - (this.width shr 1)

                var rgbaIndex1 = 0
                var rgbaIndex2: Int = this.width
                val rgbaNext2Lines: Int = this.width

                val cols: Int = this.width shr 1
                val rows: Int = this.height shr 1

                var ccb: Int
                var ccr: Int
                var r: Int
                var g: Int
                var b: Int

                val ints = bitmap.ints

                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        ccb = Cb[cIndex]
                        ccr = Cr[cIndex]
                        cIndex++
                        b = ccb + ((ccb * 103) shr 8) - 179
                        g = ((ccr * 88) shr 8) - 44 + ((ccb * 183) shr 8) - 91
                        r = ccr + ((ccr * 198) shr 8) - 227

                        // Line 1
                        val y1 = Y[yIndex1++]
                        val y2 = Y[yIndex1++]
                        val col1 = RGBA(y1 + r, y1 - g, y1 + b, 0xFF).value
                        val col2 = RGBA(y2 + r, y2 - g, y2 + b, 0xFF).value
                        //println("$rgbaIndex1: ${col1.r}, ${col1.g}, ${col1.b}, ${col1.a}")
                        ints[rgbaIndex1 + 0] = col1
                        ints[rgbaIndex1 + 1] = col2
                        rgbaIndex1 += 2

                        // Line 2
                        val y3 = Y[yIndex2++]
                        val y4 = Y[yIndex2++]
                        val col3 = RGBA(y3 + r, y3 - g, y3 + b, 0xFF).value
                        val col4 = RGBA(y4 + r, y4 - g, y4 + b, 0xFF).value
                        ints[rgbaIndex2 + 0] = col3
                        ints[rgbaIndex2 + 1] = col4
                        rgbaIndex2 += 2
                    }
                    yIndex1 += yNext2Lines
                    yIndex2 += yNext2Lines
                    rgbaIndex1 += rgbaNext2Lines
                    rgbaIndex2 += rgbaNext2Lines
                    cIndex += cNextLine
                }

                //println("frame[${frameN++}] ${hashArray(Y)} ${hashArray(Cr)} ${hashArray(Cb)}")
            }

            override fun resize(width: Int, height: Int) {
                //println("video.resize=$width,$height")
                bitmap = Bitmap32(width, height, premultiplied = true)
            }
        })
    }
    val audio = MP2(streaming = streaming, onDecodeCallback = { audio, time ->
        lastAudioTime = time
        onDecodedAudioFrame(this)
        //println("mp2.onDecodeCallback")
    }).also {
        it.connect(object : AudioDestination {
            override fun play(rate: Int, left: FloatArray, right: FloatArray) {
                //println("audio.play:rate=$rate")
                val audioSamples = AudioSamples(2, left.size)
                for (n in left.indices) audioSamples.setFloatStereo(n, left[n], right[n])
                audioSamplesDeque.write(audioSamples)
            }

            override var enqueuedTime: Double = 0.0
        })
    }
    val demuxer = TS().also {
        it.connect(TS.STREAM.VIDEO_1, video)
        it.connect(TS.STREAM.AUDIO_1, audio)
    }

    internal fun write(data: Uint8Buffer) {
        demuxer.write(data)
    }

    val audioStream = nativeSoundProvider.createPlatformAudioOutput(coroutineContext)
    //val audioStream = nativeSoundProvider.createAudioStream(Dispatchers.Unconfined)
    val audioSamplesDeque = AudioSamplesDeque(2)
    val audioSamples = AudioSamples(2, 4096)

    private var data: AsyncInputStream = byteArrayOf().openAsync()

    fun setStream(data: AsyncInputStream) {
        this.data = data
    }

    fun frameSimple(): Boolean {
        val v = video.decode()
        val a = audio.decode()
        val result = v || a
        return result
    }

    suspend fun frame(): Boolean {
        //println(demuxer.parsePacket())
        //println(video.decode())
        //for (n in 0 until 7) println(video.decode())
        while (audioSamplesDeque.availableRead > 0) {
            val count = audioSamplesDeque.read(audioSamples)
            audioStream.add(audioSamples, 0, count)
        }

        while (true) {
            val result = frameSimple()

            if (!result || video.decodedTime >= demuxer.currentTime - 4.0) {
                //val chunk = data.readBytesUpTo(128 * 1024)
                val chunk = data.readBytesUpTo(2 * 1024 * 1024)
                //val chunk = data.readBytesUpTo(100 * 1024 * 1024)
                write(chunk.toUint8Buffer())
                continue
            }

            delay(16.milliseconds)

            return result
        }
        //println()
    }

    fun frameSync() {
    }
}
