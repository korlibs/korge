package com.soywiz.korma.geom

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.Extra
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.SortOps
import com.soywiz.kds.genericSort
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.mapDouble
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.round

interface IPointArrayList : IVectorArrayList, Extra {
    override val dimensions: Int get() = 2
    override fun get(index: Int, dim: Int): Double = if (dim == 0) getX(index) else getY(index)
    fun getX(index: Int): Double
    fun getY(index: Int): Double
    fun get(index: Int, out: MPoint): IPoint = out.setTo(getX(index), getY(index))
}

operator fun IPointArrayList.get(index: Int): Point = Point(getX(index), getY(index))

fun PointArrayList.setToRoundDecimalPlaces(places: Int): PointArrayList {
    fastForEachWithIndex { index, x, y -> this.setXY(index, x.roundDecimalPlaces(places), y.roundDecimalPlaces(places)) }
    return this
}

fun IPointArrayList.roundDecimalPlaces(places: Int, out: PointArrayList = PointArrayList()): IPointArrayList {
    fastForEach { x, y -> out.add(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places)) }
    return out
}

//fun IPointArrayList.getComponent(index: Int, component: Int): Double = if (component == 0) getX(index) else getY(index)

fun IPointArrayList.getComponentList(component: Int, out: DoubleArray = DoubleArray(size)): DoubleArray {
    for (n in 0 until size) out[n] = get(n, component)
    return out
}

val IPointArrayList.firstX: Double get() = getX(0)
val IPointArrayList.firstY: Double get() = getY(0)
val IPointArrayList.lastX: Double get() = getX(size - 1)
val IPointArrayList.lastY: Double get() = getY(size - 1)
fun IPointArrayList.firstPoint(out: MPoint = MPoint()): MPoint = out.setTo(firstX, firstY)
fun IPointArrayList.lastPoint(out: MPoint = MPoint()): MPoint = out.setTo(lastX, lastY)

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

fun IPointArrayList.getPoint(index: Int, out: MPoint = MPoint()): MPoint = out.setTo(getX(index), getY(index))
fun IPointArrayList.getIPoint(index: Int): IPoint = IPoint(getX(index), getY(index))

fun IPointArrayList.toList(): List<MPoint> = (0 until size).map { getPoint(it) }

fun IPointArrayList.toPoints(): List<MPoint> = (0 until size).map { getPoint(it) }
fun IPointArrayList.toIPoints(): List<IPoint> = (0 until size).map { getIPoint(it) }

fun <T> IPointArrayList.map(gen: (x: Double, y: Double) -> T): List<T> = (0 until size).map { gen(getX(it), getY(it)) }

fun IPointArrayList.mapPoints(temp: MPoint = MPoint(), gen: (x: Double, y: Double, out: MPoint) -> IPoint): IPointArrayList {
    val out = PointArrayList(this.size)
    fastForEach { x, y -> out.add(gen(x, y, temp)) }
    return out
}

fun IPointArrayList.contains(x: Float, y: Float): Boolean = contains(x.toDouble(), y.toDouble())
fun IPointArrayList.contains(x: Int, y: Int): Boolean = contains(x.toDouble(), y.toDouble())
fun IPointArrayList.contains(x: Double, y: Double): Boolean {
    for (n in 0 until size) if (getX(n) == x && getY(n) == y) return true
    return false
}

fun IPointArrayList.clone(out: PointArrayList = PointArrayList(this.size)): PointArrayList {
    fastForEach { x, y -> out.add(x, y) }
    return out
}

open class PointArrayList(capacity: Int = 7) : IPointArrayList, Extra by Extra.Mixin() {
    override var closed: Boolean = false
    private val data = DoubleArrayList(capacity * 2)
    override val size get() = data.size / 2

    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    fun clear(): PointArrayList {
        data.clear()
        return this
    }

    companion object {
        operator fun invoke(vararg values: Double): PointArrayList = fromGen(values.size) { values[it] }
        operator fun invoke(vararg values: Float): PointArrayList = fromGen(values.size) { values[it].toDouble() }
        operator fun invoke(vararg values: Int): PointArrayList = fromGen(values.size) { values[it].toDouble() }
        inline fun fromGen(count: Int, gen: (Int) -> Double): PointArrayList {
            val size = count / 2
            val out = PointArrayList(size)
            for (n in 0 until size) out.add(gen(n * 2 + 0), gen(n * 2 + 1))
            return out
        }

        operator fun invoke(capacity: Int = 7, callback: PointArrayList.() -> Unit): PointArrayList = PointArrayList(capacity).apply(callback)
        operator fun invoke(points: List<IPoint>): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
        operator fun invoke(vararg points: IPoint): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
    }

    /**
     * Adds points with [values] in the format of interleaved (x, y) values.
     */
    fun addRaw(vararg values: Double) {
        check(values.size % 2 == 0) { "values not multiple of 2 (x, y) but '${values.size}'" }
        data.add(values)
    }

    fun add(x: Double, y: Double): PointArrayList {
        data.add(x, y)
        return this
    }
    fun add(x: Float, y: Float) = add(x.toDouble(), y.toDouble())
    fun add(x: Int, y: Int) = add(x.toDouble(), y.toDouble())

    fun add(p: Point) = add(p.x, p.y)
    fun add(p: MPoint) = add(p.x, p.y)
    fun add(p: IPoint) = add(p.x, p.y)
    fun add(p: IPointArrayList) = this.apply { p.fastForEach { x, y -> add(x, y) } }
    fun addReverse(p: IPointArrayList) = this.apply { p.fastForEachReverse { x, y -> add(x, y) } }
    fun add(p: IPointArrayList, index: Int) {
        add(p.getX(index), p.getY(index))
    }
    fun add(p: IPointArrayList, index: Int, indexEnd: Int) {
        // @TODO: Optimize this
        for (n in index until indexEnd) add(p.getX(n), p.getY(n))
    }

    fun copyFrom(other: IPointArrayList): PointArrayList {
        clear()
        add(other)
        return this
    }
    fun clone(out: PointArrayList = PointArrayList()): PointArrayList = out.clear().add(this)

    fun toList(): List<MPoint> {
        val out = arrayListOf<MPoint>()
        fastForEach { x, y -> out.add(MPoint(x, y)) }
        return out
    }

    private fun index(index: Int, offset: Int): Int = index * 2 + offset

    override fun getX(index: Int) = data.getAt(index(index, 0))
    override fun getY(index: Int) = data.getAt(index(index, 1))

    fun insertAt(index: Int, p: PointArrayList): PointArrayList {
        data.insertAt(index(index, 0), p.data.data, 0, p.data.size)
        return this
    }

    fun insertAt(index: Int, x: Double, y: Double): PointArrayList {
        data.insertAt(index(index, 0), x, y)
        return this
    }

    fun insertAt(index: Int, point: IPoint) = insertAt(index, point.x, point.y)

    fun removeAt(index: Int, count: Int = 1): PointArrayList {
        data.removeAt(index(index, 0), count * 2)
        return this
    }

    fun setX(index: Int, x: Double) { data[index(index, 0)] = x }
    fun setX(index: Int, x: Int) = setX(index, x.toDouble())
    fun setX(index: Int, x: Float) = setX(index, x.toDouble())

    fun setY(index: Int, y: Double) { data[index(index, 1)] = y }
    fun setY(index: Int, y: Int) = setY(index, y.toDouble())
    fun setY(index: Int, y: Float) = setY(index, y.toDouble())

    fun setXY(index: Int, x: Double, y: Double) {
        data[index(index, 0)] = x
        data[index(index, 1)] = y
    }
    fun setXY(index: Int, x: Int, y: Int) = setXY(index, x.toDouble(), y.toDouble())
    fun setXY(index: Int, x: Float, y: Float) = setXY(index, x.toDouble(), y.toDouble())
    fun setXY(index: Int, p: IPoint) = setXY(index, p.x, p.y)

    fun transform(matrix: MMatrix) {
        for (n in 0 until size) {
            val x = getX(n)
            val y = getY(n)
            setX(n, matrix.transformX(x, y))
            setY(n, matrix.transformY(x, y))
        }
    }

    override fun equals(other: Any?): Boolean = other is PointArrayList && data == other.data
    override fun hashCode(): Int = data.hashCode()

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
        data.swap(index(indexA, 0), index(indexB, 0))
        data.swap(index(indexA, 1), index(indexB, 1))
    }

    fun reverse() {
        for (n in 0 until size / 2) swap(0 + n, size - 1 - n)
    }

    fun sort() {
        genericSort(this, 0, this.size - 1, PointSortOpts)
    }

    object PointSortOpts : SortOps<PointArrayList>() {
        override fun compare(p: PointArrayList, l: Int, r: Int): Int = MPoint.compare(p.getX(l), p.getY(l), p.getX(r), p.getY(r))
        override fun swap(subject: PointArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}

fun pointArrayListOf(vararg values: Int): PointArrayList =
    PointArrayList(values.size / 2).also { it.addRaw(*values.mapDouble { it.toDouble() }) }
fun pointArrayListOf(vararg values: Double): PointArrayList =
    PointArrayList(values.size / 2).also { it.addRaw(*values) }
fun pointArrayListOf(vararg values: IPoint): PointArrayList = PointArrayList(*values)

//////////////////////////////////////

interface IPointIntArrayList {
    val closed: Boolean
    val size: Int
    fun getX(index: Int): Int
    fun getY(index: Int): Int
}

fun IPointIntArrayList.getPoint(index: Int, out: MPointInt = MPointInt()): MPointInt = out.setTo(getX(index), getY(index))
fun IPointIntArrayList.getIPoint(index: Int): IPointInt = IPointInt(getX(index), getY(index))
fun IPointIntArrayList.toPoints(): List<MPointInt> = (0 until size).map { getPoint(it) }
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

inline fun IPointIntArrayList.fastForEachReverse(block: (x: Int, y: Int) -> Unit) {
    for (n in 0 until size) {
        val m = size - 1 - n
        block(getX(m), getY(m))
    }
}

open class PointIntArrayList(capacity: Int = 7) : IPointIntArrayList, Extra by Extra.Mixin() {
    override var closed: Boolean = false
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
    fun addReverse(p: IPointIntArrayList) = this.apply { p.fastForEachReverse { x, y -> add(x, y) } }

    inline fun fastForEach(block: (x: Int, y: Int) -> Unit) {
        for (n in 0 until size) {
            block(getX(n), getY(n))
        }
    }

    fun toList(): List<MPointInt> {
        val out = arrayListOf<MPointInt>()
        fastForEach { x, y -> out.add(MPointInt(x, y)) }
        return out
    }

    override fun getX(index: Int) = xList.getAt(index)
    override fun getY(index: Int) = yList.getAt(index)

    fun setX(index: Int, x: Int) { xList[index] = x }
    fun setY(index: Int, y: Int) { yList[index] = y }
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
        override fun compare(p: PointIntArrayList, l: Int, r: Int): Int = MPointInt.compare(p.getX(l), p.getY(l), p.getX(r), p.getY(r))
        override fun swap(subject: PointIntArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}

inline fun <T> Iterable<T>.mapPoint(temp: MPoint = MPoint(), out: PointArrayList = PointArrayList(), block: MPoint.(value: T) -> MPoint): PointArrayList {
    for (v in this) {
        out.add(block(temp, v))
    }
    return out
}

fun List<IPointArrayList>.flatten(): IPointArrayList =
    PointArrayList(this.sumOf { it.size }).also { out -> this.fastForEach { out.add(it) } }
