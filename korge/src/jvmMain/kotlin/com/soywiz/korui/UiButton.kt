package com.soywiz.korui

import com.soywiz.korev.*
import com.soywiz.korui.native.*

internal open class UiButton(app: UiApplication, val button: NativeUiFactory.NativeButton = app.factory.createButton()) : UiComponent(app, button) {
    var icon by button::icon
    var text by button::text
}

internal inline fun UiContainer.button(text: String = "Button", noinline onClick: (UiButton.(MouseEvent) -> Unit)? = null, block: UiButton.() -> Unit = {}): UiButton =
    UiButton(app)
        .also { it.text = text }
        .also { it.parent = this }
        .also { button -> if (onClick != null) button.onClick { onClick(button, it) }  }
        .also(block)
