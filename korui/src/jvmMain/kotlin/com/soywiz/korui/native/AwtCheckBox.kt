package com.soywiz.korui.native

import javax.swing.*

open class AwtCheckBox(factory: AwtUiFactory, val checkBox: JCheckBox = JCheckBox()) : AwtComponent(factory, checkBox), NativeUiFactory.NativeCheckBox {
    override var text: String
        get() = checkBox.text
        set(value) = run { checkBox.text = value }
    override var checked: Boolean
        get() = checkBox.isSelected
        set(value) = run { checkBox.isSelected = value }
}
