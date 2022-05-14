package com.soywiz.korui

import com.soywiz.korui.native.NativeUiFactory

open class UiWindow(app: UiApplication, val window: NativeUiFactory.NativeWindow = app.factory.createWindow()) : UiContainer(app, window) {
    var title by window::title
    var menu by window::menu
    val pixelFactory by window::pixelFactor
}
