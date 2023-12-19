package korlibs.math.geom

import korlibs.number.*

data class Spacing(
    val vertical: Double,
    val horizontal: Double
) {
    operator fun unaryMinus(): Spacing = Spacing(-vertical, -horizontal)
    operator fun unaryPlus(): Spacing = this
    operator fun plus(other: Spacing): Spacing = Spacing(vertical + other.vertical, horizontal + other.horizontal)
    operator fun minus(other: Spacing): Spacing = Spacing(vertical - other.vertical, horizontal - other.horizontal)
    operator fun times(scale: Double): Spacing = Spacing(vertical * scale, horizontal * scale)
    operator fun div(scale: Double): Spacing = this * (1.0 / scale)
    operator fun rem(scale: Double): Spacing = Spacing(vertical % scale, horizontal % scale)
    operator fun rem(scale: Spacing): Spacing = Spacing(vertical % scale.vertical, horizontal % scale.horizontal)

    companion object {
        val ZERO = Spacing(0.0, 0.0)

        inline operator fun invoke(spacing: Number): Spacing = Spacing(spacing.toDouble(), spacing.toDouble())
        inline operator fun invoke(vertical: Number, horizontal: Number): Spacing = Spacing(vertical.toDouble(), horizontal.toDouble())
    }

    constructor(spacing: Double) : this(spacing, spacing)

    override fun toString(): String = "Spacing(vertical=${vertical.niceStr}, horizontal=${horizontal.niceStr})"
}
