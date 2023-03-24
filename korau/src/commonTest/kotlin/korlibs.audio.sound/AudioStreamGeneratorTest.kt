package korlibs.audio.sound

import korlibs.io.async.suspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioStreamGeneratorTest {
    @Test
    fun test() = suspendTest {
        val data = AudioStream.generator(44100, 2) { step ->
            for (channel in 0 until channels) {
                write(channel, shortArrayOf(
                    (10 + step).toShort(),
                    (20 + step).toShort(),
                    (30 + step).toShort(),
                    (40 + step).toShort())
                )
            }
            step < 4
        }.toData()
        assertEquals(2, data.channels)
        assertEquals(4 * 5, data.totalSamples)
        assertEquals(
            listOf(10, 20, 30, 40, 11, 21, 31, 41, 12, 22, 32, 42, 13, 23, 33, 43, 14, 24, 34, 44),
            data[0].map { it.toInt() }.toList()
        )
    }
}
