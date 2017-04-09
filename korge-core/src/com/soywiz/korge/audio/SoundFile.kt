package com.soywiz.korge.audio

import com.soywiz.korau.format.AudioData
import com.soywiz.korau.format.play
import com.soywiz.korau.format.readAudioData
import com.soywiz.korge.resources.Path
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.ResourcesVfs

@AsyncFactoryClass(SoundRefFactory::class)
class SoundFile(val audio: AudioData) {
	suspend fun play() {
		audio.play()
	}
}

class SoundRefFactory(
	val path: Path
) : AsyncFactory<SoundFile> {
	suspend override fun create(): SoundFile {
		return SoundFile(ResourcesVfs[path.path].readAudioData())
	}
}
