package com.soywiz.korge.audio

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.spawn
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.util.Extra

@Singleton
class SoundSystem {
	private val promises = LinkedHashSet<Promise<Unit>>()

	suspend fun play(file: SoundFile) = this.play(file.nativeSound)

	suspend fun play(nativeSound: NativeSound) {
		val promise = spawn {
			nativeSound.play()
		}
		promises += promise
		promise.await()
	}

	fun close() {
		for (promise in promises) promise.cancel()
		promises.clear()
	}
}

// @TODO: Could end having two instances!
val Views.soundSystem by Extra.PropertyThis<Views, SoundSystem> {
	SoundSystem().apply { injector.mapTyped<SoundSystem>(this) }
}
