package korlibs.math.geom

import korlibs.memory.*
import korlibs.math.internal.*

/**
 * A [top], [right], [bottom], [left] pack with FixedShort (16-bit) in the range of +-3275.9 (3.3 integer digits + 1 decimal digit)
 */
data class Margin(
    val top: Float,
    val right: Float,
    val bottom: Float,
    val left: Float,
) {
    constructor(vertical: Float, horizontal: Float) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Float) : this(margin, margin, margin, margin)

    operator fun plus(other: Margin): Margin = Margin(top + other.top, right + other.right, bottom + other.bottom, left + other.left)
    operator fun minus(other: Margin): Margin = Margin(top - other.top, right - other.right, bottom - other.bottom, left - other.left)

    val isNotZero: Boolean get() = top != 0f || left != 0f || right != 0f || bottom != 0f

    val topFixed: FixedShort get() = top.toFixedShort()
    val rightFixed: FixedShort get() = right.toFixedShort()
    val bottomFixed: FixedShort get() = bottom.toFixedShort()
    val leftFixed: FixedShort get() = left.toFixedShort()

    val leftPlusRightFixed: FixedShort get() = leftFixed + rightFixed
    val topPlusBottomFixed: FixedShort get() = topFixed + bottomFixed
    val horizontalFixed: FixedShort get() = (leftFixed + rightFixed) / 2.toFixedShort()
    val verticalFixed: FixedShort get() = (topFixed + bottomFixed) / 2.toFixedShort()

    val leftPlusRight: Float get() = left + right
    val topPlusBottom: Float get() = top + bottom

    val horizontal: Float get() = (left + right) / 2
    val vertical: Float get() = (top + bottom) / 2

    companion object {
        val ZERO = Margin(0f, 0f, 0f, 0f)
    }

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
