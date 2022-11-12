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
    //var y = bounds.y + rtext.lines.first().maxHeight
    var y = bounds.y
    for (line in rtext.lines) {
        if (line.nodes.isEmpty()) continue
        var x = bounds.x - align.horizontal.getOffsetX(line.width) + align.horizontal.getOffsetX(bounds.width)
        val wordSpacing = if (align.horizontal == HorizontalAlign.JUSTIFY) (bounds.width - line.width).toDouble() / (line.nodes.size.toDouble() - 1) else 0.0
        y += line.maxHeight
        for (node in line.nodes) {
            when (node) {
                is RichTextData.TextNode -> {
                    fun render(dx: Double, dy: Double) {
                        drawText(node.text, x + dx, y + dy, size = node.style.textSize, font = node.style.font, outMetrics = TextMetricsResult(), fillStyle = fill, stroke = stroke)
                    }
                    keepTransform {
                        if (node.style.italic) {
                            skew(12.degrees)
                            translate(8.0, 0.0)
                        }
                        if (node.style.bold) {
                            render(1.0, 0.0)
                        }
                        render(0.0, 0.0)
                    }

                    x += node.width
                    x += wordSpacing
                }
            }
        }
        y += line.maxLineHeight - line.maxHeight
    }
}
