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
    val f0: Float,
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val f4: Float,
    val f5: Float,
)

actual val BFloat6Pack.bf0: Float get() = f0
actual val BFloat6Pack.bf1: Float get() = f1
actual val BFloat6Pack.bf2: Float get() = f2
actual val BFloat6Pack.bf3: Float get() = f3
actual val BFloat6Pack.bf4: Float get() = f4
actual val BFloat6Pack.bf5: Float get() = f5

actual fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack =
    BFloat6Pack(bf0, bf1, bf2, bf3, bf4, bf5)



actual data class BFloat3Half4Pack(
    val f0: Float,
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val f4: Float,
    val f5: Float,
    val f6: Float,
)
// 21-bit BFloat precision
actual val BFloat3Half4Pack.b0: Float get() = f0
actual val BFloat3Half4Pack.b1: Float get() = f1
actual val BFloat3Half4Pack.b2: Float get() = f2
// 16-bit Half Float precision
actual val BFloat3Half4Pack.hf0: Float get() = f3
actual val BFloat3Half4Pack.hf1: Float get() = f4
actual val BFloat3Half4Pack.hf2: Float get() = f5
actual val BFloat3Half4Pack.hf3: Float get() = f6

actual fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    return BFloat3Half4Pack(b0, b1, b2, hf0, hf1, hf2, hf3)
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


actual data class Int4Pack(val x: Int, val y: Int, val z: Int, val w: Int)
actual val Int4Pack.i0: Int get() = this.x
actual val Int4Pack.i1: Int get() = this.y
actual val Int4Pack.i2: Int get() = this.z
actual val Int4Pack.i3: Int get() = this.w
actual fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = Int4Pack(i0, i1, i2, i3)





actual typealias Float2Pack = Long
actual val Float2Pack.f0: Float get() = Float.fromBits(unpackInt2X(this))
actual val Float2Pack.f1: Float get() = Float.fromBits(unpackInt2Y(this))
actual fun float2PackOf(f0: Float, f1: Float): Float2Pack = packInt2(f0.toRawBits(), f1.toRawBits())

actual typealias Int2Pack = Long
actual val Int2Pack.i0: Int get() = unpackInt2X(this)
actual val Int2Pack.i1: Int get() = unpackInt2Y(this)
actual fun int2PackOf(i0: Int, i1: Int): Int2Pack = packInt2(i0, i1)

actual typealias Short4Pack = Long
actual val Short4Pack.s0: Short get() = unpackShort4X(this)
actual val Short4Pack.s1: Short get() = unpackShort4Y(this)
actual val Short4Pack.s2: Short get() = unpackShort4Z(this)
actual val Short4Pack.s3: Short get() = unpackShort4W(this)
actual fun short4PackOf(s0: Short, s1: Short, s2: Short, s3: Short): Short4Pack = packShort4(s0, s1, s2, s3)


private inline fun unpackInt2X(v: Long): Int = v.toInt()
private inline fun unpackInt2Y(v: Long): Int = (v shr 32).toInt()
private inline fun packInt2(x: Int, y: Int): Long = (x.toLong() and 0xFFFFFFFFL) or (y.toLong() shl 32)

private inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long = packInt2(packShort2(x, y), packShort2(z, w))
private inline fun unpackShort4X(v: Long): Short = unpackInt2X(v).toShort()
private inline fun unpackShort4Y(v: Long): Short = (unpackInt2X(v) shr 16).toShort()
private inline fun unpackShort4Z(v: Long): Short = unpackInt2Y(v).toShort()
private inline fun unpackShort4W(v: Long): Short = (unpackInt2Y(v) shr 16).toShort()

private inline fun packShort2(x: Short, y: Short): Int = (x.toInt() and 0xFFFF) or (y.toInt() shl 16)
private inline fun unpackShort2X(v: Int): Short = v.toShort()
private inline fun unpackShort2Y(v: Int): Short = (v shr 16).toShort()

internal fun packHalf2(x: Half, y: Half): Int = (x.rawBits.toInt() and 0xFFFF) or (y.rawBits.toInt() shl 16)
