package com.soywiz.korau.mod

interface SoundFont {
	operator fun get(patch: Int): SoundPatch
}

val DummySoundFont = object : SoundFont {
	override fun get(patch: Int) = DummySoundPatch
}

interface SoundPatch {
	fun getSample(key: Int, time: Int): Float
}

object DummySoundPatch : SoundPatch {
	override fun getSample(key: Int, time: Int): Float {
		return 0f
	}
}
