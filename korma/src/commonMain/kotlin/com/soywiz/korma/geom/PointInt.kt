package com.soywiz.korma.geom

import com.soywiz.kds.pack.*

//@KormaValueApi
inline class PointInt internal constructor(internal val raw: Int2Pack) {
    val x: Int get() = raw.x
    val y: Int get() = raw.y

    constructor() : this(Int2Pack(0, 0))
    constructor(x: Int, y: Int) : this(Int2Pack(x, y))

    val mutable: IPointInt get() = MPointInt(x, y)
    operator fun component1(): Int = x
    operator fun component2(): Int = y

    fun copy(x: Int = this.x, y: Int = this.y): PointInt = PointInt(x, y)

    operator fun plus(that: PointInt): PointInt = PointInt(this.x + that.x, this.y + that.y)
    operator fun minus(that: PointInt): PointInt = PointInt(this.x - that.x, this.y - that.y)
    operator fun times(that: PointInt): PointInt = PointInt(this.x * that.x, this.y * that.y)
    operator fun div(that: PointInt): PointInt = PointInt(this.x / that.x, this.y / that.y)
    operator fun rem(that: PointInt): PointInt = PointInt(this.x % that.x, this.y % that.y)
}
