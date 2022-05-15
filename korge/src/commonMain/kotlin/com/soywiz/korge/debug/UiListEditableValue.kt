package com.soywiz.korge.debug

import com.soywiz.korui.UiApplication
import com.soywiz.korui.UiComboBox
import com.soywiz.korui.UiLabel
import com.soywiz.korui.layout.UiFillLayout

class UiListEditableValue<T>(
    app: UiApplication,
    val itemsFactory: () -> List<T>,
    prop: ObservableProperty<T>
) : UiEditableValue<T>(app, prop) {
    init {
        layout = UiFillLayout
        visible = true
    }

    var items = itemsFactory()
    val contentText = UiLabel(app)
        .also { it.text = "" }
        .also { it.visible = true }
    val contentComboBox = UiComboBox<T>(app)
        .also { it.items = items }
        .also { it.visible = false }

    fun setValue(value: T, setProperty: Boolean = true) {
        contentText.text = value.toString()
        if (!contentComboBox.visible) {
            contentComboBox.selectedItem = value
        }
        if (setProperty) prop.value = value
    }

    override fun hideEditor() {
        if (!contentText.visible) {
            val selectedItem = contentComboBox.selectedItem
            //println("UiListEditableValue.hideEditor.selectedItem: $selectedItem")
            contentText.visible = true
            contentComboBox.visible = false
            if (selectedItem != null) {
                setValue(selectedItem)
            }
            super.hideEditor()
        }
    }

    override fun showEditor() {
        contentText.visible = false
        contentComboBox.visible = true
        contentComboBox.focus()
    }

    init {
        setValue(prop.value)

        prop.onChange {
            items = itemsFactory()
            setValue(it, false)
        }

        contentText.onClick {
            showEditor()
        }

        contentComboBox.onChange {
            hideEditor()
        }
        contentComboBox.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            } else {
                contentComboBox.open()
            }
            //println(e)
        }
        addChild(contentText)
        addChild(contentComboBox)
    }
}
