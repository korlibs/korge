@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.segment

import korlibs.datastructure.BSearchResult
import korlibs.datastructure.IntArrayList
import korlibs.datastructure.genericBinarySearch
import korlibs.math.annotations.*
import kotlin.math.max
import kotlin.math.min

/**
 * Non-overlapping SegmentSet
 */
@KormaExperimental
class IntSegmentSet {
    @PublishedApi
    internal val min = IntArrayList(16)
    @PublishedApi
    internal val max = IntArrayList(16)
    val size get() = min.size
    fun isEmpty() = size == 0
    fun isNotEmpty() = size > 0

    fun clear() = this.apply {
        min.clear()
        max.clear()
    }

    fun copyFrom(other: IntSegmentSet) = this.apply {
        this.clear()
        addUnsafe(other)
    }

    fun clone() = IntSegmentSet().copyFrom(this)

    val minMin get() = if (isNotEmpty()) min.getAt(0) else 0
    val maxMax get() = if (isNotEmpty()) max.getAt(max.size - 1) else 0

    fun findNearIndex(x: Int): BSearchResult = BSearchResult(genericBinarySearch(0, size) { v ->
        val min = this.min.getAt(v)
        val max = this.max.getAt(v)
        when {
            x < min -> +1
            x > max -> -1
            else -> 0
        }
    })

    inline fun fastForEach(block: (n: Int, min: Int, max: Int) -> Unit) {
        for (n in 0 until size) block(n, min.getAt(n), max.getAt(n))
    }

    fun findLeftBound(x: Int): Int {
        //if (size < 8) return 0 // Do not invest time on binary search on small sets
        return (genericBinarySearchLeft(0, size) { this.min.getAt(it).compareTo(x) }).coerceIn(0, size - 1)
    }
    fun findRightBound(x: Int): Int {
        //if (size < 8) return size - 1 // Do not invest time on binary search on small sets
        return (genericBinarySearchRight(0, size) { this.max.getAt(it).compareTo(x) }).coerceIn(0, size - 1)
    }

    inline fun fastForEachInterestingRange(min: Int, max: Int, block: (n: Int, x1: Int, x2: Int) -> Unit) {
        if (isEmpty()) return
        val nmin = findLeftBound(min)
        val nmax = findRightBound(max)
        for (n in nmin..nmax) block(n, this.min.getAt(n), this.max.getAt(n))
    }

    internal fun addUnsafe(min: Int, max: Int) = this.apply {
        check(min <= max)
        insertAt(size, min, max)
    }

    internal fun addUnsafe(other: IntSegmentSet) = this.apply {
        this.min.add(other.min)
        this.max.add(other.max)
    }

    fun add(other: IntSegmentSet) = this.apply {
        other.fastForEach { n, min, max ->
            add(min, max)
        }
    }

    fun add(min: Int, max: Int) = this.apply {
        check(min <= max)
        when {
            isEmpty() -> insertAt(size, min, max)
            min == maxMax -> this.max[this.max.size - 1] = max
            max == minMin -> this.min[0] = min
            else -> {
                var removeStart = -1
                var removeCount = -1

                fastForEachInterestingRange(min, max) { n, x1, x2 ->
                    if (intersects(x1, x2, min, max)) {
                        if (removeStart == -1) removeStart = n
                        this.min[removeStart] = min(this.min.getAt(removeStart), min(x1, min))
                        this.max[removeStart] = max(this.max.getAt(removeStart), max(x2, max))
                        removeCount++
                    }
                }

                when {
                    // Combined
                    removeCount == 0 -> Unit
                    removeCount > 0 -> removeAt(removeStart + 1, removeCount)
                    // Insert at the beginning
                    max < minMin -> insertAt(0, min, max)
                    // Insert at the end
                    min > maxMax -> insertAt(size, min, max)
                    // Insert at a place
                    else -> {
                        for (m in findLeftBound(min).coerceAtLeast(1)..findRightBound(max)) {
                        //for (m in 1..findRightBound(max)) {
                            val prevMax = this.max.getAt(m - 1)
                            val currMin = this.min.getAt(m)
                            if (min > prevMax && max < currMin) {
                                insertAt(m, min, max)
                                return@apply
                            }
                        }
                        
                        error("Unexpected")
                    }
                }
            }
        }
    }

    private fun insertAt(n: Int, min: Int, max: Int) {
        this.min.insertAt(n, min)
        this.max.insertAt(n, max)
    }

    private fun removeAt(n: Int, count: Int) {
        this.min.removeAt(n, count)
        this.max.removeAt(n, count)
    }

    //fun remove(min: Int, max: Int) = this.apply { TODO() }
    //fun intersect(min: Int, max: Int) = this.apply { TODO() }

    inline fun intersection(min: Int, max: Int, out: (min: Int, max: Int) -> Unit): Boolean {
        var count = 0
        fastForEachInterestingRange(min, max) { n, x1, x2 ->
            if (intersects(x1, x2, min, max)) {
                out(max(x1, min), min(x2, max))
                count++
            }
        }

        return count > 0
    }

    // Use for testing
    // O(n * log(n))
    internal fun intersectionSlow(min: Int, max: Int): Pair<Int, Int>? {
        var out: Pair<Int, Int>? = null
        intersectionSlow(min, max) { x1, x2 -> out = x1 to x2 }
        return out
    }

    // Use for testing
    // O(n^2)
    internal inline fun intersectionSlow(min: Int, max: Int, out: (min: Int, max: Int) -> Unit): Boolean {
        var count = 0
        fastForEach { n, x1, x2 ->
            if (intersects(x1, x2, min, max)) {
                out(max(x1, min), min(x2, max))
                count++
            }
        }
        return count > 0
    }

    operator fun contains(v: Int): Boolean = findNearIndex(v).found

    fun setToIntersect(a: IntSegmentSet, b: IntSegmentSet) = this.apply {
        val aSmaller = a.size < b.size
        val av = if (aSmaller) a else b
        val bv = if (aSmaller) b else a
        clear().also { av.fastForEach { n, x1, x2 -> bv.intersection(x1, x2) { min, max -> add(min, max) } } }
    }

    // Use for testing
    internal fun setToIntersectSlow(a: IntSegmentSet, b: IntSegmentSet) = this.apply {
        clear().also { a.fastForEach { n, x1, x2 -> b.intersectionSlow(x1, x2) { min, max -> add(min, max) } } }
    }

    @PublishedApi
    internal fun intersects(x1: Int, x2: Int, y1: Int, y2: Int): Boolean = x2 >= y1 && y2 >= x1
    @PublishedApi
    internal fun intersects(x1: Int, x2: Int, index: Int): Boolean = intersects(x1, x2, min.getAt(index), max.getAt(index))

    @PublishedApi
    internal fun contains(v: Int, x1: Int, x2: Int): Boolean = v in x1 until x2
    @PublishedApi
    internal fun contains(v: Int, index: Int): Boolean = contains(v, min.getAt(index), max.getAt(index))

    override fun toString(): String = buildString {
        append("[")
        fastForEach { n, min, max ->
            val first = (n == 0)
            if (!first) append(", ")
            append("$min-$max")
        }
        append("]")
    }
}

// @TODO: In KDS latest versions
@PublishedApi
internal inline fun genericBinarySearchLeft(fromIndex: Int, toIndex: Int, check: (value: Int) -> Int): Int =
    genericBinarySearch(fromIndex, toIndex, invalid = { from, to, low, high -> min(low, high).coerceIn(from, to - 1) }, check = check)
@PublishedApi
internal inline fun genericBinarySearchRight(fromIndex: Int, toIndex: Int, check: (value: Int) -> Int): Int =
    genericBinarySearch(fromIndex, toIndex, invalid = { from, to, low, high -> max(low, high).coerceIn(from, to - 1) }, check = check)
