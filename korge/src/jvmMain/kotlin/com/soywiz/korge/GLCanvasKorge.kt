package com.soywiz.korge

import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import com.soywiz.korio.async.*
import kotlinx.coroutines.*
import java.io.*

class GLCanvasKorge private constructor(
    val dummy: Boolean,
    val canvas: GLCanvas,
    val virtualWidth: Int?,
    val virtualHeight: Int?
) : Closeable {
    val gameWindow = GLCanvasGameWindow(canvas)
    lateinit var stage: Stage
    val views get() = stage.views
    val injector get() = views.injector

    private suspend fun init() {
        //val deferred = CompletableDeferred<Stage>()
        //val context = coroutineContext
        //println("[a]")
        println("GLCanvasKorge.init[a]: ${Thread.currentThread()}")
        Thread {
            println("GLCanvasKorge.init[b]: ${Thread.currentThread()}")
            runBlocking {
                println("GLCanvasKorge.init[c]: ${Thread.currentThread()}")
                Korge(width = canvas.width, height = canvas.height, virtualWidth = virtualWidth ?: canvas.width, virtualHeight = virtualHeight ?: canvas.height, gameWindow = gameWindow) {
                    println("GLCanvasKorge.init[d]: ${Thread.currentThread()}")
                    //println("[A]")
                    this@GLCanvasKorge.stage = this@Korge
                    //deferred.complete(this@Korge)
                    //println("[B]")
                }
            }
        }.start()
        println("GLCanvasKorge.init[e]: ${Thread.currentThread()}")
        //println("[b]")
        while (!::stage.isInitialized) delay(1L)
        println("GLCanvasKorge.init[f]: ${Thread.currentThread()}")
        //this@GLCanvasKorge.stage = deferred.await()
        //println("[c]")
    }

    suspend fun executeInContext(block: suspend Stage.() -> Unit) {
        withContext(stage.coroutineContext) {
            block(stage)
        }
    }

    fun launchInContext(block: suspend Stage.() -> Unit) {
        launchImmediately(stage.coroutineContext) {
            block(stage)
        }
    }

    override fun close() {
        gameWindow.exit()
    }

    companion object {
        suspend operator fun invoke(canvas: GLCanvas, virtualWidth: Int? = null, virtualHeight: Int? = null): GLCanvasKorge {
            return GLCanvasKorge(true, canvas, virtualWidth, virtualHeight).apply { init() }
        }
    }
}
