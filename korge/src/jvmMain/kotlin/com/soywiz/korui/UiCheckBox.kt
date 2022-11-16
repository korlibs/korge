package com.soywiz.korui

import com.soywiz.korui.native.*

internal open class UiCheckBox(app: UiApplication, val checkBox: NativeUiFactory.NativeCheckBox = app.factory.createCheckBox()) : UiComponent(app, checkBox) {
    var text by checkBox::text
    var checked by checkBox::checked
    fun onChange(block: UiCheckBox.(Boolean) -> Unit) = checkBox.onChange { block(this, checked) }
}

internal inline fun UiContainer.checkBox(text: String = "CheckBox", checked: Boolean = false, block: UiCheckBox.() -> Unit = {}): UiCheckBox {
    return UiCheckBox(app)
        .also { it.text = text }
        .also { it.checked = checked }
        .also { it.parent = this }.also(block)
}
