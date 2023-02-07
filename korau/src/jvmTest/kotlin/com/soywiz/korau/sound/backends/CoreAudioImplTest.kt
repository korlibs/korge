package com.soywiz.korau.sound.backends

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class CoreAudioImplTest {
    @Test
    @Ignore
    fun test() = suspendTest {
        println("[1]")
        val sound = resourcesVfs["Snowland.mp3"].readSound().toAudioData()
        println("[2]")
        jvmCoreAudioNativeSoundProvider!!.playAndWait(sound.toStream())
        println("[3]")
        //CoreFoundation.CFRunLoopRun()
        //CoreAudioImpl2.AudioComponentInstanceNew()
        while (true) {
            delay(0.5.seconds)
        }
    }
}
