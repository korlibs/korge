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
import com.soywiz.korim.text.TextRenderer
import com.soywiz.korim.text.TextRendererActions
import com.soywiz.korim.text.invoke
import com.soywiz.korim.vector.Context2d
import com.soywiz.korio.lang.WStringReader
import com.soywiz.korio.resources.Resourceable
import com.soywiz.korma.geom.Matrix

interface Font : Resourceable<Font> {
    override fun getOrNull() = this
    override suspend fun get() = this

    val name: String

    // Metrics
    fun getFontMetrics(size: Double, metrics: FontMetrics = FontMetrics()): FontMetrics
    fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics = GlyphMetrics()): GlyphMetrics
    fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double

    // Rendering
    fun renderGlyph(ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean, metrics: GlyphMetrics, reader: WStringReader? = null)
}

data class TextToBitmapResult(
    val bmp: Bitmap,
    override val fmetrics: FontMetrics,
    override val metrics: TextMetrics,
    override val glyphs: List<PlacedGlyphMetrics>
) : BaseTextMetricsResult

data class TextMetricsResult(
    override val fmetrics: FontMetrics,
    override val metrics: TextMetrics,
    override val glyphs: List<PlacedGlyphMetrics>
) : BaseTextMetricsResult

data class PlacedGlyphMetrics(val codePoint: Int, val x: Double, val y: Double, val metrics: GlyphMetrics, val transform: Matrix, val index: Int)

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
        PlacedGlyphMetrics(codePoint, gx + border, gy + border, gmetrics, Matrix(), 0)
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
        font.drawText(this, size, text, paint, bounds.drawLeft, bounds.ascent, fill, renderer = renderer, placed = { codePoint, x, y, size, metrics, transform ->
            if (returnGlyphs) {
                glyphs += PlacedGlyphMetrics(codePoint, x, y, metrics.clone(), transform.clone(), index++)
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
    val glyphs = arrayListOf<PlacedGlyphMetrics>()
    var index = 0
    font.drawText(null, size, text, null, bounds.drawLeft, bounds.ascent, false, renderer = renderer, placed = { codePoint, x, y, size, metrics, transform ->
        glyphs += PlacedGlyphMetrics(codePoint, x, y, metrics.clone(), transform.clone(), index++)
    })
    return TextMetricsResult(bounds.fontMetrics, bounds, glyphs)
}

fun <T> Font.drawText(
    ctx: Context2d?, size: Double,
    text: T, paint: Paint?,
    x: Double = 0.0, y: Double = 0.0,
    fill: Boolean = true,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    placed: ((codePoint: Int, x: Double, y: Double, size: Double, metrics: GlyphMetrics, transform: Matrix) -> Unit)? = null
) {
    //println("drawText!!: text=$text")
    val actions = object : TextRendererActions() {
        override fun put(reader: WStringReader, codePoint: Int): GlyphMetrics {
            if (ctx != null) {
                ctx.keepTransform {
                    //val m = getGlyphMetrics(codePoint)
                    ctx.translate(this.x + x, this.y + y)
                    //ctx.translate(-m.width * transformAnchor.sx, +m.height * transformAnchor.sy)
                    ctx.transform(this.transform)
                    //ctx.translate(+m.width * transformAnchor.sx, -m.height * transformAnchor.sy)
                    ctx.fillStyle = this.paint ?: paint ?: NonePaint
                    font.renderGlyph(ctx, size, codePoint, 0.0, 0.0, true, glyphMetrics, reader)
                    placed?.invoke(codePoint, this.x + x, this.y + y, size, glyphMetrics, this.transform)
                    if (fill) ctx.fill() else ctx.stroke()
                }
            } else {
                placed?.invoke(codePoint, this.x + x, this.y + y, size, glyphMetrics, this.transform)
            }
            return glyphMetrics
        }
    }
    renderer.invoke(actions, text, size, this)
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
