package com.soywiz.korui

interface UiTextField : UiComponent, UiText {
}

fun UiContainer.textField(text: String = "Button", block: UiTextField.() -> Unit): UiTextField {
    return factory.createTextField().also { it.text = text }.also { it.parent = this }.also(block)
}
