package com.soywiz.korma.geom

import com.soywiz.kds.*
import kotlin.math.*

interface IPointArrayList {
    val size: Int
    fun getX(index: Int): Double
    fun getY(index: Int): Double
}

fun IPointArrayList.orientation(): Orientation {
    if (size < 3) return Orientation.COLLINEAR
    return Orientation.orient2dFixed(getX(0), getY(0), getX(1), getY(1), getX(2), getY(2))
}

inline fun IPointArrayList.fastForEach(block: (x: Double, y: Double) -> Unit) {
    for (n in 0 until size) {
        block(getX(n), getY(n))
    }
}

inline fun IPointArrayList.fastForEachReverse(block: (x: Double, y: Double) -> Unit) {
    for (n in 0 until size) {
        val index = size - n - 1
        block(getX(index), getY(index))
    }
}

inline fun IPointArrayList.fastForEachWithIndex(block: (index: Int, x: Double, y: Double) -> Unit) {
    for (n in 0 until size) {
        block(n, getX(n), getY(n))
    }
}

fun IPointArrayList.getPoint(index: Int, out: Point = Point()): Point = out.setTo(getX(index), getY(index))
fun IPointArrayList.getIPoint(index: Int): IPoint = IPoint(getX(index), getY(index))

fun IPointArrayList.toList(): List<Point> = (0 until size).map { getPoint(it) }

fun IPointArrayList.toPoints(): List<Point> = (0 until size).map { getPoint(it) }
fun IPointArrayList.toIPoints(): List<IPoint> = (0 until size).map { getIPoint(it) }

fun <T> IPointArrayList.map(gen: (x: Double, y: Double) -> T): List<T> = (0 until size).map { gen(getX(it), getY(it)) }

fun IPointArrayList.contains(x: Float, y: Float): Boolean = contains(x.toDouble(), y.toDouble())
fun IPointArrayList.contains(x: Int, y: Int): Boolean = contains(x.toDouble(), y.toDouble())
fun IPointArrayList.contains(x: Double, y: Double): Boolean {
    for (n in 0 until size) if (getX(n) == x && getY(n) == y) return true
    return false
}

class PointArrayList(capacity: Int = 7) : IPointArrayList {
    private val xList = DoubleArrayList(capacity)
    private val yList = DoubleArrayList(capacity)
    override val size get() = xList.size

    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    fun clear() = this.apply {
        xList.clear()
        yList.clear()
    }

    companion object {
        operator fun invoke(capacity: Int = 7, callback: PointArrayList.() -> Unit): PointArrayList = PointArrayList(capacity).apply(callback)
        operator fun invoke(points: List<IPoint>): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
        operator fun invoke(vararg points: IPoint): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
    }

    fun add(x: Double, y: Double) = this.apply {
        xList += x
        yList += y
    }
    fun add(x: Float, y: Float) = add(x.toDouble(), y.toDouble())
    fun add(x: Int, y: Int) = add(x.toDouble(), y.toDouble())

    fun add(p: Point) = add(p.x, p.y)
    fun add(p: IPoint) = add(p.x, p.y)
    fun add(p: IPointArrayList) = this.apply { p.fastForEach { x, y -> add(x, y) } }
    fun addReverse(p: IPointArrayList) = this.apply { p.fastForEachReverse { x, y -> add(x, y) } }

    fun copyFrom(other: IPointArrayList): PointArrayList = this.apply { clear() }.apply { add(other) }
    fun clone(out: PointArrayList = PointArrayList()): PointArrayList = out.clear().add(this)

    fun toList(): List<Point> {
        val out = arrayListOf<Point>()
        fastForEach { x, y -> out.add(Point(x, y)) }
        return out
    }

    override fun getX(index: Int) = xList.getAt(index)
    override fun getY(index: Int) = yList.getAt(index)

    fun insertAt(index: Int, p: PointArrayList) = this.apply {
        val size = p.size
        xList.insertAt(index, p.xList.data, 0, size)
        yList.insertAt(index, p.yList.data, 0, size)
    }

    fun insertAt(index: Int, x: Double, y: Double) = this.apply {
        xList.insertAt(index, x)
        yList.insertAt(index, y)
    }

    fun insertAt(index: Int, point: IPoint) = insertAt(index, point.x, point.y)

    fun removeAt(index: Int, count: Int = 1) = this.apply {
        xList.removeAt(index, count)
        yList.removeAt(index, count)
    }

    fun setX(index: Int, x: Double) { xList[index] = x }
    fun setX(index: Int, x: Int) = setX(index, x.toDouble())
    fun setX(index: Int, x: Float) = setX(index, x.toDouble())

    fun setY(index: Int, y: Double) { yList[index] = y }
    fun setY(index: Int, y: Int) = setY(index, y.toDouble())
    fun setY(index: Int, y: Float) = setY(index, y.toDouble())

    fun setXY(index: Int, x: Double, y: Double) {
        xList[index] = x
        yList[index] = y
    }
    fun setXY(index: Int, x: Int, y: Int) = setXY(index, x.toDouble(), y.toDouble())
    fun setXY(index: Int, x: Float, y: Float) = setXY(index, x.toDouble(), y.toDouble())

    fun transform(matrix: Matrix) {
        for (n in 0 until size) {
            val x = getX(n)
            val y = getY(n)
            setX(n, matrix.transformX(x, y))
            setY(n, matrix.transformY(x, y))
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('[')
        for (n in 0 until size) {
            val x = getX(n)
            val y = getY(n)
            if (n != 0) {
                sb.append(", ")
            }
            sb.append('(')
            if (x == round(x)) sb.append(x.toInt()) else sb.append(x)
            sb.append(", ")
            if (y == round(y)) sb.append(y.toInt()) else sb.append(y)
            sb.append(')')
        }
        sb.append(']')
        return sb.toString()
    }

    fun swap(indexA: Int, indexB: Int) {
        xList.swap(indexA, indexB)
        yList.swap(indexA, indexB)
    }

    fun reverse() {
        for (n in 0 until size / 2) swap(0 + n, size - 1 - n)
    }

    fun sort() {
        genericSort(this, 0, this.size - 1, PointSortOpts)
    }

    object PointSortOpts : SortOps<PointArrayList>() {
        override fun compare(p: PointArrayList, l: Int, r: Int): Int = Point.compare(p.getX(l), p.getY(l), p.getX(r), p.getY(r))
        override fun swap(subject: PointArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}

//////////////////////////////////////

interface IPointIntArrayList {
    val size: Int
    fun getX(index: Int): Int
    fun getY(index: Int): Int
}

fun IPointIntArrayList.getPoint(index: Int, out: PointInt = PointInt()): PointInt = out.setTo(getX(index), getY(index))
fun IPointIntArrayList.getIPoint(index: Int): IPointInt = IPointInt(getX(index), getY(index))
fun IPointIntArrayList.toPoints(): List<PointInt> = (0 until size).map { getPoint(it) }
fun IPointIntArrayList.toIPoints(): List<IPointInt> = (0 until size).map { getIPoint(it) }
fun IPointIntArrayList.contains(x: Int, y: Int): Boolean {
    for (n in 0 until size) if (getX(n) == x && getY(n) == y) return true
    return false
}
inline fun IPointIntArrayList.fastForEach(block: (x: Int, y: Int) -> Unit) {
    for (n in 0 until size) {
        block(getX(n), getY(n))
    }
}

class PointIntArrayList(capacity: Int = 7) : IPointIntArrayList {
    private val xList = IntArrayList(capacity)
    private val yList = IntArrayList(capacity)
    override val size get() = xList.size

    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    fun clear() {
        xList.clear()
        yList.clear()
    }

    companion object {
        operator fun invoke(capacity: Int = 7, callback: PointIntArrayList.() -> Unit): PointIntArrayList = PointIntArrayList(capacity).apply(callback)
        operator fun invoke(points: List<IPointInt>): PointIntArrayList = PointIntArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
        operator fun invoke(vararg points: IPointInt): PointIntArrayList = PointIntArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
    }

    fun add(x: Int, y: Int) = this.apply {
        xList += x
        yList += y
    }
    fun add(p: IPointInt) = add(p.x, p.y)
    fun add(p: IPointIntArrayList) = this.apply { p.fastForEach { x, y -> add(x, y) } }

    inline fun fastForEach(block: (x: Int, y: Int) -> Unit) {
        for (n in 0 until size) {
            block(getX(n), getY(n))
        }
    }

    fun toList(): List<PointInt> {
        val out = arrayListOf<PointInt>()
        fastForEach { x, y -> out.add(PointInt(x, y)) }
        return out
    }

    override fun getX(index: Int) = xList.getAt(index)
    override fun getY(index: Int) = yList.getAt(index)

    fun setX(index: Int, x: Int) = run { xList[index] = x }
    fun setY(index: Int, y: Int) = run { yList[index] = y }
    fun setXY(index: Int, x: Int, y: Int) {
        xList[index] = x
        yList[index] = y
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('[')
        for (n in 0 until size) {
            val x = getX(n)
            val y = getY(n)
            if (n != 0) {
                sb.append(", ")
            }
            sb.append('(')
            sb.append(x)
            sb.append(", ")
            sb.append(y)
            sb.append(')')
        }
        sb.append(']')
        return sb.toString()
    }

    fun swap(indexA: Int, indexB: Int) {
        xList.swap(indexA, indexB)
        yList.swap(indexA, indexB)
    }

    fun reverse() {
        for (n in 0 until size / 2) swap(0 + n, size - 1 - n)
    }

    fun sort() {
        genericSort(this, 0, this.size - 1, PointSortOpts)
    }

    object PointSortOpts : SortOps<PointIntArrayList>() {
        override fun compare(p: PointIntArrayList, l: Int, r: Int): Int = PointInt.compare(p.getX(l), p.getY(l), p.getX(r), p.getY(r))
        override fun swap(subject: PointIntArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}
