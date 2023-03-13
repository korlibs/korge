package com.soywiz.korma.geom.triangle.pathfind

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.triangle.pathfind.finder
import com.soywiz.korma.triangle.pathfind.spatialMesh
import com.soywiz.korma.triangle.poly2tri.triangulateSafe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpatialMeshTest {
    @Test
    fun testSpatialMesh() {
        val triangles = buildVectorPath(VectorPath()) {
            rect(0, 0, 100, 100)
            rect(20, 20, 60, 60)
        }.triangulateSafe()

        val finder = triangles.spatialMesh().finder()
        assertEquals("[(10, 10), (80, 20), (90, 90)]", "${finder.find(Point(10, 10), Point(90, 90))}")
        assertEquals("[(90, 10), (80, 20), (90, 90)]", "${finder.find(Point(90, 10), Point(90, 90))}")
        assertFailsWith<Error>("Point2d not inside triangles") { finder.find(Point(-10, -10), Point(90, 90)) }
    }
}
