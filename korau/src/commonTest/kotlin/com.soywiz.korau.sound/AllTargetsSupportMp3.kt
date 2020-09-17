package com.soywiz.korau.sound

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*
import kotlin.test.*

class AllTargetsSupportMp3 {
    @Test
    @Ignore // TRAVIS: com.[secure].korau.sound.AllTargetsSupportMp3 > testDecode FAILED  com.jogamp.openal.ALException: Error opening default OpenAL device
    fun testDecode() = suspendTest {
        if (nativeSoundProvider.target == "android") return@suspendTest
        if (OS.isJsNodeJs) return@suspendTest
        if (OS.isLinux) return@suspendTest // TRAVIS: com.[secure].korau.sound.AllTargetsSupportMp3 > testDecode FAILED  com.jogamp.openal.ALException: Error opening default OpenAL device

        val data = resourcesVfs["mp31.mp3"].readSound().decode()
    }
}
