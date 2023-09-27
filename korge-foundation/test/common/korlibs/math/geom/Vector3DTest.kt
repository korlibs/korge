package korlibs.math.geom

import kotlin.test.Test
import kotlin.test.assertEquals

class Vector3DTest {
    @Test
    fun testNormalize() {
        val v = MVector4(2, 0, 0)
        // Normalized doesn't changes the original vector
        assertEquals(MVector4(1, 0, 0), v.normalized())
        assertEquals(MVector4(2, 0, 0), v)

        // Normalize mutates the vector
        assertEquals(MVector4(1, 0, 0), v.normalize())
        assertEquals(MVector4(1, 0, 0), v)
    }

    @Test
    fun testCrossProduct() {
        val xInt = MVector4().cross(MVector4(1, 0, 0), MVector4(0, 1, 0))
        assertEquals(MVector4(0, 0, 1), xInt)
        val xDouble = MVector4().cross(MVector4(1.0, 0.0, 0.0), MVector4(0.0, 1.0, 0.0))
        assertEquals(MVector4(0.0, 0.0, 1.0), xDouble)
    }

    @Test
    fun testDotProduct() {
        val dot = MVector4(0.5, 1.0, 0.0).dot(MVector4(3.0, 1.0, 1.0))
        assertEquals(2.5f, dot)
    }

    @Test
    fun testBasicMath() {
        val v = MVector4(0,0,0)
        v.add(v, MVector4(1,0,0))
        assertEquals(MVector4(1, 0,0, 2), v)
        v.scale(5)
        assertEquals(MVector4(5, 0 ,0,10), v)
        v.sub(v, MVector4(2, 1, 0))
        assertEquals(MVector4(3, -1, 0, 9), v)
    }

}
