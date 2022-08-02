package com.soywiz.korui

import com.soywiz.korui.native.NativeUiFactory

open class UiLabel(app: UiApplication, val label: NativeUiFactory.NativeLabel = app.factory.createLabel()) : UiComponent(app, label) {
    var text by label::text
    var icon by label::icon

    override fun copyFrom(that: UiComponent) {
        super.copyFrom(that)
        that as UiLabel
        this.text = that.text
    }
}

inline fun UiContainer.label(text: String = "Button", block: UiLabel.() -> Unit = {}): UiLabel {
    return UiLabel(app).also { it.text = text }.also { it.parent = this }.also(block)
}
