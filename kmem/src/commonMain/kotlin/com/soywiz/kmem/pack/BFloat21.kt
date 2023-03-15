package com.soywiz.kmem.pack

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
