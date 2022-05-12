package com.soywiz.korge

import com.soywiz.korge.view.Stage
import com.soywiz.korgw.awt.GLCanvas
import com.soywiz.korgw.awt.GLCanvasGameWindow
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korma.geom.SizeInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.Closeable

fun GLCanvasWithKorge(
    config: Korge.Config,
    block: suspend Stage.() -> Unit
): GLCanvasWithKorge {
    val canvas = GLCanvasWithKorge()
    val korge = GLCanvasKorge(false, canvas, config)
    korge.initNoWait(block)
    return canvas
}

open class GLCanvasWithKorge : GLCanvas() {
    lateinit var korge: GLCanvasKorge
    val views get() = korge.views
    val injector get() = korge.injector
}

class GLCanvasKorge internal constructor(
    val dummy: Boolean,
    val canvas: GLCanvasWithKorge,
    val config: Korge.Config,
) : Closeable {
    val gameWindow = GLCanvasGameWindow(canvas)
    lateinit var stage: Stage
    val views get() = stage.views
    val injector get() = views.injector

    internal fun initNoWait(block: suspend Stage.() -> Unit = {}) {
        canvas.korge = this
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

    suspend fun <T> executeInContext(block: suspend Stage.() -> T): T {
        return withContext(stage.coroutineContext) {
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
            canvas: GLCanvasWithKorge,
            virtualWidth: Int? = null,
            virtualHeight: Int? = null
        ): GLCanvasKorge {
            return GLCanvasKorge(true, canvas, Korge.Config(virtualSize = virtualWidth?.let { SizeInt(virtualWidth, virtualHeight ?: virtualWidth) })).apply { init() }
        }
    }
}
