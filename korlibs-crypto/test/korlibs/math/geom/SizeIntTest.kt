package korlibs.math.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class SizeIntTest {
    @Test
    fun cover() {
        assertEquals(
            SizeInt(100, 400),
            SizeInt(50, 200).applyScaleMode(container = SizeInt(100, 100), mode = ScaleMode.COVER)
        )
        assertEquals(
            SizeInt(25, 100),
            SizeInt(50, 200).applyScaleMode(container = SizeInt(25, 25), mode = ScaleMode.COVER)
        )
    }

    @Test
    fun test() {
        assertEquals(PointInt(40, 60), PointInt(10, 20) + SizeInt(30, 40))
        assertEquals(PointInt(7, 16), PointInt(10, 20) - SizeInt(3, 4))
    }
}
