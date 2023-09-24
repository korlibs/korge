package korlibs.math.geom

import korlibs.number.*

data class Spacing(
    val vertical: Double,
    val horizontal: Double
) {
    companion object {
        val ZERO = Spacing(0.0, 0.0)

        inline operator fun invoke(spacing: Number): Spacing = Spacing(spacing.toDouble(), spacing.toDouble())
        inline operator fun invoke(vertical: Number, horizontal: Number): Spacing = Spacing(vertical.toDouble(), horizontal.toDouble())
    }

    constructor(spacing: Double) : this(spacing, spacing)

    override fun toString(): String = "Spacing(vertical=${vertical.niceStr}, horizontal=${horizontal.niceStr})"
}
