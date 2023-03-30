package korlibs.image.util

import korlibs.datastructure.*
import korlibs.datastructure.doubleArrayListOf
import korlibs.math.geom.*
import korlibs.math.geom.range.until
import kotlin.test.Test
import kotlin.test.assertEquals

class NinePatchToolsTest {
    @Test
    fun testTransform1D() {
        val result = NinePatchSlices(4f until 9f).transform1D(
            listOf(
                floatArrayListOf(1f),
                floatArrayListOf(5f),
                floatArrayListOf(10f),
                floatArrayListOf(15f),
            ),
            oldLen = 15f,
            newLen = 32f
        )
        assertEquals(
            listOf(
                floatArrayListOf(1f),
                floatArrayListOf(8.4f),
                floatArrayListOf(27f),
                floatArrayListOf(32f),
            ),
            result.toList()
        )
    }

    @Test
    fun testTransform2D() {
        val result = NinePatchSlices2D(
            x = NinePatchSlices(4f until 9f),
            y = NinePatchSlices(4f until 9f),
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
                pointArrayListOf(Point(8.4f, 14.8f)),
                pointArrayListOf(Point(27, 59)),
                pointArrayListOf(Point(32, 64)),
            ),
            result.toList()
        )
    }
}
