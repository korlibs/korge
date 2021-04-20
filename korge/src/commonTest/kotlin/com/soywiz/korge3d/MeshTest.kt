package com.soywiz.korge3d

import kotlin.test.*

@OptIn(Korge3DExperimental::class)
class MeshTest {
    @Test
    fun testCube() {
        val cube = Cube3D(1.0, 1.0, 1.0)
        val vertexCount = cube.mesh.vertexCount
        assertTrue(vertexCount > 0, "cube.mesh.vertexCount(=$vertexCount) is not positive")
    }
}
