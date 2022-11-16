package com.soywiz.korui

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.length.*
import com.soywiz.korma.length.LengthExtensions.Companion.pt

//fun NativeUiFactory.createApp() = UiApplication(this)

internal open class UiApplication constructor(val factory: NativeUiFactory) : Extra by Extra.Mixin() {
    fun window(width: Int = 300, height: Int = 300, block: UiWindow.() -> Unit): UiWindow = UiWindow(this)
        .also { it.bounds = RectangleInt(0, 0, width, height) }
        .also { it.layout = VerticalUiLayout }
        .also(block)
        .also { it.visible = true }
        .also { window -> window.onResize { window.layout?.relayout(window) } }
        .also { it.relayout() }

    fun wrapContainer(native: Any?): UiContainer = UiContainer(this, factory.wrapNativeContainer(native)).also { container ->
        container.onResize {
            //println("wrapContainer.container.onResize: ${container.bounds}")
            container.relayout()
        }
    }

    open fun evaluateExpression(expr: String): Any? {
        return expr.toDoubleOrNull() ?: 0.0
    }
}

internal interface UiCursor {
}

internal enum class UiStandardCursor : UiCursor {
    DEFAULT, CROSSHAIR, TEXT, HAND, MOVE, WAIT,
    RESIZE_EAST, RESIZE_WEST, RESIZE_SOUTH, RESIZE_NORTH,
    RESIZE_NORTH_EAST, RESIZE_NORTH_WEST, RESIZE_SOUTH_EAST, RESIZE_SOUTH_WEST;
}

internal var NativeUiFactory.NativeComponent.uiComponent by Extra.PropertyThis<NativeUiFactory.NativeComponent, UiComponent?> { null }

internal open class UiComponent(val app: UiApplication, val component: NativeUiFactory.NativeComponent) : Extra by Extra.Mixin(), LengthExtensions {
    init {
        component.uiComponent = this
    }
    val factory get() = app.factory
    var _parent: UiContainer? = null
        internal set

    val root: UiContainer? get() {
        if (this.parent == null) return this as? UiContainer?
        return this.parent?.root
    }

    var parent: UiContainer?
        get() = _parent
        set(value) {
            parent?.removeChild(this)
            value?.addChild(this)
        }
    var visible: Boolean
        get() = component.visible
        set(value) {
            component.visible = value
            root?.relayout()
        }
    var enabled by component::enabled
    open var bounds: RectangleInt
        get() = component.bounds
        set(value) {
            component.bounds = value
        }
    var cursor by component::cursor
    var focusable by component::focusable

    open fun copyFrom(that: UiComponent) {
        this.visible = that.visible
        this.enabled = that.enabled
        this.bounds = that.bounds
        this.cursor = that.cursor
        this.focusable = that.focusable
    }

    fun onMouseEvent(block: (MouseEvent) -> Unit) = component.onMouseEvent(block)
    fun onFocus(block: (FocusEvent) -> Unit) = component.onFocus(block)
    fun showPopupMenu(menu: List<UiMenuItem>, x: Int = Int.MIN_VALUE, y: Int = Int.MIN_VALUE) = component.showPopupMenu(menu, x, y)
    fun openFileDialog(file: VfsFile?, filter: (VfsFile) -> Boolean) = component.openFileDialog(file, filter)
    fun openColorPickerDialog(color: RGBA, listener: ((RGBA) -> Unit)?): RGBA? = component.openColorPickerDialog(color, listener)
    fun focus(focus: Boolean = true) = component.focus(focus)
    fun show() { visible = true }
    fun hide() { visible = false }
    fun onClick(block: (MouseEvent) -> Unit) = onMouseEvent { if (it.typeClick) block(it) }
    fun onResize(handler: (ReshapeEvent) -> Unit): Disposable = component.onResize(handler)

    fun repaintAll() = component.repaintAll()
    open fun updateUI() = component.updateUI()
}

internal open class UiContainer(app: UiApplication, val container: NativeUiFactory.NativeContainer = app.factory.createContainer()) : UiComponent(app, container) {
    private val _children = arrayListOf<UiComponent>()
    val numChildren: Int get() = _children.size
    val size: Int get() = numChildren
    var backgroundColor: RGBA? by container::backgroundColor

    var layout: UiLayout? = VerticalUiLayout

    open fun computePreferredSize(available: SizeInt): SizeInt {
        return layout?.computePreferredSize(this, available) ?: available
    }

    fun relayout() {
        layout?.relayout(this)
        updateUI()
    }

    override fun updateUI() {
        super.updateUI()
        forEachChild { it.updateUI() }
    }

    override var bounds: RectangleInt
        get() = super.bounds
        set(value) {
            super.bounds = value
            relayout()
        }

    fun getChildIndex(child: UiComponent): Int = _children.indexOf(child)
    fun getChildAt(index: Int): UiComponent = _children[index]
    fun removeChildAt(index: Int) {
        _children.removeAt(index)
        container.removeChildAt(index)
    }
    fun removeChild(child: UiComponent) {
        val index = getChildIndex(child)
        if (index >= 0) removeChildAt(index)
        child._parent = null
    }
    fun insertChildAt(index: Int, child: UiComponent) {
        if (child.parent != null) {
            child.parent?.removeChild(child)
        }
        container.insertChildAt(index, child.component)
        val rindex: Int = if (index < 0) numChildren + 1 + index else index
        _children.add(rindex.coerceAtLeast(0), child)
        child._parent = this
    }
    fun replaceChildAt(index: Int, newChild: UiComponent) {
        removeChildAt(index)
        insertChildAt(index, newChild)
    }
    operator fun get(index: Int) = getChildAt(index)
    fun removeChildren() {
        val initialNumChildren = numChildren
        while (numChildren > 0) {
            removeChildAt(numChildren - 1)
            if (initialNumChildren == numChildren) invalidOp
        }
    }
    fun addChild(child: UiComponent): Unit = insertChildAt(-1, child)
    inline fun forEachChild(block: (UiComponent) -> Unit) {
        for (n in 0 until numChildren) block(getChildAt(n))
    }
    inline fun forEachVisibleChild(block: (UiComponent) -> Unit) = forEachChild { if (it.visible) block(it) }
    val children: List<UiComponent?> get() = _children.toList()
    val firstChild get() = _children.first()
    val lastChild get() = _children.last()
}

internal inline fun UiContainer.container(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app)
        .also { it.parent = this }
        .also { it.bounds = this.bounds }
        .also(block)
}

internal inline fun UiContainer.addBlock(block: UiContainer.() -> Unit) {
    block(this)
}

internal open class UiButton(app: UiApplication, val button: NativeUiFactory.NativeButton = app.factory.createButton()) : UiComponent(app, button) {
    var icon by button::icon
    var text by button::text
}

internal inline fun UiContainer.button(text: String = "Button", noinline onClick: (UiButton.(MouseEvent) -> Unit)? = null, block: UiButton.() -> Unit = {}): UiButton =
    UiButton(app)
        .also { it.text = text }
        .also { it.parent = this }
        .also { button -> if (onClick != null) button.onClick { onClick(button, it) }  }
        .also(block)

internal open class UiCanvas(app: UiApplication, val canvas: NativeUiFactory.NativeCanvas = app.factory.createCanvas()) : UiComponent(app, canvas) {
    var image: Bitmap?
        get() = canvas.image
        set(value) {
            canvas.image = value
            this.preferredWidth = value?.width?.pt
            this.preferredHeight = value?.height?.pt
        }

    override fun copyFrom(that: UiComponent) {
        super.copyFrom(that)
        that as UiCanvas
        this.image = that.image
    }
}

internal inline fun UiContainer.canvas(image: Bitmap? = null, block: UiCanvas.() -> Unit): UiCanvas {
    return UiCanvas(app).also { it.image = image }.also { it.parent = this }.also(block)
}

internal open class UiCheckBox(app: UiApplication, val checkBox: NativeUiFactory.NativeCheckBox = app.factory.createCheckBox()) : UiComponent(app, checkBox) {
    var text by checkBox::text
    var checked by checkBox::checked
    fun onChange(block: UiCheckBox.(Boolean) -> Unit) = checkBox.onChange { block(this, checked) }
}

internal inline fun UiContainer.checkBox(text: String = "CheckBox", checked: Boolean = false, block: UiCheckBox.() -> Unit = {}): UiCheckBox {
    return UiCheckBox(app)
        .also { it.text = text }
        .also { it.checked = checked }
        .also { it.parent = this }.also(block)
}

internal open class UiComboBox<T>(app: UiApplication, val comboBox: NativeUiFactory.NativeComboBox<T> = app.factory.createComboBox()) : UiComponent(app, comboBox) {
    var items by comboBox::items
    var selectedItem by comboBox::selectedItem
    fun open() = comboBox.open()
    fun close() = comboBox.close()
    fun onChange(block: () -> Unit) = comboBox.onChange(block)
}

internal inline fun <T> UiContainer.comboBox(selectedItem: T, items: List<T>, block: UiComboBox<T>.() -> Unit = {}): UiComboBox<T> =
    UiComboBox<T>(app)
        .also { it.parent = this }
        .also { it.items = items }
        .also { it.selectedItem = selectedItem }
        .also(block)

internal open class UiLabel(app: UiApplication, val label: NativeUiFactory.NativeLabel = app.factory.createLabel()) : UiComponent(app, label) {
    var text by label::text
    var icon by label::icon

    override fun copyFrom(that: UiComponent) {
        super.copyFrom(that)
        that as UiLabel
        this.text = that.text
    }
}

internal inline fun UiContainer.label(text: String = "Button", block: UiLabel.() -> Unit = {}): UiLabel {
    return UiLabel(app).also { it.text = text }.also { it.parent = this }.also(block)
}

internal data class UiMenu(val children: List<UiMenuItem>) {
    constructor(vararg children: UiMenuItem) : this(children.toList())
}
internal data class UiMenuItem(val text: String, val children: List<UiMenuItem>? = null, val icon: Bitmap? = null, val action: () -> Unit = {}) {
}

internal open class UiScrollPanel(app: UiApplication, val panel: NativeUiFactory.NativeScrollPanel = app.factory.createScrollPanel()) : UiContainer(app, panel) {
    var xbar by panel::xbar
    var ybar by panel::ybar
}

internal inline fun UiContainer.scrollPanel(xbar: Boolean? = null, ybar: Boolean? = null, block: UiScrollPanel.() -> Unit = {}): UiScrollPanel {
    return UiScrollPanel(app)
        .also { it.parent = this }
        .also { it.xbar = xbar }
        .also { it.ybar = ybar }
        .also(block)
}

internal open class UiTextField(app: UiApplication, val textField: NativeUiFactory.NativeTextField = app.factory.createTextField()) : UiComponent(app, textField) {
    var text by textField::text
    fun select(range: IntRange? = 0 until Int.MAX_VALUE): Unit = textField.select(range)
    fun focus(): Unit = textField.focus()
    fun onKeyEvent(block: (KeyEvent) -> Unit): Disposable = textField.onKeyEvent(block)
}

internal inline fun UiContainer.textField(text: String = "Button", block: UiTextField.() -> Unit): UiTextField {
    return UiTextField(app).also { it.text = text }.also { it.parent = this }.also(block)
}

internal open class UiToggleButton(app: UiApplication, val button: NativeUiFactory.NativeToggleButton = app.factory.createToggleButton()) : UiComponent(app, button) {
    var icon by button::icon
    var text by button::text
    var pressed by button::pressed
}

internal inline fun UiContainer.toggleButton(text: String = "Button", pressed: Boolean = false, noinline onClick: (UiToggleButton.(MouseEvent) -> Unit)? = null, block: UiToggleButton.() -> Unit = {}): UiToggleButton =
    UiToggleButton(app)
        .also { it.text = text }
        .also { it.parent = this }
        .also { it.pressed = pressed }
        .also { button -> if (onClick != null) button.onClick { button.onClick(it) }  }
        .also(block)

internal open class UiToolBar(app: UiApplication, val canvas: NativeUiFactory.NativeToolbar = app.factory.createToolbar()) : UiComponent(app, canvas) {
}

internal inline fun UiContainer.toolbar(block: UiToolBar.() -> Unit): UiToolBar {
    return UiToolBar(app).also { it.parent = this }.also(block)
}

internal open class UiTree(app: UiApplication, val tree: NativeUiFactory.NativeTree = app.factory.createTree()) : UiComponent(app, tree) {
    var nodeRoot by tree::root
}

internal interface UiTreeNode : Extra {
    val parent: UiTreeNode? get() = null
    val children: List<UiTreeNode>? get() = null
}

internal class SimpleUiTreeNode(val text: String, override val children: List<SimpleUiTreeNode>? = null) : UiTreeNode, Extra by Extra.Mixin() {
    override var parent: UiTreeNode? = null

    init {
        if (children != null) {
            for (child in children) {
                child.parent = this
            }
        }
    }

    override fun toString(): String = text
}

internal inline fun UiContainer.tree(block: UiTree.() -> Unit): UiTree {
    return UiTree(app).also { it.parent = this }.also(block)
}

internal open class UiWindow(app: UiApplication, val window: NativeUiFactory.NativeWindow = app.factory.createWindow()) : UiContainer(app, window) {
    var title by window::title
    var menu by window::menu
    val pixelFactory by window::pixelFactor
}

object MathEx {
    fun <T : Comparable<T>> min(a: T, b: T): T = if (a.compareTo(b) < 0) a else b
    fun <T : Comparable<T>> max(a: T, b: T): T = if (a.compareTo(b) > 0) a else b
}

internal interface UiLayout {
    fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt
    fun relayout(container: UiContainer)
}

internal var UiContainer.layoutChildrenPadding by Extra.Property { 0 }

internal object UiFillLayout : UiLayout {
    override fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt {
        /*
        var maxWidth = 0
        var maxHeight = 0
        val ctx = LayoutContext(available)

        container.forEachVisibleChild { child ->
            val size = ctx.computeChildSize(child)
            maxWidth = max(size.width, maxWidth)
            maxHeight = max(size.height, maxHeight)
        }
        return SizeInt(maxWidth, maxHeight)
        */
        return available.clone()
    }

    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
        //container.bounds = bounds
        val padding = container.layoutChildrenPadding
        container.forEachChild { child ->
            child.bounds = RectangleInt.fromBounds(padding, padding, bounds.width - padding, bounds.height - padding)
        }
    }
}

internal object VerticalUiLayout : LineUiLayout(LayoutDirection.VERTICAL)
internal object HorizontalUiLayout : LineUiLayout(LayoutDirection.HORIZONTAL)

internal fun Length.Context.computeChildSize(child: UiComponent, direction: LayoutDirection): Int {
    val ctx = this
    val preferredSize = child.preferredSize
    return when {
        preferredSize != null -> Length.calc(ctx, 32.pt, preferredSize.getDirection(direction), child.minimumSize.getDirection(direction), child.maximumSize.getDirection(direction))
        child is UiContainer -> child.computePreferredSize(SizeInt(32, 32)).getDirection(direction)
        else -> when (direction) {
            LayoutDirection.HORIZONTAL -> 128
            LayoutDirection.VERTICAL -> 32
        }
    }
}

internal class LayoutContext(val available: SizeInt) {
    val widthContext = Length.Context().also { it.size = available.width }
    val heightContext = Length.Context().also { it.size = available.height }

    fun computeChildSize(child: UiComponent): SizeInt {
        return SizeInt(
            widthContext.computeChildSize(child, LayoutDirection.HORIZONTAL),
            heightContext.computeChildSize(child, LayoutDirection.VERTICAL),
        )
    }
}

internal open class LineUiLayout(
    open var direction: LayoutDirection = LayoutDirection.VERTICAL
) : UiLayout, LengthExtensions {
    val revDirection = direction.reversed
    override fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt {
        var sum = 0
        var max = 0
        val ctx = LayoutContext(available)
        val padding = container.layoutChildrenPadding
        container.forEachVisibleChild { child ->
            val size = ctx.computeChildSize(child)
            val main = size.getDirection(direction)
            val rev = size.getDirection(revDirection)

            //println(main)
            sum += main + padding
            max = kotlin.math.max(max, rev)
        }
        //println("${container.preferredSize} - ${container.minimumSize} - ${container.maximumSize}")

        return SizeInt(if (direction.horizontal) sum else max, if (direction.vertical) sum else max)
    }

    override fun relayout(container: UiContainer) {
        val bounds = container.bounds
        //println("$bounds: ${container.children}")
        //val ctx = Length.Context()
        //ctx.size = bounds.getSizeDirection(direction)
        val ctx = LayoutContext(bounds.size)

        val padding = container.layoutChildrenPadding
        val padding2 = padding * 2

        var sum = 0
        var cur = padding

        container.forEachVisibleChild { child ->
            val size = ctx.computeChildSize(child)
            val value = size.getDirection(direction)
            val childBounds = when (direction) {
                LayoutDirection.VERTICAL -> RectangleInt(0, cur, bounds.width, value)
                LayoutDirection.HORIZONTAL -> RectangleInt(cur, 0, value, bounds.height)
            }
            //println("$child: $childBounds")
            child.bounds = childBounds
            if (child is UiContainer) {
                child.layout?.relayout(child)
            }
            sum += value
            cur += value + padding
        }
    }
}

fun RectangleInt.getSizeDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun SizeLength.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun SizeInt.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
enum class LayoutDirection {
    VERTICAL, HORIZONTAL;

    val reversed get() = if (vertical) HORIZONTAL else VERTICAL

    val vertical get() = this == VERTICAL
    val horizontal get() = this == HORIZONTAL
}

private val DEFAULT_WIDTH = 128.0.pt
private val DEFAULT_HEIGHT = 32.0.pt

//var UiComponent.preferredSize by Extra.PropertyThis<UiComponent, Size> { Size(DEFAULT_WIDTH, DEFAULT_HEIGHT) }
internal var UiComponent.preferredSize by Extra.PropertyThis<UiComponent, SizeLength?> { null }
internal var UiComponent.minimumSize by Extra.PropertyThis<UiComponent, SizeLength> { SizeLength(null, null) }
internal var UiComponent.maximumSize by Extra.PropertyThis<UiComponent, SizeLength> { SizeLength(null, null) }

internal fun UiComponent.preferredSize(width: Length?, height: Length?) {
    preferredSize = SizeLength(width, height)
}

internal var UiComponent.preferredWidth: Length?
    get() = preferredSize?.width
    set(value) {
        preferredSize = SizeLength(value, preferredSize?.height)
    }
internal var UiComponent.preferredHeight: Length?
    get() = preferredSize?.height
    set(value) {
        preferredSize = SizeLength(preferredSize?.width, value)
    }

internal fun UiContainer.vertical(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.VERTICAL) }.also { it.parent = this }.also(block)
}

internal fun UiContainer.horizontal(block: UiContainer.() -> Unit): UiContainer {
    return UiContainer(app).also { it.layout = LineUiLayout(LayoutDirection.HORIZONTAL) }.also { it.parent = this }.also(block)
}
