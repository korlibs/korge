package com.soywiz.korma.geom.trapezoid

import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*

@KormaValueApi
data class TriangleInt(
    val p0: PointInt,
    val p1: PointInt,
    val p2: PointInt,
)

@KormaMutableApi
sealed interface ITriangleInt {
    val x0: Int
    val y0: Int
    val x1: Int
    val y1: Int
    val x2: Int
    val y2: Int
}

@KormaMutableApi
data class MTriangleInt(
    override var x0: Int, override var y0: Int,
    override var x1: Int, override var y1: Int,
    override var x2: Int, override var y2: Int,
) : ITriangleInt {
    constructor() : this(0, 0, 0, 0, 0, 0)

    fun copyFrom(other: MTriangleInt) {
        setTo(other.x0, other.y0, other.x1, other.y1, other.x2, other.y2)
    }

    fun setTo(
        x0: Int, y0: Int,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
    ) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2
    }

    override fun toString(): String = "TriangleInt(($x0, $y0), ($x1, $y1), ($x2, $y2))"
}
