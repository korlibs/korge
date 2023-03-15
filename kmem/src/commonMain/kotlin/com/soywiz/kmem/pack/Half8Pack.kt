package com.soywiz.kmem.pack

import com.soywiz.kmem.*

/*
inline class Half8Pack private constructor(val data: Int4Pack) {
    constructor(
        h0: Float,
        h1: Float,
        h2: Float,
        h3: Float,
        h4: Float,
        h5: Float,
        h6: Float,
        h7: Float,
    ) : this(packHalf8(h0, h1, h2, h3, h4, h5, h6, h7))

    val x: Half get() = unpackHalf4X(data)
    val y: Half get() = unpackHalf4Y(data)
    val z: Half get() = unpackHalf4Z(data)
    val w: Half get() = unpackHalf4W(data)
}

internal expect inline fun packHalf8(
    h0: Float,
    h1: Float,
    h2: Float,
    h3: Float,
    h4: Float,
    h5: Float,
    h6: Float,
    h7: Float,
): Int4Pack
internal expect inline fun unpackHalf4_h0(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h1(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h2(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h3(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h4(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h5(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h6(v: Int4Pack): Float
internal expect inline fun unpackHalf4_h7(v: Int4Pack): Float
*/

expect class Half8Pack
expect val Half8Pack.h0: Float
expect val Half8Pack.h1: Float
expect val Half8Pack.h2: Float
expect val Half8Pack.h3: Float
expect val Half8Pack.h4: Float
expect val Half8Pack.h5: Float
expect val Half8Pack.h6: Float
expect val Half8Pack.h7: Float
expect fun half8PackOf(h0: Float, h1: Float, h2: Float, h3: Float, h4: Float, h5: Float, h6: Float, h7: Float): Half8Pack

//fun Float4Pack.copy(f0: Float = this.f0, f1: Float = this.f1, f2: Float = this.f2, f3: Float = this.f3): Float4Pack =
//    float4PackOf(f0, f1, f2, f3)
