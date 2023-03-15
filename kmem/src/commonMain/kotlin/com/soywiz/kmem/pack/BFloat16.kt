package com.soywiz.kmem.pack

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
