package korlibs.audio.sound

import korlibs.io.async.suspendTest
import korlibs.io.file.std.resourcesVfs
import doIOTest
import kotlin.test.Test

class AllTargetsSupportMp3 {
    @Test
    fun testDecode() = suspendTest({ doIOTest }) {
        val data = resourcesVfs["mp31.mp3"].readSound().decode()
    }
}