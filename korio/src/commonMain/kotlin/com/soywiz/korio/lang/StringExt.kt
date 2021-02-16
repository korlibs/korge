package com.soywiz.korio.lang

import com.soywiz.kmem.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.internal.min2
import com.soywiz.korio.util.*

operator fun String.Companion.invoke(arrays: IntArray, offset: Int = 0, size: Int = arrays.size - offset): String {
	val sb = StringBuilder()
	for (n in offset until offset + size) {
		sb.append(arrays[n].toChar()) // @TODO: May not work the same! In JS: String.fromCodePoint
	}
	return sb.toString()
}

fun String_fromIntArray(arrays: IntArray, offset: Int = 0, size: Int = arrays.size - offset): String = String(arrays, offset, size)
fun String_fromCharArray(arrays: CharArray, offset: Int = 0, size: Int = arrays.size - offset): String = arrays.concatToString(offset, offset + size)

////////////////////////////////////
////////////////////////////////////

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

private val replaceNonPrintableCharactersRegex by lazy { Regex("[^ -~]") }
fun String.replaceNonPrintableCharacters(replacement: String = "?"): String {
	return this.replace(replaceNonPrintableCharactersRegex, replacement)
}

fun String.toBytez(len: Int, charset: Charset = UTF8): ByteArray {
	val out = ByteArrayBuilder()
	out.append(this.toByteArray(charset))
	while (out.size < len) out.append(0.toByte())
	return out.toByteArray()
}

fun String.toBytez(charset: Charset = UTF8): ByteArray {
	val out = ByteArrayBuilder()
	out.append(this.toByteArray(charset))
	out.append(0.toByte())
	return out.toByteArray()
}

fun String.indexOfOrNull(char: Char, startIndex: Int = 0): Int? = this.indexOf(char, startIndex).takeIf { it >= 0 }

fun String.lastIndexOfOrNull(char: Char, startIndex: Int = lastIndex): Int? =
	this.lastIndexOf(char, startIndex).takeIf { it >= 0 }

fun String.splitInChunks(size: Int): List<String> {
	val out = arrayListOf<String>()
	var pos = 0
	while (pos < this.length) {
		out += this.substring(pos, min2(this.length, pos + size))
		pos += size
	}
	return out
}

fun String.substr(start: Int): String = this.substr(start, this.length)

fun String.substr(start: Int, length: Int): String {
	val low = (if (start >= 0) start else this.length + start).clamp(0, this.length)
	val high = (if (length >= 0) low + length else this.length + length).clamp(0, this.length)
	return if (high >= low) this.substring(low, high) else ""
}

inline fun String.eachBuilder(transform: StringBuilder.(Char) -> Unit): String = buildString {
	@Suppress("ReplaceManualRangeWithIndicesCalls") // Performance reasons? Check that plain for doesn't allocate
	for (n in 0 until this@eachBuilder.length) transform(this, this@eachBuilder[n])
}

inline fun String.transform(transform: (Char) -> String): String = buildString {
	@Suppress("ReplaceManualRangeWithIndicesCalls") // Performance reasons? Check that plain for doesn't allocate
	for (n in 0 until this@transform.length) append(transform(this@transform[n]))
}

fun String.parseInt(): Int = when {
	this.startsWith("0x", ignoreCase = true) -> this.substring(2).toLong(16).toInt()
	this.startsWith("0o", ignoreCase = true) -> this.substring(2).toLong(8).toInt()
	this.startsWith("0b", ignoreCase = true) -> this.substring(2).toLong(2).toInt()
	else -> this.toInt()
}

val String.quoted: String get() = this.quote()

fun String.toCharArray() = CharArray(length) { this@toCharArray[it] }
