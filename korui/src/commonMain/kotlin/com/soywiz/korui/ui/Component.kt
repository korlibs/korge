package com.soywiz.korui.ui

import com.soywiz.korev.*
import com.soywiz.korui.factory.*

open class Component(val app: Application, val component: NativeUiComponent) {
    val factory get() = app.factory

    var parent: Container? = null
        set(value) {
            field = value
            factory.setParent(component, value?.component)
        }

    fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        factory.setBounds(component, x, y, width, height)
    }

    var isVisible: Boolean
        get() = factory.getVisible(component)
        set(value) {
            factory.setVisible(component, value)
        }

    fun onClick(handler: (MouseEvent) -> Unit) {
        factory.addEventListener<MouseEvent>(component) {
            if (it.type == MouseEvent.Type.CLICK) handler(it)
        }
    }

    fun show() = run { isVisible = true }
    fun hide() = run { isVisible = false }
}
