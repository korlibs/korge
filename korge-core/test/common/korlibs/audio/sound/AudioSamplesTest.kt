package korlibs.audio.sound

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioSamplesTest {
    @Test
    fun test() {
        val samples = AudioSamples(2, 22050)
        val samples2 = samples.resample(22050, 44100)
        assertEquals(44100, samples2.totalSamples)
    }
}
