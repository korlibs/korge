package com.soywiz.korma.geom

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
                Position3D(0, 0, 0),
                MQuaternion(),
                Scale3D(1, 1, 1)
            )
        )
        assertEquals(
            transMat,
            MMatrix3D().setTRS(
                Position3D(1, 2, 3),
                MQuaternion(),
                Scale3D(1, 1, 1)
            )
        )
    }

    @Test
    fun testGetTRS() {
        val pos = Position3D()
        val quat = MQuaternion()
        val scale = Scale3D()
        transMat.getTRS(pos, quat, scale)

        assertEquals(Position3D(1, 2, 3), pos)
    }

    @Test
    fun testSetGetTRS() {
        val mat = MMatrix3D()
        val opos = Position3D(1, 2, 3)
        val oquat = MQuaternion().setEuler(15.degrees, 30.degrees, 60.degrees)
        val oscale = Scale3D(1, 2, 3)

        val pos = Position3D().copyFrom(opos)
        val quat = MQuaternion().copyFrom(oquat)
        val scale = Scale3D().copyFrom(oscale)

        mat.setTRS(pos, quat, scale)
        mat.getTRS(pos, quat, scale)

        assertEquals(opos, pos)
        assertEquals(oquat, quat)
        assertEquals(oscale, scale.round())
    }

    @Test
    fun testQuat() {
        assertEquals(MQuaternion(0.7, 0.0, 0.0, 0.7).round(1), MQuaternion().setEuler(90.degrees, 0.degrees, 0.degrees).round(1))
        assertEquals(MQuaternion(0.0, 0.7, 0.0, 0.7).round(1), MQuaternion().setEuler(0.degrees, 90.degrees, 0.degrees).round(1))
        assertEquals(MQuaternion(0.0, 0.0, 0.7, 0.7).round(1), MQuaternion().setEuler(0.degrees, 0.degrees, 90.degrees).round(1))

        assertEquals(MEulerRotation(90.degrees, 0.degrees, 0.degrees), MEulerRotation().setQuaternion(MQuaternion().setEuler(90.degrees, 0.degrees, 0.degrees)), 0.1)
        assertEquals(MEulerRotation(0.degrees, 90.degrees, 0.degrees), MEulerRotation().setQuaternion(MQuaternion().setEuler(0.degrees, 90.degrees, 0.degrees)), 0.1)
        assertEquals(MEulerRotation(0.degrees, 0.degrees, 90.degrees), MEulerRotation().setQuaternion(MQuaternion().setEuler(0.degrees, 0.degrees, 90.degrees)), 0.1)
    }

    @Test
    fun testInvert() {
        val mat = MMatrix3D().setTRS(Position3D(1, 2, 3), MQuaternion().setEuler(15.degrees, 30.degrees, 60.degrees), Scale3D(1, 2, 3))
        val inv = MMatrix3D().invert(mat)
        assertEquals(
            MMatrix3D().round(2).toString(),
            (mat * inv).round(2).toString()
        )
    }

    fun assertEquals(a: MEulerRotation, b: MEulerRotation, delta: Double = 0.01) {
        assertTrue("$a\n$b\na!=b // delta=$delta") {
            abs(a.x.degrees - b.x.degrees) <= delta &&
                abs(a.y.degrees - b.y.degrees) <= delta &&
                abs(a.z.degrees - b.z.degrees) <= delta
        }
    }

    fun assertEquals(a: MQuaternion, b: MQuaternion, delta: Double = 0.01) {
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
    fun MQuaternion.round(digits: Int = 0) = setTo(round(x, digits), round(y, digits), round(z, digits), round(w, digits))
    fun MMatrix3D.round(digits: Int = 0) = setToMap { round(it, digits) }

    fun round(x: Float, digits: Int) = (round(x * 10.0.pow(digits)) / 10.0.pow(digits)).toFloat()
    fun round(x: Double, digits: Int) = round(x * 10.0.pow(digits)) / 10.0.pow(digits)

    @Test
    fun testConvert() {
        val mat = MMatrix().translate(100, 20).scale(2, 2)
        assertEquals(MPoint(240, 60), mat.transform(MPoint(20, 10)))
        val m3d = mat.toMatrix4()
        assertEquals(MVector4(240, 60, 0, 1), m3d.transform(MVector4(20, 10, 0, 1)))
    }
}
