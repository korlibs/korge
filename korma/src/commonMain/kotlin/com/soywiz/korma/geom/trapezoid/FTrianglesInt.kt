package com.soywiz.korma.geom.trapezoid

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

class FTrianglesInt {
    companion object {
        operator fun invoke(block: FTrianglesInt.() -> Unit): FTrianglesInt = FTrianglesInt().also(block)
    }
    val coords = intArrayListOf()
    val indices = intArrayListOf()
    val size: Int get() = indices.size / 3

    operator fun get(index: Int): Item = Item(index)
    inline fun fastForEach(block: FTrianglesInt.(Item) -> Unit) { for (n in 0 until size) this.block(this[n]) }
    fun toTriangleIntList(): List<TriangleInt> = map { it.toTriangleInt() }
    inline fun <T> map(block: FTrianglesInt.(Item) -> T): List<T> = fastArrayListOf<T>().also { out -> fastForEach { out.add(block(it)) } }

    private fun Item.index(i: Int, c: Int): Int = indices[index * 3 + i] * 2 + c
    private fun Item.coord(i: Int, c: Int): Int = coords[index(i, c)]
    private fun Item.coordX(i: Int): Int = coord(i, 0)
    private fun Item.coordY(i: Int): Int = coord(i, 1)

    private fun Item.setCoord(i: Int, c: Int, value: Int) { coords[index(i, c)] = value }
    private fun Item.setCoordX(i: Int, value: Int) { setCoord(i, 0, value) }
    private fun Item.setCoordY(i: Int, value: Int) { setCoord(i, 1, value) }

    var Item.x0: Int get() = coordX(0); set(value) { setCoordX(0, value) }
    var Item.y0: Int get() = coordY(0); set(value) { setCoordY(0, value) }
    var Item.x1: Int get() = coordX(1); set(value) { setCoordX(1, value) }
    var Item.y1: Int get() = coordY(1); set(value) { setCoordY(1, value) }
    var Item.x2: Int get() = coordX(2); set(value) { setCoordX(2, value) }
    var Item.y2: Int get() = coordY(2); set(value) { setCoordY(2, value) }
    fun Item.toTriangleInt(out: TriangleInt = TriangleInt()): TriangleInt {
        out.setTo(x0, y0, x1, y1, x2, y2)
        return out
    }

    fun add(x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int): Item {
        val triangleIndex = size
        val coordIndex = coords.size / 2
        coords.add(x0, y0, x1, y1, x2, y2)
        indices.add(coordIndex, coordIndex + 1, coordIndex + 2)
        return Item(triangleIndex)
    }

    fun add(v: Item): Item = add(v.x0, v.y0, v.x1, v.y1, v.x2, v.y2)
    fun add(v: TriangleInt): Item = add(v.x0, v.y0, v.x1, v.y1, v.x2, v.y2)

    //fun addStrip(x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int, x3: Int, y3: Int): Item {
    //}

    inline class Item(val index: Int) {
        inline fun <T> use(triangles: FTrianglesInt, block: FTrianglesInt.(Item) -> T): T = block(triangles, this)
    }
}

fun List<TriangleInt>.toFTrianglesInt(): FTrianglesInt = FTrianglesInt { this@toFTrianglesInt.fastForEach { add(it) } }
