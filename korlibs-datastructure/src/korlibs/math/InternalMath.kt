package korlibs.math

import korlibs.memory.InternalMemory.countLeadingZeros
import kotlin.math.*

internal object InternalMath {
    /** Clamps the integer value in the 0..255 range */
    fun Int.clampUByte(): Int {
        val n = this and -(if (this >= 0) 1 else 0)
        return (n or (0xFF - n shr 31)) and 0xFF
    }

    fun Int.clampUShort(): Int {
        val n = this and -(if (this >= 0) 1 else 0)
        return (n or (0xFFFF - n shr 31)) and 0xFFFF
    }

    /** Returns an [Int] representing this [Byte] as if it was unsigned 0x00..0xFF */
    public inline val Byte.unsigned: Int get() = this.toInt() and 0xFF

    /** Returns an [Int] representing this [Short] as if it was unsigned 0x0000..0xFFFF */
    public inline val Short.unsigned: Int get() = this.toInt() and 0xFFFF

    /** Returns a [Long] representing this [Int] as if it was unsigned 0x00000000L..0xFFFFFFFFL */
    public inline val Int.unsigned: Long get() = this.toLong() and 0xFFFFFFFFL
    fun ilog2(v: Int): Int = if (v == 0) (-1) else (31 - v.countLeadingZeros())

    private val MINUS_ZERO_F = -0.0f
    /** Performs the unsigned modulo between [this] and [other] (negative values would wrap) */
    infix fun Int.umod(other: Int): Int {
        val rm = this % other
        val remainder = if (rm == -0) 0 else rm
        return when {
            remainder < 0 -> remainder + other
            else -> remainder
        }
    }

    /** Performs the unsigned modulo between [this] and [other] (negative values would wrap) */
    infix fun Double.umod(other: Double): Double {
        val rm = this % other
        val remainder = if (rm == -0.0) 0.0 else rm
        return when {
            remainder < 0.0 -> remainder + other
            else -> remainder
        }
    }

    infix fun Float.umod(other: Float): Float {
        val rm = this % other
        val remainder = if (rm == MINUS_ZERO_F) 0f else rm
        return when {
            remainder < 0f -> remainder + other
            else -> remainder
        }
    }

    /** Divides [this] into [that] rounding to the ceil */
    public infix fun Int.divCeil(that: Int): Int = if (this % that != 0) (this / that) + 1 else (this / that)


    interface IsAlmostEquals<T> {
        fun isAlmostEquals(other: T, epsilon: Double = 0.000001): Boolean
    }

    public fun ilog2Ceil(v: Int): Int = kotlin.math.ceil(kotlin.math.log2(v.toDouble())).toInt()
}


interface IsAlmostEqualsF<T> {
    fun isAlmostEquals(other: T, epsilon: Float = 0.0001f): Boolean
}

internal fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
internal fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.00001f): Boolean = (this - other).absoluteValue < epsilon
internal val Double.niceStr: String get() = niceStr(-1, zeroSuffix = false)
internal fun Double.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = buildString { appendNice(this@niceStr.roundDecimalPlaces(decimalPlaces), zeroSuffix = zeroSuffix && decimalPlaces > 0) }
internal fun StringBuilder.appendNice(value: Double, zeroSuffix: Boolean = false): Unit {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toDouble() && value <= Int.MAX_VALUE.toDouble() -> append(round(value).toInt())
            else -> append(round(value).toLong())
        }

        else -> {
            append(value)
            return
        }
    }
    if (zeroSuffix) append(".0")
}

internal fun Double.roundDecimalPlaces(places: Int): Double {
    if (places < 0) return this
    val placesFactor: Double = 10.0.pow(places.toDouble())
    return round(this * placesFactor) / placesFactor
}
