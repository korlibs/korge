package korlibs.math.geom.vector

import korlibs.math.geom.shape.*

class StrokeToFillTest {
    private inline fun path(stroke: Double = 2.0, crossinline block: VectorPath.() -> Unit) =
        buildVectorPath(VectorPath()) {
            block()
        }.strokeToFill(stroke).cachedPoints.toList().map { it.int }.toString()

    /*
    @Test
    fun testSimple() {
        assertEquals(
            "[(0, -1), (10, -1), (10, 1), (0, 1)]",
            path { line(0, 0, 10, 0) }
        )
    }

    @Test
    fun testSimple2() {
        assertEquals(
            "[(1, 0), (1, 10), (-1, 10), (-1, 0)]",
            path { line(0, 0, 0, 10) }
        )
    }
     */

    /*
    @Test
    fun testSimple3() {
        assertEquals(
            "--",
            path {
                line(0, 0, 10, 0)
                lineToV(10)
            }
        )
    }
     */
}
