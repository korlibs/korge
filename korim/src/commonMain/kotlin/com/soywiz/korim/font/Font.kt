package com.soywiz.korim.font

import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.bitmap.context2d
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.bitmap.effect.applyEffect
import com.soywiz.korim.paint.DefaultPaint
import com.soywiz.korim.paint.NonePaint
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.text.BoundBuilderTextRendererActions
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.TextRenderer
import com.soywiz.korim.text.TextRendererActions
import com.soywiz.korim.text.invoke
import com.soywiz.korim.text.measure
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.WStringReader
import com.soywiz.korio.resources.Resourceable
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.bezier.Curves
import com.soywiz.korma.geom.bezier.toBezier
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.applyTransform
import com.soywiz.korma.geom.vector.getCurves
import com.soywiz.korma.geom.vector.rect

interface Font : Resourceable<Font> {
    override fun getOrNull() = this
    override suspend fun get() = this

    val name: String

    // Metrics
    fun getFontMetrics(size: Double, metrics: FontMetrics = FontMetrics()): FontMetrics
    fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics = GlyphMetrics()): GlyphMetrics
    fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double

    // Rendering
    // Returns true if it painted something
    fun renderGlyph(
        ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean?,
        metrics: GlyphMetrics, reader: WStringReader? = null,
        beforeDraw: (() -> Unit)? = null
    ): Boolean
}

data class TextToBitmapResult(
    val bmp: Bitmap,
    override val fmetrics: FontMetrics,
    override val metrics: TextMetrics,
    override val glyphs: List<PlacedGlyphMetrics>
) : BaseTextMetricsResult

data class TextMetricsResult(
    override var fmetrics: FontMetrics = FontMetrics(),
    override var metrics: TextMetrics = TextMetrics(),
    override var glyphs: List<PlacedGlyphMetrics> = emptyList()
) : BaseTextMetricsResult

data class PlacedGlyphMetrics(val codePoint: Int, val x: Double, val y: Double, val metrics: GlyphMetrics, val fontMetrics: FontMetrics, val transform: Matrix, val index: Int) {
    val boundsPath: VectorPath by lazy {
        buildVectorPath {
            this.optimize = false
            val rect = Rectangle().copyFrom(metrics.bounds)
            rect.y = -fontMetrics.ascent
            rect.height = fontMetrics.ascent - fontMetrics.descent

            //println("rect=$rect, ascent=${fontMetrics.ascent}, descent=${fontMetrics.descent}")

            //rect.y = -rect.y
            //rect.height = -rect.height
            //rect.y = -rect.y
            //rect.height = -rect.height
            rect(rect)
        }.applyTransform(Matrix().translate(x, y).premultiply(transform))
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
            true -> caretStart.get(0.5)
            false -> caretEnd.get(0.5)
            null -> Point.middle(caretStart.get(0.5), caretEnd.get(0.5))
        }
        return Point.distance(middle.x, middle.y, x, y)
    }

    fun distToPath(p: IPoint, startEnd: Boolean? = null): Double = distToPath(p.x, p.y, startEnd)
}

interface BaseTextMetricsResult {
    val fmetrics: FontMetrics
    val metrics: TextMetrics
    val glyphs: List<PlacedGlyphMetrics>
}

fun Font.renderGlyphToBitmap(
        size: Double, codePoint: Int, paint: Paint = DefaultPaint, fill: Boolean = true,
        effect: BitmapEffect? = null,
        border: Int = 1, nativeRendering: Boolean = true
): TextToBitmapResult {
    val font = this
    val fmetrics = getFontMetrics(size)
    val gmetrics = getGlyphMetrics(size, codePoint)
    val gx = -gmetrics.left
    val gy = gmetrics.height + gmetrics.top
    val border2 = border * 2
    val iwidth = gmetrics.width.toIntCeil() + border2
    val iheight = gmetrics.height.toIntCeil() + border2
    val image = if (nativeRendering) NativeImage(iwidth, iheight) else Bitmap32(iwidth, iheight, premultiplied = true)
    image.context2d {
        fillStyle = paint
        font.renderGlyph(this, size, codePoint, gx + border, gy + border, fill = true, metrics = gmetrics)
        if (fill) fill() else stroke()
    }
    val imageOut = image.toBMP32IfRequired().applyEffect(effect)
    return TextToBitmapResult(imageOut, fmetrics, TextMetrics(), listOf(
        PlacedGlyphMetrics(codePoint, gx + border, gy + border, gmetrics, fmetrics, Matrix(), 0)
    ))
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
    val glyphs = arrayListOf<PlacedGlyphMetrics>()
    val iwidth = bounds.width.toIntCeil() + border * 2 + 1
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
        font.drawText(this, size, text, paint, bounds.drawLeft, bounds.ascent, fill, renderer = renderer, placed = { codePoint, x, y, size, metrics, fmetrics, transform ->
            if (returnGlyphs) {
                glyphs += PlacedGlyphMetrics(codePoint, x, y, metrics.clone(), fmetrics, transform.clone(), index++)
            }
        })
    }
    return TextToBitmapResult(image, bounds.fontMetrics, bounds, glyphs)
}

fun <T> Font.measureTextGlyphs(
    size: Double,
    text: T,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
): TextMetricsResult {
    val font = this
    val bounds = getTextBounds(size, text, renderer = renderer)
    //println("BOUNDS: $bounds")
    return TextMetricsResult().also { out ->
        font.drawText(null, size, text, null, bounds.drawLeft, bounds.ascent, false, renderer = renderer, outMetrics = out)
    }
}

fun <T> Font.drawText(
    ctx: Context2d?,
    size: Double,
    text: T, paint: Paint?,
    x: Double = 0.0, y: Double = 0.0,
    fill: Boolean = true,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    align: TextAlignment = TextAlignment.BASELINE_LEFT,
    outMetrics: TextMetricsResult? = null,
    placed: ((codePoint: Int, x: Double, y: Double, size: Double, metrics: GlyphMetrics, fmetrics: FontMetrics, transform: Matrix) -> Unit)? = null
): TextMetricsResult? {
    //println("drawText!!: text=$text, align=$align")
    val glyphs = if (outMetrics != null) arrayListOf<PlacedGlyphMetrics>() else null
    val fnt = this
    //println("Font.drawText:")
    val doRender: () -> Unit = {
        //println("doRender=")
        if (fill) ctx?.fill() else ctx?.stroke()
        ctx?.beginPath()
    }
    val actions = object : TextRendererActions() {
        val metrics = renderer.measure(text, size, fnt)
        override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
            val dx = metrics.getAlignX(align.horizontal, currentLineNum, fontMetrics)
            val dy = metrics.getAlignY(align.vertical, currentLineNum, fontMetrics)
            val px = this.x + x - dx
            val py = this.y + y + dy
            ctx?.keepTransform {
                //val m = getGlyphMetrics(codePoint)
                //println("valign=$valign, glyphMetrics.height=${glyphMetrics.height}")
                //println(glyphMetrics)
                //println(fontMetrics)
                //println(dy)
                ctx.translate(px, py)
                //ctx.translate(-m.width * transformAnchor.sx, +m.height * transformAnchor.sy)
                ctx.transform(this.transform)
                //ctx.translate(+m.width * transformAnchor.sx, -m.height * transformAnchor.sy)
                ctx.fillStyle = this.paint ?: paint ?: NonePaint
                this.font.renderGlyph(ctx, size, codePoint, 0.0, 0.0, null, glyphMetrics, reader, beforeDraw = doRender)
            }
            if (glyphs != null) {
                glyphs += PlacedGlyphMetrics(codePoint, px, py, glyphMetrics.clone(), fontMetrics.clone(), transform.clone(), glyphs.size)
            }
            placed?.invoke(codePoint, px, py, size, glyphMetrics, fontMetrics, this.transform)
            return glyphMetrics
        }
    }
    ctx?.beginPath()
    renderer.invoke(actions, text, size, this)
    doRender()
    if (outMetrics != null) {
        val metrics = getTextBounds(size, text, renderer = renderer)
        outMetrics.fmetrics = metrics.fontMetrics
        outMetrics.metrics = metrics
        outMetrics.glyphs = glyphs ?: emptyList()
    }
    return outMetrics
}
fun <T> Font.getTextBounds(size: Double, text: T, out: TextMetrics = TextMetrics(), renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>): TextMetrics {
    val actions = BoundBuilderTextRendererActions()
    renderer.invoke(actions, text, size, this)
    actions.bb.getBounds(out.bounds)
    actions.flbb.getBounds(out.firstLineBounds)
    out.nlines = actions.nlines
    this.getFontMetrics(size, out.fontMetrics)
    return out
}
