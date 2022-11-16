package com.soywiz.korge.debug

import com.soywiz.korui.*
import com.soywiz.korui.layout.*

internal class UiTwoItemEditableValue<T>(app: UiApplication, left: UiEditableValue<T>, right: UiEditableValue<T>) : UiEditableValue<T>(app, left.prop) {
    init {
        layout = HorizontalUiLayout
        this.preferredWidth = 100.percent
        left.preferredWidth = 50.percent
        right.preferredWidth = 50.percent
        addChild(left)
        addChild(right)
    }
}

internal class UiFourItemEditableValue<T>(app: UiApplication, a: UiEditableValue<T>, b: UiEditableValue<T>, c: UiEditableValue<T>, d: UiEditableValue<T>) : UiEditableValue<T>(app, a.prop) {
    init {
        layout = HorizontalUiLayout
        this.preferredWidth = 100.percent
        a.preferredWidth = 25.percent
        b.preferredWidth = 25.percent
        c.preferredWidth = 25.percent
        d.preferredWidth = 25.percent
        addChild(a)
        addChild(b)
        addChild(c)
        addChild(d)
    }
}
