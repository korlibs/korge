package com.soywiz.korui

import com.soywiz.korio.util.*
import com.soywiz.korui.native.*

open class UiComboBox<T>(app: UiApplication, val comboBox: NativeUiFactory.NativeComboBox<T> = app.factory.createComboBox()) : UiComponent(app, comboBox) {
    var items by RedirectMutableField(comboBox::items)
    var selectedItem by RedirectMutableField(comboBox::selectedItem)
    fun open() = comboBox.open()
    fun close() = comboBox.close()
    fun onChange(block: () -> Unit) = comboBox.onChange(block)
}

inline fun <T> UiContainer.comboBox(block: UiComboBox<T>.() -> Unit): UiComboBox<T> =
    UiComboBox<T>(app).also { it.parent = this }.also(block)
