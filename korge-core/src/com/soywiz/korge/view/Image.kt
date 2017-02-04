package com.soywiz.korge.view

import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture

class Image(var tex: Texture, var anchorX: Double = 0.0, var anchorY: Double = anchorX, views: Views) : View(views) {
    var smoothing = true
    override fun render(ctx: RenderContext) {
        // Precalculate points to avoid matrix multiplication per vertex on each frame
        ctx.batch.addQuad(tex, x = -(tex.width * anchorX).toFloat(), y = -(tex.height * anchorY).toFloat(), m = globalMatrix, filtering = smoothing, col1 = globalCol1)
    }

    override fun hitTest(x: Double, y: Double): View? {
        val sLeft = -tex.width * anchorX
        val sTop = -tex.height * anchorY
        val sRight = sLeft + tex.width
        val sBottom = sTop + tex.height
        return if (checkGlobalBounds(x, y, sLeft, sTop, sRight, sBottom)) this else null
    }
}
