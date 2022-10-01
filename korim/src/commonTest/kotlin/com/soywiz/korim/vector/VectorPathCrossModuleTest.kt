package com.soywiz.korim.vector

import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.Test
import kotlin.test.assertEquals

class VectorPathCrossModuleTest {
    @Test
    fun testVisitEdgesSimplified() {
        val log = arrayListOf<String>()
        buildVectorPath(VectorPath()) {
            moveTo(100, 100)
            quadTo(100, 200, 200, 200)
            close()
        }.visitEdgesSimple(
            { x0, y0, x1, y1 -> log.add("line(${x0.toInt()}, ${y0.toInt()}, ${x1.toInt()}, ${y1.toInt()})") },
            { x0, y0, x1, y1, x2, y2, x3, y3 -> log.add("cubic(${x0.toInt()}, ${y0.toInt()}, ${x1.toInt()}, ${y1.toInt()}, ${x2.toInt()}, ${y2.toInt()}, ${x3.toInt()}, ${y3.toInt()})") },
            { log.add("close") },
        )
        assertEquals(
            """
                cubic(100, 100, 100, 166, 133, 200, 200, 200)
                line(200, 200, 100, 100)
                close
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}
