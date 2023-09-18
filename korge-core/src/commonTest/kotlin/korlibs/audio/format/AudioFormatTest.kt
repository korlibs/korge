package korlibs.audio.format

import korlibs.logger.*
import korlibs.memory.*
import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import doIOTest
import korlibs.platform.*
import kotlin.test.assertEquals

class AudioFormatTest {
	val formats = standardAudioFormats() + OGG
    val logger = Logger("AudioFormatTest")

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
        logger.debug { "doIOTest[0]: ${Platform.isAndroid}, ${Platform.rawOsName}" }
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
