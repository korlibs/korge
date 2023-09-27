package korlibs.audio.sound

import korlibs.io.async.suspendTest
import korlibs.time.*
import kotlin.test.*

class AudioDataTest {
    @Test
    fun testToStreamToData() = suspendTest {
        val data1 = AudioData(44100, AudioSamples(2, 44100, Array(2) { ShortArray(44100) { it.toShort() } }))
        val data2 = data1.toStream().toData()
        assertTrue { data1[0].contentEquals(data2[0]) }
        assertTrue { data1[1].contentEquals(data2[1]) }
    }

    @Test
    fun testAudioDataToStream() = suspendTest {
        val data1 = AudioData(44100, AudioSamples(2, 66150, Array(2) { ShortArray(66150) { it.toShort() } }))
        val stream = data1.toStream()

        assertEquals(66150, stream.totalLengthInSamples)
        assertEquals(1.5.seconds, stream.totalLength)
        assertEquals(44100, stream.rate)
        assertEquals(2, stream.channels)

        assertEquals(0L, stream.currentPositionInSamples)
        assertEquals(false, stream.finished)
        assertEquals(4410, stream.read(AudioSamples(2, 4410)))
        assertEquals(false, stream.finished)
        assertEquals(4410L, stream.currentPositionInSamples)
        assertEquals(4410, stream.read(AudioSamples(2, 4410)))
        assertEquals(false, stream.finished)
        assertEquals(8820L, stream.currentPositionInSamples)
        assertEquals(57330, stream.read(AudioSamples(2, 100000)))
        assertEquals(false, stream.finished)
        assertEquals(66150L, stream.currentPositionInSamples)
        assertEquals(0, stream.read(AudioSamples(2, 10)))
        assertEquals(true, stream.finished)
        assertEquals(66150L, stream.currentPositionInSamples)
        stream.currentPositionInSamples = 1000
        assertEquals(false, stream.finished)
        assertEquals(1000L, stream.currentPositionInSamples)
    }
}
