package com.soywiz.korui.native

import javax.swing.*

open class AwtButton(factory: AwtUiFactory, val button: JButton = JButton()) : AwtComponent(factory, button), NativeUiFactory.NativeButton {
    override var text: String
        get() = button.text
        set(value) = run { button.text = value }
}
