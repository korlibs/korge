package com.soywiz.korge.view

import com.soywiz.kds.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.debug.*
import com.soywiz.korge.internal.*
import com.soywiz.korge.render.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.ktree.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.*

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
        block: @ViewDslMarker Text2.() -> Unit = {}
): Text2
    = Text2(text, fontSize, color, font, horizontalAlign, verticalAlign, renderer).addTo(this, block)

@KorgeExperimental
open class Text2(
        text: String, fontSize: Double = 64.0,
        color: RGBA = Colors.WHITE, font: Font = DefaultTtfFont,
        horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT, verticalAlign: VerticalAlign = VerticalAlign.TOP,
        renderer: TextRenderer<String> = DefaultStringTextRenderer
) : Container(), KorgeDebugNode, ViewLeaf {
    object Serializer : KTreeSerializerExt<Text2>("Text", Text2::class, { Text2("Text") }, {
        add(Text2::text, "Text")
        add(Text2::fontSource)
        add(Text2::fontSize, 10.0)
        add(Text2::verticalAlign, { VerticalAlign(it) }, { it.toString() })
        add(Text2::horizontalAlign, { HorizontalAlign(it) }, { it.toString() })
        //view.fontSource = xml.str("fontSource", "")
    }) {
        override suspend fun ktreeToViewTree(xml: Xml, currentVfs: VfsFile): Text2 {
            return super.ktreeToViewTree(xml, currentVfs).also { view ->
                if ((view.fontSource ?: "").isNotBlank()) {
                    try {
                        view.forceLoadFontSource(currentVfs, view.fontSource)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

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
    private var fontLoaded: Boolean = false
    var fontSource: String? = null
        set(value) {
            field = value
            fontLoaded = false
        }

    suspend fun forceLoadFontSource(currentVfs: VfsFile, sourceFile: String?) {
        fontSource = sourceFile
        fontLoaded = true
        if (sourceFile != null) {
            font = when {
                sourceFile.endsWith(".ttf") -> currentVfs["$sourceFile"].readTtfFont()
                else -> currentVfs["$sourceFile"].readBitmapFont()
            }
        }
    }

    override fun renderInternal(ctx: RenderContext) {
        val fontSource = fontSource
        if (!fontLoaded && fontSource != null) {
            fontLoaded = true
            launchImmediately(ctx.coroutineContext) {
                forceLoadFontSource(ctx.views!!.currentVfs, fontSource)
            }
        }
        container.colorMul = color
        val font = this.font
        if (font is BitmapFont) {
            bitmapFontActions.x = 0.0
            bitmapFontActions.y = 0.0

            bitmapFontActions.mreset()
            bitmapFontActions.verticalAlign = verticalAlign
            bitmapFontActions.horizontalAlign = horizontalAlign
            renderer(bitmapFontActions, text, fontSize, font)
            while (container.numChildren < bitmapFontActions.arrayTex.size) {
                container.image(Bitmaps.transparent)
            }
            while (container.numChildren > bitmapFontActions.arrayTex.size) {
                container[container.numChildren - 1].removeFromParent()
            }
            //println(font.glyphs['H'.toInt()])
            //println(font.glyphs['a'.toInt()])
            //println(font.glyphs['g'.toInt()])

            val textWidth = bitmapFontActions.x

            val dx = -textWidth * horizontalAlign.ratio

            for (n in 0 until bitmapFontActions.arrayTex.size) {
                (container[n] as Image).also {
                    it.texture = bitmapFontActions.arrayTex[n]
                    it.x = bitmapFontActions.arrayX[n] + dx
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


    override fun UiContainer.buildDebugComponent(views: Views) {
        uiCollapsableSection("Text") {
            uiEditableValue(::text)
            uiEditableValue(::fontSize, min= 1.0, max = 300.0)
            uiEditableValue(::verticalAlign, values = { listOf(VerticalAlign.TOP, VerticalAlign.MIDDLE, VerticalAlign.BASELINE, VerticalAlign.BOTTOM) })
            uiEditableValue(::horizontalAlign, values = { listOf(HorizontalAlign.LEFT, HorizontalAlign.CENTER, HorizontalAlign.RIGHT, HorizontalAlign.JUSTIFY) })
            uiEditableValue(::fontSource, UiTextEditableValue.Kind.FILE(views.currentVfs) {
                it.extensionLC == "ttf" || it.extensionLC == "fnt"
            })
        }
    }
}

class Text2TextRendererActions : TextRendererActions() {
    var verticalAlign: VerticalAlign = VerticalAlign.TOP
    var horizontalAlign: HorizontalAlign = HorizontalAlign.LEFT
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
        val x = -g.xoffset.toDouble()
        val y = g.yoffset.toDouble() - when (verticalAlign) {
            VerticalAlign.BASELINE -> bf.base
            else -> bf.lineHeight * verticalAlign.ratio
        }

        val fontScale = fontSize / bf.fontSize

        tr.setMatrix(transform)
        //println("x: ${this.x}, y: ${this.y}")
        arrayTex += g.texture
        arrayX += this.x + transform.fastTransformX(x, y) * fontScale
        arrayY += this.y + transform.fastTransformY(x, y) * fontScale
        arraySX += tr.scaleX * fontScale
        arraySY += tr.scaleY * fontScale
        arrayRot += tr.rotation.radians
        return m
    }

}
