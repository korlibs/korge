package com.soywiz.korim.text

import com.soywiz.kds.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*

class RichTextDataPlacements(
    val placements: FastArrayList<Placement> = fastArrayListOf()
) : List<RichTextDataPlacements.Placement> by placements {
    data class Placement(
        val text: String,
        val x: Double, val y: Double,
        val size: Double,
        val font: Font,
        val fillStyle: Paint?,
        val stroke: Stroke?,
        //val align: TextAlignment
    )
}

fun RichTextData.place(
    bounds: IRectangle,
    wordWrap: Boolean = true,
    includePartialLines: Boolean = false,
    ellipsis: String? = null,
    fill: Paint? = null,
    stroke: Stroke? = null,
    align: TextAlignment = TextAlignment.TOP_LEFT,
    out : RichTextDataPlacements = RichTextDataPlacements(),
): RichTextDataPlacements {
    val text = this
    out.placements.clear()
    if (text.lines.isEmpty()) return out

    val rtext = text.limit(
        if (wordWrap) bounds.width else Double.POSITIVE_INFINITY,
        includePartialLines = includePartialLines,
        maxHeight = bounds.height,
        ellipsis = ellipsis,
        trimSpaces = true
    )
    //var y = bounds.y + rtext.lines.first().maxHeight
    val totalHeight = rtext.lines.dropLast(1).sumOf { it.maxLineHeight } + rtext.lines.last().maxHeight

    var y = bounds.y + ((bounds.height - totalHeight) * align.vertical.ratioFake0)

    for (line in rtext.lines) {
        var x = bounds.x - align.horizontal.getOffsetX(line.width) + align.horizontal.getOffsetX(bounds.width)
        val wordSpacing = if (align.horizontal == HorizontalAlign.JUSTIFY) (bounds.width - line.width).toDouble() / (line.nodes.size.toDouble() - 1) else 0.0
        y += line.maxHeight
        for (node in line.nodes) {
            when (node) {
                is RichTextData.TextNode -> {
                    fun render(dx: Double, dy: Double) {
                        out.placements.add(RichTextDataPlacements.Placement(
                            node.text,
                            x + dx, y + dy,
                            size = node.style.textSize,
                            font = node.style.font,
                            fillStyle = node.style.color ?: fill,
                            stroke = stroke,
                        ))
                    }
                    if (node.style.bold) {
                        render(1.0, 0.0)
                    }
                    render(0.0, 0.0)

                    x += node.width
                    x += wordSpacing
                }
            }
        }
        y += line.maxLineHeight - line.maxHeight
    }
    return out
}

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
    val result = text.place(bounds, wordWrap, includePartialLines, ellipsis, fill, stroke, align)
    for (place in result.placements) {
        drawText(
            place.text,
            place.x,place.y,
            size = place.size,
            font = place.font,
            fillStyle = place.fillStyle,
            stroke = place.stroke,
            align = TextAlignment.BASELINE_LEFT,
        )
    }
}
