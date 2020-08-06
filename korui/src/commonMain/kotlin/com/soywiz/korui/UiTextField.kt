package com.soywiz.korui

import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiTextField(app: UiApplication, val textField: NativeUiFactory.NativeTextField = app.factory.createTextField()) : UiComponent(app, textField) {
    var text by RedirectMutableField(textField::text)
}

inline fun UiContainer.textField(text: String = "Button", block: UiTextField.() -> Unit): UiTextField {
    return UiTextField(app).also { it.text = text }.also { it.parent = this }.also(block)
}
