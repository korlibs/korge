package com.soywiz.korui

import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*

class UiApplication(val factory: NativeUiFactory = DEFAULT_UI_FACTORY) {
    fun window(width: Int = 300, height: Int = 300, block: UiWindow.() -> Unit): UiWindow = UiWindow(this)
        .also { it.bounds = RectangleInt(0, 0, width, height) }
        .also { it.layout = VerticalUiLayout }
        .also(block)
        .also { it.visible = true }
        .also { window -> window.onResize { window.layout?.relayout(window) } }
        .also { it.relayout() }

    fun wrapContainer(native: Any?): UiContainer = UiContainer(this, factory.wrapNativeContainer(native)).also { container ->
        container.onResize {
            //println("wrapContainer.container.onResize: ${container.bounds}")
            container.relayout()
        }
    }
}
