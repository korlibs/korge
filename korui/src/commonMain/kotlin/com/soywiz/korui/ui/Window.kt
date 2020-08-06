package com.soywiz.korui.ui

open class Window(application: Application) : Container(application, application.factory.createWindow()) {
    init {
        setBounds(0, 0, 300, 300)
    }

    var title: String
        get() = factory.getText(component) ?: ""
        set(value) {
            factory.setText(component, value)
        }
}
