package com.soywiz.korau.format

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import doIOTest
import kotlin.test.*

class AudioFormatTest {
	val formats = standardAudioFormats()

	@kotlin.test.Test
	fun wav() = suspendTest({ doIOTest }) {
        assertEquals(
			"Info(duration=500ms, channels=1)",
			resourcesVfs["wav1.wav"].readSoundInfo(formats).toString()
		)
		assertEquals(
			"Info(duration=500ms, channels=1)",
			resourcesVfs["wav2.wav"].readSoundInfo(formats).toString()
		)
	}

    @kotlin.test.Test
    fun wavCorrupted() = suspendTest({ doIOTest }) {
        assertEquals(
            "Info(duration=3842.902ms, channels=1)",
            resourcesVfs["boom.wav"].readSoundInfo(formats).toString()
        )
    }

	@kotlin.test.Test
	fun ogg() = suspendTest({ doIOTest }) {
        assertEquals(
			"Info(duration=500ms, channels=1)",
			resourcesVfs["ogg1.ogg"].readSoundInfo(formats).toString()
		)
	}

	@kotlin.test.Test
	fun mp3() = suspendTest({ doIOTest }) {
        assertEquals(
			"Info(duration=546.625ms, channels=1)",
			resourcesVfs["mp31.mp3"].readSoundInfo(formats, AudioDecodingProps(exactTimings = false)).toString()
		)
        assertEquals(
            "Info(duration=574.684ms, channels=1)",
            resourcesVfs["mp31.mp3"].readSoundInfo(formats, AudioDecodingProps(exactTimings = true)).toString()
        )
	}
}
