package com.soywiz.kmem.pack

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
