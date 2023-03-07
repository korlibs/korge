package com.soywiz.korma.geom

import com.soywiz.kds.pack.*
import com.soywiz.kmem.*
import com.soywiz.kmem.pack.*

// @TODO: value class
/**
 * A [top], [right], [bottom], [left] pack with Half (16-bit) floating point precision
 */
inline class Margin internal constructor(val raw: Half4Pack) {
    constructor(top: Float, right: Float, bottom: Float, left: Float) : this(Half4Pack(top.toHalf(), right.toHalf(), bottom.toHalf(), left.toHalf()))
    constructor(vertical: Float, horizontal: Float) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Float) : this(margin, margin, margin, margin)

    val isNotZero: Boolean get() = top != 0f || left != 0f || right != 0f || bottom != 0f

    val top: Float get() = raw.x.toFloat()
    val right: Float get() = raw.y.toFloat()
    val bottom: Float get() = raw.z.toFloat()
    val left: Float get() = raw.w.toFloat()

    val leftPlusRight: Float get() = left + right
    val topPlusBottom: Float get() = top + bottom

    val horizontal: Float get() = (left + right) / 2
    val vertical: Float get() = (top + bottom) / 2

    companion object {
        val ZERO = Margin(0f, 0f, 0f, 0f)
    }
}

// @TODO: Value Class when MFVC is available
/**
 * A [top], [right], [bottom], [left] pack with Short (16-bit) precision
 */
inline class MarginInt internal constructor(val raw: Short4Pack) {
    constructor(top: Short, right: Short, bottom: Short, left: Short) : this(Short4Pack(top, right, bottom, left))
    constructor(top: Int, right: Int, bottom: Int, left: Int) : this(Short4Pack(top.toShortClamped(), right.toShortClamped(), bottom.toShortClamped(), left.toShortClamped()))
    constructor(vertical: Int, horizontal: Int) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Int) : this(margin, margin, margin, margin)

    companion object {
        val ZERO = MarginInt(Short4Pack(0, 0, 0, 0))
    }

    val top: Int get() = raw.x.toInt()
    val right: Int get() = raw.y.toInt()
    val bottom: Int get() = raw.z.toInt()
    val left: Int get() = raw.w.toInt()

    val isNotZero: Boolean get() = top != 0 || left != 0 || right != 0 || bottom != 0
    val leftPlusRight: Int get() = left + right
    val topPlusBottom: Int get() = top + bottom
    val horizontal: Int get() = (left + right) / 2
    val vertical: Int get() = (top + bottom) / 2
}
