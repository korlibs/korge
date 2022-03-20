package com.soywiz.korma.geom

interface Margin {
    val top: Double
    val right: Double
    val bottom: Double
    val left: Double

    companion object {
        operator fun invoke(top: Double, right: Double, bottom: Double, left: Double): Margin = MutableMargin(top, right, bottom, left)
        operator fun invoke(vertical: Double, horizontal: Double): Margin = MutableMargin(vertical, horizontal)
        operator fun invoke(margin: Double): Margin = MutableMargin(margin)
    }
}

val Margin.leftPlusRight: Double get() = left + right
val Margin.topPlusBottom: Double get() = top + bottom

val Margin.horizontal: Double get() = (left + right) / 2
val Margin.vertical: Double get() = (top + bottom) / 2

data class MutableMargin(
    override var top: Double = 0.0,
    override var right: Double = 0.0,
    override var bottom: Double = 0.0,
    override var left: Double = 0.0
) : Margin {
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
    fun copyFrom(other: Margin) {
        setTo(other.top, other.right, other.bottom, other.left)
    }
}

interface MarginInt {
    val top: Int
    val right: Int
    val bottom: Int
    val left: Int

    companion object {
        operator fun invoke(top: Int, right: Int, bottom: Int, left: Int): MarginInt = MutableMarginInt(top, right, bottom, left)
        operator fun invoke(vertical: Int, horizontal: Int): MarginInt = MutableMarginInt(vertical, horizontal)
        operator fun invoke(margin: Int): MarginInt = MutableMarginInt(margin)
    }
}

val MarginInt.leftPlusRight: Int get() = left + right
val MarginInt.topPlusBottom: Int get() = top + bottom

val MarginInt.horizontal: Int get() = (left + right) / 2
val MarginInt.vertical: Int get() = (top + bottom) / 2

data class MutableMarginInt(
    override var top: Int = 0,
    override var right: Int = 0,
    override var bottom: Int = 0,
    override var left: Int = 0
) : MarginInt {
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
    fun copyFrom(other: MarginInt) {
        setTo(other.top, other.right, other.bottom, other.left)
    }
}
