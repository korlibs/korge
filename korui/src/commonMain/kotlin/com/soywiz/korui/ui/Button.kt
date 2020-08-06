package com.soywiz.korui.ui

open class Button(app: Application) : Component(app, app.factory.createButton()) {
    init {
        setBounds(0, 0, 120, 32)
    }

    var text: String
        get() = factory.getText(component) ?: ""
        set(value) {
            factory.setText(component, value)
        }
}

fun Container.button(text: String = "Button", block: Button.() -> Unit): Button {
    return Button(app)
        .also { it.text = text }
        .also { addChild(it) }
        .also(block)
}
