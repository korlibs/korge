package samples

import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.text.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.math.geom.*

class MainTextBounds2 : Scene() {
    override suspend fun SContainer.sceneMain() {
        for ((tbindex, withTextBounds) in listOf(true, false).withIndex()) {
            for ((tindex, text) in listOf("hello world", "hello\nworld").withIndex()) {
                for ((findex, font) in listOf(DefaultTtfFontAsBitmap, DefaultTtfFont).withIndex()) {
                    for ((hindex, horizontal) in listOf(HorizontalAlign.LEFT, HorizontalAlign.CENTER, HorizontalAlign.RIGHT, HorizontalAlign.JUSTIFY).withIndex()) {
                        for ((vindex, vertical) in listOf(VerticalAlign.TOP, VerticalAlign.CENTER, VerticalAlign.BOTTOM, VerticalAlign.BASELINE).withIndex()) {
                            val textColor = if (findex == 0) Colors.RED else Colors.GREEN
                            container {
                                val W = 70
                                val H = 60
                                val PAD = 6
                                position(
                                    10 + (W + PAD) * (hindex) + ((findex + (tindex * 2.1)) * ((W + PAD + 2) * 4)),
                                    10 + (H + PAD) * vindex + (tbindex * ((H + PAD + 4) * 4))
                                )
                                if (withTextBounds) {
                                    solidRect(Size(W, H), Colors.DIMGREY)
                                    text(text, alignment = TextAlignment(horizontal, vertical), color = textColor, font = font, textSize = 10) {
                                        this.setTextBounds(Rectangle(0, 0, W, H))
                                    }
                                } else {
                                    line(Point(-4, 0), Point(+4, 0), Colors.DIMGREY)
                                    line(Point(0, -4), Point(0, +4), Colors.DIMGREY)
                                    text(text, alignment = TextAlignment(horizontal, vertical), color = textColor, font = font, textSize = 10)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
