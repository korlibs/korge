package korlibs.image.text

import korlibs.datastructure.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.vector.*
import korlibs.math.geom.*

data class RichTextDataPlacements(
    val placements: FastArrayList<Placement> = fastArrayListOf()
) : List<RichTextDataPlacements.Placement> by placements {
    data class Placement(
        val text: String,
        val pos: Point,
        val size: Double,
        val font: Font,
        val fillStyle: Paint?,
        val stroke: Stroke?,
        //val align: TextAlignment
    )
}

fun RichTextData.place(
    bounds: Rectangle,
    wordWrap: Boolean = true,
    includePartialLines: Boolean = false,
    ellipsis: String? = null,
    fill: Paint? = null,
    stroke: Stroke? = null,
    align: TextAlignment = TextAlignment.TOP_LEFT,
    includeFirstLineAlways: Boolean = true,
    out : RichTextDataPlacements = RichTextDataPlacements(),
): RichTextDataPlacements {
    val text = this
    out.placements.clear()
    if (text.lines.isEmpty()) return out

    val rtext = text.limit(
        (if (wordWrap) bounds.width else Double.POSITIVE_INFINITY).toDouble(),
        includePartialLines = includePartialLines,
        maxHeight = bounds.height,
        ellipsis = ellipsis,
        trimSpaces = true,
        includeFirstLineAlways = includeFirstLineAlways
    )
    //var y = bounds.y + rtext.lines.first().maxHeight
    val totalHeight = if (rtext.lines.isNotEmpty()) rtext.lines.dropLast(1).sumOf { it.maxLineHeight } + rtext.lines.last().maxHeight else 0.0

    var y = bounds.y + ((bounds.height - totalHeight) * align.vertical.ratioFake0)

    for (line in rtext.lines) {
        var x = bounds.x - align.horizontal.getOffsetX(line.width) + align.horizontal.getOffsetX(bounds.width)
        val wordSpacing: Float = if (align.horizontal == HorizontalAlign.JUSTIFY) ((bounds.width - line.width) / (line.nodes.size.toDouble() - 1)).toFloat() else 0f
        y += line.maxHeight
        for (node in line.nodes) {
            when (node) {
                is RichTextData.TextNode -> {
                    fun render(dx: Double, dy: Double) {
                        out.placements.add(RichTextDataPlacements.Placement(
                            node.text,
                            Point(x + dx, y + dy),
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
    bounds: Rectangle = Rectangle(0, 0, width, height),
    wordWrap: Boolean = true,
    includePartialLines: Boolean = false,
    ellipsis: String? = null,
    fill: Paint? = null,
    stroke: Stroke? = null,
    align: TextAlignment = TextAlignment.TOP_LEFT,
    includeFirstLineAlways: Boolean = true,
    textRangeStart: Int = 0,
    textRangeEnd: Int = Int.MAX_VALUE,
): Int {
    val result = text.place(bounds, wordWrap, includePartialLines, ellipsis, fill, stroke, align, includeFirstLineAlways = includeFirstLineAlways)
    var n = 0
    val metrics = TextMetricsResult()
    for (place in result.placements) {
        drawText(
            place.text,
            place.pos,
            size = place.size,
            font = place.font,
            fillStyle = place.fillStyle,
            stroke = place.stroke,
            align = TextAlignment.BASELINE_LEFT,
            textRangeStart = textRangeStart - n,
            textRangeEnd = textRangeEnd - n,
            outMetrics = metrics,
            renderer = DefaultStringTextRenderer
        )
        //n += place.text.length
        n += metrics.glyphs.size
    }
    return n
}

fun RichTextData.bounds(maxHeight: Int = Int.MAX_VALUE): Rectangle {
    return place(Rectangle(0, 0, Int.MAX_VALUE, maxHeight)).bounds()
}

fun RichTextDataPlacements.bounds(): Rectangle {
    var bb = BoundsBuilder()
    placements.forEach {
        bb += it.font.getTextBounds(it.size, it.text).bounds
    }
    return bb.bounds
}
