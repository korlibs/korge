import com.soywiz.klock.*
import com.soywiz.korge.*
import com.soywiz.korge.annotations.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korim.bitmap.effect.*
import com.soywiz.korim.vector.*
import com.soywiz.korim.color.*
import com.soywiz.korim.font.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.text.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korui.*
import com.soywiz.korui.layout.*

suspend fun main() {
    //GLOBAL_CHECK_GL = true
    Korge(width = 960, height = 720, bgcolor = Colors["#2b2b2b"], clipBorders = false) {
        val font0 = resourcesVfs["clear_sans.fnt"].readFont()
        val font1 = debugBmpFont
        val font2 = DefaultTtfFont
        val font3 = BitmapFont(DefaultTtfFont, 64.0)
        lateinit var text1: Text

        container {
            xy(0, 400)
            text1 = text("01xhjgÁE", 175.0, Colors.GREEN, font2, alignment = TextAlignment.BASELINE_LEFT, autoScaling = true)
                .xy(200, 0)
            val gbounds = graphics {}.xy(200, 0)

            val baseLineLine = solidRect(960, 1, Colors.ORANGE)
            val baseAscent = solidRect(960, 1, Colors.BLUE)
            val baseDescent = solidRect(960, 1, Colors.PURPLE)

            var cachedBounds: Rectangle? = null
            fun updateBounds() {
                val currentBounds = text1.getLocalBounds()
                if (cachedBounds != currentBounds) {
                    cachedBounds = currentBounds
                    gbounds.clear()
                    gbounds.stroke(Colors.RED, StrokeInfo(2.0)) {
                        rect(text1.getLocalBounds())
                    }
                    gbounds.stroke(Colors.BLUE, StrokeInfo(2.0)) {
                        line(-5, 0, +5, 0)
                        line(0, -5, 0, +5)
                    }
                    val metrics = text1.font.getOrNull()!!.getFontMetrics(175.0)
                    baseLineLine.xy(0.0, -metrics.baseline)
                    baseAscent.xy(0.0, -metrics.ascent)
                    baseDescent.xy(0.0, -metrics.descent)
                }
            }

            addUpdater {
                updateBounds()
            }
            updateBounds()

        }

        korui(width, 200) {
            horizontal {
                label("Vertical:")
                button("Top").onClick { text1.verticalAlign = VerticalAlign.TOP }
                button("Middle").onClick { text1.verticalAlign = VerticalAlign.MIDDLE }
                button("Baseline").onClick { text1.verticalAlign = VerticalAlign.BASELINE }
                button("Bottom").onClick { text1.verticalAlign = VerticalAlign.BOTTOM }
            }
            horizontal {
                label("Horizontal:")
                button("Left").onClick { text1.horizontalAlign = HorizontalAlign.LEFT }
                button("Center").onClick { text1.horizontalAlign = HorizontalAlign.CENTER }
                button("Right").onClick { text1.horizontalAlign = HorizontalAlign.RIGHT }
                button("Justify").onClick { text1.horizontalAlign = HorizontalAlign.JUSTIFY }
            }
            horizontal {
                label("Font:")
                button("DefaultTTF").onClick { text1.font = font2 }
                button("DefaultBMP").onClick { text1.font = font1 }
                button("BMPFile").onClick { text1.font = font0 }
                button("TTFtoBMP").onClick { text1.font = font3 }
            }
        }
    }
}
