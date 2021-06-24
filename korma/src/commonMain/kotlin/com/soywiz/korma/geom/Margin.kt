package com.soywiz.korma.geom

data class Margin(val top: Double, val right: Double, val bottom: Double, val left: Double) {
    constructor(vertical: Double, horizontal: Double) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Double) : this(margin, margin, margin, margin)

    val leftPlusRight get() = left + right
    val topPlusBottom get() = top + bottom

    val horizontal get() = (left + right) / 2
    val vertical get() = (top + bottom) / 2
}
