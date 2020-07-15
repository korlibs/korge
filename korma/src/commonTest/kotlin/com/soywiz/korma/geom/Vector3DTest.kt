package com.soywiz.korma.geom

import kotlin.test.*

class Vector3DTest {
    @Test
    fun test() {
        val v = Vector3D(2, 0, 0)
        // Normalized doesn't changes the original vector
        assertEquals(Vector3D(1, 0, 0), v.normalized())
        assertEquals(Vector3D(2, 0, 0), v)

        // Normalize mutates the vector
        assertEquals(Vector3D(1, 0, 0), v.normalize())
        assertEquals(Vector3D(1, 0, 0), v)
    }
}
