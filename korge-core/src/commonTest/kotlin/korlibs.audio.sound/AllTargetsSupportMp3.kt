package korlibs.audio.sound

import doIOTest
import korlibs.audio.format.*
import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.platform.*
import kotlin.test.*

class AllTargetsSupportMp3 {
    @Test
    fun testDecode() = suspendTest({ doIOTest }) {
        val data = resourcesVfs["mp31.mp3"].readSound().decode()
    }

    @Test
    fun testIsMp3() = suspendTest({ doIOTest }) {
        if (Platform.isWasm) return@suspendTest

        assertNotNull(MP3.tryReadInfo(resourcesVfs["8Khz-Mono.mp3"].open()))
        assertNull(MP3.tryReadInfo(resourcesVfs["8Khz-Mono.opus"].open()))
    }
}
