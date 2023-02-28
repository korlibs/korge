package com.soywiz.kmem.pack

import com.soywiz.kmem.*

private inline fun packInt2(x: Int, y: Int): Long = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
private inline fun unpackInt2X(v: Long): Int = v.toInt()
private inline fun unpackInt2Y(v: Long): Int = (v shr 32).toInt()
internal fun packHalf2(x: Half, y: Half): Int = (x.rawBits.toInt() and 0xFFFF) or (y.rawBits.toInt() shl 16)

internal actual inline fun packHalf4(x: Half, y: Half, z: Half, w: Half): Long = packInt2(packHalf2(x, y), packHalf2(z, w))
internal actual inline fun unpackHalf4X(v: Long): Half = Half.fromBits(unpackInt2X(v).toShort())
internal actual inline fun unpackHalf4Y(v: Long): Half = Half.fromBits((unpackInt2X(v) shr 16).toShort())
internal actual inline fun unpackHalf4Z(v: Long): Half = Half.fromBits(unpackInt2Y(v).toShort())
internal actual inline fun unpackHalf4W(v: Long): Half = Half.fromBits((unpackInt2Y(v) shr 16).toShort())
