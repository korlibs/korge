package korlibs.korge

import korlibs.datastructure.event.*
import korlibs.datastructure.thread.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.graphics.log.*
import korlibs.image.format.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.render.*
import korlibs.time.*

object KorgeHeadless {
    @OptIn(ExperimentalStdlibApi::class)
    class HeadlessGameWindow(
        val size: Size = Size(640, 480),
        val draw: Boolean = false,
        override val ag: AG = AGDummy(size),
        exitProcessOnClose: Boolean = false,
        override val devicePixelRatio: Double = 1.0,
    ) : GameWindow() {
        val syncEventLoop by lazy { eventLoop as SyncEventLoop }

        override val width: Int = size.width.toInt()
        override val height: Int = size.height.toInt()

        init {
            this.exitProcessOnClose = exitProcessOnClose
        }

        init {
            nativeThread(name = "HeadlessGameWindow-syncEventLoop") { syncEventLoop.runTasksForever() }
            eventLoop.setInterval(60.hz.timeSpan) {
                (ag as? AGOpengl?)?.context?.set()
                this@HeadlessGameWindow.dispatchNewRenderEvent()
            }
        }

        //override val ag: AG = if (draw) AGSoftware(width, height) else DummyAG(width, height)
        //override val ag: AG = AGDummy(width, height)
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend operator fun invoke(
        config: KorgeConfig,
        ag: AG = AGDummy(config.windowSize),
        devicePixelRatio: Double = 1.0,
        draw: Boolean = false,
        entry: suspend Stage.() -> Unit,
    ): HeadlessGameWindow {
        val config = config.copy(imageFormats = config.imageFormats + PNG)
        val gameWindow = HeadlessGameWindow(config.windowSize, draw = draw, ag = ag, devicePixelRatio = devicePixelRatio)
        gameWindow.exitProcessOnClose = false
        config.copy(gameWindow = gameWindow).start {
            entry()
        }
        return gameWindow
    }
}

suspend fun KorgeConfig.headless(
    ag: AG = AGDummy(this.windowSize),
    devicePixelRatio: Double = 1.0,
    draw: Boolean = false,
    entry: suspend Stage.() -> Unit,
) = KorgeHeadless.invoke(this, ag, devicePixelRatio, draw, entry)
