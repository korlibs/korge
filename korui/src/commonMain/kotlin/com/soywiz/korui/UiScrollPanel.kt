package com.soywiz.korui

import com.soywiz.korui.native.*

open class UiScrollPanel(app: UiApplication, val panel: NativeUiFactory.NativeScrollPanel = app.factory.createScrollPanel()) : UiContainer(app, panel) {
}

inline fun UiContainer.scrollPanel(block: UiScrollPanel.() -> Unit): UiScrollPanel {
    return UiScrollPanel(app).also { it.parent = this }.also(block)
}
