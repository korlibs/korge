package korlibs.math

import korlibs.memory.*
import kotlin.math.*

fun Double.betweenInclusive(min: Double, max: Double): Boolean = (this >= min) && (this <= max)

fun almostEquals(a: Float, b: Float) = almostZero(a - b)
fun almostZero(a: Float) = abs(a) <= 0.0000001

fun almostEquals(a: Double, b: Double) = almostZero(a - b)
fun almostZero(a: Double) = abs(a) <= 0.0000001

fun Float.roundDecimalPlaces(places: Int): Float {
    if (places < 0) return this
    val placesFactor: Float = 10f.pow(places.toFloat())
    return round(this * placesFactor) / placesFactor
}

fun Double.roundDecimalPlaces(places: Int): Double {
    if (places < 0) return this
    val placesFactor: Double = 10.0.pow(places.toDouble())
    return kotlin.math.round(this * placesFactor) / placesFactor
}
//fun Double.normalizeZero(): Double = if (this.isAlmostZero()) 0.0 else this
private val MINUS_ZERO_D = -0.0
private val MINUS_ZERO_F = -0.0f
fun Double.normalizeZero(): Double = if (this == MINUS_ZERO_D) 0.0 else this
fun Float.normalizeZero(): Float = if (this == MINUS_ZERO_F) 0f else this

fun isEquivalent(a: Double, b: Double, epsilon: Double = 0.0001): Boolean = (a - epsilon < b) && (a + epsilon > b)

fun Double.smoothstep(edge0: Double, edge1: Double): Double {
    if (this < edge0) return 0.0
    if (this >= edge1) return 1.0
    val v = ((this - edge0) / (edge1 - edge0))//.clamp(0.0, 1.0)
    return v * v * (3 - 2 * v)
}

fun log(v: Int, base: Int): Int = log(v.toDouble(), base.toDouble()).toInt()
fun ln(v: Int): Int = ln(v.toDouble()).toInt()
fun log2(v: Int): Int = log(v.toDouble(), 2.0).toInt()
fun log10(v: Int): Int = log(v.toDouble(), 10.0).toInt()

@Deprecated("", ReplaceWith("v.squared()"))
fun sq(v: Int): Int = v.squared()
@Deprecated("", ReplaceWith("v.squared()"))
fun sq(v: Float): Float = v.squared()
@Deprecated("", ReplaceWith("v.squared()"))
fun sq(v: Double): Double = v.squared()

/** Signs of the value. Zero will be converted into -1 */
val Int.signM1: Int get() = signNonZeroM1(this)
/** Signs of the value. Zero will be converted into -1 */
val Float.signM1: Float get() = signNonZeroM1(this).toFloat()
/** Signs of the value. Zero will be converted into -1 */
val Double.signM1: Double get() = signNonZeroM1(this).toDouble()

/** Signs of the value. Zero will be converted into +1 */
val Int.signP1: Int get() = signNonZeroP1(this)
/** Signs of the value. Zero will be converted into +1 */
val Float.signP1: Float get() = signNonZeroP1(this).toFloat()
/** Signs of the value. Zero will be converted into +1 */
val Double.signP1: Double get() = signNonZeroP1(this).toDouble()

/** Signs of the value. Zero will be converted into -1 */
fun signNonZeroM1(x: Int): Int = if (x <= 0) -1 else +1
/** Signs of the value. Zero will be converted into -1 */
fun signNonZeroM1(x: Float): Int = if (x <= 0) -1 else +1
/** Signs of the value. Zero will be converted into -1 */
fun signNonZeroM1(x: Double): Int = if (x <= 0) -1 else +1


/** Signs of the value. Zero will be converted into +1 */
fun signNonZeroP1(x: Int): Int = if (x >= 0) +1 else -1
/** Signs of the value. Zero will be converted into +1 */
fun signNonZeroP1(x: Float): Int = if (x >= 0) +1 else -1
/** Signs of the value. Zero will be converted into +1 */
fun signNonZeroP1(x: Double): Int = if (x >= 0) +1 else -1

////////////////////
////////////////////

///** Check if the absolute value of [this] floating point value is small (abs(this) <= 1e-6) */
//public fun Float.isAlmostZero(): Boolean = abs(this) <= 1e-6
///** Check if the absolute value of [this] floating point value is small (abs(this) <= 1e-19) */
//public fun Double.isAlmostZero(): Boolean = abs(this) <= 1e-19
//
///** Check if [this] floating point value is not a number or infinite */
//public fun Float.isNanOrInfinite(): Boolean = this.isNaN() || this.isInfinite()
///** Check if [this] floating point value is not a number or infinite */
//public fun Double.isNanOrInfinite(): Boolean = this.isNaN() || this.isInfinite()


//fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.0001): Boolean = (this - other).absoluteValue < epsilon
fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
fun Double.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-19
fun Double.isNanOrInfinite() = this.isNaN() || this.isInfinite()

fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.00001f): Boolean = (this - other).absoluteValue < epsilon
fun Float.isAlmostZero(): Boolean = kotlin.math.abs(this) <= 1e-6
fun Float.isNanOrInfinite() = this.isNaN() || this.isInfinite()
fun Float.normalizeAlmostZero() = if (this.isAlmostZero()) 0f else this

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

fun Double.squared(): Double = this * this
fun Float.squared(): Float = this * this
fun Int.squared(): Int = this * this

fun min(a: Int, b: Int, c: Int) = min(min(a, b), c)
fun min(a: Float, b: Float, c: Float) = min(min(a, b), c)
fun min(a: Double, b: Double, c: Double) = min(min(a, b), c)

fun min(a: Int, b: Int, c: Int, d: Int) = min(min(min(a, b), c), d)
fun min(a: Float, b: Float, c: Float, d: Float) = min(min(min(a, b), c), d)
fun min(a: Double, b: Double, c: Double, d: Double) = min(min(min(a, b), c), d)

fun min(a: Int, b: Int, c: Int, d: Int, e: Int) = min(min(min(min(a, b), c), d), e)
fun min(a: Float, b: Float, c: Float, d: Float, e: Float) = min(min(min(min(a, b), c), d), e)
fun min(a: Double, b: Double, c: Double, d: Double, e: Double) = min(min(min(min(a, b), c), d), e)

fun max(a: Int, b: Int, c: Int) = max(max(a, b), c)
fun max(a: Float, b: Float, c: Float) = max(max(a, b), c)
fun max(a: Double, b: Double, c: Double) = max(max(a, b), c)

fun max(a: Int, b: Int, c: Int, d: Int) = max(max(max(a, b), c), d)
fun max(a: Float, b: Float, c: Float, d: Float) = max(max(max(a, b), c), d)
fun max(a: Double, b: Double, c: Double, d: Double) = max(max(max(a, b), c), d)

fun max(a: Int, b: Int, c: Int, d: Int, e: Int) = max(max(max(max(a, b), c), d), e)
fun max(a: Float, b: Float, c: Float, d: Float, e: Float) = max(max(max(max(a, b), c), d), e)
fun max(a: Double, b: Double, c: Double, d: Double, e: Double) = max(max(max(max(a, b), c), d), e)

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

/** Converts [this] into [Int] rounding to the nearest */
public fun Float.toLongRound(): Long = round(this).toLong()
/** Converts [this] into [Int] rounding to the nearest */
public fun Double.toLongRound(): Long = round(this).toLong()

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
// public fun ilog2(v: Int): Int = kotlin.math.log2(v.toDouble()).toInt()
public fun ilog2Ceil(v: Int): Int = kotlin.math.ceil(kotlin.math.log2(v.toDouble())).toInt()

////////////////////
////////////////////

/** Divides [this] into [that] rounding to the floor */
public infix fun Int.divFloor(that: Int): Int = this / that
/** Divides [this] into [that] rounding to the ceil */
public infix fun Int.divCeil(that: Int): Int = if (this % that != 0) (this / that) + 1 else (this / that)
/** Divides [this] into [that] rounding to the round */
public infix fun Int.divRound(that: Int): Int = (this.toDouble() / that.toDouble()).roundToInt()

public infix fun Long.divCeil(other: Long): Long {
    val res = this / other
    if (this % other != 0L) return res + 1
    return res
}

////////////////////
////////////////////

/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
public fun Float.convertRange(srcMin: Float, srcMax: Float, dstMin: Float, dstMax: Float): Float = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
/** Converts this value considering it was in the range [srcMin]..[srcMax] into [dstMin]..[dstMax], if the value is not inside the range the output value will be outside the destination range */
public fun Double.convertRange(srcMin: Double, srcMax: Double, dstMin: Double, dstMax: Double): Double = (dstMin + (dstMax - dstMin) * ((this - srcMin) / (srcMax - srcMin)))
//fun Double.convertRange(minSrc: Double, maxSrc: Double, minDst: Double, maxDst: Double): Double = (((this - minSrc) / (maxSrc - minSrc)) * (maxDst - minDst)) + minDst
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
    val remainder = if (rm == MINUS_ZERO_F) 0f else rm
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
public fun Float.nextAlignedTo(align: Float): Float = if (this.isAlignedTo(align)) this else (((this / align).toInt() + 1) * align)
/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.nextAlignedTo(align: Double): Double = if (this.isAlignedTo(align)) this else (((this / align).toInt() + 1) * align)

/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Int.prevAlignedTo(align: Int): Int = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Long.prevAlignedTo(align: Long): Long = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Float.prevAlignedTo(align: Float): Float = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.prevAlignedTo(align: Double): Double = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align

/** Returns whether [this] is multiple of [alignment] */
public fun Int.isAlignedTo(alignment: Int): Boolean = alignment == 0 || (this % alignment) == 0
/** Returns whether [this] is multiple of [alignment] */
public fun Long.isAlignedTo(alignment: Long): Boolean = alignment == 0L || (this % alignment) == 0L
/** Returns whether [this] is multiple of [alignment] */
public fun Float.isAlignedTo(alignment: Float): Boolean = alignment == 0f || (this % alignment) == 0f
/** Returns whether [this] is multiple of [alignment] */
public fun Double.isAlignedTo(alignment: Double): Boolean = alignment == 0.0 || (this % alignment) == 0.0

/** Returns the previous or next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Float.nearestAlignedTo(align: Float): Float {
    val prev = this.prevAlignedTo(align)
    val next = this.nextAlignedTo(align)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}
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

@PublishedApi
internal fun floorCeil(v: Double): Double = if (v < 0.0) ceil(v) else floor(v)

// @TODO: Check
internal fun Double.toInt2(): Int = if (this < 0.0) floor(this).toInt() else this.toInt()
internal fun Double.toIntMod(mod: Int): Int = (this umod mod.toDouble()).toInt2()

internal infix fun Int.div2(other: Int): Int = when {
    this < 0 || this % other == 0 -> this / other
    else -> (this / other) - 1
}

fun Int.cycle(min: Int, max: Int): Int = ((this - min) umod (max - min + 1)) + min
fun Int.cycleSteps(min: Int, max: Int): Int = (this - min) / (max - min + 1)
