package com.soywiz.kds.pack

internal actual inline fun packInt2(x: Int, y: Int): Long = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
internal actual inline fun unpackInt2X(v: Long): Int = v.toInt()
internal actual inline fun unpackInt2Y(v: Long): Int = (v shr 32).toInt()
