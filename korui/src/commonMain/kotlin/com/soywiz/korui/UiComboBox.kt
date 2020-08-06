package com.soywiz.korui

interface UiComboBox<T> : UiComponent {
    var items: List<T>
        get() = listOf()
        set(value) = Unit

    var selectedItem: T?
        get() = null
        set(value) = Unit
}

inline fun <T> UiContainer.comboBox(block: UiComboBox<T>.() -> Unit): UiComboBox<T> =
    factory.createComboBox<T>().also { it.parent = this }.also(block)
