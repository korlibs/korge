package com.soywiz.korge.view

import com.soywiz.korge.component.Component
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.Cancellable
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.clamp
import com.soywiz.korio.util.isSubtypeOf
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.geom.Rectangle

open class View(val views: Views) : Renderable, Extra by Extra.Mixin() {
	var index: Int = 0
	var speed: Double = 1.0
	var parent: Container? = null
	var name: String? = null
	val id = views.lastId++
	var blendMode: BlendMode = BlendMode.INHERIT
	var alpha: Double = 1.0; set(v) = run {
		if (field != v) {
			val vv = v.clamp(0.0, 1.0)
			if (field != vv) {
				field = vv; invalidateMatrix()
			}
		}
	}
	private var _x: Double = 0.0
	private var _y: Double = 0.0
	private var _scaleX: Double = 1.0
	private var _scaleY: Double = 1.0
	private var _skewX: Double = 0.0
	private var _skewY: Double = 0.0
	private var _rotation: Double = 0.0

	private val _props = linkedMapOf<String, String>()
	val props: Map<String, String> = _props

	fun hasProp(key: String) = key in props
	fun getPropString(key: String, default: String = "") = props[key] ?: default
	fun getPropInt(key: String, default: Int = 0) = props[key]?.toIntOrNull() ?: default
	fun getPropDouble(key: String, default: Double = 0.0) = props[key]?.toDoubleOrNull() ?: default

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

	var x: Double; set(v) = run { if (_x != v) run { _x = v; invalidateMatrix() } }; get() = _x
	var y: Double; set(v) = run { if (_y != v) run { _y = v; invalidateMatrix() } }; get() = _y
	var scaleX: Double; set(v) = run { if (_scaleX != v) run { _scaleX = v; invalidateMatrix() } }; get() = _scaleX
	var scaleY: Double; set(v) = run { if (_scaleY != v) run { _scaleY = v; invalidateMatrix() } }; get() = _scaleY
	var skewX: Double; set(v) = run { if (_skewX != v) run { _skewX = v; invalidateMatrix() } }; get() = _skewX
	var skewY: Double; set(v) = run { if (_skewY != v) run { _skewY = v; invalidateMatrix() } }; get() = _skewY
	var rotation: Double; set(v) = run { if (_rotation != v) run { _rotation = v; invalidateMatrix() } }; get() = _rotation
	var rotationDegrees: Double; set(v) = run { rotation = Math.toRadians(v) }; get() = Math.toDegrees(rotation)

	var scale: Double; get() = (scaleX + scaleY) / 2.0; set(v) = run { scaleX = v; scaleY = v }

	var globalX: Double get() {
		return parent?.localToGlobalX(x, y) ?: x
	}
		set(value) {
			x = parent?.globalToLocalX(value, globalY) ?: value
		}

	var globalY: Double get() {
		return parent?.localToGlobalY(x, y) ?: y
	}
		set(value) {
			y = parent?.globalToLocalY(globalX, value) ?: value
		}

	@Suppress("NOTHING_TO_INLINE")
	inline fun setXY(x: Number, y: Number) {
		this.x = x.toDouble()
		this.y = y.toDouble()
	}

	val root: View get() = parent?.root ?: this

	var mouseEnabled: Boolean = true
	var enabled: Boolean = true
	var visible: Boolean = true

	fun setMatrix(matrix: Matrix2d) {
		this._localMatrix.copyFrom(matrix)
		validLocal = true
		invalidate()
	}

	companion object {
		private val tempTransform = Matrix2d.Transform()
	}

	fun setMatrixInterpolated(ratio: Double, l: Matrix2d, r: Matrix2d) {
		this._localMatrix.setToInterpolated(ratio, l, r)
		tempTransform.setMatrix(this._localMatrix)
		this._x = tempTransform.x
		this._y = tempTransform.y
		this._scaleX = tempTransform.scaleX
		this._scaleY = tempTransform.scaleY
		this._skewX = tempTransform.skewX
		this._skewY = tempTransform.skewY
		this._rotation = tempTransform.rotation
		validLocal = true
		invalidate()
	}

	fun setComputedTransform(transform: Matrix2d.Computed) {
		val m = transform.matrix
		val t = transform.transform
		_localMatrix.copyFrom(m)
		_x = t.x; _y = t.y
		_scaleX = t.scaleX; _scaleY = t.scaleY
		_skewX = t.skewY; _skewY = t.skewY
		_rotation = t.rotation
		validLocal = true
		invalidate()
	}

	private var _localMatrix = Matrix2d()
	var _globalMatrix = Matrix2d()
	private var _globalMatrixVersion = 0
	private var _globalMatrixInvVersion = 0
	private var _globalMatrixInv = Matrix2d()

	internal var validLocal = false
	internal var validGlobal = false

	private var _globalAlpha: Double = 1.0
	private var _globalCol1: Int = -1

	private var components: ArrayList<Component>? = null
	private var _componentsIt: ArrayList<Component>? = null
	private val componentsIt: ArrayList<Component>? get() {
		if (components != null) {
			if (_componentsIt == null) _componentsIt = ArrayList()
			_componentsIt!!.clear()
			_componentsIt!!.addAll(components!!)
		}
		return _componentsIt
	}

	inline fun <reified T : Component> getOrCreateComponent(noinline gen: (View) -> T): T = getOrCreateComponent(T::class.java, gen)

	fun removeComponent(c: Component) {
		components?.remove(c)
	}

	fun removeComponents(c: Class<out Component>) {
		components?.removeAll { it.javaClass.isSubtypeOf(c) }
	}

	fun addComponent(c: Component) {
		if (components == null) components = arrayListOf()
		components!! += c
		c.update(0)
	}

	fun addUpdatable(updatable: (dtMs: Int) -> Unit): Cancellable {
		val c = object : Component(this), Cancellable {
			override fun update(dtMs: Int) {
				updatable(dtMs)
			}

			override fun cancel(e: Throwable) = removeComponent(this)
		}
		addComponent(c)
		return c
	}

	fun <T : Component> getOrCreateComponent(clazz: Class<T>, gen: (View) -> T): T {
		if (components == null) components = arrayListOf()
		var component = components!!.firstOrNull { it.javaClass.isSubtypeOf(clazz) }
		if (component == null) {
			component = gen(this)
			components!! += component
		}
		return component!! as T
	}

	val localMatrix: Matrix2d get() {
		if (validLocal) return _localMatrix
		validLocal = true
		_localMatrix.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
		return _localMatrix
	}

	private fun _ensureGlobal() = this.apply {
		if (validGlobal) return@apply
		validGlobal = true
		if (parent != null) {
			_globalMatrix.copyFrom(parent!!.globalMatrix)
			_globalMatrix.premultiply(localMatrix)
		} else {
			_globalMatrix.copyFrom(localMatrix)
		}
		_globalAlpha = if (parent != null) parent!!.globalAlpha * alpha else alpha
		_globalCol1 = RGBA.packf(1f, 1f, 1f, _globalAlpha.toFloat())
		_globalMatrixVersion++
	}

	val globalMatrix: Matrix2d get() = _ensureGlobal()._globalMatrix

	protected val globalAlpha: Double get() = run { globalMatrix; _globalAlpha }
	protected val globalCol1: Int get() = run { globalMatrix; _globalCol1 }

	val localMouseX: Double get() = globalMatrixInv.transformX(views.input.mouse)
	val localMouseY: Double get() = globalMatrixInv.transformY(views.input.mouse)

	val globalMatrixInv: Matrix2d get() {
		_ensureGlobal()
		if (_globalMatrixInvVersion != _globalMatrixVersion) {
			_globalMatrixInvVersion = _globalMatrixVersion
			_globalMatrixInv.setToInverse(_globalMatrix)
		}
		return _globalMatrixInv
	}

	private fun invalidateMatrix() {
		validLocal = false
		invalidate()
	}

	open fun invalidate() {
		validGlobal = false
	}

	fun render(ctx: RenderContext) = render(ctx, globalMatrix)

	override fun render(ctx: RenderContext, m: Matrix2d) {
	}

	@Suppress("RemoveCurlyBracesFromTemplate")
	override fun toString(): String {
		var out = "${this::class.java.simpleName}($id)"
		if (x != 0.0 || y != 0.0) out += ":pos=($x,$y)"
		if (scaleX != 1.0 || scaleY != 1.0) out += ":scale=($scaleX,$scaleY)"
		if (skewX != 0.0 || skewY != 0.0) out += ":skew=($skewX,$skewY)"
		if (rotation != 0.0) out += ":rotation=(${rotationDegrees}º)"
		if (name != null) out += ":name=($name)"
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


	open fun hitTestInternal(x: Double, y: Double): View? = null

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
		_x = 0.0; _y = 0.0
		_scaleX = 1.0; _scaleY = 1.0
		_skewX = 0.0; _skewY = 0.0
		_rotation = 0.0
		validLocal = false
		validGlobal = false
		invalidate()
	}

	fun update(dtMs: Int) {
		updateInternal((dtMs * speed).toInt())
	}

	open protected fun updateInternal(dtMs: Int) {
		if (componentsIt != null) {
			for (c in componentsIt!!) c.update(dtMs)
		}
	}

	fun removeFromParent() {
		if (parent != null) {
			val p = parent!!
			for (i in index until p.children.size) {
				p.children[i].index--
			}
			p.children.removeAt(index)
			parent = null
			index = -1
		}
	}

	fun getConcatMatrix(target: View, out: Matrix2d = Matrix2d()): Matrix2d {
		var current: View? = this
		out.setToIdentity()
		val views = arrayListOf<View>()
		while (current != null) {
			views += current
			if (current == target) break
			current = current.parent
		}
		for (view in views.reversed()) out.premultiply(view.localMatrix)
		return out
	}

	fun getGlobalBounds(out: Rectangle = Rectangle()): Rectangle = getBounds(this.root, out)

	fun getBounds(target: View = this, out: Rectangle = Rectangle()): Rectangle {
		val concat = getConcatMatrix(target)
		val bb = BoundsBuilder()

		getLocalBounds(out)

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

	open fun getLocalBounds(out: Rectangle = Rectangle()) {
		out.setTo(0, 0, 0, 0)
	}


	interface Event
	data class StageResizedEvent(var width: Int, var height: Int) : Event {
		fun setSize(width: Int, height: Int) = this.apply {
			this.width = width
			this.height = height
		}
	}

	open fun handleEvent(e: Event) {
		if (componentsIt != null) for (c in componentsIt!!) c.handleEvent(e)
	}
}

fun View.hasAncestor(ancestor: View): Boolean {
	return if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false
}

fun View.replaceWith(view: View) {
	if (this == view) return
	if (parent == null) return
	view.parent?.children?.remove(view)
	parent!!.children[this.index] = view
	view.index = this.index
	view.parent = parent
	parent = null
	view.invalidate()
	this.index = -1
}
