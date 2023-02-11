package com.soywiz.korge.testing

import com.soywiz.kgl.*
import com.soywiz.korag.*
import com.soywiz.korge.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import com.soywiz.korim.color.*
import com.soywiz.korio.async.*
import com.soywiz.korio.async.use
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*

fun suspendTestWithOffscreenAG(callback: suspend CoroutineScope.(ag: AG) -> Unit) = suspendTest {
    KmlGlContextDefault().use { ctx ->
    //GLOBAL_HEADLESS_KML_CONTEXT.also { ctx ->
        val ag = AGOpenglAWT(checkGl = false, logGl = false, context = ctx)
        callback(ag)
    }
}

fun korgeOffscreenTest(
    width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
    virtualWidth: Int = width, virtualHeight: Int = height,
    bgcolor: RGBA? = Colors.BLACK,
    callback: suspend Stage.() -> Unit
) {
    suspendTestWithOffscreenAG { ag ->
        KorgeHeadless(width = width, height = height, virtualWidth = virtualWidth, virtualHeight = virtualHeight, bgcolor = bgcolor, ag = ag) {
            try {
                callback()
            } finally {
                gameWindow.close()
            }
        }
    }
}
