package samples

import korlibs.datastructure.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*

class MainTextBounds : Scene() {
    enum class Wrap { NO, CIRCLE }

    lateinit var playfairDisplayTTF: VectorFont
    var align = TextAlignment.BASELINE_CENTER
    //val vfont get() = DefaultTtfFont
    val vfont get() = playfairDisplayTTF
    val bmpFont by lazy { vfont.toBitmapFont(16.0) }
    var wrap = Wrap.NO

    fun Container.reload() {
        removeChildren()
        val fontSize = 128.0
        //val text = "HÉLLO\nji!"
        val text = "¡Everyone's got\na story s t st to to tell!"
        //val text = "¡¿"
        //val text = "¡"
        //val text = "st"
        //val text = "hello"
        val renderer = DefaultStringTextRenderer
            .let {
                when (wrap) {
                    Wrap.NO -> it
                    Wrap.CIRCLE -> it.aroundPath(buildVectorPath { this.circle(Point(0, 0), 256f) })
                }
            }

        //val align = TextAlignment.TOP_LEFT
        //val align = TextAlignment.BASELINE_LEFT
        container {
            xy(600, 350)
            text(
                text, fontSize, font = vfont, alignment = align, renderer = renderer,
                fill = Colors.YELLOW,
                stroke = Stroke(Colors.RED, thickness = 3.0, dash = doubleArrayListOf(50.0, 50.0))
            )//.also { it.zIndex = 1.0 }
            graphics {
                println("MainTextBounds: ----------------------")
                val stats = vfont.getTextBoundsWithGlyphs(fontSize, text, align = align, renderer = renderer)
                val metrics = stats.metrics
                //val metrics = font.getTextBounds(64.0, text, align = align)
                println("MainTextBounds: - ${metrics.bounds}")
                stroke(Colors.BLUE.withAd(0.5), lineWidth = 8.0) { rect(metrics.firstLineBounds) }
                stroke(Colors.WHITE.withAd(0.5), lineWidth = 5.0) { rect(metrics.bounds) }
                for (line in metrics.lineBounds) {
                    stroke(Colors.RED.withAd(0.5), lineWidth = 3.0) { rect(line) }
                }

                for (glyph in stats.glyphs) {
                    stroke(Colors.GREEN.withAd(0.5), lineWidth = 1.0) { path(glyph.boundsPath) }
                }
            }
            circle(16.0, Colors.PURPLE).centered
        }
    }

    override suspend fun SContainer.sceneMain() {
        playfairDisplayTTF = resourcesVfs["PlayfairDisplay-BoldItalic.ttf"].readTtfFont()

        val container = container {
            reload()
        }

        uiVerticalStack {
            for (vertical in VerticalAlign.ALL) {
                uiHorizontalStack {
                    for (horizontal in HorizontalAlign.ALL - HorizontalAlign.JUSTIFY) {
                        uiButton("$vertical-$horizontal") {
                            onClick {
                                align = TextAlignment(horizontal, vertical); container.reload()
                            }
                        }
                    }
                }
            }
            uiHorizontalStack {
                uiButton("NO-WRAP") { onClick { wrap = Wrap.NO; container.reload() } }
                uiButton("CIRCLE-WRAP") { onClick { wrap = Wrap.CIRCLE; container.reload() } }
            }
        }
    }
}