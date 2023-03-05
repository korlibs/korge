package com.soywiz.kds.pack

inline class Short4Pack private constructor(val data: Long) {
    constructor(x: Short, y: Short, z: Short, w: Short) : this(packShort4(x, y, z, w))

    val x: Short get() = unpackShort4X(data)
    val y: Short get() = unpackShort4Y(data)
    val z: Short get() = unpackShort4Z(data)
    val w: Short get() = unpackShort4W(data)
}

internal expect inline fun packShort4(x: Short, y: Short, z: Short, w: Short): Long
internal expect inline fun unpackShort4X(v: Long): Short
internal expect inline fun unpackShort4Y(v: Long): Short
internal expect inline fun unpackShort4Z(v: Long): Short
internal expect inline fun unpackShort4W(v: Long): Short
