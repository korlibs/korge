package com.soywiz.korge.audio

import com.soywiz.korau.sound.NativeSound
import com.soywiz.korau.sound.readNativeSoundOptimized
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass

@AsyncFactoryClass(SoundRefFactory::class)
class SoundFile(val audio: NativeSound) {
	suspend fun play() {
		audio.play()
	}
}

class SoundRefFactory(
	val path: Path,
	val resourcesRoot: ResourcesRoot
) : AsyncFactory<SoundFile> {
	suspend override fun create(): SoundFile {
		return SoundFile(resourcesRoot[path].readNativeSoundOptimized())
	}
}
