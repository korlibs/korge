package com.soywiz.kmem.pack

import com.soywiz.kmem.*

actual typealias Half8Pack = Vector128

actual val Half8Pack.h0: Float get() = Half.fromBits(this.getIntAt(0).extract16(0)).toFloat()
actual val Half8Pack.h1: Float get() = Half.fromBits(this.getIntAt(0).extract16(16)).toFloat()
actual val Half8Pack.h2: Float get() = Half.fromBits(this.getIntAt(1).extract16(0)).toFloat()
actual val Half8Pack.h3: Float get() = Half.fromBits(this.getIntAt(1).extract16(16)).toFloat()
actual val Half8Pack.h4: Float get() = Half.fromBits(this.getIntAt(2).extract16(0)).toFloat()
actual val Half8Pack.h5: Float get() = Half.fromBits(this.getIntAt(2).extract16(16)).toFloat()
actual val Half8Pack.h6: Float get() = Half.fromBits(this.getIntAt(3).extract16(0)).toFloat()
actual val Half8Pack.h7: Float get() = Half.fromBits(this.getIntAt(3).extract16(16)).toFloat()
actual fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack {
    val v0 = 0.insert16(h0.toHalf().rawBits.toInt(), 0).insert16(h1.toHalf().rawBits.toInt(), 16)
    val v1 = 0.insert16(h2.toHalf().rawBits.toInt(), 0).insert16(h3.toHalf().rawBits.toInt(), 16)
    val v2 = 0.insert16(h4.toHalf().rawBits.toInt(), 0).insert16(h5.toHalf().rawBits.toInt(), 16)
    val v3 = 0.insert16(h6.toHalf().rawBits.toInt(), 0).insert16(h7.toHalf().rawBits.toInt(), 16)
    return vectorOf(v0, v1, v2, v3)
}
