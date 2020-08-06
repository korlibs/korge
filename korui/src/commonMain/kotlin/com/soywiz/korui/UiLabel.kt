package com.soywiz.korui

interface UiLabel : UiComponent, UiText {
}

fun UiContainer.label(text: String = "Button", block: UiLabel.() -> Unit): UiLabel {
    return factory.createLabel().also { it.text = text }.also { it.parent = this }.also(block)
}
