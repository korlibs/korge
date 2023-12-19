package korlibs.math.geom

import korlibs.math.interpolation.*

typealias Anchor = Anchor2D
typealias Anchor3 = Anchor3F

data class Anchor2D(val sx: Double, val sy: Double) : Interpolable<Anchor> {
    fun toVector(): Vector2D = Vector2D(sx, sy)

    val ratioX: Ratio get() = sx.toRatio()
    val ratioY: Ratio get() = sy.toRatio()

    constructor(sx: Float, sy: Float) : this(sx.toDouble(), sy.toDouble())
    constructor(sx: Int, sy: Int) : this(sx.toDouble(), sy.toDouble())

    inline fun withX(sx: Number): Anchor = Anchor(sx.toDouble(), sy)
    inline fun withY(sy: Number): Anchor = Anchor(sx, sy.toDouble())

    inline fun withX(ratioX: Ratio): Anchor = Anchor(ratioX.toDouble(), sy)
    inline fun withY(ratioY: Ratio): Anchor = Anchor(sx, ratioY.toDouble())

    companion object {
        inline operator fun invoke(sx: Ratio, sy: Ratio): Anchor2D = Anchor2D(sx.toDouble(), sy.toDouble())
        inline operator fun invoke(sx: Number, sy: Number): Anchor2D = Anchor2D(sx.toDouble(), sy.toDouble())

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

data class Anchor3F(val sx: Float, val sy: Float, val sz: Float) : Interpolable<Anchor3F> {
    fun toVector(): Vector3F = Vector3F(sx, sy, sz)

    val floatX: Float get() = sx
    val floatY: Float get() = sy
    val floatZ: Float get() = sz

    val doubleX: Double get() = sx.toDouble()
    val doubleY: Double get() = sy.toDouble()
    val doubleZ: Double get() = sz.toDouble()

    val ratioX: Ratio get() = sx.toRatio()
    val ratioY: Ratio get() = sy.toRatio()
    val ratioZ: Ratio get() = sz.toRatio()

    constructor(sx: Double, sy: Double, sz: Double) : this(sx.toFloat(), sy.toFloat(), sz.toFloat())
    constructor(sx: Int, sy: Int, sz: Int) : this(sx.toFloat(), sy.toFloat(), sz.toFloat())

    fun withX(sx: Float): Anchor3F = Anchor3F(sx, sy, sz)
    fun withX(sx: Int): Anchor3F = Anchor3F(sx.toFloat(), sy, sz)
    fun withX(sx: Double): Anchor3F = Anchor3F(sx.toFloat(), sy, sz)

    fun withY(sy: Float): Anchor3F = Anchor3F(sx, sy, sz)
    fun withY(sy: Int): Anchor3F = Anchor3F(sx, sy.toFloat(), sz)
    fun withY(sy: Double): Anchor3F = Anchor3F(sx, sy.toFloat(), sz)

    fun withZ(sz: Float): Anchor3F = Anchor3F(sx, sy, sz)
    fun withZ(sz: Int): Anchor3F = Anchor3F(sx, sy, sz.toFloat())
    fun withZ(sz: Double): Anchor3F = Anchor3F(sx, sy, sz.toFloat())

    override fun interpolateWith(ratio: Ratio, other: Anchor3F): Anchor3F = Anchor3F(
        ratio.interpolate(this.sx, other.sx),
        ratio.interpolate(this.sy, other.sy),
        ratio.interpolate(this.sz, other.sz),
    )
}
