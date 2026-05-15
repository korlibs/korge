package korlibs.korge.test

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.io.file.std.*
import korlibs.korge.testing.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.math.geom.*
import org.junit.*

class TtfFontTest {
    @Test
    fun disableLigaturesWorks(): Unit = korgeScreenshotTest {
        val ttfFontWithLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont()
        val ttfFontWithoutLigatures =
            resourcesVfs["font_atkinson/AtkinsonHyperlegible-Bold.ttf"].readTtfFont(enableLigatures = false)

        val c = container {
            val t1 = text("41/41", font = ttfFontWithLigatures, textSize = 40f) {
                this.graphicsRenderer = GraphicsRenderer.CPU
            }
            text("41/41", font = ttfFontWithoutLigatures, textSize = 40f) {
                this.graphicsRenderer = GraphicsRenderer.CPU
                alignTopToBottomOf(t1)
            }
        }

        assertScreenshot(c, "text", posterize = 5, includeBackground = false, psnr = 32.0)
    }

    @Test
    fun testFillTextAlignmentWorks() = korgeScreenshotTest(Size(100, 100)) {
        run {
            val p = Point(50, 50)
            graphics {
                fill(Colors.RED) {
                    rect(Rectangle.fromBounds(20, 20, 80, 80))
                }

                fillText("I", p, size = 60.0, font = DefaultTtfFont, color = Colors.WHITE, align = TextAlignment.MIDDLE_CENTER)
                drawText("L", p, size = 60.0, font = DefaultTtfFont, fillStyle = Colors.PURPLE, align = TextAlignment.MIDDLE_CENTER)
            }
        }

        assertScreenshot(posterize = 6)
    }
}
