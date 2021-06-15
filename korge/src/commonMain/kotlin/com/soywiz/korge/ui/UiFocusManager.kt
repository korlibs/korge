package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korev.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*

@KorgeExperimental
interface UiFocusable {
    var tabIndex: Int
    var focused: Boolean
}

@KorgeExperimental
fun UiFocusable.focus() { focused = true }
@KorgeExperimental
fun UiFocusable.blur() { focused = false }

@KorgeExperimental
class UiFocusManager(override val view: Stage) : KeyComponent {
    var uiFocusedView: UIView? = null

    override fun Views.onKeyEvent(event: KeyEvent) {
        if (event.type == KeyEvent.Type.DOWN && event.key == Key.TAB) {
            val shift = event.shift
            val dir = if (shift) -1 else +1
            val focusables = stage.descendantsOfType<UiFocusable>()
            val sortedFocusables = focusables.sortedBy { it.tabIndex }
            val index = sortedFocusables.indexOf(uiFocusedView as? UiFocusable?).takeIf { it >= 0 }
            sortedFocusables
                .getCyclicOrNull(
                    when {
                        index != null -> index + dir
                        shift -> -1
                        else -> 0
                    }
                )
                ?.focus()

            //println("FOCUS MANAGER TAB shift=$shift")
            //for (view in listOf(uiFocusedView, if (shift) stage.lastTreeView else stage)) {
            //    //println("  view=$view")
            //    val nview = when {
            //        view != uiFocusedView && shift && view is UiFocusable -> view
            //        shift -> view?.prevViewOfType<UiFocusable>()
            //        else -> view?.nextViewOfType<UiFocusable>()
            //    }
            //    if (nview != null) {
            //        nview.focus()
            //        break
            //    }
            //}
        }
    }
}

@KorgeExperimental
val Stage.uiFocusManager get() = this.getOrCreateComponentKey { UiFocusManager(this) }
@KorgeExperimental
var Stage.uiFocusedView: UIView?
    get() = uiFocusManager.uiFocusedView
    set(value) { uiFocusManager.uiFocusedView = value }
