package korlibs.math.geom

import korlibs.math.interpolation.*

data class Anchor(val sx: Float, val sy: Float) : Interpolable<Anchor> {
    fun toVector(): Vector2 = Vector2(sx, sy)

    val floatX: Float get() = sx
    val floatY: Float get() = sy

    val doubleX: Double get() = sx.toDouble()
    val doubleY: Double get() = sy.toDouble()

    val ratioX: Ratio get() = sx.toRatio()
    val ratioY: Ratio get() = sy.toRatio()

    constructor(sx: Double, sy: Double) : this(sx.toFloat(), sy.toFloat())
    constructor(sx: Int, sy: Int) : this(sx.toFloat(), sy.toFloat())

    fun withX(sx: Float): Anchor = Anchor(sx, sy)
    fun withX(sx: Int): Anchor = Anchor(sx.toFloat(), sy)
    fun withX(sx: Double): Anchor = Anchor(sx.toFloat(), sy)

    fun withY(sy: Float): Anchor = Anchor(sx, sy)
    fun withY(sy: Int): Anchor = Anchor(sx, sy.toFloat())
    fun withY(sy: Double): Anchor = Anchor(sx, sy.toFloat())

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

operator fun Size.times(anchor: Anchor): Point = this.toVector() * anchor.toVector()
//operator fun SizeInt.times(anchor: Anchor): PointInt = (this.toVector().toFloat() * anchor.toVector()).toInt()
