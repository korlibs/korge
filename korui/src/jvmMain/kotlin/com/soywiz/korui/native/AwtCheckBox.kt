package com.soywiz.korui.native

import com.soywiz.korio.lang.*
import javax.swing.*
import javax.swing.event.*

open class AwtCheckBox(factory: BaseAwtUiFactory, val checkBox: JCheckBox = JCheckBox()) : AwtComponent(factory, checkBox), NativeUiFactory.NativeCheckBox {
    override var text: String
        get() = checkBox.text
        set(value) = run { checkBox.text = value }
    override var checked: Boolean
        get() = checkBox.isSelected
        set(value) = run { checkBox.isSelected = value }

    override fun onChange(block: () -> Unit): Disposable {
        val listener = ChangeListener { block() }
        checkBox.addChangeListener(listener)
        return Disposable {
            checkBox.removeChangeListener(listener)
        }
    }
}
