package com.soywiz.korma.geom

import com.soywiz.korma.interpolation.*
import kotlin.test.*

class MatrixTest {
    private val identity: IMatrix = Matrix(1, 0, 0, 1, 0, 0)

    @Test
    fun test() {
        val matrix = Matrix()
        matrix.pretranslate(10, 10)
        matrix.prescale(2, 3)
        matrix.prerotate(90.degrees)
        assertEquals(PointInt(10, 40).toString(), matrix.transform(Point(10, 0)).asInt().toString())
    }

    @Test
    fun transform() {
        val matrix = Matrix(2, 0, 0, 3, 10, 15)
        assertEquals(30.0, matrix.transformX(10, 20))
        assertEquals(75.0, matrix.transformY(10, 20))
        assertEquals(Point(30.0, 75.0), matrix.transform(Point(10, 20)))
        assertEquals(Point(20.0, 60.0), matrix.deltaTransformPoint(Point(10, 20)))

    }

    @Test
    fun type() {
        assertEquals(Matrix.Type.IDENTITY, Matrix(1, 0, 0, 1, 0, 0).getType())
        assertEquals(Matrix.Type.TRANSLATE, Matrix(1, 0, 0, 1, 10, 0).getType())
        assertEquals(Matrix.Type.TRANSLATE, Matrix(1, 0, 0, 1, 0, 10).getType())
        assertEquals(Matrix.Type.SCALE, Matrix(1, 0, 0, 2, 0, 0).getType())
        assertEquals(Matrix.Type.SCALE, Matrix(2, 0, 0, 1, 0, 0).getType())
        assertEquals(Matrix.Type.SCALE_TRANSLATE, Matrix(2, 0, 0, 2, 10, 0).getType())
        assertEquals(Matrix.Type.COMPLEX, Matrix(1, 1, 0, 1, 0, 0).getType())

        assertEquals(Matrix.Type.IDENTITY, Matrix().getType())
        assertEquals(Matrix.Type.SCALE, Matrix().apply { scale(2, 1) }.getType())
        assertEquals(Matrix.Type.SCALE, Matrix().apply { scale(1, 2) }.getType())
        assertEquals(Matrix.Type.TRANSLATE, Matrix().apply { translate(1, 0) }.getType())
        assertEquals(Matrix.Type.TRANSLATE, Matrix().apply { translate(0, 1) }.getType())
        assertEquals(Matrix.Type.SCALE_TRANSLATE, Matrix().apply { scale(2, 1).translate(0, 1) }.getType())
        assertEquals(Matrix.Type.COMPLEX, Matrix().apply { rotate(90.degrees) }.getType())
    }

    @Test
    fun identity() {
        val m = Matrix()
        assertEquals(1.0, m.a)
        assertEquals(0.0, m.b)
        assertEquals(0.0, m.c)
        assertEquals(1.0, m.d)
        assertEquals(0.0, m.tx)
        assertEquals(0.0, m.ty)
        m.setTo(2, 2, 2, 2, 2, 2)
        assertEquals(Matrix(2, 2, 2, 2, 2, 2), m)
        m.identity()
        assertEquals(identity, m)
    }

    @Test
    fun invert() {
        val a = Matrix(2, 1, 1, 2, 10, 10)
        a.invert()
        assertEquals(Matrix(a = 0.6666666666666666, b = -0.3333333333333333, c = -0.3333333333333333, d = 0.6666666666666666, tx = -3.333333333333333, ty = -3.333333333333333), a)
    }

    @Test
    fun inverted() {
        val a = Matrix(2, 1, 1, 2, 10, 10)
        val b = a.inverted()
        assertEquals(identity, a * b)
    }

    @Test
    fun clone() {
        val mat = Matrix(1, 2, 3, 4, 5, 6)
        assertNotSame(mat, mat.clone())
        assertTrue(mat !== mat.clone())
        assertEquals(mat, mat.clone())
    }

    @Test
    fun keep() {
        val m = Matrix()
        m.keep {
            m.setTo(2, 3, 4, 5, 6, 7)
            assertEquals(Matrix(2, 3, 4, 5, 6, 7), m)
        }
        assertEquals(identity, m)
    }

    @Test
    fun transform2() {
        assertEquals(Matrix(2, 0, 0, 3, 10, 20), Matrix.Transform(10, 20, scaleX = 2, scaleY = 3).toMatrix())

        // @TODO: Kotlin.JS BUG (missing arguments are NaN or undefined but it works fine on JVM)
        //val t1 = Matrix.Transform(10, 20, scaleX = 2, scaleY = 3, rotation = 90.degrees)
        //val t2 = Matrix.Transform(20, 40, scaleX = 4, scaleY = 5, rotation = 180.degrees)

        val t1 = Matrix.Transform(10, 20, scaleX = 2, scaleY = 3, skewX = 0.0, skewY = 0.0, rotation = 90.degrees)
        val t2 = Matrix.Transform(20, 40, scaleX = 4, scaleY = 5, skewX = 0.0, skewY = 0.0, rotation = 180.degrees)
        assertEquals(
            Matrix.Transform(x = 15.0, y = 30.0, scaleX = 3.0, scaleY = 4.0, skewX = 0.0, skewY = 0.0, rotation = 135.degrees),
            0.5.interpolate(t1, t2)
        )

        val identity = Matrix.Transform()
        val mt = Matrix.Transform(1, 2, 3, 4, 5, 6, 7.radians)
        mt.identity()
        assertEquals(identity, mt)
        assertNotSame(mt, mt.clone())
        assertEquals(mt, mt.clone())
    }
}
