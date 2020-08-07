package com.soywiz.korge.debug

import com.soywiz.korui.*

class ListUiEditableValue<T>(
    app: UiApplication,
    items: List<T>,
    val initial: T
) : UiEditableValue(app) {
    init {
        layout = UiFillLayout
        visible = true
    }

    var items = items
    val contentText = UiLabel(app)
        .also { it.text = "" }
        .also { it.visible = true }
    val contentComboBox = UiComboBox<T>(app)
        .also { it.items = items }
        .also { it.visible = false }

    fun setValue(value: T?) {
        contentText.text = value.toString()
        if (contentComboBox.selectedItem != value) {
            contentComboBox.selectedItem = value
        }
    }

    override fun hideEditor() {
        contentText.visible = true
        contentComboBox.visible = false
        setValue(contentComboBox.selectedItem)
    }

    override fun showEditor() {
        contentText.visible = false
        contentComboBox.visible = true
        contentComboBox.focus()
    }

    init {
        setValue(initial)

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
