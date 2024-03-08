package korlibs.image.bitmap.effect

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.math.geom.*
import kotlin.test.*

class BlurTest {
    @Test
    fun test() = suspendTest {
        val bmpWithDropShadow = Bitmap32(100, 100, premultiplied = true).context2d {
            fill(Colors.RED) {
                circle(Point(50, 50), 40.0)
            }
        }.dropShadowInplace(0, 0, 5, Colors.BLUE)
        //bmpWithDropShadow.showImageAndWait()
    }
}
