package com.soywiz.korma.geom

import com.soywiz.kds.pack.*
import com.soywiz.korma.internal.*
import kotlin.math.*

//@KormaExperimental
//@KormaValueApi
inline class Size internal constructor(internal val raw: Float2Pack) {
    operator fun component1(): Float = width
    operator fun component2(): Float = height

    fun avgComponent(): Float = width * 0.5f + height * 0.5f
    fun minComponent(): Float = min(width, height)
    fun maxComponent(): Float = max(width, height)

    val width: Float get() = raw.x
    val height: Float get() = raw.y

    val widthF: Float get() = width
    val heightF: Float get() = height
    val widthD: Double get() = width.toDouble()
    val heightD: Double get() = height.toDouble()

    val area: Float get() = width * height
    val perimeter: Float get() = width * 2 + height * 2

    //(val width: Double, val height: Double) {
    constructor() : this(0f, 0f)
    constructor(width: Float, height: Float) : this(Float2Pack(width, height))
    constructor(width: Double, height: Double) : this(Float2Pack(width.toFloat(), height.toFloat()))
    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())

    operator fun unaryMinus(): Size = Size(-width, -height)
    operator fun unaryPlus(): Size = this

    operator fun minus(other: Size): Size = Size(width - other.width, height - other.height)
    operator fun plus(other: Size): Size = Size(width + other.width, height + other.height)
    operator fun times(s: Float): Size = Size(width * s, height * s)
    operator fun times(s: Double): Size = times(s.toFloat())
    operator fun times(s: Int): Size = times(s.toFloat())
    operator fun div(other: Size): Size = Size(width / other.width, height / other.height)
    operator fun div(s: Float): Size = Size(width / s, height / s)
    operator fun div(s: Double): Size = div(s.toFloat())
    operator fun div(s: Int): Size = div(s.toFloat())

    override fun toString(): String = "Size(width=${width.niceStr}, height=${height.niceStr})"
}

val Size.mutable: MSize get() = MSize(width, height)

val MSize.immutable: Size get() = Size(width, height)

fun MSize.asInt(): MSizeInt = MSizeInt(this)
fun MSizeInt.asDouble(): MSize = this.float

fun MPoint.asSize(): MSize = MSize(this)

fun Point.toSize(): Size = Size(raw)
fun Vector2Int.toSize(): SizeInt = SizeInt(raw)

fun Size.toInt(): SizeInt = SizeInt(width.toInt(), height.toInt())
fun SizeInt.toFloat(): Size = Size(width.toFloat(), height.toFloat())
fun Size.toPoint(): Point = Point(width, height)
