---
permalink: /io/charset/
group: io
layout: default
title: Charset
title_prefix: KorIO
description: "Convert ByteArray from/to String with Charsets: LATIN1, UTF8, UTF16..."
fa-icon: fa-language
priority: 4
---

KorIO has utilities for handling different charsets.



## Charset

```kotlin
val ISO_8859_1: Charset
val LATIN1: SingleByteCharset
val UTF8: Charset
val UTF16_LE: UTF16Charset
val UTF16_BE: UTF16Charset
val ASCII: Charset

fun String.toByteArray(charset: Charset = UTF8): ByteArray
fun ByteArray.toString(charset: Charset): String
fun ByteArray.readStringz(o: Int, size: Int, charset: Charset = UTF8): String
fun ByteArray.readStringz(o: Int, charset: Charset = UTF8): String
fun ByteArray.readString(o: Int, size: Int, charset: Charset = UTF8): String

abstract class Charset(val name: String) {
	abstract fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int = 0, end: Int = src.length)
	abstract fun decode(out: StringBuilder, src: ByteArray, start: Int = 0, end: Int = src.size)
}

open class SingleByteCharset(name: String, val conv: String) : Charset(name)
```

## UTF8

Only UTF8 supported at the moment.
