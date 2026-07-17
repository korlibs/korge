package korlibs.render

import korlibs.image.color.Colors
import korlibs.image.vector.buildShape
import korlibs.io.async.suspendTest
import korlibs.math.geom.Point
import korlibs.math.geom.PointInt
import korlibs.math.geom.SizeInt
import korlibs.math.geom.Vector2I
import kotlin.test.Test
import kotlin.test.assertEquals

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
