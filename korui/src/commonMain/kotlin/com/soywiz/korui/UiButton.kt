package com.soywiz.korui

import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiButton(app: UiApplication, val button: NativeUiFactory.NativeButton = app.factory.createButton()) : UiComponent(app, button) {
    var text by RedirectMutableField(button::text)
}

inline fun UiContainer.button(text: String = "Button", block: UiButton.() -> Unit): UiButton =
    UiButton(app).also { it.text = text }.also { it.parent = this }.also(block)
