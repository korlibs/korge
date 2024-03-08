package korlibs.math.geom.trapezoid

import kotlin.test.*

class FSegmentsIntTest {
    @Test
    fun test() {
        val segments = FSegmentsInt {
            add(0, 70, 0, 100)
            add(50, 50, 100, 100)
        }
        assertEquals(2, segments.size)
        assertEquals(
            """
                Segment[0]((0, 70), (0, 100))
                Segment[1]((50, 50), (100, 100))
            """.trimIndent(),
            segments { (0 until segments.size).map { this[it].toStringDefault() } }.joinToString("\n")
        )
        assertEquals(
            listOf(50, 70, 100),
            segments.getAllYSorted().toList()
        )
    }

    @Test
    fun testSlope() {
        val segments = FSegmentsInt {
            val segment = add(10, 20, 100, 100)
            assertEquals(11, segment.y(0))
            assertEquals(20, segment.y(10))
            assertEquals(56, segment.y(50))
            assertEquals(100, segment.y(100))

            assertEquals(0, segment.x(11))
            assertEquals(10, segment.x(20))
            assertEquals(50, segment.x(56))
            assertEquals(100, segment.x(100))
        }
    }

    @Test
    fun testConversion() {
        val segments = FSegmentsInt {
            add(0, 70, 0, 100)
            add(50, 50, 100, 100)
        }

        assertEquals(
            """
                SegmentInt((0, 70), (0, 100))
                SegmentInt((50, 50), (100, 100))
            """.trimIndent(),
            segments.toSegmentIntList().toFSegmentsInt().toSegmentIntList().joinToString("\n")
        )

    }
}
