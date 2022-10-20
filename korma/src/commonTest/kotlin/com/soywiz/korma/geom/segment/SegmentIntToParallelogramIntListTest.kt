package com.soywiz.korma.geom.segment

import com.soywiz.korma.geom.parallelogram.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.*

class SegmentIntToParallelogramIntListTest {
    fun parallelograms(winding: Winding = Winding.NON_ZERO, scale: Int = 1, block: VectorBuilder.() -> Unit): List<ParallelogramInt> =
        buildVectorPath(winding = winding) { block() }.toParallelograms(scale)

    fun parallelogramsString(winding: Winding = Winding.NON_ZERO, scale: Int = 1, block: VectorBuilder.() -> Unit): String = parallelograms(winding, scale, block).joinToString("\n")

    @Test
    fun testSimpleRect() {
        assertEquals(
            """
                ParallelogramInt(x0a=0, x0b=100, y0=0, x1a=0, x1b=100, y1=100)
            """.trimIndent(),
            parallelogramsString {
                rect(0, 0, 100, 100)
            }
        )
    }

    @Test
    fun testCross() {
        assertEquals(
            """
                ParallelogramInt(x0a=0, x0b=100, y0=0, x1a=50, x1b=50, y1=50)
                ParallelogramInt(x0a=50, x0b=50, y0=50, x1a=100, x1b=0, y1=100)
            """.trimIndent(),
            parallelogramsString {
                line(0, 0, 100, 100)
                line(100, 0, 0, 100)
            }
        )
    }

    @Test
    fun testRectWithHole() {
        assertEquals(
            """
                ParallelogramInt(x0a=0, x0b=100, y0=0, x1a=0, x1b=100, y1=20)
                ParallelogramInt(x0a=0, x0b=20, y0=20, x1a=0, x1b=20, y1=80)
                ParallelogramInt(x0a=80, x0b=100, y0=20, x1a=80, x1b=100, y1=80)
                ParallelogramInt(x0a=0, x0b=100, y0=80, x1a=0, x1b=100, y1=100)
            """.trimIndent(),
            parallelogramsString {
                rect(0, 0, 100, 100)
                rectHole(20, 20, 60, 60)
            }
        )
    }

    @Test
    fun testRectWithNonHole() {
        assertEquals(
            """
                ParallelogramInt(x0a=0, x0b=100, y0=0, x1a=0, x1b=100, y1=20)
                ParallelogramInt(x0a=0, x0b=20, y0=20, x1a=0, x1b=20, y1=80)
                ParallelogramInt(x0a=20, x0b=80, y0=20, x1a=20, x1b=80, y1=80)
                ParallelogramInt(x0a=80, x0b=100, y0=20, x1a=80, x1b=100, y1=80)
                ParallelogramInt(x0a=0, x0b=100, y0=80, x1a=0, x1b=100, y1=100)
            """.trimIndent(),
            parallelogramsString {
                rect(0, 0, 100, 100)
                rect(20, 20, 60, 60)
            }
        )
    }

    @Test
    fun testCrossedRect() {
        assertEquals(
            """
                ParallelogramInt(x0a=0, x0b=50, y0=0, x1a=0, x1b=50, y1=100)
                ParallelogramInt(x0a=100, x0b=150, y0=0, x1a=100, x1b=150, y1=100)
            """.trimIndent(),
            parallelogramsString(Winding.EVEN_ODD) {
                rect(0, 0, 100, 100)
                rect(50, 0, 100, 100)
            }
        )
    }
}
