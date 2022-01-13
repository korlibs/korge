package com.soywiz.klock

import com.soywiz.klock.internal.Serializable
import com.soywiz.klock.internal.klockLazyOrGet

/**
 * Represents a right-opened range between two dates.
 */
data class DateTimeRange(val from: DateTime, val to: DateTime) : Comparable<DateTime>, Serializable {
    val valid get() = from <= to

	companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

		operator fun invoke(base: Date, from: Time, to: Time): DateTimeRange = DateTimeRange(base + from, base + to)
	}

	val size: TimeSpan get() = to - from

    val min get() = from
    val max get() = to
    /**
     * Duration [TimeSpan] without having into account actual months/years.
     */
    val duration: TimeSpan get() = to - from

    /**
     * [DateTimeSpan] distance between two dates, month and year aware.
     */
    val span: DateTimeSpan by klockLazyOrGet {
        val reverse = to < from
        val rfrom = if (!reverse) from else to
        val rto = if (!reverse) to else from

        var years = 0
        var months = 0

        var pivot = rfrom

        // Compute years
        val diffYears = (rto.year - pivot.year)
        pivot += diffYears.years
        years += diffYears
        if (pivot > rto) {
            pivot -= 1.years
            years--
        }

        // Compute months (at most an iteration of 12)
        while (true) {
            val t = pivot + 1.months
            if (t <= rto) {
                months++
                pivot = t
            } else {
                break
            }
        }

		val out = DateTimeSpan(years.years + months.months, rto - pivot)
        if (reverse) -out else out
    }

    /**
     * Checks if a date is contained in this range.
     */
    operator fun contains(date: DateTime): Boolean {
        val unix = date.unixMillisDouble
        val from = from.unixMillisDouble
        val to = to.unixMillisDouble
		return if (unix < from) false else unix < to
    }

	operator fun contains(other: DateTimeRange): Boolean {
		return other.min >= this.min && other.max <= this.max
	}

    private inline fun <T> _intersectionWith(that: DateTimeRange, rightOpen: Boolean, handler: (from: DateTime, to: DateTime, matches: Boolean) -> T): T {
        val from = max(this.from, that.from)
        val to = min(this.to, that.to)
        return handler(from, to, if (rightOpen) from < to else from <= to)
    }

    /**
     * Returns new [DateTimeRange] or null - the result of intersection of this and [that] DateTimeRanges.
     */
    fun intersectionWith(that: DateTimeRange, rightOpen: Boolean = true): DateTimeRange? {
        return _intersectionWith(that, rightOpen) { from, to, matches ->
            when {
                matches -> DateTimeRange(from, to)
                else -> null
            }
        }
    }

    /**
     * Returns true if this and [that] DateTimeRanges have intersection otherwise false.
     */
	fun intersectsWith(that: DateTimeRange, rightOpen: Boolean = true): Boolean = _intersectionWith(that, rightOpen) { _, _, matches -> matches }

    /**
     * Returns true if this and [that] DateTimeRanges have intersection or at least a common end otherwise false.
     */
    fun intersectsOrInContactWith(that: DateTimeRange): Boolean = intersectsWith(that, rightOpen = false)

    /**
     * Returns new [DateTimeRange] or null - the result of merging this and [that] DateTimeRanges if they have intersection.
     */
    fun mergeOnContactOrNull(that: DateTimeRange): DateTimeRange? {
        if (!intersectsOrInContactWith(that)) return null
        val min = min(this.min, that.min)
        val max = max(this.max, that.max)
        return DateTimeRange(min, max)
    }

    /**
     * Returns a [List] of 0, 1 or 2 [DateTimeRange]s - the result of removing [that] DateTimeRange from this one
     */
    fun without(that: DateTimeRange): List<DateTimeRange> = when {
        // Full remove
        (that.min <= this.min) && (that.max >= this.max) -> listOf()
        // To the right or left, nothing to remove
		(that.min >= this.max) || (that.max <= this.min) -> listOf(this)
        // In the middle
        else -> {
            val p0 = this.min
            val p1 = that.min
            val p2 = that.max
            val p3 = this.max
            val c1 = if (p0 < p1) DateTimeRange(p0, p1) else null
            val c2 = if (p2 < p3) DateTimeRange(p2, p3) else null
            listOfNotNull(c1, c2)
        }
    }

    fun toString(format: DateFormat): String = "${min.toString(format)}..${max.toString(format)}"
    fun toStringLongs(): String = "${min.unixMillisLong}..${max.unixMillisLong}"
    fun toStringDefault(): String = toString(DateFormat.FORMAT1)
    //override fun toString(): String = toString(DateFormat.FORMAT1)
    override fun toString(): String = "$min..$max"

    override fun compareTo(other: DateTime): Int {
        if (this.max <= other) return -1
        if (this.min > other) return +1
        return 0
    }
}

fun List<DateTimeRange>.toStringLongs() = this.map { it.toStringLongs() }.toString()

/**
 * Generates a right-opened range between two [DateTime]s
 */
infix fun DateTime.until(other: DateTime) = DateTimeRange(this, other)
