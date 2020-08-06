@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "NOTHING_TO_INLINE")

package com.soywiz.korui.ui

import com.soywiz.kds.*
import com.soywiz.korag.*
import com.soywiz.korev.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*
import com.soywiz.korev.*
import com.soywiz.korui.geom.len.*
import com.soywiz.korui.light.*
import com.soywiz.korui.style.*
import kotlin.reflect.*

open class Component(override val app: Application, val type: LightType, val lightConfig: Any? = null) : Styled, Extra by Extra.Mixin(),
    EventDispatcher, ApplicationAware {
	val coroutineContext = app.coroutineContext
	val lc = app.light

	class lightProperty<T>(
		val key: LightProperty<T>,
		val getable: Boolean = false,
		val setHandler: ((v: T) -> Unit)? = null
	) {
		inline operator fun getValue(thisRef: Component, property: KProperty<*>): T {
			if (getable) return thisRef.lc.getProperty(thisRef.handle, key)
			return thisRef.getProperty(key)
		}

		inline operator fun setValue(thisRef: Component, property: KProperty<*>, value: T): Unit = run {
			thisRef.setProperty(key, value)
			setHandler?.invoke(value)
		}
	}

	override var style = Style()
	val componentInfo = lc.create(type, lightConfig)
	var handle: Any = componentInfo.handle
	val properties = LinkedHashMap<LightProperty<*>, Any?>()
	var valid = false
	protected var nativeBounds = RectangleInt()
	val actualBounds: RectangleInt = RectangleInt()
	val eventListener by lazy { lc.getEventListener(handle) }

	override fun <T : Event> addEventListener(clazz: KClass<T>, handler: (T) -> Unit): Closeable =
		eventListener.addEventListener(clazz, handler)

	override fun <T : Event> dispatch(clazz: KClass<T>, event: T) = eventListener.dispatch(clazz, event)

	val actualWidth: Int get() = actualBounds.width
	val actualHeight: Int get() = actualBounds.height

	fun <T> setProperty(key: LightProperty<T>, value: T, reset: Boolean = false) {
		if (reset || (properties[key] != value)) {
			properties[key] = value
			lc.setProperty(handle, key, value)
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <T> getProperty(key: LightProperty<T>): T = if (key in properties) properties[key] as T else key.default

	fun setBoundsInternal(bounds: RectangleInt) = setBoundsInternal(bounds.x, bounds.y, bounds.width, bounds.height)

	fun relayout(): RectangleInt = setBoundsAndRelayout(actualBounds)

	fun setBoundsInternal(x: Int, y: Int, width: Int, height: Int): RectangleInt {
		//val changed = (actualBounds.x != x || actualBounds.y != y || actualBounds.width != width || actualBounds.height != height)
		val resized = ((nativeBounds.width != width) || (nativeBounds.height != height))
		nativeBounds.setTo(x, y, width, height)
		//println("$actualBounds: $width,$height")
		actualBounds.setTo(x, y, width, height)
		lc.setBounds(handle, x, y, width, height)
		if (resized) {
			onResized(x, y, width, height)
			repaint()
		}
		//invalidateAncestors()
		return actualBounds
	}

	protected open fun onResized(x: Int, y: Int, width: Int, height: Int) {
	}

	open fun repaint() {
	}

	open fun recreate() {
		handle = lc.create(type, null)
		lc.setBounds(handle, nativeBounds.x, nativeBounds.y, nativeBounds.width, nativeBounds.height)
		for ((key, value) in properties) {
			lc.setProperty(handle, key, value)
		}
		lc.setParent(handle, parent?.handle)
	}

	open var parent: Container? = null
		set(newParent) {
			if (field != newParent) {
				val old = field
				old?.children?.remove(this)
				field = newParent
				newParent?.children?.add(this)
				lc.setParent(handle, newParent?.handle)
				//invalidate()
				newParent?.invalidate()
				ancestorChanged(old, newParent)
			}
		}

	val root: Container? get() = parent?.root ?: (this as? Container?)
	val parentFrame: Frame? get() = (this as? Frame?) ?: parent?.parentFrame

	open fun ancestorChanged(old: Container?, newParent: Container?) {
	}

	fun invalidate() {
		//println("------invalidate")
		invalidateAncestors()
		invalidateDescendants()
	}

	open fun invalidateDescendants() {
		//println("------invalidateDescendants")
		valid = false
	}

	fun invalidateAncestors() {
		//println("------invalidateAncestors")
		if (!valid) return
		valid = false
		parent?.invalidateAncestors()
	}

	open fun setBoundsAndRelayout(x: Int, y: Int, width: Int, height: Int): RectangleInt {
		if (valid) return actualBounds
		valid = true
		return setBoundsInternal(x, y, width, height)
	}

	fun setBoundsAndRelayout(rect: RectangleInt) = setBoundsAndRelayout(rect.x, rect.y, rect.width, rect.height)

	//fun onClick(handler: (LightClickEvent) -> Unit) {
	//	lc.setEventHandler<LightClickEvent>(handle, handler)
	//}

	var mouseX = 0
	var mouseY = 0

	var visible by lightProperty(LightProperty.VISIBLE)

	override fun toString(): String = "Component($type)"

	fun focus() {
		lc.callAction(handle, LightAction.FOCUS, null)
	}

	fun copyStateFrom(props: Map<LightProperty<*>, Any?>) {
		for ((prop, value) in props) {
			this.setProperty(prop, value)
		}
	}

	open fun copyStateFrom(other: Component) {
		copyStateFrom(other.properties)
		this.style = other.style
		this.eventListener.copyFrom(other.eventListener)
		//style.copyFrom(other.style)
	}

	open fun clone(newApp: Application): Component = Component(newApp, type).also { it.copyStateFrom(this) }
}

fun <T : Container> T.addBlock(block: T.() -> Unit): T {
    block(this)
    return this
}

open class Container(app: Application, var layout: Layout, type: LightType = LightType.CONTAINER) :
	Component(app, type) {
	val children = arrayListOf<Component>()

	override fun recreate() {
		super.recreate()
		for (child in children) child.recreate()
	}

	override fun invalidateDescendants() {
		super.invalidateDescendants()
		for (child in children) child.invalidateDescendants()
	}

	override fun setBoundsAndRelayout(x: Int, y: Int, width: Int, height: Int): RectangleInt {
		//println("relayout:$valid")
		if (valid) return actualBounds
		//println("$this: relayout")
		valid = true
		return setBoundsInternal(layout.applyLayout(this, children, x, y, width, height, out = actualBounds))
	}

	fun <T : Component> add(other: T): T {
		other.parent = this
		return other
	}

    fun <T : Component> remove(other: T): T {
		other.parent = null
		return other
	}

	fun removeAll(): Unit {
		for (child in this.children.toList().reversed()) child.parent = null
	}

	override fun ancestorChanged(old: Container?, newParent: Container?) {
		for (child in children) child.ancestorChanged(old, newParent)
	}

	override fun clone(newApp: Application): Container = Container(newApp, layout, type).also { it.copyStateFrom(this) }

	override fun toString(): String = "Container($type)"
}

open class ScrollPane(app: Application, layout: Layout) : Container(app, layout, LightType.SCROLL_PANE) {
	override fun clone(newApp: Application): ScrollPane = ScrollPane(newApp, layout).also { it.copyStateFrom(this) }
	override fun toString(): String = "ScrollPane"
}

class TabPane(app: Application) : Container(app, LayeredLayout(app), LightType.TABPANE) {
	override fun clone(newApp: Application): TabPane = TabPane(newApp).also { it.copyStateFrom(this) }
	override fun toString(): String = "TabPane"
}

class TabPage(app: Application) : Container(app, LayeredLayout(app), LightType.TABPAGE) {
	var title by lightProperty(LightProperty.NAME, getable = true)
	override fun clone(newApp: Application): TabPage = TabPage(newApp).also { it.copyStateFrom(this) }
	override fun toString(): String = "TabPage"
}

class Frame(app: Application, title: String) : Container(app, LayeredLayout(app), LightType.FRAME) {
	var title by lightProperty(LightProperty.TEXT)
	var icon by lightProperty(LightProperty.ICON)
	var bgcolor by lightProperty(LightProperty.BGCOLOR)

	init {
		this.title = title
	}

	suspend fun dialogOpenFile(filter: String = ""): VfsFile {
		if (!lc.insideEventHandler) throw IllegalStateException("Can't open file dialog outside an event")
		return lc.dialogOpenFile(handle, filter)
	}

	suspend fun prompt(message: String, initialValue: String = ""): String =
		lc.dialogPrompt(handle, message, initialValue)

	suspend fun alert(message: String): Unit = lc.dialogAlert(handle, message)
	fun openURL(url: String): Unit = lc.openURL(url)

	fun onDropFiles(
		enter: () -> Boolean,
		exit: () -> Unit,
		drop: (List<VfsFile>) -> Unit
	): Closeable {
		return eventListener.addEventListener<DropFileEvent> {
			when (it.type) {
				DropFileEvent.Type.ENTER -> {
					enter()
				}
				DropFileEvent.Type.EXIT -> {
					exit()
				}
				DropFileEvent.Type.DROP -> {
					drop(it.files ?: listOf())
				}
			}
		}
	}

	override fun clone(newApp: Application) = Frame(newApp, title).also { it.copyStateFrom(this) }
	override fun toString(): String = "Frame"
}

class AgCanvas(app: Application, val config: AGConfig = AGConfig()) : Component(app, LightType.AGCANVAS, config), AGContainer {
	override val ag = componentInfo.ag ?: error("AgCanvas:componentInfo.ag == null")

	override fun repaint() {
		ag.repaint()
	}

	override fun onResized(x: Int, y: Int, width: Int, height: Int) {
		super.onResized(x, y, width, height)
        val agWidth = (width * lc.xScale).toInt()
        val agHeight = (height * lc.yScale).toInt()
        //println("onResized:($x,$y,$width,$height),ag=($agWidth,$agHeight),lc.xScale=${lc.xScale}")
		ag.resized(agWidth, agHeight)
        //ag.repaint()
	}

	suspend fun waitReady() {
		ag.onReady.await()
	}

	fun onRender(callback: (ag: AG) -> Unit) {
		ag.onRender { callback(it) }
	}

	override fun clone(newApp: Application) = AgCanvas(newApp, config).also { it.copyStateFrom(this) }
	override fun toString(): String = "AGCanvas"
}

class Button(app: Application, text: String) : Component(app, LightType.BUTTON) {
	var text by lightProperty(LightProperty.TEXT)

	init {
		this.text = text
	}

    fun onClick(block: (MouseEvent) -> Unit) {
        addEventListener<MouseEvent> {
            if (it.type == MouseEvent.Type.CLICK) {
                block(it)
            }
        }
    }
	
	override fun clone(newApp: Application) = Button(newApp, text).also { it.copyStateFrom(this) }
	override fun toString(): String = "Button"
}

class ComboBox<T>(app: Application, items: List<T>, private val toString: (T) -> String = { it.toString() }) :
	Component(app, LightType.COMBO_BOX) {
	private var rawItems = listOf<T>()
	private var internalItems by lightProperty(LightProperty.COMBO_BOX_ITEMS)
	var selectedIndex: Int by lightProperty(LightProperty.SELECTED_INDEX, getable = true)

	var items: List<T>
		set(value) {
			rawItems = value.toList()
			internalItems = rawItems.map { ComboBoxItem(it, toString(it)) }
		}
		get() = rawItems

	var selectedItem: T?
		set(value) = run { selectedIndex = rawItems.indexOf(value) }
		get() = rawItems.getOrNull(selectedIndex)

	init {
		this.items = items
	}

	fun change(callback: () -> Unit) {
		addEventListener<ChangeEvent> {
			callback()
		}
	}

	override fun clone(newApp: Application) = ComboBox(newApp, items).also { it.copyStateFrom(this) }
	override fun toString(): String = "ComboBox"
}

class Label(app: Application, text: String) : Component(app, LightType.LABEL) {
	var text by lightProperty(LightProperty.TEXT)

	init {
		this.text = text
	}

	override fun clone(newApp: Application) = Label(newApp, text).also { it.copyStateFrom(this) }
	override fun toString(): String = "Label"
}

class TextField(app: Application, text: String) : Component(app, LightType.TEXT_FIELD) {
	var text by lightProperty(LightProperty.TEXT, getable = true)

	init {
		this.text = text
	}

	override fun clone(newApp: Application) = Label(newApp, text).also { it.copyStateFrom(this) }
	override fun toString(): String = "TextField"
}

class TextArea(app: Application, text: String) : Component(app, LightType.TEXT_AREA) {
	var text by lightProperty(LightProperty.TEXT, getable = true)

	init {
		this.text = text
	}

	override fun clone(newApp: Application) = Label(newApp, text).also { it.copyStateFrom(this) }
	override fun toString(): String = "TextArea"
}

class CheckBox(app: Application, text: String, initialChecked: Boolean) : Component(app, LightType.CHECK_BOX) {
	var text by lightProperty(LightProperty.TEXT)
	var checked by lightProperty(LightProperty.CHECKED, getable = true)

	init {
		this.text = text
		this.checked = initialChecked
	}

	override fun clone(newApp: Application) = CheckBox(newApp, text, checked).also { it.copyStateFrom(this) }
	override fun toString(): String = "CheckBox"
}

class RadioButtonGroup {
	internal val mradios = arrayListOf<RadioButton>()
	val radios: List<RadioButton> = mradios
	var selected: RadioButton?
		get() = radios.firstOrNull { it.checked }
		set(value) {
			for (radio in radios) radio.internalChecked = (radio === value)
		}
	override fun toString(): String = "RadioButtonGroup"
}

class RadioButton(app: Application, initialGroup: RadioButtonGroup, text: String, initialChecked: Boolean) :
	Component(app, LightType.RADIO_BUTTON) {
	var text by lightProperty(LightProperty.TEXT)
	internal var internalChecked by lightProperty(LightProperty.CHECKED, getable = true)
	var checked: Boolean
		get() = internalChecked
		set(value) = run { if (value) group.selected = this }
	var group: RadioButtonGroup = initialGroup.apply { mradios.add(this@RadioButton) }
		set(value) {
			field.mradios -= this
			field = value
			field.mradios += this
			if (checked) group.selected = this
		}

	init {
		this.text = text
		this.checked = initialChecked
		addEventListener<ChangeEvent> {
			if (checked) {
				for (radio in group.radios) {
					if (radio != this) radio.checked = false
				}
			}
		}
	}

	override fun clone(newApp: Application) = RadioButton(newApp, group, text, checked).also { it.copyStateFrom(this) }
	override fun toString(): String = "RadioButton"
}

class Progress(app: Application, current: Int = 0, max: Int = 100) : Component(app, LightType.PROGRESS) {
	var current by lightProperty(LightProperty.PROGRESS_CURRENT)
	var max by lightProperty(LightProperty.PROGRESS_MAX)

	fun set(current: Int, max: Int) {
		this.current = current
		this.max = max
	}

	init {
		set(current, max)
	}

	override fun clone(newApp: Application) = Progress(newApp, current, max).also { it.copyStateFrom(this) }
	override fun toString(): String = "Progress"
}

class Slider(app: Application, current: Int = 0, max: Int = 100) : Component(app, LightType.SLIDER) {
	var current by lightProperty(LightProperty.PROGRESS_CURRENT, getable = true)
	var max by lightProperty(LightProperty.PROGRESS_MAX, getable = true)

	fun set(current: Int, max: Int) {
		this.current = current
		this.max = max
	}

	init {
		set(current, max)
	}

	fun onUpdate(callback: (Int) -> Unit) {
		addEventListener<ChangeEvent> {
			callback(this.current)
		}
	}

	override fun clone(newApp: Application) = Slider(newApp, current, max).also { it.copyStateFrom(this) }
	override fun toString(): String = "Slider"
}

class Spacer(app: Application) : Component(app, LightType.CONTAINER) {
	override fun clone(newApp: Application) = Spacer(newApp).also { it.copyStateFrom(this) }
	override fun toString(): String = "Spacer"
}

class Image(app: Application) : Component(app, LightType.IMAGE) {
	var image by lightProperty(LightProperty.IMAGE) {
		if (it != null) {
			if (this.style.defaultSize.width != it.width.pt || this.style.defaultSize.height != it.height.pt) {
				this.style.defaultSize.setTo(it.width.pt, it.height.pt)
			}
			invalidate()
		}
	}

	var smooth by lightProperty(LightProperty.IMAGE_SMOOTH)

	fun refreshImage() {
		setProperty(LightProperty.IMAGE, image, reset = true)
	}

	override fun copyStateFrom(other: Component) {
		other as Image
		super.copyStateFrom(other)
		this.image = other.image
	}

	override fun clone(newApp: Application) = Image(newApp).also { it.copyStateFrom(this) }
	override fun toString(): String = "Image"
}

open class CustomComponent(app: Application) : Container(app, LayeredLayout(app)) {
	override fun clone(newApp: Application) = CustomComponent(newApp).also { it.copyStateFrom(this) }
}

abstract class BaseCanvas(app: Application) : Container(app, LayeredLayout(app)) {
	private val img = image(NativeImage(1, 1))
	var antialiased = true
	var highDpi = true

	override fun onResized(x: Int, y: Int, width: Int, height: Int) {
		super.onResized(x, y, width, height)
		repaint(width, height)
	}

	override fun repaint() {
		repaint(actualWidth, actualHeight)
	}

	private fun repaint(width: Int, height: Int) {
		val scale = if (highDpi) app.devicePixelRatio else 1.0
		//val scale = 1.0
		val rwidth = (width * scale).toInt()
		val rheight = (height * scale).toInt()
		val image = NativeImage(rwidth, rheight)
		val ctx = image.getContext2d(antialiasing = antialiased).withScaledRenderer(scale)
		//val ctx = image.getContext2d(antialiasing = antialiased)
		ctx.render()
		img.image = image
	}

	abstract fun Context2d.render(): Unit
}

class VectorImage(app: Application) : BaseCanvas(app) {
	var d: Drawable? = null
	var targetWidth: Int? = null
	var targetHeight: Int? = null

	fun setVector(d: Drawable?, width: Int?, height: Int?) {
		this.d = d
		this.targetWidth = width
		this.targetHeight = height
		invalidate()
	}

	override fun Context2d.render() {
		val twidth = targetWidth
		val theight = targetHeight
		val sx = if (twidth != null) width.toDouble() / twidth.toDouble() else 1.0
		val sy = if (theight != null) height.toDouble() / theight.toDouble() else 1.0

		d?.draw(this.withScaledRenderer(sx, sy))
		//d.draw(this)
	}

	override fun copyStateFrom(other: Component) {
		other as VectorImage
		super.copyStateFrom(other)
		setVector(other.d, other.targetWidth, other.targetHeight)
	}

	override fun clone(newApp: Application) = VectorImage(newApp).also { it.copyStateFrom(this) }
}


//fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.style.size.setTo(width, height) }

inline fun Container.button(text: String, callback: (@ComponentDslMarker Button).() -> Unit = {}): Button =
	add(Button(this.app, text).apply(callback))

inline fun Container.progress(current: Int, max: Int, callback: (@ComponentDslMarker Progress).() -> Unit = {}) =
	add(Progress(this.app, current, max).apply(callback))

inline fun Container.slider(current: Int, max: Int, callback: (@ComponentDslMarker Slider).() -> Unit = {}) =
	add(Slider(this.app, current, max).apply(callback))

inline fun Container.agCanvas(config: AGConfig = AGConfig(), callback: (@ComponentDslMarker AgCanvas).() -> Unit = {}) =
	add(AgCanvas(this.app, config).apply(callback))

inline fun Container.image(bitmap: Bitmap, callback: (@ComponentDslMarker Image).() -> Unit) =
	add(Image(this.app).apply(callback).apply { image = bitmap })

inline fun Container.image(bitmap: Bitmap) = add(Image(this.app).apply {
	image = bitmap
	this.style.defaultSize.width = bitmap.width.pt
	this.style.defaultSize.height = bitmap.height.pt
})

inline fun Container.spacer() = add(Spacer(this.app))

inline fun Container.label(text: String, callback: (@ComponentDslMarker Label).() -> Unit = {}) =
	add(Label(this.app, text).apply(callback))

inline fun <T> Container.comboBox(
	vararg items: T,
	noinline toString: (T) -> String = { it.toString() },
	callback: (@ComponentDslMarker ComboBox<T>).() -> Unit = {}
): ComboBox<T> =
	add(ComboBox(this.app, items.toList(), toString).apply(callback))

inline fun Container.checkBox(
	text: String,
	checked: Boolean = false,
	callback: (@ComponentDslMarker CheckBox).() -> Unit = {}
) = add(CheckBox(this.app, text, checked).apply(callback))

inline fun Container.radioButton(
	group: RadioButtonGroup,
	text: String,
	checked: Boolean = false,
	callback: (@ComponentDslMarker RadioButton).() -> Unit = {}
) = add(RadioButton(this.app, group, text, checked).apply(callback))

inline fun Container.textField(text: String = "", callback: (@ComponentDslMarker TextField).() -> Unit = {}) =
	add(TextField(this.app, text).apply { callback(this) })

inline fun Container.textArea(text: String = "", callback: (@ComponentDslMarker TextArea).() -> Unit = {}) =
	add(TextArea(this.app, text).apply { callback(this) })

inline fun Container.layers(callback: (@ComponentDslMarker Container).() -> Unit): Container =
	add(Container(this.app, LayeredLayout(app)).apply(callback))

inline fun Container.layersKeepAspectRatio(
	anchor: Anchor = Anchor.MIDDLE_CENTER,
	scaleMode: ScaleMode = ScaleMode.SHOW_ALL,
	callback: Container.() -> Unit
): Container {
	return add(Container(this.app, LayeredKeepAspectLayout(app, anchor, scaleMode)).apply(callback))
}

inline fun Container.vertical(callback: (@ComponentDslMarker Container).() -> Unit): Container =
	add(Container(this.app, VerticalLayout(app)).apply(callback))

inline fun Container.horizontal(callback: (@ComponentDslMarker Container).() -> Unit): Container =
	add(Container(this.app, HorizontalLayout(app)).apply(callback))

inline fun Container.inline(callback: (@ComponentDslMarker Container).() -> Unit): Container =
	add(Container(this.app, InlineLayout(app)).apply(callback))

inline fun Container.relative(callback: (@ComponentDslMarker Container).() -> Unit): Container =
	add(Container(this.app, RelativeLayout(app)).apply(callback))

inline fun Container.scrollPane(callback: (@ComponentDslMarker ScrollPane).() -> Unit): ScrollPane =
	add(ScrollPane(this.app, ScrollPaneLayout(app)).apply(callback))

inline fun Container.tabPane(callback: (@ComponentDslMarker TabPane).() -> Unit): TabPane =
	add(TabPane(this.app).apply(callback))

inline fun TabPane.page(title: String, callback: (@ComponentDslMarker TabPage).() -> Unit): TabPage =
	add(TabPage(this.app).apply { this.title = title }.apply(callback))

inline fun Container.vectorImage(vector: SizedDrawable, callback: VectorImage.() -> Unit = {}) =
	add(VectorImage(this.app).apply {
		setVector(vector, vector.width, vector.height)
		callback(this)
	})

inline fun Container.vectorImage(
	vector: Drawable,
	crossinline callback: VectorImage.() -> Unit = {}
) = add(VectorImage(this.app).apply {
	setVector(vector, null, null)
	callback(this)
})


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ComponentDslMarker
