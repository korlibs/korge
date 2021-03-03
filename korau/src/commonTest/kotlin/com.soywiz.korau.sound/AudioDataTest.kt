package com.soywiz.korau.sound

import com.soywiz.korio.async.*
import kotlin.test.*

class AudioDataTest {
    @Test
    fun testToStreamToData() = suspendTest {
        val data1 = AudioData(44100, AudioSamples(2, 44100, Array(2) { ShortArray(44100) { it.toShort() } }))
        val data2 = data1.toStream().toData()
        assertTrue { data1[0].contentEquals(data2[0]) }
        assertTrue { data1[1].contentEquals(data2[1]) }
    }
}
