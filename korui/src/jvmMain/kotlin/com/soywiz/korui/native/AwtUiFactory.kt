package com.soywiz.korui.native

import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import java.awt.*
import javax.swing.*

actual val DEFAULT_UI_FACTORY: NativeUiFactory get() = DEFAULT_AWT_UI_FACTORY

var DEFAULT_AWT_UI_FACTORY: NativeUiFactory = AwtUiFactory()

class AwtUiFactory : BaseAwtUiFactory() {
    init {
        //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
}

open class BaseAwtUiFactory : NativeUiFactory {
    override fun wrapNative(native: Any?) = AwtComponent(this, native as Component)
    override fun wrapNativeContainer(native: Any?): NativeUiFactory.NativeContainer {
        val container = native as Container
        return AwtContainer(this, container)
    }
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

    open fun createJScrollPane() = JScrollPane().also {
        it.verticalScrollBar.unitIncrement = 16
        it.horizontalScrollBar.unitIncrement = 16
    }
    open fun createJPopupMenu() = JPopupMenu()
    open fun createJMenuItem() = JMenuItem()
    open fun createJMenu() = JMenu()
    open fun createJMenuBar(): JMenuBar = JMenuBar()
    open fun awtOpenFileDialog(component: Component, file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
        TODO()
    }

    open fun awtOpenColorPickerDialog(component: Component, color: RGBA): RGBA? {
        TODO()
    }
}
