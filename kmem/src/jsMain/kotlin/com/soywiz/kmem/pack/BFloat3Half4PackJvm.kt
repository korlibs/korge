package com.soywiz.kmem.pack

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
