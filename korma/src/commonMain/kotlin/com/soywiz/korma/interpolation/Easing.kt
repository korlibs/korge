package com.soywiz.korma.interpolation

import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.math.clamp
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sin

private inline fun combine(it: Double, start: Easing, end: Easing) =
    if (it < 0.5) 0.5 * start(it * 2.0) else 0.5 * end((it - 0.5) * 2.0) + 0.5

private const val BOUNCE_FACTOR = 1.70158
private const val HALF_PI = PI / 2.0

@Suppress("unused")
fun interface Easing {
    operator fun invoke(it: Double): Double

    companion object {
        operator fun invoke(name: () -> String, block: (Double) -> Double): Easing {
            return object : Easing {
                override fun invoke(it: Double): Double = block(it)
                override fun toString(): String = name()
            }
        }

        fun steps(steps: Int, easing: Easing): Easing = Easing({ "steps($steps, $easing)" }) {
            easing((it * steps).toInt().toDouble() / steps)
        }
        fun cubic(x1: Double, y1: Double, x2: Double, y2: Double, name: String? = null): Easing =
            EasingCubic(x1, y1, x2, y2, name)

        fun cubic(f: (t: Double, b: Double, c: Double, d: Double) -> Double): Easing = Easing { f(it, 0.0, 1.0, 1.0) }
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
class EasingCubic(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val name: String? = null) : Easing {
    val cubic = Bezier.Cubic(0.0, 0.0, x1.clamp(0.0, 1.0), y1, x2.clamp(0.0, 1.0), y2, 1.0, 1.0)
    override fun toString(): String = name ?: "cubic-bezier($x1, $y1, $x2, $y2)"

    // @TODO: this doesn't work properly for `it` outside range [0, 1], and not in constant time
    override fun invoke(it: Double): Double {
        var pivotLeft = if (it < 0.0) it * 10.0 else 0.0
        var pivotRight = if (it > 1.0) it * 10.0 else 1.0
        //var pivot = (pivotLeft + pivotRight) * 0.5
        var pivot = it
        //println(" - x=$x, time=$time, pivotLeft=$pivotLeft, pivotRight=$pivotRight, pivot=$pivot")
        var lastX = 0.0
        var lastY = 0.0
        var steps = 0
        for (n in 0 until 50) {
            steps++
            val res = cubic.calc(pivot)
            lastX = res.x
            lastY = res.y
            if ((lastX - it).absoluteValue < 0.001) break
            if (it < lastX) {
                pivotRight = pivot
                pivot = (pivotLeft + pivot) * 0.5
            } else if (it > lastX) {
                pivotLeft = pivot
                pivot = (pivotRight + pivot) * 0.5
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
        override fun invoke(it: Double): Double = it * it * (3 - 2 * it)
    },
    EASE_IN_ELASTIC {
        override fun invoke(it: Double): Double =
            if (it == 0.0 || it == 1.0) {
                it
            } else {
                val p = 0.3
                val s = p / 4.0
                val inv = it - 1

                -1.0 * 2.0.pow(10.0 * inv) * sin((inv - s) * (2.0 * PI) / p)
            }
    },
    EASE_OUT_ELASTIC {
        override fun invoke(it: Double): Double =
            if (it == 0.0 || it == 1.0) {
                it
            } else {
                val p = 0.3
                val s = p / 4.0
                2.0.pow(-10.0 * it) * sin((it - s) * (2.0 * PI) / p) + 1
            }
    },
    EASE_OUT_BOUNCE {
        override fun invoke(it: Double): Double {
            val s = 7.5625
            val p = 2.75
            return when {
                it < 1.0 / p -> s * it.pow(2.0)
                it < 2.0 / p -> s * (it - 1.5 / p).pow(2.0) + 0.75
                it < 2.5 / p -> s * (it - 2.25 / p).pow(2.0) + 0.9375
                else -> s * (it - 2.625 / p).pow(2.0) + 0.984375
            }
        }
    },
    //Easing.cubic(0.0, 0.0, 1.0, 1.0, "linear"),
    LINEAR {
        override fun invoke(it: Double): Double = it
    },
    // https://developer.mozilla.org/en-US/docs/Web/CSS/animation-timing-function
    EASE {
        val easing = EasingCubic(0.25, 0.1, 0.25, 1.0, "ease")
        override fun invoke(it: Double): Double = easing.invoke(it)
    },
    EASE_IN {
        val easing = EasingCubic(0.42, 0.0, 1.0, 1.0, "ease-in")
        override fun invoke(it: Double): Double = easing.invoke(it)
    },
    EASE_OUT {
        val easing = EasingCubic(0.0, 0.0, 0.58, 1.0, "ease-out")
        override fun invoke(it: Double): Double = easing.invoke(it)
    },
    EASE_IN_OUT {
        val easing = EasingCubic(0.42, 0.0, 0.58, 1.0, "ease-in-out")
        override fun invoke(it: Double): Double = easing.invoke(it)
    },
    //EASE_OUT_IN {
    //    val easing = EasingCubic(-, "ease-out-in")
    //    override fun invoke(it: Double): Double = easing.invoke(it)
    //},
    EASE_IN_OLD {
        override fun invoke(it: Double): Double = it * it * it
    },
    EASE_OUT_OLD {
        override fun invoke(it: Double): Double =
            (it - 1.0).let { inv ->
                inv * inv * inv + 1
            }
    },
    EASE_IN_OUT_OLD {
        override fun invoke(it: Double): Double = combine(it, EASE_IN_OLD, EASE_OUT_OLD)
    },
    EASE_OUT_IN_OLD {
        override fun invoke(it: Double): Double = combine(it, EASE_OUT_OLD, EASE_IN_OLD)
    },
    EASE_IN_BACK {
        override fun invoke(it: Double): Double = it.pow(2.0) * ((BOUNCE_FACTOR + 1.0) * it - BOUNCE_FACTOR)
    },
    EASE_OUT_BACK {
        override fun invoke(it: Double): Double =
            (it - 1.0).let { inv ->
                inv.pow(2.0) * ((BOUNCE_FACTOR + 1.0) * inv + BOUNCE_FACTOR) + 1.0
            }
    },
    EASE_IN_OUT_BACK {
        override fun invoke(it: Double): Double = combine(it, EASE_IN_BACK, EASE_OUT_BACK)
    },
    EASE_OUT_IN_BACK {
        override fun invoke(it: Double): Double = combine(it, EASE_OUT_BACK, EASE_IN_BACK)
    },
    EASE_IN_OUT_ELASTIC {
        override fun invoke(it: Double): Double = combine(it, EASE_IN_ELASTIC, EASE_OUT_ELASTIC)
    },
    EASE_OUT_IN_ELASTIC {
        override fun invoke(it: Double): Double = combine(it, EASE_OUT_ELASTIC, EASE_IN_ELASTIC)
    },
    EASE_IN_BOUNCE {
        override fun invoke(it: Double): Double = 1.0 - EASE_OUT_BOUNCE(1.0 - it)
    },
    EASE_IN_OUT_BOUNCE {
        override fun invoke(it: Double): Double = combine(it, EASE_IN_BOUNCE, EASE_OUT_BOUNCE)
    },
    EASE_OUT_IN_BOUNCE {
        override fun invoke(it: Double): Double = combine(it, EASE_OUT_BOUNCE, EASE_IN_BOUNCE)
    },
    EASE_IN_QUAD {
        override fun invoke(it: Double): Double = 1.0 * it * it
    },
    EASE_OUT_QUAD {
        override fun invoke(it: Double): Double = -1.0 * it * (it - 2)
    },
    EASE_IN_OUT_QUAD {
        override fun invoke(it: Double): Double =
            (it * 2.0).let { t ->
                if (t < 1) {
                    1.0 / 2 * t * t
                } else {
                    -1.0 / 2 * ((t - 1) * ((t - 1) - 2) - 1)
                }
            }
    },
    EASE_SINE {
        override fun invoke(it: Double): Double = sin(it * HALF_PI)
    },
    EASE_CLAMP_START {
        override fun invoke(it: Double): Double = if (it <= 0.0) 0.0 else 1.0
    },
    EASE_CLAMP_END {
        override fun invoke(it: Double): Double = if (it < 1.0) 0.0 else 1.0
    },
    EASE_CLAMP_MIDDLE {
        override fun invoke(it: Double): Double = if (it < 0.5) 0.0 else 1.0
    };

    override fun toString(): String = super.toString().replace('_', '-').lowercase()
}
