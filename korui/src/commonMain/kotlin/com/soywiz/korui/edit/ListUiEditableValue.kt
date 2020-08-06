package com.soywiz.korui.edit

import com.soywiz.korui.*

class ListUiEditableValue<T>(app: UiApplication, items: List<T>) : UiEditableValue(app) {
    init {
        layout = UiFillLayout
        visible = true
    }

    val contentText = UiLabel(app).also { it.text = "world" }.also { it.visible = true }
    val contentComboBox = UiComboBox<T>(app).also { it.items =items }.also { it.visible = false }

    override fun hideEditor() {
        contentText.visible = true
        contentComboBox.visible = false
    }

    override fun showEditor() {
        contentText.visible = false
        contentComboBox.visible = true
        contentComboBox.focus()
    }

    init {
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
