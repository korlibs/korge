package com.soywiz.korui.native

import java.awt.*
import javax.swing.*

actual val DEFAULT_UI_FACTORY: NativeUiFactory get() = DEFAULT_AWT_UI_FACTORY

var DEFAULT_AWT_UI_FACTORY: NativeUiFactory = AwtUiFactory()

open class AwtUiFactory : NativeUiFactory {
    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
    override fun wrapNative(native: Any?) = AwtComponent(this, native as Component)
    override fun createWindow() = AwtWindow(this)
    override fun createContainer() = AwtContainer(this)
    override fun createScrollPanel() = AwtScrollPanel(this)
    override fun createButton() = AwtButton(this)
    override fun createLabel() = AwtLabel(this)
    override fun createCanvas() = AwtCanvas(this)
    override fun createCheckBox() = AwtCheckBox(this)
    override fun createTextField() = AwtTextField(this)
    override fun <T> createComboBox() = AwtComboBox<T>(this)
    override fun createTree() = AwtTree(this)
}
