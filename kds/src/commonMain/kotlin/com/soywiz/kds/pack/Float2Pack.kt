package com.soywiz.kds.pack

inline class Float2Pack private constructor(val data: Long) {
    constructor(x: Float, y: Float) : this(packFloat2(x, y))

    val x: Float get() = unpackFloat2X(data)
    val y: Float get() = unpackFloat2Y(data)
}

//private inline fun packFloat2(x: Float, y: Float): Long = (x.toRawBits().toLong() and 0xFFFFFFFFL) or (y.toRawBits().toLong() shl 32)
//private inline fun unpackFloat2X(v: Long): Float = Float.fromBits(v.toInt())
//private inline fun unpackFloat2Y(v: Long): Float = Float.fromBits((v shr 32).toInt())

internal expect inline fun packFloat2(x: Float, y: Float): Long
internal expect inline fun unpackFloat2X(v: Long): Float
internal expect inline fun unpackFloat2Y(v: Long): Float
