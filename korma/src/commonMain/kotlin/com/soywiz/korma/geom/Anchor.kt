package com.soywiz.korma.geom

import com.soywiz.kds.pack.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.interpolation.*

//@KormaValueApi
//data class Anchor(val sx: Double, val sy: Double) : Interpolable<Anchor> {
inline class Anchor internal constructor(internal val raw: Float2Pack) : Interpolable<Anchor> {
    val sx: Float get() = raw.x
    val sy: Float get() = raw.y

    val sxF: Float get() = sx
    val syF: Float get() = sy

    val sxD: Double get() = sx.toDouble()
    val syD: Double get() = sy.toDouble()

    constructor(sx: Float, sy: Float) : this(Float2Pack(sx, sy))
    constructor(sx: Double, sy: Double) : this(sx.toFloat(), sy.toFloat())
    constructor(sx: Int, sy: Int) : this(sx.toFloat(), sy.toFloat())

    companion object {
        val TOP_LEFT: Anchor = Anchor(0.0, 0.0)
        val TOP_CENTER: Anchor = Anchor(0.5, 0.0)
        val TOP_RIGHT: Anchor = Anchor(1.0, 0.0)

        val MIDDLE_LEFT: Anchor = Anchor(0.0, 0.5)
        val MIDDLE_CENTER: Anchor = Anchor(0.5, 0.5)
        val MIDDLE_RIGHT: Anchor = Anchor(1.0, 0.5)

        val BOTTOM_LEFT: Anchor = Anchor(0.0, 1.0)
        val BOTTOM_CENTER: Anchor = Anchor(0.5, 1.0)
        val BOTTOM_RIGHT: Anchor = Anchor(1.0, 1.0)

        val TOP: Anchor get() = TOP_CENTER
        val LEFT: Anchor get() = MIDDLE_LEFT
        val RIGHT: Anchor get() = MIDDLE_RIGHT
        val BOTTOM: Anchor get() = BOTTOM_CENTER
        val CENTER: Anchor get() = MIDDLE_CENTER
    }

    override fun interpolateWith(ratio: Ratio, other: Anchor): Anchor = Anchor(
        ratio.interpolate(this.sx, other.sx),
        ratio.interpolate(this.sy, other.sy)
    )

    fun toNamedString(): String = when (this) {
        TOP_LEFT -> "Anchor.TOP_LEFT"
        TOP -> "Anchor.TOP"
        TOP_RIGHT -> "Anchor.TOP_RIGHT"
        LEFT -> "Anchor.LEFT"
        CENTER -> "Anchor.MIDDLE_CENTER"
        RIGHT -> "Anchor.RIGHT"
        BOTTOM_LEFT -> "Anchor.BOTTOM_LEFT"
        BOTTOM_CENTER -> "Anchor.BOTTOM_CENTER"
        BOTTOM_RIGHT -> "Anchor.BOTTOM_RIGHT"
        else -> toString()
    }
}
