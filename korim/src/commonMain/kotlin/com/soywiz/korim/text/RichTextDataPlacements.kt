package com.soywiz.korim.text

import com.soywiz.kds.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*

private var RichTextData.cachedTextMetricsResult: TextMetricsResult? by extraProperty { null }

fun RichTextData.getGlyphMetrics(bounds: IRectangle): TextMetricsResult {
    if (cachedTextMetricsResult == null) {
        cachedTextMetricsResult = place(bounds).toTextMetricsResult()
    }
    return cachedTextMetricsResult!!
}

fun RichTextDataPlacements.toTextMetricsResult(out: TextMetricsResult = TextMetricsResult()): TextMetricsResult {
    val mgm = MultiplePlacedGlyphMetrics()
    var oldY = Double.NaN
    var line = -1
    var n = 0
    placements.fastForEach { placement ->
        var x = placement.x
        val y = placement.y
        if (oldY != y) {
            oldY = y
            line++
        }
        val fmetrics = placement.font.getFontMetrics(placement.size)
        placement.text.forEachCodePoint { index, codePoint, error ->
            val metrics = placement.font.getGlyphMetrics(placement.size, codePoint)
            mgm.add(PlacedGlyphMetrics(codePoint, x, y, metrics, fmetrics, MMatrix(), n, line))
            x += metrics.xadvance
            n++
        }
    }
    out.glyphs = mgm.glyphs.toList()
    out.glyphsPerLine = mgm.glyphsPerLine.toList()
    return out
}
