package com.soywiz.korge.debug

import com.soywiz.korge.view.views
import com.soywiz.korui.UiApplication
import com.soywiz.korui.UiContainer

open class UiEditableValue<T>(app: UiApplication, override val prop: ObservableProperty<T>) : UiContainer(app), ObservablePropertyHolder<T> {
    fun completedEditing() {
        app.views?.completedEditing(prop)
    }

    open fun hideEditor() {
        completedEditing()
    }

    open fun showEditor() {
    }
}
