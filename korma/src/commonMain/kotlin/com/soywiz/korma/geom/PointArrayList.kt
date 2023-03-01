package com.soywiz.korma.geom

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.jvm.*
import kotlin.math.round

sealed interface IPointArrayList : IVectorArrayList, Extra {
    override val dimensions: Int get() = 2
    override fun get(index: Int, dim: Int): Float = if (dim == 0) getX(index) else getY(index)
    fun getX(index: Int): Float
    fun getY(index: Int): Float
    fun getXD(index: Int): Double = getX(index).toDouble()
    fun getYD(index: Int): Double = getY(index).toDouble()
    fun get(index: Int): Point = Point(getX(index), getY(index))
}

operator fun IPointArrayList.get(index: Int): Point = Point(getX(index), getY(index))

fun PointArrayList.setToRoundDecimalPlaces(places: Int): PointArrayList {
    fastForEachWithIndex { index, p -> this.setXY(index, p.roundDecimalPlaces(places)) }
    return this
}

fun IPointArrayList.roundDecimalPlaces(places: Int, out: PointArrayList = PointArrayList()): IPointArrayList {
    fastForEach { p -> out.add(p.roundDecimalPlaces(places)) }
    return out
}

//fun IPointArrayList.getComponent(index: Int, component: Int): Double = if (component == 0) getX(index) else getY(index)

fun IPointArrayList.getComponentList(component: Int, out: FloatArray = FloatArray(size)): FloatArray {
    for (n in 0 until size) out[n] = get(n, component)
    return out
}

val IPointArrayList.first: Point get() = get(0)
@Deprecated("") val IPointArrayList.firstX: Float get() = getX(0)
@Deprecated("") val IPointArrayList.firstY: Float get() = getY(0)
val IPointArrayList.last: Point get() = get(size - 1)
@Deprecated("") val IPointArrayList.lastX: Float get() = getX(size - 1)
@Deprecated("") val IPointArrayList.lastY: Float get() = getY(size - 1)
fun IPointArrayList.firstPoint(): Point = first
fun IPointArrayList.lastPoint(): Point = last

fun IPointArrayList.orientation(): Orientation {
    if (size < 3) return Orientation.COLLINEAR
    return Orientation.orient2dFixed(get(0), get(1), get(2))
}

inline fun IPointArrayList.fastForEach(block: (p: Point) -> Unit) {
    for (n in 0 until size) {
        block(get(n))
    }
}

inline fun IPointArrayList.fastForEachReverse(block: (p: Point) -> Unit) {
    for (n in 0 until size) {
        val index = size - n - 1
        block(get(index))
    }
}

inline fun IPointArrayList.fastForEachWithIndex(block: (index: Int, p: Point) -> Unit) {
    for (n in 0 until size) {
        block(n, get(n))
    }
}

fun IPointArrayList.getPoint(index: Int): Point = get(index)
fun IPointArrayList.getMPoint(index: Int, out: MPoint = MPoint()): MPoint = out.setTo(getX(index), getY(index))
fun IPointArrayList.getIPoint(index: Int): IPoint = IPoint(getX(index), getY(index))

fun IPointArrayList.toList(): List<Point> = (0 until size).map { getPoint(it) }
fun IPointArrayList.toMList(): List<MPoint> = (0 until size).map { getMPoint(it) }

fun IPointArrayList.toPoints(): List<Point> = (0 until size).map { getPoint(it) }
fun IPointArrayList.toMPoints(): List<MPoint> = (0 until size).map { getMPoint(it) }
fun IPointArrayList.toIPoints(): List<IPoint> = (0 until size).map { getIPoint(it) }

fun <T> IPointArrayList.map(gen: (Point) -> T): List<T> = (0 until size).map { gen(get(it)) }

fun IPointArrayList.mapPoints(gen: (Point) -> Point): IPointArrayList {
    val out = PointArrayList(this.size)
    fastForEach { out.add(gen(it)) }
    return out
}

fun IPointArrayList.contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
fun IPointArrayList.contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())
fun IPointArrayList.contains(x: Float, y: Float): Boolean {
    for (n in 0 until size) if (getX(n) == x && getY(n) == y) return true
    return false
}

fun IPointArrayList.clone(out: PointArrayList = PointArrayList(this.size)): PointArrayList {
    fastForEach { out.add(it) }
    return out
}

open class PointArrayList(capacity: Int = 7) : IPointArrayList, Extra by Extra.Mixin() {
    override var closed: Boolean = false
    private val data = FloatArrayList(capacity * 2)
    override val size get() = data.size / 2

    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    fun clear(): PointArrayList {
        data.clear()
        return this
    }

    constructor(points: List<IPoint>) : this(points.size) {
        for (n in points.indices) add(points[n].x, points[n].y)
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
        operator fun invoke(points: List<Point>): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
        inline operator fun <T : Point> invoke(vararg points: T): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n])
        }
    }

    /**
     * Adds points with [values] in the format of interleaved (x, y) values.
     */
    fun addRaw(vararg values: Float) {
        check(values.size % 2 == 0) { "values not multiple of 2 (x, y) but '${values.size}'" }
        data.add(values)
    }

    fun add(x: Float, y: Float): PointArrayList {
        data.add(x, y)
        return this
    }
    fun add(x: Double, y: Double) = add(x.toFloat(), y.toFloat())
    fun add(x: Int, y: Int) = add(x.toFloat(), y.toFloat())

    fun add(p: Point) = add(p.x, p.y)
    fun add(p: MPoint) = add(p.x, p.y)
    fun add(p: IPoint) = add(p.x, p.y)
    fun add(p: IPointArrayList) = this.apply { p.fastForEach { add(it) } }
    fun addReverse(p: IPointArrayList) = this.apply { p.fastForEachReverse { add(it) } }
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

    fun toList(): List<Point> {
        val out = arrayListOf<Point>()
        fastForEach { out.add(Point(it.x, it.y)) }
        return out
    }

    private fun index(index: Int, offset: Int): Int = index * 2 + offset

    override fun getX(index: Int): Float = data.getAt(index(index, 0))
    override fun getY(index: Int): Float = data.getAt(index(index, 1))
    override fun get(index: Int): Point {
        val i = index(index, 0)
        return Point(data.getAt(i), data.getAt(i + 1))
    }

    fun insertAt(index: Int, p: PointArrayList): PointArrayList {
        data.insertAt(index(index, 0), p.data.data, 0, p.data.size)
        return this
    }

    fun insertAt(index: Int, x: Float, y: Float): PointArrayList {
        data.insertAt(index(index, 0), x, y)
        return this
    }

    fun insertAt(index: Int, x: Double, y: Double): PointArrayList = insertAt(index, x.toFloat(), y.toFloat())

    fun insertAt(index: Int, point: Point) = insertAt(index, point.x, point.y)
    fun insertAt(index: Int, point: IPoint) = insertAt(index, point.x, point.y)

    fun removeAt(index: Int, count: Int = 1): PointArrayList {
        data.removeAt(index(index, 0), count * 2)
        return this
    }

    fun setX(index: Int, x: Float) { data[index(index, 0)] = x }
    fun setX(index: Int, x: Int) = setX(index, x.toFloat())
    fun setX(index: Int, x: Double) = setX(index, x.toFloat())

    fun setY(index: Int, y: Float) { data[index(index, 1)] = y }
    fun setY(index: Int, y: Int) = setY(index, y.toFloat())
    fun setY(index: Int, y: Double) = setY(index, y.toFloat())

    fun setXY(index: Int, x: Float, y: Float) {
        val i = index(index, 0)
        data[i + 0] = x
        data[i + 1] = y
    }
    fun setXY(index: Int, x: Int, y: Int) = setXY(index, x.toFloat(), y.toFloat())
    fun setXY(index: Int, x: Double, y: Double) = setXY(index, x.toFloat(), y.toFloat())
    fun setXY(index: Int, p: Point) = setXY(index, p.x, p.y)
    fun setXY(index: Int, p: IPoint) = setXY(index, p.x, p.y)

    fun transform(matrix: IMatrix) {
        for (n in 0 until size) setXY(n, matrix.transform(get(n)))
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
        override fun compare(p: PointArrayList, l: Int, r: Int): Int = Point.compare(p.getX(l), p.getY(l), p.getX(r), p.getY(r))
        override fun swap(subject: PointArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}

fun pointArrayListOf(vararg values: Int): PointArrayList =
    PointArrayList(values.size / 2).also { it.addRaw(*values.mapFloat { it.toFloat() }) }
fun pointArrayListOf(vararg values: Float): PointArrayList =
    PointArrayList(values.size / 2).also { it.addRaw(*values) }
inline fun <T : Point> pointArrayListOf(vararg values: T): PointArrayList = PointArrayList(*values)

//////////////////////////////////////

sealed interface IPointIntArrayList {
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
