package com.soywiz.korui

interface UiButton : UiComponent, UiWithText {
}

inline fun UiContainer.button(text: String = "Button", block: UiButton.() -> Unit): UiButton =
    factory.createButton().also { it.text = text }.also { it.parent = this }.also(block)
