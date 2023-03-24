package korlibs.math.geom

import korlibs.memory.pack.*
import korlibs.math.interpolation.*

//@KormaValueApi
//data class Anchor(val sx: Double, val sy: Double) : Interpolable<Anchor> {
inline class Anchor internal constructor(internal val raw: Float2Pack) : Interpolable<Anchor> {
    val sx: Float get() = raw.f0
    val sy: Float get() = raw.f1

    val floatX: Float get() = raw.f0
    val floatY: Float get() = raw.f1

    val doubleX: Double get() = sx.toDouble()
    val doubleY: Double get() = sy.toDouble()

    val ratioX: Ratio get() = sx.toRatio()
    val ratioY: Ratio get() = sy.toRatio()

    constructor(sx: Float, sy: Float) : this(float2PackOf(sx, sy))
    constructor(sx: Double, sy: Double) : this(sx.toFloat(), sy.toFloat())
    constructor(sx: Int, sy: Int) : this(sx.toFloat(), sy.toFloat())

    companion object {
        val TOP_LEFT: Anchor = Anchor(0f, 0f)
        val TOP_CENTER: Anchor = Anchor(.5f, 0f)
        val TOP_RIGHT: Anchor = Anchor(1f, 0f)

        val MIDDLE_LEFT: Anchor = Anchor(0f, .5f)
        val MIDDLE_CENTER: Anchor = Anchor(.5f, .5f)
        val MIDDLE_RIGHT: Anchor = Anchor(1f, .5f)

        val BOTTOM_LEFT: Anchor = Anchor(0f, 1f)
        val BOTTOM_CENTER: Anchor = Anchor(.5f, 1f)
        val BOTTOM_RIGHT: Anchor = Anchor(1f, 1f)

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