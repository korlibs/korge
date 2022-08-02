package com.soywiz.kmem

import kotlin.math.*

////////////////////
////////////////////

/** Converts this [Boolean] into integer: 1 for true, 0 for false */
public inline fun Boolean.toInt(): Int = if (this) 1 else 0
public inline fun Boolean.toByte(): Byte = if (this) 1 else 0

public inline fun Byte.toBoolean(): Boolean = this.toInt() != 0

////////////////////
////////////////////

/** Converts [this] into [Int] rounding to the ceiling */
public fun Float.toIntCeil(): Int = ceil(this).toInt()
/** Converts [this] into [Int] rounding to the ceiling */
public fun Double.toIntCeil(): Int = ceil(this).toInt()

/** Converts [this] into [Int] rounding to the floor */
public fun Float.toIntFloor(): Int = floor(this).toInt()
/** Converts [this] into [Int] rounding to the floor */
public fun Double.toIntFloor(): Int = floor(this).toInt()

/** Converts [this] into [Int] rounding to the nearest */
public fun Float.toIntRound(): Int = round(this).toInt()
/** Converts [this] into [Int] rounding to the nearest */
public fun Double.toIntRound(): Int = round(this).toInt()

/** Convert this [Long] into an [Int] but throws an [IllegalArgumentException] in the case that operation would produce an overflow */
public fun Long.toIntSafe(): Int = if (this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) this.toInt() else throw IllegalArgumentException("Long doesn't fit Integer")

////////////////////
////////////////////

/** Returns an [Int] representing this [Byte] as if it was unsigned 0x00..0xFF */
public inline val Byte.unsigned: Int get() = this.toInt() and 0xFF

/** Returns a [Long] representing this [Int] as if it was unsigned 0x00000000L..0xFFFFFFFFL */
public inline val Int.unsigned: Long get() = this.toLong() and 0xFFFFFFFFL

////////////////////
////////////////////

/** Performs a fast integral logarithmic of base two */
public fun ilog2(v: Int): Int = if (v == 0) (-1) else (31 - v.countLeadingZeros())

////////////////////
////////////////////

/** Divides [this] into [that] rounding to the floor */
public infix fun Int.divFloor(that: Int): Int = this / that
/** Divides [this] into [that] rounding to the ceil */
public infix fun Int.divCeil(that: Int): Int = if (this % that != 0) (this / that) + 1 else (this / that)
/** Divides [this] into [that] rounding to the round */
public infix fun Int.divRound(that: Int): Int = (this.toDouble() / that.toDouble()).roundToInt()

////////////////////
////////////////////

/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
public fun Float.convertRange(srcMin: Float, srcMax: Float, dstMin: Float, dstMax: Float): Float = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
public fun Double.convertRange(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
public fun Int.convertRange(srcMin: Int, srcMax: Int, dstMin: Int, dstMax: Int): Int = (dstMin + (dstMax - dstMin) * ((this - srcMin).toDouble() / (srcMax - srcMin).toDouble())).toInt()
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
public fun Long.convertRange(srcMin: Long, srcMax: Long, dstMin: Long, dstMax: Long): Long = (dstMin + (dstMax - dstMin) * ((this - srcMin).toDouble() / (srcMax - srcMin).toDouble())).toLong()

/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
public fun Float.convertRangeClamped(srcMin: Float, srcMax: Float, dstMin: Float, dstMax: Float): Float = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
public fun Double.convertRangeClamped(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
public fun Int.convertRangeClamped(srcMin: Int, srcMax: Int, dstMin: Int, dstMax: Int): Int = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be clamped to the nearest bound */
public fun Long.convertRangeClamped(srcMin: Long, srcMax: Long, dstMin: Long, dstMax: Long): Long = convertRange(srcMin, srcMax, dstMin, dstMax).clamp(dstMin, dstMax)

////////////////////
////////////////////

/** Check if the absolute value of [this] floating point value is small (abs(this) <= 1e-19) */
public fun Float.isAlmostZero(): Boolean = abs(this) <= 1e-19
/** Check if the absolute value of [this] floating point value is small (abs(this) <= 1e-19) */
public fun Double.isAlmostZero(): Boolean = abs(this) <= 1e-19

/** Check if [this] floating point value is not a number or infinite */
public fun Float.isNanOrInfinite(): Boolean = this.isNaN() || this.isInfinite()
/** Check if [this] floating point value is not a number or infinite */
public fun Double.isNanOrInfinite(): Boolean = this.isNaN() || this.isInfinite()

////////////////////
////////////////////

/** Performs the unsigned modulo between [this] and [other] (negative values would wrap) */
public infix fun Int.umod(other: Int): Int {
    val rm = this % other
    val remainder = if (rm == -0) 0 else rm
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

/** Performs the unsigned modulo between [this] and [other] (negative values would wrap) */
public infix fun Double.umod(other: Double): Double {
    val rm = this % other
    val remainder = if (rm == -0.0) 0.0 else rm
    return when {
        remainder < 0.0 -> remainder + other
        else -> remainder
    }
}

public infix fun Float.umod(other: Float): Float {
    val rm = this % other
    val remainder = if (rm == -0f) 0f else rm
    return when {
        remainder < 0f -> remainder + other
        else -> remainder
    }
}

public inline fun fract(value: Float): Float = value - value.toIntFloor()
public inline fun fract(value: Double): Double = value - value.toIntFloor()


////////////////////
////////////////////

/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Int.nextAlignedTo(align: Int): Int = if (this.isAlignedTo(align)) this else (((this / align) + 1) * align)
/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Long.nextAlignedTo(align: Long): Long = if (this.isAlignedTo(align)) this else (((this / align) + 1) * align)
/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.nextAlignedTo(align: Double): Double = if (this.isAlignedTo(align)) this else (((this / align).toInt() + 1) * align)

/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Int.prevAlignedTo(align: Int): Int = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Long.prevAlignedTo(align: Long): Long = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.prevAlignedTo(align: Double): Double = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align

/** Returns whether [this] is multiple of [alignment] */
public fun Int.isAlignedTo(alignment: Int): Boolean = alignment == 0 || (this % alignment) == 0
/** Returns whether [this] is multiple of [alignment] */
public fun Long.isAlignedTo(alignment: Long): Boolean = alignment == 0L || (this % alignment) == 0L
/** Returns whether [this] is multiple of [alignment] */
public fun Double.isAlignedTo(alignment: Double): Boolean = alignment == 0.0 || (this % alignment) == 0.0

/** Returns the previous or next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.nearestAlignedTo(align: Double): Double {
    val prev = this.prevAlignedTo(align)
    val next = this.nextAlignedTo(align)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}


////////////////////
////////////////////

/** Clamps [this] value into the range [min] and [max] */
public fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
/** Clamps [this] value into the range [min] and [max] */
public fun Long.clamp(min: Long, max: Long): Long = if (this < min) min else if (this > max) max else this
/** Clamps [this] value into the range [min] and [max] */
public fun Double.clamp(min: Double, max: Double): Double = if (this < min) min else if (this > max) max else this
/** Clamps [this] value into the range [min] and [max] */
public fun Float.clamp(min: Float, max: Float): Float = if ((this < min)) min else if ((this > max)) max else this

/** Clamps [this] value into the range 0 and 1 */
public fun Double.clamp01(): Double = clamp(0.0, 1.0)
/** Clamps [this] value into the range 0 and 1 */
public fun Float.clamp01(): Float = clamp(0f, 1f)

/** Clamps [this] [Long] value into the range [min] and [max] converting it into [Int]. The default parameters will cover the whole range of values. */
public fun Long.toIntClamp(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
    if (this < min) return min
    if (this > max) return max
    return this.toInt()
}

/** Clamps [this] [Long] value into the range [min] and [max] converting it into [Int] (where [min] must be zero or positive). The default parameters will cover the whole range of positive and zero values. */
public fun Long.toUintClamp(min: Int = 0, max: Int = Int.MAX_VALUE): Int = this.toIntClamp(min, max)

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

////////////////////
////////////////////

/** Checks if [this] is odd (not multiple of two) */
public val Int.isOdd: Boolean get() = (this % 2) == 1
/** Checks if [this] is even (multiple of two) */
public val Int.isEven: Boolean get() = (this % 2) == 0

////////////////////
////////////////////

/** Returns the next power of two of [this] */
public val Int.nextPowerOfTwo: Int get() {
    var v = this
    v--
    v = v or (v shr 1)
    v = v or (v shr 2)
    v = v or (v shr 4)
    v = v or (v shr 8)
    v = v or (v shr 16)
    v++
    return v
}

/** Returns the previous power of two of [this] */
public val Int.prevPowerOfTwo: Int get() = if (isPowerOfTwo) this else (nextPowerOfTwo ushr 1)

/** Checks if [this] value is power of two */
public val Int.isPowerOfTwo: Boolean get() = this.nextPowerOfTwo == this
