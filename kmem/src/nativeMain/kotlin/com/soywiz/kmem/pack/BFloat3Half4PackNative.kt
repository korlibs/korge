package com.soywiz.kmem.pack

import com.soywiz.kmem.*

actual typealias BFloat3Half4Pack = Vector128
// 21-bit BFloat precision
actual val BFloat3Half4Pack.b0: Float get() = BFloat21.unpack3(getLongAt(0), 0)
actual val BFloat3Half4Pack.b1: Float get() = BFloat21.unpack3(getLongAt(0), 1)
actual val BFloat3Half4Pack.b2: Float get() = BFloat21.unpack3(getLongAt(0), 2)
// 16-bit Half Float precision
actual val BFloat3Half4Pack.hf0: Float get() = BFloat16.unpack4(getLongAt(0), 0)
actual val BFloat3Half4Pack.hf1: Float get() = BFloat16.unpack4(getLongAt(0), 1)
actual val BFloat3Half4Pack.hf2: Float get() = BFloat16.unpack4(getLongAt(0), 2)
actual val BFloat3Half4Pack.hf3: Float get() = BFloat16.unpack4(getLongAt(0), 3)

actual fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    val pack1 = BFloat21.pack3(b0, b1, b2)
    val pack2 = BFloat16.pack4(hf0, hf1, hf2, hf3)
    return vectorOf(pack1.low, pack1.high, pack2.low, pack2.high)
}
