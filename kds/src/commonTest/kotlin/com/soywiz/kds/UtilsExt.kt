package com.soywiz.kds

import kotlin.math.*

fun Int.reverseBytes(): Int {
	val v0 = ((this ushr 0) and 0xFF)
	val v1 = ((this ushr 8) and 0xFF)
	val v2 = ((this ushr 16) and 0xFF)
	val v3 = ((this ushr 24) and 0xFF)
	return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
}

fun Short.reverseBytes(): Short {
	val low = ((this.toInt() ushr 0) and 0xFF)
	val high = ((this.toInt() ushr 8) and 0xFF)
	return ((high and 0xFF) or (low shl 8)).toShort()
}

fun Char.reverseBytes(): Char {
	val low = ((this.toInt() ushr 0) and 0xFF)
	val high = ((this.toInt() ushr 8) and 0xFF)
	return ((high and 0xFF) or (low shl 8)).toChar()
}

fun Long.reverseBytes(): Long {
	val v0 = (this ushr 0).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
	val v1 = (this ushr 32).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
	return (v0 shl 32) or (v1 shl 0)
}

private val formatRegex = Regex("%([-]?\\d+)?(\\w)")

fun String.format(vararg params: Any): String {
	var paramIndex = 0
	return formatRegex.replace(this) { mr ->
		val param = params[paramIndex++]
		//println("param: $param")
		val size = mr.groupValues[1]
		val type = mr.groupValues[2]
		val str = when (type) {
			"d" -> (param as Number).toLong().toString()
			"X", "x" -> {
				val res = when (param) {
					is Int -> param.toStringUnsigned(16)
					else -> (param as Number).toLong().toStringUnsigned(16)
				}
				if (type == "X") res.toUpperCase() else res.toLowerCase()
			}
			else -> "$param"
		}
		val prefix = if (size.startsWith('0')) '0' else ' '
		val asize = size.toIntOrNull()
		var str2 = str
		if (asize != null) {
			while (str2.length < asize) {
				str2 = prefix + str2
			}
		}
		str2
	}
}

fun String.splitKeep(regex: Regex): List<String> {
	val str = this
	val out = arrayListOf<String>()
	var lastPos = 0
	for (part in regex.findAll(this)) {
		val prange = part.range
		if (lastPos != prange.start) {
			out += str.substring(lastPos, prange.start)
		}
		out += str.substring(prange)
		lastPos = prange.endInclusive + 1
	}
	if (lastPos != str.length) {
		out += str.substring(lastPos)
	}
	return out
}


fun Long.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

fun Int.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0) {
		return "0"
	} else {
		var out = ""
		while (temp != 0) {
			val digit = temp % radix
			temp /= radix
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return if (isNegative) "-$rout" else rout
	}
}

fun Int.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0) {
		return "0"
	} else {
		var out = ""
		while (temp != 0) {
			val digit = temp urem radix
			temp = temp udiv radix
			out += Hex.DIGITS_UPPER[digit]
		}
		val rout = out.reversed()
		return rout
	}
}

fun Long.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0L) {
		return "0"
	} else {
		var out = ""
		while (temp != 0L) {
			val digit = temp urem radix.toLong()
			temp = temp udiv radix.toLong()
			out += Hex.DIGITS_UPPER[digit.toInt()]
		}
		val rout = out.reversed()
		return rout
	}
}

object Hex {
	val DIGITS = "0123456789ABCDEF"
	val DIGITS_UPPER = DIGITS.toUpperCase()
	val DIGITS_LOWER = DIGITS.toLowerCase()

	fun isHexDigit(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

	fun decode(str: String): ByteArray {
		val out = ByteArray(str.length / 2)
		for (n in 0 until out.size) {
			out[n] = (str.substring(n * 2, n * 2 + 2).toIntOrNull(16) ?: 0).toByte()
		}
		return out
	}

	fun encode(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)

	fun encodeLower(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)
	fun encodeUpper(src: ByteArray): String = encodeBase(src, DIGITS_UPPER)

	private fun encodeBase(data: ByteArray, digits: String = DIGITS): String {
		val out = StringBuilder(data.size * 2)
		for (n in data.indices) {
			val v = data[n].toInt() and 0xFF
			out.append(digits[(v ushr 4) and 0xF])
			out.append(digits[(v ushr 0) and 0xF])
		}
		return out.toString()
	}
}

infix fun Int.udiv(that: Int) = IntEx.divideUnsigned(this, that)
infix fun Int.urem(that: Int) = IntEx.remainderUnsigned(this, that)

infix fun Long.udiv(that: Long) = LongEx.divideUnsigned(this, that)
infix fun Long.urem(that: Long) = LongEx.remainderUnsigned(this, that)

object LongEx {
	val MIN_VALUE: Long = 0x7fffffffffffffffL.inv()
	val MAX_VALUE: Long = 0x7fffffffffffffffL

	fun compare(x: Long, y: Long): Int = if (x < y) -1 else if (x == y) 0 else 1
	fun compareUnsigned(x: Long, y: Long): Int = compare(x xor MIN_VALUE, y xor MIN_VALUE)

	fun divideUnsigned(dividend: Long, divisor: Long): Long {
		if (divisor < 0) return (if (compareUnsigned(dividend, divisor) < 0) 0 else 1).toLong()
		if (dividend >= 0) return dividend / divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return quotient + if (compareUnsigned(rem, divisor) >= 0) 1 else 0
	}

	fun remainderUnsigned(dividend: Long, divisor: Long): Long {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) dividend else dividend - divisor
		if (dividend >= 0) return dividend % divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return rem - if (compareUnsigned(rem, divisor) >= 0) divisor else 0
	}
}

object IntEx {
	private val MIN_VALUE = -0x80000000
	private val MAX_VALUE = 0x7fffffff

	fun compare(l: Int, r: Int): Int = if (l < r) -1 else if (l > r) 1 else 0
	fun compareUnsigned(l: Int, r: Int): Int = compare(l xor MIN_VALUE, r xor MIN_VALUE)
	fun divideUnsigned(dividend: Int, divisor: Int): Int {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) 0 else 1
		if (dividend >= 0) return dividend / divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return quotient + if (compareUnsigned(rem, divisor) >= 0) 1 else 0
	}

	fun remainderUnsigned(dividend: Int, divisor: Int): Int {
		if (divisor < 0) return if (compareUnsigned(dividend, divisor) < 0) dividend else dividend - divisor
		if (dividend >= 0) return dividend % divisor
		val quotient = dividend.ushr(1) / divisor shl 1
		val rem = dividend - quotient * divisor
		return rem - if (compareUnsigned(rem, divisor) >= 0) divisor else 0
	}
}
