package com.soywiz.korui

import javax.swing.*

open class AwtTextField(factory: AwtUiFactory, val textField: JTextField = JTextField()) : AwtComponent(factory, textField), UiTextField {
    override var text: String
        get() = textField.text
        set(value) = run { textField.text = value }
}
