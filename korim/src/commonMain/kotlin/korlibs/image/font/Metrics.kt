package korlibs.image.font

import korlibs.io.util.*
import korlibs.math.geom.*
import kotlin.math.*

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
    var size: Float = 0f,
    /** maximum top for any character like É  */
    var top: Float = 0f,
    /** ascent part of E */
    var ascent: Float = 0f,
    /** base of 'E' */
    var baseline: Float = 0f, // Should be 0.0
    /** lower part of 'j' */
    var descent: Float = 0f,
    /** lowest part without the gap */
    var bottom: Float = 0f,
    /** extra height, line-gap */
    var lineGap: Float = 0f,
    /** maximum number of width */
    var maxWidth: Float = 0f,
    /** units per EM (historically 'M' width) */
    var unitsPerEm: Float = 0f,
) {
    /** Top-most part of the glyphs */
    val rtop: Float get() = max(ascent, top)
    /** Bottom-most part of the glyphs (without the [lineGap] between lines) */
    val rbottom: Float get() = min(descent, bottom)

    val lineHeightWithoutGap: Float get() = rtop - rbottom

    /** Total size of a line including from [top] to [bottom] + [lineGap] */
    val lineHeight: Float get() = lineHeightWithoutGap + lineGap // Including gap!

    fun copyFrom(other: FontMetrics): FontMetrics = this.copyFromScaled(other, 1f)
    fun copyFromNewSize(other: FontMetrics, size: Float): FontMetrics = this.copyFromScaled(other, size / other.size)

    fun copyFromScaled(other: FontMetrics, scale: Float): FontMetrics {
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
    fun cloneScaled(scale: Float): FontMetrics = copy().copyFromScaled(this, scale)
    fun cloneWithNewSize(size: Float): FontMetrics = copy().copyFromNewSize(this, size)
}

data class GlyphMetrics(
    var size: Float = 0f,
    var existing: Boolean = false,
    var codePoint: Int = 0,
    var bounds: Rectangle = Rectangle(),
    var xadvance: Float = 0f,
) {
    val right: Float get() = bounds.right
    val bottom: Float get() = bounds.bottom
    val left: Float get() = bounds.left
    val top: Float get() = bounds.top
    val width: Float get() = bounds.width
    val height: Float get() = bounds.height
    //val size: Size get() = bounds.size

    fun clone() = copy(bounds = bounds.clone())

    fun copyFromNewSize(other: GlyphMetrics, size: Float, codePoint: Int = other.codePoint): GlyphMetrics =
        this.copyFromScaled(other, size / other.size, codePoint)

    fun copyFromScaled(other: GlyphMetrics, scale: Float, codePoint: Int = other.codePoint): GlyphMetrics {
        this.size = other.size
        this.existing = other.existing
        this.codePoint = codePoint
        this.bounds = other.bounds * scale
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
    var bounds: Rectangle = Rectangle(),
    var lineBounds: List<Rectangle> = emptyList(),
    val fontMetrics: FontMetrics = FontMetrics(),
    var nlines: Int = 0,
) {
    val firstLineBounds: Rectangle get() = lineBounds.firstOrNull() ?: Rectangle.ZERO

    val left: Float get() = bounds.left
    val top: Float get() = bounds.top
    val right: Float get() = bounds.right
    val bottom: Float get() = bounds.bottom
    val width: Float get() = bounds.width
    val height: Float get() = bounds.height

    val drawLeft: Float get() = -left
    val drawTop: Float get() = (firstLineBounds.height + firstLineBounds.top)

    val ascent: Float get() = fontMetrics.ascent
    val descent: Float get() = fontMetrics.descent
    val lineHeight: Float get() = fontMetrics.lineHeight
    val allLineHeight: Float get() = lineHeight * nlines

    fun round(): TextMetrics {
        bounds = bounds.rounded()
        lineBounds = lineBounds.map { it.rounded() }
        return this
    }

    fun clear() {
        bounds = Rectangle(0, 0, 0, 0)
        fontMetrics.clear()
        nlines = 0
    }

    override fun toString(): String = "TextMetrics[${left.niceStr}, ${top.niceStr}, ${width.niceStr}, ${height.niceStr}][${drawLeft.niceStr}, ${drawTop.niceStr}]"
}
