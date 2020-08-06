package com.soywiz.korui.edit

import com.soywiz.korev.*
import com.soywiz.korui.*

class NumberUiEditableValue(app: UiApplication, var initial: Double) : UiEditableValue(app) {
    init {
        layout = UiFillLayout
        visible = true
    }

    val contentText = UiLabel(app).also { it.text = "$initial" }.also { it.visible = true }
    val contentTextField = UiTextField(app).also { it.text = contentText.text }.also { it.visible = false }

    override fun hideEditor() {
        contentText.visible = true
        contentTextField.visible = false
        contentText.text = contentTextField.text
    }

    override fun showEditor() {
        contentTextField.text = contentText.text
        contentText.visible = false
        contentTextField.visible = true
        contentTextField.select()
        contentTextField.focus()
    }

    init {
        contentText.onClick {
            showEditor()
        }
        contentTextField.onKeyEvent { e ->
            if (e.typeDown && e.key == Key.RETURN) {
                hideEditor()
            }
            //println(e)
        }
        contentTextField.onFocus { e ->
            if (e.typeBlur) {
                hideEditor()
            }
            //println(e)
        }
        var startX = 0
        var startY = 0
        contentText.onMouseEvent { e ->
            if (e.typeDown) {
                startX = e.x
                startY = e.y
                e.requestLock()
            }
            if (e.typeDrag) {
                val dx = e.x - startX
                val dy = e.y - startY
                contentText.text = "$dx"
                contentTextField.text = "$dx"
            }
        }
        contentText.cursor = UiStandardCursor.RESIZE_EAST
        addChild(contentText)
        addChild(contentTextField)
    }
}
