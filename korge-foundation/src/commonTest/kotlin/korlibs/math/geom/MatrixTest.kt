package korlibs.math.geom

import korlibs.math.interpolation.*
import kotlin.test.*

class MatrixTest {
    private val identity: Matrix = Matrix(1, 0, 0, 1, 0, 0)

    @Test
    fun test() {
        val matrix = Matrix.IDENTITY
            .pretranslated(Point(10, 10))
            .prescaled(Scale(2, 3))
            .prerotated(90.degrees)
        assertEquals(Vector2I(10, 40), matrix.transform(Point(10, 0)).toIntRound())
    }


    @Test
    fun transform() {
        val matrix = Matrix(2, 0, 0, 3, 10, 15)
        assertEqualsFloat(Point(30.0, 75.0), matrix.transform(Point(10, 20)), absoluteTolerance = 0.01)
        assertEqualsFloat(Point(20.0, 60.0), matrix.deltaTransformPoint(Point(10, 20)), absoluteTolerance = 0.01)
    }

    @Test
    fun type() {
        assertEquals(MatrixType.IDENTITY, Matrix(1, 0, 0, 1, 0, 0).type)
        assertEquals(MatrixType.TRANSLATE, Matrix(1, 0, 0, 1, 10, 0).type)
        assertEquals(MatrixType.TRANSLATE, Matrix(1, 0, 0, 1, 0, 10).type)
        assertEquals(MatrixType.SCALE, Matrix(1, 0, 0, 2, 0, 0).type)
        assertEquals(MatrixType.SCALE, Matrix(2, 0, 0, 1, 0, 0).type)
        assertEquals(MatrixType.SCALE_TRANSLATE, Matrix(2, 0, 0, 2, 10, 0).type)
        assertEquals(MatrixType.COMPLEX, Matrix(1, 1, 0, 1, 0, 0).type)

        assertEquals(MatrixType.IDENTITY, Matrix.IDENTITY.type)
        assertEquals(MatrixType.SCALE, Matrix.IDENTITY.scaled(Scale(2, 1)).type)
        assertEquals(MatrixType.SCALE, Matrix.IDENTITY.scaled(Scale(1, 2)).type)
        assertEquals(MatrixType.TRANSLATE, Matrix.IDENTITY.translated(Point(1, 0)).type)
        assertEquals(MatrixType.TRANSLATE, Matrix.IDENTITY.translated(Point(0, 1)).type)
        assertEquals(MatrixType.SCALE_TRANSLATE, Matrix.IDENTITY.scaled(Scale(2, 1)).translated(Point(0, 1)).type)
        assertEquals(MatrixType.COMPLEX, Matrix.IDENTITY.rotated(90.degrees).type)
        assertEquals(Matrix(), Matrix.IDENTITY)
    }

    @Test
    fun identity() {
        var m = Matrix.IDENTITY
        assertEquals(1.0, m.a)
        assertEquals(0.0, m.b)
        assertEquals(0.0, m.c)
        assertEquals(1.0, m.d)
        assertEquals(0.0, m.tx)
        assertEquals(0.0, m.ty)
        m = Matrix(2, 2, 2, 2, 2, 2)
        assertEquals(Matrix(2, 2, 2, 2, 2, 2), m)
        m = Matrix.IDENTITY
        assertEquals(identity, m)
    }

    @Test
    fun invert() {
        val a = Matrix(2, 1, 1, 2, 10, 10).inverted()
        assertEqualsFloat(
            Matrix(a = 0.6666666666666666, b = -0.3333333333333333, c = -0.3333333333333333, d = 0.6666666666666666, tx = -3.333333333333333, ty = -3.333333333333333),
            a
        )
    }

    @Test
    fun inverted() {
        val a = Matrix(2, 1, 1, 2, 10, 10)
        val b = a.inverted()
        assertEqualsFloat(identity, a * b)
    }

    @Test
    fun transform2() {
        assertEquals(Matrix(2, 0, 0, 3, 10, 20), MatrixTransform(10.0, 20.0, scaleX = 2.0, scaleY = 3.0).toMatrix())

        // @TODO: Kotlin.JS BUG (missing arguments are NaN or undefined but it works fine on JVM)
        //val t1 = Matrix.Transform(10, 20, scaleX = 2, scaleY = 3, rotation = 90.degrees)
        //val t2 = Matrix.Transform(20, 40, scaleX = 4, scaleY = 5, rotation = 180.degrees)

        val t1 = MatrixTransform(10.0, 20.0, scaleX = 2.0, scaleY = 3.0, skewX = 0.0.degrees, skewY = 0.0.degrees, rotation = 90.degrees)
        val t2 = MatrixTransform(20.0, 40.0, scaleX = 4.0, scaleY = 5.0, skewX = 0.0.degrees, skewY = 0.0.degrees, rotation = 180.degrees)
        assertEquals(
            MatrixTransform(x = 15.0, y = 30.0, scaleX = 3.0, scaleY = 4.0, skewX = 0.0.degrees, skewY = 0.0.degrees, rotation = 135.degrees),
            Ratio.HALF.interpolate(t1, t2)
        )
    }

    @Test
    fun transform3() {
        val t = MatrixTransform(rotation = -(91.degrees), scaleX = 1.3, scaleY = 1.3)
        assertEqualsFloat(t, MatrixTransform.fromMatrix(t.toMatrix()), 0.1)
    }

    //@Test
    //@Ignore
    //fun benchmark() {
    //    var res = 0f
    //    for (n in 0 until 1_000_000) {
    //        val matrix = Matrix().scaled(Scale(2f * n)).translated(Point(100 * n, 100 + n)).rotated((45 + n).degrees)
    //        res += matrix.a + matrix.b + matrix.c + matrix.d + matrix.tx + matrix.ty
    //    }
    //    println(res)
    //}
}
