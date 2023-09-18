package korlibs.math.geom

import korlibs.number.*
import kotlin.test.Test
import kotlin.test.assertEquals

class BoundsBuilderTest {
    @Test
    fun name() {
        val bb = MBoundsBuilder()
        bb.add(MRectangle(20, 10, 200, 300))
        bb.add(MRectangle(2000, 70, 400, 50))
        bb.add(MRectangle(10000, 10000, 0, 0))
        assertEquals("Rectangle(x=20, y=10, width=2380, height=300)", bb.getBounds().toString())
        bb.reset()
        assertEquals("null", bb.getBoundsOrNull().toString())
        assertEquals("Rectangle(x=0, y=0, width=0, height=0)", bb.getBounds().toString())
        bb.add(MRectangle.fromBounds(0, 0, 1, 1))
        assertEquals("Rectangle(x=0, y=0, width=1, height=1)", bb.getBoundsOrNull().toString())
        assertEquals("Rectangle(x=0, y=0, width=1, height=1)", bb.getBounds().toString())
    }

    @Test
    fun test2() {
        val bb = MBoundsBuilder()
            .add(-100, 100)
            .add(-90, 100)
            .add(-100, 110)
            .add(-90, 110)

        assertEquals(MRectangle(-100, 100, 10, 10), bb.getBounds())
    }

    fun MBoundsBuilder.toPropsString(): String {
        val bb = this
        return """
            ${bb.npoints}, ${bb.hasPoints}
            (${bb.xmin.niceStr}, ${bb.xmax.niceStr}, ${bb.ymin.niceStr}, ${bb.ymax.niceStr})
            (${bb.xminOrNull?.niceStr}, ${bb.xmaxOrNull?.niceStr}, ${bb.yminOrNull?.niceStr}, ${bb.ymaxOrNull?.niceStr})
            (${bb.xminOr(0.0).niceStr}, ${bb.xmaxOr(0.0).niceStr}, ${bb.yminOr(0.0).niceStr}, ${bb.ymaxOr(0.0).niceStr})
        """.trimIndent()
    }

    @Test
    fun testNoPoints() {
        assertEquals(
            """
                0, false
                (Infinity, -Infinity, Infinity, -Infinity)
                (null, null, null, null)
                (0, 0, 0, 0)
            """.trimIndent(),
            MBoundsBuilder().toPropsString()
        )
    }

    @Test
    fun testOnePoint() {
        assertEquals(
            """
                1, true
                (0, 0, 0, 0)
                (0, 0, 0, 0)
                (0, 0, 0, 0)
            """.trimIndent(),
            MBoundsBuilder().also { it.add(0, 0) }.toPropsString()
        )

        assertEquals(
            """
                1, true
                (-1, -1, -3, -3)
                (-1, -1, -3, -3)
                (-1, -1, -3, -3)
            """.trimIndent(),
            MBoundsBuilder().also { it.add(-1, -3) }.toPropsString()
        )

        assertEquals(
            """
                1, true
                (7, 7, 5, 5)
                (7, 7, 5, 5)
                (7, 7, 5, 5)
            """.trimIndent(),
            MBoundsBuilder().also { it.add(+7, +5) }.toPropsString()
        )
    }

    @Test
    fun testTwoPoints() {
        assertEquals(
            """
                2, true
                (-1, 7, -5, 3)
                (-1, 7, -5, 3)
                (-1, 7, -5, 3)
            """.trimIndent(),
            MBoundsBuilder().also { it.add(-1, -5).add(+7, +3) }.toPropsString()
        )
    }
}
