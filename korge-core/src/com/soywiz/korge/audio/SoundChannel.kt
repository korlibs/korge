package com.soywiz.korge.audio

import com.soywiz.korio.inject.Prototype

@Prototype
class SoundChannel {
	var enabled: Boolean = true

	suspend fun play(sound: SoundFile) {
		if (enabled) sound.play()
	}
}
