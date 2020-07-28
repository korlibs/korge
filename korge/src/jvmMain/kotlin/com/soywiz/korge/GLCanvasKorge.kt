package com.soywiz.korge

import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import kotlinx.coroutines.*
import java.io.*

class GLCanvasKorge private constructor(val canvas: GLCanvas, val dummy: Boolean) : Closeable {
    val gameWindow = GLCanvasGameWindow(canvas)
    lateinit var stage: Stage
    val views get() = stage.views

    private suspend fun init() {
        //val deferred = CompletableDeferred<Stage>()
        //val context = coroutineContext
        //println("[a]")
        Thread {
            runBlocking {
                Korge(width = canvas.width, height = canvas.height, gameWindow = gameWindow) {
                    //println("[A]")
                    this@GLCanvasKorge.stage = this@Korge
                    //deferred.complete(this@Korge)
                    //println("[B]")
                }
            }
        }.start()
        //println("[b]")
        while (!::stage.isInitialized) delay(1L)
        //this@GLCanvasKorge.stage = deferred.await()
        //println("[c]")
    }

    suspend fun executeInContext(block: suspend Stage.() -> Unit) {
        withContext(stage.coroutineContext) {
            block(stage)
        }
    }

    override fun close() {
        gameWindow.exit()
    }

    companion object {
        suspend operator fun invoke(canvas: GLCanvas): GLCanvasKorge {
            return GLCanvasKorge(canvas, false).apply { init() }
        }
    }
}
