package com.soywiz.korge.debug

import com.soywiz.korui.UiApplication
import com.soywiz.korui.UiComponent
import com.soywiz.korui.UiContainer
import com.soywiz.korui.UiLabel
import com.soywiz.korui.layout.HorizontalUiLayout
import com.soywiz.korui.layout.preferredSize
import com.soywiz.korui.layout.preferredWidth

class UiRowEditableValue(app: UiApplication, val labelText: String, val editor: UiComponent) : UiContainer(app) {
    val leftPadding = UiLabel(app)
    val label = UiLabel(app).apply {
        text = labelText
        preferredWidth = 50.percent
    }
    init {
        layout = HorizontalUiLayout
        leftPadding.preferredSize(16.pt, 32.pt)
        label.preferredSize(50.percent - 16.pt, 32.pt)
        editor.preferredSize(50.percent, 32.pt)
        //backgroundColor = Colors.RED
        addChild(leftPadding)
        addChild(label)
        addChild(editor)
        label.onClick {
            if (editor is UiEditableValue<*>) {
                editor.hideEditor()
            }
        }
        //addChild(UiLabel(app).also { it.text = "text" }.also { it.bounds = RectangleInt(120, 0, 120, 32) })
    }
}
