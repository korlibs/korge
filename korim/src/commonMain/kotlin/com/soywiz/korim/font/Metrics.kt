package com.soywiz.korim.font

import com.soywiz.kmem.toIntRound
import com.soywiz.korio.util.niceStr
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 *                              ... [top] (Positive)
 *
 *           ##                 --- [ascent] (Positive)
 *
 *         ######       ##
 *        ##   ##
 *       ########       ##
 *      ##     ##       ##
 *     ##      ##       ##      ---- [baseline] (Always 0)
 *                 ##  ##
 *                  ####        ---- [descent] (Negative)
 *
 *                              ---- [bottom] (Negative)
 *
 *     .......................  ---- [bottom] - [lineGap] (Negative)
 */
data class FontMetrics(
    /** size of the font metric, typically [top] - [bottom] */
    var size: Double = 0.0,
    /** maximum top for any character like É  */
    var top: Double = 0.0,
    /** ascent part of E */
    var ascent: Double = 0.0,
    /** base of 'E' */
    var baseline: Double = 0.0, // Should be 0.0
    /** lower part of 'j' */
    var descent: Double = 0.0,
    /** lowest part without the gap */
    var bottom: Double = 0.0,
    /** extra height, line-gap */
    var lineGap: Double = 0.0,
    /** maximum number of width */
    var maxWidth: Double = 0.0,
    /** units per EM (historically 'M' width) */
    var unitsPerEm: Double = 0.0,
) {
    /** Top-most part of the glyphs */
    val rtop: Double get() = max(ascent, top)
    /** Bottom-most part of the glyphs (without the [lineGap] between lines) */
    val rbottom: Double get() = min(descent, bottom)

    /** Total size of a line including from [top] to [bottom] + [lineGap] */
    val lineHeight: Double get() = rtop - rbottom + lineGap // Including gap!

    fun copyFrom(other: FontMetrics): FontMetrics = this.copyFromScaled(other, 1.0)
    fun copyFromNewSize(other: FontMetrics, size: Double): FontMetrics = this.copyFromScaled(other, size / other.size)

    fun copyFromScaled(other: FontMetrics, scale: Double): FontMetrics {
        this.size = other.size * scale
        this.top = other.top * scale
        this.ascent = other.ascent * scale
        this.baseline = other.baseline * scale
        this.descent = other.descent * scale
        this.bottom = other.bottom * scale
        this.lineGap = other.lineGap * scale
        this.unitsPerEm = other.unitsPerEm * scale
        this.maxWidth = other.maxWidth * scale
        return this
    }

    override fun toString(): String = buildString {
        append("FontMetrics(")
        append("size=${size.niceStr(1)}, ")
        append("top=${top.niceStr(1)}, ")
        append("ascent=${ascent.niceStr(1)}, ")
        append("baseline=${baseline.niceStr(1)}, ")
        append("descent=${descent.niceStr(1)}, ")
        append("bottom=${bottom.niceStr(1)}, ")
        append("lineGap=${lineGap.niceStr(1)}, ")
        append("unitsPerEm=${unitsPerEm.niceStr(1)}, ")
        append("maxWidth=${maxWidth.niceStr(1)}, ")
        append("lineHeight=${lineHeight.niceStr(1)}")
        append(")")
    }

    fun clear() {
    }

    fun clone(): FontMetrics = copy()
    fun cloneScaled(scale: Double): FontMetrics = copy().copyFromScaled(this, scale)
    fun cloneWithNewSize(size: Double): FontMetrics = copy().copyFromNewSize(this, size)
}

data class GlyphMetrics(
    var size: Double = 0.0,
    var existing: Boolean = false,
    var codePoint: Int = 0,
    val bounds: Rectangle = Rectangle(),
    var xadvance: Double = 0.0,
) {
    val right: Double get() = bounds.right
    val bottom: Double get() = bounds.bottom
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top
    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    fun clone() = copy(bounds = bounds.clone())

    fun copyFromNewSize(other: GlyphMetrics, size: Double, codePoint: Int = other.codePoint): GlyphMetrics =
        this.copyFromScaled(other, size / other.size, codePoint)

    fun copyFromScaled(other: GlyphMetrics, scale: Double, codePoint: Int = other.codePoint): GlyphMetrics {
        this.size = other.size
        this.existing = other.existing
        this.codePoint = codePoint
        this.bounds.setTo(other.bounds.x * scale, other.bounds.y * scale, other.bounds.width * scale, other.bounds.height * scale)
        this.xadvance = other.xadvance * scale
        return this
    }

    override fun toString(): String = buildString {
        append("GlyphMetrics(")
        append("codePoint=${codePoint} ('${codePoint.toChar()}'), ")
        append("existing=$existing, ")
        append("xadvance=${xadvance.roundToInt()}, ")
        append("bounds=${bounds.toInt()}")
        append(")")
    }
}

data class TextMetrics constructor(
    val bounds: Rectangle = Rectangle(),
    var lineBounds: List<Rectangle> = emptyList(),
    val fontMetrics: FontMetrics = FontMetrics(),
    var nlines: Int = 0,
) {
    val firstLineBounds: Rectangle get() = lineBounds.firstOrNull() ?: Rectangle()

    val left: Double get() = bounds.left
    val top: Double get() = bounds.top

    val right: Double get() = bounds.right
    val bottom: Double get() = bounds.bottom

    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    val drawLeft: Double get() = -left
    val drawTop: Double get() = firstLineBounds.height + firstLineBounds.top

    val ascent: Double get() = fontMetrics.ascent
    val descent: Double get() = fontMetrics.descent
    val lineHeight: Double get() = fontMetrics.lineHeight
    val allLineHeight: Double get() = lineHeight * nlines

    fun round(): TextMetrics {
        bounds.round()
        firstLineBounds.round()
        return this
    }

    fun clear() {
        bounds.setTo(0, 0, 0, 0)
        firstLineBounds.setTo(0, 0, 0, 0)
        fontMetrics.clear()
        nlines = 0
    }

    override fun toString(): String = "TextMetrics[${left.niceStr}, ${top.niceStr}, ${width.niceStr}, ${height.niceStr}][${drawLeft.niceStr}, ${drawTop.niceStr}]"
}
