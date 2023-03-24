package korlibs.math.geom

import kotlin.test.*

class Vector3Test {
    @Test
    fun testAdd() = assertEquals(Vector3(11f, 22f, 33f), Vector3(1f, 2f, 3f) + Vector3(10f, 20f, 30f))
    @Test
    fun testTimes() = assertEquals(Vector3(-1f, -4f, -9f), Vector3(1f, 2f, 3f) * Vector3(-1f, -2f, -3f))
    @Test
    fun testToString() = assertEquals("Vector3(1, 2, 3)", Vector3(1f, 2f, 3f).toString())
    @Test
    fun testNormalized() = assertEquals(1f, Vector3(1f, 2f, 4f).normalized().length, 0.00001f)
    @Test
    fun testEquals() {
        assertEquals(true, Vector3(1f, 2f, 3f) == Vector3(1f, 2f, 3f))
        assertEquals(false, Vector3(1f, 2f, 3f) == Vector3(1f, 2f, -3f))
    }
}
