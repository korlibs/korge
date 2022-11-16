package com.soywiz.korui.native

import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

internal val DEFAULT_UI_FACTORY: NativeUiFactory get() = DEFAULT_AWT_UI_FACTORY

internal var DEFAULT_AWT_UI_FACTORY: NativeUiFactory = AwtUiFactory()

internal class AwtUiFactory : BaseAwtUiFactory() {
    init {
        //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }
}

internal open class BaseAwtUiFactory : NativeUiFactory {
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
    override fun createToggleButton() = AwtToggleButton(this)
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

    open fun createJToolBar() = JToolBar().also {
        it.isOpaque = true
        it.background = JPanel().background
    }

    open fun createJPopupMenu() = JPopupMenu()
    open fun createJMenuItem() = JMenuItem()
    open fun createJMenu() = JMenu()
    open fun createJMenuBar(): JMenuBar = JMenuBar()
    open fun awtOpenFileDialog(component: Component, file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = file?.absolutePath?.let { java.io.File(it) }
        val selection = fileChooser.showOpenDialog(component)
        return fileChooser.selectedFile?.let { localVfs(it) }
    }

    open fun awtOpenColorPickerDialog(component: Component, color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? {
        var fcolor = color.toAwt()

        val pane = JColorChooser(fcolor)
        pane.selectionModel.addChangeListener {
            listener?.invoke(pane.color.toRgba())
        }
        val dialog = JColorChooser.createDialog(
            component, "Pick color", true, pane,
            { fcolor = pane.color },
            null
        )
        dialog.addComponentListener(object : ComponentAdapter() {
            override fun componentHidden(e: ComponentEvent?) {
                val w = e!!.component as Window
                w.dispose()
            }
        })

        dialog.show()

        return fcolor.toRgba()
    }

    open fun createJPanel(): JPanel {
        return object : JPanel() {
            internal var cachedBounds: Dimension? = null
            override fun isPreferredSizeSet(): Boolean = true
            override fun preferredSize(): Dimension {
                //cachedBounds = null
                if (cachedBounds == null) {
                    val bb = BoundsBuilder()
                    for (n in 0 until componentCount) {
                        val b = this.getComponent(n).bounds
                        bb.add(b.x, b.y)
                        bb.add(b.x + b.width, b.y + b.height)
                    }
                    cachedBounds = Dimension(bb.xmax.toInt(), bb.ymax.toInt())
                }
                return cachedBounds!!
            }
        }
    }
}
