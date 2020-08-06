package com.soywiz.korui

import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*

class UiApplication(val factory: NativeUiFactory = DEFAULT_UI_FACTORY) {
    fun window(width: Int = 300, height: Int = 300, block: UiWindow.() -> Unit): UiWindow = UiWindow(this)
        .also { it.bounds = RectangleInt(0, 0, width, height) }
        .also { it.layout = LineUiLayout(it, LayoutDirection.VERTICAL) }
        .also(block)
        .also { it.visible = true }
        .also { window -> window.onResize { window.layout.relayout(RectangleInt(0, 0, it.width, it.height)) } }
        .also { it.layout.relayout(RectangleInt(0, 0, 600, 600)) }
}
