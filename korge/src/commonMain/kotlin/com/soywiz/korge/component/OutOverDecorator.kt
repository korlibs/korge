package com.soywiz.korge.component

import com.soywiz.korge.input.mouse
import com.soywiz.korge.view.View

fun <T : View> T.decorateOutOver(onEvent: (view: T, over: Boolean) -> Unit = { view, over -> }): T {
    val view = this
    onEvent(view, false)
    mouse {
        this.over { onEvent(view, true) }
        this.out { if (it.input.numActiveTouches == 0) onEvent(view, false) }
        this.upOutside { onEvent(view, false) }
    }
    return this
}

fun <T : View> T.decorateOutOverAlpha(alpha: (over: Boolean) -> Float = { if (it) 1.0f else 0.75f }): T {
    return decorateOutOver { view, over -> view.alpha = alpha(over) }
}
