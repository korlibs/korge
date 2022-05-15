package com.soywiz.korge.lipsync

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.milliseconds
import com.soywiz.klock.seconds
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.readSound
import com.soywiz.korev.Event
import com.soywiz.korev.dispatch
import com.soywiz.korge.animate.play
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korge.component.EventComponent
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.file.VfsFile

class LipSync(val lipsync: String) {
	val totalTime: TimeSpan get() = (lipsync.length * 16).milliseconds
	operator fun get(time: TimeSpan): Char = lipsync.getOrElse(time.millisecondsInt / 16) { 'X' }
	fun getAF(time: TimeSpan): Char {
		val c = this[time]
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
class Voice(val voice: Sound, val lipsync: LipSync) {
	val totalTime: TimeSpan get() = lipsync.totalTime
	operator fun get(time: TimeSpan): Char = lipsync[time]
	fun getAF(time: TimeSpan): Char = lipsync.getAF(time)
	val event = LipSyncEvent()

	suspend fun play(name: String, views: Views) {
		play(name) { e ->
			views.dispatch(e)
		}
	}

	suspend fun play(name: String, handler: (LipSyncEvent) -> Unit) {
		voice.playAndWait { current, total ->
			if (current >= total) {
				handler(event.set(name, 0.seconds, 'X'))
			} else {
				handler(event.set(name, current, lipsync[current]))
			}
		}
	}
}

data class LipSyncEvent(var name: String = "", var time: TimeSpan = 0.seconds, var lip: Char = 'X') : Event() {
	fun set(name: String, elapsedTime: TimeSpan, lip: Char) = apply {
		this.name = name
		this.time = elapsedTime
		this.lip = lip
	}

	val timeMs: Int get() = time.millisecondsInt
}

class LipSyncComponent(override val view: BaseView) : EventComponent {
	override fun onEvent(event: Event) {
		if (event is LipSyncEvent) {
			val name = view.getPropString("lipsync")
			if (event.name == name) {
                (view as? View?)?.play("${event.lip}")
			}
		}
	}
}

fun View.lipsync() = this.getOrCreateComponentEvent<LipSyncComponent> { LipSyncComponent(it) }

suspend fun VfsFile.readVoice(): Voice {
	val lipsyncFile = this.withExtension("lipsync")
	return Voice(
		this.readSound(),
		LipSync(if (lipsyncFile.exists()) lipsyncFile.readString().trim() else "")
	)
}
