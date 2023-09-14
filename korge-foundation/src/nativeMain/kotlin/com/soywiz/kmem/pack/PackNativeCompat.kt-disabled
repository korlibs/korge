package com.soywiz.kmem.pack

import com.soywiz.kmem.*

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







actual data class Float4Pack(val x: Float, val y: Float, val z: Float, val w: Float)
actual val Float4Pack.f0: Float get() = this.x
actual val Float4Pack.f1: Float get() = this.y
actual val Float4Pack.f2: Float get() = this.z
actual val Float4Pack.f3: Float get() = this.w
actual fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack = Float4Pack(f0, f1, f2, f3)








actual data class BFloat6Pack(
    val v0: Long,
    val v1: Long,
)

private inline fun unpack(long: Long, index: Int): Float = BFloat.unpackLong(long, index).float

actual val BFloat6Pack.bf0: Float get() = unpack(v0, 0)
actual val BFloat6Pack.bf1: Float get() = unpack(v0, 1)
actual val BFloat6Pack.bf2: Float get() = unpack(v0, 2)
actual val BFloat6Pack.bf3: Float get() = unpack(v1, 0)
actual val BFloat6Pack.bf4: Float get() = unpack(v1, 1)
actual val BFloat6Pack.bf5: Float get() = unpack(v1, 2)

actual fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack {
    val v0 = BFloat(bf0)
    val v1 = BFloat(bf1)
    val v2 = BFloat(bf2)
    val v3 = BFloat(bf3)
    val v4 = BFloat(bf4)
    val v5 = BFloat(bf5)
    val pack1 = BFloat.packLong(v0, v1, v2)
    val pack2 = BFloat.packLong(v3, v4, v5)
    return BFloat6Pack(pack1, pack2)
}



actual data class BFloat3Half4Pack(val v0: Long, val v1: Long)
// 21-bit BFloat precision
actual val BFloat3Half4Pack.b0: Float get() = BFloat21.unpack3(v0, 0)
actual val BFloat3Half4Pack.b1: Float get() = BFloat21.unpack3(v0, 1)
actual val BFloat3Half4Pack.b2: Float get() = BFloat21.unpack3(v0, 2)
// 16-bit Half Float precision
actual val BFloat3Half4Pack.hf0: Float get() = BFloat16.unpack4(v1, 0)
actual val BFloat3Half4Pack.hf1: Float get() = BFloat16.unpack4(v1, 1)
actual val BFloat3Half4Pack.hf2: Float get() = BFloat16.unpack4(v1, 2)
actual val BFloat3Half4Pack.hf3: Float get() = BFloat16.unpack4(v1, 3)

actual fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    val pack1 = BFloat21.pack3(b0, b1, b2)
    val pack2 = BFloat16.pack4(hf0, hf1, hf2, hf3)
    return BFloat3Half4Pack(pack1, pack2)
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


actual data class Int4Pack(val x: Int, val y: Int, val z: Int, val w: Int)
actual val Int4Pack.i0: Int get() = this.x
actual val Int4Pack.i1: Int get() = this.y
actual val Int4Pack.i2: Int get() = this.z
actual val Int4Pack.i3: Int get() = this.w
actual fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = Int4Pack(i0, i1, i2, i3)









internal actual inline fun packFloat2(x: Float, y: Float): Long = (x.toRawBits().toLong() and 0xFFFFFFFFL) or (y.toRawBits().toLong() shl 32)
internal actual inline fun unpackFloat2X(v: Long): Float = Float.fromBits(v.toInt())
internal actual inline fun unpackFloat2Y(v: Long): Float = Float.fromBits((v shr 32).toInt())


internal actual inline fun packInt2(x: Int, y: Int): Long = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)
internal actual inline fun unpackInt2X(v: Long): Int = v.toInt()
internal actual inline fun unpackInt2Y(v: Long): Int = (v shr 32).toInt()


internal actual inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long =
    packInt2(packShort2(x, y), packShort2(z, w))
internal actual inline fun unpackShort4X(v: Long): Short = unpackInt2X(v).toShort()
internal actual inline fun unpackShort4Y(v: Long): Short = (unpackInt2X(v) shr 16).toShort()
internal actual inline fun unpackShort4Z(v: Long): Short = unpackInt2Y(v).toShort()
internal actual inline fun unpackShort4W(v: Long): Short = (unpackInt2Y(v) shr 16).toShort()

internal fun packShort2(x: Short, y: Short): Int = (x.toInt() and 0xFFFF) or (y.toInt() shl 16)
internal fun unpackShort2X(v: Int): Short = v.toShort()
internal fun unpackShort2Y(v: Int): Short = (v shr 16).toShort()


internal fun packHalf2(x: Half, y: Half): Int = (x.rawBits.toInt() and 0xFFFF) or (y.rawBits.toInt() shl 16)

internal actual inline fun packHalf4(x: Half, y: Half, z: Half, w: Half): Long = packInt2(packHalf2(x, y), packHalf2(z, w))
internal actual inline fun unpackHalf4X(v: Long): Half = Half.fromBits(unpackInt2X(v).toShort())
internal actual inline fun unpackHalf4Y(v: Long): Half = Half.fromBits((unpackInt2X(v) shr 16).toShort())
internal actual inline fun unpackHalf4Z(v: Long): Half = Half.fromBits(unpackInt2Y(v).toShort())
internal actual inline fun unpackHalf4W(v: Long): Half = Half.fromBits((unpackInt2Y(v) shr 16).toShort())
