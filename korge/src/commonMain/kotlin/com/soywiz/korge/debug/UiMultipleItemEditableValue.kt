package com.soywiz.korge.debug

import com.soywiz.korui.*
import com.soywiz.korui.layout.*

class UiMultipleItemEditableValue<T>(app: UiApplication, items: List<UiEditableValue<T>>) : UiEditableValue<T>(app, items.first().prop) {
    init {
        layout = HorizontalUiLayout
        this.preferredWidth = 100.percent
        for (item in items) {
            item.preferredWidth = (100 / items.size).percent
            addChild(item)
        }
    }
}
