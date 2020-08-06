package com.soywiz.korui

import javax.swing.*

open class AwtCheckBox(factory: AwtUiFactory, val checkBox: JCheckBox = JCheckBox()) : AwtComponent(factory, checkBox), UiCheckBox {
    override var text: String
        get() = checkBox.text
        set(value) = run { checkBox.text = value }
    override var checked: Boolean
        get() = checkBox.isSelected
        set(value) = run { checkBox.isSelected = value }

    override fun copyFrom(nchild: UiComponent) {
        super<AwtComponent>.copyFrom(nchild)
        nchild as UiCheckBox
        this.text = nchild.text
        this.checked = nchild.checked
    }
}
