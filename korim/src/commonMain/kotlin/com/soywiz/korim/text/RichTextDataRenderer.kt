package com.soywiz.korim.text

import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*

fun Context2d.drawRichText(
    text: RichTextData,
    bounds: IRectangle = Rectangle(0, 0, width, height),
    wordWrap: Boolean = true,
    includePartialLines: Boolean = false,
    ellipsis: String? = null,
    fill: Paint? = null,
    stroke: Stroke? = null,
    align: TextAlignment = TextAlignment.TOP_LEFT,
) {
    if (text.lines.isEmpty()) return

    val rtext = text.limit(
        if (wordWrap) bounds.width else Double.POSITIVE_INFINITY,
        includePartialLines = includePartialLines,
        maxHeight = bounds.height,
        ellipsis = ellipsis,
        trimSpaces = true
    )
    var y = bounds.y + rtext.lines.first().maxHeight
    for (line in rtext.lines) {
        if (line.nodes.isEmpty()) continue
        var x = bounds.x - align.horizontal.getOffsetX(line.width) + align.horizontal.getOffsetX(bounds.width)
        val wordSpacing = if (align.horizontal == HorizontalAlign.JUSTIFY) (bounds.width - line.width).toDouble() / (line.nodes.size.toDouble() - 1) else 0.0
        for (node in line.nodes) {
            when (node) {
                is RichTextData.TextNode -> {
                    drawText(node.text, x, y, size = node.textSize, font = node.font, outMetrics = TextMetricsResult(), fillStyle = fill, stroke = stroke)
                    x += node.width
                    x += wordSpacing
                }
            }
        }
        y += line.maxLineHeight
    }
}
