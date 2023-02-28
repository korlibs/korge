package com.soywiz.kds.pack

internal actual inline fun packFloat2(x: Float, y: Float): Long = (x.toRawBits().toLong() and 0xFFFFFFFFL) or (y.toRawBits().toLong() shl 32)
internal actual inline fun unpackFloat2X(v: Long): Float = Float.fromBits(v.toInt())
internal actual inline fun unpackFloat2Y(v: Long): Float = Float.fromBits((v shr 32).toInt())
