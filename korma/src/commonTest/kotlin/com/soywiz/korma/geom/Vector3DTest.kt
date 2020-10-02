package com.soywiz.korma.geom

import kotlin.test.*

class Vector3DTest {
    @Test
    fun testNormalize() {
        val v = Vector3D(2, 0, 0)
        // Normalized doesn't changes the original vector
        assertEquals(Vector3D(1, 0, 0), v.normalized())
        assertEquals(Vector3D(2, 0, 0), v)

        // Normalize mutates the vector
        assertEquals(Vector3D(1, 0, 0), v.normalize())
        assertEquals(Vector3D(1, 0, 0), v)
    }

    @Test
    fun testCrossProduct() {
        val xInt = Vector3D().cross(Vector3D(1, 0, 0), Vector3D(0, 1, 0))
        assertEquals(Vector3D(0, 0, 1), xInt)
        val xDouble = Vector3D().cross(Vector3D(1.0, 0.0, 0.0), Vector3D(0.0, 1.0, 0.0))
        assertEquals(Vector3D(0.0, 0.0, 1.0), xDouble)
    }

    @Test
    fun testDotProduct() {
        val dot = Vector3D(0.5, 1.0, 0.0).dot(Vector3D(3.0, 1.0, 1.0))
        assertEquals(2.5f, dot)
    }

    @Test
    fun testBasicMath() {
        val v = Vector3D(0,0,0)
        v.add(v, Vector3D(1,0,0))
        assertEquals(Vector3D(1, 0,0, 2), v)
        v.scale(5)
        assertEquals(Vector3D(5, 0 ,0,10), v)
        v.sub(v, Vector3D(2, 1, 0))
        assertEquals(Vector3D(3, -1, 0, 9), v)
    }

}
