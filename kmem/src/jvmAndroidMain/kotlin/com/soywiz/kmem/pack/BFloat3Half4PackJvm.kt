package com.soywiz.kmem.pack

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
