package com.soywiz.korau.sound.backends

import com.soywiz.kds.thread.*
import com.soywiz.kmem.dyn.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.sun.jna.Memory
import com.sun.jna.Pointer
import kotlin.coroutines.*
import kotlin.test.*

class JvmWaveOutImplTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        val audioData = resourcesVfs["Snowland.mp3"].readMusic().toAudioData()

        jvmWaveOutNativeSoundProvider!!.playAndWait(audioData.toStream())
    }
}
