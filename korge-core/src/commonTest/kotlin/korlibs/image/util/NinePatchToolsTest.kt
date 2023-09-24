package korlibs.image.util

import korlibs.datastructure.*
import korlibs.math.geom.*
import korlibs.math.range.*
import kotlin.test.*

class NinePatchToolsTest {
    @Test
    fun testTransform1D() {
        val result = NinePatchSlices(4.0 until 9.0).transform1D(
            listOf(
                doubleArrayListOf(1.0),
                doubleArrayListOf(5.0),
                doubleArrayListOf(10.0),
                doubleArrayListOf(15.0),
            ),
            oldLen = 15.0,
            newLen = 32.0,
        )
        assertEquals(
            listOf(
                doubleArrayListOf(1.0),
                doubleArrayListOf(8.4),
                doubleArrayListOf(27.0),
                doubleArrayListOf(32.0),
            ),
            result.toList()
        )
    }

    @Test
    fun testTransform2D() {
        val result = NinePatchSlices2D(
            x = NinePatchSlices(4.0 until 9.0),
            y = NinePatchSlices(4.0 until 9.0),
        ).transform2D(
            listOf(
                pointArrayListOf(Point(1, 1)),
                pointArrayListOf(Point(5, 5)),
                pointArrayListOf(Point(10, 10)),
                pointArrayListOf(Point(15, 15)),
            ),
            oldSize = Size(15.0, 15.0),
            newSize = Size(32.0, 64.0)
        )

        assertEquals(
            listOf(
                pointArrayListOf(Point(1, 1)),
                pointArrayListOf(Point(8.4, 14.8)),
                pointArrayListOf(Point(27, 59)),
                pointArrayListOf(Point(32, 64)),
            ),
            result.toList()
        )
    }
}
