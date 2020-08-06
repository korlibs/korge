package com.soywiz.korui

import javax.swing.*

open class AwtComboBox<T>(factory: AwtUiFactory, val comboBox: JComboBox<T> = JComboBox<T>()) : AwtComponent(factory, comboBox), UiComboBox<T> {
    override var items: List<T>
        get() {
            val model = comboBox.model
            return (0 until model.size).map { model.getElementAt(it) }
        }
        set(value) {
            comboBox.model = DefaultComboBoxModel((value as List<Any>).toTypedArray()) as DefaultComboBoxModel<T>
        }

    override var selectedItem: T?
        get() = comboBox.selectedItem as T?
        set(value) = run { comboBox.selectedItem = value }

}
