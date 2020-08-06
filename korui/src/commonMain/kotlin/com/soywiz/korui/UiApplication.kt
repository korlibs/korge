package com.soywiz.korui

import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*

class UiApplication(val factory: UiFactory = DEFAULT_UI_FACTORY) {
    fun window(block: UiWindow.() -> Unit): UiWindow = factory.createWindow()
        .also { it.layout = LineUiLayout(it, LayoutDirection.VERTICAL) }
        .also(block)
        .also { it.visible = true }
        .also { window -> window.onResize { window.layout.relayout(RectangleInt(0, 0, it.width, it.height)) } }
        .also { it.layout.relayout(RectangleInt(0, 0, 600, 600)) }
}
