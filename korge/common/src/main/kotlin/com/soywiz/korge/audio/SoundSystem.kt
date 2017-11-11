package com.soywiz.korge.audio

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.nativeSoundProvider
import com.soywiz.korau.sound.readNativeSoundOptimized
import com.soywiz.korau.sound.registerNativeSoundSpecialReader
import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.resources.VPath
import com.soywiz.korge.view.Views
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.go
import com.soywiz.korinject.AsyncDependency
import com.soywiz.korinject.AsyncFactory
import com.soywiz.korinject.Prototype
import com.soywiz.korinject.Singleton
import com.soywiz.korio.time.TimeProvider
import com.soywiz.kds.Extra
import com.soywiz.korio.util.clamp
import com.soywiz.korio.vfs.VfsFile

object SoundPlugin : KorgePlugin() {
	init {
		registerNativeSoundSpecialReader()
	}

	suspend override fun register(views: Views) {
		views.injector
			.mapFactory(SoundFile::class) {
				//AnLibrary.Factory(getOrNull(), getOrNull(), get(), get(), get()) // @TODO: Kotlin.js bug
				SoundFile.Factory(
					getOrNull(Path::class),
					getOrNull(VPath::class),
					get(ResourcesRoot::class),
					get(SoundSystem::class)
				)
			}
	}
}

@Singleton
class SoundSystem(val views: Views) : AsyncDependency {
	suspend override fun init() {
		nativeSoundProvider.init()
	}

	internal val promises = LinkedHashSet<Promise<*>>()

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
	val position: Int
		get() {
			return if (playing) {
				(TimeProvider.now() - startedTime).toInt()
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
			stop()

			startedTime = TimeProvider.now()
			length = sound.lengthInMs.toInt()
			playing = true

			promise = go(soundSystem.views.coroutineContext) {
				sound.play()
				_end()
			}

			soundSystem.promises += promise!!
		}
		return this
	}

	fun stop() {
		_end()
		promise?.cancel()
	}

	private fun _end() {
		if (promise != null) soundSystem.promises -= promise!!
		length = 0
		playing = false
	}


	suspend fun await() {
		promise?.await()
	}
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(SoundFile.Factory::class)
class SoundFile(
	val nativeSound: NativeSound,
	val soundSystem: SoundSystem
) {
	fun play() = soundSystem.play(this.nativeSound)

	class Factory(
		val path: Path?,
		val vpath: VPath?,
		val resourcesRoot: ResourcesRoot,
		val soundSystem: SoundSystem
	) : AsyncFactory<SoundFile> {
		suspend override fun create(): SoundFile {
			val rpath = path?.path ?: vpath?.path ?: ""
			return SoundFile(resourcesRoot[rpath].readNativeSoundOptimized(), soundSystem)
		}
	}
}

// @TODO: Could end having two instances!
val Views.soundSystem by Extra.PropertyThis<Views, SoundSystem> {
	SoundSystem(this).apply { injector.mapInstance<SoundSystem>(this) }
}

suspend fun VfsFile.readSoundFile(soundSystem: SoundSystem) = SoundFile(this.readNativeSoundOptimized(), soundSystem)
