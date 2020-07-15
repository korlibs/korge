package com.soywiz.korma.interpolation

import kotlin.math.*

@Suppress("unused")
interface Easing {
    operator fun invoke(it: Double): Double

    companion object {
        fun cubic(f: (t: Double, b: Double, c: Double, d: Double) -> Double): Easing = Easing { f(it, 0.0, 1.0, 1.0) }
        fun combine(start: Easing, end: Easing) =
            Easing { if (it < 0.5) 0.5 * start(it * 2.0) else 0.5 * end((it - 0.5) * 2.0) + 0.5 }

        operator fun invoke(f: (Double) -> Double) = object : Easing {
            override fun invoke(it: Double): Double = f(it)
        }

        val SMOOTH get() = Easings.SMOOTH
        val EASE_IN_ELASTIC get() = Easings.EASE_IN_ELASTIC
        val EASE_OUT_ELASTIC get() = Easings.EASE_OUT_ELASTIC
        val EASE_OUT_BOUNCE get() = Easings.EASE_OUT_BOUNCE
        val LINEAR get() = Easings.LINEAR
        val EASE_IN get() = Easings.EASE_IN
        val EASE_OUT get() = Easings.EASE_OUT
        val EASE_IN_OUT get() = Easings.EASE_IN_OUT
        val EASE_OUT_IN get() = Easings.EASE_OUT_IN
        val EASE_IN_BACK get() = Easings.EASE_IN_BACK
        val EASE_OUT_BACK get() = Easings.EASE_OUT_BACK
        val EASE_IN_OUT_BACK get() = Easings.EASE_IN_OUT_BACK
        val EASE_OUT_IN_BACK get() = Easings.EASE_OUT_IN_BACK
        val EASE_IN_OUT_ELASTIC get() = Easings.EASE_IN_OUT_ELASTIC
        val EASE_OUT_IN_ELASTIC get() = Easings.EASE_OUT_IN_ELASTIC
        val EASE_IN_BOUNCE get() = Easings.EASE_IN_BOUNCE
        val EASE_IN_OUT_BOUNCE get() = Easings.EASE_IN_OUT_BOUNCE
        val EASE_OUT_IN_BOUNCE get() = Easings.EASE_OUT_IN_BOUNCE
        val EASE_IN_QUAD get() = Easings.EASE_IN_QUAD
        val EASE_OUT_QUAD get() = Easings.EASE_OUT_QUAD
        val EASE_IN_OUT_QUAD get() = Easings.EASE_IN_OUT_QUAD
        val EASE_SINE get() = Easings.EASE_SINE
    }
}

private object Easings {
    private const val BOUNCE_10 = 1.70158

    val SMOOTH = Easing { it * it * (3 - 2 * it) }

    val EASE_IN_ELASTIC = Easing {
        if (it == 0.0 || it == 1.0) it else {
            val p = 0.3
            val s = p / 4.0
            val inv = it - 1
            -1.0 * 2.0.pow(10.0 * inv) * sin((inv - s) * (2.0 * PI) / p)
        }
    }

    val EASE_OUT_ELASTIC = Easing {
        if (it == 0.0 || it == 1.0) it else {
            val p = 0.3
            val s = p / 4.0
            2.0.pow(-10.0 * it) * sin((it - s) * (2.0 * PI) / p) + 1
        }
    }

    val EASE_OUT_BOUNCE = Easing {
        val s = 7.5625
        val p = 2.75
        when {
            it < (1.0 / p) -> s * it.pow(2.0)
            it < (2.0 / p) -> s * (it - 1.5 / p).pow(2.0) + 0.75
            it < 2.5 / p -> s * (it - 2.25 / p).pow(2.0) + 0.9375
            else -> s * (it - 2.625 / p).pow(2.0) + 0.984375
        }
    }

    val LINEAR = Easing { it }
    val EASE_IN = Easing { it * it * it }
    val EASE_OUT = Easing { val inv = it - 1.0; inv * inv * inv + 1 }
    val EASE_IN_OUT = Easing.combine(EASE_IN, EASE_OUT)
    val EASE_OUT_IN = Easing.combine(EASE_OUT, EASE_IN)
    val EASE_IN_BACK = Easing { it.pow(2.0) * ((BOUNCE_10 + 1.0) * it - BOUNCE_10) }
    val EASE_OUT_BACK = Easing { val inv = it - 1.0; inv.pow(2.0) * ((BOUNCE_10 + 1.0) * inv + BOUNCE_10) + 1.0 }
    val EASE_IN_OUT_BACK = Easing.combine(EASE_IN_BACK, EASE_OUT_BACK)
    val EASE_OUT_IN_BACK = Easing.combine(EASE_OUT_BACK, EASE_IN_BACK)
    val EASE_IN_OUT_ELASTIC = Easing.combine(EASE_IN_ELASTIC, EASE_OUT_ELASTIC)
    val EASE_OUT_IN_ELASTIC = Easing.combine(EASE_OUT_ELASTIC, EASE_IN_ELASTIC)
    val EASE_IN_BOUNCE = Easing { 1.0 - EASE_OUT_BOUNCE(1.0 - it) }
    val EASE_IN_OUT_BOUNCE = Easing.combine(EASE_IN_BOUNCE, EASE_OUT_BOUNCE)
    val EASE_OUT_IN_BOUNCE = Easing.combine(EASE_OUT_BOUNCE, EASE_IN_BOUNCE)
    val EASE_IN_QUAD = Easing { 1.0 * it * it }
    val EASE_OUT_QUAD = Easing { -1.0 * it * (it - 2) }
    val EASE_IN_OUT_QUAD =
        Easing { val t = it * 2.0; if (t < 1) (1.0 / 2 * t * t) else (-1.0 / 2 * ((t - 1) * ((t - 1) - 2) - 1)) }

    val EASE_SINE = Easing { sin(it) }
}
