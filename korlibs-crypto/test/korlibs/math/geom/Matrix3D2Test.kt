package korlibs.math.geom

import korlibs.datastructure.*
import kotlin.math.*
import kotlin.test.*

class Matrix3D2Test {
    val transMat = MMatrix3D.fromRows(
        1f, 0f, 0f, 1f,
        0f, 1f, 0f, 2f,
        0f, 0f, 1f, 3f,
        0f, 0f, 0f, 1f
    )

    @Test
    fun testSetTRS() {
        assertEquals(
            MMatrix3D(),
            MMatrix3D().setTRS(
                MPosition3D(0, 0, 0),
                Quaternion(),
                MScale3D(1, 1, 1)
            )
        )
        assertEquals(
            transMat,
            MMatrix3D().setTRS(
                MPosition3D(1, 2, 3),
                Quaternion(),
                MScale3D(1, 1, 1)
            )
        )
    }

    @Test
    fun testGetTRS() {
        val pos = MPosition3D()
        val quat = Ref(Quaternion())
        val scale = MScale3D()
        transMat.getTRS(pos, quat, scale)

        assertEquals(MPosition3D(1, 2, 3), pos)
    }

    @Test
    fun testSetGetTRS() {
        val mat = MMatrix3D()
        val opos = MPosition3D(1, 2, 3)
        val oquat = Quaternion.fromEuler(15.degrees, 30.degrees, 60.degrees)
        val oscale = MScale3D(1, 2, 3)

        val pos = MPosition3D().copyFrom(opos)
        val quat = Ref(oquat)
        val scale = MScale3D().copyFrom(oscale)

        mat.setTRS(pos, quat.value, scale)
        mat.getTRS(pos, quat, scale)

        assertEquals(opos, pos)
        assertEquals(oquat, quat.value)
        assertEquals(oscale, scale.round())
    }

    @Test
    fun testQuat() {
        assertEquals(Quaternion(0.7, 0.0, 0.0, 0.7).round(1), Quaternion.fromEuler(90.degrees, 0.degrees, 0.degrees).round(1))
        assertEquals(Quaternion(0.0, 0.7, 0.0, 0.7).round(1), Quaternion.fromEuler(0.degrees, 90.degrees, 0.degrees).round(1))
        assertEquals(Quaternion(0.0, 0.0, 0.7, 0.7).round(1), Quaternion.fromEuler(0.degrees, 0.degrees, 90.degrees).round(1))

        assertEquals(EulerRotation(90.degrees, 0.degrees, 0.degrees), EulerRotation.fromQuaternion(Quaternion.fromEuler(90.degrees, 0.degrees, 0.degrees)), 0.1)
        assertEquals(EulerRotation(0.degrees, 90.degrees, 0.degrees), EulerRotation.fromQuaternion(Quaternion.fromEuler(0.degrees, 90.degrees, 0.degrees)), 0.1)
        assertEquals(EulerRotation(0.degrees, 0.degrees, 90.degrees), EulerRotation.fromQuaternion(Quaternion.fromEuler(0.degrees, 0.degrees, 90.degrees)), 0.1)
    }

    @Test
    fun testInvert() {
        val mat = MMatrix3D().setTRS(MPosition3D(1, 2, 3), Quaternion.fromEuler(15.degrees, 30.degrees, 60.degrees), MScale3D(1, 2, 3))
        val inv = MMatrix3D().invert(mat)
        assertEquals(
            MMatrix3D().round(2).toString(),
            (mat * inv).round(2).toString()
        )
    }

    fun assertEquals(a: EulerRotation, b: EulerRotation, delta: Double = 0.01) {
        assertTrue("$a\n$b\na!=b // delta=$delta") {
            abs(a.x.degrees - b.x.degrees) <= delta &&
                abs(a.y.degrees - b.y.degrees) <= delta &&
                abs(a.z.degrees - b.z.degrees) <= delta
        }
    }

    fun assertEquals(a: Quaternion, b: Quaternion, delta: Double = 0.01) {
        assertTrue("$a\n$b\na!=b // delta=$delta") {
            abs(a.x - b.x) <= delta &&
                abs(a.y - b.y) <= delta &&
                abs(a.z - b.z) <= delta &&
                abs(a.w - b.w) <= delta
        }
    }

    fun assertEquals(a: Double, b: Double, delta: Double) {
        assertTrue("$a != $b // delta=$delta") { abs(a - b) <= delta }
    }

    fun assertEquals(a: Float, b: Float, delta: Float) {
        assertTrue("$a != $b // delta=$delta") { abs(a - b) <= delta }
    }

    fun MVector4.round(digits: Int = 0) = setTo(round(x, digits), round(y, digits), round(z, digits), round(w, digits))
    fun Quaternion.round(digits: Int = 0) = Quaternion(round(x, digits), round(y, digits), round(z, digits), round(w, digits))
    fun MMatrix3D.round(digits: Int = 0) = setToMap { round(it, digits) }

    fun round(x: Float, digits: Int) = (round(x * 10.0.pow(digits)) / 10.0.pow(digits)).toFloat()
    fun round(x: Double, digits: Int) = round(x * 10.0.pow(digits)) / 10.0.pow(digits)

    @Test
    fun testConvert() {
        val mat = Matrix.IDENTITY.translated(100, 20).scaled(2, 2)
        assertEqualsFloat(Point(240, 60), mat.transform(Point(20, 10)))
        val m3d = mat.toMatrix4()
        assertEqualsFloat(Vector4F(240, 60, 0, 1), m3d.transform(Vector4F(20, 10, 0, 1)))
    }
}
