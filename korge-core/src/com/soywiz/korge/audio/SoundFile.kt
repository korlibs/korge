package com.soywiz.korge.audio

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readNativeSoundOptimized
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass

@AsyncFactoryClass(SoundFile.Factory::class)
class SoundFile(
	val nativeSound: NativeSound,
	val soundSystem: SoundSystem
) {
	suspend fun play() {
		soundSystem.play(this.nativeSound)
	}

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

