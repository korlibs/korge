package com.soywiz.korau.sound.backends

import com.soywiz.korau.sound.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*

class JvmWaveOutImplTest {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val audioData = resourcesVfs["Snowland.mp3"].readMusic().toAudioData()
            jvmWaveOutNativeSoundProvider!!.playAndWait(audioData.toStream())
        }
    }
}
