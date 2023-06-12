package korlibs.datastructure.internal

import kotlin.math.*

internal val Double.niceStr: String get() = buildString { appendNice(this@niceStr) }
internal fun Double.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr

internal val Float.niceStr: String get() = this.toDouble().niceStr
internal fun Float.niceStr(decimalPlaces: Int): String = this.toDouble().niceStr(decimalPlaces)

//val Float.niceStr: String get() = buildString { appendNice(this@niceStr) }
//fun Float.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr

internal fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
internal fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.000001f): Boolean = (this - other).absoluteValue < epsilon

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
