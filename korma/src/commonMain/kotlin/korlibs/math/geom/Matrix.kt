@file:Suppress("NOTHING_TO_INLINE")

package korlibs.math.geom

import korlibs.memory.pack.*
import korlibs.math.annotations.*
import korlibs.math.internal.*
import korlibs.math.interpolation.*
import korlibs.math.math.*
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
inline class Matrix(val data: BFloat6Pack) {
    val a: Float get() = data.bf0
    val b: Float get() = data.bf1
    val c: Float get() = data.bf2
    val d: Float get() = data.bf3
    val tx: Float get() = data.bf4
    val ty: Float get() = data.bf5

    @Deprecated("", ReplaceWith("this")) val immutable: Matrix get() = this
    val mutable: MMatrix get() = MMatrix(a, b, c, d, tx, ty)
    val mutableOrNull: MMatrix? get() = if (isNIL) null else MMatrix(a, b, c, d, tx, ty)

    //constructor() : this(1f, 0f, 0f, 1f, 0f, 0f)
    constructor(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float) :
        this(bfloat6PackOf(a, b, c, d, tx, ty))
    constructor(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) :
        this(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), tx.toFloat(), ty.toFloat())
    constructor(a: Int, b: Int, c: Int, d: Int, tx: Int, ty: Int) :
        this(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), tx.toFloat(), ty.toFloat())

    fun copy(a: Float = this.a, b: Float = this.b, c: Float = this.c, d: Float = this.d, tx: Float = this.tx, ty: Float = this.ty): Matrix =
        Matrix(a, b, c, d, tx, ty)

    operator fun times(other: Matrix): Matrix = Matrix.multiply(this, other)
    operator fun times(scale: Float): Matrix = Matrix(a * scale, b * scale, c * scale, d * scale, tx * scale, ty * scale)
    operator fun times(scale: Double): Matrix = times(scale.toFloat())

    //val isNIL: Boolean get() = this == NIL
    val isNIL: Boolean get() = this.a.isNaN()
    val isNotNIL: Boolean get() = !isNIL
    val isNaN: Boolean get() = isNIL
    val isIdentity: Boolean get() = type == MatrixType.IDENTITY

    val type: MatrixType get() {
        val hasRotation = b != 0f || c != 0f
        val hasScale = a != 1f || d != 1f
        val hasTranslation = tx != 0f || ty != 0f

        return when {
            hasRotation -> MatrixType.COMPLEX
            hasScale && hasTranslation -> MatrixType.SCALE_TRANSLATE
            hasScale -> MatrixType.SCALE
            hasTranslation -> MatrixType.TRANSLATE
            else -> MatrixType.IDENTITY
        }
    }

    inline fun transform(p: Point): Point = Point(
        this.a * p.x + this.c * p.y + this.tx,
        this.d * p.y + this.b * p.x + this.ty
    )

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(p: Point): Float = transform(p).x
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(p: Point): Float = transform(p).y

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(x: Float, y: Float): Float = transform(Point(x, y)).x
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(x: Float, y: Float): Float = transform(Point(x, y)).y

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(x: Double, y: Double): Double = transform(Point(x, y)).xD
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(x: Double, y: Double): Double = transform(Point(x, y)).yD

    @Deprecated("", ReplaceWith("transform(p).x")) fun transformX(x: Int, y: Int): Double = transform(Point(x, y)).xD
    @Deprecated("", ReplaceWith("transform(p).y")) fun transformY(x: Int, y: Int): Double = transform(Point(x, y)).yD

    fun deltaTransform(p: Point): Point = Point((p.x * a) + (p.y * c), (p.x * b) + (p.y * d))

    fun rotated(angle: Angle): Matrix {
        val theta = angle.radians
        val cos = cos(theta)
        val sin = sin(theta)

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
        val sinX = sinf(skewX)
        val cosX = cosf(skewX)
        val sinY = sinf(skewY)
        val cosY = cosf(skewY)

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
        val m = this
        val norm = m.a * m.d - m.b * m.c

        return when (norm) {
            0f -> Matrix(0f, 0f, 0f, 0f, -m.tx, -m.ty)
            else -> {
                val inorm = 1f / norm
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

    fun toArray(value: FloatArray, offset: Int = 0) {
        value[offset + 0] = a
        value[offset + 1] = b
        value[offset + 2] = c
        value[offset + 3] = d
        value[offset + 4] = tx
        value[offset + 5] = ty
    }

    fun toArray(value: DoubleArray, offset: Int = 0) {
        value[offset + 0] = a.toDouble()
        value[offset + 1] = b.toDouble()
        value[offset + 2] = c.toDouble()
        value[offset + 3] = d.toDouble()
        value[offset + 4] = tx.toDouble()
        value[offset + 5] = ty.toDouble()
    }

    override fun toString(): String = "Matrix(${a.niceStr}, ${b.niceStr}, ${c.niceStr}, ${d.niceStr}, ${tx.niceStr}, ${ty.niceStr})"

    fun isAlmostEquals(other: Matrix, epsilon: Float = 0.001f): Boolean = isAlmostEquals(this, other, epsilon)

    // @TODO: Is this order correct?
    fun preconcated(other: Matrix): Matrix = this * other

    companion object {
        val IDENTITY = Matrix(1f, 0f, 0f, 1f, 0f, 0f)
        val NIL = Matrix(Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN)
        val NaN = NIL

        //@Deprecated("", ReplaceWith("korlibs.math.geom.Matrix.IDENTITY", "korlibs.math.geom.Matrix"))
        operator fun invoke(): Matrix = IDENTITY

        fun isAlmostEquals(a: Matrix, b: Matrix, epsilon: Float = 0.001f): Boolean =
            a.tx.isAlmostEquals(b.tx, epsilon)
                && a.ty.isAlmostEquals(b.ty, epsilon)
                && a.a.isAlmostEquals(b.a, epsilon)
                && a.b.isAlmostEquals(b.b, epsilon)
                && a.c.isAlmostEquals(b.c, epsilon)
                && a.d.isAlmostEquals(b.d, epsilon)

        fun multiply(l: Matrix, r: Matrix): Matrix = Matrix(
            l.a * r.a + l.b * r.c,
            l.a * r.b + l.b * r.d,
            l.c * r.a + l.d * r.c,
            l.c * r.b + l.d * r.d,
            l.tx * r.a + l.ty * r.c + r.tx,
            l.tx * r.b + l.ty * r.d + r.ty
        )

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
            pivotX: Float = 0f,
            pivotY: Float = 0f,
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
            x: Float,
            y: Float,
            rotation: Angle,
            scaleX: Float,
            scaleY: Float,
            skewX: Angle,
            skewY: Angle,
            pivotX: Float = 0f,
            pivotY: Float = 0f,
        ): Matrix {
            // +0.0 drops the negative -0.0
            val a = cosf(rotation + skewY) * scaleX + 0f
            val b = sinf(rotation + skewY) * scaleX + 0f
            val c = -sinf(rotation - skewX) * scaleY + 0f
            val d = cosf(rotation - skewX) * scaleY + 0f
            val tx: Float
            val ty: Float

            if (pivotX == 0f && pivotY == 0f) {
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
inline class MatrixTransform(
    val raw: BFloat3Half4Pack
    //val x: Float = 0f, val y: Float = 0f,
    //val scaleX: Float = 1f, val scaleY: Float = 1f,
    //val skewX: Angle = Angle.ZERO, val skewY: Angle = Angle.ZERO,
    //val rotation: Angle = Angle.ZERO
) {
    val x: Float get() = raw.b0
    val y: Float get() = raw.b1
    val rotation: Angle get() = Angle.fromRatio(raw.b2)
    val scaleX: Float get() = raw.hf0
    val scaleY: Float get() = raw.hf1
    val skewX: Angle get() = Angle.fromRatio(raw.hf2)
    val skewY: Angle get() = Angle.fromRatio(raw.hf3)
    val scale: Scale get() = Scale(scaleX, scaleY)

    override fun toString(): String = "MatrixTransform(x=${x.niceStr}, y=${y.niceStr}, scaleX=${scaleX}, scaleY=${scaleY}, skewX=${skewX}, skewY=${skewY}, rotation=${rotation})"

    constructor(
        x: Float = 0f, y: Float = 0f,
        scaleX: Float = 1f, scaleY: Float = 1f,
        skewX: Angle = Angle.ZERO, skewY: Angle = Angle.ZERO,
        rotation: Angle = Angle.ZERO
    ) : this(bfloat3Half4PackOf(x, y, rotation.ratioF, scaleX, scaleY, skewX.ratioF, skewY.ratioF))
    constructor() : this(0f, 0f, 1f, 1f, Angle.ZERO, Angle.ZERO, Angle.ZERO)
    constructor(
        x: Double, y: Double,
        scaleX: Double, scaleY: Double,
        skewX: Angle, skewY: Angle,
        rotation: Angle
    ) : this(x.toFloat(), y.toFloat(), scaleX.toFloat(), scaleY.toFloat(), skewX, skewY, rotation)

    companion object {
        val IDENTITY = MatrixTransform(0f, 0f, 1f, 1f, Angle.ZERO, Angle.ZERO, Angle.ZERO)

        fun fromMatrix(matrix: Matrix, pivotX: Float = 0f, pivotY: Float = 0f): MatrixTransform {
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
            val tx: Float
            val ty: Float

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

            if (pivotX == 0f && pivotY == 0f) {
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

        fun isAlmostEquals(a: MatrixTransform, b: MatrixTransform, epsilon: Float = 0.0001f): Boolean =
            a.x.isAlmostEquals(b.x, epsilon)
                && a.y.isAlmostEquals(b.y, epsilon)
                && a.scaleX.isAlmostEquals(b.scaleX, epsilon)
                && a.scaleY.isAlmostEquals(b.scaleY, epsilon)
                && a.skewX.isAlmostEquals(b.skewX, epsilon)
                && a.skewY.isAlmostEquals(b.skewY, epsilon)
                && a.rotation.isAlmostEquals(b.rotation, epsilon)
    }

    fun isAlmostEquals(other: MatrixTransform, epsilon: Float = 0.01f): Boolean = isAlmostEquals(this, other, epsilon)

    val scaleAvg: Float get() = (scaleX + scaleY) * 0.5f

    fun toMatrix(pivotX: Float = 0f, pivotY: Float = 0f): Matrix = Matrix.fromTransform(this, pivotX, pivotY)

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

@KormaValueApi
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
    val tx = if (delta) 0f else this.tx
    val ty = if (delta) 0f else this.ty

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

    var tmp = 0f

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
