package com.soywiz.korma.geom.trapezoid

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.vector.*
import kotlin.jvm.*
import kotlin.math.*

/**
 *   (x0a, y0)  (x0b, y0)
 *   +----------+
 *  /            \
 * +--------------+
 * (x1a, y1)     (x1b, y1)
 */
class FTrapezoidsInt(capacity: Int = 5) {
    var assumeSorted: Boolean = false
    private val data = IntArrayList(capacity * 7)
    val size: Int get() = data.size / 6

    fun clear() { data.clear() }
    operator fun get(index: Int): Item = Item(index)
    inline fun fastForEach(block: FTrapezoidsInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    inline fun <T> map(block: FTrapezoidsInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }
    fun toTrapezoidIntList(): List<TrapezoidInt> = map { it.toTrapezoidInt() }

    override fun toString(): String = "FTrapezoidsInt[$size]"

    fun containsPoint(x: Int, y: Int, assumeSorted: Boolean = this.assumeSorted): Boolean = pointInside(x, y, assumeSorted)

    data class PointInsideStats(
        var found: Boolean = false,
        var iterations: Int = 0,
        var iterations2: Int = 0,
        var total: Int = 0,
    )

    // @TODO: Optimize for [assumeSorted] = true
    inline fun pointInside(
        x: Int,
        y: Int,
        assumeSorted: Boolean = this.assumeSorted,
        out: FTrapezoidsInt.(Item) -> Unit = { },
        stats: PointInsideStats? = null
    ): Boolean {
        //println("x=$x, y=$y")
        //println(this.map { it.toStringDefault() })
        var iterations = 0
        var iterations2 = 0
        var found = false
        if (assumeSorted) {
            val size = this.size
            val result = genericBinarySearchResult(0, size - 1, check = {
                iterations2++
                Item(it).y0.compareTo(y)
            })
            var index = result.nearIndex
            //println("xy=($x,$y), nearIndex=$index")

            while (index in 0 until size) {
                val item = Item(index)
                if (y > item.y1) break
                iterations++
                index--
            }
            index++
            for (n in index until size) {
                iterations++
                val item = Item(n)
                val inside = item.inside(x, y)
                //println("xy=($x,$y), inside=$inside, index=$n : ${item.toStringDefault()}")
                if (inside) {
                    found = true
                    break
                }
                if (item.y0 > y) break
            }
        } else {
            for (n in 0 until size) {
                iterations++
                val it = this[n]
                if (it.inside(x, y)) {
                    found = true
                    this.out(it)
                    break
                }
            }
        }
        if (stats != null) {
            stats.iterations = iterations
            stats.iterations2 = iterations2
            stats.total = size
            stats.found = found
        }
        //println("found=$found, iterations2=$iterations2, iterations=$iterations, total=$size")
        return found
    }

    companion object {
        operator fun invoke(capacity: Int = 5, block: FTrapezoidsInt.() -> Unit): FTrapezoidsInt =
            FTrapezoidsInt(capacity).apply(block)
    }

    fun Item.toStringDefault(): String = "Trapezoid[$index](($x0a, $x0b, $y0), ($x1a, $x1b, $y1))"

    /** Left coordinate of top part */
    var Item.x0a: Int; get() = data[index * 6 + 0]; set(value) { data[index * 6 + 0] = value }
    /** Right coordinate of top part */
    var Item.x0b: Int; get() = data[index * 6 + 1]; set(value) { data[index * 6 + 1] = value }
    /** Top coordinate */
    var Item.y0: Int; get() = data[index * 6 + 2]; set(value) { data[index * 6 + 2] = value }

    /** Left coordinate of bottom part */
    var Item.x1a: Int; get() = data[index * 6 + 3]; set(value) { data[index * 6 + 3] = value }
    /** Right coordinate of bottom part */
    var Item.x1b: Int; get() = data[index * 6 + 4]; set(value) { data[index * 6 + 4] = value }
    /** Bottom coordinate */
    var Item.y1: Int; get() = data[index * 6 + 5]; set(value) { data[index * 6 + 5] = value }

    fun Item.containsY(y: Int): Boolean = y in y0..y1

    fun Item.toTrapezoidInt(): TrapezoidInt = TrapezoidInt(x0a, x0b, y0, x1a, x1b, y1)

    fun Item.inside(x: Int, y: Int): Boolean = TrapezoidInt.inside(x0a, x0b, y0, x1a, x1b, y1, x, y)
    fun Item.triangulate(out: FTrianglesInt = FTrianglesInt()): FTrianglesInt = TrapezoidInt.triangulate(x0a, x0b, y0, x1a, x1b, y1, out)

    fun add(x0a: Int, x0b: Int, y0: Int, x1a: Int, x1b: Int, y1: Int): Item = Item(size).also {
        data.add(x0a, x0b, y0, x1a, x1b, y1)
    }
    fun add(v: Item): Item = add(v.x0a, v.x0b, v.y0, v.x1a, v.x1b, v.y1)
    fun add(v: TrapezoidInt): Item = add(v.x0a, v.x0b, v.y0, v.x1a, v.x1b, v.y1)

    @JvmInline
    value class Item(val index: Int) {
        inline fun <T> use(parallelograms: FTrapezoidsInt, block: FTrapezoidsInt.(Item) -> T): T = block(parallelograms, this)
    }
}

fun List<TrapezoidInt>.toFTrapezoidsInt(): FTrapezoidsInt = FTrapezoidsInt(this.size) {
    this@toFTrapezoidsInt.fastForEach { add(it) }
}
