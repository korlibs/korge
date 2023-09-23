package korlibs.image.font

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.atlas.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.math.*
import korlibs.math.geom.*

class LazyBitmapFont(
    val font: VectorFont,
    override val fontSize: Double,
    override val distanceField: String? = null,
) : BitmapFont, Extra by Extra.Mixin() {
    val atlas = MutableAtlasUnit(border = 2, width = 1024, height = 1024).also {
        if (distanceField != null) it.bitmap else it.bitmap.mipmaps()
    }

    private val vfontMetrics = font.getFontMetrics(fontSize)

    override val lineHeight: Double = vfontMetrics.lineHeight
    override val base: Double = vfontMetrics.baseline
    override val glyphs: IntMap<BitmapFont.Glyph> = IntMap()
    override val kernings: IntMap<BitmapFont.Kerning> = IntMap()
    override val anyGlyph: BitmapFont.Glyph by lazy {
        //ensureCodePoints(SPACE)
        glyphs[glyphs.keys.iterator().next()] ?: invalidGlyph
    }
    override val invalidGlyph: BitmapFont.Glyph by lazy { BitmapFont.Glyph(fontSize, -1, Bitmaps.transparent, 0, 0, 0) }
    override val name: String get() = font.name

    override val naturalFontMetrics: FontMetrics by lazy {
        val descent = (vfontMetrics.descent - vfontMetrics.lineGap)
        FontMetrics(
            size = vfontMetrics.size,
            top = vfontMetrics.top,
            ascent = vfontMetrics.ascent,
            baseline = vfontMetrics.baseline,
            descent = descent,
            bottom = descent,
            lineGap = 0.0,
            unitsPerEm = 0.0,
            maxWidth = vfontMetrics.maxWidth,
        )
    }
    override val naturalNonExistantGlyphMetrics: GlyphMetrics = GlyphMetrics(fontSize, false, 0, Rectangle(), 0.0)

    override fun getKerning(first: Int, second: Int): BitmapFont.Kerning? {
        val kerning = font.getKerning(fontSize, first, second)
        return if (kerning == 0.0) null else kernings.getOrPut(BitmapFont.Kerning.buildKey(first, second)) {
            BitmapFont.Kerning(first, second, kerning.toInt())
        }
    }

    override fun getOrNull(codePoint: Int): BitmapFont.Glyph? {
        //if (font.getGlyphPath(fontSize, codePoint) == null) return null
        if (glyphs[codePoint] == null) {
            ensureRange(codePoint, 'a'.code..'z'.code)
            ensureRange(codePoint, 'A'.code..'Z'.code)
            ensureRange(codePoint, '0'.code..'9'.code)
            ensureCodePoints(intArrayOf(codePoint))
        }
        return glyphs[codePoint]
    }

    fun ensureRange(codePoint: Int, range: IntRange) {
        if (codePoint in range) ensureCodePoints(range.toIntList().toIntArray())
    }
    fun ensureChars(chars: String) = ensureCodePoints(chars.map { it.code }.toIntArray())
    fun ensureCharacterSet(characterSet: CharacterSet) = ensureCodePoints(characterSet.codePoints)

    fun ensureCodePoints(codePoints: IntArray) {
        atlas.bitmap.lock {
            // @TODO: precompute all glyph images to try to add first the bigger ones to better optimize space
            codePoints.fastForEach { codePoint ->
                glyphs.getOrPut(codePoint) { codePoint ->
                    // @TODO: This border is affecting text bounds. We should check.
                    val border = if (distanceField != null) kotlin.math.max(4, (fontSize / 8.0).toIntCeil()) else 1
                    val result = font.renderGlyphToBitmap(fontSize, codePoint, paint = Colors.WHITE, fill = true, border = border, effect = null)
                    val bmp = result.bmp.toBMP32()

                    val rbmp: Bitmap32 = when (distanceField) {
                        "msdf" -> result.shape!!.getPath().msdfBmp(bmp.width, bmp.height)
                        "sdf" -> result.shape!!.getPath().sdfBmp(bmp.width, bmp.height)
                        else -> bmp
                    }

                    val entry = atlas.add(rbmp, Unit)
                    val slice = entry.slice.sliceWithBounds(border, border, entry.slice.width - border, entry.slice.height - border)

                    //val slice = bmp.slice()
                    //val fm = result.fmetrics
                    val g = result.glyphs.first()
                    //val fm = it.data.fmetrics
                    val m = g.metrics
                    val xadvance = m.xadvance

                    //println("codePoint='$codePoint': $slice, gx=${g.x}, gy=${g.y}")

                    //println("codePoint=$codePoint, char='${codePoint.toChar()}', xadvance=${xadvance}, height=${m.height}, top=${m.top}, ascent=${fm.ascent}, slice=$slice")
                    //BitmapFont.Glyph(fontSize, codePoint, entry.slice, 0, (m.height - m.top + fm.ascent).toInt(), xadvance.toInt())
                    BitmapFont.Glyph(
                        fontSize, codePoint, slice,
                        //-border,
                        //(border - m.height - m.top + fm.ascent).toIntRound(), xadvance.toIntRound()
                        -g.pos.x.toIntRound(),
                        -g.pos.y.toIntRound(),
                        xadvance.toIntRound(),
                    )
                }
            }
        }
    }

    companion object {
        private val SPACE = intArrayOf(' '.code)
    }
}

fun VectorFont.toLazyBitmapFont(fontSize: Double, distanceField: String? = null): LazyBitmapFont =
    LazyBitmapFont(this, fontSize, distanceField)

/**
 * Returns, creates and caches a [LazyBitmapFont] instance from the [VectorFont] that will
 * generate and cache glyphs as required.
 */
val VectorFont.lazyBitmap: LazyBitmapFont by Extra.PropertyThis {
    this.toLazyBitmapFont(32.0, distanceField = null)
}

val VectorFont.lazyBitmapSDF: LazyBitmapFont by Extra.PropertyThis {
    this.toLazyBitmapFont(32.0, distanceField = "sdf")
}

/**
 * Gets a [BitmapFont] from the font, that is going to be computed lazily as glyphs are required.
 *
 * If the font is already a BitmapFont the instance is returned, if a [VectorFont] is used,
 * it returns a [LazyBitmapFont] that will compute glyphs as required.
 */
val Font.lazyBitmap: BitmapFont get() = when (this) {
    is VectorFont -> this.lazyBitmap
    is BitmapFont -> this
    else -> TODO()
}
