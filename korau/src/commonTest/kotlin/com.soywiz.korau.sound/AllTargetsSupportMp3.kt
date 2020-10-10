package com.soywiz.korau.sound

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.std.resourcesVfs
import doIOTest
import kotlin.test.Test

class AllTargetsSupportMp3 {
    @Test
    fun testDecode() = suspendTest({ doIOTest }) {
        val data = resourcesVfs["mp31.mp3"].readSound().decode()
    }
}
