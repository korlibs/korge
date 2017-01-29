package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.geom.Matrix2d
import com.soywiz.korim.geom.Point2d
import com.soywiz.korio.util.clamp

open class View(val views: Views) {
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

	var scale: Double; get() = (scaleX + scaleY) / 2.0; set(v) = run { scaleX = v; scaleY = v }

	private var _localMatrix = Matrix2d()
	private var _globalMatrix = Matrix2d()
	private var _globalMatrixInv = Matrix2d()

	internal var validLocal = false
	internal var validGlobal = false
	internal var validGlobalInv = false

	private var _globalAlpha: Double = 1.0
	private var _globalCol1: Int = -1

	protected val localMatrix: Matrix2d get() {
		if (!validLocal) {
			validLocal = true
			_localMatrix.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
		}
		return _localMatrix
	}

	protected val globalMatrix: Matrix2d get() {
		if (!validGlobal) {
			validGlobal = true
			if (parent != null) {
				_globalMatrix.copyFrom(parent!!.globalMatrix)
				_globalMatrix.premulitply(localMatrix)
			} else {
				_globalMatrix.copyFrom(localMatrix)
			}
			_globalAlpha = if (parent != null) parent!!.globalAlpha * alpha else alpha
			_globalCol1 = RGBA.packf(1f, 1f, 1f, _globalAlpha.toFloat())
		}
		return _globalMatrix
	}

	protected val globalAlpha: Double get() = run { globalMatrix; _globalAlpha }
	protected val globalCol1: Int get() = run { globalMatrix; _globalCol1 }

	protected val globalMatrixInv: Matrix2d get() {
		if (!validGlobalInv) {
			validGlobalInv = true
			_globalMatrixInv.setToInverse(globalMatrix)
		}
		return _globalMatrixInv
	}

	private fun invalidateMatrix() {
		validLocal = false
		validGlobal = false
		validGlobalInv = false
		invalidate()
	}

	open fun invalidate() {
		if (validGlobal && validGlobalInv) return
		validGlobal = false
		validGlobalInv = false
	}

	open fun render(ctx: RenderContext) {
	}

	override fun toString(): String = "${javaClass.simpleName}($id)"

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
}
