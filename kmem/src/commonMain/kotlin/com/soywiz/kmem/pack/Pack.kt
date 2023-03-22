package com.soywiz.kmem.pack

import com.soywiz.kmem.*


// Based on https://en.wikipedia.org/wiki/Bfloat16_floating-point_format
// But occupies 21 bits. This is done to encode 6 floats in 128bits. Converting to float is just a shift.
// Can pack 3 floats in a 64-bit Long
// 21-bit: sign:1 - exp:8 - smantissa:1, mantissa:11
internal object BFloat21 {
    internal fun unpack3(v: Long, index: Int): Float = unpack((v ushr (21 * index)).toInt())
    internal fun pack3(a: Float, b: Float, c: Float): Long =
        (pack(a).toLong() shl 0) or (pack(b).toLong() shl 21) or (pack(c).toLong() shl 42)
    internal fun pack(v: Float): Int = v.toRawBits() ushr 12
    internal fun unpack(v: Int): Float = Float.fromBits(v shl 12)
}




internal object BFloat16 {
    internal fun unpack4(v: Long, index: Int): Float = unpack((v ushr (16 * index)).toInt())

    internal fun pack4(a: Float, b: Float, c: Float, d: Float): Long {
        val pack1 = (pack(a) shl 0) or (pack(b) shl 16)
        val pack2 = (pack(c) shl 0) or (pack(d) shl 16)
        return pack1.toLong() or (pack2.toLong() shl 32)
    }

    internal fun pack(v: Float): Int = v.toRawBits() ushr 17
    internal fun unpack(v: Int): Float = Float.fromBits(v shl 17)
}



expect class Int4Pack
expect val Int4Pack.i0: Int
expect val Int4Pack.i1: Int
expect val Int4Pack.i2: Int
expect val Int4Pack.i3: Int
expect fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack
fun Int4Pack.copy(i0: Int = this.i0, i1: Int = this.i1, i2: Int = this.i2, i3: Int = this.i3): Int4Pack = int4PackOf(i0, i1, i2, i3)




expect class Half8Pack
expect val Half8Pack.h0: Float
expect val Half8Pack.h1: Float
expect val Half8Pack.h2: Float
expect val Half8Pack.h3: Float
expect val Half8Pack.h4: Float
expect val Half8Pack.h5: Float
expect val Half8Pack.h6: Float
expect val Half8Pack.h7: Float
expect fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack





inline class Half4Pack private constructor(val data: Long) {
    constructor(x: Half, y: Half, z: Half, w: Half) : this(packHalf4(x, y, z, w))
    val x: Half get() = unpackHalf4X(data)
    val y: Half get() = unpackHalf4Y(data)
    val z: Half get() = unpackHalf4Z(data)
    val w: Half get() = unpackHalf4W(data)
}
internal expect inline fun packHalf4(x: Half, y: Half, z: Half, w: Half): Long
internal expect inline fun unpackHalf4X(v: Long): Half
internal expect inline fun unpackHalf4Y(v: Long): Half
internal expect inline fun unpackHalf4Z(v: Long): Half
internal expect inline fun unpackHalf4W(v: Long): Half






expect class Float4Pack
expect val Float4Pack.f0: Float
expect val Float4Pack.f1: Float
expect val Float4Pack.f2: Float
expect val Float4Pack.f3: Float
expect fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack
fun Float4Pack.copy(f0: Float = this.f0, f1: Float = this.f1, f2: Float = this.f2, f3: Float = this.f3): Float4Pack = float4PackOf(f0, f1, f2, f3)






expect class BFloat3Half4Pack
// 21-bit BFloat precision
expect val BFloat3Half4Pack.b0: Float
expect val BFloat3Half4Pack.b1: Float
expect val BFloat3Half4Pack.b2: Float
// 16-bit Half Float precision
expect val BFloat3Half4Pack.hf0: Float
expect val BFloat3Half4Pack.hf1: Float
expect val BFloat3Half4Pack.hf2: Float
expect val BFloat3Half4Pack.hf3: Float
expect fun bfloat3Half4PackOf(b0: Float, b1: Float, b2: Float, hf0: Float, hf1: Float, hf2: Float, hf3: Float): BFloat3Half4Pack






expect class BFloat6Pack
expect val BFloat6Pack.bf0: Float
expect val BFloat6Pack.bf1: Float
expect val BFloat6Pack.bf2: Float
expect val BFloat6Pack.bf3: Float
expect val BFloat6Pack.bf4: Float
expect val BFloat6Pack.bf5: Float
expect fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack






inline class Float2Pack private constructor(val data: Long) {
    constructor(x: Float, y: Float) : this(packFloat2(x, y))

    val x: Float get() = unpackFloat2X(data)
    val y: Float get() = unpackFloat2Y(data)
}

//private inline fun packFloat2(x: Float, y: Float): Long = (x.toRawBits().toLong() and 0xFFFFFFFFL) or (y.toRawBits().toLong() shl 32)
//private inline fun unpackFloat2X(v: Long): Float = Float.fromBits(v.toInt())
//private inline fun unpackFloat2Y(v: Long): Float = Float.fromBits((v shr 32).toInt())

internal expect inline fun packFloat2(x: Float, y: Float): Long
internal expect inline fun unpackFloat2X(v: Long): Float
internal expect inline fun unpackFloat2Y(v: Long): Float





inline class Int2Pack private constructor(val data: Long) {
    constructor(x: Int, y: Int) : this(packInt2(x, y))

    val x: Int get() = unpackInt2X(data)
    val y: Int get() = unpackInt2Y(data)
}

internal expect inline fun packInt2(x: Int, y: Int): Long
internal expect inline fun unpackInt2X(v: Long): Int
internal expect inline fun unpackInt2Y(v: Long): Int



inline class Short4Pack private constructor(val data: Long) {
    constructor(x: Short, y: Short, z: Short, w: Short) : this(packShort4(x, y, z, w))

    val x: Short get() = unpackShort4X(data)
    val y: Short get() = unpackShort4Y(data)
    val z: Short get() = unpackShort4Z(data)
    val w: Short get() = unpackShort4W(data)
}

internal expect inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long
internal expect inline fun unpackShort4X(v: Long): Short
internal expect inline fun unpackShort4Y(v: Long): Short
internal expect inline fun unpackShort4Z(v: Long): Short
internal expect inline fun unpackShort4W(v: Long): Short


