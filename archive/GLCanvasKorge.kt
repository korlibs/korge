package korlibs.korge

/*
import korlibs.io.async.*
import korlibs.korge.internal.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.awt.*
import korlibs.render.platform.*
import kotlinx.coroutines.*
import java.awt.*
import java.io.*

fun GLCanvasWithKorge(
    config: KorgeConfig,
    block: suspend Stage.() -> Unit
): GLCanvasWithKorge {
    val canvas = GLCanvasWithKorge()
    val korge = GLCanvasKorge(false, canvas, config)
    korge.initNoWait(block)
    return canvas
}

fun Korge.glCanvas(block: suspend Stage.() -> Unit): GLCanvasWithKorge = GLCanvasWithKorge(this, block)

open class GLCanvasWithKorge : GLCanvas() {
    lateinit var korge: GLCanvasKorge
    val views get() = korge.views
    val injector get() = korge.injector
}

class GLCanvasKorge internal constructor(
    val dummy: Boolean,
    val canvas: GLCanvasWithKorge,
    val config: KorgeConfig,
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
                config.copy(
                    gameWindow = gameWindow,
                    main = {
                        println("GLCanvasKorge.init[d]: ${Thread.currentThread()}")
                        //println("[A]")
                        this@GLCanvasKorge.stage = this@copy
                        config.main?.invoke(this)
                        block()
                    }
                ).start()
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

    @Suppress("unused")
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
            virtualSize: Size? = null,
        ): GLCanvasKorge {
            return GLCanvasKorge(true, canvas, KorgeConfig(virtualSize = virtualSize ?: DefaultViewport.SIZE)).apply { init() }
        }
    }
}

open class GLCanvasGameWindow(
    val canvas: GLCanvas,
) : BaseAwtGameWindow(canvas.ag) {
    init {
        exitProcessOnClose = false
        canvas.defaultRenderer = { gl, g ->
            framePaint(g)
        }
    }

    override val ctx: BaseOpenglContext get() = canvas.ctx!!
    override val component: Component get() = canvas
    override val contentComponent: Component get() = canvas
}
*/
