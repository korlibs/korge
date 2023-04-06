package korlibs.korge

import korlibs.graphics.*
import korlibs.graphics.log.*
import korlibs.image.format.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import korlibs.time.*

object KorgeHeadless {
    class HeadlessGameWindowCoroutineDispatcher(val gameWindow: HeadlessGameWindow) : GameWindowCoroutineDispatcher() {
        //init {
        //    frameRenderLoop()
        //}
//
        //fun frameRenderLoop() {
        //    this.invokeOnTimeout(16L, Runnable {
        //        //println("frameRenderLoop")
        //        gameWindow.frameRender()
        //        frameRenderLoop()
        //    }, gameWindow.coroutineDispatcher)
        //}

        override fun executePending(availableTime: TimeSpan) {
            //println("HeadlessGameWindowCoroutineDispatcher.executePending: timedTasks=${_timedTasks.size}, tasks=${_tasks.size}")
            super.executePending(availableTime)
        }
    }

    class HeadlessGameWindow(
        val size: Size = Size(640, 480),
        val draw: Boolean = false,
        override val ag: AG = AGDummy(size),
        exitProcessOnClose: Boolean = false,
        override val devicePixelRatio: Float = 1f,
    ) : GameWindow() {
        override val width: Int = size.width.toInt()
        override val height: Int = size.height.toInt()

        init {
            this.exitProcessOnClose = exitProcessOnClose
        }

        override val coroutineDispatcher: GameWindowCoroutineDispatcher = HeadlessGameWindowCoroutineDispatcher(this)


        //override val ag: AG = if (draw) AGSoftware(width, height) else DummyAG(width, height)
        //override val ag: AG = AGDummy(width, height)
    }

    suspend operator fun invoke(
        config: KorgeConfig,
        ag: AG = AGDummy(config.windowSize),
        devicePixelRatio: Float = 1f,
        draw: Boolean = false,
        entry: suspend Stage.() -> Unit,
    ): HeadlessGameWindow {
        val config = config.copy(imageFormats = config.imageFormats + PNG)
        val gameWindow = HeadlessGameWindow(config.windowSize, draw = draw, ag = ag, devicePixelRatio = devicePixelRatio)
        gameWindow.exitProcessOnClose = false
        config.copy(gameWindow = gameWindow).start {
            //config.main?.invoke(this)
            entry()
        }
        return gameWindow
    }
}

suspend fun KorgeConfig.headless(
    ag: AG = AGDummy(this.windowSize),
    devicePixelRatio: Float = 1f,
    draw: Boolean = false,
    entry: suspend Stage.() -> Unit,
) = KorgeHeadless.invoke(this, ag, devicePixelRatio, draw, entry)
