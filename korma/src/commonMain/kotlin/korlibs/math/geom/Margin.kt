package korlibs.math.geom

import korlibs.memory.*
import korlibs.memory.pack.*
import korlibs.math.internal.*

// @TODO: value class
/**
 * A [top], [right], [bottom], [left] pack with FixedShort (16-bit) in the range of +-3275.9 (3.3 integer digits + 1 decimal digit)
 */
inline class Margin internal constructor(val raw: Short4Pack) {
    constructor(top: Float, right: Float, bottom: Float, left: Float) : this(
        short4PackOf(
            top.toFixedShort().raw, right.toFixedShort().raw,
            bottom.toFixedShort().raw, left.toFixedShort().raw
        )
    )
    constructor(vertical: Float, horizontal: Float) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Float) : this(margin, margin, margin, margin)

    operator fun plus(other: Margin): Margin = Margin(top + other.top, right + other.right, bottom + other.bottom, left + other.left)
    operator fun minus(other: Margin): Margin = Margin(top - other.top, right - other.right, bottom - other.bottom, left - other.left)

    val isNotZero: Boolean get() = top != 0f || left != 0f || right != 0f || bottom != 0f

    val topFixed: FixedShort get() = FixedShort.fromRaw(raw.s0)
    val rightFixed: FixedShort get() = FixedShort.fromRaw(raw.s1)
    val bottomFixed: FixedShort get() = FixedShort.fromRaw(raw.s2)
    val leftFixed: FixedShort get() = FixedShort.fromRaw(raw.s3)

    val leftPlusRightFixed: FixedShort get() = leftFixed + rightFixed
    val topPlusBottomFixed: FixedShort get() = topFixed + bottomFixed
    val horizontalFixed: FixedShort get() = (leftFixed + rightFixed) / 2.toFixedShort()
    val verticalFixed: FixedShort get() = (topFixed + bottomFixed) / 2.toFixedShort()

    val top: Float get() = topFixed.toFloat()
    val right: Float get() = rightFixed.toFloat()
    val bottom: Float get() = bottomFixed.toFloat()
    val left: Float get() = leftFixed.toFloat()

    val leftPlusRight: Float get() = left + right
    val topPlusBottom: Float get() = top + bottom

    val horizontal: Float get() = (left + right) / 2
    val vertical: Float get() = (top + bottom) / 2

    companion object {
        val ZERO = Margin(0f, 0f, 0f, 0f)
    }

    override fun toString(): String = "Margin(top=${top.niceStr}, right=${right.niceStr}, bottom=${bottom.niceStr}, left=${left.niceStr})"
}

// @TODO: Value Class when MFVC is available
/**
 * A [top], [right], [bottom], [left] pack with Short (16-bit) precision (+-32767)
 */
inline class MarginInt internal constructor(val raw: Short4Pack) {
    constructor(top: Short, right: Short, bottom: Short, left: Short) : this(short4PackOf(top, right, bottom, left))
    constructor(top: Int, right: Int, bottom: Int, left: Int) : this(short4PackOf(top.toShortClamped(), right.toShortClamped(), bottom.toShortClamped(), left.toShortClamped()))
    constructor(vertical: Int, horizontal: Int) : this(vertical, horizontal, vertical, horizontal)
    constructor(margin: Int) : this(margin, margin, margin, margin)

    operator fun plus(other: MarginInt): MarginInt = MarginInt(top + other.top, right + other.right, bottom + other.bottom, left + other.left)
    operator fun minus(other: MarginInt): MarginInt = MarginInt(top - other.top, right - other.right, bottom - other.bottom, left - other.left)

    val isNotZero: Boolean get() = top != 0 || left != 0 || right != 0 || bottom != 0

    val top: Int get() = raw.s0.toInt()
    val right: Int get() = raw.s1.toInt()
    val bottom: Int get() = raw.s2.toInt()
    val left: Int get() = raw.s3.toInt()

    val leftPlusRight: Int get() = left + right
    val topPlusBottom: Int get() = top + bottom
    val horizontal: Int get() = (left + right) / 2
    val vertical: Int get() = (top + bottom) / 2

    companion object {
        val ZERO = MarginInt(short4PackOf(0, 0, 0, 0))
    }

    override fun toString(): String = "MarginInt(top=${top}, right=${right}, bottom=${bottom}, left=${left})"
}
