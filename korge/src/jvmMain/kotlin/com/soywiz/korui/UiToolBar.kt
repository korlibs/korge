package com.soywiz.korui

import com.soywiz.korui.native.*

internal open class UiToolBar(app: UiApplication, val canvas: NativeUiFactory.NativeToolbar = app.factory.createToolbar()) : UiComponent(app, canvas) {
}

internal inline fun UiContainer.toolbar(block: UiToolBar.() -> Unit): UiToolBar {
    return UiToolBar(app).also { it.parent = this }.also(block)
}
