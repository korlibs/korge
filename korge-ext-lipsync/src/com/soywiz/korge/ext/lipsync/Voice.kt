package com.soywiz.korge.ext.lipsync

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readNativeSoundOptimized
import com.soywiz.korge.animate.play
import com.soywiz.korge.audio.soundSystem
import com.soywiz.korge.component.Component
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.go
import com.soywiz.korio.async.suspendCancellableCoroutine
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.util.Cancellable
import com.soywiz.korio.util.Extra
import com.soywiz.korio.vfs.VfsFile

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

@AsyncFactoryClass(Voice.Factory::class)
class Voice(val views: Views, val voice: NativeSound, val lipsync: LipSync) {
	val timeMs: Int get() = lipsync.timeMs
	operator fun get(timeMs: Int): Char = lipsync[timeMs]
	fun getAF(timeMs: Int): Char = lipsync.getAF(timeMs)

	suspend fun play(name: String) {
		views.lipSync.play(this, name)
	}

	class Factory(
		val path: Path,
		val resourcesRoot: ResourcesRoot,
		val views: Views
	) : AsyncFactory<Voice> {
		suspend override fun create(): Voice = resourcesRoot[path].readVoice(views)
	}
}

data class LipSyncEvent(var name: String = "", var timeMs: Int = 0, var lip: Char = 'X')

class LipSyncHandler(val views: Views) {
	val event = LipSyncEvent()

	private fun dispatch(name: String, elapsedTime: Int, lip: Char) {
		views.dispatch(event.apply {
			this.name = name
			this.timeMs = elapsedTime
			this.lip = lip
		})
	}

	suspend fun play(voice: Voice, name: String) = suspendCancellableCoroutine<Unit> { c ->
		val startTime = System.currentTimeMillis()
		var cancel: Cancellable? = null
		cancel = views.stage.addUpdatable {
			val currentTime = System.currentTimeMillis()
			val elapsedTime = (currentTime - startTime).toInt()
			if (elapsedTime >= voice.timeMs) {
				cancel?.cancel()
				dispatch(name, 0, 'X')
			} else {
				dispatch(name, elapsedTime, voice[elapsedTime])
			}
		}
		val cancel2 = go(c.context) {
			views.soundSystem.play(voice.voice)
			c.resume(Unit)
		}

		c.onCancel {
			cancel?.cancel(it)
			cancel2.cancel(it)
			dispatch(name, 0, 'X')
		}
	}
}

class LipSyncComponent(view: View) : Component(view) {
	init {
		addEventListener<LipSyncEvent> { it ->
			val name = view.getPropString("lipsync")
			if (it.name == name) {
				view.play("${it.lip}")
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
