---
permalink: /math/utils/
group: math
layout: default
title: Math Utils
title_short: Math Utils
description: "Clamping, interpolation and easing"
fa-icon: fa-less-than-equal
priority: 10
---

KorMA provides some mathematical utilities.

## Math utils

### Clamping

```kotlin
fun Long.clamp(min: Long, max: Long): Long
fun Int.clamp(min: Int, max: Int): Int
fun Double.clamp(min: Double, max: Double): Double
fun Float.clamp(min: Float, max: Float): Float
```

### Interpolation

Korma defines two interfaces for interpolable classes and provides several extension methods for `Double` (the ratio between 0 and 1) to interpolate several kind of types.

```kotlin
interface Interpolable<T> {
    fun interpolateWith(ratio: Double, other: T): T
}

interface MutableInterpolable<T> {
    fun setToInterpolated(ratio: Double, l: T, r: T): T
}

fun <T> Double.interpolateAny(min: T, max: T): T
fun Double.interpolate(l: Float, r: Float): Float
fun Double.interpolate(l: Double, r: Double): Double
fun Double.interpolate(l: Int, r: Int): Int
fun Double.interpolate(l: Long, r: Long): Long
fun <T> Double.interpolate(l: Interpolable<T>, r: Interpolable<T>): T
fun <T : Interpolable<T>> Double.interpolate(l: T, r: T): T
```

### Easing

Korma defines some standard Easing functions and a way to include additional easing functions and combine them.

```kotlin
interface Easing {
    operator fun invoke(it: Double): Double

    companion object {
        operator fun invoke(f: (Double) -> Double) = object : Easing

        fun cubic(f: (t: Double, b: Double, c: Double, d: Double) -> Double): Easing
        fun combine(start: Easing, end: Easing): Easing

        val SMOOTH: Easing
        val EASE_IN_ELASTIC: Easing
        val EASE_OUT_ELASTIC: Easing
        val EASE_OUT_BOUNCE: Easing
        val LINEAR: Easing
        val EASE_IN: Easing
        val EASE_OUT: Easing
        val EASE_IN_OUT: Easing
        val EASE_OUT_IN: Easing
        val EASE_IN_BACK: Easing
        val EASE_OUT_BACK: Easing
        val EASE_IN_OUT_BACK: Easing
        val EASE_OUT_IN_BACK: Easing
        val EASE_IN_OUT_ELASTIC: Easing
        val EASE_OUT_IN_ELASTIC: Easing
        val EASE_IN_BOUNCE: Easing
        val EASE_IN_OUT_BOUNCE: Easing
        val EASE_OUT_IN_BOUNCE: Easing
        val EASE_IN_QUAD: Easing
        val EASE_OUT_QUAD: Easing
        val EASE_IN_OUT_QUAD: Easing
        val EASE_SINE: Easing
    }
}
```

