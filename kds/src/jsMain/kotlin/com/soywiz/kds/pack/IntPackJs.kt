package com.soywiz.kds.pack

//internal actual inline fun packInt2(x: Int, y: Int): Long = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
//internal actual inline fun unpackInt2X(v: Long): Int = v.toInt()
//internal actual inline fun unpackInt2Y(v: Long): Int = (v shr 32).toInt()

//internal actual inline fun packInt2(x: Int, y: Int): Long { val out = (-999L).asDynamic(); out.low_1 = x; out.high_1 = y; return out }
//internal actual inline fun unpackInt2X(v: Long): Int = v.asDynamic().low_1
//internal actual inline fun unpackInt2Y(v: Long): Int = v.asDynamic().high_1

data class IntPack(val x: Int, val y: Int)
internal actual inline fun packInt2(x: Int, y: Int): Long = IntPack(x, y).asDynamic()
internal actual inline fun unpackInt2X(v: Long): Int = v.unsafeCast<IntPack>().x
internal actual inline fun unpackInt2Y(v: Long): Int = v.unsafeCast<IntPack>().y
