package korlibs.math.geom

import korlibs.math.*
import korlibs.math.interpolation.*
import korlibs.number.*
import kotlin.math.*


//@KormaValueApi
//data class Matrix(
//    val a: Float,
//    val b: Float,
//    val c: Float,
//    val d: Float,
//    val tx: Float,
//    val ty: Float,
//) {

// a, b, c, d, tx and ty are BFloat21
data class Matrix(
    val a: Double, val b: Double, val c: Double, val d: Double,
    val tx: Double = 0.0, val ty: Double = 0.0
) : IsAlmostEquals<Matrix> {
    //private val twobits: Int get() = data.twobits

    //constructor() : this(1f, 0f, 0f, 1f, 0f, 0f)
    constructor(a: Float, b: Float, c: Float, d: Float, tx: Float = 0f, ty: Float = 0f) :
        this(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())
    constructor(a: Int, b: Int, c: Int, d: Int, tx: Int = 0, ty: Int = 0) :
        this(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

    operator fun times(other: Matrix): Matrix = Matrix.multiply(this, other)
    operator fun times(scale: Double): Matrix = Matrix(a * scale, b * scale, c * scale, d * scale, tx * scale, ty * scale)
    operator fun times(scale: Float): Matrix = times(scale.toDouble())

    //val isNIL: Boolean get() = this == NIL
    val isNIL: Boolean get() = this.a.isNaN()
    val isNotNIL: Boolean get() = !isNIL
    val isNaN: Boolean get() = isNIL
    val isIdentity: Boolean get() = (a == 1.0 && b == 0.0 && c == 0.0 && d == 1.0 && tx == 0.0 && ty == 0.0)
    //val isIdentity: Boolean get() = twobits == 1

    val type: MatrixType get() {
        val hasRotation = b != 0.0 || c != 0.0
        val hasScale = a != 1.0 || d != 1.0
        val hasTranslation = tx != 0.0 || ty != 0.0

        return when {
            hasRotation -> MatrixType.COMPLEX
            hasScale && hasTranslation -> MatrixType.SCALE_TRANSLATE
            hasScale -> MatrixType.SCALE
            hasTranslation -> MatrixType.TRANSLATE
            else -> MatrixType.IDENTITY
        }
    }

    inline fun transform(p: Vector2F): Vector2F {
        if (this.isNIL) return p
        return Vector2F(
            this.a * p.x + this.c * p.y + this.tx,
            this.d * p.y + this.b * p.x + this.ty
        )
    }
    inline fun transform(p: Vector2D): Vector2D {
        if (this.isNIL) return p
        return Vector2D(
            transformX(p.x, p.y),
            transformY(p.x, p.y),
        )
    }

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(p: Point): Double = transformX(p.x, p.y)
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(p: Point): Double = transformY(p.x, p.y)

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(x: Float, y: Float): Float = transformX(x.toDouble(), y.toDouble()).toFloat()
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(x: Float, y: Float): Float = transformY(x.toDouble(), y.toDouble()).toFloat()

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(x: Double, y: Double): Double = this.a * x + this.c * y + this.tx
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(x: Double, y: Double): Double = this.d * y + this.b * x + this.ty

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(x: Int, y: Int): Double = transformX(x.toDouble(), y.toDouble())
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(x: Int, y: Int): Double = transformY(x.toDouble(), y.toDouble())

    fun deltaTransform(p: Vector2F): Vector2F = Vector2F((p.x * a) + (p.y * c), (p.x * b) + (p.y * d))
    fun deltaTransform(p: Vector2D): Vector2D = Vector2D((p.x * a) + (p.y * c), (p.x * b) + (p.y * d))

    fun rotated(angle: Angle): Matrix {
        val cos = cos(angle)
        val sin = sin(angle)

        val a1 = this.a * cos - this.b * sin
        val b = (this.a * sin + this.b * cos)
        val a = a1

        val c1 = this.c * cos - this.d * sin
        val d = (this.c * sin + this.d * cos)
        val c = c1

        val tx1 = this.tx * cos - this.ty * sin
        val ty = (this.tx * sin + this.ty * cos)
        val tx = tx1

        return Matrix(a, b, c, d, tx, ty)
    }

    fun skewed(skewX: Angle, skewY: Angle): Matrix {
        val sinX = sin(skewX)
        val cosX = cos(skewX)
        val sinY = sin(skewY)
        val cosY = cos(skewY)

        return Matrix(
            a * cosY - b * sinX,
            a * sinY + b * cosX,
            c * cosY - d * sinX,
            c * sinY + d * cosX,
            tx * cosY - ty * sinX,
            tx * sinY + ty * cosX
        )
    }

    fun scaled(scale: Scale): Matrix = Matrix(a * scale.scaleX, b * scale.scaleX, c * scale.scaleY, d * scale.scaleY, tx * scale.scaleX, ty * scale.scaleY)
    fun scaled(scaleX: Int, scaleY: Int = scaleX): Matrix = scaled(Scale(scaleX, scaleY))
    fun scaled(scaleX: Float, scaleY: Float = scaleX): Matrix = scaled(Scale(scaleX, scaleY))
    fun scaled(scaleX: Double, scaleY: Double = scaleX): Matrix = scaled(Scale(scaleX, scaleY))

    fun prescaled(scale: Scale): Matrix = Matrix(a * scale.scaleX, b * scale.scaleX, c * scale.scaleY, d * scale.scaleY, tx, ty)
    fun prescaled(scaleX: Int, scaleY: Int = scaleX): Matrix = prescaled(Scale(scaleX, scaleY))
    fun prescaled(scaleX: Float, scaleY: Float = scaleX): Matrix = prescaled(Scale(scaleX, scaleY))
    fun prescaled(scaleX: Double, scaleY: Double = scaleX): Matrix = prescaled(Scale(scaleX, scaleY))

    fun translated(delta: Point): Matrix = Matrix(a, b, c, d, tx + delta.x, ty + delta.y)
    fun translated(x: Int, y: Int): Matrix = translated(Point(x, y))
    fun translated(x: Float, y: Float): Matrix = translated(Point(x, y))
    fun translated(x: Double, y: Double): Matrix = translated(Point(x, y))

    fun pretranslated(delta: Point): Matrix = Matrix(a, b, c, d, tx + (a * delta.x + c * delta.y), ty + (b * delta.x + d * delta.y))
    fun pretranslated(deltaX: Int, deltaY: Int): Matrix = pretranslated(Point(deltaX, deltaY))
    fun pretranslated(deltaX: Float, deltaY: Float): Matrix = pretranslated(Point(deltaX, deltaY))
    fun pretranslated(deltaX: Double, deltaY: Double): Matrix = pretranslated(Point(deltaX, deltaY))

    fun prerotated(angle: Angle): Matrix = rotating(angle) * this
    fun preskewed(skewX: Angle, skewY: Angle): Matrix = skewing(skewX, skewY) * this

    fun premultiplied(m: Matrix): Matrix = m * this
    fun multiplied(m: Matrix): Matrix = this * m

    /** Transform point without translation */
    fun deltaTransformPoint(p: Point): Point = Point((p.x * a) + (p.y * c), (p.x * b) + (p.y * d))

    @Deprecated("", ReplaceWith("this")) fun clone(): Matrix = this

    fun inverted(): Matrix {
        if (this.isNIL) return Matrix.IDENTITY
        val m = this
        val norm = m.a * m.d - m.b * m.c

        return when (norm) {
            0.0 -> Matrix(0.0, 0.0, 0.0, 0.0, -m.tx, -m.ty)
            else -> {
                val inorm = 1.0 / norm
                val d = m.a * inorm
                val a = m.d * inorm
                val b = m.b * -inorm
                val c = m.c * -inorm
                Matrix(a, b, c, d, -a * m.tx - c * m.ty, -b * m.tx - d * m.ty)
            }
        }
    }

    fun toTransform(): MatrixTransform = decompose()
    fun decompose(): MatrixTransform = MatrixTransform.fromMatrix(this)

    fun toArray(value: DoubleArray, offset: Int = 0) {
        value[offset + 0] = a
        value[offset + 1] = b
        value[offset + 2] = c
        value[offset + 3] = d
        value[offset + 4] = tx
        value[offset + 5] = ty
    }

    fun toArray(value: FloatArray, offset: Int = 0) {
        value[offset + 0] = a.toFloat()
        value[offset + 1] = b.toFloat()
        value[offset + 2] = c.toFloat()
        value[offset + 3] = d.toFloat()
        value[offset + 4] = tx.toFloat()
        value[offset + 5] = ty.toFloat()
    }

    override fun toString(): String = "Matrix(${a.niceStr}, ${b.niceStr}, ${c.niceStr}, ${d.niceStr}, ${tx.niceStr}, ${ty.niceStr})"

    override fun isAlmostEquals(other: Matrix, epsilon: Double): Boolean = isAlmostEquals(this, other, epsilon)
    fun isAlmostIdentity(epsilon: Double = 0.00001): Boolean = isAlmostEquals(this, IDENTITY, epsilon)

    // @TODO: Is this order correct?
    fun preconcated(other: Matrix): Matrix = this * other

    companion object {
        val IDENTITY = Matrix(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        val NIL = Matrix(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)
        val NaN = NIL

        //@Deprecated("", ReplaceWith("korlibs.math.geom.Matrix.IDENTITY", "korlibs.math.geom.Matrix"))
        operator fun invoke(): Matrix = IDENTITY

        fun isAlmostEquals(a: Matrix, b: Matrix, epsilon: Double = 0.00001): Boolean =
            a.tx.isAlmostEquals(b.tx, epsilon)
                && a.ty.isAlmostEquals(b.ty, epsilon)
                && a.a.isAlmostEquals(b.a, epsilon)
                && a.b.isAlmostEquals(b.b, epsilon)
                && a.c.isAlmostEquals(b.c, epsilon)
                && a.d.isAlmostEquals(b.d, epsilon)

        fun multiply(l: Matrix, r: Matrix): Matrix {
            if (l.isNIL) return r
            if (r.isNIL) return l
            return Matrix(
                l.a * r.a + l.b * r.c,
                l.a * r.b + l.b * r.d,
                l.c * r.a + l.d * r.c,
                l.c * r.b + l.d * r.d,
                l.tx * r.a + l.ty * r.c + r.tx,
                l.tx * r.b + l.ty * r.d + r.ty
            )
        }

        fun translating(delta: Point): Matrix = Matrix.IDENTITY.copy(tx = delta.x, ty = delta.y)
        fun rotating(angle: Angle): Matrix = Matrix.IDENTITY.rotated(angle)
        fun skewing(skewX: Angle, skewY: Angle): Matrix = Matrix.IDENTITY.skewed(skewX, skewY)

        fun fromArray(value: FloatArray, offset: Int = 0): Matrix = Matrix(
            value[offset + 0], value[offset + 1], value[offset + 2],
            value[offset + 3], value[offset + 4], value[offset + 5]
        )

        fun fromArray(value: DoubleArray, offset: Int = 0): Matrix = Matrix(
            value[offset + 0], value[offset + 1], value[offset + 2],
            value[offset + 3], value[offset + 4], value[offset + 5]
        )

        fun fromTransform(
            transform: MatrixTransform,
            pivotX: Double = 0.0,
            pivotY: Double = 0.0,
        ): Matrix = fromTransform(
            transform.x,
            transform.y,
            transform.rotation,
            transform.scaleX,
            transform.scaleY,
            transform.skewX,
            transform.skewY,
            pivotX,
            pivotY,
        )

        fun fromTransform(
            x: Double,
            y: Double,
            rotation: Angle = Angle.ZERO,
            scaleX: Double = 1.0,
            scaleY: Double = 1.0,
            skewX: Angle = Angle.ZERO,
            skewY: Angle = Angle.ZERO,
            pivotX: Double = 0.0,
            pivotY: Double = 0.0,
        ): Matrix {
            // +0.0 drops the negative -0.0
            val a = cos(rotation + skewY) * scaleX + 0f
            val b = sin(rotation + skewY) * scaleX + 0f
            val c = -sin(rotation - skewX) * scaleY + 0f
            val d = cos(rotation - skewX) * scaleY + 0f
            val tx: Double
            val ty: Double

            if (pivotX == 0.0 && pivotY == 0.0) {
                tx = x
                ty = y
            } else {
                tx = x - ((pivotX * a) + (pivotY * c))
                ty = y - ((pivotX * b) + (pivotY * d))
            }
            return Matrix(a, b, c, d, tx, ty)
        }

        fun transform(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, p: Point): Point = Point(
            a * p.x + c * p.y + tx,
            d * p.y + b * p.x + ty
        )

        fun interpolated(l: Matrix, r: Matrix, ratio: Ratio): Matrix = Matrix(
            ratio.interpolate(l.a, r.a),
            ratio.interpolate(l.b, r.b),
            ratio.interpolate(l.c, r.c),
            ratio.interpolate(l.d, r.d),
            ratio.interpolate(l.tx, r.tx),
            ratio.interpolate(l.ty, r.ty),
        )
    }
}

//@KormaValueApi
data class MatrixTransform(
    val x: Double = 0.0, val y: Double = 0.0,
    val scaleX: Double = 1.0, val scaleY: Double = 1.0,
    val skewX: Angle = Angle.ZERO, val skewY: Angle = Angle.ZERO,
    val rotation: Angle = Angle.ZERO
) : IsAlmostEquals<MatrixTransform> {
    val scale: Scale get() = Scale(scaleX, scaleY)

    override fun toString(): String = "MatrixTransform(x=${x.niceStr}, y=${y.niceStr}, scaleX=${scaleX}, scaleY=${scaleY}, skewX=${skewX}, skewY=${skewY}, rotation=${rotation})"

    constructor() : this(0.0, 0.0, 1.0, 1.0, Angle.ZERO, Angle.ZERO, Angle.ZERO)
    constructor(
        x: Float, y: Float,
        scaleX: Float, scaleY: Float,
        skewX: Angle, skewY: Angle,
        rotation: Angle
    ) : this(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), skewX, skewY, rotation)

    companion object {
        val IDENTITY = MatrixTransform(0.0, 0.0, 1.0, 1.0, Angle.ZERO, Angle.ZERO, Angle.ZERO)

        fun fromMatrix(matrix: Matrix, pivotX: Double = 0.0, pivotY: Double = 0.0): MatrixTransform {
            val a = matrix.a
            val b = matrix.b
            val c = matrix.c
            val d = matrix.d

            val skewX = -atan2(-c, d)
            val skewY = atan2(b, a)

            val delta = abs(skewX + skewY)

            val trotation: Angle
            val tskewX: Angle
            val tskewY: Angle
            val tx: Double
            val ty: Double

            if (delta < 0.001f || abs((PI * 2) - delta) < 0.001f) {
                trotation = skewY.radians
                tskewX = 0.0.radians
                tskewY = 0.0.radians
            } else {
                trotation = 0.radians
                tskewX = skewX.radians
                tskewY = skewY.radians
            }

            val tscaleX = hypot(a, b)
            val tscaleY = hypot(c, d)

            if (pivotX == 0.0 && pivotY == 0.0) {
                tx = matrix.tx
                ty = matrix.ty
            } else {
                tx = matrix.tx + ((pivotX * a) + (pivotY * c));
                ty = matrix.ty + ((pivotX * b) + (pivotY * d));
            }
            return MatrixTransform(tx, ty, tscaleX, tscaleY, tskewX, tskewY, trotation)
        }

        fun interpolated(l: MatrixTransform, r: MatrixTransform, ratio: Ratio): MatrixTransform = MatrixTransform(
            ratio.toRatio().interpolate(l.x, r.x),
            ratio.toRatio().interpolate(l.y, r.y),
            ratio.toRatio().interpolate(l.scaleX, r.scaleX),
            ratio.toRatio().interpolate(l.scaleY, r.scaleY),
            ratio.toRatio().interpolateAngleDenormalized(l.skewX, r.skewX),
            ratio.toRatio().interpolateAngleDenormalized(l.skewY, r.skewY),
            ratio.toRatio().interpolateAngleDenormalized(l.rotation, r.rotation),
        )

        fun isAlmostEquals(a: MatrixTransform, b: MatrixTransform, epsilon: Double = 0.000001): Boolean =
            a.x.isAlmostEquals(b.x, epsilon)
                && a.y.isAlmostEquals(b.y, epsilon)
                && a.scaleX.isAlmostEquals(b.scaleX, epsilon)
                && a.scaleY.isAlmostEquals(b.scaleY, epsilon)
                && a.skewX.isAlmostEquals(b.skewX, epsilon)
                && a.skewY.isAlmostEquals(b.skewY, epsilon)
                && a.rotation.isAlmostEquals(b.rotation, epsilon)
    }

    override fun isAlmostEquals(other: MatrixTransform, epsilon: Double): Boolean = isAlmostEquals(this, other, epsilon)

    val scaleAvg: Double get() = (scaleX + scaleY) * 0.5

    fun toMatrix(pivotX: Double = 0.0, pivotY: Double = 0.0): Matrix = Matrix.fromTransform(this, pivotX, pivotY)

    operator fun plus(that: MatrixTransform): MatrixTransform = MatrixTransform(
        x + that.x, y + that.y,
        scaleX * that.scaleX, scaleY * that.scaleY,
        skewX + that.skewX, skewY + that.skewY,
        rotation + that.rotation,
    )
    operator fun minus(that: MatrixTransform): MatrixTransform = MatrixTransform(
        x - that.x, y - that.y,
        scaleX / that.scaleX, scaleY / that.scaleY,
        skewX - that.skewX, skewY - that.skewY,
        rotation - that.rotation,
    )
}

class MatrixComputed(val matrix: Matrix, val transform: MatrixTransform) {
    companion object;
    constructor(matrix: Matrix) : this(matrix, MatrixTransform.fromMatrix(matrix))
    constructor(transform: MatrixTransform) : this(transform.toMatrix(), transform)
}

enum class MatrixType(val id: Int, val hasRotation: Boolean, val hasScale: Boolean, val hasTranslation: Boolean) {
    IDENTITY(1, hasRotation = false, hasScale = false, hasTranslation = false),
    TRANSLATE(2, hasRotation = false, hasScale = false, hasTranslation = true),
    SCALE(3, hasRotation = false, hasScale = true, hasTranslation = false),
    SCALE_TRANSLATE(4, hasRotation = false, hasScale = true, hasTranslation = true),
    COMPLEX(5, hasRotation = true, hasScale = true, hasTranslation = true);
}

@Suppress("DuplicatedCode")
fun Matrix.transformRectangle(rectangle: Rectangle, delta: Boolean = false): Rectangle {
    val a = this.a
    val b = this.b
    val c = this.c
    val d = this.d
    val tx = if (delta) 0.0 else this.tx
    val ty = if (delta) 0.0 else this.ty

    val x = rectangle.x
    val y = rectangle.y
    val xMax = x + rectangle.width
    val yMax = y + rectangle.height

    var x0 = a * x + c * y + tx
    var y0 = b * x + d * y + ty
    var x1 = a * xMax + c * y + tx
    var y1 = b * xMax + d * y + ty
    var x2 = a * xMax + c * yMax + tx
    var y2 = b * xMax + d * yMax + ty
    var x3 = a * x + c * yMax + tx
    var y3 = b * x + d * yMax + ty

    var tmp = 0.0

    if (x0 > x1) {
        tmp = x0
        x0 = x1
        x1 = tmp
    }
    if (x2 > x3) {
        tmp = x2
        x2 = x3
        x3 = tmp
    }

    val rx = floor(if (x0 < x2) x0 else x2)
    val rw = ceil((if (x1 > x3) x1 else x3) - rectangle.x)

    if (y0 > y1) {
        tmp = y0
        y0 = y1
        y1 = tmp
    }
    if (y2 > y3) {
        tmp = y2
        y2 = y3
        y3 = tmp
    }

    val ry = floor(if (y0 < y2) y0 else y2)
    val rh = ceil((if (y1 > y3) y1 else y3) - rectangle.y)

    return Rectangle(rx, ry, rw, rh)
}
