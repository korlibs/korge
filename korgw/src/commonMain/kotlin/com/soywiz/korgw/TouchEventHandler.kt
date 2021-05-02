package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korio.concurrent.lock.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class TouchEventHandler {
    @PublishedApi
    internal val lock = Lock()
    @PublishedApi
    internal val touchesEventPool = Pool { TouchEvent() }
    @PublishedApi
    internal var lastTouchEvent: TouchEvent = TouchEvent()

    inline fun handleEvent(gameWindow: GameWindow, coroutineContext: CoroutineContext, kind: TouchEvent.Type, emitter: (TouchEvent) -> Unit) {
        val currentTouchEvent = lock {
            val currentTouchEvent = touchesEventPool.alloc()
            currentTouchEvent.copyFrom(lastTouchEvent)
            currentTouchEvent.startFrame(kind)
            emitter(currentTouchEvent)
            currentTouchEvent.endFrame()

            lastTouchEvent.copyFrom(currentTouchEvent)
            currentTouchEvent
        }

        gameWindow.coroutineDispatcher.dispatch(coroutineContext, Runnable {
            gameWindow.dispatch(currentTouchEvent)
            lock { touchesEventPool.free(currentTouchEvent) }
        })
    }
}
