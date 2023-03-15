package com.soywiz.kmem.pack

actual data class Half8Pack(
    val h0: Float,
    val h1: Float,
    val h2: Float,
    val h3: Float,
    val h4: Float,
    val h5: Float,
    val h6: Float,
    val h7: Float,
)
actual val Half8Pack.h0: Float get() = this.h0
actual val Half8Pack.h1: Float get() = this.h1
actual val Half8Pack.h2: Float get() = this.h2
actual val Half8Pack.h3: Float get() = this.h3
actual val Half8Pack.h4: Float get() = this.h4
actual val Half8Pack.h5: Float get() = this.h5
actual val Half8Pack.h6: Float get() = this.h6
actual val Half8Pack.h7: Float get() = this.h7
actual fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack {
    return Half8Pack(h0, h1, h2, h3, h4, h5, h6, h7)
}
