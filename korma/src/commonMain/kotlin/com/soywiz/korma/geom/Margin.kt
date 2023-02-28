package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.kds.pack.*
import com.soywiz.korma.annotations.*

// @TODO: value class
@KormaValueApi
data class Margin(
    val top: Double,
    val right: Double,
    val bottom: Double,
    val left: Double,
)

// @TODO: value class
@KormaValueApi
data class MarginInt internal constructor(val raw: Short4Pack) {
    val top: Short get() = raw.x
    val right: Short get() = raw.y
    val bottom: Short get() = raw.z
    val left: Short get() = raw.w

    constructor(top: Short, right: Short, bottom: Short, left: Short) : this(Short4Pack(top, right, bottom, left))
}

@KormaMutableApi
interface IMargin {
    val top: Double
    val right: Double
    val bottom: Double
    val left: Double

    val isNotZero: Boolean get() = top != 0.0 || left != 0.0 || right != 0.0 || bottom != 0.0
    val leftPlusRight: Double get() = left + right
    val topPlusBottom: Double get() = top + bottom
    val horizontal: Double get() = (left + right) / 2
    val vertical: Double get() = (top + bottom) / 2

    fun duplicate(
        top: Double = this.top,
        right: Double = this.right,
        bottom: Double = this.bottom,
        left: Double = this.left,
    ): IMargin = MMargin(top, right, bottom, left)

    companion object {
        val EMPTY: IMargin = MMargin()

        operator fun invoke(top: Double, right: Double, bottom: Double, left: Double): IMargin = MMargin(top, right, bottom, left)
        operator fun invoke(vertical: Double, horizontal: Double): IMargin = MMargin(vertical, horizontal)
        operator fun invoke(margin: Double): IMargin = MMargin(margin)
    }
}

@KormaMutableApi
data class MMargin(
    override var top: Double = 0.0,
    override var right: Double = 0.0,
    override var bottom: Double = 0.0,
    override var left: Double = 0.0
) : IMargin {
    constructor(vertical: Double, horizontal: Double) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Double) : this(margin, margin, margin, margin)

    fun setTo(margin: Double): Unit = setTo(margin, margin, margin, margin)
    fun setTo(vertical: Double, horizontal: Double): Unit = setTo(vertical, horizontal, vertical, horizontal)
    fun setTo(top: Double, right: Double, bottom: Double, left: Double) {
        this.top = top
        this.right = right
        this.left = left
        this.bottom = bottom
    }
    fun copyFrom(other: IMargin) {
        setTo(other.top, other.right, other.bottom, other.left)
    }
}

@KormaMutableApi
interface IMarginInt {
    val top: Int
    val right: Int
    val bottom: Int
    val left: Int

    val isNotZero: Boolean get() = top != 0 || left != 0 || right != 0 || bottom != 0
    val leftPlusRight: Int get() = left + right
    val topPlusBottom: Int get() = top + bottom
    val horizontal: Int get() = (left + right) / 2
    val vertical: Int get() = (top + bottom) / 2

    companion object {
        val ZERO: IMarginInt = IMarginInt(0)
        operator fun invoke(top: Int, right: Int, bottom: Int, left: Int): IMarginInt = MMarginInt(top, right, bottom, left)
        operator fun invoke(vertical: Int, horizontal: Int): IMarginInt = MMarginInt(vertical, horizontal)
        operator fun invoke(margin: Int): IMarginInt = MMarginInt(margin)
    }
}

@KormaMutableApi
data class MMarginInt(
    override var top: Int = 0,
    override var right: Int = 0,
    override var bottom: Int = 0,
    override var left: Int = 0
) : IMarginInt {
    companion object {
        val POOL: ConcurrentPool<MMarginInt> = ConcurrentPool<MMarginInt>({ it.setTo(0) }) { MMarginInt() }
    }

    constructor(vertical: Int, horizontal: Int) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Int) : this(margin, margin, margin, margin)

    fun setTo(margin: Int): Unit = setTo(margin, margin, margin, margin)
    fun setTo(vertical: Int, horizontal: Int): Unit = setTo(vertical, horizontal, vertical, horizontal)
    fun setTo(top: Int, right: Int, bottom: Int, left: Int) {
        this.top = top
        this.right = right
        this.left = left
        this.bottom = bottom
    }
    fun copyFrom(other: IMarginInt) {
        setTo(other.top, other.right, other.bottom, other.left)
    }
}
