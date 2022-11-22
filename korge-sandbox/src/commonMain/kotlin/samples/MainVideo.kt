package samples

import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korvi.*
import kotlinx.coroutines.*

class MainVideo : ScaledScene(1280, 720) {
    override suspend fun SContainer.sceneMain() {
        //addUpdaterOnce {
        val view = korviView(this@MainVideo, resourcesVfs["video.mp4"])
        if (OS.isJs) {
            val text = textOld("Click to start playing the video...")
            mouse.click.once {
                text.removeFromParent()
                view.play()
            }
        } else {
            view.play()
        }
        //}
    }


    fun View.addUpdaterOnce(block: () -> Unit) {
        var cancellable: Cancellable? = null
        cancellable = addUpdater {
            cancellable?.cancel()
            block()
        }
    }

    inline fun Container.korviView(coroutineScope: CoroutineScope, video: KorviVideo, callback: KorviView.() -> Unit = {}): KorviView = KorviView(coroutineScope, video).also { addChild(it) }.also { callback(it) }
    suspend inline fun Container.korviView(coroutineScope: CoroutineScope, video: VfsFile, autoPlay: Boolean = true, callback: KorviView.() -> Unit = {}): KorviView = KorviView(coroutineScope, video, autoPlay).also { addChild(it) }.also { callback(it) }
    class KorviView(val coroutineScope: CoroutineScope, val video: KorviVideo) : BaseImage(Bitmaps.transparent), AsyncCloseable, BaseKorviSeekable by video {
        val onPrepared = Signal<Unit>()
        val onCompleted = Signal<Unit>()
        var autoLoop = true

        companion object {
            suspend operator fun invoke(coroutineScope: CoroutineScope, file: VfsFile, autoPlay: Boolean = true): KorviView {
                return KorviView(coroutineScope, KorviVideo(file)).also {
                    if (autoPlay) {
                        it.play()
                    }
                }
            }
        }

        private var _prepared: Boolean = false

        private suspend fun ensurePrepared() {
            if (!_prepared) {
                onPrepared.waitOne()
            }
        }

        override fun renderInternal(ctx: RenderContext) {
            if (!_prepared) {
                video.prepare()
                _prepared = true
                onPrepared()
            } else {
                video.render()
            }
            super.renderInternal(ctx)
        }

        val elapsedTime: TimeSpan get() = video.elapsedTime
        val elapsedTimeHr: HRTimeSpan get() = video.elapsedTimeHr

        fun play() {
            if (video.running) return
            coroutineScope.launchImmediately {
                ensurePrepared()
                //try {
                    video.play()
                //} finally {
                //    video.stop()
                //}
            }
        }

        private var bmp = Bitmap32(1, 1)

        init {
            coroutineScope.launchImmediately {
                try {
                    while (true) delay(100.seconds)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    video.stop()
                }
            }
            addUpdater {
                if (video.running) invalidateRender()
            }
            video.onVideoFrame {
                //println("VIDEO FRAME! : ${it.position.timeSpan},  ${it.duration.timeSpan}")
                if (OS.isJs || OS.isAndroid) {
                    //if (false) {
                    bitmap = it.data.slice()
                    //println(it.data)
                } else {
                    val itData = it.data.toBMP32IfRequired()
                    //println("itData: $itData: ${it.data.width}, ${it.data.height}")
                    if (bmp.width != itData.width || bmp.height != itData.height) {
                        bmp = Bitmap32(itData.width, itData.height)
                        bitmap = bmp.slice()
                    }

                    if (!itData.ints.contentEquals(bmp.ints)) {
                        bmp.lock {
                            com.soywiz.kmem.arraycopy(itData.ints, 0, bmp.ints, 0, bmp.area)
                        }
                    }
                }
            }
            video.onComplete {
                coroutineScope.launchImmediately {
                    if (autoLoop) {
                        seek(0L)
                        video.play()
                    }
                }
            }
        }
    }
}
