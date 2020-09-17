package com.soywiz.korau

import com.soywiz.korau.sound.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*

object KorauTestMain  {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val music = resourcesVfs["mp31_joint_stereo_vbr.mp3"].readMusic()
        }
    }
}
