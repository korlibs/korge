package com.soywiz.korim.font

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.WStringReader

interface VectorFont : Font {
    fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath = GlyphPath(), reader: WStringReader? = null): GlyphPath?

    override fun renderGlyph(
        ctx: Context2d,
        size: Double,
        codePoint: Int,
        x: Double,
        y: Double,
        fill: Boolean,
        metrics: GlyphMetrics,
        reader: WStringReader?,
    ) {
        getGlyphMetrics(size, codePoint, metrics)
        val g = getGlyphPath(size, codePoint, reader = reader)
        if (g != null) {
            ctx.keepTransform {
                ctx.translate(x, y)
                g.draw(ctx)
            }
            if (fill) ctx.fill() else ctx.stroke()
        }
    }
}

fun VectorFont.withFallback(vararg other: VectorFont?): VectorFontList = when (this) {
    is VectorFontList -> VectorFontList((list + other).filterNotNull())
    else -> VectorFontList(listOfNotNull(this, *other))
}

data class VectorFontList(val list: List<VectorFont>) : VectorFont {
    constructor(vararg fonts: VectorFont?) : this(fonts.filterNotNull())

    override val name: String = list.joinToString(", ") { it.name }

    private val temp = GlyphPath()

    override fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath, reader: WStringReader?): GlyphPath? =
        list.firstNotNullOfOrNull { it.getGlyphPath(size, codePoint, path, reader) }

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        list.first().getFontMetrics(size, metrics)

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics): GlyphMetrics {
        list.fastForEach { font ->
            if (font.getGlyphPath(size, codePoint, temp) != null) {
                return font.getGlyphMetrics(size, codePoint, metrics)
            }
        }
        return list.first().getGlyphMetrics(size, codePoint, metrics)
    }

    override fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double =
        list.first().getKerning(size, leftCodePoint, rightCodePoint)

}
