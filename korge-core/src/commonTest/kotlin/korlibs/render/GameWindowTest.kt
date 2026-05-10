package korlibs.render

import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.io.async.*
import korlibs.math.geom.*
import kotlin.test.*

class GameWindowTest {
    @Test
    fun testCustomCursor() = suspendTest {
        val cursor = CustomCursor(buildShape {
            fill(Colors.RED) {
                moveTo(Point(0, 0))
                lineTo(Point(-32, -32))
                lineTo(Point(+32, -32))
                close()
            }
        })
        val bitmap = cursor.createBitmap()
        assertEquals(SizeInt(64, 32), bitmap.bitmap.size)
        assertEquals(Vector2I(32, 31), bitmap.hotspot)
        assertEquals(PointInt(32, 31), bitmap.hotspot)
        //bitmap.bitmap.showImageAndWait()
    }
}
