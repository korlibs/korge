package korlibs.math.geom

import kotlin.test.*

class EllipseTest {
    @Test
    fun testContainsPoint() {
        val ellipse = Ellipse(Point(15, 5), Size(15, 4))
        assertEquals(
            """
                ...............................
                ........##############.........
                ...########################....
                .############################..
                ##############################.
                ##############################.
                .############################..
                ...########################....
                ........##############.........
                ...............................
                ...............................
            """.trimIndent(),
            (0 .. 10).joinToString("\n") { y ->
                buildString { for (x in 0 .. 30) append(if (ellipse.containsPoint(Point(x + 0.5f, y + 0.5f))) '#' else '.') }
            }
        )
    }
}
