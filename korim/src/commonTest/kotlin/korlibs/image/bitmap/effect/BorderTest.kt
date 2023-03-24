package korlibs.image.bitmap.effect

import korlibs.image.bitmap.Bitmap32
import korlibs.image.bitmap.context2d
import korlibs.image.color.Colors
import korlibs.image.font.DefaultTtfFont
import korlibs.io.async.suspendTest
import kotlin.test.Test

class BorderTest {
    @Test
    fun test() = suspendTest {
        val bmp = Bitmap32(100, 100, premultiplied = false).context2d {
            drawText("Hello", x = 20.0, y = 20.0, font = DefaultTtfFont, paint = Colors.RED)
        }
        val bmpBorder = bmp.border(4, Colors.GREEN)
        //bmpBorder.showImageAndWait()
    }
}
