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

    operator fun get(index: Int): Item = Item(index)
    inline fun fastForEach(block: FSegmentsInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    inline fun <T> map(block: FSegmentsInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }
    fun toSegmentIntList(): List<SegmentInt> = map { it.toSegmentInt() }

    inline class Item(val index: Int)

    var Item.x0: Int; get() = data[index * 4 + 0]; set(value) { data[index * 4 + 0] = value }
    var Item.y0: Int; get() = data[index * 4 + 1]; set(value) { data[index * 4 + 1] = value }
    var Item.x1: Int; get() = data[index * 4 + 2]; set(value) { data[index * 4 + 2] = value }
    var Item.y1: Int; get() = data[index * 4 + 3]; set(value) { data[index * 4 + 3] = value }

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
