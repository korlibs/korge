package com.soywiz.korma.math

import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

fun Double.betweenInclusive(min: Double, max: Double): Boolean = (this >= min) && (this <= max)

fun almostEquals(a: Float, b: Float) = almostZero(a - b)
fun almostZero(a: Float) = abs(a) <= 0.0000001

fun almostEquals(a: Double, b: Double) = almostZero(a - b)
fun almostZero(a: Double) = abs(a) <= 0.0000001

fun Double.roundDecimalPlaces(places: Int): Double {
    if (places < 0) return this
    val placesFactor: Double = 10.0.pow(places.toDouble())
    return kotlin.math.round(this * placesFactor) / placesFactor
}
//fun Double.normalizeZero(): Double = if (this.isAlmostZero()) 0.0 else this
fun Double.normalizeZero(): Double = if (this == -0.0) 0.0 else this

fun isEquivalent(a: Double, b: Double, epsilon: Double = 0.0001): Boolean = (a - epsilon < b) && (a + epsilon > b)

fun Double.smoothstep(edge0: Double, edge1: Double): Double {
    if (this < edge0) return 0.0
    if (this >= edge1) return 1.0
    val v = ((this - edge0) / (edge1 - edge0))//.clamp(0.0, 1.0)
    return v * v * (3 - 2 * v)
}

fun Double.convertRange(minSrc: Double, maxSrc: Double, minDst: Double, maxDst: Double): Double = (((this - minSrc) / (maxSrc - minSrc)) * (maxDst - minDst)) + minDst

fun log(v: Int, base: Int): Int = log(v.toDouble(), base.toDouble()).toInt()
fun ln(v: Int): Int = ln(v.toDouble()).toInt()
fun log2(v: Int): Int = log(v.toDouble(), 2.0).toInt()
fun log10(v: Int): Int = log(v.toDouble(), 10.0).toInt()

fun signNonZeroM1(x: Double): Int = if (x <= 0) -1 else +1
fun signNonZeroP1(x: Double): Int = if (x >= 0) +1 else -1

//fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.0001): Boolean = (this - other).absoluteValue < epsilon
fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
fun Double.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-19
fun Double.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.000001f): Boolean = (this - other).absoluteValue < epsilon
fun Float.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-19
fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Double.closestMultipleOf(multiple: Double): Double {
    val prev = prevMultipleOf(multiple)
    val next = nextMultipleOf(multiple)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}
fun Int.closestMultipleOf(multiple: Int): Int {
    val prev = prevMultipleOf(multiple)
    val next = nextMultipleOf(multiple)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}
fun Long.closestMultipleOf(multiple: Long): Long {
    val prev = prevMultipleOf(multiple)
    val next = nextMultipleOf(multiple)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}

fun Double.nextMultipleOf(multiple: Double) = if (this.isMultipleOf(multiple)) this else (((this / multiple) + 1) * multiple)
fun Int.nextMultipleOf(multiple: Int) = if (this.isMultipleOf(multiple)) this else (((this / multiple) + 1) * multiple)
fun Long.nextMultipleOf(multiple: Long) = if (this.isMultipleOf(multiple)) this else (((this / multiple) + 1) * multiple)

fun Double.prevMultipleOf(multiple: Double) = if (this.isMultipleOf(multiple)) this else nextMultipleOf(multiple) - multiple
fun Int.prevMultipleOf(multiple: Int) = if (this.isMultipleOf(multiple)) this else nextMultipleOf(multiple) - multiple
fun Long.prevMultipleOf(multiple: Long) = if (this.isMultipleOf(multiple)) this else nextMultipleOf(multiple) - multiple

fun Double.isMultipleOf(multiple: Double) = multiple.isAlmostZero() || (this % multiple).isAlmostZero()
fun Int.isMultipleOf(multiple: Int) = multiple == 0 || (this % multiple) == 0
fun Long.isMultipleOf(multiple: Long) = multiple == 0L || (this % multiple) == 0L

fun min(a: Int, b: Int, c: Int) = min(min(a, b), c)
fun min(a: Float, b: Float, c: Float) = min(min(a, b), c)
fun min(a: Double, b: Double, c: Double) = min(min(a, b), c)

fun min(a: Int, b: Int, c: Int, d: Int) = min(min(min(a, b), c), d)
fun min(a: Float, b: Float, c: Float, d: Float) = min(min(min(a, b), c), d)
fun min(a: Double, b: Double, c: Double, d: Double) = min(min(min(a, b), c), d)

fun max(a: Int, b: Int, c: Int) = max(max(a, b), c)
fun max(a: Float, b: Float, c: Float) = max(max(a, b), c)
fun max(a: Double, b: Double, c: Double) = max(max(a, b), c)

fun max(a: Int, b: Int, c: Int, d: Int) = max(max(max(a, b), c), d)
fun max(a: Float, b: Float, c: Float, d: Float) = max(max(max(a, b), c), d)
fun max(a: Double, b: Double, c: Double, d: Double) = max(max(max(a, b), c), d)
