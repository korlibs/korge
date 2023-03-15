package com.soywiz.kmem.pack

import com.soywiz.kmem.*

actual typealias BFloat6Pack = Vector128

private inline fun unpack(long: Long, index: Int): Float = BFloat.unpackLong(long, index).float

actual val BFloat6Pack.bf0: Float get() = unpack(getLongAt(0), 0)
actual val BFloat6Pack.bf1: Float get() = unpack(getLongAt(0), 1)
actual val BFloat6Pack.bf2: Float get() = unpack(getLongAt(0), 2)
actual val BFloat6Pack.bf3: Float get() = unpack(getLongAt(1), 0)
actual val BFloat6Pack.bf4: Float get() = unpack(getLongAt(1), 1)
actual val BFloat6Pack.bf5: Float get() = unpack(getLongAt(1), 2)

actual fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack {
    val v0 = BFloat(bf0)
    val v1 = BFloat(bf1)
    val v2 = BFloat(bf2)
    val v3 = BFloat(bf3)
    val v4 = BFloat(bf4)
    val v5 = BFloat(bf5)
    val pack1 = BFloat.packLong(v0, v1, v2)
    val pack2 = BFloat.packLong(v3, v4, v5)
    return vectorOf(pack1.low, pack1.high, pack2.low, pack2.high)
}
