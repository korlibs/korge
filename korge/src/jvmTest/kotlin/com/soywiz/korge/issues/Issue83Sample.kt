package com.soywiz.korge.issues

import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.*

object Issue83Sample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            main()
        }
    }

    suspend fun main() = Korge(width = 512, height = 512, bgcolor = Colors["#2b2b2b"]) {
        val image = image(resourcesVfs["korge.png"].readBitmap()) {
            anchor(.5, .5)
            scale(.8)
            position(256, 256)
        }

        onClick {
            println("onClick:" + it.currentPosLocal)
        }
        onDown {
            println("onDown:" + it.downPosGlobal)
        }

    }
}
