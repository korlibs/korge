package com.soywiz.korio.util

import kotlin.math.*

object NumberParser {
	fun parseInt(str: String, start: Int = 0, end: Int = str.length, radix: Int = 10): Int {
		var positive = true
		var out = 0
		loop@ for (n in start until end) {
			val c = str[n]
			if (c == '-' || c == '+') {
				positive = (c == '+')
			} else {
				val value = c.ctypeAsInt()
				if (value < 0) break@loop
				out *= radix
				out += value
			}
		}
		return if (positive) out else -out
	}

	fun parseDouble(str: String, start: Int = 0, end: Int = str.length): Double {
		var out = 0.0
		var frac = 1.0
		var pointSeen = false
		var eSeen = false
		var negate = false
		var negateExponent = false
		var exponent = 0
		for (n in start until end) {
			val c = str[n]
			when (c) {
				'e', 'E' -> eSeen = true
				'-' -> {
					if (eSeen) negateExponent = true else negate = true
				}
				'.' -> pointSeen = true
				else -> {
					if (eSeen) {
						exponent *= 10
						exponent += c.ctypeAsInt()
					} else {
						if (pointSeen) frac /= 10
						out *= 10
						out += c.ctypeAsInt()
					}
				}
			}
		}
		val res = (out * frac) * 10.0.pow(if (negateExponent) -exponent else exponent)
		return if (negate) -res else res
	}
}

@Suppress("ConvertTwoComparisonsToRangeCheck") // @TODO: Kotlin-Native doesn't optimize ranges
private fun Char.ctypeAsInt(): Int = when {
	this >= '0' && this <= '9' -> this - '0'
	this >= 'a' && this <= 'z' -> this - 'a' + 10
	this >= 'A' && this <= 'Z' -> this - 'A' + 10
	else -> -1
}
