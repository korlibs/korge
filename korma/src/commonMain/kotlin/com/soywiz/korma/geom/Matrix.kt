@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.*

@KormaValueApi
data class Matrix(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val tx: Float,
    val ty: Float,
) {
    constructor() : this(1f, 0f, 0f, 1f, 0f, 0f)
    constructor(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double) :
        this(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), tx.toFloat(), ty.toFloat())

    operator fun times(other: Matrix): Matrix = Matrix.multiply(this, other)
    operator fun times(scale: Float): Matrix = Matrix(a * scale, b * scale, c * scale, d * scale, tx * scale, ty * scale)
    operator fun times(scale: Double): Matrix = times(scale.toFloat())

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
    fun prescaled(scale: Scale): Matrix = Matrix(a * scale.scaleX, b * scale.scaleX, c * scale.scaleY, d * scale.scaleY, tx, ty)

    fun translated(delta: Point): Matrix = Matrix(a, b, c, d, tx + delta.x, ty + delta.y)
    fun pretranslated(delta: Point): Matrix = Matrix(a, b, c, d, tx + (a * delta.x + c * delta.y), ty + (b * delta.x + d * delta.y))

    fun prerotated(angle: Angle): Matrix = rotating(angle) * this
    fun preskewed(skewX: Angle, skewY: Angle): Matrix = skewing(skewX, skewY) * this

    fun premultipled(m: Matrix): Matrix = m * this
    fun multipled(m: Matrix): Matrix = this * m

    /** Transform point without translation */
    fun deltaTransformPoint(p: Point): Point = Point((p.x * a) + (p.y * c), (p.x * b) + (p.y * d))

    fun inverted(m: Matrix): Matrix {
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

    companion object {
        val IDENTITY = Matrix()
        val NaN = Matrix(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)

        fun isAlmostEquals(a: Matrix, b: Matrix, epsilon: Float = 0.000001f): Boolean =
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

        fun translating(delta: Point): Matrix = Matrix().copy(tx = delta.x, ty = delta.y)
        fun rotating(angle: Angle): Matrix = Matrix().rotated(angle)
        fun skewing(skewX: Angle, skewY: Angle): Matrix = Matrix().skewed(skewX, skewY)

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
        ): Matrix {
            // +0.0 drops the negative -0.0
            val a = cosf(transform.rotation + transform.skewY) * transform.scaleX + 0f
            val b = sinf(transform.rotation + transform.skewY) * transform.scaleX + 0f
            val c = -sinf(transform.rotation - transform.skewX) * transform.scaleY + 0f
            val d = cosf(transform.rotation - transform.skewX) * transform.scaleY + 0f
            val tx: Float
            val ty: Float

            if (pivotX == 0f && pivotY == 0f) {
                tx = transform.x
                ty = transform.y
            } else {
                tx = transform.x - ((pivotX * a) + (pivotY * c))
                ty = transform.y - ((pivotX * b) + (pivotY * d))
            }
            return Matrix(a, b, c, d, tx, ty)
        }
    }
}

@KormaValueApi
data class MatrixTransform(
    val x: Float, val y: Float,
    val scaleX: Float, val scaleY: Float,
    val skewX: Angle, val skewY: Angle,
    val rotation: Angle
) {
    constructor() : this(0.0, 0.0, 1.0, 1.0, Angle.ZERO, Angle.ZERO, Angle.ZERO)
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

            if (delta < 0.00001 || abs((PI * 2) - delta) < 0.00001) {
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

        fun interpolated(l: MatrixTransform, r: MatrixTransform, ratio: Double): MatrixTransform = MatrixTransform(
            ratio.toRatio().interpolate(l.x, r.x),
            ratio.toRatio().interpolate(l.y, r.y),
            ratio.toRatio().interpolate(l.scaleX, r.scaleX),
            ratio.toRatio().interpolate(l.scaleY, r.scaleY),
            ratio.toRatio().interpolateAngleDenormalized(l.rotation, r.rotation),
            ratio.toRatio().interpolateAngleDenormalized(l.skewX, r.skewX),
            ratio.toRatio().interpolateAngleDenormalized(l.skewY, r.skewY)
        )
    }


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

@KormaMutableApi
sealed interface IMatrix {
    val a: Double
    val b: Double
    val c: Double
    val d: Double
    val tx: Double
    val ty: Double

    // Transform points
    fun transform(p: Point): Point = Point(transformX(p.x, p.y), transformY(p.x, p.y))
    fun transform(p: IPoint, out: MPoint = MPoint()): MPoint = transform(p.x, p.y, out)
    fun transform(px: Double, py: Double, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))
    fun transform(px: Float, py: Float, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))
    fun transform(px: Int, py: Int, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))

    fun transformX(p: IPoint): Double = transformX(p.x, p.y)
    fun transformX(px: Double, py: Double): Double = this.a * px + this.c * py + this.tx
    fun transformX(px: Float, py: Float): Double = this.a * px + this.c * py + this.tx
    fun transformX(px: Int, py: Int): Double = this.a * px + this.c * py + this.tx

    fun transformY(p: IPoint): Double = transformY(p.x, p.y)
    fun transformY(px: Double, py: Double): Double = this.d * py + this.b * px + this.ty
    fun transformY(px: Float, py: Float): Double = this.d * py + this.b * px + this.ty
    fun transformY(px: Int, py: Int): Double = this.d * py + this.b * px + this.ty

    fun transformXf(p: IPoint): Float = transformX(p.x, p.y).toFloat()
    fun transformXf(px: Double, py: Double): Float = transformX(px, py).toFloat()
    fun transformXf(px: Float, py: Float): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
    fun transformXf(px: Int, py: Int): Float = transformX(px.toDouble(), py.toDouble()).toFloat()

    fun transformYf(p: IPoint): Float = transformY(p.x, p.y).toFloat()
    fun transformYf(px: Double, py: Double): Float = transformY(px, py).toFloat()
    fun transformYf(px: Float, py: Float): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
    fun transformYf(px: Int, py: Int): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
}

@KormaMutableApi
data class MMatrix(
    override var a: Double = 1.0,
    override var b: Double = 0.0,
    override var c: Double = 0.0,
    override var d: Double = 1.0,
    override var tx: Double = 0.0,
    override var ty: Double = 0.0
) : MutableInterpolable<MMatrix>, Interpolable<MMatrix>, IMatrix {
    companion object {
        val POOL: ConcurrentPool<MMatrix> = ConcurrentPool<MMatrix>({ it.identity() }) { MMatrix() }

        inline operator fun invoke(a: Float, b: Float = 0f, c: Float = 0f, d: Float = 1f, tx: Float = 0f, ty: Float = 0f) =
            MMatrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        inline operator fun invoke(a: Int, b: Int = 0, c: Int = 0, d: Int = 1, tx: Int = 0, ty: Int = 0) =
            MMatrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        operator fun invoke(m: MMatrix, out: MMatrix = MMatrix()): MMatrix = out.copyFrom(m)

        fun transformXf(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, px: Float, py: Float): Float = a * px + c * py + tx
        fun transformYf(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, px: Float, py: Float): Float = d * py + b * px + ty


        fun isAlmostEquals(a: IMatrix, b: IMatrix, epsilon: Double = 0.000001): Boolean =
            a.tx.isAlmostEquals(b.tx, epsilon)
                && a.ty.isAlmostEquals(b.ty, epsilon)
                && a.a.isAlmostEquals(b.a, epsilon)
                && a.b.isAlmostEquals(b.b, epsilon)
                && a.c.isAlmostEquals(b.c, epsilon)
                && a.d.isAlmostEquals(b.d, epsilon)
    }

    fun isAlmostEquals(other: IMatrix, epsilon: Double = 0.000001): Boolean = isAlmostEquals(this, other, epsilon)

    var af: Float
        get() = a.toFloat()
        set(value) { a = value.toDouble() }

    var bf: Float
        get() = b.toFloat()
        set(value) { b = value.toDouble() }

    var cf: Float
        get() = c.toFloat()
        set(value) { c = value.toDouble() }

    var df: Float
        get() = d.toFloat()
        set(value) { d = value.toDouble() }

    var txf: Float
        get() = tx.toFloat()
        set(value) { tx = value.toDouble() }

    var tyf: Float
        get() = ty.toFloat()
        set(value) { ty = value.toDouble() }

    fun getType(): MatrixType {
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

    fun setTo(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): MMatrix {
        this.a = a
        this.b = b
        this.c = c
        this.d = d
        this.tx = tx
        this.ty = ty
        return this

    }
    fun setTo(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float): MMatrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())
    fun setTo(a: Int, b: Int, c: Int, d: Int, tx: Int, ty: Int): MMatrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

    fun copyTo(that: MMatrix = MMatrix()): MMatrix {
        that.copyFrom(this)
        return that
    }

    fun copyFromInverted(that: MMatrix): MMatrix {
        return invert(that)
    }

    fun copyFrom(that: MMatrix?): MMatrix {
        if (that != null) {
            setTo(that.a, that.b, that.c, that.d, that.tx, that.ty)
        } else {
            identity()
        }
        return this
    }

    fun rotate(angle: Angle) = this.apply {
        val theta = angle.radians
        val cos = cos(theta)
        val sin = sin(theta)

        val a1 = a * cos - b * sin
        b = (a * sin + b * cos)
        a = a1

        val c1 = c * cos - d * sin
        d = (c * sin + d * cos)
        c = c1

        val tx1 = tx * cos - ty * sin
        ty = (tx * sin + ty * cos)
        tx = tx1
    }

    fun skew(skewX: Angle, skewY: Angle): MMatrix {
        val sinX = sind(skewX)
        val cosX = cosd(skewX)
        val sinY = sind(skewY)
        val cosY = cosd(skewY)

        return this.setTo(
            a * cosY - b * sinX,
            a * sinY + b * cosX,
            c * cosY - d * sinX,
            c * sinY + d * cosX,
            tx * cosY - ty * sinX,
            tx * sinY + ty * cosX
        )
    }

    fun setToMultiply(l: MMatrix?, r: MMatrix?): MMatrix {
        when {
            l != null && r != null -> multiply(l, r)
            l != null -> copyFrom(l)
            r != null -> copyFrom(r)
            else -> identity()
        }
        return this
    }

    fun scale(sx: Double, sy: Double = sx) = setTo(a * sx, b * sx, c * sy, d * sy, tx * sx, ty * sy)
    fun scale(sx: Float, sy: Float = sx) = scale(sx.toDouble(), sy.toDouble())
    fun scale(sx: Int, sy: Int = sx) = scale(sx.toDouble(), sy.toDouble())

    fun prescale(sx: Double, sy: Double = sx) = setTo(a * sx, b * sx, c * sy, d * sy, tx, ty)
    fun prescale(sx: Float, sy: Float = sx) = prescale(sx.toDouble(), sy.toDouble())
    fun prescale(sx: Int, sy: Int = sx) = prescale(sx.toDouble(), sy.toDouble())

    fun translate(dx: Double, dy: Double) = this.apply { this.tx += dx; this.ty += dy }
    fun translate(dx: Float, dy: Float) = translate(dx.toDouble(), dy.toDouble())
    fun translate(dx: Int, dy: Int) = translate(dx.toDouble(), dy.toDouble())

    fun pretranslate(dx: Double, dy: Double) = this.apply { tx += a * dx + c * dy; ty += b * dx + d * dy }
    fun pretranslate(dx: Float, dy: Float) = pretranslate(dx.toDouble(), dy.toDouble())
    fun pretranslate(dx: Int, dy: Int) = pretranslate(dx.toDouble(), dy.toDouble())

    fun prerotate(angle: Angle) = this.apply {
        val m = MMatrix()
        m.rotate(angle)
        this.premultiply(m)
    }

    fun preskew(skewX: Angle, skewY: Angle) = this.apply {
        val m = MMatrix()
        m.skew(skewX, skewY)
        this.premultiply(m)
    }

    fun premultiply(m: MMatrix) = this.premultiply(m.a, m.b, m.c, m.d, m.tx, m.ty)
    fun postmultiply(m: MMatrix) = multiply(this, m)

    fun premultiply(la: Double, lb: Double, lc: Double, ld: Double, ltx: Double, lty: Double): MMatrix = setTo(
        la * a + lb * c,
        la * b + lb * d,
        lc * a + ld * c,
        lc * b + ld * d,
        ltx * a + lty * c + tx,
        ltx * b + lty * d + ty
    )
    fun premultiply(la: Float, lb: Float, lc: Float, ld: Float, ltx: Float, lty: Float): MMatrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())
    fun premultiply(la: Int, lb: Int, lc: Int, ld: Int, ltx: Int, lty: Int): MMatrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())

    fun multiply(l: IMatrix, r: IMatrix): MMatrix = setTo(
        l.a * r.a + l.b * r.c,
        l.a * r.b + l.b * r.d,
        l.c * r.a + l.d * r.c,
        l.c * r.b + l.d * r.d,
        l.tx * r.a + l.ty * r.c + r.tx,
        l.tx * r.b + l.ty * r.d + r.ty
    )

    /** Transform point without translation */
    fun deltaTransformPoint(point: IPoint, out: MPoint = MPoint()) = deltaTransformPoint(point.x, point.y, out)
    fun deltaTransformPoint(x: Float, y: Float, out: MPoint = MPoint()): MPoint = deltaTransformPoint(x.toDouble(), y.toDouble(), out)
    fun deltaTransformPoint(x: Double, y: Double, out: MPoint = MPoint()): MPoint {
        out.x = deltaTransformX(x, y)
        out.y = deltaTransformY(x, y)
        return out
    }

    fun deltaTransformX(x: Double, y: Double): Double = (x * a) + (y * c)
    fun deltaTransformY(x: Double, y: Double): Double = (x * b) + (y * d)

    fun identity() = setTo(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    fun setToNan() = setTo(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN)

    fun isIdentity() = getType() == MatrixType.IDENTITY

    fun invert(matrixToInvert: IMatrix = this): MMatrix {
        val src = matrixToInvert
        val dst = this
        val norm = src.a * src.d - src.b * src.c

        if (norm == 0.0) {
            dst.setTo(0.0, 0.0, 0.0, 0.0, -src.tx, -src.ty)
        } else {
            val inorm = 1.0 / norm
            val d = src.a * inorm
            val a = src.d * inorm
            val b = src.b * -inorm
            val c = src.c * -inorm
            dst.setTo(a, b, c, d, -a * src.tx - c * src.ty, -b * src.tx - d * src.ty)
        }

        return this
    }

    fun concat(value: IMatrix): MMatrix = this.multiply(this, value)
    fun preconcat(value: IMatrix): MMatrix = this.multiply(this, value)
    fun postconcat(value: IMatrix): MMatrix = this.multiply(value, this)

    fun inverted(out: MMatrix = MMatrix()) = out.invert(this)

    fun setTransform(
        transform: Transform,
        pivotX: Double = 0.0,
        pivotY: Double = 0.0,
    ): MMatrix {
        return setTransform(
            transform.x, transform.y,
            transform.scaleX, transform.scaleY,
            transform.rotation, transform.skewX, transform.skewY,
            pivotX, pivotY
        )
    }

    fun setTransform(
        x: Double = 0.0,
        y: Double = 0.0,
        scaleX: Double = 1.0,
        scaleY: Double = 1.0,
        rotation: Angle = Angle.ZERO,
        skewX: Angle = Angle.ZERO,
        skewY: Angle = Angle.ZERO,
        pivotX: Double = 0.0,
        pivotY: Double = 0.0,
    ): MMatrix {
        // +0.0 drops the negative -0.0
        this.a = cosd(rotation + skewY) * scaleX + 0.0
        this.b = sind(rotation + skewY) * scaleX + 0.0
        this.c = -sind(rotation - skewX) * scaleY + 0.0
        this.d = cosd(rotation - skewX) * scaleY + 0.0

        if (pivotX == 0.0 && pivotY == 0.0) {
            this.tx = x
            this.ty = y
        } else {
            this.tx = x - ((pivotX * this.a) + (pivotY * this.c))
            this.ty = y - ((pivotX * this.b) + (pivotY * this.d))
        }
        return this
    }
    fun setTransform(x: Float = 0f, y: Float = 0f, scaleX: Float = 1f, scaleY: Float = 1f, rotation: Angle = Angle.ZERO, skewX: Angle = Angle.ZERO, skewY: Angle = Angle.ZERO): MMatrix =
        setTransform(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX, skewY)

    fun clone(): MMatrix = MMatrix(a, b, c, d, tx, ty)

    operator fun times(that: IMatrix): MMatrix = MMatrix().multiply(this, that)
    operator fun times(scale: Double): MMatrix = MMatrix().copyFrom(this).scale(scale)

    fun toTransform(out: Transform = Transform()): Transform {
        out.setMatrixNoReturn(this)
        return out
    }

    @Suppress("DuplicatedCode")
    fun transformRectangle(rectangle: MRectangle, delta: Boolean = false) {
        val a = this.af
        val b = this.bf
        val c = this.cf
        val d = this.df
        val tx = if (delta) 0f else this.txf
        val ty = if (delta) 0f else this.tyf

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

        rectangle.x = floor(if (x0 < x2) x0 else x2)
        rectangle.width = ceil((if (x1 > x3) x1 else x3) - rectangle.x)

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

        rectangle.y = floor(if (y0 < y2) y0 else y2)
        rectangle.height = ceil((if (y1 > y3) y1 else y3) - rectangle.y)
    }

    fun copyFromArray(value: FloatArray, offset: Int = 0): MMatrix = setTo(
        value[offset + 0], value[offset + 1], value[offset + 2],
        value[offset + 3], value[offset + 4], value[offset + 5]
    )

    fun copyFromArray(value: DoubleArray, offset: Int = 0): MMatrix = setTo(
        value[offset + 0].toFloat(), value[offset + 1].toFloat(), value[offset + 2].toFloat(),
        value[offset + 3].toFloat(), value[offset + 4].toFloat(), value[offset + 5].toFloat()
    )

    fun decompose(out: Transform = Transform()): Transform {
        return out.setMatrix(this)
    }

    data class Transform(
        var x: Double = 0.0, var y: Double = 0.0,
        var scaleX: Double = 1.0, var scaleY: Double = 1.0,
        var skewX: Angle = 0.radians, var skewY: Angle = 0.radians,
        var rotation: Angle = 0.radians
    ) : MutableInterpolable<Transform>, Interpolable<Transform> {
        val scale: Scale get() = Scale(scaleX, scaleY)

        var scaleAvg: Double
            get() = (scaleX + scaleY) * 0.5
            set(value) {
                scaleX = value
                scaleY = value
            }

        override fun interpolateWith(ratio: Ratio, other: Transform): Transform = Transform().setToInterpolated(ratio, this, other)

        override fun setToInterpolated(ratio: Ratio, l: Transform, r: Transform): Transform = this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.scaleX, r.scaleX),
            ratio.interpolate(l.scaleY, r.scaleY),
            ratio.interpolateAngleDenormalized(l.rotation, r.rotation),
            ratio.interpolateAngleDenormalized(l.skewX, r.skewX),
            ratio.interpolateAngleDenormalized(l.skewY, r.skewY)
        )

        fun identity() {
            x = 0.0
            y = 0.0
            scaleX = 1.0
            scaleY = 1.0
            skewX = 0.0.radians
            skewY = 0.0.radians
            rotation = 0.0.radians
        }

        fun setMatrixNoReturn(matrix: IMatrix, pivotX: Double = 0.0, pivotY: Double = 0.0) {
            val a = matrix.a
            val b = matrix.b
            val c = matrix.c
            val d = matrix.d

            val skewX = -atan2(-c, d)
            val skewY = atan2(b, a)

            val delta = abs(skewX + skewY)

            if (delta < 0.00001 || abs((PI * 2) - delta) < 0.00001) {
                this.rotation = skewY.radians
                this.skewX = 0.0.radians
                this.skewY = 0.0.radians
            } else {
                this.rotation = 0.radians
                this.skewX = skewX.radians
                this.skewY = skewY.radians
            }

            this.scaleX = hypot(a, b)
            this.scaleY = hypot(c, d)

            if (pivotX == 0.0 && pivotY == 0.0) {
                this.x = matrix.tx
                this.y = matrix.ty
            } else {
                this.x = matrix.tx + ((pivotX * a) + (pivotY * c));
                this.y = matrix.ty + ((pivotX * b) + (pivotY * d));
            }
        }

        fun setMatrix(matrix: IMatrix, pivotX: Double = 0.0, pivotY: Double = 0.0): Transform {
            setMatrixNoReturn(matrix, pivotX, pivotY)
            return this
        }

        fun toMatrix(out: MMatrix = MMatrix()): MMatrix = out.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
        fun copyFrom(that: Transform) = setTo(that.x, that.y, that.scaleX, that.scaleY, that.rotation, that.skewX, that.skewY)

        fun setTo(x: Double, y: Double, scaleX: Double, scaleY: Double, rotation: Angle, skewX: Angle, skewY: Angle): Transform {
            this.x = x
            this.y = y
            this.scaleX = scaleX
            this.scaleY = scaleY
            this.rotation = rotation
            this.skewX = skewX
            this.skewY = skewY
            return this
        }
        fun setTo(x: Float, y: Float, scaleX: Float, scaleY: Float, rotation: Angle, skewX: Angle, skewY: Angle): Transform =
            setTo(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX, skewY)

        fun add(value: Transform): Transform = setTo(
            x + value.x,
            y + value.y,
            scaleX * value.scaleX,
            scaleY * value.scaleY,
            skewX + value.skewX,
            skewY + value.skewY,
            rotation + value.rotation,
        )

        fun minus(value: Transform): Transform = setTo(
            x - value.x,
            y - value.y,
            scaleX / value.scaleX,
            scaleY / value.scaleY,
            skewX - value.skewX,
            skewY - value.skewY,
            rotation - value.rotation,
        )

        fun clone() = Transform().copyFrom(this)

        fun isAlmostEquals(other: Transform, epsilon: Double = 0.000001): Boolean = isAlmostEquals(this, other, epsilon)

        companion object {
            fun isAlmostEquals(a: Transform, b: Transform, epsilon: Double = 0.000001): Boolean =
                a.x.isAlmostEquals(b.x, epsilon)
                    && a.y.isAlmostEquals(b.y, epsilon)
                    && a.scaleX.isAlmostEquals(b.scaleX, epsilon)
                    && a.scaleY.isAlmostEquals(b.scaleY, epsilon)
                    && a.skewX.isAlmostEquals(b.skewX, epsilon)
                    && a.skewY.isAlmostEquals(b.skewY, epsilon)
                    && a.rotation.isAlmostEquals(b.rotation, epsilon)

        }
    }

    class Computed(val matrix: MMatrix, val transform: Transform) {
        companion object;
        constructor(matrix: MMatrix) : this(matrix, Transform().also { it.setMatrixNoReturn(matrix) })
        constructor(transform: Transform) : this(transform.toMatrix(), transform)
    }

    override fun setToInterpolated(ratio: Ratio, l: MMatrix, r: MMatrix) = this.setTo(
        a = ratio.interpolate(l.a, r.a),
        b = ratio.interpolate(l.b, r.b),
        c = ratio.interpolate(l.c, r.c),
        d = ratio.interpolate(l.d, r.d),
        tx = ratio.interpolate(l.tx, r.tx),
        ty = ratio.interpolate(l.ty, r.ty)
    )

    override fun interpolateWith(ratio: Ratio, other: MMatrix): MMatrix =
        MMatrix().setToInterpolated(ratio, this, other)

    inline fun <T> keepMatrix(callback: (MMatrix) -> T): T {
        val a = this.a
        val b = this.b
        val c = this.c
        val d = this.d
        val tx = this.tx
        val ty = this.ty
        try {
            return callback(this)
        } finally {
            this.a = a
            this.b = b
            this.c = c
            this.d = d
            this.tx = tx
            this.ty = ty
        }
    }

    override fun toString(): String = "Matrix(a=$a, b=$b, c=$c, d=$d, tx=$tx, ty=$ty)"
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
