@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.interpolation

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.number.*
import kotlin.math.*

private inline fun combine(it: Float, start: Easing, end: Easing) =
    if (it < .5f) .5f * start(it * 2f) else .5f * end((it - .5f) * 2f) + .5f

private const val BOUNCE_FACTOR = 1.70158f
private const val HALF_PI = PI.toFloat() / 2f

@Suppress("unused")
fun interface Easing {
    operator fun invoke(it: Float): Float
    operator fun invoke(it: Double): Double = invoke(it.toFloat()).toDouble()
    operator fun invoke(it: Ratio): Ratio = Ratio(invoke(it.toFloat()).toDouble())

    companion object {
        operator fun invoke(name: () -> String, block: (Float) -> Float): Easing {
            return object : Easing {
                override fun invoke(it: Float): Float = block(it)
                override fun toString(): String = name()
            }
        }

        fun steps(steps: Int, easing: Easing): Easing = Easing({ "steps($steps, $easing)" }) {
            easing((it * steps).toInt().toFloat() / steps)
        }
        fun cubic(x1: Float, y1: Float, x2: Float, y2: Float, name: String? = null): Easing = EasingCubic(x1, y1, x2, y2, name)
        fun cubic(x1: Double, y1: Double, x2: Double, y2: Double, name: String? = null): Easing = EasingCubic(x1, y1, x2, y2, name)

        fun cubic(f: (t: Float, b: Float, c: Float, d: Float) -> Float): Easing = Easing { f(it, 0f, 1f, 1f) }
        fun combine(start: Easing, end: Easing) = Easing { combine(it, start, end) }

        private val _ALL_LIST: List<Easings> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Easings.values().toList()
        }

        val ALL_LIST: List<Easing> get() = _ALL_LIST

        /**
         * Retrieves a mapping of all standard easings defined directly in [Easing], for example "SMOOTH" -> Easing.SMOOTH.
         */
        val ALL: Map<String, Easing> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            _ALL_LIST.associateBy { it.name }
        }

        // Author's note:
        // 1. Make sure new standard easings are added both here and in the Easings enum class
        // 2. Make sure the name is the same, otherwise [ALL] will return confusing results

        val SMOOTH: Easing get() = Easings.SMOOTH
        val EASE_IN_ELASTIC: Easing get() = Easings.EASE_IN_ELASTIC
        val EASE_OUT_ELASTIC: Easing get() = Easings.EASE_OUT_ELASTIC
        val EASE_OUT_BOUNCE: Easing get() = Easings.EASE_OUT_BOUNCE
        val LINEAR: Easing get() = Easings.LINEAR
        val EASE: Easing get() = Easings.EASE
        val EASE_IN: Easing get() = Easings.EASE_IN
        val EASE_OUT: Easing get() = Easings.EASE_OUT
        val EASE_IN_OUT: Easing get() = Easings.EASE_IN_OUT
        val EASE_IN_OLD: Easing get() = Easings.EASE_IN_OLD
        val EASE_OUT_OLD: Easing get() = Easings.EASE_OUT_OLD
        val EASE_IN_OUT_OLD: Easing get() = Easings.EASE_IN_OUT_OLD
        val EASE_OUT_IN_OLD: Easing get() = Easings.EASE_OUT_IN_OLD
        val EASE_IN_BACK: Easing get() = Easings.EASE_IN_BACK
        val EASE_OUT_BACK: Easing get() = Easings.EASE_OUT_BACK
        val EASE_IN_OUT_BACK: Easing get() = Easings.EASE_IN_OUT_BACK
        val EASE_OUT_IN_BACK: Easing get() = Easings.EASE_OUT_IN_BACK
        val EASE_IN_OUT_ELASTIC: Easing get() = Easings.EASE_IN_OUT_ELASTIC
        val EASE_OUT_IN_ELASTIC: Easing get() = Easings.EASE_OUT_IN_ELASTIC
        val EASE_IN_BOUNCE: Easing get() = Easings.EASE_IN_BOUNCE
        val EASE_IN_OUT_BOUNCE: Easing get() = Easings.EASE_IN_OUT_BOUNCE
        val EASE_OUT_IN_BOUNCE: Easing get() = Easings.EASE_OUT_IN_BOUNCE
        val EASE_IN_QUAD: Easing get() = Easings.EASE_IN_QUAD
        val EASE_OUT_QUAD: Easing get() = Easings.EASE_OUT_QUAD
        val EASE_IN_OUT_QUAD: Easing get() = Easings.EASE_IN_OUT_QUAD
        val EASE_SINE: Easing get() = Easings.EASE_SINE
        val EASE_CLAMP_START: Easing get() = Easings.EASE_CLAMP_START
        val EASE_CLAMP_END: Easing get() = Easings.EASE_CLAMP_END
        val EASE_CLAMP_MIDDLE: Easing get() = Easings.EASE_CLAMP_MIDDLE
    }
}

// @TODO: We need to heavily optimize this. If we can have a formula instead of doing a bisect, this would be much faster.
class EasingCubic(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val name: String? = null) : Easing {
    constructor(x1: Double, y1: Double, x2: Double, y2: Double, name: String? = null) : this(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), name)
    val cubic = Bezier(Point(0f, 0f), Point(x1.clamp(0f, 1f), y1), Point(x2.clamp(0f, 1f), y2), Point(1.0, 1.0))
    override fun toString(): String = name ?: "cubic-bezier($x1, $y1, $x2, $y2)"

    // @TODO: this doesn't work properly for `it` outside range [0, 1], and not in constant time
    override fun invoke(it: Float): Float {
        var pivotLeft = if (it < 0f) it * 10f else 0f
        var pivotRight = if (it > 1f) it * 10f else 1f
        //var pivot = (pivotLeft + pivotRight) * 0.5
        var pivot = it
        //println(" - x=$x, time=$time, pivotLeft=$pivotLeft, pivotRight=$pivotRight, pivot=$pivot")
        var lastX = 0f
        var lastY = 0f
        var steps = 0
        for (n in 0 until 50) {
            steps++
            val res = cubic.calc(pivot.toRatio())
            lastX = res.x.toFloat()
            lastY = res.y.toFloat()
            if ((lastX - it).absoluteValue < 0.001) break
            if (it < lastX) {
                pivotRight = pivot
                pivot = (pivotLeft + pivot) * 0.5f
            } else if (it > lastX) {
                pivotLeft = pivot
                pivot = (pivotRight + pivot) * 0.5f
            } else {
                break
            }
        }
        //println("Requested steps=$steps, deviation=${(lastX - x).absoluteValue} requestedX=$x, lastX=$lastX, pivot=$pivot, pivotLeft=$pivotLeft, pivotRight=$pivotRight, lastY=$lastY")
        return lastY
    }

    /*
    override fun invoke(it: Double): Double {
        val points = listOf(cubic.p0, cubic.p1, cubic.p2, cubic.p3)
        val time = it

        /** Step 0 */
        val n = 5
        val x = doubleArrayOf(0.0, points[0].x, points[1].x, points[2].x, points[3].x, 1.0)
        val a = doubleArrayOf(0.0, points[0].y, points[1].y, points[2].y, points[3].y, 1.0)
        val h = DoubleArray(n)
        val A = DoubleArray(n)
        val l = DoubleArray(n + 1)
        val u = DoubleArray(n + 1)
        val z = DoubleArray(n + 1)
        val c = DoubleArray(n + 1)
        val b = DoubleArray(n)
        val d = DoubleArray(n)
        /** Step 1 */
        for (i in 0 until n) h[i] = x[i + 1] - x[i]
        /** Step 2 */
        for (i in 1 until n) A[i] = (3.0 * (a[i + 1] - a[i]) / h[i]) - (3.0 * (a[i] - a[i - 1]) / h[i - 1])
        /** Step 3 */
        l[0] = 1.0
        u[0] = 0.0
        z[0] = 0.0
        /** Step 4 */
        for (i in 1 until n) {
            l[i] = 2.0 * (x[i + 1] - x[i - 1]) - (h[i - 1] * u[i - 1])
            u[i] = h[i] / l[i]
            z[i] = (A[i] - h[i - 1] * z[i - 1]) / l[i]
        }
        /** Step 5 */
        l[n] = 1.0
        z[n] = 0.0
        c[n] = 0.0
        /** Step 6 */
        for (j in (n - 1) downTo 0) {
            c[j] = z[j] - (u[j] * c[j + 1])
            b[j] = ((a[j + 1] - a[j]) / h[j]) - (h[j] * (c[j + 1] + 2 * c[j]) / 3)
            d[j] = (c[j + 1] - c[j]) / (3 * h[j])
        }
        // get t position
        var result = 0.0
        var t = time
        for (i in 0 until n) {
            if (t >= x[i] && t < x[i + 1]) {
                t -= x[i]
                result = a[i] + b[i] * t + c[i] * t * t + d[i] * t * t * t
            }
        }
        return result
    }
    */
}

private enum class Easings : Easing {
    SMOOTH {
        override fun invoke(it: Float): Float = it * it * (3 - 2 * it)
    },
    EASE_IN_ELASTIC {
        override fun invoke(it: Float): Float =
            if (it == 0f || it == 1f) {
                it
            } else {
                val p = 0.3f
                val s = p / 4.0f
                val inv = it - 1
                -1f * 2f.pow(10f * inv) * sin((inv - s) * (2f * PI.toFloat()) / p)
            }
    },
    EASE_OUT_ELASTIC {
        override fun invoke(it: Float): Float =
            if (it == 0f || it == 1f) {
                it
            } else {
                val p = 0.3f
                val s = p / 4.0f
                2.0f.pow(-10.0f * it) * sin((it - s) * (2.0f * PI.toFloat()) / p) + 1
            }
    },
    EASE_OUT_BOUNCE {
        override fun invoke(it: Float): Float {
            val s = 7.5625f
            val p = 2.75f
            return when {
                it < 1f / p -> s * it.pow(2f)
                it < 2f / p -> s * (it - 1.5f / p).pow(2.0f) + 0.75f
                it < 2.5f / p -> s * (it - 2.25f / p).pow(2.0f) + 0.9375f
                else -> s * (it - 2.625f / p).pow(2.0f) + 0.984375f
            }
        }
    },
    //Easing.cubic(0.0, 0.0, 1.0, 1.0, "linear"),
    LINEAR {
        override fun invoke(it: Float): Float = it
    },
    // https://developer.mozilla.org/en-US/docs/Web/CSS/animation-timing-function
    EASE {
        val easing = EasingCubic(0.25f, 0.1f, 0.25f, 1.0f, "ease")
        override fun invoke(it: Float): Float = easing.invoke(it)
    },
    EASE_IN {
        val easing = EasingCubic(0.42f, 0.0f, 1.0f, 1.0f, "ease-in")
        override fun invoke(it: Float): Float = easing.invoke(it)
    },
    EASE_OUT {
        val easing = EasingCubic(0.0f, 0.0f, 0.58f, 1.0f, "ease-out")
        override fun invoke(it: Float): Float = easing.invoke(it)
    },
    EASE_IN_OUT {
        val easing = EasingCubic(0.42f, 0.0f, 0.58f, 1.0f, "ease-in-out")
        override fun invoke(it: Float): Float = easing.invoke(it)
    },
    //EASE_OUT_IN {
    //    val easing = EasingCubic(-, "ease-out-in")
    //    override fun invoke(it: Double): Double = easing.invoke(it)
    //},
    EASE_IN_OLD {
        override fun invoke(it: Float): Float = it * it * it
    },
    EASE_OUT_OLD {
        override fun invoke(it: Float): Float =
            (it - 1f).let { inv ->
                inv * inv * inv + 1
            }
    },
    EASE_IN_OUT_OLD {
        override fun invoke(it: Float): Float = combine(it, EASE_IN_OLD, EASE_OUT_OLD)
    },
    EASE_OUT_IN_OLD {
        override fun invoke(it: Float): Float = combine(it, EASE_OUT_OLD, EASE_IN_OLD)
    },
    EASE_IN_BACK {
        override fun invoke(it: Float): Float = it.pow(2f) * ((BOUNCE_FACTOR + 1f) * it - BOUNCE_FACTOR)
    },
    EASE_OUT_BACK {
        override fun invoke(it: Float): Float =
            (it - 1f).let { inv ->
                inv.pow(2f) * ((BOUNCE_FACTOR + 1f) * inv + BOUNCE_FACTOR) + 1f
            }
    },
    EASE_IN_OUT_BACK {
        override fun invoke(it: Float): Float = combine(it, EASE_IN_BACK, EASE_OUT_BACK)
    },
    EASE_OUT_IN_BACK {
        override fun invoke(it: Float): Float = combine(it, EASE_OUT_BACK, EASE_IN_BACK)
    },
    EASE_IN_OUT_ELASTIC {
        override fun invoke(it: Float): Float = combine(it, EASE_IN_ELASTIC, EASE_OUT_ELASTIC)
    },
    EASE_OUT_IN_ELASTIC {
        override fun invoke(it: Float): Float = combine(it, EASE_OUT_ELASTIC, EASE_IN_ELASTIC)
    },
    EASE_IN_BOUNCE {
        override fun invoke(it: Float): Float = 1f - EASE_OUT_BOUNCE(1f - it)
    },
    EASE_IN_OUT_BOUNCE {
        override fun invoke(it: Float): Float = combine(it, EASE_IN_BOUNCE, EASE_OUT_BOUNCE)
    },
    EASE_OUT_IN_BOUNCE {
        override fun invoke(it: Float): Float = combine(it, EASE_OUT_BOUNCE, EASE_IN_BOUNCE)
    },
    EASE_IN_QUAD {
        override fun invoke(it: Float): Float = 1f * it * it
    },
    EASE_OUT_QUAD {
        override fun invoke(it: Float): Float = -1f * it * (it - 2)
    },
    EASE_IN_OUT_QUAD {
        override fun invoke(it: Float): Float =
            (it * 2f).let { t ->
                if (t < 1) {
                    +1f / 2 * t * t
                } else {
                    -1f / 2 * ((t - 1) * ((t - 1) - 2) - 1)
                }
            }
    },
    EASE_SINE {
        override fun invoke(it: Float): Float = sin(it * HALF_PI)
    },
    EASE_CLAMP_START {
        override fun invoke(it: Float): Float = if (it <= 0f) 0f else 1f
    },
    EASE_CLAMP_END {
        override fun invoke(it: Float): Float = if (it < 1f) 0f else 1f
    },
    EASE_CLAMP_MIDDLE {
        override fun invoke(it: Float): Float = if (it < 0.5f) 0f else 1f
    };

    override fun toString(): String = super.toString().replace('_', '-').lowercase()
}



interface Interpolable<T> {
    fun interpolateWith(ratio: Ratio, other: T): T
}

interface MutableInterpolable<T> {
    fun setToInterpolated(ratio: Ratio, l: T, r: T): T
}

fun Ratio.interpolate(l: Vector2D, r: Vector2D): Vector2D = Vector2D(interpolate(l.x, r.x), interpolate(l.y, r.y))
fun Ratio.interpolate(l: Vector2F, r: Vector2F): Vector2F = Vector2F(interpolate(l.x, r.x), interpolate(l.y, r.y))
fun Ratio.interpolate(l: Size, r: Size): Size = Size(interpolate(l.width, r.width), interpolate(l.height, r.height))
fun Ratio.interpolate(l: Scale, r: Scale): Scale = Scale(interpolate(l.scaleX, r.scaleX), interpolate(l.scaleY, r.scaleY))
fun Ratio.interpolate(l: Float, r: Float): Float = (l + (r - l) * this.toFloat())
fun Ratio.interpolate(l: Double, r: Double): Double = (l + (r - l) * this.toDouble())
fun Ratio.interpolate(l: Ratio, r: Ratio): Ratio = (l + (r - l) * this)
fun Ratio.interpolate(l: Int, r: Int): Int = (l + (r - l) * this.toDouble()).toInt()
fun Ratio.interpolate(l: Long, r: Long): Long = (l + (r - l) * this.toDouble()).toLong()
fun <T> Ratio.interpolate(l: Interpolable<T>, r: Interpolable<T>): T = l.interpolateWith(this, r.fastCastTo<T>())
fun <T : Interpolable<T>> Ratio.interpolate(l: T, r: T): T = l.interpolateWith(this, r)

fun Ratio.interpolate(l: Matrix, r: Matrix): Matrix = Matrix.interpolated(l, r, this)
fun Ratio.interpolate(l: MatrixTransform, r: MatrixTransform): MatrixTransform = MatrixTransform.interpolated(l, r, this)
fun Ratio.interpolate(l: Rectangle, r: Rectangle): Rectangle = Rectangle.interpolated(l, r, this)


//inline class Ratio(val valueD: Double) : Comparable<Ratio> {
//    constructor(ratio: Float) : this(ratio.toDouble())
//    val value: Double get() = valueD
//    val valueF: Float get() = value.toFloat()
inline class Ratio(val value: Double) : Comparable<Ratio> {
    constructor(ratio: Float) : this(ratio.toDouble())

    fun toFloat(): Float = value.toFloat()
    fun toDouble(): Double = value.toDouble()

    constructor(value: Int, maximum: Int) : this(value.toFloat() / maximum.toFloat())
    constructor(value: Float, maximum: Float) : this(value / maximum)
    constructor(value: Double, maximum: Double) : this(value / maximum)

    operator fun unaryPlus(): Ratio = Ratio(+this.value)
    operator fun unaryMinus(): Ratio = Ratio(-this.value)
    operator fun plus(that: Ratio): Ratio = Ratio(this.value + that.value)
    operator fun minus(that: Ratio): Ratio = Ratio(this.value - that.value)

    operator fun times(that: Ratio): Ratio = Ratio(this.value * that.value)
    operator fun div(that: Ratio): Ratio = Ratio(this.value / that.value)
    operator fun times(that: Double): Double = (this.value * that)
    operator fun div(that: Double): Double = (this.value / that)

    val absoluteValue: Ratio get() = Ratio(value.absoluteValue)
    val clamped: Ratio get() = Ratio(value.clamp01())

    fun roundDecimalPlaces(places: Int): Ratio = Ratio(value.roundDecimalPlaces(places))

    fun convertToRange(min: Float, max: Float): Float = this.toFloat().convertRange(0f, 1f, min, max)
    fun convertToRange(min: Double, max: Double): Double = this.toDouble().convertRange(0.0, 1.0, min, max)
    fun convertToRange(min: Ratio, max: Ratio): Ratio = Ratio(this.toDouble().convertRange(0.0, 1.0, min.toDouble(), max.toDouble()))

    override fun compareTo(other: Ratio): Int = value.compareTo(other.value)

    fun isNaN(): Boolean = value.isNaN()

    override fun toString(): String = "$value"

    companion object {
        val ZERO = Ratio(0.0)
        val QUARTER = Ratio(.25)
        val HALF = Ratio(.5)
        val ONE = Ratio(1.0)
        val NaN = Ratio(Float.NaN)

        inline fun fromValueInRange(value: Number, min: Number, max: Number): Ratio =
            value.toDouble().convertRange(min.toDouble(), max.toDouble(), 0.0, 1.0).toRatio()

        inline fun fromValueInRangeClamped(value: Number, min: Number, max: Number): Ratio =
            value.toDouble().convertRangeClamped(min.toDouble(), max.toDouble(), 0.0, 1.0).toRatio()

        inline fun forEachRatio(steps: Int, include0: Boolean = true, include1: Boolean = true, block: (ratio: Ratio) -> Unit) {
            val NS = steps - 1
            val NSd = NS.toDouble()
            val start = if (include0) 0 else 1
            val end = if (include1) NS else NS - 1
            for (n in start..end) {
                val ratio = n.toFloat() / NSd
                block(ratio.toRatio())
            }
        }
    }
}

inline operator fun Float.times(ratio: Ratio): Float = (this * ratio.value).toFloat()
inline operator fun Double.times(ratio: Ratio): Double = this * ratio.value
inline operator fun Int.times(ratio: Ratio): Double = this.toDouble() * ratio.value
inline operator fun Float.div(ratio: Ratio): Float = (this / ratio.value).toFloat()
inline operator fun Double.div(ratio: Ratio): Double = this / ratio.value
inline operator fun Int.div(ratio: Ratio): Double = this.toDouble() / ratio.value

inline operator fun Ratio.times(value: Ratio): Ratio = Ratio(this.value * value.value)

inline operator fun Ratio.times(value: Float): Float = (this.value * value).toFloat()
inline operator fun Ratio.times(value: Double): Double = this.value * value
inline operator fun Ratio.div(value: Float): Float = (this.value / value).toFloat()
inline operator fun Ratio.div(value: Double): Double = this.value / value

@Deprecated("", ReplaceWith("this")) fun Ratio.toRatio(): Ratio = this

inline fun Number.toRatio(): Ratio = Ratio(this.toDouble())
fun Float.toRatio(): Ratio = Ratio(this)
fun Double.toRatio(): Ratio = Ratio(this)

inline fun Number.toRatio(max: Number): Ratio = Ratio(this.toDouble(), max.toDouble())
fun Float.toRatio(max: Float): Ratio = Ratio(this, max)
fun Double.toRatio(max: Double): Ratio = Ratio(this, max)

inline fun Number.toRatioClamped(): Ratio = Ratio(this.toDouble().clamp01())
fun Float.toRatioClamped(): Ratio = Ratio(this.clamp01())
fun Double.toRatioClamped(): Ratio = Ratio(this.clamp01())

fun Ratio.convertRange(srcMin: Ratio, srcMax: Ratio, dstMin: Ratio, dstMax: Ratio): Ratio = Ratio(this.toDouble().convertRange(srcMin.toDouble(), srcMax.toDouble(), dstMin.toDouble(), dstMax.toDouble()))
fun Ratio.isAlmostEquals(that: Ratio, epsilon: Ratio = Ratio(0.000001)): Boolean = this.toDouble().isAlmostEquals(that.toDouble(), epsilon.toDouble())
fun Ratio.isAlmostZero(epsilon: Ratio = Ratio(0.000001)): Boolean = this.isAlmostEquals(Ratio.ZERO, epsilon)

fun abs(a: Ratio): Ratio = Ratio(a.value.absoluteValue)
fun min(a: Ratio, b: Ratio): Ratio = Ratio(kotlin.math.min(a.value, b.value))
fun max(a: Ratio, b: Ratio): Ratio = Ratio(kotlin.math.max(a.value, b.value))
fun Ratio.clamp(min: Ratio, max: Ratio): Ratio = when {
    this < min -> min
    this > max -> max
    else -> this
}

val Ratio.niceStr: String get() = this.toDouble().niceStr
fun Ratio.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = this.toDouble().niceStr(decimalPlaces, zeroSuffix)
