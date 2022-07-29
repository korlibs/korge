package com.soywiz.korvi.mpeg

import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.Uint8ClampedBuffer
import com.soywiz.korim.color.YCbCr
import com.soywiz.korvi.mpeg.mux.TS
import com.soywiz.korvi.mpeg.stream.AudioDestination
import com.soywiz.korvi.mpeg.stream.VideoDestination
import com.soywiz.korvi.mpeg.stream.audio.MP2
import com.soywiz.korvi.mpeg.stream.video.MPEG1

class JSMpegPlayer {
    val video = MPEG1(false, onDecodeCallback = { video, time ->
        println("mpeg1.onDecodeCallback")
    }).also {
        it.connect(object : VideoDestination {
            override fun render(y: Uint8ClampedBuffer?, Cr: Uint8ClampedBuffer?, Cb: Uint8ClampedBuffer?, v: Boolean) {
                println("video.render: ${y?.size}, ${Cr?.size}, ${Cb?.size}")
            }

            override fun resize(width: Int, height: Int) {
                println("video.resize=$width,$height")
            }
        })
    }
    val audio = MP2(false, onDecodeCallback = { audio, time ->
        println("mp2.onDecodeCallback")
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
        println(demuxer.parsePacket())
        println(video.decode())
        println(audio.decode())
    }
}
