package com.soywiz.korge.ui

import com.soywiz.kds.getCyclicOrNull
import com.soywiz.korev.Key
import com.soywiz.korev.KeyEvent
import com.soywiz.korev.SoftKeyboardConfig
import com.soywiz.korev.ISoftKeyboardConfig
import com.soywiz.korge.annotations.KorgeExperimental
import com.soywiz.korge.component.KeyComponent
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korge.view.descendantsOfType

@KorgeExperimental
interface UIFocusable {
    val UIFocusManager.focusView: View
    var tabIndex: Int
    var focused: Boolean
}

@KorgeExperimental
fun UIFocusable.focus() { focused = true }
@KorgeExperimental
fun UIFocusable.blur() { focused = false }

@KorgeExperimental
class UIFocusManager(override val view: Stage) : KeyComponent {
    val stage = view
    val gameWindow get() = view.gameWindow
    var uiFocusedView: UIFocusable? = null

    //private var toggleKeyboardTimeout: Closeable? = null

    fun requestToggleSoftKeyboard(show: Boolean, view: UIFocusable?) {
        //toggleKeyboardTimeout?.close()
        //toggleKeyboardTimeout = stage.timeout(1.seconds) {
            if (show) {
                if (view != null) {
                    view.apply {
                        gameWindow.setInputRectangle(this@UIFocusManager.focusView.getWindowBounds(stage))
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
            val focusables = stage.descendantsOfType<UIFocusable>()
            val sortedFocusables = focusables.sortedBy { it.tabIndex }
            val index = sortedFocusables.indexOf(uiFocusedView).takeIf { it >= 0 }
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
