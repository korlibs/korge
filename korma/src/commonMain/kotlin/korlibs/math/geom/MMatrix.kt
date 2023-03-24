package korlibs.math.geom

import korlibs.datastructure.ConcurrentPool
import korlibs.math.annotations.KormaMutableApi
import korlibs.math.interpolation.Interpolable
import korlibs.math.interpolation.MutableInterpolable
import korlibs.math.interpolation.Ratio
import korlibs.math.interpolation.interpolate
import korlibs.math.math.isAlmostEquals
import kotlin.math.*

val MMatrix?.immutable: Matrix get() = if (this == null) Matrix.NIL else Matrix(a, b, c, d, tx, ty)

@KormaMutableApi
@Deprecated("Use Matrix")
data class MMatrix(
    var a: Double = 1.0,
    var b: Double = 0.0,
    var c: Double = 0.0,
    var d: Double = 1.0,
    var tx: Double = 0.0,
    var ty: Double = 0.0
) : MutableInterpolable<MMatrix>, Interpolable<MMatrix> {
    val immutable: Matrix get() = Matrix(a, b, c, d, tx, ty)

    companion object {
        val POOL: ConcurrentPool<MMatrix> = ConcurrentPool<MMatrix>({ it.identity() }) { MMatrix() }

        inline operator fun invoke(a: Float, b: Float = 0f, c: Float = 0f, d: Float = 1f, tx: Float = 0f, ty: Float = 0f) =
            MMatrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        inline operator fun invoke(a: Int, b: Int = 0, c: Int = 0, d: Int = 1, tx: Int = 0, ty: Int = 0) =
            MMatrix(a.toDouble(), b.toDouble(), c.toDouble(), d.toDouble(), tx.toDouble(), ty.toDouble())

        operator fun invoke(m: MMatrix, out: MMatrix = MMatrix()): MMatrix = out.copyFrom(m)

        @Deprecated("Use transform instead")
        fun transformXf(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, px: Float, py: Float): Float = a * px + c * py + tx
        @Deprecated("Use transform instead")
        fun transformYf(a: Float, b: Float, c: Float, d: Float, tx: Float, ty: Float, px: Float, py: Float): Float = d * py + b * px + ty

        fun isAlmostEquals(a: MMatrix, b: MMatrix, epsilon: Double = 0.000001): Boolean =
            a.tx.isAlmostEquals(b.tx, epsilon)
                && a.ty.isAlmostEquals(b.ty, epsilon)
                && a.a.isAlmostEquals(b.a, epsilon)
                && a.b.isAlmostEquals(b.b, epsilon)
                && a.c.isAlmostEquals(b.c, epsilon)
                && a.d.isAlmostEquals(b.d, epsilon)
    }

    fun isAlmostEquals(other: MMatrix, epsilon: Double = 0.000001): Boolean = isAlmostEquals(this, other, epsilon)

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

    fun copyFrom(that: Matrix): MMatrix = setTo(that.a, that.b, that.c, that.d, that.tx, that.ty)

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

    fun premultiply(m: Matrix) = this.premultiply(m.a, m.b, m.c, m.d, m.tx, m.ty)
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

    fun multiply(l: MMatrix, r: MMatrix): MMatrix = setTo(
        l.a * r.a + l.b * r.c,
        l.a * r.b + l.b * r.d,
        l.c * r.a + l.d * r.c,
        l.c * r.b + l.d * r.d,
        l.tx * r.a + l.ty * r.c + r.tx,
        l.tx * r.b + l.ty * r.d + r.ty
    )

    /** Transform point without translation */
    fun deltaTransformPoint(point: MPoint, out: MPoint = MPoint()) = deltaTransformPoint(point.x, point.y, out)
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

    fun invert(matrixToInvert: MMatrix = this): MMatrix {
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

    fun concat(value: MMatrix): MMatrix = this.multiply(this, value)
    fun preconcat(value: MMatrix): MMatrix = this.multiply(this, value)
    fun postconcat(value: MMatrix): MMatrix = this.multiply(value, this)

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

    operator fun times(that: MMatrix): MMatrix = MMatrix().multiply(this, that)
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


    // Transform points
    fun transform(p: Point): Point = Point(transformX(p.x, p.y), transformY(p.x, p.y))
    @Deprecated("")
    fun transform(p: MPoint, out: MPoint = MPoint()): MPoint = transform(p.x, p.y, out)
    @Deprecated("")
    fun transform(px: Double, py: Double, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))
    @Deprecated("")
    fun transform(px: Float, py: Float, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))
    @Deprecated("")
    fun transform(px: Int, py: Int, out: MPoint = MPoint()): MPoint = out.setTo(transformX(px, py), transformY(px, py))

    @Deprecated("")
    fun transformX(p: MPoint): Double = transformX(p.x, p.y)
    @Deprecated("")
    fun transformX(px: Double, py: Double): Double = this.a * px + this.c * py + this.tx
    @Deprecated("")
    fun transformX(px: Float, py: Float): Double = this.a * px + this.c * py + this.tx
    @Deprecated("")
    fun transformX(px: Int, py: Int): Double = this.a * px + this.c * py + this.tx

    @Deprecated("")
    fun transformY(p: MPoint): Double = transformY(p.x, p.y)
    @Deprecated("")
    fun transformY(px: Double, py: Double): Double = this.d * py + this.b * px + this.ty
    @Deprecated("")
    fun transformY(px: Float, py: Float): Double = this.d * py + this.b * px + this.ty
    @Deprecated("")
    fun transformY(px: Int, py: Int): Double = this.d * py + this.b * px + this.ty

    @Deprecated("")
    fun transformXf(p: MPoint): Float = transformX(p.x, p.y).toFloat()
    @Deprecated("")
    fun transformXf(px: Double, py: Double): Float = transformX(px, py).toFloat()
    @Deprecated("")
    fun transformXf(px: Float, py: Float): Float = transformX(px.toDouble(), py.toDouble()).toFloat()
    @Deprecated("")
    fun transformXf(px: Int, py: Int): Float = transformX(px.toDouble(), py.toDouble()).toFloat()

    @Deprecated("")
    fun transformYf(p: MPoint): Float = transformY(p.x, p.y).toFloat()
    @Deprecated("")
    fun transformYf(px: Double, py: Double): Float = transformY(px, py).toFloat()
    @Deprecated("")
    fun transformYf(px: Float, py: Float): Float = transformY(px.toDouble(), py.toDouble()).toFloat()
    @Deprecated("")
    fun transformYf(px: Int, py: Int): Float = transformY(px.toDouble(), py.toDouble()).toFloat()

    @Deprecated("Use MatrixTransform")
    data class Transform(
        var x: Double = 0.0, var y: Double = 0.0,
        var scaleX: Double = 1.0, var scaleY: Double = 1.0,
        var skewX: Angle = 0.radians, var skewY: Angle = 0.radians,
        var rotation: Angle = 0.radians
    ) : MutableInterpolable<Transform>, Interpolable<Transform> {
        val immutable: MatrixTransform get() = MatrixTransform(x, y, scaleX, scaleY, skewX, skewY, rotation)

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

        fun setMatrixNoReturn(matrix: MMatrix, pivotX: Double = 0.0, pivotY: Double = 0.0) {
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

        fun setMatrix(matrix: MMatrix, pivotX: Double = 0.0, pivotY: Double = 0.0): Transform {
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
