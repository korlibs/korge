package com.soywiz.korui.native

import com.soywiz.korim.bitmap.*
import com.soywiz.korui.native.util.*
import javax.swing.*

open class AwtLabel(factory: AwtUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), NativeUiFactory.NativeLabel {
    override var text: String
        get() = label.text
        set(value) = run { label.text = value }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            label.icon = value?.toAwtIcon()
        }
}
