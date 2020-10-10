package com.soywiz.korui

import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiCheckBox(app: UiApplication, val checkBox: NativeUiFactory.NativeCheckBox = app.factory.createCheckBox()) : UiComponent(app, checkBox) {
    var text by RedirectMutableField(checkBox::text)
    var checked by RedirectMutableField(checkBox::checked)
    fun onChange(block: () -> Unit) = checkBox.onChange(block)
}

inline fun UiContainer.checkBox(text: String = "CheckBox", checked: Boolean = false, block: UiCheckBox.() -> Unit = {}): UiCheckBox {
    return UiCheckBox(app)
        .also { it.text = text }
        .also { it.checked = checked }
        .also { it.parent = this }.also(block)
}
