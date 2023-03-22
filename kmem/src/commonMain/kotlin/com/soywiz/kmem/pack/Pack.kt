package com.soywiz.kmem.pack

import kotlin.jvm.*

@JvmInline value class Half8Pack(
    val h0: Float,
    val h1: Float,
    val h2: Float,
    val h3: Float,
    val h4: Float,
    val h5: Float,
    val h6: Float,
    val h7: Float,
)
val Half8Pack.h0: Float get() = this.h0
val Half8Pack.h1: Float get() = this.h1
val Half8Pack.h2: Float get() = this.h2
val Half8Pack.h3: Float get() = this.h3
val Half8Pack.h4: Float get() = this.h4
val Half8Pack.h5: Float get() = this.h5
val Half8Pack.h6: Float get() = this.h6
val Half8Pack.h7: Float get() = this.h7
fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack {
    return Half8Pack(h0, h1, h2, h3, h4, h5, h6, h7)
}







@JvmInline value class Float4Pack(val x: Float, val y: Float, val z: Float, val w: Float)
val Float4Pack.f0: Float get() = this.x
val Float4Pack.f1: Float get() = this.y
val Float4Pack.f2: Float get() = this.z
val Float4Pack.f3: Float get() = this.w
fun float4PackOf(f0: Float, f1: Float, f2: Float, f3: Float): Float4Pack = Float4Pack(f0, f1, f2, f3)








@JvmInline value class BFloat6Pack(
    val f0: Float,
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val f4: Float,
    val f5: Float,
)

val BFloat6Pack.bf0: Float get() = f0
val BFloat6Pack.bf1: Float get() = f1
val BFloat6Pack.bf2: Float get() = f2
val BFloat6Pack.bf3: Float get() = f3
val BFloat6Pack.bf4: Float get() = f4
val BFloat6Pack.bf5: Float get() = f5

fun bfloat6PackOf(bf0: Float, bf1: Float, bf2: Float, bf3: Float, bf4: Float, bf5: Float): BFloat6Pack =
    BFloat6Pack(bf0, bf1, bf2, bf3, bf4, bf5)



@JvmInline value class BFloat3Half4Pack(
    val f0: Float,
    val f1: Float,
    val f2: Float,
    val f3: Float,
    val f4: Float,
    val f5: Float,
    val f6: Float,
)
// 21-bit BFloat precision
val BFloat3Half4Pack.b0: Float get() = f0
val BFloat3Half4Pack.b1: Float get() = f1
val BFloat3Half4Pack.b2: Float get() = f2
// 16-bit Half Float precision
val BFloat3Half4Pack.hf0: Float get() = f3
val BFloat3Half4Pack.hf1: Float get() = f4
val BFloat3Half4Pack.hf2: Float get() = f5
val BFloat3Half4Pack.hf3: Float get() = f6

fun bfloat3Half4PackOf(
    b0: Float, b1: Float, b2: Float,
    hf0: Float, hf1: Float, hf2: Float, hf3: Float
): BFloat3Half4Pack {
    return BFloat3Half4Pack(b0, b1, b2, hf0, hf1, hf2, hf3)
}


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


@JvmInline value class Int4Pack(val x: Int, val y: Int, val z: Int, val w: Int)
val Int4Pack.i0: Int get() = this.x
val Int4Pack.i1: Int get() = this.y
val Int4Pack.i2: Int get() = this.z
val Int4Pack.i3: Int get() = this.w
fun int4PackOf(i0: Int, i1: Int, i2: Int, i3: Int): Int4Pack = Int4Pack(i0, i1, i2, i3)





@JvmInline value class Float2Pack(val x: Float, val y: Float)
val Float2Pack.f0: Float get() = x
val Float2Pack.f1: Float get() = y
fun float2PackOf(f0: Float, f1: Float): Float2Pack = Float2Pack(f0, f1)

@JvmInline value class Int2Pack(val x: Int, val y: Int)
val Int2Pack.i0: Int get() = x
val Int2Pack.i1: Int get() = y
fun int2PackOf(i0: Int, i1: Int): Int2Pack = Int2Pack(i0, i1)

@JvmInline value class Short4Pack(val x: Short, val y: Short, val z: Short, val w: Short)
val Short4Pack.s0: Short get() = x
val Short4Pack.s1: Short get() = y
val Short4Pack.s2: Short get() = z
val Short4Pack.s3: Short get() = w
fun short4PackOf(s0: Short, s1: Short, s2: Short, s3: Short): Short4Pack = Short4Pack(s0, s1, s2, s3)
