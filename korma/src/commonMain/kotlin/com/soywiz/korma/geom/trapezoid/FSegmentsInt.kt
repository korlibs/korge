package com.soywiz.korma.geom.trapezoid

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*

class FSegmentsInt(capacity: Int = 5) {
    companion object {
        operator fun invoke(block: FSegmentsInt.() -> Unit): FSegmentsInt = FSegmentsInt().also(block)
    }
    inline operator fun <R> invoke(block: FSegmentsInt.() -> R): R = run(block)
    private val data = IntArrayList(capacity * 4)
    val size: Int get() = data.size / 4

    fun clone(): FSegmentsInt = FSegmentsInt(size).also { out ->
        fastForEach { out.add(it.x0, it.y0, it.x1, it.y1) }
    }

    fun sortedBy(gen: FSegmentsInt.(Item) -> Int): FSegmentsInt = clone().also { it.sortBy(gen) }

    fun sortBy(gen: FSegmentsInt.(Item) -> Int) {
        genericSort(this, 0, size - 1, SortOps(gen))
    }

    fun swap(a: Item, b: Item) {
        val ax0 = a.x0
        val ay0 = a.y0
        val ax1 = a.x1
        val ay1 = a.y1
        a.setTo(b.x0, b.y0, b.x1, b.y1)
        b.setTo(ax0, ay0, ax1, ay1)
    }

    class SortOps(val gen: FSegmentsInt.(Item) -> Int) : com.soywiz.kds.SortOps<FSegmentsInt>() {
        override fun compare(subject: FSegmentsInt, l: Int, r: Int): Int = subject.gen(Item(l)) compareTo subject.gen(Item(r))
        override fun swap(subject: FSegmentsInt, indexL: Int, indexR: Int) = subject.swap(Item(indexL), Item(indexR))
    }

    operator fun get(index: Int): Item = Item(index)
    fun getOrNull(index: Int): Item? = if (index in 0 until size) get(index) else null
    inline fun fastForEach(block: FSegmentsInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    inline fun <T> map(block: FSegmentsInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }
    inline fun filter(block: FSegmentsInt.(Item) -> Boolean): FSegmentsInt = FSegmentsInt().also {  out ->
        this@FSegmentsInt.fastForEach { if (this@FSegmentsInt.block(it)) out.add(it, this) }
    }
    fun toSegmentIntList(): List<SegmentInt> = map { it.toSegmentInt() }

    inline class Item(val index: Int) {
        inline fun <T> use(segments: FSegmentsInt, block: FSegmentsInt.(Item) -> T): T = block(segments, this)
    }

    var Item.x0: Int; get() = data[index * 4 + 0]; set(value) { data[index * 4 + 0] = value }
    var Item.y0: Int; get() = data[index * 4 + 1]; set(value) { data[index * 4 + 1] = value }
    var Item.x1: Int; get() = data[index * 4 + 2]; set(value) { data[index * 4 + 2] = value }
    var Item.y1: Int; get() = data[index * 4 + 3]; set(value) { data[index * 4 + 3] = value }

    fun Item.setTo(x0: Int, y0: Int, x1: Int, y1: Int) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
    }

    val Item.dx: Int get() = x1 - x0 // run
    val Item.dy: Int get() = y1 - y0 // rise
    val Item.slope: Double get() = dy.toDouble() / dx.toDouble()
    val Item.islope: Double get() = dx.toDouble() / dy.toDouble()
    val Item.xMin: Int get() = minOf(x0, x1)
    val Item.yMin: Int get() = minOf(y0, y1)
    val Item.xMax: Int get() = maxOf(x0, x1)
    val Item.yMax: Int get() = maxOf(y0, y1)
    fun Item.x(y: Int): Int = x0 + ((y - y0) * islope).toIntRound()
    fun Item.y(x: Int): Int = y0 + (slope * (x - x0)).toIntRound()
    fun Item.containsX(x: Int): Boolean = x in xMin..xMax
    fun Item.containsY(y: Int): Boolean = y in yMin..yMax
    fun Item.getIntersectY(other: Item): Int = SegmentInt.getIntersectY(x0, y0, x1, y1, other.x0, other.y0, other.x1, other.y1)

    fun Item.toStringDefault(): String = "Segment[$index](($x0, $y0), ($x1, $y1))"
    fun Item.toSegmentInt(out: SegmentInt = SegmentInt()): SegmentInt = out.also { it.setTo(x0, y0, x1, y1) }

    fun add(x0: Int, y0: Int, x1: Int, y1: Int): Item {
        val index = size
        data.add(x0, y0, x1, y1)
        return Item(index)
    }
    fun add(v: Item): Item = add(v.x0, v.y0, v.x1, v.y1)
    fun add(v: Item, segments: FSegmentsInt): Item = segments.run { this@FSegmentsInt.add(v.x0, v.y0, v.x1, v.y1) }
    fun add(v: SegmentInt): Item = add(v.x0, v.y0, v.x1, v.y1)
}

fun List<SegmentInt>.toFSegmentsInt(): FSegmentsInt = FSegmentsInt { this@toFSegmentsInt.fastForEach { add(it) } }

fun FSegmentsInt.getAllYSorted(): IntArray {
    val set = IntArray(size * 2)
    for (n in 0 until size) {
        val segment = this[n]
        set[n * 2 + 0] = segment.y0
        set[n * 2 + 1] = segment.y1
    }
    return set.distinct().toIntArray().sortedArray()

    //val set = IntSet()
    //for (n in 0 until size) {
    //    val segment = this[n]
    //    set.add(segment.y0)
    //    set.add(segment.y1)
    //}
    //return set.toIntArray().sortedArray()
}
