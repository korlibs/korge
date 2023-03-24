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

    suspend operator fun invoke(
        config: KorgeConfig,
        ag: AG = AGDummy(config.finalWindowSize.width, config.finalWindowSize.height),
        devicePixelRatio: Double = 1.0,
        entry: suspend Stage.() -> Unit,
    ): HeadlessGameWindow {
        val gameWindow = HeadlessGameWindow(config.finalWindowSize.width, config.finalWindowSize.height, draw = config.headlessDraw, ag = ag, devicePixelRatio = devicePixelRatio)
        gameWindow.exitProcessOnClose = false
        config.copy(gameWindow = gameWindow).start {
            //config.main?.invoke(this)
            entry()
        }
        return gameWindow
    }
}

suspend fun KorgeConfig.headless(
    ag: AG = AGDummy(this.finalWindowSize.width, this.finalWindowSize.height),
    devicePixelRatio: Double = 1.0,
    entry: suspend Stage.() -> Unit,
) = KorgeHeadless.invoke(this, ag, devicePixelRatio, entry)
