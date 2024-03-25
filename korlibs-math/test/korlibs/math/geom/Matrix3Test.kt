package korlibs.math.geom

import korlibs.datastructure.*
import korlibs.platform.*
import kotlin.test.*

class Matrix3Test {
    @Test
    fun testRows() {
        val m = Matrix3.fromRows(
            1f, 2f, 3f,
            -4f, -5f, -6f,
            7f, -8f, 9f
        )
        assertEquals(Vector3F(1f, 2f, 3f), m.r0)
        assertEquals(Vector3F(-4f, -5f, -6f), m.r1)
        assertEquals(Vector3F(7f, -8f, 9f), m.r2)
        assertEquals(m.r0, m.r(0))
        assertEquals(m.r1, m.r(1))
        assertEquals(m.r2, m.r(2))

        assertEquals(Vector3F(1f, -4f, 7f), m.c0)
        assertEquals(Vector3F(2f, -5f, -8f), m.c1)
        assertEquals(Vector3F(3f, -6f, 9f), m.c2)
        assertEquals(m.c0, m.c(0))
        assertEquals(m.c1, m.c(1))
        assertEquals(m.c2, m.c(2))

        assertEquals(m.c0, Vector3F(m.v00, m.v10, m.v20))
        assertEquals(m.c1, Vector3F(m.v01, m.v11, m.v21))
        assertEquals(m.c2, Vector3F(m.v02, m.v12, m.v22))

        assertEquals(Vector3F(m[0, 0], m[1, 0], m[2, 0]), Vector3F(m.v00, m.v01, m.v02))
        assertEquals(Vector3F(m[0, 1], m[1, 1], m[2, 1]), Vector3F(m.v10, m.v11, m.v12))
        assertEquals(Vector3F(m[0, 2], m[1, 2], m[2, 2]), Vector3F(m.v20, m.v21, m.v22))
    }

    @Test
    fun testColumns() {
        val m = Matrix3.fromColumns(
            1f, 2f, 3f,
            -4f, -5f, -6f,
            7f, -8f, 9f
        )

        assertEquals(Vector3F(1f, 2f, 3f), m.c0)
        assertEquals(Vector3F(-4f, -5f, -6f), m.c1)
        assertEquals(Vector3F(7f, -8f, 9f), m.c2)
    }

    @Test
    fun testTranspose() {
        assertEquals(
            Matrix3.fromRows(
                1f, 2f, 3f,
                -4f, -5f, -6f,
                7f, -8f, 9f
            ),
            Matrix3.fromColumns(
                1f, 2f, 3f,
                -4f, -5f, -6f,
                7f, -8f, 9f
            ).transposed()
        )
    }

    @Test
    fun testConstructorRows() {
        val m1 = Matrix3.fromRows(
            Vector3F(1f, 2f, 3f),
            Vector3F(-4f, -5f, -6f),
            Vector3F(7f, -8f, 9f)
        )
        val m2 = Matrix3.fromRows(
            1f, 2f, 3f,
            -4f, -5f, -6f,
            7f, -8f, 9f
        )

        assertEquals(m1, m2)
        assertEquals(m1.hashCode(), m2.hashCode())
        //assertNotEquals(m1.identityHashCode(), m2.identityHashCode())

        assertNotEquals(m1, m2.transposed())
        assertNotEquals(m1.hashCode(), m2.transposed().hashCode())
    }

    @Test
    fun testConstructorCols() {
        if (Platform.isWasm) return

        val m1 = Matrix3.fromColumns(
            Vector3F(1f, 2f, 3f),
            Vector3F(-4f, -5f, -6f),
            Vector3F(7f, -8f, 9f)
        )
        val m2 = Matrix3.fromColumns(
            1f, 2f, 3f,
            -4f, -5f, -6f,
            7f, -8f, 9f
        )

        assertEquals(m1, m2)
        assertEquals(m1.hashCode(), m2.hashCode())
        assertNotEquals(m1.identityHashCode(), m2.identityHashCode())
    }

    @Test
    fun testInverse() {
        val m = Matrix3.fromRows(
            1f, 2f, 3f,
            -4f, -5f, -6f,
            7f, -8f, 9f
        )

        assertEqualsFloat(Matrix3.IDENTITY, m * m.inverted())
    }

    @Test
    fun testMultiply() {
        val m = Matrix3.fromRows(
            1f, 2f, 3f,
            -4f, -5f, -6f,
            7f, -8f, 9f
        )

        assertEqualsFloat(Matrix3.IDENTITY, m * m.inverted())
    }

    @Test
    fun testScale() {
        assertEqualsFloat(Matrix3.fromRows(
            2f, 0f, 0f,
            0f, 2f, 0f,
            0f, 0f, 2f,
        ), Matrix3.IDENTITY * 2f)
    }

    @Test
    fun testUnary() {
        assertEqualsFloat(Matrix3.IDENTITY * (-1f), -Matrix3.IDENTITY)
        assertEqualsFloat(Matrix3.IDENTITY, +Matrix3.IDENTITY)
    }

    @Test
    fun testDeterminant() {
        assertEqualsFloat(1f, Matrix3.IDENTITY.determinant)
        assertEqualsFloat(96f, Matrix3.fromRows(1f, 2f, 3f, -4f, -5f, -6f, 7f, -8f, 9f).determinant)
    }

    @Test
    fun testQuaternion() {
        val quat = Quaternion.fromEuler(30.degrees, 15.degrees, 90.degrees)
        val vec = Vector3F(1f, -2f, 3f)
        assertEqualsFloat(
            quat,
            quat.toMatrix3().toQuaternion(),
        )
        assertEqualsFloat(
            quat.transform(vec),
            quat.toMatrix3().transform(vec)
        )
    }
}
