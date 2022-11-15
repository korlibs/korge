package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import kotlin.math.*

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
                    val border = 1
                    val result = font.renderGlyphToBitmap(fontSize, codePoint, paint = Colors.WHITE, fill = true, border = border, effect = null)
                    val bmp = result.bmp.toBMP32()

                    val rbmp: Bitmap32 = when (distanceField) {
                        "msdf" -> {
                            //for (n in 0 until 10000) {
                            //    val path = result.shape!!.getPath()
                            //    val msdf = path.msdf(bmp.width, bmp.height)
                            //    msdf.scale(-1f)
                            //    msdf.clamp(-2f, +2f)
                            //    //msdf.updateComponent { component, value -> if (component == 3) 2f else value }
                            //    msdf.normalizeUniform()
                            //    msdf.toBMP32()
                            //}

                            result.shape!!.getPath().msdfBmp(bmp.width, bmp.height)
                        }
                        else -> bmp
                    }

                    val entry = atlas.add(rbmp, Unit)
                    val slice = entry.slice
                    //val slice = bmp.slice()
                    //val fm = result.fmetrics
                    val g = result.glyphs.first()
                    //val fm = it.data.fmetrics
                    val m = g.metrics
                    val xadvance = m.xadvance

                    //println("codePoint=$codePoint, char='${codePoint.toChar()}', xadvance=${xadvance}, height=${m.height}, top=${m.top}, ascent=${fm.ascent}, slice=$slice")
                    //BitmapFont.Glyph(fontSize, codePoint, entry.slice, 0, (m.height - m.top + fm.ascent).toInt(), xadvance.toInt())
                    BitmapFont.Glyph(
                        fontSize, codePoint, slice,
                        //-border,
                        //(border - m.height - m.top + fm.ascent).toIntRound(), xadvance.toIntRound()
                        -g.x.toIntRound(),
                        -g.y.toIntRound(),
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
