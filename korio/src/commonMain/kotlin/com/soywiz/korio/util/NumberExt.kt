@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import com.soywiz.kmem.isNanOrInfinite
import com.soywiz.korio.lang.*
import kotlin.math.*

fun Int.toStringUnsigned(radix: Int): String = this.toUInt().toString(radix)
fun Long.toStringUnsigned(radix: Int): String = this.toULong().toString(radix)

val Float.niceStr: String get() = if (round(this) == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (round(this) == this) "${this.toLong()}" else "$this"

fun Double.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String {
    if (this.isNanOrInfinite()) return this.toString()

	val res = this.toString()

	val containsEup = res.contains('E')
	val containsElo = res.contains('e')
	val rez = if (containsEup || containsElo) {
		val (base, exp) = res.split(if (containsEup) 'E' else 'e', limit = 2)
		val rbase = if (base.contains(".")) base else "$base.0"
		val expInt = exp.toInt()
		val zeros = "0".repeat(expInt.absoluteValue + 2)
		val part = if (expInt > 0) "$rbase$zeros" else "$zeros$rbase"
		val pointIndex = part.indexOf(".")
		val outIndex = pointIndex + expInt
		val part2 = part.replace(".", "")
		(part2.substr(0, outIndex) + "." + part2.substr(outIndex)).replace(Regex("^0+\\."), "0.")
	} else {
		res
	}

	val parts = rez.split('.', limit = 2)
	val integral = parts.getOrElse(0) { "0" }
	val decimal = parts.getOrElse(1) { "0" }
	if (decimalPlaces == 0) return integral
	var out = integral + "." + (decimal + "0".repeat(decimalPlaces)).substr(0, decimalPlaces)
	if (skipTrailingZeros) {
		while (out.endsWith('0')) out = out.substring(0, out.length - 1)
		if (out.endsWith('.')) out = out.substring(0, out.length - 1)
	}
	return out
}

fun Float.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String = this.toDouble().toStringDecimal(decimalPlaces, skipTrailingZeros)
