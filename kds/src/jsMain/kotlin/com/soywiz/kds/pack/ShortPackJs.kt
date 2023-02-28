package com.soywiz.kds.pack

//internal actual inline fun packInt2(x: Int, y: Int): Long = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
//internal actual inline fun unpackInt2X(v: Long): Int = v.toInt()
//internal actual inline fun unpackInt2Y(v: Long): Int = (v shr 32).toInt()

//internal actual inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long { val out = (-999L).asDynamic(); out.low_1 = packShort2(x, y); out.high_1 = packShort2(z, w); return out }
//internal actual inline fun unpackShort4X(v: Long): Short = unpackShort2X(v.asDynamic().low_1.unsafeCast<Int>())
//internal actual inline fun unpackShort4Y(v: Long): Short = unpackShort2Y(v.asDynamic().low_1.unsafeCast<Int>())
//internal actual inline fun unpackShort4Z(v: Long): Short = unpackShort2X(v.asDynamic().high_1.unsafeCast<Int>())
//internal actual inline fun unpackShort4W(v: Long): Short = unpackShort2Y(v.asDynamic().high_1.unsafeCast<Int>())

internal data class JsShort4Pack(val x: Short, val y: Short, val z: Short, val w: Short)
internal actual inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long = JsShort4Pack(x, y, z, w).asDynamic()
internal actual inline fun unpackShort4X(v: Long): Short = v.unsafeCast<JsShort4Pack>().x
internal actual inline fun unpackShort4Y(v: Long): Short = v.unsafeCast<JsShort4Pack>().y
internal actual inline fun unpackShort4Z(v: Long): Short = v.unsafeCast<JsShort4Pack>().z
internal actual inline fun unpackShort4W(v: Long): Short = v.unsafeCast<JsShort4Pack>().w

//data class ShortPack(val x: Int, val y: Int)
//internal actual inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long = ShortPack(packShort2(x, y), packShort2(z, w)).asDynamic()
//internal actual inline fun unpackShort4X(v: Long): Short = unpackShort2X(v.unsafeCast<ShortPack>().x)
//internal actual inline fun unpackShort4Y(v: Long): Short = unpackShort2Y(v.unsafeCast<ShortPack>().x)
//internal actual inline fun unpackShort4Z(v: Long): Short = unpackShort2X(v.unsafeCast<ShortPack>().y)
//internal actual inline fun unpackShort4W(v: Long): Short = unpackShort2Y(v.unsafeCast<ShortPack>().y)
