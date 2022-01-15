package com.soywiz.korge

import com.soywiz.korge.view.*
import com.soywiz.korgw.awt.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.SizeInt
import kotlinx.coroutines.*
import java.io.*

fun GLCanvasWithKorge(
    config: Korge.Config,
    block: suspend Stage.() -> Unit
): GLCanvas {
    val canvas = GLCanvas()
    val korge = GLCanvasKorge(false, canvas, config)
    korge.initNoWait(block)
    return canvas
}

class GLCanvasKorge internal constructor(
    val dummy: Boolean,
    val canvas: GLCanvas,
    val config: Korge.Config,
) : Closeable {
    val gameWindow = GLCanvasGameWindow(canvas)
    lateinit var stage: Stage
    val views get() = stage.views
    val injector get() = views.injector

    internal fun initNoWait(block: suspend Stage.() -> Unit = {}) {
        //val deferred = CompletableDeferred<Stage>()
        //val context = coroutineContext
        //println("[a]")
        println("GLCanvasKorge.init[a]: ${Thread.currentThread()}")
        Thread {
            println("GLCanvasKorge.init[b]: ${Thread.currentThread()}")
            runBlocking {
                println("GLCanvasKorge.init[c]: ${Thread.currentThread()}")
                Korge(config.copy(
                    gameWindow = gameWindow,
                    main = {
                        println("GLCanvasKorge.init[d]: ${Thread.currentThread()}")
                        //println("[A]")
                        this@GLCanvasKorge.stage = this@copy
                        config.main?.invoke(this)
                        block()
                    }
                ))
            }
        }
            .also { it.isDaemon = true }
            //.also { thread = it }
            .start()
        println("GLCanvasKorge.init[e]: ${Thread.currentThread()}")
    }

    internal suspend fun init() {
        initNoWait()
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
        suspend operator fun invoke(
            canvas: GLCanvas,
            virtualWidth: Int? = null,
            virtualHeight: Int? = null
        ): GLCanvasKorge {
            return GLCanvasKorge(true, canvas, Korge.Config(virtualSize = virtualWidth?.let { SizeInt(virtualWidth, virtualHeight ?: virtualWidth) })).apply { init() }
        }
    }
}
