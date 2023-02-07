package com.soywiz.korge.view

import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*

/**
 * [RectBase] is an abstract [Container] [View] that represents something with a Rect-like shape: like a [SolidRect] or an [Image].
 * It supports anchoring [anchorX] and [anchorY] ratios [0..1] for anchoring this rectangle, and handles pre-computing of vertices for performance.
 */
@OptIn(KorgeInternal::class)
open class RectBase(
	anchorX: Double = 0.0,
	anchorY: Double = anchorX,
	hitShape: VectorPath? = null,
	var smoothing: Boolean = true
) : View(), Anchorable {
    init {
        this.hitShape = hitShape
    }

	//abstract val width: Double
	//abstract val height: Double

    protected var baseBitmap: BitmapCoords = Bitmaps.white
        set(v) {
            if (field !== v) {
                field = v
                dirtyVertices = true
                invalidateRender()
            }
        }

	override var anchorX: Double = anchorX; set(v) { if (field != v) { field = v; dirtyVertices = true; invalidateRender() } }
    override var anchorY: Double = anchorY; set(v) { if (field != v) { field = v; dirtyVertices = true; invalidateRender() } }

    protected open val bwidth get() = 0.0
	protected open val bheight get() = 0.0

    override val anchorDispX get() = (anchorX * bwidth)
    override val anchorDispY get() = (anchorY * bheight)

    protected open val sLeft get() = -anchorDispX
	protected open val sTop get() = -anchorDispY

	val sRight get() = sLeft + bwidth
	val sBottom get() = sTop + bheight

    protected val vertices = TexturedVertexArray(4, TexturedVertexArray.QUAD_INDICES)

	override fun renderInternal(ctx: RenderContext) {
		if (!visible) return
        if (baseBitmap === Bitmaps.transparent) return
        if (dirtyVertices) {
            dirtyVertices = false
            computeVertices()
        }
        //println("$name: ${vertices.str(0)}, ${vertices.str(1)}, ${vertices.str(2)}, ${vertices.str(3)}")
        drawVertices(ctx)
        //super.renderInternal(ctx)
	}

    var wrapTexture: Boolean = false
    var program: Program? = null
    private var _programUniforms: AGUniformValues? = null
    var programUniforms: AGUniformValues
        set(value) { _programUniforms = value }
        get()  {
            if (_programUniforms == null) _programUniforms = AGUniformValues()
            return _programUniforms!!
        }

    protected open fun drawVertices(ctx: RenderContext) {
        ctx.useBatcher { batch ->
            //batch.texture1212
            batch.setTemporalUniforms(_programUniforms) {
                batch.drawVertices(
                    vertices, ctx.getTex(baseBitmap).base, smoothing, renderBlendMode,
                    program = program,
                    premultiplied = baseBitmap.base.premultiplied, wrap = wrapTexture
                )
            }
        }
    }

    protected open fun computeVertices() {
        vertices.quad(0, sLeft, sTop, bwidth, bheight, globalMatrix, baseBitmap, renderColorMul, renderColorAdd)
    }

	override fun getLocalBoundsInternal(out: Rectangle) {
        out.setTo(sLeft, sTop, bwidth, bheight)
	}

    /*
	override fun hitTest(x: Double, y: Double): View? {
		val lres = if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) &&
			(hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) != false)
		) this else null
		return lres ?: super.hitTestInternal(x, y)
	}
    */

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
