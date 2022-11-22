package com.soywiz.korma.geom.trapezoid

data class TriangleInt(
    var x0: Int, var y0: Int,
    var x1: Int, var y1: Int,
    var x2: Int, var y2: Int,
) {
    constructor() : this(0, 0, 0, 0, 0, 0)

    fun copyFrom(other: TriangleInt) {
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
}
