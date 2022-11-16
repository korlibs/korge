package com.soywiz.korui

import com.soywiz.korui.native.*

internal open class UiComboBox<T>(app: UiApplication, val comboBox: NativeUiFactory.NativeComboBox<T> = app.factory.createComboBox()) : UiComponent(app, comboBox) {
    var items by comboBox::items
    var selectedItem by comboBox::selectedItem
    fun open() = comboBox.open()
    fun close() = comboBox.close()
    fun onChange(block: () -> Unit) = comboBox.onChange(block)
}

internal inline fun <T> UiContainer.comboBox(selectedItem: T, items: List<T>, block: UiComboBox<T>.() -> Unit = {}): UiComboBox<T> =
    UiComboBox<T>(app)
        .also { it.parent = this }
        .also { it.items = items }
        .also { it.selectedItem = selectedItem }
        .also(block)
