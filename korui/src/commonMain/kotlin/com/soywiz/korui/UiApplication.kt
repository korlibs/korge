package com.soywiz.korui

import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*

class UiApplication(val factory: UiFactory = DEFAULT_UI_FACTORY) {
    fun window(width: Int = 300, height: Int = 300, block: UiWindow.() -> Unit): UiWindow = factory.createWindow()
        .also { it.setBounds(0, 0, width, height) }
        .also { it.layout = LineUiLayout(it, LayoutDirection.VERTICAL) }
        .also(block)
        .also { it.visible = true }
        .also { window -> window.onResize { window.layout.relayout(RectangleInt(0, 0, it.width, it.height)) } }
        .also { it.layout.relayout(RectangleInt(0, 0, 600, 600)) }
}
