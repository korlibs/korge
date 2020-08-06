package com.soywiz.korui

interface UiButton : UiComponent, UiText {
}

fun UiContainer.button(text: String = "Button", block: UiButton.() -> Unit): UiButton {
    return factory.createButton().also { it.text = text }.also { it.setParent(this) }.also(block)
}
