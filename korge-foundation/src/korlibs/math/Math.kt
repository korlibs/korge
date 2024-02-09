package korlibs.math

import korlibs.memory.*
import kotlin.math.*

const val PIF = PI.toFloat()
const val PI2F = (PI * 2).toFloat()

fun Double.betweenInclusive(min: Double, max: Double): Boolean = (this >= min) && (this <= max)

fun almostEquals(a: Float, b: Float) = almostZero(a - b)
fun almostZero(a: Float) = abs(a) <= 0.0000001

fun almostEquals(a: Double, b: Double) = almostZero(a - b)
fun almostZero(a: Double) = abs(a) <= 0.0000001


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



// @TODO: Optimize this
fun Int.numberOfDigits(radix: Int = 10): Int = radix.toString(radix).length
fun Long.numberOfDigits(radix: Int = 10): Int = radix.toString(radix).length

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
