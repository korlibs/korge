package com.soywiz.korge.view

import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

/**
 * [RectBase] is an abstract [Container] [View] that represents something with a Rect-like shape: like a [SolidRect] or an [Image].
 * It supports anchoring [anchorX] and [anchorY] ratios [0..1] for anchoring this rectangle, and handles pre-computing of vertices for performance.
 */
// @TODO: In KorGE 2.0 this should extend [Container] but [View]
@UseExperimental(KorgeInternal::class)
open class RectBase(
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	var hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : Container(), Anchorable {
	//abstract val width: Double
	//abstract val height: Double

	protected var baseBitmap: BmpSlice = Bitmaps.white; set(v) { field = v; dirtyVertices = true }
	override var anchorX: Double = anchorX; set(v) { field = v; dirtyVertices = true }
    override var anchorY: Double = anchorY; set(v) { field = v; dirtyVertices = true }

    protected open val bwidth get() = 0.0
	protected open val bheight get() = 0.0

    protected val anchorDispX get() = (anchorX * bwidth)
    protected val anchorDispY get() = (anchorY * bheight)

    protected open val sLeft get() = -anchorDispX
	protected open val sTop get() = -anchorDispY

	val sRight get() = sLeft + bwidth
	val sBottom get() = sTop + bheight

    private val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)

	private fun computeVertexIfRequired() {
		if (!dirtyVertices) return
		dirtyVertices = false
		vertices.quad(0, sLeft, sTop, bwidth, bheight, globalMatrix, baseBitmap, renderColorMul, renderColorAdd)
	}

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
		if (baseBitmap !== Bitmaps.transparent) {
			computeVertexIfRequired()
			//println("$name: ${vertices.str(0)}, ${vertices.str(1)}, ${vertices.str(2)}, ${vertices.str(3)}")
			ctx.batch.drawVertices(vertices, ctx.getTex(baseBitmap).base, smoothing, renderBlendMode.factors)
		}
		super.renderInternal(ctx)
	}

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(sLeft, sTop, bwidth, bheight)
	}

	override fun hitTest(x: Double, y: Double): View? {
		val lres = if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
			(hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
		) this else null
		return lres ?: super.hitTestInternal(x, y)
	}

	//override fun hitTestInternal(x: Double, y: Double): View? {
	//	return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
	//		(hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
	//	) this else null
	//}

	override fun toString(): String {
		var out = super.toString()
		if (anchorX != 0.0 || anchorY != 0.0) out += ":anchor=(${anchorX.str}, ${anchorY.str})"
		return out
	}
}
