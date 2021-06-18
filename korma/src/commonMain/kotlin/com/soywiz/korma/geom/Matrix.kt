@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import kotlin.jvm.*
import kotlin.math.*

data class Matrix(
    var a: Double = 1.0,
    var b: Double = 0.0,
    var c: Double = 0.0,
    var d: Double = 1.0,
    var tx: Double = 0.0,
    var ty: Double = 0.0
) : MutableInterpolable<Matrix>, Interpolable<Matrix> {
    companion object {
        inline operator fun invoke(a: Float, b: Float = 0f, c: Float = 0f, d: Float = 1f, tx: Float = 0f, ty: Float = 0f) =
            Matrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        inline operator fun invoke(a: Int, b: Int = 0, c: Int = 0, d: Int = 1, tx: Int = 0, ty: Int = 0) =
            Matrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        operator fun invoke(m: Matrix, out: Matrix = Matrix()): Matrix = out.copyFrom(m)
    }

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

    fun copyFrom(that: Matrix?): Matrix {
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

    fun skew(skewX: Angle, skewY: Angle): Matrix {
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
        val m = Matrix()
        m.rotate(angle)
        this.premultiply(m)
    }

    fun preskew(skewX: Angle, skewY: Angle) = this.apply {
        val m = Matrix()
        m.skew(skewX, skewY)
        this.premultiply(m)
    }

    fun premultiply(m: Matrix) = this.premultiply(m.a, m.b, m.c, m.d, m.tx, m.ty)
    fun postmultiply(m: Matrix) = multiply(this, m)

    fun premultiply(la: Double, lb: Double, lc: Double, ld: Double, ltx: Double, lty: Double): Matrix = setTo(
        la * a + lb * c,
        la * b + lb * d,
        lc * a + ld * c,
        lc * b + ld * d,
        ltx * a + lty * c + tx,
        ltx * b + lty * d + ty
    )
    fun premultiply(la: Float, lb: Float, lc: Float, ld: Float, ltx: Float, lty: Float): Matrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())
    fun premultiply(la: Int, lb: Int, lc: Int, ld: Int, ltx: Int, lty: Int): Matrix = premultiply(la.toDouble(), lb.toDouble(), lc.toDouble(), ld.toDouble(), ltx.toDouble(), lty.toDouble())

    fun multiply(l: Matrix, r: Matrix): Matrix = setTo(
        l.a * r.a + l.b * r.c,
        l.a * r.b + l.b * r.d,
        l.c * r.a + l.d * r.c,
        l.c * r.b + l.d * r.d,
        l.tx * r.a + l.ty * r.c + r.tx,
        l.tx * r.b + l.ty * r.d + r.ty
    )

    /** Transform point without translation */
    fun deltaTransformPoint(point: IPoint, out: Point = Point()) = deltaTransformPoint(point.x, point.y, out)
    fun deltaTransformPoint(x: Float, y: Float, out: Point = Point()): Point = deltaTransformPoint(x.toDouble(), y.toDouble(), out)
    fun deltaTransformPoint(x: Double, y: Double, out: Point = Point()): Point {
        out.x = deltaTransformX(x, y)
        out.y = deltaTransformY(x, y)
        return out
    }

    fun deltaTransformX(x: Double, y: Double): Double = (x * a) + (y * c)
    fun deltaTransformY(x: Double, y: Double): Double = (x * b) + (y * d)

    fun identity() = setTo(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)

    fun isIdentity() = getType() == Type.IDENTITY

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

    fun concat(value: Matrix): Matrix = this.multiply(this, value)

    fun inverted(out: Matrix = Matrix()) = out.invert(this)

    fun setTransform(
        x: Double,
        y: Double,
        scaleX: Double,
        scaleY: Double,
        rotation: Angle,
        skewX: Angle,
        skewY: Angle
    ): Matrix {
        if (skewX == 0.0.radians && skewY == 0.0.radians) {
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
    fun setTransform(x: Float, y: Float, scaleX: Float, scaleY: Float, rotation: Angle, skewX: Angle, skewY: Angle): Matrix = setTransform(x.toDouble(), y.toDouble(), scaleX.toDouble(), scaleY.toDouble(), rotation, skewX, skewY)

    fun clone() = Matrix(a, b, c, d, tx, ty)

    operator fun times(that: Matrix): Matrix = Matrix().multiply(this, that)

    fun toTransform(out: Transform = Transform()): Transform {
        out.setMatrixNoReturn(this)
        return out
    }

    // Transform points
    fun transform(p: IPoint, out: Point = Point()): Point = transform(p.x, p.y, out)
    fun transform(px: Double, py: Double, out: Point = Point()): Point = out.setTo(transformX(px, py), transformY(px, py))
    fun transform(px: Float, py: Float, out: Point = Point()): Point = out.setTo(transformX(px, py), transformY(px, py))
    fun transform(px: Int, py: Int, out: Point = Point()): Point = out.setTo(transformX(px, py), transformY(px, py))

    fun transformX(p: IPoint): Double = transformX(p.x, p.y)
    fun transformX(px: Double, py: Double): Double = this.a * px + this.c * py + this.tx
    fun transformX(px: Float, py: Float): Double = this.a * px + this.c * py + this.tx
    fun transformX(px: Int, py: Int): Double = this.a * px + this.c * py + this.tx

    fun transformY(p: IPoint): Double = transformY(p.x, p.y)
    fun transformY(px: Double, py: Double): Double = this.d * py + this.b * px + this.ty
    fun transformY(px: Float, py: Float): Double = this.d * py + this.b * px + this.ty
    fun transformY(px: Int, py: Int): Double = this.d * py + this.b * px + this.ty

    fun transformXf(px: Double, py: Double): Float = transformX(px, py).toFloat()
    fun transformXf(px: Float, py: Float): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
    fun transformXf(px: Int, py: Int): Float = transformX(px.toDouble(), py.toDouble()).toFloat()

    fun transformYf(px: Double, py: Double): Float = transformY(px, py).toFloat()
    fun transformYf(px: Float, py: Float): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
    fun transformYf(px: Int, py: Int): Float = transformY(px.toDouble(), py.toDouble()).toFloat()

    @Suppress("DuplicatedCode")
    fun transformRectangle(rectangle: Rectangle, delta: Boolean = false): Unit {
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



    fun copyFromArray(value: FloatArray, offset: Int = 0): Matrix = setTo(
        value[offset + 0], value[offset + 1], value[offset + 2],
        value[offset + 3], value[offset + 4], value[offset + 5]
    )

    fun copyFromArray(value: DoubleArray, offset: Int = 0): Matrix = setTo(
        value[offset + 0].toFloat(), value[offset + 1].toFloat(), value[offset + 2].toFloat(),
        value[offset + 3].toFloat(), value[offset + 4].toFloat(), value[offset + 5].toFloat()
    )

    data class Transform(
        override var x: Double = 0.0, override var y: Double = 0.0,
        var scaleX: Double = 1.0, var scaleY: Double = 1.0,
        var skewX: Angle = 0.radians, var skewY: Angle = 0.radians,
        var rotation: Angle = 0.radians
    ) : MutableInterpolable<Transform>, Interpolable<Transform>, XY, XYf {

        override var xf: Float
            get() = x.toFloat()
            set(value) { x = value.toDouble() }

        override var yf: Float
            get() = y.toFloat()
            set(value) { y = value.toDouble() }

        val scaleAvg get() = (scaleX + scaleY) * 0.5

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
            skewX = 0.0.radians
            skewY = 0.0.radians
            rotation = 0.0.radians
        }

        fun setMatrixNoReturn(matrix: Matrix) {
            val PI_4 = PI / 4.0
            this.x = matrix.tx
            this.y = matrix.ty

            this.skewX = atan(-matrix.c / matrix.d).radians
            this.skewY = atan(matrix.b / matrix.a).radians

            // Faster isNaN
            if (this.skewX != this.skewX) this.skewX = 0.0.radians
            if (this.skewY != this.skewY) this.skewY = 0.0.radians

            this.scaleY =
                if (this.skewX > -PI_4.radians && this.skewX < PI_4.radians) matrix.d / cos(this.skewX) else -matrix.c / sin(this.skewX)
            this.scaleX =
                if (this.skewY > -PI_4.radians && this.skewY < PI_4.radians) matrix.a / cos(this.skewY) else matrix.b / sin(this.skewY)

            if (abs(this.skewX - this.skewY).radians < 0.0001) {
                this.rotation = this.skewX
                this.skewX = 0.0.radians
                this.skewY = 0.0.radians
            } else {
                this.rotation = 0.radians
            }
        }

        fun setMatrix(matrix: Matrix): Transform {
            setMatrixNoReturn(matrix)
            return this
        }

        fun toMatrix(out: Matrix = Matrix()): Matrix = out.setTransform(x, y, scaleX, scaleY, rotation, skewX, skewY)
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
            rotation + value.rotation,
            skewX + value.skewX,
            skewY + value.skewY,
        )

        fun minus(value: Transform): Transform = setTo(
            x - value.x,
            y - value.y,
            scaleX / value.scaleX,
            scaleY / value.scaleY,
            rotation - value.rotation,
            skewX - value.skewX,
            skewY - value.skewY,
        )

        fun clone() = Transform().copyFrom(this)
    }

    class Computed(val matrix: Matrix, val transform: Transform) {
        companion object;
        constructor(matrix: Matrix) : this(matrix, Transform().also { it.setMatrixNoReturn(matrix) })
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
            return this.callback()
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
