package com.soywiz.kmem.pack

import com.soywiz.kmem.*

inline class Half4Pack private constructor(val data: Long) {
    constructor(x: Half, y: Half, z: Half, w: Half) : this(packHalf4(x, y, z, w))

    val x: Half get() = unpackHalf4X(data)
    val y: Half get() = unpackHalf4Y(data)
    val z: Half get() = unpackHalf4Z(data)
    val w: Half get() = unpackHalf4W(data)
}

internal expect inline fun packHalf4(x: Half, y: Half, z: Half, w: Half): Long
internal expect inline fun unpackHalf4X(v: Long): Half
internal expect inline fun unpackHalf4Y(v: Long): Half
internal expect inline fun unpackHalf4Z(v: Long): Half
internal expect inline fun unpackHalf4W(v: Long): Half
