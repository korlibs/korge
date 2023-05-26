package korlibs.bignumber.ranges

import korlibs.bignumber.BigInt
import korlibs.bignumber.internal.getProgressionLastElement

/**
 * Represents an inclusive range between two [BigInt] between [start]..[endInclusive].
 *
 * @see kotlin.ranges.IntRange
 */
class BigIntRange(
    start: BigInt,
    endInclusive: BigInt
) : BigIntProgression(start, endInclusive, BigInt.ONE), ClosedRange<BigInt> {
    override val start: BigInt get() = first
    override val endInclusive: BigInt get() = last

    @Suppress("ConvertTwoComparisonsToRangeCheck")
    override fun contains(value: BigInt): Boolean = first <= value && value <= last

    /**
     * Checks whether the range is empty.
     *
     * The range is empty if its start value is greater than the end value.
     */
    override fun isEmpty(): Boolean = first > last

    override fun equals(other: Any?): Boolean =
        other is BigIntRange && (isEmpty() && other.isEmpty() ||
            first == other.first && last == other.last)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * first.toInt() + last.toInt())

    override fun toString(): String = "$first..$last"

    companion object {
        /** An empty range of values of type BigInt. */
        val EMPTY: IntRange = IntRange(1, 0)
    }
}

/**
 * Represents an inclusive progression between two [BigInt] in the range [start]..[endInclusive] with a specific [step]
 *
 * @see kotlin.ranges.IntProgression
 */
open class BigIntProgression internal constructor(
    start: BigInt,
    endInclusive: BigInt,
    val step: BigInt
) : Iterable<BigInt> {
    init {
        if (step == BigInt.ZERO) throw IllegalArgumentException("Step must be non-zero.")
    }

    val first: BigInt = start
    val last: BigInt = getProgressionLastElement(start, endInclusive, step)

    open fun isEmpty(): Boolean = if (step > BigInt.ZERO) first > last else first < last

    override fun iterator(): Iterator<BigInt> = BigIntProgressionIterator(first, last, step)

    override fun equals(other: Any?): Boolean =
        other is BigIntProgression && (isEmpty() && other.isEmpty() ||
            first == other.first && last == other.last && step == other.step)

    override fun hashCode(): Int =
        if (isEmpty()) -1 else (31 * (31 * first.toInt() + last.toInt()) + step.toInt())

    override fun toString(): String =
        if (step > BigInt.ZERO) "$first..$last step $step" else "$first downTo $last step ${-step}"

    /**
     * @see IntProgression.step
     */
    infix fun step(step: BigInt): BigIntProgression {
        return fromClosedRange(first, last, if (this.step > BigInt.ZERO) step else -step)
    }

    companion object {
        fun fromClosedRange(rangeStart: BigInt, rangeEnd: BigInt, step: BigInt): BigIntProgression =
            BigIntProgression(rangeStart, rangeEnd, step)
    }
}


/**
 * @see kotlin.ranges.IntProgressionIterator
 */
class BigIntProgressionIterator(first: BigInt, last: BigInt, val step: BigInt) : Iterator<BigInt> {
    private val finalElement: BigInt = last
    private var hasNext: Boolean = if (step > BigInt.ZERO) first <= last else first >= last
    private var next: BigInt = if (hasNext) first else finalElement

    override fun hasNext(): Boolean = hasNext

    override fun next(): BigInt {
        val value = next
        if (value == finalElement) {
            if (!hasNext) throw NoSuchElementException()
            hasNext = false
        }
        else {
            next += step
        }
        return value
    }
}
