package com.soywiz.korau.sound

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*
import kotlin.test.*

class NativeAudioStreamNativeLinuxTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        coroutineScope {
            val sound = resourcesVfs["mp31.mp3"].readSound()
            launchAsap {
                sound.playAndWait()
            }
            launchAsap {
                delay(100.milliseconds)
                sound.playAndWait()
            }
            launchAsap {
                delay(200.milliseconds)
                sound.playAndWait()
            }
            launchAsap {
                delay(300.milliseconds)
                sound.playAndWait()
            }
            launchAsap {
                delay(400.milliseconds)
                sound.playAndWait()
            }
            launchAsap {
                delay(500.milliseconds)
                val channel = sound.play()
                channel.pitch = 0.2
                channel.await()
            }
        }
    }
}
