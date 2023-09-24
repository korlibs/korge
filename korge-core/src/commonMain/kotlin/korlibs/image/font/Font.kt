package korlibs.image.font

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.bitmap.effect.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.image.vector.*
import korlibs.io.lang.*
import korlibs.io.resources.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*

interface Font : Resourceable<Font>, Extra {
    override fun getOrNull() = this
    override suspend fun get() = this

    val name: String

    // Metrics
    fun getFontMetrics(size: Double, metrics: FontMetrics = FontMetrics()): FontMetrics
    fun getGlyphMetrics(
        size: Double,
        codePoint: Int,
        metrics: GlyphMetrics = GlyphMetrics(),
        reader: WStringReader? = null
    ): GlyphMetrics
    fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double

    // Rendering
    // Returns true if it painted something
    fun renderGlyph(
        ctx: Context2d, size: Double, codePoint: Int, pos: Point, fill: Boolean?,
        metrics: GlyphMetrics, reader: WStringReader? = null,
        beforeDraw: (() -> Unit)? = null
    ): Boolean
}

data class TextToBitmapResult(
    val bmp: Bitmap,
    override val fmetrics: FontMetrics,
    override val metrics: TextMetrics,
    override val glyphs: List<PlacedGlyphMetrics>,
    override val glyphsPerLine: List<List<PlacedGlyphMetrics>>,
    val shape: Shape? = null
) : BaseTextMetricsResult

data class TextMetricsResult(
    override var fmetrics: FontMetrics = FontMetrics(),
    override var metrics: TextMetrics = TextMetrics(),
    override var glyphs: List<PlacedGlyphMetrics> = emptyList(),
    override var glyphsPerLine: List<List<PlacedGlyphMetrics>> = emptyList(),
) : BaseTextMetricsResult

class MultiplePlacedGlyphMetrics {
    val glyphs: FastArrayList<PlacedGlyphMetrics> = FastArrayList()
    val glyphsPerLine: FastArrayList<FastArrayList<PlacedGlyphMetrics>> = FastArrayList()

    val size: Int get() = glyphs.size

    fun add(glyph: PlacedGlyphMetrics) {
        glyphs += glyph
        while (glyphsPerLine.size <= glyph.nline) glyphsPerLine.add(FastArrayList())
        glyphsPerLine[glyph.nline].add(glyph)
    }

    operator fun plusAssign(glyph: PlacedGlyphMetrics) = add(glyph)
}

data class PlacedGlyphMetrics constructor(
    val codePoint: Int,
    val pos: Point,
    val metrics: GlyphMetrics,
    val fontMetrics: FontMetrics,
    val transform: Matrix,
    val index: Int,
    val nline: Int,
) {
    val boundsPath: VectorPath by lazy {
        buildVectorPath {
            this.optimize = false
            rect(
                metrics.left,
                -fontMetrics.ascent,
                metrics.xadvance,
                fontMetrics.lineHeight
            )
        }.applyTransform(Matrix().translated(pos).premultiplied(transform))
    }
    val boundsPathCurves: Curves by lazy {
        val curves = boundsPath.getCurves()
        if (curves.beziers.size != 4) {
            println("curves=${curves.beziers.size}")
        }
        curves
    }
    val caretStart: Bezier by lazy { boundsPathCurves.beziers[3].toLine().flipped().toBezier() }
    val caretEnd: Bezier by lazy { boundsPathCurves.beziers[1] }

    fun distToPath(x: Double, y: Double, startEnd: Boolean? = null): Double {
        if (boundsPath.containsPoint(x, y)) return 0.0

        val middle = when (startEnd) {
            true -> caretStart[Ratio.HALF]
            false -> caretEnd[Ratio.HALF]
            null -> Point.middle(caretStart[Ratio.HALF], caretEnd[Ratio.HALF])
        }
        return Point.distance(middle.x, middle.y, x, y)
    }

    fun distToPath(p: Point, startEnd: Boolean? = null): Double = distToPath(p.x, p.y, startEnd)
}

interface BaseTextMetricsResult {
    val fmetrics: FontMetrics
    val metrics: TextMetrics
    val glyphs: List<PlacedGlyphMetrics>
    val glyphsPerLine: List<List<PlacedGlyphMetrics>>
}

fun Font.renderGlyphToBitmap(
    size: Double, codePoint: Int, paint: Paint = DefaultPaint, fill: Boolean = true,
    effect: BitmapEffect? = null,
    border: Int = 1,
    nativeRendering: Boolean = true,
    reader: WStringReader? = null,
): TextToBitmapResult {
    val font = this
    val fmetrics = getFontMetrics(size)
    val gmetrics = getGlyphMetrics(size, codePoint, reader = reader)
    val gx = -gmetrics.left
    val gy = gmetrics.height + gmetrics.top
    val border2 = border * 2
    val iwidth = gmetrics.width.toIntCeil() + border2
    val iheight = gmetrics.height.toIntCeil() + border2
    val image = if (nativeRendering) NativeImage(iwidth, iheight) else Bitmap32(iwidth, iheight, premultiplied = true)

    fun Context2d.renderGlyph() {
        fillStyle = paint
        font.renderGlyph(this, size, codePoint, Point(gx + border, gy + border), fill = true, metrics = gmetrics)
        if (fill) fill() else stroke()
    }

    image.context2d {
        renderGlyph()
    }
    val imageOut = image.toBMP32IfRequired().applyEffect(effect)
    val glyph = PlacedGlyphMetrics(codePoint, Point(gx + border, gy + border), gmetrics, fmetrics, Matrix.IDENTITY, 0, 0)
    return TextToBitmapResult(imageOut, fmetrics, TextMetrics(), listOf(glyph), listOf(listOf(glyph)), buildShape(iwidth, iheight) {
        renderGlyph()
    })
}

// @TODO: Fix metrics
fun <T> Font.renderTextToBitmap(
    size: Double,
    text: T,
    paint: Paint = DefaultPaint,
    background: Paint = NonePaint,
    fill: Boolean = true,
    border: Int = 0,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    returnGlyphs: Boolean = true,
    nativeRendering: Boolean = true,
    drawBorder: Boolean = false
): TextToBitmapResult {
    val font = this
    val bounds = getTextBounds(size, text, renderer = renderer)
    //println("BOUNDS: $bounds")
    val glyphs = MultiplePlacedGlyphMetrics()
    val iwidth = bounds.width.toIntCeil() + border * 2 + 1
    //println("bounds.nlines=${bounds.nlines}, bounds.allLineHeight=${bounds.allLineHeight}")
    val iheight = (if (drawBorder) bounds.allLineHeight else bounds.height).toIntCeil() + border * 2 + 1
    val image = if (nativeRendering) NativeImage(iwidth, iheight) else Bitmap32(iwidth, iheight, premultiplied = true)
    //println("bounds.firstLineBounds: ${bounds.firstLineBounds}")
    //println("bounds.bounds: ${bounds.bounds}")
    image.context2d {
        if (background != NonePaint) {
            this.fillStyle(background) {
                fillRect(0, 0, iwidth, iheight)
            }
        }
        //font.drawText(this, size, text, paint, bounds.drawLeft, bounds.drawTop, fill, renderer = renderer, placed = { codePoint, x, y, size, metrics, transform ->
        var index = 0
        font.drawText(this, size, text, paint, Point(bounds.drawLeft, bounds.ascent), fill, renderer = renderer, placed = { codePoint, pos, size, metrics, fmetrics, transform ->
            if (returnGlyphs) {
                glyphs += PlacedGlyphMetrics(codePoint, pos, metrics.clone(), fmetrics, transform.clone(), index++, currentLineNum)
            }
        })
    }
    return TextToBitmapResult(image, bounds.fontMetrics, bounds, glyphs.glyphs, glyphs.glyphsPerLine)
}

fun <T> Font.drawText(
    ctx: Context2d?,
    size: Double,
    text: T,
    paint: Paint?, // Deprecated parameter
    pos: Point = Point.ZERO,
    fill: Boolean = true, // Deprecated parameter
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    align: TextAlignment = TextAlignment.BASELINE_LEFT,
    outMetrics: TextMetricsResult? = null,

    fillStyle: Paint? = null,
    stroke: Stroke? = null,

    textRangeStart: Int = 0,
    textRangeEnd: Int = Int.MAX_VALUE,

    placed: (TextRendererActions.(codePoint: Int, pos: Point, size: Double, metrics: GlyphMetrics, fmetrics: FontMetrics, transform: Matrix) -> Unit)? = null
): TextMetricsResult? {
    //println("drawText!!: text=$text, align=$align")
    val glyphs = if (outMetrics != null) MultiplePlacedGlyphMetrics() else null
    val fnt = this
    //println("Font.drawText:")
    val doRender: () -> Unit = {
        //println("doRender=")
        if (fillStyle != null || stroke != null) {
            ctx?.fillStroke(fillStyle, stroke)
        } else {
            if (fill) ctx?.fill() else ctx?.stroke()
        }
        ctx?.beginPath()
    }
    val actions = object : TextRendererActions() {
        val metrics = renderer.measure(text, size, fnt)
        var n = 0
        override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
            val nline = currentLineNum
            val dx = metrics.getAlignX(align.horizontal, nline)
            val dy = metrics.getAlignY(align.vertical, fontMetrics)
            val px = this.pos.x + pos.x - dx
            val py = this.pos.y + pos.y + dy
            ctx?.keepTransform {
                //val m = getGlyphMetrics(codePoint)
                //println("valign=$valign, glyphMetrics.height=${glyphMetrics.height}")
                //println(glyphMetrics)
                //println(fontMetrics)
                //println(dy)
                ctx.translate(px, py)
                //ctx.translate(-m.width * transformAnchor.sx, +m.height * transformAnchor.sy)
                ctx.transform(this.transform.immutable)
                //ctx.translate(+m.width * transformAnchor.sx, -m.height * transformAnchor.sy)
                ctx.fillStyle = this.paint ?: paint ?: NonePaint
                //println("n=$n, textRangeStart=$textRangeStart, textRangeEnd=$textRangeEnd, doDraw=$doDraw")
                if (n in textRangeStart until textRangeEnd) {
                    this.font.renderGlyph(ctx, size, codePoint, Point.ZERO, null, glyphMetrics, reader, beforeDraw = doRender)
                }
                n++
            }
            if (glyphs != null) {
                val glyph = PlacedGlyphMetrics(
                    codePoint, Point(px, py), glyphMetrics.clone(), fontMetrics.clone(), transform,
                    glyphs.size, nline
                )
                glyphs.add(glyph)
            }
            placed?.invoke(this, codePoint, Point(px, py), size, glyphMetrics, fontMetrics, this.transform)
            return glyphMetrics
        }
    }
    ctx?.beginPath()
    renderer.invoke(actions, text, size, this)
    doRender()
    if (outMetrics != null) {
        glyphs!!
        outMetrics.glyphs = glyphs.glyphs
        outMetrics.glyphsPerLine = glyphs.glyphsPerLine

        if (true) {
            val fmetrics = this.getFontMetrics(size)
            outMetrics.fmetrics = fmetrics
            val metrics = outMetrics.metrics
            metrics.fontMetrics.copyFrom(fmetrics)
            metrics.nlines = glyphs.glyphsPerLine.size
            metrics.lineBounds = glyphs.glyphsPerLine.map { glyphs ->
                var bb = BoundsBuilder()
                for (g in glyphs) bb += g.boundsPath
                bb.bounds
            }
            metrics.bounds = (BoundsBuilder() + metrics.lineBounds).bounds
        } else {
            val metrics = getTextBounds(size, text, renderer = renderer, align = align)
            outMetrics.fmetrics = metrics.fontMetrics
            outMetrics.metrics = metrics
        }
    }
    return outMetrics
}

fun <T> Font.getTextBoundsWithGlyphs(
    size: Double,
    text: T,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    align: TextAlignment = TextAlignment.BASELINE_LEFT
): TextMetricsResult {
    val font = this
    //val bounds = getTextBounds(size, text, renderer = renderer)
    //println("BOUNDS: $bounds")
    return TextMetricsResult().also { out ->
        //font.drawText(null, size, text, null, bounds.drawLeft, bounds.ascent, false, renderer = renderer, outMetrics = out, align = align)
        font.drawText(null, size, text, null, Point.ZERO, false, renderer = renderer, outMetrics = out, align = align)
    }
}

fun <T> Font.getTextBounds(
    size: Double,
    text: T,
    out: TextMetrics = TextMetrics(),
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    align: TextAlignment = TextAlignment.TOP_LEFT
): TextMetrics {
    val actions = BoundBuilderTextRendererActions()
    renderer.invoke(actions, text, size, this)
    this.getFontMetrics(size, out.fontMetrics)
    out.nlines = actions.nlines

    // Compute
    var bb = BoundsBuilder()
    var dy = 0.0
    val lineBounds = FastArrayList<Rectangle>()
    val offsetY = actions.getAlignY(align.vertical, out.fontMetrics)
    //println("--")
    //println("offsetY=$offsetY, totalMaxLineHeight=${actions.totalMaxLineHeight}, align=$align")
    //printStackTrace()
    for (line in actions.lines) {
        val offsetX = line.getAlignX(align.horizontal)
        val rect = Rectangle(
            -offsetX,
            +offsetY + dy - out.ascent,
            line.maxX,
            line.maxLineHeight
        )
        //println("rect=$rect, offsetX=$offsetX, drawLeft=${out.drawLeft}")
        bb += rect
        lineBounds.add(rect)
        dy += line.maxLineHeight
    }
    out.lineBounds = lineBounds
    out.bounds = bb.bounds

    return out
}
