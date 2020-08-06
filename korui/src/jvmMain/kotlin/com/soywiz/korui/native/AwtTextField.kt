package com.soywiz.korui.native

import javax.swing.*

open class AwtTextField(factory: AwtUiFactory, val textField: JTextField = JTextField()) : AwtComponent(factory, textField), NativeUiFactory.NativeTextField {
    override var text: String
        get() = textField.text
        set(value) = run { textField.text = value }
}
