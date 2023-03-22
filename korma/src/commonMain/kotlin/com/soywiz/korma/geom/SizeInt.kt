package com.soywiz.korma.geom

import com.soywiz.kmem.pack.*

inline class SizeInt internal constructor(internal val raw: Int2Pack) {
    operator fun component1(): Int = width
    operator fun component2(): Int = height

    fun avgComponent(): Int = (width + height) / 2
    fun minComponent(): Int = kotlin.math.min(width, height)
    fun maxComponent(): Int = kotlin.math.max(width, height)

    val width: Int get() = raw.i0
    val height: Int get() = raw.i1

    val area: Int get() = width * height
    val perimeter: Int get() = width * 2 + height * 2

    constructor() : this(0, 0)
    constructor(width: Int, height: Int) : this(int2PackOf(width, height))
}
