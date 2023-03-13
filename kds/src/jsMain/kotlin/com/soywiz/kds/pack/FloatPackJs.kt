package com.soywiz.kds.pack

/*
internal actual inline fun packFloat2(x: Float, y: Float): Long = (x.toRawBits().toLong() and 0xFFFFFFFFL) or (y.toRawBits().toLong() shl 32)
internal actual inline fun unpackFloat2X(v: Long): Float = Float.fromBits(v.toInt())
internal actual inline fun unpackFloat2Y(v: Long): Float = Float.fromBits((v shr 32).toInt())
*/
//
//internal actual inline fun packFloat2(x: Float, y: Float): Long = (x.reinterpretAsInt().toLong() and 0xFFFFFFFFL) or (y.reinterpretAsInt().toLong() shl 32)
//internal actual inline fun unpackFloat2X(v: Long): Float = Float.fromBits(v.toInt())
//internal actual inline fun unpackFloat2Y(v: Long): Float = Float.fromBits((v shr 32).toInt())

//private val f32 = Float32Array(1)
//private val i32 = Int32Array(f32.buffer)
//private fun Int.reinterpretAsFloat(): Float {
//    i32[0] = this
//    return f32[0]
//}
//private fun Float.reinterpretAsInt(): Int {
//    f32[0] = this
//    return i32[0]
//}
//internal actual inline fun packFloat2(x: Float, y: Float): Long {
//    val x0 = x.reinterpretAsInt()
//    val y0 = y.reinterpretAsInt()
//    //return js("new kotlin_kotlin.\$_\$.i9(x0, y0)")
//    return (-9999L).also {
//        it.asDynamic().low_1 = x0
//        it.asDynamic().high_1 = y0
//    }
//}
//internal actual inline fun unpackFloat2X(v: Long): Float = v.asDynamic().low_1.unsafeCast<Int>().reinterpretAsFloat()
//internal actual inline fun unpackFloat2Y(v: Long): Float = v.asDynamic().high_1.unsafeCast<Int>().reinterpretAsFloat()

//low_1

//internal actual inline fun packFloat2(x: Float, y: Float): Long { val out = (-999L).asDynamic(); out.low_1 = x; out.high_1 = y; return out }
//internal actual inline fun unpackFloat2X(v: Long): Float = v.asDynamic().low_1
//internal actual inline fun unpackFloat2Y(v: Long): Float = v.asDynamic().high_1

internal data class JsFloat2Pack(val x: Float, val y: Float)
internal actual inline fun packFloat2(x: Float, y: Float): Long = JsFloat2Pack(x, y).asDynamic()
internal actual inline fun unpackFloat2X(v: Long): Float = v.unsafeCast<JsFloat2Pack>().x
internal actual inline fun unpackFloat2Y(v: Long): Float = v.unsafeCast<JsFloat2Pack>().y

//internal actual inline fun packFloat2(x: Float, y: Float): Long {
//    val out = FloatArray(2).asDynamic()
//    out[0] = x
//    out[1] = y
//    return out
//}
//internal actual inline fun unpackFloat2X(v: Long): Float = v.asDynamic()[0]
//internal actual inline fun unpackFloat2Y(v: Long): Float = v.asDynamic()[1]
