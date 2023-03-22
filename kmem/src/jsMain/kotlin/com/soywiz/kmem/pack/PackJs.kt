package com.soywiz.kmem.pack

import com.soywiz.kmem.*


actual data class Int4Pack(val x: Int, val y: Int, val z: Int, val w: Int)
actual val Int4Pack.i0: Int get() = this.x
actual val Int4Pack.i1: Int get() = this.y
actual val Int4Pack.i2: Int get() = this.z
actual val Int4Pack.i3: Int get() = this.w
actual fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = Int4Pack(i0, i1, i2, i3)







actual data class Half8Pack(
    val h0: Float,
    val h1: Float,
    val h2: Float,
    val h3: Float,
    val h4: Float,
    val h5: Float,
    val h6: Float,
    val h7: Float,
)
actual val Half8Pack.h0: Float get() = this.h0
actual val Half8Pack.h1: Float get() = this.h1
actual val Half8Pack.h2: Float get() = this.h2
actual val Half8Pack.h3: Float get() = this.h3
actual val Half8Pack.h4: Float get() = this.h4
actual val Half8Pack.h5: Float get() = this.h5
actual val Half8Pack.h6: Float get() = this.h6
actual val Half8Pack.h7: Float get() = this.h7
actual fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack {
    return Half8Pack(h0, h1, h2, h3, h4, h5, h6, h7)
}



internal data class JsHalf4Pack(val x: Half, val y: Half, val z: Half, val w: Half)
internal actual inline fun packHalf4(x: Half, y: Half, z: Half, w: Half): Long = JsHalf4Pack(x, y, z, w).asDynamic()
internal actual inline fun unpackHalf4X(v: Long): Half = v.unsafeCast<JsHalf4Pack>().x
internal actual inline fun unpackHalf4Y(v: Long): Half = v.unsafeCast<JsHalf4Pack>().y
internal actual inline fun unpackHalf4Z(v: Long): Half = v.unsafeCast<JsHalf4Pack>().z
internal actual inline fun unpackHalf4W(v: Long): Half = v.unsafeCast<JsHalf4Pack>().w





actual data class Float4Pack(val x: Float, val y: Float, val z: Float, val w: Float)
actual val Float4Pack.f0: Float get() = this.x
actual val Float4Pack.f1: Float get() = this.y
actual val Float4Pack.f2: Float get() = this.z
actual val Float4Pack.f3: Float get() = this.w
actual fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack = Float4Pack(f0, f1, f2, f3)






actual data class BFloat6Pack(
    val v0: Float,
    val v1: Float,
    val v2: Float,
    val v3: Float,
    val v4: Float,
    val v5: Float,
)
actual val BFloat6Pack.bf0: Float get() = this.v0
actual val BFloat6Pack.bf1: Float get() = this.v1
actual val BFloat6Pack.bf2: Float get() = this.v2
actual val BFloat6Pack.bf3: Float get() = this.v3
actual val BFloat6Pack.bf4: Float get() = this.v4
actual val BFloat6Pack.bf5: Float get() = this.v5
actual fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack =
    BFloat6Pack(bf0, bf1, bf2, bf3, bf4, bf5)






actual data class BFloat3Half4Pack(
    val b0: Float, val b1: Float, val b2: Float,
    val hf0: Float, val hf1: Float, val hf2: Float, val hf3: Float,
)
// 21-bit BFloat precision
actual inline val BFloat3Half4Pack.b0: Float get() = b0
actual inline val BFloat3Half4Pack.b1: Float get() = b1
actual inline val BFloat3Half4Pack.b2: Float get() = b2
// 16-bit Half Float precision
actual inline val BFloat3Half4Pack.hf0: Float get() = hf0
actual inline val BFloat3Half4Pack.hf1: Float get() = hf1
actual inline val BFloat3Half4Pack.hf2: Float get() = hf2
actual inline val BFloat3Half4Pack.hf3: Float get() = hf3

actual fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    return BFloat3Half4Pack(b0, b1, b2, hf0, hf1, hf2, hf3)
}




internal data class JsFloat2Pack(val x: Float, val y: Float)
internal actual inline fun packFloat2(x: Float, y: Float): Long = JsFloat2Pack(x, y).asDynamic()
internal actual inline fun unpackFloat2X(v: Long): Float = v.unsafeCast<JsFloat2Pack>().x
internal actual inline fun unpackFloat2Y(v: Long): Float = v.unsafeCast<JsFloat2Pack>().y


internal data class JsInt2Pack(val x: Int, val y: Int)
internal actual inline fun packInt2(x: Int, y: Int): Long = JsInt2Pack(x, y).asDynamic()
internal actual inline fun unpackInt2X(v: Long): Int = v.unsafeCast<JsInt2Pack>().x
internal actual inline fun unpackInt2Y(v: Long): Int = v.unsafeCast<JsInt2Pack>().y



internal data class JsShort4Pack(val x: Short, val y: Short, val z: Short, val w: Short)
internal actual inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long = JsShort4Pack(x, y, z, w).asDynamic()
internal actual inline fun unpackShort4X(v: Long): Short = v.unsafeCast<JsShort4Pack>().x
internal actual inline fun unpackShort4Y(v: Long): Short = v.unsafeCast<JsShort4Pack>().y
internal actual inline fun unpackShort4Z(v: Long): Short = v.unsafeCast<JsShort4Pack>().z
internal actual inline fun unpackShort4W(v: Long): Short = v.unsafeCast<JsShort4Pack>().w

