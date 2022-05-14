package com.soywiz.korvi

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.hr.timeSpan
import com.soywiz.klock.seconds
import com.soywiz.klock.toTimeString
import com.soywiz.korau.sound.AudioData
import com.soywiz.korau.sound.AudioSamples
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.SystemFont
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.VerticalAlign

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
    override val video: List<KorviVideoStream> = listOf(DummyKorviVideoStream(this))
    override val audio: List<KorviAudioStream> = listOf(DummyKorviAudioStream(this))

    override suspend fun close() {
    }

    // https://youtrack.jetbrains.com/issue/KT-46214
    open class DummyBaseStream<TFrame : KorviFrame>(val base: DummyKorviVideoLL) : BaseKorviStream<TFrame> {
        var currentFrame = 0L

        override suspend fun getTotalFrames(): Long? = base.totalFrames
        override suspend fun getDuration(): HRTimeSpan? = base.timePerFrame * base.totalFrames.toDouble()
        override suspend fun seek(frame: Long) { currentFrame = frame }
        override suspend fun seek(time: HRTimeSpan) { seek((time / base.timePerFrame).toLong()) }
    }

    class DummyKorviVideoStream(base: DummyKorviVideoLL) : DummyBaseStream<KorviVideoFrame>(base) {
        override suspend fun readFrame(): KorviVideoFrame? {
            if (currentFrame >= base.totalFrames) return null
            val frame = currentFrame++
            val currentTime = base.timePerFrame * frame.toDouble()
            val data = NativeImage(base.width, base.height)
            data.context2d {
                fill(Colors.DARKGREEN) {
                    fillRect(0, 0, base.width, base.height)
                }
                fillText(
                    currentTime.timeSpan.toTimeString(),
                    base.width * 0.5,
                    base.height * 0.5,
                    color = Colors.WHITE,
                    font = SystemFont("Arial"),
                    fontSize = 32.0,
                    halign = HorizontalAlign.CENTER,
                    valign = VerticalAlign.MIDDLE
                )
            }
            return KorviVideoFrame({ data.toBMP32() }, frame, base.timePerFrame * frame.toDouble(), base.timePerFrame)
        }
    }

    class DummyKorviAudioStream(base: DummyKorviVideoLL) : DummyBaseStream<KorviAudioFrame>(base) {
        override suspend fun readFrame(): KorviAudioFrame? {
            if (currentFrame >= base.totalFrames) return null
            val frame = currentFrame++
            val data = AudioData(44100, AudioSamples(2, (44100 * base.timePerFrame.timeSpan.seconds).toInt()))
            return KorviAudioFrame(data, frame, base.timePerFrame * frame.toDouble(), base.timePerFrame)
        }
    }
}
