package korlibs.datastructure.internal

import korlibs.math.*
import kotlin.math.*

internal val Double.niceStr: String get() = niceStr(-1, zeroSuffix = false)
internal fun Double.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = buildString { appendNice(this@niceStr.roundDecimalPlaces(decimalPlaces), zeroSuffix = zeroSuffix && decimalPlaces > 0) }

internal val Float.niceStr: String get() = niceStr(-1, zeroSuffix = false)
internal fun Float.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = buildString { appendNice(this@niceStr.roundDecimalPlaces(decimalPlaces), zeroSuffix = zeroSuffix && decimalPlaces > 0) }

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

internal fun StringBuilder.appendNice(value: Float, zeroSuffix: Boolean = false): Unit {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toFloat() && value <= Int.MAX_VALUE.toFloat() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> {
            append(value)
            return
        }
    }
    if (zeroSuffix) append(".0")
}
