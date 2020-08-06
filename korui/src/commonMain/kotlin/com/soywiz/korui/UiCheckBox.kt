package com.soywiz.korui

interface UiCheckBox : UiComponent, UiWithText {
}

inline fun UiContainer.checkBox(text: String = "CheckBox", checked: Boolean = false, block: UiCheckBox.() -> Unit): UiCheckBox {
    return factory.createCheckBox()
        .also { it.text = text }
        .also { it.checked = checked }
        .also { it.parent = this }.also(block)
}
