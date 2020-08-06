package com.soywiz.korui

import javax.swing.*

open class AwtLabel(factory: AwtUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), UiLabel {
    override var text: String
        get() = label.text
        set(value) = run { label.text = value }
}
