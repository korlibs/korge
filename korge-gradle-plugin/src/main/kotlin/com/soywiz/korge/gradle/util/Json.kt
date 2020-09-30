package com.soywiz.korge.gradle.util

import java.io.*
import kotlin.math.*


object Json {
	fun parse(s: String): Any? = parse(StrReader(s))
	fun stringify(obj: Any?) = StringBuilder().apply { stringify(obj, this) }.toString()

	interface CustomSerializer {
		fun encodeToJson(b: StringBuilder)
	}

	private fun parse(s: StrReader): Any? = when (val ic = s.skipSpaces().read()) {
		'{' -> LinkedHashMap<String, Any?>().apply {
			obj@ while (true) {
				when (s.skipSpaces().read()) {
					'}' -> break@obj; ',' -> continue@obj; else -> s.unread()
				}
				val key = parse(s) as String
				s.skipSpaces().expect(':')
				val value = parse(s)
				this[key] = value
			}
		}
		'[' -> arrayListOf<Any?>().apply {
			array@ while (true) {
				when (s.skipSpaces().read()) {
					']' -> break@array; ',' -> continue@array; else -> s.unread()
				}
				val item = parse(s)
				this += item
			}
		}
		'-', '+', in '0'..'9' -> {
			s.unread()
			val res = s.readWhile { (it in '0'..'9') || it == '.' || it == 'e' || it == 'E' || it == '-' || it == '+' }
			val dres = res.toDouble()
			if (dres.toInt().toDouble() == dres) dres.toInt() else dres
		}
		't', 'f', 'n' -> {
			s.unread()
			when {
				s.tryRead("true") -> true
				s.tryRead("false") -> false
				s.tryRead("null") -> null
				else -> invalidJson()
			}
		}
		'"' -> {
			s.unread()
			s.readStringLit()
		}
		else -> invalidJson("Not expected '$ic'")
	}

	fun stringify(obj: Any?, b: StringBuilder) {
		when (obj) {
			null -> b.append("null")
			is Boolean -> b.append(if (obj) "true" else "false")
			is Map<*, *> -> {
				b.append('{')
				for ((i, v) in obj.entries.withIndex()) {
					if (i != 0) b.append(',')
					stringify(v.key, b)
					b.append(':')
					stringify(v.value, b)
				}
				b.append('}')
			}
			is Iterable<*> -> {
				b.append('[')
				for ((i, v) in obj.withIndex()) {
					if (i != 0) b.append(',')
					stringify(v, b)
				}
				b.append(']')
			}
			is Enum<*> -> encodeString(obj.name, b)
			is String -> encodeString(obj, b)
			is Number -> b.append("$obj")
			is CustomSerializer -> obj.encodeToJson(b)
			else -> throw RuntimeException("Don't know how to serialize $obj") //encode(ClassFactory(obj::class).toMap(obj), b)
		}
	}

	private fun encodeString(str: String) = StringBuilder().apply { encodeString(str, this) }.toString()

	private fun encodeString(str: String, b: StringBuilder) {
		b.append('"')
		for (c in str) {
			when (c) {
				'\\' -> b.append("\\\\"); '/' -> b.append("\\/"); '\'' -> b.append("\\'")
				'"' -> b.append("\\\""); '\b' -> b.append("\\b"); '\u000c' -> b.append("\\f")
				'\n' -> b.append("\\n"); '\r' -> b.append("\\r"); '\t' -> b.append("\\t")
				else -> b.append(c)
			}
		}
		b.append('"')
	}

	private fun invalidJson(msg: String = "Invalid JSON"): Nothing = throw IOException(msg)
}

fun String.fromJson(): Any? = Json.parse(this)
fun Map<*, *>.toJson(pretty: Boolean = false): String = Json.stringify(this)

private class StrReader(val str: String, val file: String = "file", var pos: Int = 0) {
	val length: Int = this.str.length
	val hasMore: Boolean get() = (this.pos < this.str.length)

	inline fun slice(action: () -> Unit): String? {
		val start = this.pos
		action()
		val end = this.pos
		return if (end > start) this.slice(start, end) else null
	}

	fun slice(start: Int, end: Int): String = this.str.substring(start, end)
	fun peek(count: Int): String = substr(this.pos, count)
	fun peekChar(): Char = if (hasMore) this.str[this.pos] else '\u0000'
	fun read(count: Int): String = this.peek(count).apply { skip(count) }
	inline fun skipWhile(filter: (Char) -> Boolean) = run { while (hasMore && filter(this.peekChar())) this.readChar() }

	inline fun readWhile(filter: (Char) -> Boolean) = this.slice { skipWhile(filter) } ?: ""
	fun unread(count: Int = 1) = this.apply { this.pos -= count; }
	fun readChar(): Char = if (hasMore) this.str[this.pos++] else '\u0000'
	fun read(): Char = if (hasMore) this.str[this.pos++] else '\u0000'

	fun readExpect(expected: String): String {
		val readed = this.read(expected.length)
		if (readed != expected) throw IllegalArgumentException("Expected '$expected' but found '$readed' at $pos")
		return readed
	}

	fun expect(expected: Char) = readExpect("$expected")
	fun skip(count: Int = 1) = this.apply { this.pos += count; }
	private fun substr(pos: Int, length: Int): String {
		return this.str.substring(min(pos, this.length), min(pos + length, this.length))
	}

	fun skipSpaces() = this.apply { this.skipWhile { it.isWhitespace() } }

	fun tryRead(str: String): Boolean {
		if (peek(str.length) == str) {
			skip(str.length)
			return true
		}
		return false
	}

	fun readStringLit(reportErrors: Boolean = true): String {
		val out = StringBuilder()
		val quotec = read()
		when (quotec) {
			'"', '\'' -> Unit
			else -> throw RuntimeException("Invalid string literal")
		}
		var closed = false
		while (hasMore) {
			val c = read()
			if (c == '\\') {
				val cc = read()
				out.append(
					when (cc) {
						'\\' -> '\\'; '/' -> '/'; '\'' -> '\''; '"' -> '"'
						'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
						'u' -> read(4).toInt(0x10).toChar()
						else -> throw IOException("Invalid char '$cc'")
					}
				)
			} else if (c == quotec) {
				closed = true
				break
			} else {
				out.append(c)
			}
		}
		if (!closed && reportErrors) {
			throw RuntimeException("String literal not closed! '${this.str}'")
		}
		return out.toString()
	}
}
