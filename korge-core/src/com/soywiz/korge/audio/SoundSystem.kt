package com.soywiz.korge.audio

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readNativeSoundOptimized
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.go
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.inject.Prototype
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.clamp

@Singleton
class SoundSystem(val views: Views) {
	private val promises = LinkedHashSet<Promise<Unit>>()

	fun play(file: SoundFile) = createChannel().play(file.nativeSound)
	fun play(nativeSound: NativeSound): SoundChannel = createChannel().play(nativeSound)

	fun createChannel(): SoundChannel = SoundChannel(this)

	fun close() {
		for (promise in promises) promise.cancel()
		promises.clear()
	}
}

@Prototype
class SoundChannel(val soundSystem: SoundSystem) {
	var enabled: Boolean = true

	var playing: Boolean = false; private set
	val position: Int get() {
		return if (playing) {
			(System.currentTimeMillis() - startedTime).toInt()
		} else {
			0
		}
	}
	var length: Int = 0; private set
	val remaining: Int get() = (length - position).clamp(0, Int.MAX_VALUE)

	private var startedTime: Long = 0L
	private var promise: Promise<*>? = null

	fun play(sound: NativeSound): SoundChannel {
		if (enabled) {
			promise?.cancel()

			startedTime = System.currentTimeMillis()
			length = sound.lengthInMs.toInt()
			playing = true

			promise = go(soundSystem.views.coroutineContext) {
				sound.play()
				length = 0
				playing = false
			}
		}
		return this
	}

	fun stop() {
		promise?.cancel()
	}

	suspend fun await() {
		promise?.await()
	}
}

@AsyncFactoryClass(SoundFile.Factory::class)
class SoundFile(
	val nativeSound: NativeSound,
	val soundSystem: SoundSystem
) {
	fun play() = soundSystem.play(this.nativeSound)

	class Factory(
		val path: Path,
		val resourcesRoot: ResourcesRoot,
		val soundSystem: SoundSystem
	) : AsyncFactory<SoundFile> {
		suspend override fun create(): SoundFile {
			return SoundFile(resourcesRoot[path].readNativeSoundOptimized(), soundSystem)
		}
	}
}

// @TODO: Could end having two instances!
val Views.soundSystem by Extra.PropertyThis<Views, SoundSystem> {
	SoundSystem(this).apply { injector.mapTyped<SoundSystem>(this) }
}
