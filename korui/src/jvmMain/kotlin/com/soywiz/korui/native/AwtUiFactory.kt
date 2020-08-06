package com.soywiz.korui.native

import java.awt.*
import javax.swing.*

actual val DEFAULT_UI_FACTORY: NativeUiFactory get() = DEFAULT_AWT_UI_FACTORY

var DEFAULT_AWT_UI_FACTORY: NativeUiFactory = AwtUiFactory()

class AwtUiFactory : BaseAwtUiFactory() {
    init {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
}

open class BaseAwtUiFactory : NativeUiFactory {
    override fun wrapNative(native: Any?) = AwtComponent(this, native as Component)
    override fun createWindow() = AwtWindow(this)
    override fun createContainer() = AwtContainer(this)
    override fun createToolbar() = AwtToolbar(this)
    override fun createScrollPanel() = AwtScrollPanel(this)
    override fun createButton() = AwtButton(this)
    override fun createLabel() = AwtLabel(this)
    override fun createCanvas() = AwtCanvas(this)
    override fun createCheckBox() = AwtCheckBox(this)
    override fun createTextField() = AwtTextField(this)
    override fun <T> createComboBox() = AwtComboBox<T>(this)
    override fun createTree() = AwtTree(this)

    open fun createJPopupMenu() = JPopupMenu()
    open fun createJMenuItem() = JMenuItem()
    open fun createJMenu() = JMenu()
    open fun createJMenuBar(): JMenuBar = JMenuBar()
}
