---
permalink: /io/encoding/
group: io
layout: default
title: Encoding
title_prefix: KorIO
description: "Escaping, Quoting, Base64, Hex, CType..."
fa-icon: fa-hashtag
priority: 5
---

KorIO has utilities for handling different encodings.



## Escaping and Quoting

```kotlin
// Escaping and quoting
fun String.escape(): String // C-Style \xNN
fun String.uescape(): String  // Unicode-Style \uNNNN

fun String?.quote(): String
fun String?.uquote(): String
val String?.quoted: String

// Unescaping and unquoting
fun String.unescape(): String

fun String.unquote(): String
val String.unquoted: String

// Check if quoted
fun String.isQuoted(): Boolean
```

## Base64

```kotlin
fun String.fromBase64IgnoreSpaces(): ByteArray
fun String.fromBase64(): ByteArray
fun ByteArray.toBase64(): String

object Base64 {
	fun decode(str: String): ByteArray
	fun decode(src: ByteArray, dst: ByteArray): Int

	fun encode(src: String, charset: Charset): String
	fun encode(src: ByteArray): String
}
```

You can create multiline base64 with:

```kotlin
myByteArray.chunked(64).joinToString("\n")
```

## Hex

The Hex utilities, allows you to encode and decode Hexadecimal from/to ByteArray and Strings.

```kotlin
val List<String>.unhexIgnoreSpaces: ByteArray
val String.unhexIgnoreSpaces: ByteArray
val String.unhex: ByteArray
val ByteArray.hex: String // Hex in lower case

val Int.hex: String // Adds 0x prefix
val Int.shex: String

object Hex {
	val DIGITS_UPPER: String
	val DIGITS_LOWER: String

	fun isHexDigit(c: Char): Boolean

	fun decodeChar(c: Char): Int // Returns -1 if not hex
	fun decode(str: String): ByteArray

	fun encodeCharLower(v: Int): Char
	fun encodeCharUpper(v: Int): Char
	fun encodeLower(src: ByteArray): String
	fun encodeUpper(src: ByteArray): String
}
```

## CType

Utility classes for getting information about characters.

```kotlin
fun Char.isWhitespaceFast(): Boolean // Faster than isWhitepsace specially on javascript because do not use regular expressions
fun Char.isDigit(): Boolean
fun Char.isLetter(): Boolean
fun Char.isLetterOrDigit(): Boolean
fun Char.isLetterOrUnderscore(): Boolean
fun Char.isLetterDigitOrUnderscore(): Boolean
fun Char.isLetterOrDigitOrDollar(): Boolean
val Char.isNumeric: Boolean
fun Char.isPrintable(): Boolean
val Char.isPossibleFloatChar: Boolean
```
