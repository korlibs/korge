package com.soywiz.korio.serialization.json

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.collections.set

object Json {
	fun parse(s: String, context: Context = Context.DEFAULT): Any? = parse(StrReader(s), context)
	fun stringify(obj: Any?, pretty: Boolean = false) = when {
		pretty -> Indenter().apply { stringifyPretty(obj, this) }.toString(doIndent = true, indentChunk = "\t")
		else -> StringBuilder().apply { stringify(obj, this) }.toString()
	}

	interface CustomSerializer {
		fun encodeToJson(b: StringBuilder)
	}

    class Context(val optimizedNumericLists: Boolean) {
        companion object {
            val DEFAULT = Context(optimizedNumericLists = false)
        }
    }

	fun parse(s: StrReader, context: Context = Context.DEFAULT): Any? = when (val ic = s.skipSpaces().read()) {
		'{' -> LinkedHashMap<String, Any?>().apply {
			obj@ while (true) {
				when (s.skipSpaces().read()) {
					'}' -> break@obj; ',' -> continue@obj; else -> s.unread()
				}
				val key = parse(s, context) as String
				s.skipSpaces().skipExpect(':')
				val value = parse(s, context)
				this[key] = value
			}
		}
		'[' -> {
            var out: ArrayList<Any?>? = null
            var outNumber: DoubleArrayList? = null
            array@ while (true) {
                when (s.skipSpaces().read()) {
                    ']' -> break@array; ',' -> continue@array; else -> s.unread()
                }
                if (out == null && context.optimizedNumericLists && s.peek() in '0'..'9') {
                    if (outNumber == null) {
                        outNumber = DoubleArrayList()
                    }
                    outNumber.add(parseNumber(s))
                } else {
                    if (out == null) out = arrayListOf()
                    if (outNumber != null) {
                        outNumber.fastForEach { out.add(it) }
                        outNumber = null
                    }
                    out.add(parse(s, context))
                }
            }
            outNumber ?: out ?: arrayListOf<Any?>()
        }
		//'-', '+', in '0'..'9' -> { // @TODO: Kotlin native doesn't optimize char ranges
		'-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
			s.unread()
			val dres = parseNumber(s)
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

	private fun Char.isNumberStart() = when (this) {
		'-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true
		else -> false
	}

	private fun parseNumber(s: StrReader): Double {
		val start = s.pos
		s.skipWhile { ((it >= '0') && (it <= '9')) || it == '.' || it == 'e' || it == 'E' || it == '-' || it == '+' }
		val end = s.pos
		return NumberParser.parseDouble(s.str, start, end)
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
			else -> invalidOp("Don't know how to serialize $obj") //encode(ClassFactory(obj::class).toMap(obj), b)
		}
	}

	fun stringifyPretty(obj: Any?, b: Indenter) {
		when (obj) {
			null -> b.inline("null")
			is Boolean -> b.inline(if (obj) "true" else "false")
			is Map<*, *> -> {
				b.line("{")
				b.indent {
					val entries = obj.entries
					for ((i, v) in entries.withIndex()) {
						if (i != 0) b.line(",")
						b.inline(encodeString("" + v.key))
						b.inline(": ")
						stringifyPretty(v.value, b)
						if (i == entries.size - 1) b.line("")
					}
				}
				b.inline("}")
			}
			is Iterable<*> -> {
				b.line("[")
				b.indent {
					val entries = obj.toList()
					for ((i, v) in entries.withIndex()) {
						if (i != 0) b.line(",")
						stringifyPretty(v, b)
						if (i == entries.size - 1) b.line("")
					}
				}
				b.inline("]")
			}
			is String -> b.inline(encodeString(obj))
			is Number -> b.inline("$obj")
			is CustomSerializer -> b.inline(StringBuilder().apply { obj.encodeToJson(this) }.toString())
			else -> {
				invalidOp("Don't know how to serialize $obj")
				//encode(ClassFactory(obj::class).toMap(obj), b)
			}
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
fun Map<*, *>.toJson(pretty: Boolean = false): String = Json.stringify(this, pretty)
