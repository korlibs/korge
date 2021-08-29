package com.soywiz.korim.font

import com.soywiz.kmem.toIntRound
import com.soywiz.korio.util.*
import com.soywiz.korma.geom.Rectangle
import kotlin.math.roundToInt

data class FontMetrics(
    /** size of the font metric */
    var size: Double = 0.0,
    /** maximum top for any character like É  */
    var top: Double = 0.0,
    /** ascent part of E */
    var ascent: Double = 0.0,
    /** base of 'E' */
    var baseline: Double = 0.0, // Should be 0.0
    /** lower part of 'j' */
    var descent: Double = 0.0,
    /** descent + linegap */
    var bottom: Double = 0.0,
    /** extra height */
    var leading: Double = 0.0,
    /** maximum number of width */
    var maxWidth: Double = 0.0
) {
    /* 'E' height */
    val emHeight get() = ascent - descent
    /* 'É' + 'j' + linegap */
    val lineHeight get() = top - bottom

    fun copyFromNewSize(other: FontMetrics, size: Double) = this.copyFromScaled(other, size / other.size)

    fun copyFromScaled(other: FontMetrics, scale: Double) = this.apply {
        this.size = other.size * scale
        this.top = other.top * scale
        this.ascent = other.ascent * scale
        this.baseline = other.baseline * scale
        this.descent = other.descent * scale
        this.bottom = other.bottom * scale
        this.leading = other.leading * scale
        this.maxWidth = other.maxWidth * scale
    }

    override fun toString(): String = buildString {
        append("FontMetrics(")
        append("size=${size.toIntRound()}, ")
        append("top=${top.toIntRound()}, ")
        append("ascent=${ascent.toIntRound()}, ")
        append("baseline=${baseline.toIntRound()}, ")
        append("descent=${descent.toIntRound()}, ")
        append("bottom=${bottom.toIntRound()}, ")
        append("leading=${leading.toIntRound()}, ")
        append("emHeight=${emHeight.toIntRound()}, ")
        append("lineHeight=${lineHeight.toIntRound()}")
        append(")")
    }

    fun clear() {
    }
}

data class GlyphMetrics(
    var size: Double = 0.0,
    var existing: Boolean = false,
    var codePoint: Int = 0,
    val bounds: Rectangle = Rectangle(),
    var xadvance: Double = 0.0
) {
    val right: Double get() = bounds.right
    val bottom: Double get() = bounds.bottom
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top
    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    fun clone() = copy(bounds = bounds.clone())

    fun copyFromNewSize(other: GlyphMetrics, size: Double, codePoint: Int = other.codePoint) = this.copyFromScaled(other, size / other.size, codePoint)

    fun copyFromScaled(other: GlyphMetrics, scale: Double, codePoint: Int = other.codePoint) = this.apply {
        this.size = other.size
        this.existing = other.existing
        this.codePoint = codePoint
        this.bounds.setTo(other.bounds.x * scale, other.bounds.y * scale, other.bounds.width * scale, other.bounds.height * scale)
        this.xadvance = other.xadvance * scale
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

data class TextMetrics(
    val bounds: Rectangle = Rectangle(),
    val firstLineBounds: Rectangle = Rectangle(),
    val fontMetrics: FontMetrics = FontMetrics(),
    var nlines: Int = 0,
) {
    val left: Double get() = bounds.left
    val top: Double get() = bounds.top

    val right: Double get() = bounds.right
    val bottom: Double get() = bounds.bottom

    val width: Double get() = bounds.width
    val height: Double get() = bounds.height

    val drawLeft get() = -left
    val drawTop get() = firstLineBounds.height + firstLineBounds.top

    val ascent get() = fontMetrics.ascent
    val descent get() = fontMetrics.descent
    val lineHeight get() = fontMetrics.lineHeight
    val allLineHeight get() = lineHeight * nlines

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
