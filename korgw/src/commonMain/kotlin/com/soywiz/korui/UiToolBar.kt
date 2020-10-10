package com.soywiz.korui

import com.soywiz.korim.bitmap.*
import com.soywiz.korui.layout.*
import com.soywiz.korui.native.*

open class UiToolBar(app: UiApplication, val canvas: NativeUiFactory.NativeToolbar = app.factory.createToolbar()) : UiComponent(app, canvas) {
}

inline fun UiContainer.toolbar(block: UiToolBar.() -> Unit): UiToolBar {
    return UiToolBar(app).also { it.parent = this }.also(block)
}
