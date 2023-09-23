package korlibs.math.geom

import korlibs.datastructure.*
import kotlin.math.*
import kotlin.test.*

class Matrix3DTest {
    @Test
    fun testToString() {
        val mat = MMatrix3D.fromRows(
            1f, 2f, 3f, 4f,
            5f, 6f, 7f, 8f,
            9f, 10f, 11f, 12f,
            13f, 14f, 15f, 16f
        )
        assertEquals(
            """
            Matrix3D(
              [ 1, 2, 3, 4 ],
              [ 5, 6, 7, 8 ],
              [ 9, 10, 11, 12 ],
              [ 13, 14, 15, 16 ],
            )
            """.trimIndent(),
            mat.toString()
        )
    }

    @Test
    fun testMultiply() {
        val l = MMatrix3D.fromRows(
            1f, 2f, 3f, 4f,
            5f, 6f, 7f, 8f,
            9f, 10f, 11f, 12f,
            13f, 14f, 15f, 16f
        )
        val r = MMatrix3D.fromRows(
            17f, 18f, 19f, 20f,
            21f, 22f, 23f, 24f,
            25f, 26f, 27f, 28f,
            29f, 30f, 31f, 32f
        )
        assertEquals(
            MMatrix3D.fromRows(
                250f,    260f,    270f,    280f,
                618f,    644f,    670f,    696f,
                986f,    1028f,   1070f,   1112f,
                1354f,   1412f,   1470f,   1528f

            ),
            (l * r)
        )
    }

    @Test
    fun testMatrix4() {
        val matrix = MMatrix3D()
        val identityData = listOf(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
        )
        assertEquals(identityData, matrix.data.map { it.toInt() })
        val matrix2 = matrix.clone().transpose()
        assertEquals(identityData, matrix2.data.map { it.toInt() })
    }

    @Test
    fun test2() {
        val mat = MMatrix3D.fromRows(
            1f, 2f, 3f, 11f,
            4f, 5f, 6f, 12f,
            7f, 8f, 9f, 13f,
            14f, 15f, 16f, 17f
        ) * (-1)

        val floats = FloatArray(9)
        val floats2 = FloatArray(10)
        val floats3 = FloatArray(17)

        mat.copyToFloat3x3(floats, MatrixMajorOrder.ROW)
        assertEquals(listOf(-1f, -2f, -3f, -4f, -5f, -6f, -7f, -8f, -9f), floats.toList())

        mat.copyToFloat3x3(floats2, MatrixMajorOrder.ROW, 1)
        assertEquals(listOf(0f, -1f, -2f, -3f, -4f, -5f, -6f, -7f, -8f, -9f), floats2.toList())

        mat.copyToFloat3x3(floats2, MatrixMajorOrder.COLUMN, 1)
        assertEquals(listOf(0f, -1f, -4f, -7f, -2f, -5f, -8f, -3f, -6f, -9f), floats2.toList())

        mat.copyToFloat4x4(floats3, MatrixMajorOrder.ROW, 1)
        assertEquals(listOf(0f, -1f, -2f, -3f, -11f, -4f, -5f, -6f, -12f, -7f, -8f, -9f, -13f, -14f, -15f, -16f, -17f), floats3.toList())
        mat.copyToFloat4x4(floats3, MatrixMajorOrder.COLUMN, 0)
        assertEquals(listOf(-1f, -4f, -7f, -14f, -2f, -5f, -8f, -15f, -3f, -6f, -9f, -16f, -11f, -12f, -13f, -17f, -17f), floats3.toList())
    }

    @Test
    fun test3() {
        run {
            val mat = MMatrix(2, 0, 0, 2, 20, 20)
            val mat4 = mat.toMatrix4()
            assertEquals(MPoint(40, 40), mat.transform(MPoint(10, 10)))
            assertEquals(MVector4(40, 40, 0), mat4.transform(MVector4(10f, 10f, 0f)))
        }
        run {
            val mat = MMatrix(1, 2, 3, 4, 5, 6)
            val mat4 = mat.toMatrix4()
            assertEquals(MPoint(45, 66), mat.transform(MPoint(10, 10)))
            assertEquals(MVector4(45, 66, 0), mat4.transform(MVector4(10f, 10f, 0f)))
        }
    }

    @Test
    fun ortho() {
        run {
            val projection = MMatrix3D().setToOrtho(0f, 200f, 100f, 0f, 0f, -20f)
            assertEquals(MVector4(0, 0, -1), MVector4(100f, 50f, 0f).transform(projection))
            assertEquals(MVector4(0, 0, +1), MVector4(100f, 50f, 20f).transform(projection))
        }
        run {
            val projection = MMatrix3D().setToOrtho(0f, 200f, 100f, 0f, 0f, +20f)
            assertEquals(MVector4(0, 0, -1), MVector4(100f, 50f, 0f).transform(projection))
            assertEquals(MVector4(0, 0, +1), MVector4(100f, 50f, -20f).transform(projection))
        }
        run {
            val projection = MMatrix3D().setToOrtho(MRectangle(0, 0, 200, 100), 0f, +20f)
            assertEquals(MVector4(0, 0, -1), MVector4(100f, 50f, 0f).transform(projection))
            assertEquals(MVector4(0, 0, +1), MVector4(100f, 50f, -20f).transform(projection))
        }
    }

    @Test
    fun translation() {
        assertEquals(MVector4(11f, 22f, 33f), MVector4(10f, 20f, 30f).transform(MMatrix3D().setToTranslation(1f, 2f, 3f)))
    }

    @Test
    fun scale() {
        assertEquals(MVector4(100f, 400f, 900f), MVector4(10f, 20f, 30f).transform(MMatrix3D().setToScale(10f, 20f, 30f)))
    }

    @Test
    fun rotation() {
        assertEquals(MVector4(0, 10, 0), MVector4(10, 0, 0).transform(MMatrix3D().setToRotationZ(90.degrees)))
        assertEquals(MVector4(-10, 0, 0), MVector4(10, 0, 0).transform(MMatrix3D().setToRotationZ(180.degrees)))
        assertEquals(MVector4(0, 10, 0), MVector4(10, 0, 0).transform(MMatrix3D().setToRotation(90.degrees, MVector4(0, 0, 1))))
    }
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
        val quat = Ref<Quaternion>()
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
    }

    @Test
    fun testEuler0() {
        assertEquals(EulerRotation(90.degrees, 0.degrees, 0.degrees), EulerRotation.fromQuaternion(Quaternion.fromEuler(90.degrees, 0.degrees, 0.degrees)), 0.1)
    }
    @Test
    fun testEuler1() {
        assertEquals(EulerRotation(0.degrees, 90.degrees, 0.degrees), EulerRotation.fromQuaternion(Quaternion.fromEuler(0.degrees, 90.degrees, 0.degrees)), 0.1)
    }
    @Test
    fun testEuler2() {
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
    fun Quaternion.round(digits: Int = 0): Quaternion = Quaternion(round(x, digits), round(y, digits), round(z, digits), round(w, digits))
    fun MMatrix3D.round(digits: Int = 0) = setToMap { round(it, digits) }

    fun round(x: Float, digits: Int) = (kotlin.math.round(x * 10.0.pow(digits)) / 10.0.pow(digits)).toFloat()
    fun round(x: Double, digits: Int) = kotlin.math.round(x * 10.0.pow(digits)) / 10.0.pow(digits)
}
