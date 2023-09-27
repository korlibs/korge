package korlibs.audio.sound

import kotlin.test.Test
import kotlin.test.assertEquals

class AudioSamplesDequeTest {
    @Test
    fun testWriteMonoToStereoDeque() {
        val deque = AudioSamplesDeque(2)
        deque.write(AudioSamplesInterleaved(1, 2, shortArrayOf(1024, 4000)))
        val temp = AudioSamplesInterleaved(2, 2)
        val readCount = deque.read(temp)
        assertEquals(2, readCount)
        assertEquals(shortArrayOf(1024, 1024, 4000, 4000).toList(), temp.data.toList())
    }
}
