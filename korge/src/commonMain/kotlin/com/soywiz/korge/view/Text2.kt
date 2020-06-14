package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korma.geom.*

/*
// Example:
val font = BitmapFont(DefaultTtfFont, 64.0)

var offset = 0.degrees
addUpdater { offset += 10.degrees }
text2("Hello World!", color = Colors.RED, font = font, renderer = CreateStringTextRenderer { text, n, c, c1, g, advance ->
    transform.identity()
    val sin = sin(offset + (n * 360 / text.length).degrees)
    transform.rotate(15.degrees)
    transform.translate(0.0, sin * 16)
    transform.scale(1.0, 1.0 + sin * 0.1)
    put(c)
    advance(advance)
}).position(100, 100)
*/
@KorgeExperimental
inline fun Container.text2(
    text: String, fontSize: Double = 64.0,
    color: RGBA = Colors.WHITE, font: Font = DefaultTtfFont,
    horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT, verticalAlign: VerticalAlign = VerticalAlign.TOP,
    noinline renderer: TextRenderer<String> = DefaultStringTextRenderer,
    block: Text2.() -> Unit = {}
): Text2
    = Text2(text, fontSize, color, font, horizontalAlign, verticalAlign, renderer).addTo(this, block)

@KorgeExperimental
open class Text2(
    text: String, fontSize: Double = 64.0,
    color: RGBA = Colors.WHITE, font: Font = DefaultTtfFont,
    horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT, verticalAlign: VerticalAlign = VerticalAlign.TOP,
    renderer: TextRenderer<String> = DefaultStringTextRenderer
) : Container() {
    private var cachedVersion = -1
    private var version = 0

    var text: String = text; set(value) { if (field != value) { field = value; version++ } }
    var color: RGBA = color; set(value) { if (field != value) { field = value; version++ } }
    var font: Font = font; set(value) { if (field != value) { field = value; version++ } }
    var fontSize: Double = fontSize; set(value) { if (field != value) { field = value; version++ } }
    var horizontalAlign: HorizontalAlign = horizontalAlign; set(value) { if (field != value) { field = value; version++ } }
    var verticalAlign: VerticalAlign = verticalAlign; set(value) { if (field != value) { field = value; version++ } }
    var renderer: TextRenderer<String> = renderer; set(value) { if (field != value) { field = value; version++ } }
    private lateinit var textToBitmapResult: TextToBitmapResult
    private val container = container()
    private val bitmapFontActions = Text2TextRendererActions()

    override fun renderInternal(ctx: RenderContext) {
        container.colorMul = color
        if (font is BitmapFont) {
            bitmapFontActions.x = 0.0
            bitmapFontActions.y = 0.0

            bitmapFontActions.mreset()
            renderer(bitmapFontActions, text, fontSize, font)
            while (container.numChildren < bitmapFontActions.arrayTex.size) {
                container.image(Bitmaps.transparent)
            }
            while (container.numChildren > bitmapFontActions.arrayTex.size) {
                container[container.numChildren - 1].removeFromParent()
            }
            for (n in 0 until bitmapFontActions.arrayTex.size) {
                (container[n] as Image).also {
                    it.texture = bitmapFontActions.arrayTex[n]
                    it.x = bitmapFontActions.arrayX[n]
                    it.y = bitmapFontActions.arrayY[n]
                    it.scaleX = bitmapFontActions.arraySX[n]
                    it.scaleY = bitmapFontActions.arraySY[n]
                    it.rotationRadians = bitmapFontActions.arrayRot[n]
                }
            }
        } else {
            if (cachedVersion != version) {
                cachedVersion = version
                textToBitmapResult = font.renderTextToBitmap(fontSize, text, paint = ColorPaint(Colors.WHITE), fill = true, renderer = renderer)

                val x = textToBitmapResult.metrics.left - horizontalAlign.getOffsetX(textToBitmapResult.bmp.width.toDouble())
                val y = verticalAlign.getOffsetY(textToBitmapResult.fmetrics.lineHeight, textToBitmapResult.metrics.top.toDouble())

                container.removeChildren()
                container.image(textToBitmapResult.bmp).position(x, y)
            }
        }
        super.renderInternal(ctx)
    }
}

class Text2TextRendererActions : TextRendererActions() {
    internal val arrayTex = arrayListOf<BmpSlice>()
    internal val arrayX = doubleArrayListOf()
    internal val arrayY = doubleArrayListOf()
    internal val arraySX = doubleArrayListOf()
    internal val arraySY = doubleArrayListOf()
    internal val arrayRot = doubleArrayListOf()
    private val tr = Matrix.Transform()

    fun mreset() {
        arrayTex.clear()
        arrayX.clear()
        arrayY.clear()
        arraySX.clear()
        arraySY.clear()
        arrayRot.clear()
    }

    override fun put(codePoint: Int): GlyphMetrics {
        val bf = font as BitmapFont
        val m = getGlyphMetrics(codePoint)
        val g = bf[codePoint]
        val x = 0.0
        val y = -m.height
        tr.setMatrix(transform)
        //println("x: ${this.x}, y: ${this.y}")
        arrayTex += g.texture
        arrayX += this.x + transform.fastTransformX(x, y)
        arrayY += this.y + transform.fastTransformY(x, y)
        arraySX += tr.scaleX
        arraySY += tr.scaleY
        arrayRot += tr.rotation.radians
        return m
    }
}
