package korlibs.korge.component

import korlibs.korge.input.*
import korlibs.korge.view.*

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

fun <T : View> T.decorateOutOverAlpha(alpha: (over: Boolean) -> Double = { if (it) 1.0 else 0.75 }): T {
    return decorateOutOver { view, over -> view.alpha = alpha(over) }
}
