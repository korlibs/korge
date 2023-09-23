package korlibs.image.text

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.math.geom.*
import kotlin.test.*

class RichTextDataRendererText {
    @Test
    fun test() = suspendTest {
        val nativeImage = NativeImage(512, 512)
        nativeImage.context2d {
            val textBounds = Rectangle(50, 50, 150, 100)
            stroke(Colors.BLUE, lineWidth = 2.0) {
                rect(textBounds)
            }
            drawRichText(
                RichTextData.fromHTML("hello world<br /><br /> this is a long test", style = RichTextData.Style.DEFAULT.copy(textSize = 24.0)),
                bounds = textBounds,
                ellipsis = "...",
                fill = Colors.RED,
                //align = TextAlignment.RIGHT,
                //align = TextAlignment.CENTER,
                align = TextAlignment.MIDDLE_CENTER,
            )
        }
        //nativeImage.showImageAndWait()
    }
}
