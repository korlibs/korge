package com.soywiz.korge3d.experimental

import com.soywiz.korma.geom.*
import kotlin.math.*
import kotlin.test.*

class Matrix3DTest {
    val transMat = Matrix3D.fromRows(
            1f, 0f, 0f, 1f,
            0f, 1f, 0f, 2f,
            0f, 0f, 1f, 3f,
            0f, 0f, 0f, 1f
    )

    @Test
    fun testSetTRS() {
        assertEquals(
                Matrix3D(),
                Matrix3D().setTRS(
                        Position3D(0, 0, 0),
                        Quaternion(),
                        Scale3D(1, 1, 1)
                )
        )
        assertEquals(
                transMat,
                Matrix3D().setTRS(
                        Position3D(1, 2, 3),
                        Quaternion(),
                        Scale3D(1, 1, 1)
                )
        )
    }

    @Test
    fun testGetTRS() {
        val pos = Position3D()
        val quat = Quaternion()
        val scale = Scale3D()
        transMat.getTRS(pos, quat, scale)

        assertEquals(Position3D(1, 2, 3), pos)
    }

    @Test
    fun testSetGetTRS() {
        val mat = Matrix3D()
        val opos = Position3D(1, 2, 3)
        val oquat = Quaternion().setEuler(15.degrees, 30.degrees, 60.degrees)
        val oscale = Scale3D(1, 2, 3)

        val pos = Position3D().copyFrom(opos)
        val quat = Quaternion().copyFrom(oquat)
        val scale = Scale3D().copyFrom(oscale)

        mat.setTRS(pos, quat, scale)
        mat.getTRS(pos, quat, scale)

        assertEquals(opos, pos)
        assertEquals(oquat, quat)
        assertEquals(oscale, scale.round())
    }

    @Test
    fun testQuat() {
        assertEquals(Quaternion(0.7, 0.0, 0.0, 0.7).round(1), Quaternion().setEuler(90.degrees, 0.degrees, 0.degrees).round(1))
        assertEquals(Quaternion(0.0, 0.7, 0.0, 0.7).round(1), Quaternion().setEuler(0.degrees, 90.degrees, 0.degrees).round(1))
        assertEquals(Quaternion(0.0, 0.0, 0.7, 0.7).round(1), Quaternion().setEuler(0.degrees, 0.degrees, 90.degrees).round(1))

        assertEquals(EulerRotation(90.degrees, 0.degrees, 0.degrees), EulerRotation().setQuaternion(Quaternion().setEuler(90.degrees, 0.degrees, 0.degrees)), 0.1)
        assertEquals(EulerRotation(0.degrees, 90.degrees, 0.degrees), EulerRotation().setQuaternion(Quaternion().setEuler(0.degrees, 90.degrees, 0.degrees)), 0.1)
        assertEquals(EulerRotation(0.degrees, 0.degrees, 90.degrees), EulerRotation().setQuaternion(Quaternion().setEuler(0.degrees, 0.degrees, 90.degrees)), 0.1)
    }

    @Test
    fun testInvert() {
        val mat = Matrix3D().setTRS(Position3D(1, 2, 3), Quaternion().setEuler(15.degrees, 30.degrees, 60.degrees), Scale3D(1, 2, 3))
        val inv = Matrix3D().invert(mat)
        assertEquals(
                Matrix3D().round(2).toString(),
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

    fun Vector3D.round(digits: Int = 0) = setTo(round(x, digits), round(y, digits), round(z, digits), round(w, digits))
    fun Quaternion.round(digits: Int = 0) = setTo(round(x, digits), round(y, digits), round(z, digits), round(w, digits))
    fun Matrix3D.round(digits: Int = 0) = setToMap { round(it, digits) }

    fun round(x: Float, digits: Int) = (round(x * 10.0.pow(digits)) / 10.0.pow(digits)).toFloat()
    fun round(x: Double, digits: Int) = round(x * 10.0.pow(digits)) / 10.0.pow(digits)

}
