package korlibs.audio.sound

import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import doIOTest
import korlibs.audio.format.*
import kotlin.test.*

class AllTargetsSupportMp3 {
    @Test
    fun testDecode() = suspendTest({ doIOTest }) {
        val data = resourcesVfs["mp31.mp3"].readSound().decode()
    }

    @Test
    fun testIsMp3() = suspendTest({ doIOTest }) {
        assertNotNull(MP3.tryReadInfo(resourcesVfs["8Khz-Mono.mp3"].open()))
        assertNull(MP3.tryReadInfo(resourcesVfs["8Khz-Mono.opus"].open()))
    }
}
