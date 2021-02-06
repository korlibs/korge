package com.soywiz.korim.font

import com.soywiz.kds.*
import com.soywiz.kmem.toIntCeil
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.bitmap.effect.BitmapEffect
import com.soywiz.korim.bitmap.effect.applyEffect
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.text.*
import com.soywiz.korio.resources.*
import com.soywiz.korma.geom.*

interface Font : Resourceable<Font> {
    override fun getOrNull() = this
    override suspend fun get() = this

    val name: String

    // Metrics
    fun getFontMetrics(size: Double, metrics: FontMetrics = FontMetrics()): FontMetrics
    fun getGlyphMetrics(size: Double, codePoint: Int, metrics: GlyphMetrics = GlyphMetrics()): GlyphMetrics
    fun getKerning(size: Double, leftCodePoint: Int, rightCodePoint: Int): Double

    // Rendering
    fun renderGlyph(ctx: Context2d, size: Double, codePoint: Int, x: Double, y: Double, fill: Boolean, metrics: GlyphMetrics)
}

data class TextToBitmapResult(
    val bmp: Bitmap,
    val fmetrics: FontMetrics,
    val metrics: TextMetrics,
    val glyphs: List<PlacedGlyph>
) {
    data class PlacedGlyph(val codePoint: Int, val x: Double, val y: Double, val metrics: GlyphMetrics, val transform: Matrix)
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
        TextToBitmapResult.PlacedGlyph(codePoint, gx + border, gy + border, gmetrics, Matrix())
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
    val glyphs = arrayListOf<TextToBitmapResult.PlacedGlyph>()
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
        font.drawText(this, size, text, paint, bounds.drawLeft, bounds.ascent, fill, renderer = renderer, placed = { codePoint, x, y, size, metrics, transform ->
            if (returnGlyphs) {
                glyphs += TextToBitmapResult.PlacedGlyph(codePoint, x, y, metrics.clone(), transform.clone())
            }
        })
    }
    return TextToBitmapResult(image, bounds.fontMetrics, bounds, glyphs)
}

fun <T> Font.drawText(
    ctx: Context2d?, size: Double,
    text: T, paint: Paint,
    x: Double = 0.0, y: Double = 0.0,
    fill: Boolean = true,
    renderer: TextRenderer<T> = DefaultStringTextRenderer as TextRenderer<T>,
    placed: ((codePoint: Int, x: Double, y: Double, size: Double, metrics: GlyphMetrics, transform: Matrix) -> Unit)? = null
) {
    val actions = object : TextRendererActions() {
        override fun put(codePoint: Int): GlyphMetrics {
            if (ctx != null) {
                ctx.keepTransform {
                    val m = getGlyphMetrics(codePoint)
                    ctx.translate(this.x + x, this.y + y)
                    //ctx.translate(-m.width * transformAnchor.sx, +m.height * transformAnchor.sy)
                    ctx.transform(this.transform)
                    //ctx.translate(+m.width * transformAnchor.sx, -m.height * transformAnchor.sy)
                    ctx.fillStyle = this.paint ?: paint
                    font.renderGlyph(ctx, size, codePoint, 0.0, 0.0, true, glyphMetrics)
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
