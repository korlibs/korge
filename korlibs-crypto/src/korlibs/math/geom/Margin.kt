package korlibs.math.geom

import korlibs.math.*
import korlibs.number.*

/**
 * A [top], [right], [bottom], [left] pack with FixedShort (16-bit) in the range of +-3275.9 (3.3 integer digits + 1 decimal digit)
 */
data class Margin(
    val top: Double,
    val right: Double,
    val bottom: Double,
    val left: Double,
) : IsAlmostEquals<Margin> {
    companion object {
        val ZERO = Margin(0.0, 0.0, 0.0, 0.0)

        inline operator fun invoke(margin: Number): Margin = Margin(margin.toDouble(), margin.toDouble(), margin.toDouble(), margin.toDouble())
        inline operator fun invoke(vertical: Number, horizontal: Number): Margin = Margin(vertical.toDouble(), horizontal.toDouble(), vertical.toDouble(), horizontal.toDouble())
        inline operator fun invoke(top: Number, right: Number, bottom: Number, left: Number): Margin = Margin(top.toDouble(), right.toDouble(), bottom.toDouble(), left.toDouble())
    }

    constructor(vertical: Double, horizontal: Double) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Double) : this(margin, margin, margin, margin)

    operator fun plus(other: Margin): Margin = Margin(top + other.top, right + other.right, bottom + other.bottom, left + other.left)
    operator fun minus(other: Margin): Margin = Margin(top - other.top, right - other.right, bottom - other.bottom, left - other.left)

    val isNotZero: Boolean get() = top != 0.0 || left != 0.0 || right != 0.0 || bottom != 0.0

    override fun isAlmostEquals(other: Margin, epsilon: Double): Boolean =
        this.left.isAlmostEquals(other.left, epsilon) &&
            this.right.isAlmostEquals(other.right, epsilon) &&
            this.top.isAlmostEquals(other.top, epsilon) &&
            this.bottom.isAlmostEquals(other.bottom, epsilon)
    fun isAlmostZero(epsilon: Double = 0.000001): Boolean = isAlmostEquals(ZERO, epsilon)

    val topFixed: FixedShort get() = top.toFixedShort()
    val rightFixed: FixedShort get() = right.toFixedShort()
    val bottomFixed: FixedShort get() = bottom.toFixedShort()
    val leftFixed: FixedShort get() = left.toFixedShort()

    val leftPlusRightFixed: FixedShort get() = leftFixed + rightFixed
    val topPlusBottomFixed: FixedShort get() = topFixed + bottomFixed
    val horizontalFixed: FixedShort get() = (leftFixed + rightFixed) / 2.toFixedShort()
    val verticalFixed: FixedShort get() = (topFixed + bottomFixed) / 2.toFixedShort()

    val leftPlusRight: Double get() = left + right
    val topPlusBottom: Double get() = top + bottom

    val horizontal: Double get() = (left + right) / 2
    val vertical: Double get() = (top + bottom) / 2

    override fun toString(): String = "Margin(top=${top.niceStr}, right=${right.niceStr}, bottom=${bottom.niceStr}, left=${left.niceStr})"
}

/**
 * A [top], [right], [bottom], [left] pack with Int)
 */
data class MarginInt(
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int,
) {
    constructor(top: Short, right: Short, bottom: Short, left: Short) : this(top.toInt(), right.toInt(), bottom.toInt(), left.toInt())
    constructor(vertical: Int, horizontal: Int) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Int) : this(margin, margin, margin, margin)

    operator fun plus(other: MarginInt): MarginInt = MarginInt(top + other.top, right + other.right, bottom + other.bottom, left + other.left)
    operator fun minus(other: MarginInt): MarginInt = MarginInt(top - other.top, right - other.right, bottom - other.bottom, left - other.left)

    val isNotZero: Boolean get() = top != 0 || left != 0 || right != 0 || bottom != 0

    val leftPlusRight: Int get() = left + right
    val topPlusBottom: Int get() = top + bottom
    val horizontal: Int get() = (left + right) / 2
    val vertical: Int get() = (top + bottom) / 2

    companion object {
        val ZERO = MarginInt(0, 0, 0, 0)
    }

    override fun toString(): String = "MarginInt(top=${top}, right=${right}, bottom=${bottom}, left=${left})"
}
