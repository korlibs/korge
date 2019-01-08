package com.soywiz.korge.audio

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korau.format.*
import com.soywiz.korau.sound.*
import com.soywiz.korge.view.*
import com.soywiz.korinject.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import kotlinx.coroutines.*

//@Singleton
class SoundSystem(val views: Views) : AsyncDependency {
	override suspend fun init() {
		nativeSoundProvider.initOnce()
	}

	internal val promises = LinkedHashSet<Deferred<*>>()

	fun play(file: SoundFile) = createChannel().play(file.nativeSound)
	fun play(nativeSound: NativeSound): SoundChannel = createChannel().play(nativeSound)

	fun createChannel(): SoundChannel = SoundChannel(this)

	fun createSoundChannel(): SoundChannel = SoundChannel(this)
	fun createMusicChannel(): MusicChannel = MusicChannel(this)

	fun close() {
		for (promise in promises) promise.cancel()
		promises.clear()
	}
}

interface AudioChannel {
	val soundSystem: SoundSystem
	fun stop(): Unit
}

//@Prototype
open class SoundChannel(override val soundSystem: SoundSystem) : AudioChannel {
	var enabled: Boolean = true

	var playing: Boolean = false; private set
	var position: TimeSpan = TimeSpan.ZERO; private set
	var length: TimeSpan = TimeSpan.ZERO; private set
	val remaining: TimeSpan get() = (length - position)
	var volume = 1.0

	private var startedTime: DateTime = DateTime.EPOCH
	private var promise: Deferred<*>? = null

	fun play(
		sound: NativeSound,
		progress: (current: TimeSpan, total: TimeSpan) -> Unit = { current, total -> }
	): SoundChannel {
		if (enabled) {
			stop()

			startedTime = TimeProvider.now()
			length = sound.length
			playing = true

			promise = asyncImmediately(soundSystem.views.coroutineContext) {
				sound.playAndWait { current, total ->
					this@SoundChannel.position = current
					this@SoundChannel.length = total
					progress(current, total)
				}
				_end()
			}

			soundSystem.promises += promise!!
		}
		return this
	}


	fun play(stream: BaseAudioStream, bufferSeconds: Double = 0.1) {
		stop()

		val astream = object : BaseAudioStream by stream {
			override suspend fun read(out: ShortArray, offset: Int, length: Int): Int {
				val read = stream.read(out, offset, length)
				for (n in offset until offset + read) {
					out[n] = (out[n] * volume).clamp(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toShort()
				}
				return read
			}
		}

		promise = asyncImmediately(soundSystem.views.coroutineContext) {
			astream.playAndWait(bufferSeconds)
		}
	}

	override fun stop() {
		_end()
		promise?.cancel()
		promise = null
	}

	private fun _end() {
		if (promise != null) soundSystem.promises -= promise!!
		position = 0.seconds
		length = 0.seconds
		playing = false
	}

	suspend fun await() {
		promise?.await()
	}
}

//@Prototype
class MusicChannel(override val soundSystem: SoundSystem) : SoundChannel(soundSystem) {
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(SoundFile.Factory::class)
class SoundFile(
	val nativeSound: NativeSound,
	val soundSystem: SoundSystem
) {
	fun play() = soundSystem.play(this.nativeSound)
}

// @TODO: Could end having two instances!
val Views.soundSystem by Extra.PropertyThis<Views, SoundSystem> {
	SoundSystem(this).apply { injector.mapInstance<SoundSystem>(this) }
}

suspend fun VfsFile.readSoundFile(soundSystem: SoundSystem) = SoundFile(this.readNativeSoundOptimized(), soundSystem)
