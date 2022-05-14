package com.soywiz.korui

import com.soywiz.korio.util.RedirectMutableField
import com.soywiz.korui.native.NativeUiFactory

open class UiCheckBox(app: UiApplication, val checkBox: NativeUiFactory.NativeCheckBox = app.factory.createCheckBox()) : UiComponent(app, checkBox) {
    var text by RedirectMutableField(checkBox::text)
    var checked by RedirectMutableField(checkBox::checked)
    fun onChange(block: UiCheckBox.(Boolean) -> Unit) = checkBox.onChange { block(this, checked) }
}

inline fun UiContainer.checkBox(text: String = "CheckBox", checked: Boolean = false, block: UiCheckBox.() -> Unit = {}): UiCheckBox {
    return UiCheckBox(app)
        .also { it.text = text }
        .also { it.checked = checked }
        .also { it.parent = this }.also(block)
}
