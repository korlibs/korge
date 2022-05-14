package com.soywiz.korui

import com.soywiz.korui.native.NativeUiFactory

open class UiToolBar(app: UiApplication, val canvas: NativeUiFactory.NativeToolbar = app.factory.createToolbar()) : UiComponent(app, canvas) {
}

inline fun UiContainer.toolbar(block: UiToolBar.() -> Unit): UiToolBar {
    return UiToolBar(app).also { it.parent = this }.also(block)
}
