package com.soywiz.korui.native

import javax.swing.*

open class AwtLabel(factory: AwtUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), NativeUiFactory.NativeLabel {
    override var text: String
        get() = label.text
        set(value) = run { label.text = value }
}
