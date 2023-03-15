package com.soywiz.kmem.pack

import com.soywiz.kmem.*

/*
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
*/

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
