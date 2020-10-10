package com.soywiz.korui.native

import com.soywiz.korim.bitmap.*
import com.soywiz.korui.native.util.*
import java.awt.*
import javax.swing.*

open class AwtCanvas(factory: BaseAwtUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), NativeUiFactory.NativeCanvas {
    override var image: Bitmap? = null
        set(value) {
            field = value
            label.icon = value?.toAwtIcon()
        }
}
