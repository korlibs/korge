package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korev.FocusEvent
import com.soywiz.korev.KeyEvent
import com.soywiz.korim.awt.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import java.awt.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.*
import java.awt.event.MouseEvent
import java.awt.image.*
import java.util.*
import javax.swing.*
import javax.swing.event.*
import javax.swing.tree.*

internal val DEFAULT_UI_FACTORY: NativeUiFactory get() = DEFAULT_AWT_UI_FACTORY

internal var DEFAULT_AWT_UI_FACTORY: NativeUiFactory = NativeUiFactory()

internal open class NativeUiFactory {
    fun wrapNativeContainer(native: Any?): NativeContainer {
        val container = native as Container
        return AwtContainer(this, container)
    }

    fun createContainer() = AwtContainer(this)
    fun createScrollPanel() = AwtScrollPanel(this)
    fun createButton() = AwtButton(this)
    fun createLabel() = AwtLabel(this)
    fun createCanvas() = AwtCanvas(this)
    fun createCheckBox() = AwtCheckBox(this)
    fun createTextField() = AwtTextField(this)
    fun <T> createComboBox() = AwtComboBox<T>(this)

    interface NativeButton : NativeComponent, NativeWithText {
        var icon: Bitmap?
            get() = null
            set(value) = Unit
    }

    interface NativeCheckBox : NativeComponent, NativeWithText {
        var checked: Boolean
            get() = false
            set(value) = Unit
        fun onChange(block: () -> Unit) = Disposable { }
    }

    interface NativeComboBox<T> : NativeComponent {
        var items: List<T>
            get() = listOf()
            set(value) = Unit

        var selectedItem: T?
            get() = null
            set(value) = Unit

        fun open(): Unit = Unit
        fun close(): Unit = Unit
        fun onChange(block: () -> Unit) = Disposable { }
    }

    interface NativeComponent : Extra {
        val factory: NativeUiFactory
        var bounds: RectangleInt
            get() = RectangleInt(0, 0, 0, 0)
            set(value) = Unit
        //fun setBounds(x: Int, y: Int, width: Int, height: Int) = Unit
        var parent: NativeContainer?
            get() = null
            set(value) {
                parent?.removeChild(this)
                value?.insertChildAt(-1, this)
            }
        var index: Int
            get() = -1
            set(value) = Unit
        var visible: Boolean
            get() = true
            set(value) = Unit
        var focusable: Boolean
            get() = true
            set(value) = Unit
        var enabled: Boolean
            get() = true
            set(value) = Unit

        fun onMouseEvent(handler: (com.soywiz.korev.MouseEvent) -> Unit): Disposable = Disposable { }
        fun onFocus(handler: (FocusEvent) -> Unit): Disposable = Disposable { }
        fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = Disposable { }

        fun repaintAll() = Unit
        fun focus(focus: Boolean) = Unit
        fun updateUI() = Unit

        fun openFileDialog(file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
            TODO()
            return null
        }

        fun openColorPickerDialog(color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? {
            return color
        }
    }

    interface NativeChildren {
        val numChildren: Int get() = 0
        fun getChildAt(index: Int): NativeComponent = TODO()
        fun insertChildAt(index: Int, child: NativeComponent): Unit = TODO()
        fun removeChild(child: NativeComponent): Unit = TODO()
        fun removeChildAt(index: Int): Unit = TODO()

        class Mixin : NativeChildren {
            val children = arrayListOf<NativeComponent>()

            override val numChildren: Int get() = children.size
            override fun getChildAt(index: Int): NativeComponent = children[index]
            override fun insertChildAt(index: Int, child: NativeComponent) {
                if (index < 0) {
                    children.add(child)
                } else {
                    children.add(index, child)
                }
            }
            override fun removeChild(child: NativeComponent) {
                children.remove(child)
            }
            override fun removeChildAt(index: Int) {
                children.removeAt(index)
            }
        }
    }

    interface NativeContainer : NativeComponent, NativeChildren {
        var backgroundColor: RGBA?
            get() = null
            set(value) = Unit
    }

    interface NativeLabel : NativeComponent, NativeWithText {
        var icon: Bitmap?
            get() = null
            set(value) = Unit
    }

    interface NativeCanvas : NativeComponent {
        var image: Bitmap?
            get() = null
            set(value) = Unit
    }

    interface NativeScrollPanel : NativeContainer {
        var xbar: Boolean?
            get() = null
            set(value) = Unit
        var ybar: Boolean?
            get() = null
            set(value) = Unit
    }

    interface NativeTextField : NativeComponent, NativeWithText {
        fun select(range: IntRange? = 0 until Int.MAX_VALUE): Unit = Unit
        fun focus(): Unit = Unit
        fun onKeyEvent(block: (KeyEvent) -> Unit): Disposable = Disposable { }
    }

    interface NativeWithText : NativeComponent {
        var text: String
            get() = ""
            set(value) = Unit
    }
    open fun createJScrollPane() = JScrollPane().also {
        it.verticalScrollBar.unitIncrement = 16
        it.horizontalScrollBar.unitIncrement = 16
    }

    open fun awtOpenFileDialog(component: Component, file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
        val fileDialog = FileDialog(null as Frame?)
        fileDialog.file = file?.absolutePath?.let { java.io.File(it) }?.absolutePath
        fileDialog.isVisible = true
        return localVfs(fileDialog.file)

        //val fileChooser = JFileChooser()
        //fileChooser.selectedFile = file?.absolutePath?.let { java.io.File(it) }
        //val selection = fileChooser.showOpenDialog(component)
        //return fileChooser.selectedFile?.let { localVfs(it) }
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

internal val awtToWrappersMap = WeakMap<Component, AwtComponent>()
internal fun Component.toAwt(): AwtComponent? = awtToWrappersMap[this]

internal open class AwtComponent(override val factory: NativeUiFactory, val component: Component) : NativeUiFactory.NativeComponent, Extra by Extra.Mixin() {
    init {
        awtToWrappersMap[component] = this
    }

    override var bounds: RectangleInt
        get() {
            val b = component.bounds
            return RectangleInt(b.x, b.y, b.width, b.height)
        }
        set(value) {
            component.bounds = Rectangle(value.x, value.y, value.width, value.height)
        }

    //override fun setBounds(x: Int, y: Int, width: Int, height: Int) { component.setBounds(x, y, width, height) }

    override var parent: NativeUiFactory.NativeContainer? = null
        //get() {
        //    println(component.parent.parent.parent)
        //    return awtToWrappersMap[component.parent] as? UiContainer?
        //}
        set(p) {
            field = p
            super.parent = p
        }

    override var index: Int
        get() = super.index
        set(value) {}

    override var visible: Boolean
        get() = component.isVisible
        set(value) { component.isVisible = value }

    override var focusable: Boolean
        get() = component.isFocusable
        set(value) { component.isFocusable = value }

    override var enabled: Boolean
        get() = component.isEnabled
        set(value) { component.isEnabled = value }

    //var lastMouseEvent: java.awt.event.MouseEvent? = null

    override fun onFocus(handler: (FocusEvent) -> Unit): Disposable {
        val event = com.soywiz.korev.FocusEvent()

        fun dispatch(e: java.awt.event.FocusEvent, type: com.soywiz.korev.FocusEvent.Type) {
            event.type = type
            handler(event)
        }
        val listener = object : FocusAdapter() {
            override fun focusGained(e: java.awt.event.FocusEvent) = dispatch(e, FocusEvent.Type.FOCUS)
            override fun focusLost(e: java.awt.event.FocusEvent) = dispatch(e, FocusEvent.Type.BLUR)
        }
        component.addFocusListener(listener)
        return Disposable {
            component.removeFocusListener(listener)
        }
    }

    companion object {
        val blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "blank cursor");
    }

    override fun onMouseEvent(handler: (com.soywiz.korev.MouseEvent) -> Unit): Disposable {
        val event = com.soywiz.korev.MouseEvent()

        var lockingX = 0
        var lockingY = 0
        var lockingDeltaX = 0
        var lockingDeltaY = 0
        var locking = false
        var lastAwtComponent: Component? = null
        var lockAwtComponent: Component? = null
        var lastAwtCursor: Cursor? = null

        fun dispatch(e: MouseEvent, type: com.soywiz.korev.MouseEvent.Type) {
            event.component = e.component
            event.type = type
            event.button = MouseButton[e.button]
            event.isShiftDown = e.isShiftDown
            event.isCtrlDown = e.isControlDown
            event.isAltDown = e.isAltDown
            event.isMetaDown = e.isMetaDown
            if (event.typeUp && locking) {
                locking = false
                @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                component.cursor = lastAwtCursor
            }
            if (locking) {
                val dx = e.xOnScreen - e.x
                val dy = e.yOnScreen - e.y
                lockingDeltaX += MouseInfo.getPointerInfo().location.x - lockingX
                lockingDeltaY += MouseInfo.getPointerInfo().location.y - lockingY
                event.x = lockingDeltaX - dx
                event.y = lockingDeltaY - dy
                Robot().mouseMove(lockingX, lockingY)
            } else {
                event.x = e.x
                event.y = e.y
            }
            handler(event)
        }

        event.requestLock = {
            val component = (event.component as? Component?)
            if (component != null) {
                lastAwtCursor = component.cursor
                component.cursor = blankCursor
            }
            locking = true
            lockingX = MouseInfo.getPointerInfo().location.x
            lockingY = MouseInfo.getPointerInfo().location.y
            lockingDeltaX = lockingX
            lockingDeltaY = lockingY
        }

        val listener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.CLICK)
            override fun mousePressed(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.DOWN)
            override fun mouseReleased(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.UP)
            override fun mouseEntered(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.ENTER)
            override fun mouseExited(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.EXIT)
            //override fun mouseWheelMoved(e: MouseWheelEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.WEE)
            override fun mouseDragged(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.DRAG)
            override fun mouseMoved(e: MouseEvent) = dispatch(e, com.soywiz.korev.MouseEvent.Type.MOVE)
        }

        component.addMouseListener(listener)
        component.addMouseMotionListener(listener)
        return Disposable {
            component.removeMouseMotionListener(listener)
            component.removeMouseListener(listener)
        }
    }

    open val componentPane get() = component

    override fun onResize(handler: (ReshapeEvent) -> Unit): Disposable {
        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                handler(ReshapeEvent(component.x, component.y, componentPane.width, componentPane.height))
            }
        }
        component.addComponentListener(listener)
        return Disposable {
            component.removeComponentListener(listener)
        }
    }

    override fun openFileDialog(file: VfsFile?, filter: (VfsFile) -> Boolean): VfsFile? {
        return factory.awtOpenFileDialog(component, file, filter)
    }

    override fun openColorPickerDialog(color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? {
        return factory.awtOpenColorPickerDialog(component, color, listener)
    }

    override fun repaintAll() {
        component.doLayout()
        component.revalidate()
        component.repaint()
    }

    override fun focus(focus: Boolean) {
        if (focus) {
            component.requestFocus()
        } else {
            component.parent.requestFocus()
        }
    }

    override fun updateUI() {
    }
}

internal open class AwtButton(factory: NativeUiFactory, val button: JButton = JButton()) : AwtComponent(factory, button), NativeUiFactory.NativeButton {
    override var text: String
        get() = button.text
        set(value) { button.text = value }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            button.icon = value?.toAwtIcon()
        }
}

internal open class AwtCanvas(factory: NativeUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), NativeUiFactory.NativeCanvas {
    override var image: Bitmap? = null
        set(value) {
            field = value
            label.icon = value?.toAwtIcon()
        }
}

internal open class AwtCheckBox(factory: NativeUiFactory, val checkBox: JCheckBox = JCheckBox()) : AwtComponent(factory, checkBox),
    NativeUiFactory.NativeCheckBox {
    override var text: String
        get() = checkBox.text
        set(value) { checkBox.text = value }
    override var checked: Boolean
        get() = checkBox.isSelected
        set(value) { checkBox.isSelected = value }

    override fun onChange(block: () -> Unit): Disposable {
        val listener = ChangeListener { block() }
        checkBox.addChangeListener(listener)
        return Disposable {
            checkBox.removeChangeListener(listener)
        }
    }
}

internal open class AwtComboBox<T>(factory: NativeUiFactory, val comboBox: JComboBox<T> = JComboBox<T>()) : AwtComponent(factory, comboBox),
    NativeUiFactory.NativeComboBox<T> {
    override var items: List<T>
        get() {
            val model = comboBox.model
            return (0 until model.size).map { model.getElementAt(it) }
        }
        set(value) {
            comboBox.model = DefaultComboBoxModel((value as List<Any>).toTypedArray()) as DefaultComboBoxModel<T>
        }

    override var selectedItem: T?
        get() = comboBox.selectedItem as T?
        set(value) { comboBox.selectedItem = value }

    override fun open() {
        comboBox.showPopup()
        //println("ComboBox.open")
    }

    override fun close() {
        comboBox.hidePopup()
    }


    override fun onChange(block: () -> Unit): Disposable {
        val listener = ActionListener { block() }
        comboBox.addActionListener(listener)
        return Disposable {
            comboBox.removeActionListener(listener)
        }
    }
}

internal open class AwtContainer(
    factory: NativeUiFactory,
    val container: Container = factory.createJPanel(),
    val childContainer: Container = container
) : AwtComponent(factory, container), NativeUiFactory.NativeContainer {
    init {
        //container.layout = null
        childContainer.layout = null
    }

    override var backgroundColor: RGBA?
        get() = container.background?.toRgba()
        set(value) {
            //container.isOpaque
            //container.isOpaque = value != null
            container.background = value?.toAwt()
        }

    /*
    override var bounds: RectangleInt
        get() {
            val b = childContainer.bounds
            return RectangleInt(b.x, b.y, b.width, b.height)
        }
        set(value) {
            container.bounds = Rectangle(value.x, value.y, value.width, value.height)
        }
    */

    override val numChildren: Int get() = childContainer.componentCount
    override fun getChildAt(index: Int): NativeUiFactory.NativeComponent = awtToWrappersMap[childContainer.getComponent(index)] ?: error("Can't find component")
    override fun insertChildAt(index: Int, child: NativeUiFactory.NativeComponent) {
        childContainer.add((child as AwtComponent).component, index)
    }
    override fun removeChild(child: NativeUiFactory.NativeComponent) {
        childContainer.remove((child as AwtComponent).component)
    }
    override fun removeChildAt(index: Int) {
        childContainer.remove(index)
    }
}

internal open class AwtLabel(factory: NativeUiFactory, val label: JLabel = JLabel()) : AwtComponent(factory, label), NativeUiFactory.NativeLabel {
    override var text: String
        get() = label.text
        set(value) { label.text = value }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            label.icon = value?.toAwtIcon()
        }
}

internal open class AwtTextField(factory: NativeUiFactory, val textField: JTextField = JTextField()) : AwtComponent(factory, textField),
    NativeUiFactory.NativeTextField {
    override var text: String
        get() = textField.text
        set(value) { textField.text = value }

    override fun select(range: IntRange?) {
        if (range == null) {
            textField.select(0, 0)
        } else {
            textField.select(range.first, range.last + 1)
        }
    }
    override fun focus() = textField.requestFocus()
    override fun onKeyEvent(block: (KeyEvent) -> Unit): Disposable {
        val event = KeyEvent()

        fun dispatch(e: java.awt.event.KeyEvent, type: KeyEvent.Type) {
            event.type = type
            event.keyCode = e.keyCode
            event.character = e.keyChar
            event.key = awtKeyCodeToKey(e.keyCode)
            event.shift = e.isShiftDown
            event.ctrl = e.isControlDown
            event.alt = e.isAltDown
            event.meta = e.isMetaDown
            block(event)
        }

        val listener = object : KeyAdapter() {
            override fun keyTyped(e: java.awt.event.KeyEvent) = dispatch(e, KeyEvent.Type.TYPE)
            override fun keyPressed(e: java.awt.event.KeyEvent) = dispatch(e, KeyEvent.Type.DOWN)
            override fun keyReleased(e: java.awt.event.KeyEvent) = dispatch(e, KeyEvent.Type.UP)
        }
        textField.addKeyListener(listener)
        return Disposable { textField.removeKeyListener(listener) }
    }
}

internal open class AwtScrollPanel(
    factory: NativeUiFactory,
    val view: JFixedSizeContainer = AwtContainer(factory, JFixedSizeContainer()).container as JFixedSizeContainer,
    val scrollPanel: JScrollPane = factory.createJScrollPane()
) : AwtContainer(factory, scrollPanel, view), NativeUiFactory.NativeScrollPanel {
    override var bounds: RectangleInt
        get() = super<AwtContainer>.bounds
        set(value) {
            super<AwtContainer>.bounds = value
            //scrollPanel.setViewportView()
            //view.setBounds(0, 0, value.width, value.height)
        }

    override var xbar: Boolean? = null
        set(value) {
            field = value
            scrollPanel.horizontalScrollBarPolicy = when (value) {
                null -> ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                true -> ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
                false -> ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            }
        }
    override var ybar: Boolean? = null
        set(value) {
            field = value
            scrollPanel.verticalScrollBarPolicy = when (value) {
                null -> ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
                true -> ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
                false -> ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
            }
        }

    init {
        scrollPanel.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPanel.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        //scrollPanel.layout = ScrollPaneLayout()
        //view.background = Color.GREEN
        //view.layout = BoxLayout(view, BoxLayout.Y_AXIS)
        //view.setBounds(0, 0, 1000, 1000)
        //view.minimumSize = Dimension(300, 300)

        scrollPanel.setViewportView(view)
        //view.preferredSize = Dimension(2000, 2000)
        //view.size = Dimension(2000, 2000)
        //scrollPanel.viewport.extentSize = Dimension(2000, 2000)
        //scrollPanel.viewport.viewSize = Dimension(2000, 2000)
    }

    override fun updateUI() {
        super<AwtContainer>.updateUI()
        view.cachedBounds = null
    }
}

open class JFixedSizeContainer : JPanel() {
    init {
        this.layout = null
    }
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

internal fun Bitmap.toAwtIcon() = ImageIcon(this.toAwt())
