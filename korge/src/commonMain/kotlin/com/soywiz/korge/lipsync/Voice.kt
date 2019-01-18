package com.soywiz.korge.lipsync

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korev.*
import com.soywiz.korge.animate.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class LipSync(val lipsync: String) {
	val timeMs: Int get() = lipsync.length * 16
	operator fun get(timeMs: Int): Char = lipsync.getOrElse(timeMs / 16) { 'X' }
	fun getAF(timeMs: Int): Char {
		val c = this[timeMs]
		return when (c) {
			'G' -> 'B'
			'H' -> 'C'
			'X' -> 'A'
			else -> c
		}
	}
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(Voice.Factory::class)
class Voice(val views: Views, val voice: NativeSound, val lipsync: LipSync) {
	val timeMs: Int get() = lipsync.timeMs
	operator fun get(timeMs: Int): Char = lipsync[timeMs]
	fun getAF(timeMs: Int): Char = lipsync.getAF(timeMs)

	suspend fun play(name: String) {
		views.lipSync.play(this, name)
	}
}

data class LipSyncEvent(var name: String = "", var time: TimeSpan = 0.seconds, var lip: Char = 'X') : Event() {
	val timeMs: Int get() = time.millisecondsInt
}

class LipSyncHandler(val views: Views) {
	val event = LipSyncEvent()

	private fun dispatch(name: String, elapsedTime: TimeSpan, lip: Char) {
		views.dispatch(event.apply {
			this.name = name
			this.time = elapsedTime
			this.lip = lip
		})
	}

	suspend fun play(voice: Voice, name: String) = suspendCancellableCoroutine<Unit> { c ->
		var cancel: Cancellable? = null

		val channel = voice.voice.play()

		cancel = views.stage.addUpdatable {
			val elapsedTime = channel.current
			val elapsedTimeMs = elapsedTime.millisecondsInt
			//println("elapsedTime:$elapsedTime, channel.length=${channel.length}")
			if (elapsedTime >= channel.total) {
				cancel?.cancel()
				dispatch(name, 0.seconds, 'X')
			} else {
				dispatch(name, channel.current, voice[elapsedTimeMs])
			}
		}

		val cancel2 = launchImmediately(c.context) {
			channel.await()
			c.resume(Unit)
		}

		c.invokeOnCancellation {
			val error = it ?: error("Unknown")
			cancel?.cancel(error)
			cancel2.cancel(error)
			channel.stop()
			dispatch(name, 0.seconds, 'X')
		}
	}
}

class LipSyncComponent(override val view: View) : EventComponent {
	override fun onEvent(event: Event) {
		if (event is LipSyncEvent) {
			val name = view.getPropString("lipsync")
			if (event.name == name) {
				view.play("${event.lip}")
			}
		}
	}
}

val Views.lipSync by Extra.PropertyThis<Views, LipSyncHandler> { LipSyncHandler(this) }

suspend fun VfsFile.readVoice(views: Views): Voice {
	val lipsyncFile = this.withExtension("lipsync")
	return Voice(
		views,
		this.readNativeSoundOptimized(),
		LipSync(if (lipsyncFile.exists()) lipsyncFile.readString().trim() else "")
	)
}
