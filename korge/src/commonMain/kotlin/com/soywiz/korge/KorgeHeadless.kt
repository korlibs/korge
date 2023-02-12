package com.soywiz.korge

import com.soywiz.klock.*
import com.soywiz.korag.*
import com.soywiz.korag.log.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korma.geom.*
import kotlinx.coroutines.*

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
        override val width: Int = 640,
        override val height: Int = 480,
        val draw: Boolean = false,
        override val ag: AG = AGDummy(width, height),
        exitProcessOnClose: Boolean = false,
        override val devicePixelRatio: Double = 1.0,
    ) : GameWindow() {
        init {
            this.exitProcessOnClose = exitProcessOnClose
        }

        override val coroutineDispatcher: GameWindowCoroutineDispatcher = HeadlessGameWindowCoroutineDispatcher(this)


        //override val ag: AG = if (draw) AGSoftware(width, height) else DummyAG(width, height)
        //override val ag: AG = AGDummy(width, height)
    }

    suspend operator fun invoke(config: Korge.Config) = Korge(config.copy(gameWindow = HeadlessGameWindow()))

    suspend operator fun invoke(
        title: String = "Korge",
        width: Int = DefaultViewport.WIDTH, height: Int = DefaultViewport.HEIGHT,
        virtualWidth: Int = width, virtualHeight: Int = height,
        icon: Bitmap? = null,
        iconPath: String? = null,
        //iconDrawable: SizedDrawable? = null,
        imageFormats: ImageFormat = ImageFormats(PNG),
        quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC,
        targetFps: Double = 0.0,
        scaleAnchor: Anchor = Anchor.MIDDLE_CENTER,
        scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
        clipBorders: Boolean = true,
        bgcolor: RGBA? = Colors.BLACK,
        debug: Boolean = false,
        debugFontExtraScale: Double = 1.0,
        debugFontColor: RGBA = Colors.WHITE,
        fullscreen: Boolean? = null,
        args: Array<String> = arrayOf(),
        timeProvider: TimeProvider = TimeProvider,
        injector: AsyncInjector = AsyncInjector(),
        blocking:Boolean = true,
        debugAg: Boolean = false,
        draw: Boolean = false,
        ag: AG = AGDummy(width, height),
        devicePixelRatio: Double = 1.0,
        stageBuilder: (Views) -> Stage = { Stage(it) },
        entry: suspend Stage.() -> Unit,
    ): HeadlessGameWindow {
        val gameWindow = HeadlessGameWindow(width, height, draw = draw, ag = ag, devicePixelRatio = devicePixelRatio)
        gameWindow.exitProcessOnClose = false
        Korge(
            title, width, height, virtualWidth, virtualHeight, icon, iconPath, /*iconDrawable,*/ imageFormats, quality,
            targetFps, scaleAnchor, scaleMode, clipBorders, bgcolor, debug, debugFontExtraScale, debugFontColor,
            fullscreen, args, gameWindow, timeProvider, injector,
            blocking = blocking, debugAg = debugAg, stageBuilder = stageBuilder, entry = {
                entry()
            }, forceRenderEveryFrame = true
        )
        return gameWindow
    }
}
