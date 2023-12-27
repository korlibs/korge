package korlibs.math.geom

import korlibs.math.interpolation.*
import kotlin.test.*

class MMatrixTest {
    private val identity: MMatrix = MMatrix(1, 0, 0, 1, 0, 0)

    @Test
    fun test() {
        val matrix = MMatrix()
        matrix.pretranslate(10, 10)
        matrix.prescale(2, 3)
        matrix.prerotate(90.degrees)
        assertEquals(MPointInt(10, 40).toString(), matrix.transform(MPoint(10, 0)).asInt().toString())
    }

    @Test
    fun transform() {
        val matrix = MMatrix(2, 0, 0, 3, 10, 15)
        assertEquals(30.0, matrix.transformX(10, 20))
        assertEquals(75.0, matrix.transformY(10, 20))
        assertEquals(MPoint(30.0, 75.0), matrix.transform(MPoint(10, 20)))
        assertEquals(MPoint(20.0, 60.0), matrix.deltaTransformPoint(MPoint(10, 20)))
    }

    @Test
    fun type() {
        assertEquals(MatrixType.IDENTITY, MMatrix(1, 0, 0, 1, 0, 0).getType())
        assertEquals(MatrixType.TRANSLATE, MMatrix(1, 0, 0, 1, 10, 0).getType())
        assertEquals(MatrixType.TRANSLATE, MMatrix(1, 0, 0, 1, 0, 10).getType())
        assertEquals(MatrixType.SCALE, MMatrix(1, 0, 0, 2, 0, 0).getType())
        assertEquals(MatrixType.SCALE, MMatrix(2, 0, 0, 1, 0, 0).getType())
        assertEquals(MatrixType.SCALE_TRANSLATE, MMatrix(2, 0, 0, 2, 10, 0).getType())
        assertEquals(MatrixType.COMPLEX, MMatrix(1, 1, 0, 1, 0, 0).getType())

        assertEquals(MatrixType.IDENTITY, MMatrix().getType())
        assertEquals(MatrixType.SCALE, MMatrix().apply { scale(2, 1) }.getType())
        assertEquals(MatrixType.SCALE, MMatrix().apply { scale(1, 2) }.getType())
        assertEquals(MatrixType.TRANSLATE, MMatrix().apply { translate(1, 0) }.getType())
        assertEquals(MatrixType.TRANSLATE, MMatrix().apply { translate(0, 1) }.getType())
        assertEquals(MatrixType.SCALE_TRANSLATE, MMatrix().apply { scale(2, 1).translate(0, 1) }.getType())
        assertEquals(MatrixType.COMPLEX, MMatrix().apply { rotate(90.degrees) }.getType())
    }

    @Test
    fun identity() {
        val m = MMatrix()
        assertEquals(1.0, m.a)
        assertEquals(0.0, m.b)
        assertEquals(0.0, m.c)
        assertEquals(1.0, m.d)
        assertEquals(0.0, m.tx)
        assertEquals(0.0, m.ty)
        m.setTo(2, 2, 2, 2, 2, 2)
        assertEquals(MMatrix(2, 2, 2, 2, 2, 2), m)
        m.identity()
        assertEquals(identity, m)
    }

    @Test
    fun invert() {
        val a = MMatrix(2, 1, 1, 2, 10, 10)
        a.invert()
        assertEquals(MMatrix(a = 0.6666666666666666, b = -0.3333333333333333, c = -0.3333333333333333, d = 0.6666666666666666, tx = -3.333333333333333, ty = -3.333333333333333), a)
    }

    @Test
    fun inverted() {
        val a = MMatrix(2, 1, 1, 2, 10, 10)
        val b = a.inverted()
        assertEquals(identity, a * b)
    }

    @Test
    fun clone() {
        val mat = MMatrix(1, 2, 3, 4, 5, 6)
        assertNotSame(mat, mat.clone())
        assertTrue(mat !== mat.clone())
        assertEquals(mat, mat.clone())
    }

    @Test
    fun keep() {
        val m = MMatrix()
        m.keepMatrix {
            m.setTo(2, 3, 4, 5, 6, 7)
            assertEquals(MMatrix(2, 3, 4, 5, 6, 7), m)
        }
        assertEquals(identity, m)
    }

    @Test
    fun transform2() {
        assertEquals(MMatrix(2, 0, 0, 3, 10, 20), MMatrix.Transform(10.0, 20.0, scaleX = 2.0, scaleY = 3.0).toMatrix())

        // @TODO: Kotlin.JS BUG (missing arguments are NaN or undefined but it works fine on JVM)
        //val t1 = Matrix.Transform(10, 20, scaleX = 2, scaleY = 3, rotation = 90.degrees)
        //val t2 = Matrix.Transform(20, 40, scaleX = 4, scaleY = 5, rotation = 180.degrees)

        val t1 = MMatrix.Transform(10.0, 20.0, scaleX = 2.0, scaleY = 3.0, skewX = 0.0.degrees, skewY = 0.0.degrees, rotation = 90.degrees)
        val t2 = MMatrix.Transform(20.0, 40.0, scaleX = 4.0, scaleY = 5.0, skewX = 0.0.degrees, skewY = 0.0.degrees, rotation = 180.degrees)
        assertEquals(
            MMatrix.Transform(x = 15.0, y = 30.0, scaleX = 3.0, scaleY = 4.0, skewX = 0.0.degrees, skewY = 0.0.degrees, rotation = 135.degrees),
            Ratio.HALF.interpolate(t1, t2)
        )

        val identity = MMatrix.Transform()
        val mt = MMatrix.Transform(1.0, 2.0, 3.0, 4.0, 5.0.radians, 6.0.radians, 7.0.radians)
        mt.identity()
        assertEquals(identity, mt)
        assertNotSame(mt, mt.clone())
        assertEquals(mt, mt.clone())
    }

    @Test
    fun transform3() {
        val m = MMatrix()
        val t = MMatrix.Transform()
        val t2 = MMatrix.Transform()
        t.rotation = -(91.degrees)
        t.scaleAvg = 1.3
        t.toMatrix(m)
        t2.setMatrix(m)
        assertTrue { t.isAlmostEquals(t2) }
    }
}
