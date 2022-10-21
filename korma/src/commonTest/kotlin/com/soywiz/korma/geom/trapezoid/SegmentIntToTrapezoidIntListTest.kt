package com.soywiz.korma.geom.trapezoid

import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class SegmentIntToTrapezoidIntListTest {
    fun trapezoids(winding: Winding = Winding.NON_ZERO, scale: Int = 1, block: VectorBuilder.() -> Unit): FTrapezoidsInt =
        buildVectorPath(winding = winding) { block() }.toTrapezoids(scale)

    fun trapezoidsStr(winding: Winding = Winding.NON_ZERO, scale: Int = 1, block: VectorBuilder.() -> Unit): String = trapezoids(winding, scale, block).toTrapezoidIntList().joinToString("\n")

    @Test
    fun testSimpleRect() {
        assertEquals(
            """
                TrapezoidInt(x0a=0, x0b=100, y0=0, x1a=0, x1b=100, y1=100)
            """.trimIndent(),
            trapezoidsStr {
                rect(0, 0, 100, 100)
            }
        )
    }

    @Test
    fun testCross() {
        assertEquals(
            """
                TrapezoidInt(x0a=0, x0b=100, y0=0, x1a=50, x1b=50, y1=50)
                TrapezoidInt(x0a=50, x0b=50, y0=50, x1a=100, x1b=0, y1=100)
            """.trimIndent(),
            trapezoidsStr {
                line(0, 0, 100, 100)
                line(100, 0, 0, 100)
            }
        )
    }

    @Test
    fun testRectWithHole() {
        assertEquals(
            """
                TrapezoidInt(x0a=0, x0b=100, y0=0, x1a=0, x1b=100, y1=20)
                TrapezoidInt(x0a=0, x0b=20, y0=20, x1a=0, x1b=20, y1=80)
                TrapezoidInt(x0a=80, x0b=100, y0=20, x1a=80, x1b=100, y1=80)
                TrapezoidInt(x0a=0, x0b=100, y0=80, x1a=0, x1b=100, y1=100)
            """.trimIndent(),
            trapezoidsStr {
                rect(0, 0, 100, 100)
                rectHole(20, 20, 60, 60)
            }
        )
    }

    @Test
    fun testRectWithNonHole() {
        assertEquals(
            """
                TrapezoidInt(x0a=0, x0b=100, y0=0, x1a=0, x1b=100, y1=20)
                TrapezoidInt(x0a=0, x0b=20, y0=20, x1a=0, x1b=20, y1=80)
                TrapezoidInt(x0a=20, x0b=80, y0=20, x1a=20, x1b=80, y1=80)
                TrapezoidInt(x0a=80, x0b=100, y0=20, x1a=80, x1b=100, y1=80)
                TrapezoidInt(x0a=0, x0b=100, y0=80, x1a=0, x1b=100, y1=100)
            """.trimIndent(),
            trapezoidsStr {
                rect(0, 0, 100, 100)
                rect(20, 20, 60, 60)
            }
        )
    }

    @Test
    fun testCrossedRect() {
        assertEquals(
            """
                TrapezoidInt(x0a=0, x0b=50, y0=0, x1a=0, x1b=50, y1=100)
                TrapezoidInt(x0a=100, x0b=150, y0=0, x1a=100, x1b=150, y1=100)
            """.trimIndent(),
            trapezoidsStr(Winding.EVEN_ODD) {
                rect(0, 0, 100, 100)
                rect(50, 0, 100, 100)
            }
        )
    }

    //@Test
    //fun testRectWithHoleBench() {
    //    for (n in 0 until 100000) {
    //        trapezoids {
    //            rect(0, 0, 100, 100)
    //            rectHole(20, 20, 60, 60)
    //        }
    //    }
    //}
}
