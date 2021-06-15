package com.soywiz.korge.component

import com.soywiz.korge.input.*
import com.soywiz.korge.view.*

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
    return decorateOutOver(onOver = { it.alpha = overAlpha() }, onOut = { it.alpha = outAlpha() })
}
