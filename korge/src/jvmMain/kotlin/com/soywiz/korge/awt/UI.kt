package com.soywiz.korge.awt

import com.soywiz.kds.*
import com.soywiz.korev.*
import com.soywiz.korim.color.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.length.*

//fun NativeUiFactory.createApp() = UiApplication(this)

internal open class UiApplication constructor(val factory: NativeUiFactory) : Extra by Extra.Mixin() {
    fun wrapContainer(native: Any?): UiContainer = UiContainer(this, factory.wrapNativeContainer(native)).also { container ->
        container.onResize {
            //println("wrapContainer.container.onResize: ${container.bounds}")
            container.relayout()
        }
    }
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
    var focusable by component::focusable

    open fun copyFrom(that: UiComponent) {
        this.visible = that.visible
        this.enabled = that.enabled
        this.bounds = that.bounds
        this.focusable = that.focusable
    }

    fun onMouseEvent(block: (MouseEvent) -> Unit) = component.onMouseEvent(block)
    fun onFocus(block: (FocusEvent) -> Unit) = component.onFocus(block)
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

internal open class UiCheckBox(app: UiApplication, val checkBox: NativeUiFactory.NativeCheckBox = app.factory.createCheckBox()) : UiComponent(app, checkBox) {
    var text by checkBox::text
    var checked by checkBox::checked
    fun onChange(block: UiCheckBox.(Boolean) -> Unit) = checkBox.onChange { block(this, checked) }
}

internal open class UiComboBox<T>(app: UiApplication, val comboBox: NativeUiFactory.NativeComboBox<T> = app.factory.createComboBox()) : UiComponent(app, comboBox) {
    var items by comboBox::items
    var selectedItem by comboBox::selectedItem
    fun open() = comboBox.open()
    fun close() = comboBox.close()
    fun onChange(block: () -> Unit) = comboBox.onChange(block)
}

internal open class UiLabel(app: UiApplication, val label: NativeUiFactory.NativeLabel = app.factory.createLabel()) : UiComponent(app, label) {
    var text by label::text
    var icon by label::icon

    override fun copyFrom(that: UiComponent) {
        super.copyFrom(that)
        that as UiLabel
        this.text = that.text
    }
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

internal interface UiLayout {
    fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt
    fun relayout(container: UiContainer)
}

internal var UiContainer.layoutChildrenPadding by Extra.Property { 0 }

internal object UiFillLayout : UiLayout {
    override fun computePreferredSize(container: UiContainer, available: SizeInt): SizeInt = available.clone()

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

fun SizeLength.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
fun SizeInt.getDirection(direction: LayoutDirection) = if (direction == LayoutDirection.VERTICAL) height else width
enum class LayoutDirection {
    VERTICAL, HORIZONTAL;

    val reversed get() = if (vertical) HORIZONTAL else VERTICAL

    val vertical get() = this == VERTICAL
    val horizontal get() = this == HORIZONTAL
}

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
