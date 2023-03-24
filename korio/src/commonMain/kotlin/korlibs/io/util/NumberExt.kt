@file:Suppress("NOTHING_TO_INLINE")

package korlibs.io.util

import korlibs.memory.isNanOrInfinite
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

fun Int.toStringUnsigned(radix: Int): String = this.toUInt().toString(radix)
fun Long.toStringUnsigned(radix: Int): String = this.toULong().toString(radix)

val Double.niceStr: String get() = buildString { appendNice(this@niceStr) }
fun Double.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr

val Float.niceStr: String get() = this.toDouble().niceStr
fun Float.niceStr(decimalPlaces: Int): String = this.toDouble().niceStr(decimalPlaces)

//val Float.niceStr: String get() = buildString { appendNice(this@niceStr) }
//fun Float.niceStr(decimalPlaces: Int): String = roundDecimalPlaces(decimalPlaces).niceStr

private fun Double.isAlmostEquals(other: Double, epsilon: Double = 0.000001): Boolean = (this - other).absoluteValue < epsilon
private fun Float.isAlmostEquals(other: Float, epsilon: Float = 0.000001f): Boolean = (this - other).absoluteValue < epsilon

fun StringBuilder.appendNice(value: Double) {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toDouble() && value <= Int.MAX_VALUE.toDouble() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> append(value)
    }
}
fun StringBuilder.appendNice(value: Float) {
    when {
        round(value).isAlmostEquals(value) -> when {
            value >= Int.MIN_VALUE.toFloat() && value <= Int.MAX_VALUE.toFloat() -> append(value.toInt())
            else -> append(value.toLong())
        }
        else -> append(value)
    }
}

//private fun Double.normalizeZero(): Double = if (this.isAlmostZero()) 0.0 else this
private val MINUS_ZERO_D = -0.0
private fun Double.normalizeZero(): Double = if (this == MINUS_ZERO_D) 0.0 else this

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

fun Double.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String {
    if (this.isNanOrInfinite()) return this.toString()

    //val bits = this.toRawBits()
    //val sign = (bits ushr 63) != 0L
    //val exponent = (bits ushr 52) and 0b11111111111
    //val fraction = bits and ((1L shl 52) - 1L)

	val res = this.roundDecimalPlaces(decimalPlaces).normalizeZero().toString()

	val eup = res.indexOf('E')
	val elo = res.indexOf('e')
    val eIndex = if (eup >= 0) eup else elo
	val rez = if (eIndex >= 0) {
        val base = res.substring(0, eIndex)
        val exp = res.substring(eIndex + 1).toInt()
        val rbase = if (base.contains(".")) base else "$base.0"
        val zeros = "0".repeat(exp.absoluteValue + 2)
		val part = if (exp > 0) "$rbase$zeros" else "$zeros$rbase"
		val pointIndex2 = part.indexOf(".")
        val pointIndex = if (pointIndex2 < 0) part.length else pointIndex2
		val outIndex = pointIndex + exp
		val part2 = part.replace(".", "")
        buildString {
            if ((0 until outIndex).all { part2[it] == '0' }) {
                append('0')
            } else {
                append(part2, 0, outIndex)
            }
            append('.')
            append(part2, outIndex, part2.length)
        }
	} else {
		res
	}

    val pointIndex = rez.indexOf('.')
	val integral = if (pointIndex >= 0) rez.substring(0, pointIndex) else rez
    if (decimalPlaces == 0) return integral

	val decimal = if (pointIndex >= 0) rez.substring(pointIndex + 1).trimEnd('0') else ""
    return buildString(2 + integral.length + decimalPlaces) {
        append(integral)
        if (decimal.isNotEmpty() || !skipTrailingZeros) {
            val decimalCount = min(decimal.length, decimalPlaces)
            val allZeros = (0 until decimalCount).all { decimal[it] == '0' }
            if (!skipTrailingZeros || !allZeros) {
                append('.')
                append(decimal, 0, decimalCount)
                if (!skipTrailingZeros) repeat(decimalPlaces - decimalCount) { append('0') }
            }
        }
    }
}

fun Float.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String = this.toDouble().toStringDecimal(decimalPlaces, skipTrailingZeros)