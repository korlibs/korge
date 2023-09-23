package korlibs.image.font

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.vector.*
import korlibs.io.lang.*
import korlibs.math.geom.*

interface VectorFont : Font {
    fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath = GlyphPath(), reader: WStringReader? = null): GlyphPath?

    override fun renderGlyph(
        ctx: Context2d,
        size: Double,
        codePoint: Int,
        pos: Point,
        fill: Boolean?,
        metrics: GlyphMetrics,
        reader: WStringReader?,
        beforeDraw: (() -> Unit)?
    ): Boolean {
        reader.keep {
            getGlyphMetrics(size, codePoint, metrics, reader)
        }
        val g = getGlyphPath(size, codePoint, reader = reader)
        if (g != null) {
            if (fill != null) {
                ctx.beginPath()
            }
            if (!g.isOnlyPath) {
                beforeDraw?.invoke()
            }
            ctx.keepTransform {
                ctx.translate(pos)
                g.draw(ctx)
            }
            when (fill) {
                true -> ctx.fill()
                false -> ctx.stroke()
                null -> Unit
            }
            return !g.isOnlyPath
        }
        return false
    }
}

/** When getting glyphs, it first tries to resolve with [this], then in order [other] */
fun VectorFont.withFallback(vararg other: VectorFont?): VectorFontList = when (this) {
    is VectorFontList -> VectorFontList((list + other).filterNotNull())
    else -> VectorFontList(listOfNotNull(this, *other))
}

/** When getting glyphs, it first tries to resolve with [first], then this [this] */
fun VectorFont.asFallbackOf(first: VectorFont): VectorFontList = first.withFallback(this)

data class VectorFontList(val list: List<VectorFont>) : VectorFont, Extra by Extra.Mixin() {
    constructor(vararg fonts: VectorFont?) : this(fonts.filterNotNull())

    override val name: String = list.joinToString(", ") { it.name }

    private val temp = GlyphPath()

    override fun getGlyphPath(size: Double, codePoint: Int, path: GlyphPath, reader: WStringReader?): GlyphPath? =
        list.firstNotNullOfOrNull { it.getGlyphPath(size, codePoint, path, reader) }

    override fun getFontMetrics(size: Double, metrics: FontMetrics): FontMetrics =
        list.first().getFontMetrics(size, metrics)

    override fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics, reader: WStringReader?): GlyphMetrics {
        list.fastForEach { font ->
            if (font.getGlyphPath(size, codePoint, temp, reader) != null) {
                return font.getGlyphMetrics(size, codePoint, metrics, reader)
            }
        }
        return list.first().getGlyphMetrics(size, codePoint, metrics, reader)
    }

    override fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double =
        list.first().getKerning(size, leftCodePoint, rightCodePoint)

}
