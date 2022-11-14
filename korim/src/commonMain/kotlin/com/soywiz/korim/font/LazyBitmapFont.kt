package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korim.atlas.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.vector.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

class LazyBitmapFont(
    val font: VectorFont,
    override val fontSize: Double,
    override val distanceField: String? = null,
) : BitmapFont, Extra by Extra.Mixin() {
    private val vectorFontMetrics = font.getFontMetrics(fontSize)
    private val atlas = MutableAtlasUnit()

    override val lineHeight: Double = vectorFontMetrics.lineHeight
    override val base: Double = vectorFontMetrics.baseline
    override val glyphs: IntMap<BitmapFont.Glyph> = IntMap<BitmapFont.Glyph>()
    override val kernings: IntMap<BitmapFont.Kerning> = IntMap<BitmapFont.Kerning>()
    override val anyGlyph: BitmapFont.Glyph by lazy { glyphs[glyphs.keys.iterator().next()] ?: invalidGlyph }
    override val invalidGlyph: BitmapFont.Glyph by lazy { BitmapFont.Glyph(fontSize, -1, Bitmaps.transparent, 0, 0, 0) }
    override val name: String get() = font.name
    val naturalDescent = lineHeight - base

    override val naturalFontMetrics: FontMetrics by lazy {
        ensureCodePoints(intArrayOf(32))
        val ascent = base
        val baseline = 0.0
        FontMetrics(
            fontSize, ascent, ascent, baseline, -naturalDescent, -naturalDescent, 0.0,
            maxWidth = run {
                var width = 0.0
                for (glyph in glyphs.values) if (glyph != null) width = max(width, glyph.texture.width.toDouble())
                width
            }
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
                    val result = font.renderGlyphToBitmap(fontSize, codePoint, paint = Colors.WHITE, fill = true, border = 1, effect = null)
                    val entry = atlas.add(result.bmp.toBMP32(), Unit)
                    val fm = result.fmetrics
                    val m = result.glyphs.first().metrics
                    val xadvance = m.xadvance

                    //println("codePoint=$codePoint, xadvance=${xadvance}, height=${m.height}, top=${m.top}, ascent=${fm.ascent}")
                    //BitmapFont.Glyph(fontSize, codePoint, entry.slice, 0, (m.height - m.top + fm.ascent).toInt(), xadvance.toInt())
                    BitmapFont.Glyph(fontSize, codePoint, entry.slice, 0, (-m.bottom + fm.ascent).toIntRound(), xadvance.toInt())
                }
            }
        }
    }
}

fun VectorFont.toLazyBitmapFont(fontSize: Double, distanceField: String? = null): LazyBitmapFont =
    LazyBitmapFont(this, fontSize, distanceField)
