package samples

import com.soywiz.kds.doubleArrayListOf
import com.soywiz.korge.input.onClick
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.ui.uiButton
import com.soywiz.korge.ui.uiHorizontalStack
import com.soywiz.korge.ui.uiVerticalStack
import com.soywiz.korge.view.Container
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.centered
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.container
import com.soywiz.korge.view.graphics
import com.soywiz.korge.view.text
import com.soywiz.korge.view.xy
import com.soywiz.korim.color.Colors
import com.soywiz.korim.font.DefaultTtfFont
import com.soywiz.korim.font.VectorFont
import com.soywiz.korim.font.getTextBoundsWithGlyphs
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.font.toBitmapFont
import com.soywiz.korim.paint.Stroke
import com.soywiz.korim.text.DefaultStringTextRenderer
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.text.aroundPath
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.vector.circle
import com.soywiz.korma.geom.vector.rect

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
                    Wrap.CIRCLE -> it.aroundPath(buildVectorPath { this.circle(0.0, 0.0, 256.0) })
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
