package korlibs.number

import kotlin.math.*

val Double.niceStr: String get() = niceStr(-1, zeroSuffix = false)
fun Double.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = buildString { appendNice(this@niceStr.roundDecimalPlaces(decimalPlaces), zeroSuffix = zeroSuffix && decimalPlaces > 0) }

val Float.niceStr: String get() = niceStr(-1, zeroSuffix = false)
fun Float.niceStr(decimalPlaces: Int, zeroSuffix: Boolean = false): String = buildString { appendNice(this@niceStr.roundDecimalPlaces(decimalPlaces), zeroSuffix = zeroSuffix && decimalPlaces > 0) }

fun StringBuilder.appendNice(value: Double, zeroSuffix: Boolean = false): Unit {
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
fun StringBuilder.appendNice(value: Float, zeroSuffix: Boolean = false): Unit {
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

//val Float.niceStr: String get() = buildString { appendNice(this@niceStr) }
//fun Float.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr
//val Float.niceStr: String get() = buildString { appendNice(this@niceStr) }
//fun Float.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr

/*
internal fun StringBuilder.appendNice(value: Double) {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toDouble() && value <= Int.MAX_VALUE.toDouble() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> append(value)
    }
}
internal fun StringBuilder.appendNice(value: Float) {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toFloat() && value <= Int.MAX_VALUE.toFloat() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> append(value)
    }
}
*/

fun Int.toStringUnsigned(radix: Int = 10): String = this.toUInt().toString(radix)
fun Long.toStringUnsigned(radix: Int = 10): String = this.toULong().toString(radix)

private fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
private fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.000001f): Boolean = (this - other).absoluteValue < epsilon

private fun Float.roundDecimalPlaces(places: Int): Float {
    if (places < 0) return this
    val placesFactor: Float = 10f.pow(places.toFloat())
    return round(this * placesFactor) / placesFactor
}

private fun Double.roundDecimalPlaces(places: Int): Double {
    if (places < 0) return this
    val placesFactor: Double = 10.0.pow(places.toDouble())
    return round(this * placesFactor) / placesFactor
}
