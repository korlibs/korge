package com.soywiz.korge.debug

import com.soywiz.korge.view.*
import com.soywiz.korge.view.property.*
import com.soywiz.korui.*

fun <T> Views.completedEditing(prop: ObservableProperty<T>) {
    debugSaveView("Adjusted ${prop.name}", null)
    completedEditing(Unit)
}


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
