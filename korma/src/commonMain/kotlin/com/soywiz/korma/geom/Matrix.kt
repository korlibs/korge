@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import kotlin.math.*

@Deprecated("Use Matrix instead")
interface IMatrix {
    val _a: Double
    val _b: Double
    val _c: Double
    val _d: Double
    val _tx: Double
    val _ty: Double
    companion object {
        @Deprecated("Kotlin/Native boxes inline + Number")
        operator fun invoke(a: Number = 1, b: Number = 0, c: Number = 0, d: Number = 1, tx: Number = 0, ty: Number = 0): IMatrix = Matrix(
            a.toDouble(),
            b.toDouble(),
            c.toDouble(),
            d.toDouble(),
            tx.toDouble(),
            ty.toDouble()
        )
    }
}

@Deprecated("Use Matrix instead")
val IMatrix.a get() = _a
@Deprecated("Use Matrix instead")
val IMatrix.b get() = _b
@Deprecated("Use Matrix instead")
val IMatrix.c get() = _c
@Deprecated("Use Matrix instead")
val IMatrix.d get() = _d
@Deprecated("Use Matrix instead")
val IMatrix.tx get() = _tx
@Deprecated("Use Matrix instead")
val IMatrix.ty get() = _ty

data class Matrix(
    var a: Double = 1.0,
    var b: Double = 0.0,
    var c: Double = 0.0,
    var d: Double = 1.0,
    var tx: Double = 0.0,
    var ty: Double = 0.0
) : IMatrix, MutableInterpolable<Matrix>, Interpolable<Matrix> {

    override val _a: Double get() = a
    override val _b: Double get() = b
    override val _c: Double get() = c
    override val _d: Double get() = d
    override val _tx: Double get() = tx
    override val _ty: Double get() = ty

    companion object {
        inline operator fun invoke(a: Float, b: Float = 0f, c: Float = 0f, d: Float = 1f, tx: Float = 0f, ty: Float = 0f) =
            Matrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        inline operator fun invoke(a: Int, b: Int = 0, c: Int = 0, d: Int = 1, tx: Int = 0, ty: Int = 0) =
            Matrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        @Deprecated("Kotlin/Native boxes inline + Number")
        inline operator fun invoke(a: Number, b: Number = 0.0, c: Number = 0.0, d: Number = 1.0, tx: Number = 0.0, ty: Number = 0.0) =
            Matrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        operator fun invoke(m: Matrix, out: Matrix = Matrix()): Matrix = out.copyFrom(m)
    }

    enum class Type(val id: Int, val hasRotation: Boolean, val hasScale: Boolean, val hasTranslation: Boolean) {
        IDENTITY(1, hasRotation = false, hasScale = false, hasTranslation = false),
        TRANSLATE(2, hasRotation = false, hasScale = false, hasTranslation = true),
        SCALE(3, hasRotation = false, hasScale = true, hasTranslation = false),
        SCALE_TRANSLATE(4, hasRotation = false, hasScale = true, hasTranslation = true),
        COMPLEX(5, hasRotation = true, hasScale = true, hasTranslation = true);
    }

    fun getType(): Type {
        val hasRotation = b != 0.0 || c != 0.0
        val hasScale = a != 1.0 || d != 1.0
        val hasTranslation = tx != 0.0 || ty != 0.0

        return when {
            hasRotation -> Type.COMPLEX
            hasScale && hasTranslation -> Type.SCALE_TRANSLATE
            hasScale -> Type.SCALE
            hasTranslation -> Type.TRANSLATE
            else -> Type.IDENTITY
        }
    }

    fun setTo(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): Matrix = this.apply {
        this.a = a
        this.b = b
        this.c = c
        this.d = d
        this.tx = tx
        this.ty = ty
    }

    fun setTo(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float): Matrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())
    fun setTo(a: Int, b: Int, c: Int, d: Int, tx: Int, ty: Int): Matrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

    fun copyFrom(that: Matrix): Matrix {
        setTo(that.a, that.b, that.c, that.d, that.tx, that.ty)
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

    fun skew(skewX: Double, skewY: Double): Matrix {
        val sinX = sin(skewX)
        val cosX = cos(skewX)
        val sinY = sin(skewY)
        val cosY = cos(skewY)

        return this.setTo(
            a * cosY - b * sinX,
            a * sinY + b * cosX,
            c * cosY - d * sinX,
            c * sinY + d * cosX,
            tx * cosY - ty * sinX,
            tx * sinY + ty * cosX
        )
    }

    fun scale(sx: Double, sy: Double = sx) = setTo(a * sx, b * sx, c * sy, d * sy, tx * sx, ty * sy)
    fun prescale(sx: Double, sy: Double = sx) = setTo(a * sx, b * sx, c * sy, d * sy, tx, ty)
    fun translate(dx: Double, dy: Double) = this.apply { this.tx += dx; this.ty += dy }
    fun pretranslate(dx: Double, dy: Double) = this.apply { tx += a * dx + c * dy; ty += b * dx + d * dy }

    fun prerotate(angle: Angle) = this.apply {
        val m = Matrix()
        m.rotate(angle)
        this.premultiply(m)
    }

    fun preskew(skewX: Double, skewY: Double) = this.apply {
        val m = Matrix()
        m.skew(skewX, skewY)
        this.premultiply(m)
    }

    fun premultiply(m: Matrix) = this.premultiply(m.a, m.b, m.c, m.d, m.tx, m.ty)

    fun premultiply(la: Double, lb: Double, lc: Double, ld: Double, ltx: Double, lty: Double): Matrix = setTo(
        la * a + lb * c,
        la * b + lb * d,
        lc * a + ld * c,
        lc * b + ld * d,
        ltx * a + lty * c + tx,
        ltx * b + lty * d + ty
    )

    fun multiply(l: Matrix, r: Matrix): Matrix = setTo(
        l.a * r.a + l.b * r.c,
        l.a * r.b + l.b * r.d,
        l.c * r.a + l.d * r.c,
        l.c * r.b + l.d * r.d,
        l.tx * r.a + l.ty * r.c + r.tx,
        l.tx * r.b + l.ty * r.d + r.ty
    )

    /** Transform point without translation */
    fun deltaTransformPoint(point: IPoint) = IPoint(point.x * a + point.y * c, point.x * b + point.y * d)

    fun identity() = setTo(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    fun invert(matrixToInvert: Matrix = this): Matrix {
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

    fun inverted(out: Matrix = Matrix()) = out.invert(this)

    fun setTransform(
        x: Double,
        y: Double,
        scaleX: Double,
        scaleY: Double,
        rotation: Angle,
        skewX: Double,
        skewY: Double
    ): Matrix {
        if (skewX == 0.0 && skewY == 0.0) {
            if (rotation == 0.radians) {
                this.setTo(scaleX, 0.0, 0.0, scaleY, x, y)
            } else {
                val cos = cos(rotation)
                val sin = sin(rotation)
                this.setTo(cos * scaleX, sin * scaleY, -sin * scaleX, cos * scaleY, x, y)
            }
        } else {
            this.identity()
            scale(scaleX, scaleY)
            skew(skewX, skewY)
            rotate(rotation)
            translate(x, y)
        }
        return this
    }

    fun clone() = Matrix(a, b, c, d, tx, ty)

    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun setTo(a: Number, b: Number, c: Number, d: Number, tx: Number, ty: Number): Matrix = setTo(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun scale(sx: Number, sy: Number = sx) = scale(sx.toDouble(), sy.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun prescale(sx: Number, sy: Number = sx) = prescale(sx.toDouble(), sy.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun translate(dx: Number, dy: Number) = translate(dx.toDouble(), dy.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun pretranslate(dx: Number, dy: Number) = pretranslate(dx.toDouble(), dy.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun skew(skewX: Number, skewY: Number): Matrix = skew(skewX.toDouble(), skewY.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun preskew(skewX: Number, skewY: Number) = preskew(skewX.toDouble(), skewY.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun premultiply(la: Number, lb: Number, lc: Number, ld: Number, ltx: Number, lty: Number): Matrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun rotateRadians(angle: Number) = rotate(angle.radians)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun rotateDegrees(angle: Number) = rotate(angle.degrees)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun prerotateRadians(angle: Number) = prerotate(angle.radians)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun prerotateDegrees(angle: Number) = prerotate(angle.degrees)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun setTransform(x: Number, y: Number, scaleX: Number, scaleY: Number, rotation: Angle, skewX: Number, skewY: Number): Matrix = setTransform(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX.toDouble(), skewY.toDouble())

    operator fun times(that: Matrix): Matrix = Matrix().multiply(this, that)

    // Transform points
    fun transformX(px: Double, py: Double): Double = this.a * px + this.c * py + this.tx
    fun transformY(px: Double, py: Double): Double = this.d * py + this.b * px + this.ty
    fun transform(px: Double, py: Double, out: Point = Point()): Point = out.setTo(transformX(px, py), transformY(px, py))
    fun transform(p: IPoint, out: Point = Point()): Point = transform(p.x, p.y, out)
    fun transformX(p: IPoint): Double = transformX(p.x, p.y)
    fun transformY(p: IPoint): Double = transformY(p.x, p.y)
    fun transformXf(px: Double, py: Double): Float = transformX(px, py).toFloat()
    fun transformYf(px: Double, py: Double): Float = transformY(px, py).toFloat()
    fun transformXf(px: Float, py: Float): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
    fun transformYf(px: Float, py: Float): Float = transformY(px.toDouble(), py.toDouble()).toFloat()

    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun transform(px: Number, py: Number, out: Point = Point()): Point = transform(px.toDouble(), py.toDouble(), out)
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun transformXf(px: Number, py: Number): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun transformYf(px: Number, py: Number): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun transformX(px: Number, py: Number): Double = transformX(px.toDouble(), py.toDouble())
    @Deprecated("Kotlin/Native boxes inline + Number")
    inline fun transformY(px: Number, py: Number): Double = transformY(px.toDouble(), py.toDouble())


    data class Transform(
        var x: Double = 0.0, var y: Double = 0.0,
        var scaleX: Double = 1.0, var scaleY: Double = 1.0,
        var skewX: Double = 0.0, var skewY: Double = 0.0,
        var rotation: Angle = 0.radians
    ) : MutableInterpolable<Transform>, Interpolable<Transform> {
        companion object {
            @Deprecated("Kotlin/Native boxes inline + Number")
            inline operator fun invoke(x: Number = 0.0, y: Number = 0.0, scaleX: Number = 1.0, scaleY: Number = 1.0, skewX: Number = 0.0, skewY: Number = 0.0, rotation: Angle = 0.radians) =
                Transform(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), skewX.toDouble(), skewY.toDouble(), rotation)
        }

        override fun interpolateWith(ratio: Double, other: Transform): Transform = Transform().setToInterpolated(ratio, this, other)

        override fun setToInterpolated(ratio: Double, l: Transform, r: Transform): Transform = this.setTo(
            ratio.interpolate(l.x, r.x),
            ratio.interpolate(l.y, r.y),
            ratio.interpolate(l.scaleX, r.scaleX),
            ratio.interpolate(l.scaleY, r.scaleY),
            ratio.interpolate(l.rotation, r.rotation),
            ratio.interpolate(l.skewX, r.skewX),
            ratio.interpolate(l.skewY, r.skewY)
        )

        fun identity() {
            x = 0.0
            y = 0.0
            scaleX = 1.0
            scaleY = 1.0
            skewX = 0.0
            skewY = 0.0
            rotation = 0.radians
        }

        @Deprecated("Use Matrix instead")
        fun setMatrix(matrix: IMatrix): Transform {
            val PI_4 = PI / 4.0
            this.x = matrix._tx
            this.y = matrix._ty

            this.skewX = atan(-matrix._c / matrix._d)
            this.skewY = atan(matrix._b / matrix._a)

            // Faster isNaN
            if (this.skewX != this.skewX) this.skewX = 0.0
            if (this.skewY != this.skewY) this.skewY = 0.0

            this.scaleY =
                if (this.skewX > -PI_4 && this.skewX < PI_4) matrix._d / cos(this.skewX) else -matrix._c / sin(this.skewX)
            this.scaleX =
                if (this.skewY > -PI_4 && this.skewY < PI_4) matrix._a / cos(this.skewY) else matrix._b / sin(this.skewY)

            if (abs(this.skewX - this.skewY) < 0.0001) {
                this.rotation = this.skewX.radians
                this.skewX = 0.0
                this.skewY = 0.0
            } else {
                this.rotation = 0.radians
            }

            return this
        }

        fun setMatrix(matrix: Matrix): Transform {
            val PI_4 = PI / 4.0
            this.x = matrix.tx
            this.y = matrix.ty

            this.skewX = atan(-matrix.c / matrix.d)
            this.skewY = atan(matrix.b / matrix.a)

            // Faster isNaN
            if (this.skewX != this.skewX) this.skewX = 0.0
            if (this.skewY != this.skewY) this.skewY = 0.0

            this.scaleY =
                if (this.skewX > -PI_4 && this.skewX < PI_4) matrix.d / cos(this.skewX) else -matrix.c / sin(this.skewX)
            this.scaleX =
                if (this.skewY > -PI_4 && this.skewY < PI_4) matrix.a / cos(this.skewY) else matrix.b / sin(this.skewY)

            if (abs(this.skewX - this.skewY) < 0.0001) {
                this.rotation = this.skewX.radians
                this.skewX = 0.0
                this.skewY = 0.0
            } else {
                this.rotation = 0.radians
            }

            return this
        }

        fun toMatrix(out: Matrix = Matrix()): Matrix = out.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
        fun copyFrom(that: Transform) = setTo(that.x, that.y, that.scaleX, that.scaleY, that.rotation, that.skewX, that.skewY)

        fun setTo(x: Double, y: Double, scaleX: Double, scaleY: Double, rotation: Angle, skewX: Double, skewY: Double): Transform {
            this.x = x
            this.y = y
            this.scaleX = scaleX
            this.scaleY = scaleY
            this.rotation = rotation
            this.skewX = skewX
            this.skewY = skewY
            return this
        }

        @Deprecated("Kotlin/Native boxes inline + Number")
        inline fun setTo(x: Number, y: Number, scaleX: Number, scaleY: Number, rotation: Angle, skewX: Number, skewY: Number): Transform =
            setTo(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX.toDouble(), skewY.toDouble())

        fun clone() = Transform().copyFrom(this)
    }

    class Computed(val matrix: Matrix, val transform: Transform) {
        companion object;
        constructor(matrix: Matrix) : this(matrix, Transform().setMatrix(matrix))
        constructor(transform: Transform) : this(transform.toMatrix(), transform)
    }

    override fun setToInterpolated(ratio: Double, l: Matrix, r: Matrix) = this.setTo(
        a = ratio.interpolate(l.a, r.a),
        b = ratio.interpolate(l.b, r.b),
        c = ratio.interpolate(l.c, r.c),
        d = ratio.interpolate(l.d, r.d),
        tx = ratio.interpolate(l.tx, r.tx),
        ty = ratio.interpolate(l.ty, r.ty)
    )

    override fun interpolateWith(ratio: Double, other: Matrix): Matrix =
        Matrix().setToInterpolated(ratio, this, other)

    inline fun <T> keep(callback: Matrix.() -> T): T {
        val a = this.a
        val b = this.b
        val c = this.c
        val d = this.d
        val tx = this.tx
        val ty = this.ty
        try {
            return callback()
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

@Deprecated("Use Matrix instead")
operator fun IMatrix.times(that: IMatrix): Matrix = Matrix().multiply(this, that)

// Transform points
@Deprecated("Use Matrix instead")
fun IMatrix.transformX(px: Double, py: Double): Double = this._a * px + this._c * py + this._tx
@Deprecated("Use Matrix instead")
fun IMatrix.transformY(px: Double, py: Double): Double = this._d * py + this._b * px + this._ty
@Deprecated("Use Matrix instead")
fun IMatrix.transform(px: Double, py: Double, out: Point = Point()): Point = out.setTo(transformX(px, py), transformY(px, py))
@Deprecated("Use Matrix instead")
inline fun IMatrix.transform(px: Number, py: Number, out: Point = Point()): Point = transform(px.toDouble(), py.toDouble(), out)
@Deprecated("Use Matrix instead")
inline fun IMatrix.transform(p: IPoint, out: Point = Point()): Point = transform(p.x, p.y, out)
@Deprecated("Use Matrix instead")
inline fun IMatrix.transformXf(px: Number, py: Number): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
@Deprecated("Use Matrix instead")
inline fun IMatrix.transformYf(px: Number, py: Number): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
@Deprecated("Use Matrix instead")
inline fun IMatrix.transformX(px: Number, py: Number): Double = transformX(px.toDouble(), py.toDouble())
@Deprecated("Use Matrix instead")
inline fun IMatrix.transformY(px: Number, py: Number): Double = transformY(px.toDouble(), py.toDouble())
@Deprecated("Use Matrix instead")
inline fun IMatrix.transformX(p: IPoint): Double = transformX(p.x, p.y)
@Deprecated("Use Matrix instead")
inline fun IMatrix.transformY(p: IPoint): Double = transformY(p.x, p.y)

@Deprecated("Use Matrix instead")
fun Matrix.invert(matrixToInvert: IMatrix = this): Matrix {
    val src = matrixToInvert
    val dst = this
    val norm = src._a * src._d - src._b * src._c

    if (norm == 0.0) {
        dst.setTo(0.0, 0.0, 0.0, 0.0, -src._tx, -src._ty)
    } else {
        val inorm = 1.0 / norm
        val d = src._a * inorm
        val a = src._d * inorm
        val b = src._b * -inorm
        val c = src._c * -inorm
        dst.setTo(a, b, c, d, -a * src._tx - c * src._ty, -b * src._tx - d * src._ty)
    }

    return this
}

@Deprecated("Use Matrix instead")
fun Matrix.multiply(l: IMatrix, r: IMatrix): Matrix = setTo(
    l._a * r._a + l._b * r._c,
    l._a * r._b + l._b * r._d,
    l._c * r._a + l._d * r._c,
    l._c * r._b + l._d * r._d,
    l._tx * r._a + l._ty * r._c + r._tx,
    l._tx * r._b + l._ty * r._d + r._ty
)
@Deprecated("Use Matrix instead")
fun Matrix.premultiply(m: IMatrix) = this.premultiply(m._a, m._b, m._c, m._d, m._tx, m._ty)

@Deprecated("Use Matrix instead")
fun Matrix.copyFrom(that: IMatrix): Matrix {
    setTo(that._a, that._b, that._c, that._d, that._tx, that._ty)
    return this
}
