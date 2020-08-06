package com.soywiz.korui.ui

open class Label(app: Application) : Component(app, app.factory.createLabel()) {
    init {
        setBounds(0, 0, 120, 32)
    }

    var text: String
        get() = factory.getText(component) ?: ""
        set(value) {
            factory.setText(component, value)
        }
}

fun Container.label(text: String = "Label", block: Label.() -> Unit): Label {
    return Label(app)
        .also { it.text = text }
        .also { addChild(it) }
        .also(block)
}
