package com.soywiz.korge.debug

import com.soywiz.korui.*

class UiListEditableValue<T>(
    app: UiApplication,
    val itemsFactory: () -> List<T>,
    val prop: ObservableProperty<T>
) : UiEditableValue(app) {
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
        if (contentComboBox.selectedItem != value) {
            contentComboBox.selectedItem = value
            if (setProperty) prop.value = value
        }
    }

    override fun hideEditor() {
        contentText.visible = true
        contentComboBox.visible = false
        setValue(contentComboBox.selectedItem ?: items.first())
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
