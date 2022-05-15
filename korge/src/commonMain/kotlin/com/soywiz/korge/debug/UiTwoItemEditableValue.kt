package com.soywiz.korge.debug

import com.soywiz.korui.UiApplication
import com.soywiz.korui.layout.HorizontalUiLayout
import com.soywiz.korui.layout.preferredWidth

class UiTwoItemEditableValue<T>(app: UiApplication, left: UiEditableValue<T>, right: UiEditableValue<T>) : UiEditableValue<T>(app, left.prop) {
    init {
        layout = HorizontalUiLayout
        this.preferredWidth = 100.percent
        left.preferredWidth = 50.percent
        right.preferredWidth = 50.percent
        addChild(left)
        addChild(right)
    }
}
