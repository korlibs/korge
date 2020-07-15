package com.soywiz.korim.font

import com.soywiz.korim.vector.*

interface VectorFont : Font {
    fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath = GlyphPath()): GlyphPath?

    override fun renderGlyph(
        ctx: Context2d,
        size: Double,
        codePoint: Int,
        x: Double,
        y: Double,
        fill: Boolean,
        metrics: GlyphMetrics
    ) {
        getGlyphMetrics(size, codePoint, metrics)
        val g = getGlyphPath(size, codePoint)
        if (g != null) {
            ctx.keepTransform {
                ctx.translate(x, y)
                g.draw(ctx)
            }
            if (fill) ctx.fill() else ctx.stroke()
        }
    }
}
