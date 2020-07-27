import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.util.*
import com.soywiz.korvi.*
import kotlin.coroutines.coroutineContext

suspend fun main() = Korge(width = 1280, height = 720, bgcolor = Colors["#2b2b2b"]) {
	//addUpdaterOnce {
		val view = korviView(views, resourcesVfs["video.mp4"])
		if (OS.isJs) {
			val text = text("Click to start playing the video...")
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

inline fun Container.korviView(views: Views, video: KorviVideo, callback: KorviView.() -> Unit = {}): KorviView = KorviView(views, video).also { addChild(it) }.also { callback(it) }
suspend inline fun Container.korviView(views: Views, video: VfsFile, autoPlay: Boolean = true, callback: KorviView.() -> Unit = {}): KorviView = KorviView(views, video, autoPlay).also { addChild(it) }.also { callback(it) }
class KorviView(val views: Views, val video: KorviVideo) : Image(Bitmaps.transparent), AsyncCloseable, BaseKorviSeekable by video {
	val onPrepared = Signal<Unit>()
	val onCompleted = Signal<Unit>()
	var autoLoop = true

	companion object {
		suspend operator fun invoke(views: Views, file: VfsFile, autoPlay: Boolean = true): KorviView {
			return KorviView(views, KorviVideo(file)).also {
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
		views.launchImmediately {
			ensurePrepared()
			video.play()
		}
	}

	private var bmp = Bitmap32(1, 1)

	init {
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

				if (!itData.data.ints.contentEquals(bmp.data.ints)) {
					bmp.lock {
						arraycopy(itData.data, 0, bmp.data, 0, bmp.area)
					}
				}
			}
		}
		video.onComplete {
			views.launchImmediately {
				if (autoLoop) {
					seek(0L)
					video.play()
				}
			}
		}
	}
}
