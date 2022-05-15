package com.soywiz.korge.component

import com.soywiz.korge.input.mouse
import com.soywiz.korge.view.View

@Deprecated("Use decorateOutOverSimple")
fun <T : View> T.decorateOutOver(onOver: (T) -> Unit = { }, onOut: (T) -> Unit = { }): T {
    onOut(this)
    mouse {
        this.over { onOver(this@decorateOutOver) }
        this.out {
            if (it.input.numActiveTouches == 0) onOut(this@decorateOutOver)
        }
        this.upOutside { onOut(this@decorateOutOver) }
    }
    return this
}

fun <T : View> T.decorateOutOverAlpha(overAlpha: () -> Double = { 1.0 }, outAlpha: () -> Double = { 0.75 }): T {
    return decorateOutOverSimple()
}

fun <T : View> T.decorateOutOverSimple(onEvent: (view: T, over: Boolean) -> Unit = { view, over -> }): T {
    val view = this
    onEvent(view, false)
    mouse {
        this.over { onEvent(view, true) }
        this.out { if (it.input.numActiveTouches == 0) onEvent(view, false) }
        this.upOutside { onEvent(view, false) }
    }
    return this
}

fun <T : View> T.decorateOutOverAlphaSimple(alpha: (over: Boolean) -> Double = { if (it) 1.0 else 0.75 }): T {
    return decorateOutOverSimple { view, over -> view.alpha = alpha(over) }
}
