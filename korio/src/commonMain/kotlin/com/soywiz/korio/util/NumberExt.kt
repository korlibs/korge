@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import com.soywiz.kmem.isNanOrInfinite
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.round

fun Int.toStringUnsigned(radix: Int): String = this.toUInt().toString(radix)
fun Long.toStringUnsigned(radix: Int): String = this.toULong().toString(radix)

val Float.niceStr: String get() = if (round(this) == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (round(this) == this) "${this.toLong()}" else "$this"

fun Double.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String {
    if (this.isNanOrInfinite()) return this.toString()

    //val bits = this.toRawBits()
    //val sign = (bits ushr 63) != 0L
    //val exponent = (bits ushr 52) and 0b11111111111
    //val fraction = bits and ((1L shl 52) - 1L)

	val res = this.toString()

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
            append('.')
            val decimalCount = min(decimal.length, decimalPlaces)
            append(decimal, 0, decimalCount)
            if (!skipTrailingZeros) repeat(decimalPlaces - decimalCount) { append('0') }
        }
    }
}

fun Float.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String = this.toDouble().toStringDecimal(decimalPlaces, skipTrailingZeros)
