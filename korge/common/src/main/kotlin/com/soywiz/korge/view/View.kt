package com.soywiz.korge.view

import com.soywiz.korge.component.Component
import com.soywiz.korge.event.EventDispatcher
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.ColorTransform
import com.soywiz.korio.async.CoroutineContextHolder
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.go
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.error.MustOverrideException
import com.soywiz.korio.math.toDegrees
import com.soywiz.korio.math.toRadians
import com.soywiz.korio.util.Cancellable
import com.soywiz.korio.util.Extra
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle
import kotlin.reflect.KClass

class CustomView(views: Views, override val autoFlush: Boolean = true) : View(views)

open class View(val views: Views) : Renderable, Updatable, Extra by Extra.Mixin(), EventDispatcher by EventDispatcher.Mixin(), CoroutineContextHolder {
	companion object {
		private val tempTransform = Matrix2d.Transform()
		//private val tempMatrix = Matrix2d()

		fun commonAncestor(left: View?, right: View?): View? {
			var l: View? = left
			var r: View? = right
			var lCount = l.ancestorCount
			var rCount = r.ancestorCount
			while (lCount != rCount) {
				if (lCount > rCount) {
					lCount--
					l = l?.parent
				} else {
					rCount--
					r = r?.parent
				}
				if (lCount < 0 && rCount < 0) break
			}
			return if (l == r) l else null
		}
	}

	override val coroutineContext: CoroutineContext get() = views.coroutineContext
	open var ratio: Double = 0.0
	open val autoFlush: Boolean = false
	var index: Int = 0; internal set
	var speed: Double = 1.0
	var parent: Container? = null; internal set
	var name: String? = null
	val id = views.lastId++
	var blendMode: BlendMode = BlendMode.INHERIT
		set(value) {
			if (field != value) {
				field = value
				invalidate()
			}
		}

	var _computedBlendMode: BlendMode = BlendMode.INHERIT

	// @TODO: Cache results
	val computedBlendMode: BlendMode
		get() {
			_ensureGlobal()
			return _computedBlendMode
		}

	private var _scaleX: Double = 1.0
	private var _scaleY: Double = 1.0
	private var _skewX: Double = 0.0
	private var _skewY: Double = 0.0
	private var _rotation: Double = 0.0

	val pos = Point2d()

	var x: Double; set(v) = run { ensureTransform(); if (pos.x != v) run { pos.x = v; invalidateMatrix() } }; get() = ensureTransform().pos.x
	var y: Double; set(v) = run { ensureTransform(); if (pos.y != v) run { pos.y = v; invalidateMatrix() } }; get() = ensureTransform().pos.y
	var scaleX: Double; set(v) = run { ensureTransform(); if (_scaleX != v) run { _scaleX = v; invalidateMatrix() } }; get() = ensureTransform()._scaleX
	var scaleY: Double; set(v) = run { ensureTransform(); if (_scaleY != v) run { _scaleY = v; invalidateMatrix() } }; get() = ensureTransform()._scaleY
	var skewX: Double; set(v) = run { ensureTransform(); if (_skewX != v) run { _skewX = v; invalidateMatrix() } }; get() = ensureTransform()._skewX
	var skewY: Double; set(v) = run { ensureTransform(); if (_skewY != v) run { _skewY = v; invalidateMatrix() } }; get() = ensureTransform()._skewY
	var rotation: Double; set(v) = run { ensureTransform(); if (_rotation != v) run { _rotation = v; invalidateMatrix() } }; get() = ensureTransform()._rotation

	var rotationDegrees: Double; set(v) = run { rotation = toRadians(v) }; get() = toDegrees(rotation)
	var scale: Double; get() = (scaleX + scaleY) / 2.0; set(v) = run { scaleX = v; scaleY = v }
	var globalX: Double get() = parent?.localToGlobalX(x, y) ?: x; set(value) = run { x = parent?.globalToLocalX(value, globalY) ?: value }
	var globalY: Double get() = parent?.localToGlobalY(x, y) ?: y; set(value) = run { y = parent?.globalToLocalY(globalX, value) ?: value }

	fun setSize(width: Double, height: Double) = _setSize(width, true, height, true)

	private fun _setSize(width: Double, swidth: Boolean, height: Double, sheight: Boolean) {
		//val bounds = parent?.getLocalBounds() ?: this.getLocalBounds()
		val bounds = this.getLocalBounds()
		if (swidth) scaleX = width / bounds.width
		if (sheight) scaleY = height / bounds.height
	}

	open var width: Double
		get() = getLocalBounds().width * scaleX
		set(value) {
			_setSize(value, true, 0.0, false)
		}

	open var height: Double
		get() = getLocalBounds().height * scaleY
		set(value) {
			_setSize(0.0, false, value, true)
		}

	private val _colorTransform = ColorTransform()
	private var _globalColorTransform = ColorTransform()

	var colorMul: Int get() = _colorTransform.colorMul; set(v) = run { _colorTransform.colorMul = v; invalidateColorTransform() }
	var colorAdd: Int get() = _colorTransform.colorAdd; set(v) = run { _colorTransform.colorAdd = v; invalidateColorTransform() }
	var alpha: Double get() = _colorTransform.mA; set(v) = run { _colorTransform.mA = v;invalidateColorTransform() }
	var colorTransform: ColorTransform get() = _colorTransform; set(v) = run { _colorTransform.copyFrom(v); invalidateColorTransform() }

	private fun invalidateColorTransform() {
		invalidate()
	}

	// Properties
	private val _props = linkedMapOf<String, String>()
	val props: Map<String, String> get() = _props

	fun hasProp(key: String) = key in _props
	fun getPropString(key: String, default: String = "") = _props[key] ?: default
	fun getPropInt(key: String, default: Int = 0) = _props[key]?.toIntOrNull() ?: default
	fun getPropDouble(key: String, default: Double = 0.0) = _props[key]?.toDoubleOrNull() ?: default

	fun addProp(key: String, value: String) {
		_props[key] = value
		val componentGen = views.propsTriggers[key]
		if (componentGen != null) {
			componentGen(this, key, value)
		}
	}

	fun addProps(values: Map<String, String>) {
		for (pair in values) addProp(pair.key, pair.value)
	}

	private fun ensureTransform() = this.apply {
		if (!validLocalProps) {
			validLocalProps = true
			val t = tempTransform.setMatrix(this._localMatrix)
			this.pos.x = t.x
			this.pos.y = t.y
			this._scaleX = t.scaleX
			this._scaleY = t.scaleY
			this._skewX = t.skewX
			this._skewY = t.skewY
			this._rotation = t.rotation
		}
	}

	@Suppress("NOTHING_TO_INLINE")
	inline fun setXY(x: Number, y: Number) {
		this.x = x.toDouble()
		this.y = y.toDouble()
	}

	val root: View get() = parent?.root ?: this

	var mouseEnabled: Boolean = true
	//var mouseChildren: Boolean = false
	var enabled: Boolean = true
	var visible: Boolean = true

	fun setMatrix(matrix: Matrix2d) {
		this._localMatrix.copyFrom(matrix)
		this.validLocalProps = false
	}

	fun setMatrixInterpolated(ratio: Double, l: Matrix2d, r: Matrix2d) {
		this._localMatrix.setToInterpolated(ratio, l, r)
		this.validLocalProps = false
	}

	fun setComputedTransform(transform: Matrix2d.Computed) {
		val m = transform.matrix
		val t = transform.transform
		_localMatrix.copyFrom(m)
		pos.x = t.x; pos.y = t.y
		_scaleX = t.scaleX; _scaleY = t.scaleY
		_skewX = t.skewY; _skewY = t.skewY
		_rotation = t.rotation
		validLocalProps = true
		validLocalMatrix = true
		invalidate()
	}

	private var _localMatrix = Matrix2d()
	var _globalMatrix = Matrix2d()
	private var _globalVersion = 0
	private var _globalMatrixInvVersion = 0
	private var _globalMatrixInv = Matrix2d()

	internal var validLocalProps = true
	internal var validLocalMatrix = true
	internal var validGlobal = false

	private var components: ArrayList<Component>? = null
	private var _componentsIt: ArrayList<Component>? = null
	private val componentsIt: ArrayList<Component>?
		get() {
			if (components != null) {
				if (_componentsIt == null) _componentsIt = ArrayList()
				_componentsIt!!.clear()
				_componentsIt!!.addAll(components!!)
			}
			return _componentsIt
		}

	inline fun <reified T : Component> getOrCreateComponent(noinline gen: (View) -> T): T = getOrCreateComponent(T::class, gen)

	fun removeComponent(c: Component): Unit = run { components?.remove(c) }
	//fun removeComponents(c: KClass<out Component>) = run { components?.removeAll { it.javaClass.isSubtypeOf(c) } }
	fun removeComponents(c: KClass<out Component>) = run { components?.removeAll { it::class == c } }

	fun removeAllComponents() = run { components?.clear() }

	fun addComponent(c: Component) {
		if (components == null) components = arrayListOf()
		components!! += c
		c.update(0)
	}

	fun addUpdatable(updatable: (dtMs: Int) -> Unit): Cancellable {
		val c = object : Component(this), Cancellable {
			override fun update(dtMs: Int) = run { updatable(dtMs) }

			override fun cancel(e: Throwable) = removeComponent(this)
		}
		addComponent(c)
		return c
	}

	fun <T : Component> getOrCreateComponent(clazz: KClass<T>, gen: (View) -> T): T {
		if (components == null) components = arrayListOf()
		//var component = components!!.firstOrNull { it::class.isSubtypeOf(clazz) }
		var component = components!!.firstOrNull { it::class == clazz }
		if (component == null) {
			component = gen(this)
			components!! += component
		}
		return component!! as T
	}

	var localMatrix: Matrix2d
		get() {
			if (validLocalMatrix) return _localMatrix
			validLocalMatrix = true
			_localMatrix.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
			return _localMatrix
		}
		set(value) {
			setMatrix(value)
			invalidate()
		}

	private fun _ensureGlobal() = this.apply {
		if (validGlobal) return@apply
		validGlobal = true
		if (parent != null) {
			_globalMatrix.multiply(localMatrix, parent!!.globalMatrix)
			_globalColorTransform.setToConcat(_colorTransform, parent!!.globalColorTransform)
			_computedBlendMode = if (blendMode == BlendMode.INHERIT) parent!!.computedBlendMode else blendMode
		} else {
			_globalMatrix.copyFrom(localMatrix)
			_globalColorTransform.copyFrom(_colorTransform)
			_computedBlendMode = if (blendMode == BlendMode.INHERIT) BlendMode.NORMAL else blendMode
		}
		_globalVersion++
	}

	var globalMatrix: Matrix2d
		get() = _ensureGlobal()._globalMatrix
		set(value) {
			if (parent != null) {
				this.localMatrix.multiply(value, parent!!.globalMatrixInv)
			} else {
				this.localMatrix.copyFrom(value)
			}
		}

	val globalColorTransform: ColorTransform get() = run { _ensureGlobal(); _globalColorTransform }
	val globalColorMul: Int get() = globalColorTransform.colorMul
	val globalColorAdd: Int get() = globalColorTransform.colorAdd
	val globalAlpha: Double get() = globalColorTransform.mA

	val localMouseX: Double get() = globalMatrixInv.transformX(views.input.mouse)
	val localMouseY: Double get() = globalMatrixInv.transformY(views.input.mouse)

	val globalMatrixInv: Matrix2d
		get() {
			_ensureGlobal()
			if (_globalMatrixInvVersion != _globalVersion) {
				_globalMatrixInvVersion = _globalVersion
				_globalMatrixInv.setToInverse(_globalMatrix)
			}
			return _globalMatrixInv
		}

	fun invalidateMatrix() {
		validLocalMatrix = false
		invalidate()
	}

	open fun invalidate() {
		validGlobal = false
	}

	fun render(ctx: RenderContext) {
		if (autoFlush) ctx.flush()
		render(ctx, globalMatrix)
	}

	override fun render(ctx: RenderContext, m: Matrix2d) {
	}

	@Suppress("RemoveCurlyBracesFromTemplate")
	override fun toString(): String {
		var out = "${this::class}($id)"
		if (x != 0.0 || y != 0.0) out += ":pos=($x,$y)"
		if (scaleX != 1.0 || scaleY != 1.0) out += ":scale=($scaleX,$scaleY)"
		if (skewX != 0.0 || skewY != 0.0) out += ":skew=($skewX,$skewY)"
		if (rotation != 0.0) out += ":rotation=(${rotationDegrees}º)"
		if (name != null) out += ":name=($name)"
		if (blendMode != BlendMode.INHERIT) out += ":blendMode=($blendMode)"
		return out
	}

	fun globalToLocalX(x: Double, y: Double): Double = globalMatrixInv.run { transformX(x, y) }
	fun globalToLocalY(x: Double, y: Double): Double = globalMatrixInv.run { transformY(x, y) }

	fun localToGlobalX(x: Double, y: Double): Double = globalMatrix.run { transformX(x, y) }
	fun localToGlobalY(x: Double, y: Double): Double = globalMatrix.run { transformY(x, y) }

	fun globalToLocal(p: Point2d, out: Point2d = Point2d()): Point2d = globalMatrixInv.run { transform(p.x, p.y, out) }
	fun localToGlobal(p: Point2d, out: Point2d = Point2d()): Point2d = globalMatrix.run { transform(p.x, p.y, out) }

	enum class HitTestType {
		BOUNDING, SHAPE
	}

	fun hitTest(x: Double, y: Double, type: HitTestType): View? = when (type) {
		HitTestType.SHAPE -> hitTest(x, y)
		HitTestType.BOUNDING -> hitTestBounding(x, y)
	}

	fun hitTest(pos: Point2d): View? = hitTest(pos.x, pos.y)

	fun hitTest(x: Double, y: Double): View? {
		if (!mouseEnabled) return null
		return hitTestInternal(x, y)
	}

	fun hitTestBounding(x: Double, y: Double): View? {
		if (!mouseEnabled) return null
		return hitTestBoundingInternal(x, y)
	}

	open fun hitTestInternal(x: Double, y: Double): View? {
		val bounds = getLocalBounds()
		val sLeft = bounds.left
		val sTop = bounds.top
		val sRight = bounds.right
		val sBottom = bounds.bottom
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom)) this else null
	}

	open fun hitTestBoundingInternal(x: Double, y: Double): View? {
		val bounds = getGlobalBounds()
		return if (bounds.contains(x, y)) this else null
	}

	protected fun checkGlobalBounds(x: Double, y: Double, sLeft: Double, sTop: Double, sRight: Double, sBottom: Double): Boolean {
		val lx = globalToLocalX(x, y)
		val ly = globalToLocalY(x, y)
		return lx >= sLeft && ly >= sTop && lx < sRight && ly < sBottom
	}

	open fun reset() {
		_localMatrix.setToIdentity()
		pos.setTo(0.0, 0.0)
		_scaleX = 1.0; _scaleY = 1.0
		_skewX = 0.0; _skewY = 0.0
		_rotation = 0.0
		validLocalMatrix = false
		validGlobal = false
		invalidate()
	}

	final override fun update(dtMs: Int) {
		val actualDtMs = (dtMs * speed).toInt()
		if (componentsIt != null) {
			for (c in componentsIt!!) c.update(actualDtMs)
		}
		updateInternal(actualDtMs)
	}

	open protected fun updateInternal(dtMs: Int) {
	}

	fun removeFromParent() {
		if (parent == null) return
		val p = parent!!
		for (i in index + 1 until p.children.size) p.children[i].index--
		p.children.removeAt(index)
		parent = null
		index = -1
	}

	//fun getConcatMatrix(target: View, out: Matrix2d = Matrix2d()): Matrix2d {
	//	var current: View? = this
	//	out.setToIdentity()
	//
	//	val views = arrayListOf<View>()
	//	while (current != null) {
	//		views += current
	//		if (current == target) break
	//		current = current.parent
	//	}
	//	for (view in views.reversed()) out.premultiply(view.localMatrix)
	//
	//	return out
	//}

	fun getConcatMatrix(target: View, out: Matrix2d = Matrix2d()): Matrix2d {
		var current: View? = this
		out.setToIdentity()

		while (current != null) {
			//out.premultiply(current.localMatrix)
			out.multiply(out, current.localMatrix)
			if (current == target) break
			current = current.parent
		}

		return out
	}

	val globalBounds: Rectangle get() = getGlobalBounds()
	fun getGlobalBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(this.root, out)

	fun getBounds(target: View? = this, out: Rectangle = Rectangle()): Rectangle {
		//val concat = (parent ?: this).getConcatMatrix(target ?: this)
		val concat = (this).getConcatMatrix(target ?: this)
		val bb = BoundsBuilder()

		getLocalBoundsInternal(out)

		val p1 = Point2d(out.left, out.top)
		val p2 = Point2d(out.right, out.top)
		val p3 = Point2d(out.right, out.bottom)
		val p4 = Point2d(out.left, out.bottom)

		bb.add(concat.transformX(p1.x, p1.y), concat.transformY(p1.x, p1.y))
		bb.add(concat.transformX(p2.x, p2.y), concat.transformY(p2.x, p2.y))
		bb.add(concat.transformX(p3.x, p3.y), concat.transformY(p3.x, p3.y))
		bb.add(concat.transformX(p4.x, p4.y), concat.transformY(p4.x, p4.y))

		bb.getBounds(out)
		return out
	}

	fun getLocalBounds(out: Rectangle = Rectangle()) = out.apply { getLocalBoundsInternal(out) }

	open fun getLocalBoundsInternal(out: Rectangle = Rectangle()) {
		out.setTo(0, 0, 0, 0)
	}

	open protected fun createInstance(): View = throw MustOverrideException("Must Override ${this::class}.createInstance()")

	open fun copyPropsFrom(source: View) {
		this.name = source.name
		this.colorAdd = source.colorAdd
		this.colorMul = source.colorMul
		this.setMatrix(source.localMatrix)
		this.visible = source.visible
		this.ratio = source.ratio
		this.speed = source.speed
		this.blendMode = source.blendMode
	}

	open fun clone(): View = createInstance().apply {
		this@apply.copyPropsFrom(this@View)
	}
}

class DummyView(views: Views) : View(views) {
	override fun createInstance(): View = DummyView(views)
}

fun View.hasAncestor(ancestor: View): Boolean {
	return if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false
}

fun View.replaceWith(view: View): Boolean {
	if (this == view) return false
	if (parent == null) return false
	view.parent?.children?.remove(view)
	parent!!.children[this.index] = view
	view.index = this.index
	view.parent = parent
	parent = null
	view.invalidate()
	this.index = -1
	return true
}

val View?.ancestorCount: Int get() = this?.parent?.ancestorCount?.plus(1) ?: 0

suspend fun Updatable.updateLoop(eventLoop: EventLoop, step: Int = 10, callback: suspend () -> Unit) {
	val view = this
	var done = false
	go {
		while (!done) {
			view.update(step)
			eventLoop.step(step)
			eventLoop.sleep(1)
		}
	}
	val p = go {
		callback()
	}
	try {
		p.await()
	} finally {
		done = true
	}
}

fun View?.ancestorsUpTo(target: View?): List<View> {
	var current = this
	val out = arrayListOf<View>()
	while (current != null && current != target) {
		out += current
		current = current.parent
	}
	return out
}

val View?.ancestors: List<View> get() = ancestorsUpTo(null)
