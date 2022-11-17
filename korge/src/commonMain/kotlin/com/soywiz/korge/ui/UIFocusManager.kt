@file:OptIn(KorgeExperimental::class)

package com.soywiz.korge.ui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.component.*
import com.soywiz.korge.view.*
import kotlin.native.concurrent.*

@KorgeExperimental
interface UIFocusable {
    val UIFocusManager.Scope.focusView: View
    var tabIndex: Int
    fun focusChanged(value: Boolean)
}
var UIFocusable.focused: Boolean
    get() = UIFocusManager.Scope.focusView.stage?.uiFocusedView == UIFocusManager.Scope.focusView
    set(value) {
        if (value) UIFocusManager.Scope.focusView.stage?.uiFocusManager?.uiFocusedView = this
    }

@ThreadLocal
private var View._focusable: UIFocusable? by extraProperty { null }

var View.focusable: UIFocusable?
    get() = (this as? UIFocusable?) ?: _focusable
    set(value) {
        _focusable = value
    }

@KorgeExperimental
fun UIFocusable.focus() { focused = true }
@KorgeExperimental
fun UIFocusable.blur() { focused = false }

@KorgeExperimental
class UIFocusManager(override val view: Stage) : KeyComponent {
    object Scope

    val stage = view
    val gameWindow get() = view.gameWindow
    var uiFocusedView: UIFocusable? = null
        set(value) {
            if (field == value) return
            field?.focusChanged(false)
            field = value
            field?.focusChanged(true)
        }

    //private var toggleKeyboardTimeout: Closeable? = null

    fun requestToggleSoftKeyboard(show: Boolean, view: UIFocusable?) {
        //toggleKeyboardTimeout?.close()
        //toggleKeyboardTimeout = stage.timeout(1.seconds) {
            if (show) {
                if (view != null) {
                    view.apply {
                        gameWindow.setInputRectangle(Scope.focusView.getWindowBounds(stage))
                    }
                }
                gameWindow.showSoftKeyboard(config = view as? ISoftKeyboardConfig?)
            } else {
                gameWindow.hideSoftKeyboard()
            }
        //}
    }

    override fun Views.onKeyEvent(event: KeyEvent) {
        if (event.type == KeyEvent.Type.DOWN && event.key == Key.TAB) {
            val shift = event.shift
            val dir = if (shift) -1 else +1
            //val focusables = stage.descendantsOfType<UIFocusable>()
            val focusables = stage
                .descendantsWith { it.focusable != null }
                .mapNotNull { it.focusable }
            val sortedFocusables = focusables.sortedBy { it.tabIndex }
            val index = sortedFocusables.indexOf(uiFocusedView).takeIf { it >= 0 }
            //println("sortedFocusables=$sortedFocusables, index=$index, dir=$dir")
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
val Stage.uiFocusManager get() = this.getOrCreateComponentKey { UIFocusManager(this) }
@KorgeExperimental
var Stage.uiFocusedView: UIFocusable?
    get() = uiFocusManager.uiFocusedView
    set(value) { uiFocusManager.uiFocusedView = value }
