package com.soywiz.korma.geom

import com.soywiz.kmem.*
import com.soywiz.kmem.pack.*

inline class PointFixed internal constructor(internal val raw: Int2Pack) {
    val x: Fixed get() = Fixed.fromRaw(raw.x)
    val y: Fixed get() = Fixed.fromRaw(raw.y)
    constructor(x: Fixed, y: Fixed) : this(Int2Pack(x.raw, y.raw))

    operator fun unaryMinus(): PointFixed = PointFixed(-this.x, -this.y)
    operator fun unaryPlus(): PointFixed = this

    operator fun plus(that: PointFixed): PointFixed = PointFixed(this.x + that.x, this.y + that.y)
    operator fun minus(that: PointFixed): PointFixed = PointFixed(this.x - that.x, this.y - that.y)
    operator fun times(that: PointFixed): PointFixed = PointFixed(this.x * that.x, this.y * that.y)
    operator fun times(that: Fixed): PointFixed = PointFixed(this.x * that, this.y * that)
    operator fun div(that: PointFixed): PointFixed = PointFixed(this.x / that.x, this.y / that.y)
    operator fun rem(that: PointFixed): PointFixed = PointFixed(this.x % that.x, this.y % that.y)

    override fun toString(): String = "($x, $y)"
}
