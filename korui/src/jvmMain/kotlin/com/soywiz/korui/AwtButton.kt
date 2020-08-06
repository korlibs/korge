package com.soywiz.korui

import javax.swing.*

open class AwtButton(factory: AwtUiFactory, val button: JButton = JButton()) : AwtComponent(factory, button), UiButton {
    override var text: String
        get() = button.text
        set(value) = run { button.text = value }
}
