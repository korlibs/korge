package com.soywiz.korvi.internal

import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.HtmlImage
import com.soywiz.korim.format.HtmlNativeImage
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.std.UrlVfs
import com.soywiz.korvi.KorviVideo
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.events.Event
import kotlinx.browser.document
import kotlinx.browser.window

internal actual val korviInternal: KorviInternal = JsKorviInternal()

internal class JsKorviInternal : KorviInternal() {
    override suspend fun createHighLevel(file: VfsFile): KorviVideo {
        val final = file.getUnderlyingUnscapedFile()
        val vfs = final.vfs
        when (vfs) {
            is UrlVfs -> return KorviVideoJs(final.file.absolutePath)
            else -> error("Unsupported playing video from: ${final}")
        }
    }
}

class KorviVideoJs(val url: String) : KorviVideo() {
    var video = document.createElement("video").unsafeCast<HTMLVideoElement>()
    init {
        video.src = url
    }

    override val running: Boolean get() = !video.paused
    override val elapsedTimeHr: HRTimeSpan get() = video.currentTime.seconds.hr

    override suspend fun getTotalFrames(): Long? = null
    override suspend fun getDuration(): HRTimeSpan? = video.duration.seconds.hr

    val videoComplete = { e: Event? ->
        onComplete(Unit)
    }

    private var videoImage: HtmlNativeImage? = null
    val videoFrame = { e: Event? ->
        if (video.videoWidth != 0 && video.videoHeight != 0) {
            if (videoImage == null || videoImage!!.width != video.videoWidth || videoImage!!.height != video.videoHeight) {
                videoImage = HtmlNativeImage(video, video.videoWidth, video.videoHeight)
            }
            onVideoFrame(
                Frame(
                    videoImage!!.also { videoImage!!.contentVersion++ },
                    video.currentTime.seconds.hr,
                    40.milliseconds.hr
                )
            )
        }
    }

    override suspend fun play() {
        removeListeners()
        addListeners()
        video.play()
    }

    override suspend fun seek(frame: Long) {
        super.seek(frame)
    }

    override suspend fun seek(time: HRTimeSpan) {
        video.fastSeek(time.secondsDouble)
    }

    lateinit var animationFrame: (Double) -> Unit
    init {
        animationFrame = {
            videoFrame(null)
            animationHandle = window.requestAnimationFrame(animationFrame)
        }
    }

    private fun addListeners() {
        animationFrame(0.0)
        video.addEventListener("ended", videoComplete)
    }

    private var animationHandle = -1
    private fun removeListeners() {
        window.cancelAnimationFrame(animationHandle)
        video.removeEventListener("ended", videoComplete)
    }

    override suspend fun stop() {
        removeListeners()
        video.pause()
    }

    override suspend fun close() {
        stop()
    }
}
