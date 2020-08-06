package com.soywiz.korui

interface UiContainer : UiComponent {
}

fun UiContainer.container(block: UiContainer.() -> Unit): UiContainer {
    return factory.createContainer().also { it.setParent(this) }.also(block)
}
