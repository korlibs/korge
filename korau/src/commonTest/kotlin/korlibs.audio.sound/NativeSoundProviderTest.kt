package korlibs.audio.sound

import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class NativeSoundProviderTest {
    class DequeNativeSoundProvider : LogNativeSoundProvider() {
    }

    @Test
    fun test() = suspendTest {
        val values = listOf(null, 1.0, 0.1).map { volume ->
            val nativeSoundProvider = DequeNativeSoundProvider()
            val sound = nativeSoundProvider
                .createSound(resourcesVfs["wav8bit.wav"], streaming = true)
                .also { if (volume != null) it.volume = volume }
            sound.playAndWait()
            val data = nativeSoundProvider.streams.first().data
            (0 until 10).map { data.read(0).toInt() }
        }
        assertEquals(
            listOf(
                listOf(0, 1020, 2295, 3315, 4335, 5610, 6630, 7650, 8670, 9690),
                listOf(0, 1020, 2295, 3315, 4335, 5610, 6630, 7650, 8670, 9690),
                listOf(0, 102, 229, 331, 433, 561, 663, 765, 867, 969),
            ),
            values
        )
    }
}
