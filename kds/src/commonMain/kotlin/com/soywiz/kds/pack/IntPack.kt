package com.soywiz.kds.pack

inline class IntPack private constructor(val data: Long) {
    constructor(x: Int, y: Int) : this(packInt2(x, y))

    val x: Int get() = unpackInt2X(data)
    val y: Int get() = unpackInt2Y(data)
}

internal expect inline fun packInt2(x: Int, y: Int): Long
internal expect inline fun unpackInt2X(v: Long): Int
internal expect inline fun unpackInt2Y(v: Long): Int
