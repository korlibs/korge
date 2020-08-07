package com.soywiz.korge.debug

import com.soywiz.korui.*
import com.soywiz.korui.layout.*

class RowUiEditableValue(app: UiApplication, val labelText: String, val editor: UiEditableValue) : UiContainer(app) {
    init {
        //this.bounds = RectangleInt(0, 0, 240, 32)
        layout = HorizontalUiLayout
    }
    val label = UiLabel(app).apply {
        text = labelText
        preferredWidth = 50.percent
    }
    init {
        editor.preferredWidth = 50.percent
        //backgroundColor = Colors.RED
        addChild(UiLabel(app).apply {
            preferredWidth = 16.pt
        })
        addChild(label)
        addChild(editor)
        label.onClick {
            editor.hideEditor()
        }
        //addChild(UiLabel(app).also { it.text = "text" }.also { it.bounds = RectangleInt(120, 0, 120, 32) })
    }
}
