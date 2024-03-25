package korlibs.audio.sound

import korlibs.time.seconds
import kotlin.test.Test
import kotlin.test.assertEquals

class AudioToneTest {
    @Test
    fun test() {
        val data = AudioTone.generate(1.seconds, 440.0)
        assertEquals(44100, data.totalSamples)
        assertEquals(1, data.samples.data.size)
        val channel = data.samples.data[0]
        assertEquals(44100, channel.size)
        assertEquals(
            "0,2052,4097,6126,8130,10103,12036,13921,15752,17521,19222,20846,22389,23844,25205,26467",
            (0 until 16).map { channel[it] }.joinToString(",")
        )
    }
}
