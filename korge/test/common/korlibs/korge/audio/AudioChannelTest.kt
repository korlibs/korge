package korlibs.korge.audio

import korlibs.audio.sound.*
import korlibs.korge.tests.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlin.test.*

class AudioChannelTest : ViewsForTesting() {
    val ViewsContainer.musicChannel by audioChannel()

    @Test
    fun test() = viewsTest {
        val soundProvider = LogNativeSoundProvider()
        val instance1 = musicChannel
        val instance2 = musicChannel
        assertSame(instance1, instance2)
        val tone = AudioTone.generate(0.1.seconds, 0.0).toSound(soundProvider)
        assertNull(instance1.channel)
        instance1.play(tone)
        assertNotNull(instance2.channel)
        instance2.stop()
        assertNull(instance1.channel)
    }
}
