package com.soywiz.kmem

// Based on https://en.wikipedia.org/wiki/Bfloat16_floating-point_format
// But occupies 21 bits. This is done to encode 6 floats in 128bits. Converting to float is just a shift.
// Can pack 3 floats in a 64-bit Long
// 21-bit: sign:1 - exp:8 - smantissa:1, mantissa:11
inline class BFloat @PublishedApi internal constructor(val rawBits: Int) {
    companion object {
        private inline fun mask(bits: Int): Int = (1 shl bits) - 1

        const val BITS = 21
        const val MASK = (1 shl BITS) - 1

        inline fun fromBits(bits: Int): BFloat = BFloat(bits)
        inline fun packLong(a: BFloat, b: BFloat, c: BFloat): Long =
            (a.rawBits.toLong() shl 0) or (b.rawBits.toLong() shl 21) or (c.rawBits.toLong() shl 42)
        inline fun unpackLong(long: Long, index: Int): BFloat =
            fromBits(((long ushr (21 * index)) and MASK.toLong()).toInt())
    }

    constructor(v: Float) : this(v.toRawBits() ushr 12)
    val float: Float get() = Float.fromBits(rawBits shl 12)
    fun toFloat(): Float = float
}
