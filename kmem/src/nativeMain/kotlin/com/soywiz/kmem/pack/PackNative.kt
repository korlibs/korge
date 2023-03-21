package com.soywiz.kmem.pack

// @TODO: https://youtrack.jetbrains.com/issue/KT-57496/linkReleaseFrameworkIosArm64-e-Compilation-failed-An-operation-is-not-implemented.

/*
import com.soywiz.kmem.*

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

actual typealias BFloat3Half4Pack = Vector128
// 21-bit BFloat precision
actual val BFloat3Half4Pack.b0: Float get() = BFloat21.unpack3(getLongAt(0), 0)
actual val BFloat3Half4Pack.b1: Float get() = BFloat21.unpack3(getLongAt(0), 1)
actual val BFloat3Half4Pack.b2: Float get() = BFloat21.unpack3(getLongAt(0), 2)
// 16-bit Half Float precision
actual val BFloat3Half4Pack.hf0: Float get() = BFloat16.unpack4(getLongAt(1), 0)
actual val BFloat3Half4Pack.hf1: Float get() = BFloat16.unpack4(getLongAt(1), 1)
actual val BFloat3Half4Pack.hf2: Float get() = BFloat16.unpack4(getLongAt(1), 2)
actual val BFloat3Half4Pack.hf3: Float get() = BFloat16.unpack4(getLongAt(1), 3)

actual fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    val pack1 = BFloat21.pack3(b0, b1, b2)
    val pack2 = BFloat16.pack4(hf0, hf1, hf2, hf3)
    return vectorOf(pack1.low, pack1.high, pack2.low, pack2.high)
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

actual typealias Float4Pack = Vector128
actual val Float4Pack.f0: Float get() = this.getFloatAt(0)
actual val Float4Pack.f1: Float get() = this.getFloatAt(1)
actual val Float4Pack.f2: Float get() = this.getFloatAt(2)
actual val Float4Pack.f3: Float get() = this.getFloatAt(3)
actual fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack = vectorOf(f0, f1, f2, f3)

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

actual typealias Int4Pack = Vector128
actual val Int4Pack.i0: Int get() = this.getIntAt(0)
actual val Int4Pack.i1: Int get() = this.getIntAt(1)
actual val Int4Pack.i2: Int get() = this.getIntAt(2)
actual val Int4Pack.i3: Int get() = this.getIntAt(3)
actual fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = vectorOf(i0, i1, i2, i3)
*/
