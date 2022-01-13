package com.soywiz.klock

import com.soywiz.klock.internal.BSearchResult
import com.soywiz.klock.internal.Serializable
import com.soywiz.klock.internal.fastForEach
import com.soywiz.klock.internal.genericBinarySearch
import com.soywiz.klock.internal.klockLazyOrGet

// Properties:
//  - ranges are sorted
//  - ranges do not overlap/intersect between each other (they are merged and normalized)
// These properties allows to do some tricks and optimizations like binary search and a lot of O(n) operations.
data class DateTimeRangeSet private constructor(val dummy: Boolean, val ranges: List<DateTimeRange>) : Serializable {

    /** [DateTimeRange] from the beginning of the first element to the end of the last one. */
    val bounds = DateTimeRange(
        ranges.firstOrNull()?.from ?: DateTime.EPOCH,
        ranges.lastOrNull()?.to ?: DateTime.EPOCH
    )

    /** Total time of all [ranges]. */
	val size: TimeSpan by klockLazyOrGet {
		var out = 0.seconds
		ranges.fastForEach { out += it.size }
		out
	}

	constructor(ranges: List<DateTimeRange>) : this(false, Fast.combine(ranges))
    constructor(range: DateTimeRange) : this(listOf(range))
    constructor(vararg ranges: DateTimeRange) : this(ranges.toList())

    operator fun plus(range: DateTimeRange): DateTimeRangeSet = this + DateTimeRangeSet(range)
    operator fun plus(right: DateTimeRangeSet): DateTimeRangeSet = DateTimeRangeSet(this.ranges + right.ranges)

    operator fun minus(range: DateTimeRange): DateTimeRangeSet = this - DateTimeRangeSet(range)
    operator fun minus(right: DateTimeRangeSet): DateTimeRangeSet = Fast.minus(this, right)

    operator fun contains(time: DateTime): Boolean = Fast.contains(time, this)
	operator fun contains(time: DateTimeRange): Boolean = Fast.contains(time, this)

	fun intersection(range: DateTimeRange): DateTimeRangeSet = this.intersection(DateTimeRangeSet(range))
    fun intersection(vararg range: DateTimeRange): DateTimeRangeSet = this.intersection(DateTimeRangeSet(*range))
    fun intersection(right: DateTimeRangeSet): DateTimeRangeSet = Fast.intersection(this, right)

    companion object {
        @Suppress("MayBeConstant", "unused")
        private const val serialVersionUID = 1L

        fun toStringLongs(ranges: List<DateTimeRange>): String = "${ranges.map { it.toStringLongs() }}"
    }

    object Fast {
        internal fun combine(ranges: List<DateTimeRange>): List<DateTimeRange> {
            if (ranges.isEmpty()) return ranges

            val sorted = ranges.sortedBy { it.from.unixMillis }
            val out = arrayListOf<DateTimeRange>()
            var pivot = sorted.first()
            for (n in 1 until sorted.size) {
                val current = sorted[n]
                val result = pivot.mergeOnContactOrNull(current)
                pivot = if (result != null) {
                    result
                } else {
                    out.add(pivot)
                    current
                }
            }
            return out + listOf(pivot)
        }

        internal fun minus(left: DateTimeRangeSet, right: DateTimeRangeSet): DateTimeRangeSet {
            if (left.ranges.isEmpty() || right.ranges.isEmpty()) return left

            val ll = left.ranges
            val rr = right.ranges.filter { it.intersectsWith(left.bounds) }
            var lpos = 0
            var rpos = 0
            var l = ll.getOrNull(lpos++)
            var r = rr.getOrNull(rpos++)
            val out = arrayListOf<DateTimeRange>()
            //debug { "-----------------" }
            //debug { "Minus:" }
            //debug { "  - ll=${toStringLongs(ll)}" }
            //debug { "  - rr=${toStringLongs(rr)}" }
            while (l != null && r != null) {
                val result = l.without(r)
                //debug { "Minus ${l!!.toStringLongs()} with ${r!!.toStringLongs()} -- ${toStringLongs(result)}" }
                when (result.size) {
                    0 -> {
                        //debug { "  - Full remove" }
                        l = ll.getOrNull(lpos++)
                    }
                    1 -> {
                        //debug { "  - Result 1" }
                        when {
                            r.from >= l.to -> {
                                //debug { "    - Move left. Emit ${result[0].toStringLongs()}" }
                                out.add(result[0])
                                l = ll.getOrNull(lpos++)
                            }
                            l == result[0] -> {
                                //debug { "    - Move right. Change l from ${l!!.toStringLongs()} to ${result[0].toStringLongs()}" }
                                r = rr.getOrNull(rpos++)
                            }
                            else -> {
                                //debug { "    - Use this l=${result[0].toStringLongs()} from ${l!!.toStringLongs()}" }
                                l = result[0]
                            }
                        }
                    }
                    else -> {
                        //debug { "  - One chunk removed: ${result.map { it.toStringLongs() }}" }
                        //debug { "    - Emit: ${result[0].toStringLongs()}" }
                        //debug { "    - Keep: ${result[1].toStringLongs()}" }
                        out.add(result[0])
                        l = result[1]
                    }
                }
            }
            if (l != null) {
                out.add(l)
            }
            while (lpos < ll.size) out.add(ll[lpos++])

            //debug { toStringLongs(out) }
            return DateTimeRangeSet(out)
        }

        fun intersection(left: DateTimeRangeSet, right: DateTimeRangeSet): DateTimeRangeSet {
            if (left.ranges.isEmpty() || right.ranges.isEmpty()) return DateTimeRangeSet(listOf())

            val ll = left.ranges.filter { it.intersectsWith(right.bounds) }
            val rr = right.ranges.filter { it.intersectsWith(left.bounds) }
            val out = arrayListOf<DateTimeRange>()
            //debug { "-----------------" }
            //debug { "Intersection:" }
            //debug { "  - ll=${toStringLongs(ll)}" }
            //debug { "  - rr=${toStringLongs(rr)}" }
            var rpos = 0
            for (l in ll) {
                rpos = 0
                // We should be able to do this because the time ranges doesn't intersect each other
                //while (rpos > 0) {
                //    val r = rr.getOrNull(rpos) ?: break
                //    if ((r.from < l.from) && (r.to < l.from)) break // End since we are already
                //    rpos--
                //}
                while (rpos < rr.size) {
                    val r = rr.getOrNull(rpos) ?: break
                    if (r.min > l.max) break // End since the rest are going to be farther
                    val res = l.intersectionWith(r)
                    if (res != null) {
                        out.add(res)
                    }
                    rpos++
                }
            }

            //debug { toStringLongs(out) }
            return DateTimeRangeSet(out)
        }

		fun contains(time: DateTime, rangeSet: DateTimeRangeSet): Boolean {
			if (time !in rangeSet.bounds) return false // Early guard clause
			val ranges = rangeSet.ranges
			val result = BSearchResult(genericBinarySearch(0, ranges.size) { index -> ranges[index].compareTo(time) })
			return result.found
		}

		fun contains(time: DateTimeRange, rangeSet: DateTimeRangeSet): Boolean {
			if (time !in rangeSet.bounds) return false // Early guard clause
			val ranges = rangeSet.ranges
			val result = BSearchResult(genericBinarySearch(0, ranges.size) { index ->
				val range = ranges[index]
				when {
					time in range -> 0
					time.min < range.min -> +1
					else -> -1
				}
			})
			return result.found
		}
        //private inline fun debug(gen: () -> String) { println(gen()) }
    }

    object Slow {
        // @TODO: Optimize
        internal fun minus(l: DateTimeRangeSet, r: DateTimeRangeSet): DateTimeRangeSet {
            val rightList = r.ranges
            var out = l.ranges.toMutableList()
            restart@ while (true) {
                for ((leftIndex, left) in out.withIndex()) {
                    for (right in rightList) {
                        val result = left.without(right)
                        if (result.size != 1 || result[0] != left) {
                            out = (out.slice(0 until leftIndex) + result + out.slice(leftIndex + 1 until out.size)).toMutableList()
                            continue@restart
                        }
                    }
                }
                break
            }
            return DateTimeRangeSet(out)
        }

        internal fun combine(ranges: List<DateTimeRange>): List<DateTimeRange> {
            // @TODO: Improve performance and verify fast combiner
            val ranges = ranges.toMutableList()
            restart@ while (true) {
                for (i in ranges.indices) {
                    for (j in ranges.indices) {
                        if (i == j) continue
                        val ri = ranges[i]
                        val rj = ranges[j]
                        val concat = ri.mergeOnContactOrNull(rj)
                        if (concat != null) {
                            //println("Combining $ri and $rj : $concat")
                            ranges.remove(rj)
                            ranges[i] = concat
                            continue@restart
                        }
                    }
                }
                break
            }
            return ranges
        }

        fun intersection(left: DateTimeRangeSet, right: DateTimeRangeSet): DateTimeRangeSet {
            val leftList = left.ranges
            val rightList = right.ranges
            val out = arrayListOf<DateTimeRange>()
            for (l in leftList) {
                for (r in rightList) {
                    if (r.min > l.max) break
                    val result = l.intersectionWith(r)
                    if (result != null) {
                        out.add(result)
                    }
                }
                //val chunks = rightList.mapNotNull { r -> l.intersectionWith(r) }
                //out.addAll(DateTimeRangeSet(chunks).ranges)
            }
            return DateTimeRangeSet(out)
        }

		fun contains(time: DateTime, rangeSet: DateTimeRangeSet): Boolean {
			if (time !in rangeSet.bounds) return false // Early guard clause
			// @TODO: Fast binary search, since the ranges doesn't intersect each other
			rangeSet.ranges.fastForEach { range ->
				if (time in range) return true
			}
			return false
		}

		fun contains(time: DateTimeRange, rangeSet: DateTimeRangeSet): Boolean {
			if (time !in rangeSet.bounds) return false // Early guard clause
			// @TODO: Fast binary search, since the ranges doesn't intersect each other
			rangeSet.ranges.fastForEach { range ->
				if (time in range) return true
			}
			return false
		}
    }

    fun toStringLongs(): String = "${ranges.map { it.toStringLongs() }}"
    override fun toString(): String = "$ranges"
}

fun Iterable<DateTimeRange>.toRangeSet() = DateTimeRangeSet(this.toList())
