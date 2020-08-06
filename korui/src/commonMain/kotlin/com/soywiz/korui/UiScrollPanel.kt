package com.soywiz.korui

import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiScrollPanel(app: UiApplication, val panel: NativeUiFactory.NativeScrollPanel = app.factory.createScrollPanel()) : UiContainer(app, panel) {
    var xbar by redirect(panel::xbar)
    var ybar by redirect(panel::ybar)
}

inline fun UiContainer.scrollPanel(xbar: Boolean? = null, ybar: Boolean? = null, block: UiScrollPanel.() -> Unit): UiScrollPanel {
    return UiScrollPanel(app)
        .also { it.parent = this }
        .also { it.xbar = xbar }
        .also { it.ybar = ybar }
        .also(block)
}
