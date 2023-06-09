package korlibs.math.geom

import kotlin.test.*

class QuaternionTest {
    @Test
    fun testTransformMatrix() {
        assertEqualsFloat(Vector3.RIGHT, Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT).toMatrix().inverted().transform(Vector3.UP))
    }
    @Test
    fun testTransformQuat() {
        assertEqualsFloat(Vector3.RIGHT, Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT).transform(Vector3.UP))
    }
    @Test
    fun testScaled() {
        assertEqualsFloat(Vector3.UP, Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT).scaled(0f).transform(Vector3.UP))
        assertEqualsFloat(Vector3.RIGHT, Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT).scaled(1f).transform(Vector3.UP))
        assertEqualsFloat(Vector3.LEFT, Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT).scaled(-1f).transform(Vector3.UP))
        assertEqualsFloat(Vector3.DOWN, Quaternion.fromVectors(Vector3.UP, Vector3.RIGHT).scaled(2f).transform(Vector3.UP))
    }
}
