package korlibs.math.geom.trapezoid

import kotlin.test.*

class FTrapezoidsIntTest {
    val trapezoids = FTrapezoidsInt {
        add(0, 0, 0, -10, +10, 10)
        add(1, 2, 3, 4, 5, 6)
    }

    @Test
    fun testBasic() {
        assertEquals(
            """
                Trapezoid[0]((0, 0, 0), (-10, 10, 10))
                Trapezoid[1]((1, 2, 3), (4, 5, 6))
            """.trimIndent(),
            trapezoids.map { it.toStringDefault() }.joinToString("\n")
        )

    }

    @Test
    fun testConversion() {
        assertEquals(
            """
                TrapezoidInt(x0a=0, x0b=0, y0=0, x1a=-10, x1b=10, y1=10)
                TrapezoidInt(x0a=1, x0b=2, y0=3, x1a=4, x1b=5, y1=6)
            """.trimIndent(),
            trapezoids.toTrapezoidIntList().joinToString("\n")
        )
    }

    @Test
    fun testConversionAndBack() {
        assertEquals(
            trapezoids.toTrapezoidIntList(),
            trapezoids.toTrapezoidIntList().toFTrapezoidsInt().toTrapezoidIntList()
        )
    }

    @Test
    fun testTriangulate() {
        assertEquals(
            """
                TriangleInt((-10, 10), (0, 0), (10, 10))
                TriangleInt((1, 3), (2, 3), (4, 6))
                TriangleInt((4, 6), (2, 3), (5, 6))
            """.trimIndent(),
            trapezoids.toTrapezoidIntList().triangulate().toTriangleIntList().toFTrianglesInt().toTriangleIntList().joinToString("\n")
        )

    }
}
