package com.soywiz.korui

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
import com.soywiz.korui.*
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
    fun wrapNative(native: Any?) = AwtComponent(this, native as Component)
    fun wrapNativeContainer(native: Any?): NativeContainer {
        val container = native as Container
        return AwtContainer(this, container)
    }

    fun createWindow() = AwtWindow(this)
    fun createContainer() = AwtContainer(this)
    fun createToolbar() = AwtToolbar(this)
    fun createScrollPanel() = AwtScrollPanel(this)
    fun createButton() = AwtButton(this)
    fun createToggleButton() = AwtToggleButton(this)
    fun createLabel() = AwtLabel(this)
    fun createCanvas() = AwtCanvas(this)
    fun createCheckBox() = AwtCheckBox(this)
    fun createTextField() = AwtTextField(this)
    fun <T> createComboBox() = AwtComboBox<T>(this)
    fun createTree() = AwtTree(this)

    interface NativeToolbar : NativeContainer {
    }

    interface NativeAbstractButton : NativeComponent, NativeWithText {
        var icon: Bitmap?
            get() = null
            set(value) = Unit
    }

    interface NativeButton : NativeAbstractButton {
    }

    interface NativeToggleButton : NativeAbstractButton {
        var pressed: Boolean
            get() = false
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
        var cursor: UiCursor?
            get() = null
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

        fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = Unit
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

    interface NativeTree : NativeComponent {
        var root: UiTreeNode?
            get() = null
            set(value) = Unit
        fun select(node: UiTreeNode?) = Unit

        fun onSelect(block: (nodes: List<UiTreeNode>) -> Unit) = Unit
    }

    interface NativeWindow : NativeContainer {
        var title: String
            get() = ""
            set(value) = Unit
        var menu: UiMenu?
            get() = null
            set(value) = Unit
        val pixelFactor: Double get() = 1.0
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

    override var cursor: UiCursor? = null
        set(value) {
            field = value
            component.cursor = value.toAwt()
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

    override fun showPopupMenu(menu: List<UiMenuItem>, x: Int, y: Int) {
        val jmenu = factory.createJPopupMenu()
        //jmenu.border = DropShadowBorder()
        for (it in menu) jmenu.add(it.toMenuItem(factory))
        //var x = if (x >= 0) x
        try {
            jmenu.show(
                component,
                if (x == Int.MIN_VALUE) component.mousePosition?.x ?: 0 else x,
                if (y == Int.MIN_VALUE) component.mousePosition?.y ?: 0 else y
            )
        } catch (e: Throwable) {
            e.printStackTrace()
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

internal open class AwtToggleButton(factory: NativeUiFactory, val button: JToggleButton = JToggleButton()) : AwtComponent(factory, button),
    NativeUiFactory.NativeToggleButton {
    override var text: String
        get() = button.text
        set(value) { button.text = value }

    override var icon: Bitmap? = null
        set(value) {
            field = value
            button.icon = value?.toAwtIcon()
        }

    override var pressed: Boolean
        get() = button.isSelected
        set(value) { button.isSelected = value }
}

internal open class AwtToolbar(factory: NativeUiFactory, val toolbar: JToolBar = factory.createJToolBar()) : AwtContainer(factory, toolbar),
    NativeUiFactory.NativeToolbar {
}

internal open class AwtWindow(factory: NativeUiFactory, val frame: JFrame = JFrame()) : AwtContainer(factory, frame, frame.contentPane),
    NativeUiFactory.NativeWindow {
    init {
        frame.contentPane.layout = null
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.setLocationRelativeTo(null)
    }

    override val componentPane get() = frame.contentPane

    override var bounds: RectangleInt
        get() {
            val b = frame.contentPane.bounds
            return RectangleInt(b.x, b.y, b.width, b.height)
        }
        set(value) {
            frame.contentPane.bounds = Rectangle(value.x, value.y, value.width, value.height)
            frame.bounds = Rectangle(value.x, value.y, value.width, value.height)
        }

    override var visible: Boolean
        get() = super<AwtContainer>.visible
        set(value) {
            super<AwtContainer>.visible = value
            frame.setLocationRelativeTo(null)
        }
    override var title: String
        get() = frame.title
        set(value) {
            frame.title = value
        }

    override var menu: UiMenu? = null
        set(value) {
            field = value
            frame.jMenuBar = value?.toJMenuBar(factory)
        }

    override var focusable: Boolean
        get() = frame.contentPane.isFocusable
        set(value) {
            frame.contentPane.isFocusable = value
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

internal val UiTreeNode.awt by Extra.PropertyThis<UiTreeNode, AwtTreeNode>() { AwtTreeNode(this) }

internal data class AwtTreeNode(val node: UiTreeNode) : TreeNode {
    override fun getChildAt(childIndex: Int): TreeNode? = node.children?.get(childIndex)?.awt
    override fun getChildCount(): Int = node.children?.size ?: 0
    override fun getParent(): TreeNode? = node.parent?.let { it.awt }
    override fun getIndex(node: TreeNode?): Int = this.node.children?.indexOf(node as UiTreeNode?) ?: -1
    override fun getAllowsChildren(): Boolean = node.children != null
    override fun isLeaf(): Boolean = node.children == null
    override fun children(): Enumeration<out TreeNode> = Vector(node.children ?: listOf()).elements() as Enumeration<out TreeNode>
    override fun toString(): String = node.toString()
}

internal open class AwtTree(factory: NativeUiFactory, val tree: JTree = JTree()) : AwtComponent(factory, tree), NativeUiFactory.NativeTree {
    val model get() = tree.model as DefaultTreeModel
    override var root: UiTreeNode?
        get() = (model.root as? AwtTreeNode?)?.node
        set(value) {
            tree.model = DefaultTreeModel(value?.awt)
        }

    override fun select(node: UiTreeNode?) {
        val path = TreePath(model.getPathToRoot(node?.awt))
        tree.clearSelection()
        tree.selectionPath = path
        tree.expandPath(path)
    }

    override fun onSelect(block: (nodes: List<UiTreeNode>) -> Unit) {
        tree.addTreeSelectionListener {
            block(tree.selectionPaths.map { (it.lastPathComponent as AwtTreeNode).node })
        }
    }
}

internal fun UiMenuItem.toMenuItem(factory: NativeUiFactory): JMenuItem {
    val item = factory.createJMenuItem()
    item.text = this.text
    item.icon = this.icon?.toAwtIcon()
    item.addActionListener { this.action() }
    if (this.children != null) {
        for (child in this.children!!) {
            item.add(child.toMenuItem(factory))
        }
    }
    return item
}

internal fun UiMenuItem.toMenu(factory: NativeUiFactory): JMenu {
    val item = factory.createJMenu()
    item.text = this.text
    item.addActionListener { this.action() }
    if (this.children != null) {
        for (child in this.children!!) {
            item.add(child.toMenuItem(factory))
        }
    }
    return item
}

internal fun UiMenu.toJMenuBar(factory: NativeUiFactory): JMenuBar {
    val bar = factory.createJMenuBar()
    for (child in children) {
        bar.add(child.toMenu(factory))
    }
    return bar
}

internal fun Bitmap.toAwtIcon() = javax.swing.ImageIcon(this.toAwt())

private val standardCursorToAwt = mapOf(
    UiStandardCursor.DEFAULT to Cursor.DEFAULT_CURSOR,
    UiStandardCursor.CROSSHAIR to Cursor.CROSSHAIR_CURSOR,
    UiStandardCursor.TEXT to Cursor.TEXT_CURSOR,
    UiStandardCursor.HAND to Cursor.HAND_CURSOR,
    UiStandardCursor.MOVE to Cursor.MOVE_CURSOR,
    UiStandardCursor.WAIT to Cursor.WAIT_CURSOR,
    UiStandardCursor.RESIZE_EAST to Cursor.E_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_WEST to Cursor.W_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_SOUTH to Cursor.S_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_NORTH to Cursor.N_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_NORTH_EAST to Cursor.NE_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_NORTH_WEST to Cursor.NW_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_SOUTH_EAST to Cursor.SE_RESIZE_CURSOR,
    UiStandardCursor.RESIZE_SOUTH_WEST to Cursor.SW_RESIZE_CURSOR
)
private val standardCursorToAwtRev = standardCursorToAwt.flip()

internal fun UiCursor?.toAwt(): Cursor {
    return when (this) {
        null -> Cursor(Cursor.DEFAULT_CURSOR)
        is UiStandardCursor -> Cursor(standardCursorToAwt[this] ?: Cursor.DEFAULT_CURSOR)
        else -> {
            TODO()
        }
    }
}

