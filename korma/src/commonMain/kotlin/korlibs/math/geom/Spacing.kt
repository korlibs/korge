package korlibs.math.geom

import korlibs.math.internal.*

data class Spacing(
    val vertical: Float,
    val horizontal: Float
) {
    constructor(spacing: Float) : this(spacing, spacing)

    override fun toString(): String = "Spacing(vertical=${vertical.niceStr}, horizontal=${horizontal.niceStr})"

    companion object {
        val ZERO = Spacing(0f, 0f)
    }
}
