package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.VectorPath

class Image(var tex: Texture, var anchorX: Double = 0.0, var anchorY: Double = anchorX, views: Views) : View(views) {
	var hitShape: VectorPath? = null
    var smoothing = true

	private val sLeft get() = -tex.width * anchorX
	private val sTop get() = -tex.height * anchorY

	override fun render(ctx: RenderContext) {
        // Precalculate points to avoid matrix multiplication per vertex on each frame
        ctx.batch.addQuad(tex, x = -(tex.width * anchorX).toFloat(), y = -(tex.height * anchorY).toFloat(), m = globalMatrix, filtering = smoothing, col1 = globalCol1)
    }

	override fun getLocalBounds(out: Rectangle) {
		out.setTo(sLeft, sTop,  tex.width, tex.height)
	}

    override fun hitTest(x: Double, y: Double): View? {
        val sRight = sLeft + tex.width
        val sBottom = sTop + tex.height
		return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom) && (hitShape?.containsPoint(globalToLocalX(x, y), globalToLocalY(x, y)) ?: true)) this else null
    }
}

fun Views.image(tex: Texture, anchorX: Double = 0.0, anchorY: Double = anchorX) = Image(tex, anchorX, anchorY, this)

fun Container.image(texture: Texture, anchorX: Double = 0.0, anchorY: Double = 0.0): Image = image(texture, anchorX, anchorY) { }

inline fun Container.image(texture: Texture, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: Image.() -> Unit): Image {
    val child = views.image(texture, anchorX, anchorY)
    this += child
    callback(child)
    return child
}

