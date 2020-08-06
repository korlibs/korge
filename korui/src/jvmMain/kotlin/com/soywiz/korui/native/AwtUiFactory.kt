package com.soywiz.korui.native

import javax.swing.*

actual val DEFAULT_UI_FACTORY: NativeUiFactory by lazy { AwtUiFactory() }

open class AwtUiFactory : NativeUiFactory {
    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
    override fun createWindow() = AwtWindow(this)
    override fun createContainer() = AwtContainer(this)
    override fun createScrollPanel() = AwtScrollPanel(this)
    override fun createButton() = AwtButton(this)
    override fun createLabel() = AwtLabel(this)
    override fun createCheckBox() = AwtCheckBox(this)
    override fun createTextField() = AwtTextField(this)
    override fun <T> createComboBox() = AwtComboBox<T>(this)
    override fun createTree() = AwtTree(this)
}

