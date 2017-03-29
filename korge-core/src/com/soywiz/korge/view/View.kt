package com.soywiz.korge.view

import com.soywiz.korge.component.Component
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.RGBA
import com.soywiz.korio.util.Cancellable
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.clamp
import com.soywiz.korma.geom.Point2d
import com.soywiz.korma.math.Matrix2d

open class View(val views: Views) : Renderable, Extra by Extra.Mixin() {
	var index: Int = 0
	var speed: Double = 1.0
	var parent: Container? = null
	var name: String? = null
	val id = views.lastId++
	var alpha: Double = 1.0; set(v) = run {
		if (field != v) {
			val vv = v.clamp(0.0, 1.0)
			if (field != vv) {
				field = vv; invalidateMatrix()
			}
		}
	}
	var _x: Double = 0.0
	var _y: Double = 0.0
	var _scaleX: Double = 1.0
	var _scaleY: Double = 1.0
	var _skewX: Double = 0.0
	var _skewY: Double = 0.0
	var _rotation: Double = 0.0

	var x: Double; set(v) = run { if (_x != v) run { _x = v; invalidateMatrix() } }; get() = _x
	var y: Double; set(v) = run { if (_y != v) run { _y = v; invalidateMatrix() } }; get() = _y
	var scaleX: Double; set(v) = run { if (_scaleX != v) run { _scaleX = v; invalidateMatrix() } }; get() = _scaleX
	var scaleY: Double; set(v) = run { if (_scaleY != v) run { _scaleY = v; invalidateMatrix() } }; get() = _scaleY
	var skewX: Double; set(v) = run { if (_skewX != v) run { _skewX = v; invalidateMatrix() } }; get() = _skewX
	var skewY: Double; set(v) = run { if (_skewY != v) run { _skewY = v; invalidateMatrix() } }; get() = _skewY
	var rotation: Double; set(v) = run { if (_rotation != v) run { _rotation = v; invalidateMatrix() } }; get() = _rotation
	var rotationDegrees: Double; set(v) = run { rotation = Math.toRadians(v) }; get() = Math.toDegrees(rotation)

	var scale: Double; get() = (scaleX + scaleY) / 2.0; set(v) = run { scaleX = v; scaleY = v }

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
	private var _globalMatrix = Matrix2d()
	private var _globalMatrixVersion = 0
	private var _globalMatrixInvVersion = 0
	private var _globalMatrixInv = Matrix2d()

	internal var validLocal = false
	internal var validGlobal = false

	private var _globalAlpha: Double = 1.0
	private var _globalCol1: Int = -1

	private var componentsByClass: HashMap<Class<out Component>, ArrayList<Component>>? = null

	inline fun <reified T : Component> getOrCreateComponent(noinline gen: (View) -> T): T = getOrCreateComponent(T::class.java, gen)

	fun removeComponent(c: Component) {
		val cc = componentsByClass?.get(c::class.java)
		cc?.remove(c)
	}

	fun addComponent(c: Component) {
		if (componentsByClass == null) componentsByClass = hashMapOf()
		val array = componentsByClass!!.getOrPut(c::class.java) { arrayListOf() }
		array += c
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
		if (componentsByClass == null) componentsByClass = hashMapOf()
		val array = componentsByClass!!.getOrPut(clazz) { arrayListOf() }
		if (array.isEmpty()) array += gen(this)
		return componentsByClass!![clazz]!!.first() as T
	}

	protected val localMatrix: Matrix2d get() {
		if (!validLocal) {
			validLocal = true
			_localMatrix.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
		}
		return _localMatrix
	}

	private fun _ensureGlobal() = this.apply {
		if (validGlobal) return@apply
		validGlobal = true
		if (parent != null) {
			_globalMatrix.copyFrom(parent!!.globalMatrix)
			_globalMatrix.premulitply(localMatrix)
			_globalMatrixVersion++
		} else {
			_globalMatrix.copyFrom(localMatrix)
			_globalMatrixVersion++
		}
		_globalAlpha = if (parent != null) parent!!.globalAlpha * alpha else alpha
		_globalCol1 = RGBA.packf(1f, 1f, 1f, _globalAlpha.toFloat())
	}

	protected val globalMatrix: Matrix2d get() = _ensureGlobal()._globalMatrix

	protected val globalAlpha: Double get() = run { globalMatrix; _globalAlpha }
	protected val globalCol1: Int get() = run { globalMatrix; _globalCol1 }

	protected val globalMatrixInv: Matrix2d get() {
		_ensureGlobal()
		if (_globalMatrixVersion != _globalMatrixInvVersion) {
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

	override fun render(ctx: RenderContext) {
	}

	@Suppress("RemoveCurlyBracesFromTemplate")
	override fun toString(): String {
		var out = "${this::class.java.simpleName}($id)"
		if (x != 0.0 || y != 0.0) out += ":pos=($x,$y)"
		if (scaleX != 1.0 || scaleY != 1.0) out += ":scale=($scaleX,$scaleY)"
		if (skewX != 0.0 || skewY != 0.0) out += ":skew=($skewX,$skewY)"
		if (rotation != 0.0) out += ":rotation=(${rotationDegrees}º)"
		return out
	}

	fun globalToLocalX(x: Double, y: Double): Double = globalMatrixInv.run { transformX(x, y) }
	fun globalToLocalY(x: Double, y: Double): Double = globalMatrixInv.run { transformY(x, y) }

	fun localToGlobalX(x: Double, y: Double): Double = globalMatrix.run { transformX(x, y) }
	fun localToGlobalY(x: Double, y: Double): Double = globalMatrix.run { transformY(x, y) }

	fun globalToLocal(p: Point2d, out: Point2d = Point2d()): Point2d = globalMatrixInv.run { transform(p.x, p.y, out) }
	fun localToGlobal(p: Point2d, out: Point2d = Point2d()): Point2d = globalMatrix.run { transform(p.x, p.y, out) }
	fun hitTest(pos: Point2d): View? = hitTest(pos.x, pos.y)
	open fun hitTest(x: Double, y: Double): View? = null

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
		validLocal = true
		invalidate()
	}

	fun update(dtMs: Int) {
		updateInternal((dtMs * speed).toInt())
	}

	open protected fun updateInternal(dtMs: Int) {
		if (componentsByClass != null) for (c in componentsByClass!!.values.flatMap { it }) {
			c.update(dtMs)
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
	this.index = -1
}
