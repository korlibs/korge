package com.soywiz.korui.native

import com.soywiz.korim.bitmap.*
import com.soywiz.korui.native.util.*
import javax.swing.*

open class AwtButton(factory: BaseAwtUiFactory, val button: JButton = JButton()) : AwtComponent(factory, button), NativeUiFactory.NativeButton {
    override var text: String
        get() = button.text
        set(value) { button.text = value }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            button.icon = value?.toAwtIcon()
        }
}
