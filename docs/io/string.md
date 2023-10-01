---
permalink: /io/string/
group: io
layout: default
title: String Utils
title_prefix: KorIO
description: "UUID, Indenter, StrReader, Number parsing and stringifying tools, String extensions"
fa-icon: fa-text-width
priority: 11
---

KorIO has utilities for handling texts.



## Indenter

```kotlin
fun Indenter(init: Indenter.() -> Unit): Indenter
fun Indenter(str: String): Indenter

class Indenter(internal val actions: ArrayList<Action> = arrayListOf()) {
    companion object {
        fun genString(init: Indenter.() -> Unit) = gen(init).toString()
        val EMPTY: Indenter
        inline fun gen(init: Indenter.() -> Unit): Indenter
        fun single(str: String): Indenter

        fun replaceString(templateString: String, replacements: Map<String, String>): String
    }

    object INDENTS {
        operator fun get(index: Int): String
    }

    interface Action {
        interface Text : Action { val str: String }
        data class Marker(val data: Any) : Action
        data class Inline(override val str: String) : Text
        data class Line(override val str: String) : Text
        data class LineDeferred(val callback: () -> Indenter) : Action
        object EmptyLineOnce : Action
        object Indent : Action
        object Unindent : Action
    }

    fun inline(str: String): Indenter
    fun line(indenter: Indenter): Indenter
    fun line(str: String): Indenter
    fun line(str: String?)
    fun mark(data: Any): Indenter
    fun linedeferred(init: Indenter.() -> Unit): Indenter

    inline fun line(str: String, callback: () -> Unit): Indenter
    inline fun line(str: String, after: String = "", after2: String = "", callback: () -> Unit): Indenter	inline fun indent(callback: () -> Unit): Indenter

    inline operator fun String.invoke(suffix: String = "", callback: () -> Unit)
    inline operator fun String.unaryPlus() = line(this)

    inline fun String.xml(callback: () -> Unit)

    fun toString(markHandler: ((sb: StringBuilder, line: Int, data: Any) -> Unit)?, doIndent: Boolean): String
    fun toString(markHandler: ((sb: StringBuilder, line: Int, data: Any) -> Unit)?): String
    fun toString(doIndent: Boolean = true, indentChunk: String = "\t"): String
    override fun toString(): String = toString(null, doIndent = true)
}

val Indenter.SEPARATOR: Unit
fun Indenter.EMPTY_LINE_ONCE()L Unit
fun Indenter.SEPARATOR(callback: Indenter.() -> Unit)

class XmlIndenter(val indenter: Indenter) {
    inline operator fun String.invoke(callback: () -> Unit)
}

fun Indenter.xml(callback: XmlIndenter.() -> Unit)
```

## StrReader

```kotlin
fun String.reader(file: String = "file", pos: Int = 0): StrReader = StrReader(this, file, pos)

class StrReader(val str: String, val file: String = "file", var pos: Int = 0) {
	companion object {
		fun literals(vararg lits: String): Literals
	}

	val length: Int
	val available: Int
	val eof: Boolean
	val hasMore: Boolean

	fun reset()
	fun createRange(range: IntRange): TRange
	fun createRange(start: Int = this.pos, end: Int = this.pos): TRange

	fun readRange(length: Int): TRange
	inline fun slice(action: () -> Unit): String?

	fun slice(start: Int, end: Int): String
	fun peek(count: Int): String
	fun peek(): Char
	fun peekChar(): Char
	fun read(count: Int): String
	fun skipUntil(char: Char)
	fun skipUntilIncluded(char: Char)
	inline fun skipWhile(filter: (Char) -> Boolean)
	inline fun skipUntil(filter: (Char) -> Boolean)
	inline fun matchWhile(check: (Char) -> Boolean): String?
	fun readUntil(char: Char): String?
	fun readUntilIncluded(char: Char): String?
	inline fun readWhile(filter: (Char) -> Boolean): String?
	inline fun readUntil(filter: (Char) -> Boolean): String?
	fun unread(count: Int = 1): StrReader
	fun readChar(): Char
	fun read(): Char
	fun readRemaining(): String
	fun readExpect(expected: String): String
	fun skipExpect(expected: Char)
	fun expect(expected: Char): String
	fun skip(count: Int = 1): StrReader
	fun tryLit(lit: String): String?
	fun tryLitRange(lit: String): TRange?
	fun matchLit(lit: String): String?
	fun matchLitRange(lit: String): TRange?
	fun matchLitListRange(lits: Literals): TRange?
	fun skipSpaces(): StrReader

	fun matchIdentifier(): String?
	fun matchSingleOrDoubleQuoteString(): String?
	fun tryRegex(v: Regex): String?
	fun tryRegexRange(v: Regex): TRange?
	fun matchStartEnd(start: String, end: String): String?

	fun clone(): StrReader

	fun tryRead(str: String): Boolean

	class Literals(
		lits: Array<String>,
        map: MutableMap<String, Boolean>,
		val lengths: Array<Int>
	) {
		companion object {
			fun invoke(vararg lits: String): Literals
			fun fromList(lits: Array<String>): Literals
		}
		fun contains(lit: String): Boolean
		fun matchAt(str: String, offset: Int): String?
	}

	class TRange(val min: Int, val max: Int, val reader: StrReader) {
		companion object {
			fun combine(a: TRange, b: TRange): TRange
			fun combineList(list: List<TRange>): TRange?
			fun createDummy(): TRange
		}

		fun contains(index: Int): Boolean
		val file: String
		val text: String

		fun startEmptyRange(): TRange
		fun endEmptyRange(): TRange
		fun displace(offset: Int): TRange
	}

	fun readStringLit(reportErrors: Boolean = true): String
	fun tryReadInt(default: Int): Int
	fun tryReadNumber(default: Double = Double.NaN): Double
	fun tryExpect(str: String): Boolean
	fun tryExpect(str: Char): Boolean
	fun peekOffset(offset: Int = 0): Char
	fun readFloats(list: FloatArrayList = FloatArrayList(7)): FloatArrayList
	fun readIds(list: ArrayList<String> = ArrayList(7)): ArrayList<String>
	fun readInts(list: IntArrayList = IntArrayList(7)): IntArrayList
	fun tryReadId(): String?
}
```

## UUID

```kotlin
fun UUID(str: String): UUID

class UUID(val data: UByteArrayInt) {
	constructor(str: String)
	
	companion object {
		fun randomUUID(random: Random = SecureRandom): UUID
	}

	val version: Int
	val variant: Int

	override fun toString(): String
}

```


## Number Tools

### Parsing

Allocation-free parsing from substrings (used in `StrReader`):

```kotlin
object NumberParser {
    fun parseInt(str: String, start: Int, end: Int, radix: Int = 10): Int
    fun parseDouble(str: String, start: Int = 0, end: Int = str.length): Double
}
```

### Stringifying

```kotlin
fun Int.toStringUnsigned(radix: Int): String
fun Long.toStringUnsigned(radix: Int): String

// Do not include .0 suffix
val Float.niceStr: String
val Double.niceStr: String

// Consistent toString on all the targets
fun Double.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String
fun Float.toStringDecimal(decimalPlaces: Int, skipTrailingZeros: Boolean = false): String
```

## StringExt

```kotlin
operator fun String.Companion.invoke(arrays: IntArray, offset: Int = 0, size: Int
fun String_fromIntArray(arrays: IntArray, offset: Int = 0, size: Int = arrays.size - offset): Stringarrays.size - offset): String
fun String_fromCharArray(arrays: CharArray, offset: Int = 0, size: Int
fun String.format(vararg params: Any): String
fun String.splitKeep(regex: Regex): List<String>
fun String.replaceNonPrintableCharacters(replacement: String = "?"): String
fun String.toBytez(len: Int, charset: Charset = UTF8): ByteArray
fun String.toBytez(charset: Charset = UTF8): ByteArray
fun String.indexOfOrNull(char: Char, startIndex: Int = 0): Int?
fun String.lastIndexOfOrNull(char: Char, startIndex: Int = lastIndex): Int?
fun String.splitInChunks(size: Int): List<String>
fun String.substr(start: Int): String
fun String.substr(start: Int, length: Int): String
fun String.eachBuilder(transform: StringBuilder.(Char) -> Unit): String
fun String.transform(transform: (Char) -> String): String
fun String.parseInt(): Int
val String.quoted: String
fun String.toCharArray(): CharArray
```
