package com.soywiz.korvi

import com.soywiz.klock.*
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.hr.timeSpan
import com.soywiz.korau.sound.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.vector.*

class DummyKorviVideoLL(
    val totalFrames: Long,
    val timePerFrame: HRTimeSpan,
    val width: Int = 320,
    val height: Int = 240
) : KorviVideoLL() {
    companion object {
        operator fun invoke(
            time: TimeSpan = 60.seconds,
            fps: Number = 24,
            width: Int = 320,
            height: Int = 240
        ) : KorviVideoLL {
            val timePerFrame = 1.seconds * (1 / fps.toDouble())
            return DummyKorviVideoLL((time / timePerFrame).toLong(), timePerFrame.hr, width, height)
        }
    }
    override val video: List<KorviVideoStream> = listOf(DummyKorviVideoStream())
    override val audio: List<KorviAudioStream> = listOf(DummyKorviAudioStream())

    override suspend fun close() {
    }

    open inner class DummyBaseStream<TFrame : KorviFrame> : BaseKorviStream<TFrame> {
        var currentFrame = 0L

        override suspend fun getTotalFrames(): Long? = this@DummyKorviVideoLL.totalFrames
        override suspend fun getDuration(): HRTimeSpan? = timePerFrame * this@DummyKorviVideoLL.totalFrames.toDouble()
        override suspend fun seek(frame: Long) = run { currentFrame = frame }
        override suspend fun seek(time: HRTimeSpan) = run { seek((time / timePerFrame).toLong()) }
    }

    inner class DummyKorviVideoStream : DummyBaseStream<KorviVideoFrame>() {
        override suspend fun readFrame(): KorviVideoFrame? {
            if (currentFrame >= totalFrames) return null
            val frame = currentFrame++
            val currentTime = timePerFrame * frame.toDouble()
            val data = NativeImage(width, height)
            data.context2d {
                fill(Colors.DARKGREEN) {
                    fillRect(0, 0, width, height)
                }
                fillText(
                    currentTime.timeSpan.toTimeString(),
                    width * 0.5,
                    height * 0.5,
                    color = Colors.WHITE,
                    font = SystemFont("Arial"),
                    fontSize = 32.0,
                    halign = HorizontalAlign.CENTER,
                    valign = VerticalAlign.MIDDLE
                )
            }
            return KorviVideoFrame({ data.toBMP32() }, frame, timePerFrame * frame.toDouble(), timePerFrame)
        }
    }

    inner class DummyKorviAudioStream : DummyBaseStream<KorviAudioFrame>() {
        override suspend fun readFrame(): KorviAudioFrame? {
            if (currentFrame >= totalFrames) return null
            val frame = currentFrame++
            val data = AudioData(44100, AudioSamples(2, (44100 * timePerFrame.timeSpan.seconds).toInt()))
            return KorviAudioFrame(data, frame, timePerFrame * frame.toDouble(), timePerFrame)
        }
    }
}
