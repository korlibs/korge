package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korui.native.*

internal open class UiToggleButton(app: UiApplication, val button: NativeUiFactory.NativeToggleButton = app.factory.createToggleButton()) : UiComponent(app, button) {
    var icon by button::icon
    var text by button::text
    var pressed by button::pressed
}

internal inline fun UiContainer.toggleButton(text: String = "Button", pressed: Boolean = false, noinline onClick: (UiToggleButton.(MouseEvent) -> Unit)? = null, block: UiToggleButton.() -> Unit = {}): UiToggleButton =
    UiToggleButton(app)
        .also { it.text = text }
        .also { it.parent = this }
        .also { it.pressed = pressed }
        .also { button -> if (onClick != null) button.onClick { button.onClick(it) }  }
        .also(block)
