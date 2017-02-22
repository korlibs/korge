package com.soywiz.korge.view

import com.soywiz.korge.component.Component
import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.geom.Point2d
import com.soywiz.korio.util.Extra
import com.soywiz.korio.util.clamp

open class View(val views: Views) : Renderable, Extra by Extra.Mixin() {
    var parent: Container? = null
    val id = views.lastId++
    var alpha: Double = 1.0; set(v) = run {
        if (field != v) {
            val vv = v.clamp(0.0, 1.0)
            if (field != vv) {
                field = vv; invalidateMatrix()
            }
        }
    }
    var x: Double = 0.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var y: Double = 0.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var scaleX: Double = 1.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var scaleY: Double = 1.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var skewX: Double = 0.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var skewY: Double = 0.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var rotation: Double = 0.0; set(v) = run { if (field != v) run { field = v; invalidateMatrix() } }
    var rotationDegrees: Double; set(v) = run { rotation = Math.toRadians(v) }; get() = Math.toDegrees(rotation)

    var scale: Double; get() = (scaleX + scaleY) / 2.0; set(v) = run { scaleX = v; scaleY = v }

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

    override fun toString(): String = "${this::class.java.simpleName}($id)"

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

    open fun update(dtMs: Int) {
        if (componentsByClass != null) for (c in componentsByClass!!.values.flatMap { it }) {
            c.update(dtMs)
        }
    }
}

fun View.hasAncestor(ancestor: View): Boolean {
    return if (this == ancestor) true else this.parent?.hasAncestor(ancestor) ?: false
}