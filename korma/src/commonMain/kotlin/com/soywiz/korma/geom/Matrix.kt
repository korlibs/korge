@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korma.geom

import com.soywiz.korma.interpolation.Interpolable
import com.soywiz.korma.interpolation.MutableInterpolable
import com.soywiz.korma.interpolation.interpolate
import kotlin.jvm.*
import kotlin.math.*

class Matrix @PublishedApi internal constructor(dummy: Boolean) : MutableInterpolable<Matrix>, Interpolable<Matrix> {
    @PublishedApi
    internal val data: FloatArray = FloatArray(6)
    override fun hashCode(): Int = data.contentHashCode()
    override fun equals(other: Any?): Boolean = (other is Matrix) && (this.data.contentEquals(other.data))
    fun copy() = Matrix(true).copyFrom(this)

    var af: Float get() = data[0] ; set(value) { data[0] = value }
    var bf: Float get() = data[1] ; set(value) { data[1] = value }
    var cf: Float get() = data[2] ; set(value) { data[2] = value }
    var df: Float get() = data[3] ; set(value) { data[3] = value }
    var txf: Float get() = data[4] ; set(value) { data[4] = value }
    var tyf: Float get() = data[5] ; set(value) { data[5] = value }

    var a: Double get() = af.toDouble() ; set(value) { af = value.toFloat() }
    var b: Double get() = bf.toDouble() ; set(value) { bf = value.toFloat() }
    var c: Double get() = cf.toDouble() ; set(value) { cf = value.toFloat() }
    var d: Double get() = df.toDouble() ; set(value) { df = value.toFloat() }
    var tx: Double get() = txf.toDouble() ; set(value) { txf = value.toFloat() }
    var ty: Double get() = tyf.toDouble() ; set(value) { tyf = value.toFloat() }

    companion object {
        operator fun invoke(
            a: Double,
            b: Double = 0.0,
            c: Double = 0.0,
            d: Double = 1.0,
            tx: Double = 0.0,
            ty: Double = 0.0,
        ) = Matrix(true).setTo(a, b, c, d, tx, ty)

        operator fun invoke(
            a: Float,
            b: Float = 0f,
            c: Float = 0f,
            d: Float = 1f,
            tx: Float = 0f,
            ty: Float = 0f
        ) = Matrix(true).setTo(a, b, c, d, tx, ty)

        operator fun invoke() = Matrix(1f, 0f, 0f, 1f, 0f, 0f)

        inline operator fun invoke(a: Int, b: Int = 0, c: Int = 0, d: Int = 1, tx: Int = 0, ty: Int = 0) =
            Matrix(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), tx.toFloat(), ty.toFloat())

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
        val hasRotation = bf != 0f || cf != 0f
        val hasScale = af != 1f || df != 1f
        val hasTranslation = txf != 0f || tyf != 0f

        return when {
            hasRotation -> Type.COMPLEX
            hasScale && hasTranslation -> Type.SCALE_TRANSLATE
            hasScale -> Type.SCALE
            hasTranslation -> Type.TRANSLATE
            else -> Type.IDENTITY
        }
    }

    fun setTo(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double): Matrix =
        setTo(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), tx.toFloat(), ty.toFloat())

    fun setTo(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float): Matrix {
        this.af = a
        this.bf = b
        this.cf = c
        this.df = d
        this.txf = tx
        this.tyf = ty
        return this
    }

    fun setTo(a: Int, b: Int, c: Int, d: Int, tx: Int, ty: Int): Matrix =
        setTo(a.toFloat(), b.toFloat(), c.toFloat(), d.toFloat(), tx.toFloat(), ty.toFloat())

    fun copyFrom(that: Matrix?): Matrix =
        if (that != null) setTo(that.af, that.bf, that.cf, that.df, that.txf, that.tyf) else identity()

    fun rotate(angle: Angle): Matrix {
        val theta = angle.radiansf
        val cos = cos(theta)
        val sin = sin(theta)

        val a1 = af * cos - bf * sin
        bf = (af * sin + bf * cos)
        af = a1
        val c1 = cf * cos - df * sin
        df = (cf * sin + df * cos)
        cf = c1
        val tx1 = txf * cos - tyf * sin
        tyf = (txf * sin + tyf * cos)
        txf = tx1

        return this
    }

    fun skew(skewX: Angle, skewY: Angle): Matrix {
        val sinX = sinf(skewX)
        val cosX = cosf(skewX)
        val sinY = sinf(skewY)
        val cosY = cosf(skewY)

        return this.setTo(
            af * cosY - bf * sinX,
            af * sinY + bf * cosX,
            cf * cosY - df * sinX,
            cf * sinY + df * cosX,
            txf * cosY - tyf * sinX,
            txf * sinY + tyf * cosX
        )
    }

    fun scale(sx: Double, sy: Double = sx) = scale(sx.toFloat(), sy.toFloat())
    fun scale(sx: Float, sy: Float = sx) = setTo(af * sx, bf * sx, cf * sy, df * sy, txf * sx, tyf * sy)
    fun scale(sx: Int, sy: Int = sx) = scale(sx.toFloat(), sy.toFloat())

    fun prescale(sx: Double, sy: Double = sx) = prescale(sx.toFloat(), sy.toFloat())
    fun prescale(sx: Float, sy: Float = sx) = setTo(af * sx, bf * sx, cf * sy, df * sy, txf, tyf)
    fun prescale(sx: Int, sy: Int = sx) = prescale(sx.toDouble(), sy.toDouble())

    fun translate(dx: Double, dy: Double): Matrix = translate(dx.toFloat(), dy.toFloat())
    fun translate(dx: Float, dy: Float): Matrix {
        this.txf += dx
        this.tyf += dy
        return this
    }
    fun translate(dx: Int, dy: Int) = translate(dx.toFloat(), dy.toFloat())

    fun pretranslate(dx: Double, dy: Double): Matrix = pretranslate(dx.toFloat(), dy.toFloat())

    fun pretranslate(dx: Float, dy: Float): Matrix {
        txf += af * dx + cf * dy
        tyf += bf * dx + df * dy
        return this
    }
    fun pretranslate(dx: Int, dy: Int) = pretranslate(dx.toDouble(), dy.toDouble())

    fun prerotate(angle: Angle): Matrix {
        val m = Matrix()
        m.rotate(angle)
        this.premultiply(m)
        return this
    }

    fun preskew(skewX: Angle, skewY: Angle): Matrix {
        val m = Matrix()
        m.skew(skewX, skewY)
        this.premultiply(m)
        return this
    }

    fun premultiply(m: Matrix) = this.premultiply(m.af, m.bf, m.cf, m.df, m.txf, m.tyf)
    fun postmultiply(m: Matrix) = multiply(this, m)

    fun premultiply(la: Double, lb: Double, lc: Double, ld: Double, ltx: Double, lty: Double): Matrix = setTo(la.toFloat(), lb.toFloat(), lc.toFloat(), ld.toFloat(), ltx.toFloat(), lty.toFloat())

    fun premultiply(la: Float, lb: Float, lc: Float, ld: Float, ltx: Float, lty: Float): Matrix = setTo(
        la * af + lb * cf,
        la * bf + lb * df,
        lc * af + ld * cf,
        lc * bf + ld * df,
        ltx * af + lty * cf + txf,
        ltx * bf + lty * df + tyf
    )

    fun premultiply(la: Int, lb: Int, lc: Int, ld: Int, ltx: Int, lty: Int): Matrix =
        premultiply(la.toFloat(), lb.toFloat(), lc.toFloat(), ld.toFloat(), ltx.toFloat(), lty.toFloat())

    fun multiply(l: Matrix, r: Matrix): Matrix = setTo(
        l.af * r.af + l.bf * r.cf,
        l.af * r.bf + l.bf * r.df,
        l.cf * r.af + l.df * r.cf,
        l.cf * r.bf + l.df * r.df,
        l.txf * r.af + l.tyf * r.cf + r.txf,
        l.txf * r.bf + l.tyf * r.df + r.tyf,
    )

    /** Transform point without translation */
    fun deltaTransformPoint(point: IPoint, out: Point = Point()) = deltaTransformPoint(point.x, point.y, out)
    fun deltaTransformPoint(x: Float, y: Float, out: Point = Point()): Point {
        out.xf = deltaTransformX(x, y)
        out.yf = deltaTransformY(x, y)
        return out
    }

    fun deltaTransformPoint(x: Double, y: Double, out: Point = Point()): Point = deltaTransformPoint(x.toFloat(), y.toFloat(), out)

    fun deltaTransformX(x: Double, y: Double): Double = (x * a) + (y * c)
    fun deltaTransformY(x: Double, y: Double): Double = (x * b) + (y * d)

    fun deltaTransformX(x: Float, y: Float): Float = (x * af) + (y * cf)
    fun deltaTransformY(x: Float, y: Float): Float = (x * bf) + (y * df)

    fun identity() = setTo(1f, 0f, 0f, 1f, 0f, 0f)

    fun isIdentity() = getType() == Type.IDENTITY

    fun invert(matrixToInvert: Matrix = this): Matrix {
        val src = matrixToInvert
        val dst = this
        val norm = src.af * src.df - src.bf * src.cf

        if (norm == 0f) {
            dst.setTo(0f, 0f, 0f, 0f, -src.txf, -src.tyf)
        } else {
            val inorm = 1f / norm
            val d = src.af * inorm
            val a = src.df * inorm
            val b = src.bf * -inorm
            val c = src.cf * -inorm
            dst.setTo(a, b, c, d, -a * src.txf - c * src.tyf, -b * src.txf - d * src.tyf)
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
    ): Matrix = setTransform(x.toFloat(), y.toFloat(), scaleX.toFloat(), scaleY.toFloat(), rotation, skewX, skewY)

    fun setTransform(
        x: Float,
        y: Float,
        scaleX: Float,
        scaleY: Float,
        rotation: Angle,
        skewX: Angle,
        skewY: Angle
    ): Matrix {
        if (skewX == 0f.radians && skewY == 0f.radians) {
            if (rotation == 0f.radians) {
                this.setTo(scaleX, 0f, 0f, scaleY, x, y)
            } else {
                val cos = cosf(rotation)
                val sin = sinf(rotation)
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

    fun clone() = Matrix(af, bf, cf, df, txf, tyf)

    operator fun times(that: Matrix): Matrix = Matrix().multiply(this, that)

    fun toTransform(out: Transform = Transform()): Transform = out.setMatrix(this)

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

    fun transformXf(px: Float, py: Float): Float = this.af * px + this.cf * py + this.txf
    fun transformXf(px: Double, py: Double): Float = transformXf(px.toFloat(), py.toFloat())
    fun transformXf(px: Int, py: Int): Float = transformXf(px.toFloat(), py.toFloat())

    fun transformYf(px: Float, py: Float): Float = this.df * py + this.bf * px + this.tyf
    fun transformYf(px: Double, py: Double): Float = transformYf(px.toFloat(), py.toFloat())
    fun transformYf(px: Int, py: Int): Float = transformYf(px.toFloat(), py.toFloat())

    @Suppress("DuplicatedCode")
    fun transformRectangle(rectangle: Rectangle, delta: Boolean = false): Unit {
        val a = this.af
        val b = this.bf
        val c = this.cf
        val d = this.df
        val tx = if (delta) 0f else this.txf
        val ty = if (delta) 0f else this.tyf

        val x = rectangle.x.toFloat()
        val y = rectangle.y.toFloat()
        val xMax = x + rectangle.width.toFloat()
        val yMax = y + rectangle.height.toFloat()

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

        rectangle.x = floor(if (x0 < x2) x0 else x2).toDouble()
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

        rectangle.y = floor(if (y0 < y2) y0 else y2).toDouble()
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

    class Transform private constructor(
        dummy: Boolean
    ) : MutableInterpolable<Transform>, Interpolable<Transform>, XY, XYf {
        override fun equals(other: Any?): Boolean = (other is Transform) && data.contentEquals(other.data)
        override fun hashCode(): Int = data.contentHashCode()

        @PublishedApi
        internal val data = FloatArray(7)

        override var xf: Float get() = data[0] ; set(value) { data[0] = value }
        override var yf: Float get() = data[1] ; set(value) { data[1] = value }
        var scaleXf: Float get() = data[2] ; set(value) { data[2] = value }
        var scaleYf: Float get() = data[3] ; set(value) { data[3] = value }
        var skewXRadiansf: Float get() = data[4] ; set(value) { data[4] = value }
        var skewYRadiansf: Float get() = data[5] ; set(value) { data[5] = value }
        var rotationRadiansf: Float get() = data[6] ; set(value) { data[6] = value }

        override var x: Double get() = xf.toDouble() ; set(value) { xf = value.toFloat() }
        override var y: Double get() = yf.toDouble() ; set(value) { yf = value.toFloat() }
        var scaleX: Double get() = scaleXf.toDouble() ; set(value) { scaleXf = value.toFloat() }
        var scaleY: Double get() = scaleYf.toDouble() ; set(value) { scaleYf = value.toFloat() }

        var skewX: Angle get() = Angle(skewXRadiansf) ; set(value) { skewXRadiansf = value.radiansf }
        var skewY: Angle get() = Angle(skewYRadiansf) ; set(value) { skewYRadiansf = value.radiansf }
        var rotation: Angle get() = Angle(rotationRadiansf) ; set(value) { rotationRadiansf = value.radiansf }

        constructor(
            x: Double = 0.0, y: Double = 0.0,
            scaleX: Double = 1.0, scaleY: Double = 1.0,
            skewX: Angle = 0.radians, skewY: Angle = 0.radians,
            rotation: Angle = 0.radians
        ) : this(false) {
            setTo(x, y, scaleX, scaleY, skewX, skewY, rotation)
        }

        constructor(
            x: Float, y: Float,
            scaleX: Float = 1f, scaleY: Float = 1f,
            skewX: Angle = 0.radians, skewY: Angle = 0.radians,
            rotation: Angle = 0.radians
        ) : this(false) {
            setTo(x, y, scaleX, scaleY, skewX, skewY, rotation)
        }

        val scaleAvg get() = (scaleX + scaleY) * 0.5

        override fun interpolateWith(ratio: Double, other: Transform): Transform =
            Transform().setToInterpolated(ratio, this, other)

        override fun setToInterpolated(ratio: Double, l: Transform, r: Transform): Transform = this.setTo(
            ratio.interpolate(l.xf, r.xf),
            ratio.interpolate(l.yf, r.yf),
            ratio.interpolate(l.scaleXf, r.scaleXf),
            ratio.interpolate(l.scaleYf, r.scaleYf),
            ratio.interpolate(l.rotation, r.rotation),
            ratio.interpolate(l.skewX, r.skewX),
            ratio.interpolate(l.skewY, r.skewY),
        )

        fun identity() {
            xf = 0f
            yf = 0f
            scaleXf = 1f
            scaleYf = 1f
            skewX = 0f.radians
            skewY = 0f.radians
            rotation = 0f.radians
        }

        fun setMatrix(matrix: Matrix): Transform {
            val PI_4 = PI.toFloat() / 4f
            this.xf = matrix.txf
            this.yf = matrix.tyf

            this.skewX = atan(-matrix.cf / matrix.df).radians
            this.skewY = atan(matrix.bf / matrix.af).radians

            // Faster isNaN
            if (this.skewX != this.skewX) this.skewX = 0f.radians
            if (this.skewY != this.skewY) this.skewY = 0f.radians

            this.scaleYf = if (this.skewX > -PI_4.radians && this.skewX < PI_4.radians) matrix.df / cosf(this.skewX) else -matrix.cf / sinf(this.skewX)
            this.scaleXf = if (this.skewY > -PI_4.radians && this.skewY < PI_4.radians) matrix.af / cosf(this.skewY) else matrix.bf / sinf(this.skewY)

            if (abs(this.skewX - this.skewY).radiansf < 0.0001f) {
                this.rotation = this.skewX
                this.skewX = 0f.radians
                this.skewY = 0f.radians
            } else {
                this.rotation = 0f.radians
            }

            return this
        }

        fun toMatrix(out: Matrix = Matrix()): Matrix = out.setTransform(xf, yf, scaleXf, scaleYf, rotation, skewX, skewY)
        fun copyFrom(that: Transform) =
            setTo(that.xf, that.yf, that.scaleXf, that.scaleYf, that.rotation, that.skewX, that.skewY)

        fun setTo(
            x: Double,
            y: Double,
            scaleX: Double,
            scaleY: Double,
            rotation: Angle,
            skewX: Angle,
            skewY: Angle
        ): Transform = setTo(x.toFloat(), y.toFloat(), scaleX.toFloat(), scaleY.toFloat(), rotation, skewX, skewY)

        fun setTo(
            x: Float,
            y: Float,
            scaleX: Float,
            scaleY: Float,
            rotation: Angle,
            skewX: Angle,
            skewY: Angle
        ): Transform {
            this.xf = x
            this.yf = y
            this.scaleXf = scaleX
            this.scaleYf = scaleY
            this.rotation = rotation
            this.skewX = skewX
            this.skewY = skewY
            return this
        }

        fun add(value: Transform): Transform = setTo(
            xf + value.xf,
            yf + value.yf,
            scaleXf * value.scaleXf,
            scaleYf * value.scaleYf,
            rotation + value.rotation,
            skewX + value.skewX,
            skewY + value.skewY,
        )

        fun minus(value: Transform): Transform = setTo(
            xf - value.xf,
            yf - value.yf,
            scaleXf / value.scaleXf,
            scaleYf / value.scaleYf,
            rotation - value.rotation,
            skewX - value.skewX,
            skewY - value.skewY,
        )

        fun clone() = Transform().copyFrom(this)
    }

    class Computed(val matrix: Matrix, val transform: Transform) {
        companion object;
        constructor(matrix: Matrix) : this(matrix, Transform().setMatrix(matrix))
        constructor(transform: Transform) : this(transform.toMatrix(), transform)
    }

    override fun setToInterpolated(ratio: Double, l: Matrix, r: Matrix) = this.setTo(
        a = ratio.interpolate(l.af, r.af),
        b = ratio.interpolate(l.bf, r.bf),
        c = ratio.interpolate(l.cf, r.cf),
        d = ratio.interpolate(l.df, r.df),
        tx = ratio.interpolate(l.txf, r.txf),
        ty = ratio.interpolate(l.tyf, r.tyf)
    )

    override fun interpolateWith(ratio: Double, other: Matrix): Matrix =
        Matrix().setToInterpolated(ratio, this, other)

    inline fun <T> keep(callback: Matrix.() -> T): T {
        val a = this.af
        val b = this.bf
        val c = this.cf
        val d = this.df
        val tx = this.txf
        val ty = this.tyf
        try {
            return this.callback()
        } finally {
            this.af = a
            this.bf = b
            this.cf = c
            this.df = d
            this.txf = tx
            this.tyf = ty
        }
    }

    override fun toString(): String = "Matrix(a=$a, b=$b, c=$c, d=$d, tx=$tx, ty=$ty)"
}

