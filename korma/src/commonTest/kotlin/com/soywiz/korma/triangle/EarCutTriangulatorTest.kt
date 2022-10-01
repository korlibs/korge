package com.soywiz.korma.triangle

import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EarCutTriangulatorTest {
    @Test
    fun test() {
        val path = //rect(200, 0, 100, 100)
            buildVectorPath(VectorPath()) {
                rect(0, 0, 10, 10)
                rectHole(2, 2, 6, 6)
                //rect(200, 0, 100, 100)
            }

        val triangles = path.triangulateEarCut()

        assertEquals(
            "[(0, 0), (10, 0), (10, 10), (0, 10), (2, 2), (2, 8), (8, 8), (8, 2)]",
            triangles.points.toList().toString()
        )

        assertEquals(
            "3,0,4,7,4,0,3,4,5,7,0,1,2,3,5,6,7,1,2,5,6,6,1,2",
            triangles.indices.toList().joinToString(",")
        )
        //println(path.toPathList())
        //Triangulator.triangulate()
    }
}
