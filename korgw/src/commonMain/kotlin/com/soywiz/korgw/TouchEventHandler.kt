package com.soywiz.korgw

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.korev.*

class TouchEventHandler {
    @PublishedApi
    internal val lock = NonRecursiveLock()
    @PublishedApi
    internal val touchesEventPool = Pool { TouchEvent() }
    @PublishedApi
    internal var lastTouchEvent: TouchEvent = TouchEvent()

    inline fun handleEvent(gameWindow: GameWindow, kind: TouchEvent.Type, emitter: (TouchEvent) -> Unit) {
        val currentTouchEvent = lock {
            val currentTouchEvent = touchesEventPool.alloc()
            currentTouchEvent.copyFrom(lastTouchEvent)
            currentTouchEvent.startFrame(kind)
            emitter(currentTouchEvent)
            currentTouchEvent.endFrame()

            lastTouchEvent.copyFrom(currentTouchEvent)
            currentTouchEvent
        }

        gameWindow.queue {
            try {
                gameWindow.dispatch(currentTouchEvent)
            } finally {
                lock { touchesEventPool.free(currentTouchEvent) }
            }
        }
    }
}
