package korlibs.math.geom.trapezoid

import kotlin.test.*

class TrapezoidIntTest {
    @Test
    fun test() {
        val trapezoid = TrapezoidInt(3, 8, 2, 1, 10, 6)

        assertEquals(
            """
               baseA: 5
               baseB: 9
               height: 4
               area: 28
               ............
               ............
               ...######...
               ...######...
               ..########..
               ..########..
               .##########.
               ............
            """.trimIndent(),
            (listOf(
                "baseA: ${trapezoid.baseA}",
                "baseB: ${trapezoid.baseB}",
                "height: ${trapezoid.height}",
                "area: ${trapezoid.area.toInt()}",
            ) +
                (0 until 8).map { y ->
                    Array(12) { x -> if (trapezoid.inside(x, y)) "#" else "." }.joinToString("")
                }).joinToString("\n")
        )
    }
}
