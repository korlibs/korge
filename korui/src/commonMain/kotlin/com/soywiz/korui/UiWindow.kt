package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiWindow(app: UiApplication, val window: NativeUiFactory.NativeWindow = app.factory.createWindow()) : UiContainer(app, window) {
    var title by RedirectMutableField(window::title)
    var menu by RedirectMutableField(window::menu)
    fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = window.onResize(handler)
}
