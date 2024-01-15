package korlibs.datastructure.internal.math

import korlibs.datastructure.internal.memory.Memory.countLeadingZeros
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

internal object Math {
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
    infix fun Int.divCeil(that: Int): Int = if (this % that != 0) (this / that) + 1 else (this / that)

    fun ilog2Ceil(v: Int): Int = ceil(log2(v.toDouble())).toInt()
}


internal fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean =
    (this - other).absoluteValue < epsilon

internal fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.00001f): Boolean =
    (this - other).absoluteValue < epsilon

internal val Double.niceStr: String get() = niceStr(-1, zeroSuffix = false)
internal fun Double.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = buildString {
    appendNice(
        this@niceStr.roundDecimalPlaces(decimalPlaces),
        zeroSuffix = zeroSuffix && decimalPlaces > 0
    )
}

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
