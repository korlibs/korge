package korlibs.bignumber.ranges

import korlibs.bignumber.*

/**
 * Represents an inclusive range between two [BigNum] between [start]..[endInclusive].
 *
 * @see kotlin.ranges.ClosedFloatRange
 */
class ClosedBigNumRange(
    override val start: BigNum,
    override val endInclusive: BigNum
) : ClosedRange<BigNum> {

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun contains(value: BigNum): Boolean = value >= start && value <= endInclusive

    /**
     * @see kotlin.ranges.ClosedFloatRange.isEmpty
     */
    @Suppress("SimplifyNegatedBinaryExpression")
    override fun isEmpty(): Boolean = !(start <= endInclusive)

    override fun equals(other: Any?): Boolean {
        return other is ClosedBigNumRange && (isEmpty() && other.isEmpty() ||
            start == other.start && endInclusive == other.endInclusive)
    }

    override fun hashCode(): Int {
        return if (isEmpty()) -1 else 31 * start.hashCode() + endInclusive.hashCode()
    }

    override fun toString(): String = "$start..$endInclusive"
}
