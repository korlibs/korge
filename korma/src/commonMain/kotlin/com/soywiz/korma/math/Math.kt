package com.soywiz.korma.math

import kotlin.math.*

fun Long.clamp(min: Long, max: Long): Long = if (this < min) min else if (this > max) max else this
fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
fun Double.clamp(min: Double, max: Double): Double = if (this < min) min else if (this > max) max else this
fun Float.clamp(min: Float, max: Float): Float = if (this < min) min else if (this > max) max else this
fun Double.betweenInclusive(min: Double, max: Double): Boolean = (this >= min) && (this <= max)

/** Clamps the integer value in the 0..255 range */
fun Int.clampUByte(): Int {
    val n = this and -(if (this >= 0) 1 else 0)
    return (n or (0xFF - n shr 31)) and 0xFF
}
fun Int.clampUShort(): Int {
    val n = this and -(if (this >= 0) 1 else 0)
    return (n or (0xFFFF - n shr 31)) and 0xFFFF
}

fun Int.toShortClamped(): Short = this.clamp(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
fun Int.toByteClamped(): Byte = this.clamp(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()

fun almostEquals(a: Float, b: Float) = almostZero(a - b)
fun almostZero(a: Float) = abs(a) <= 0.0000001

fun almostEquals(a: Double, b: Double) = almostZero(a - b)
fun almostZero(a: Double) = abs(a) <= 0.0000001

fun Double.roundDecimalPlaces(places: Int): Double {
    val placesFactor: Double = 10.0.pow(places.toDouble())
    return kotlin.math.round(this * placesFactor) / placesFactor
}

fun isEquivalent(a: Double, b: Double, epsilon: Double = 0.0001): Boolean = (a - epsilon < b) && (a + epsilon > b)

fun Double.smoothstep(edge0: Double, edge1: Double): Double {
    val v = (this - edge0) / (edge1 - edge0)
    val step2 = v.clamp(0.0, 1.0)
    return step2 * step2 * (3 - 2 * step2)
}

fun Double.convertRange(minSrc: Double, maxSrc: Double, minDst: Double, maxDst: Double): Double = (((this - minSrc) / (maxSrc - minSrc)) * (maxDst - minDst)) + minDst

fun log(v: Int, base: Int): Int = log(v.toDouble(), base.toDouble()).toInt()
fun ln(v: Int): Int = ln(v.toDouble()).toInt()
fun log2(v: Int): Int = log(v.toDouble(), 2.0).toInt()
fun log10(v: Int): Int = log(v.toDouble(), 10.0).toInt()

fun signNonZeroM1(x: Double): Int = if (x <= 0) -1 else +1
fun signNonZeroP1(x: Double): Int = if (x >= 0) +1 else -1

fun Double.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-19
fun Double.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Float.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-19
fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Int.nextMultipleOf(multiple: Int) = if (this.isMultipleOf(multiple)) this else (((this / multiple) + 1) * multiple)
fun Long.nextMultipleOf(multiple: Long) = if (this.isMultipleOf(multiple)) this else (((this / multiple) + 1) * multiple)

fun Int.prevMultipleOf(multiple: Int) = if (this.isMultipleOf(multiple)) this else nextMultipleOf(multiple) - multiple
fun Long.prevMultipleOf(multiple: Long) = if (this.isMultipleOf(multiple)) this else nextMultipleOf(multiple) - multiple

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
