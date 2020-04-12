package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.annotations.*
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
    = Text2(text, fontSize, color, font, horizontalAlign, verticalAlign, renderer).addTo(this).also(block)

@KorgeExperimental
open class Text2(
    text: String, fontSize: Double = 64.0,
    color: RGBA = Colors.WHITE, font: Font = DefaultTtfFont,
    horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT, verticalAlign: VerticalAlign = VerticalAlign.TOP,
    renderer: TextRenderer<String> = DefaultStringTextRenderer
) : Container() {
    private var cachedVersion = -1
    private var version = 0

    var text: String = text; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    var color: RGBA = color; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    var font: Font = font; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    var fontSize: Double = fontSize; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    var horizontalAlign: HorizontalAlign = horizontalAlign; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    var verticalAlign: VerticalAlign = verticalAlign; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    var renderer: TextRenderer<String> = renderer; set(value) = run { if (field != value) run { field = value }.also { version++ } }
    private lateinit var textToBitmapResult: TextToBitmapResult
    private val container = container()

    private val arrayTex = arrayListOf<BmpSlice>()
    private val arrayX = doubleArrayListOf()
    private val arrayY = doubleArrayListOf()
    private val arraySX = doubleArrayListOf()
    private val arraySY = doubleArrayListOf()
    private val arrayRot = doubleArrayListOf()

    private val bitmapFontActions = object : TextRendererActions() {
        val tr = Matrix.Transform()

        override fun put(codePoint: Int): GlyphMetrics {
            val bf = font as BitmapFont
            val m = getGlyphMetrics(codePoint)
            val g = bf[codePoint]
            val x = 0.0
            val y = -m.height
            tr.setMatrix(transform)
            //println("x: ${this.x}, y: ${this.y}")
            arrayTex += g.texture
            arrayX += this.x + transform.transformX(x, y)
            arrayY += this.y + transform.transformY(x, y)
            arraySX += tr.scaleX
            arraySY += tr.scaleY
            arrayRot += tr.rotation.radians
            return m
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        container.colorMul = color
        if (font is BitmapFont) {
            bitmapFontActions.x = 0.0
            bitmapFontActions.y = 0.0
            arrayTex.clear()
            arrayX.clear()
            arrayY.clear()
            arraySX.clear()
            arraySY.clear()
            arrayRot.clear()
            renderer(bitmapFontActions, text, fontSize, font)
            while (container.numChildren < arrayTex.size) {
                container.image(Bitmaps.transparent)
            }
            while (container.numChildren > arrayTex.size) {
                container[container.numChildren - 1].removeFromParent()
            }
            for (n in 0 until arrayTex.size) {
                (container[n] as Image).also {
                    it.texture = arrayTex[n]
                    it.x = arrayX[n]
                    it.y = arrayY[n]
                    it.scaleX = arraySX[n]
                    it.scaleY = arraySY[n]
                    it.rotationRadians = arrayRot[n]
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
