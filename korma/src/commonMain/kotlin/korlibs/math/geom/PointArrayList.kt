package korlibs.math.geom

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.annotations.*
import korlibs.math.math.*
import kotlin.math.*

@Deprecated("Use PointList directly")
typealias IPointArrayList = PointList

sealed interface PointList : IVectorArrayList, Extra {
    override val dimensions: Int get() = 2
    override fun get(index: Int, dim: Int): Float = if (dim == 0) getX(index) else getY(index)
    @Deprecated("")
    fun getX(index: Int): Float
    @Deprecated("")
    fun getY(index: Int): Float

    operator fun get(index: Int): Point = Point(getX(index), getY(index))

    fun toList(): List<Point> = ArrayList<Point>(this.size).also { out -> fastForEach { out.add(it) } }

    fun roundDecimalPlaces(places: Int, out: PointArrayList = PointArrayList()): PointList {
        fastForEach { (x, y) -> out.add(x.roundDecimalPlaces(places), y.roundDecimalPlaces(places)) }
        return out
    }

//fun IPointArrayList.getComponent(index: Int, component: Int): Double = if (component == 0) getX(index) else getY(index)

    fun getComponentList(component: Int, out: FloatArray = FloatArray(size)): FloatArray {
        for (n in 0 until size) out[n] = get(n, component)
        return out
    }

    val first: Point get() = get(0)
    val last: Point get() = get(size - 1)

    fun orientation(): Orientation {
        if (size < 3) return Orientation.COLLINEAR
        return Orientation.orient2dFixed(getX(0).toDouble(), getY(0).toDouble(), getX(1).toDouble(), getY(1).toDouble(), getX(2).toDouble(), getY(2).toDouble())
    }

    operator fun contains(p: Point): Boolean = contains(p.x, p.y)
    fun contains(x: Double, y: Double): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Int, y: Int): Boolean = contains(x.toFloat(), y.toFloat())
    fun contains(x: Float, y: Float): Boolean {
        for (n in 0 until size) if (getX(n) == x && getY(n) == y) return true
        return false
    }

    fun clone(out: PointArrayList = PointArrayList(this.size)): PointArrayList {
        out.copyFrom(this)
        return out
    }

    operator fun plus(other: PointList): PointArrayList = PointArrayList(size + other.size).also {
        it.add(this)
        it.add(other)
    }

}

fun PointArrayList.setToRoundDecimalPlaces(places: Int): PointArrayList {
    fastForEachIndexed { index, p -> this[index] = p.roundDecimalPlaces(places) }
    return this
}

inline fun PointList.fastForEachIndexed(block: (index: Int, p: Point) -> Unit) { for (n in 0 until size) block(n, get(n)) }
inline fun PointList.fastForEachReverseIndexed(block: (index: Int, p: Point) -> Unit) { for (n in 0 until size) { val index = size - n - 1; block(index, get(index)) } }
inline fun PointList.fastForEach(block: (Point) -> Unit) { for (n in 0 until size) block(get(n)) }
inline fun PointList.fastForEachReverse(block: (Point) -> Unit) { for (n in 0 until size) block(get(size - n - 1)) }

fun <T> PointList.map(gen: (x: Double, y: Double) -> T): List<T> = (0 until size).map { gen(getX(it).toDouble(), getY(it).toDouble()) }

fun PointList.mapPoints(gen: (p: Point) -> Point): PointList {
    val out = PointArrayList(this.size)
    fastForEach { out.add(gen(it)) }
    return out
}

fun List<Point>.toPointArrayList(): PointArrayList = PointArrayList(size).also { for (p in this) it.add(p) }

open class PointArrayList(capacity: Int = 7) : PointList, Extra by Extra.Mixin() {
    override var closed: Boolean = false
    private val data = FloatArrayList(capacity * 2)
    override val size get() = data.size / 2

    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0

    fun clear(): PointArrayList {
        data.clear()
        return this
    }

    companion object {
        @Deprecated("")
        operator fun invoke(vararg values: Double): PointArrayList = fromGen(values.size) { values[it].toFloat() }
        @Deprecated("")
        operator fun invoke(vararg values: Float): PointArrayList = fromGen(values.size) { values[it] }
        @Deprecated("")
        operator fun invoke(vararg values: Int): PointArrayList = fromGen(values.size) { values[it].toFloat() }
        inline fun fromGen(count: Int, gen: (Int) -> Float): PointArrayList {
            val size = count / 2
            val out = PointArrayList(size)
            for (n in 0 until size) out.add(gen(n * 2 + 0), gen(n * 2 + 1))
            return out
        }

        operator fun invoke(capacity: Int = 7, callback: PointArrayList.() -> Unit): PointArrayList = PointArrayList(capacity).apply(callback)
        operator fun invoke(points: List<Point>): PointArrayList = PointArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
        operator fun invoke(capacity: Int = 7): PointArrayList = PointArrayList(capacity)
        @Deprecated("Use pointArrayListOf")
        operator fun invoke(p0: Point): PointArrayList = PointArrayList(1).add(p0)
        @Deprecated("Use pointArrayListOf")
        operator fun invoke(p0: Point, p1: Point): PointArrayList = PointArrayList(2).add(p0).add(p1)
        @Deprecated("Use pointArrayListOf")
        operator fun invoke(p0: Point, p1: Point, p2: Point): PointArrayList = PointArrayList(3).add(p0).add(p1).add(p2)
        @Deprecated("Use pointArrayListOf")
        operator fun invoke(p0: Point, p1: Point, p2: Point, p3: Point): PointArrayList = PointArrayList(4).add(p0).add(p1).add(p2).add(p3)
        @Deprecated("Use pointArrayListOf")
        inline operator fun <T : Point> invoke(vararg points: T): PointArrayList = pointArrayListOf(*points)
    }

    /**
     * Adds points with [values] in the format of interleaved (x, y) values.
     */
    fun addRaw(vararg values: Double) = addRaw(*values.mapFloat { it.toFloat() })

    fun addRaw(vararg values: Float) {
        check(values.size % 2 == 0) { "values not multiple of 2 (x, y) but '${values.size}'" }
        data.add(values)
    }

    fun add(x: Float, y: Float): PointArrayList {
        data.add(x, y)
        return this
    }
    fun add(x: Double, y: Double) = add(x.toFloat(), y.toFloat())
    fun add(x: Int, y: Int) = add(x.toDouble(), y.toDouble())

    fun add(p: Point) = add(p.x, p.y)
    fun add(p: PointList) = this.apply { p.fastForEach { (x, y) -> add(x, y) } }
    fun addReverse(p: PointList) = this.apply { p.fastForEachReverse { (x, y) -> add(x, y) } }
    fun add(p: PointList, index: Int) {
        add(p.getX(index), p.getY(index))
    }
    fun add(p: PointList, index: Int, indexEnd: Int) {
        // @TODO: Optimize this
        for (n in index until indexEnd) add(p.getX(n), p.getY(n))
    }

    fun copyFrom(other: PointList): PointArrayList {
        clear()
        add(other)
        return this
    }
    override fun clone(out: PointArrayList): PointArrayList = out.clear().add(this)

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
        data.insertAt(index(index, 0), x.toFloat(), y.toFloat())
        return this
    }

    fun insertAt(index: Int, point: Point) = insertAt(index, point.x, point.y)

    fun removeAt(index: Int, count: Int = 1): PointArrayList {
        data.removeAt(index(index, 0), count * 2)
        return this
    }
    fun removeFirst() {
        removeAt(0)
    }
    fun removeLast() {
        removeAt(size - 1)
    }

    fun setX(index: Int, x: Float) { data[index(index, 0)] = x }
    fun setX(index: Int, x: Int) = setX(index, x.toFloat())
    fun setX(index: Int, x: Double) = setX(index, x.toFloat())

    fun setY(index: Int, y: Float) { data[index(index, 1)] = y }
    fun setY(index: Int, y: Int) = setY(index, y.toFloat())
    fun setY(index: Int, y: Double) = setY(index, y.toFloat())

    fun setXY(index: Int, x: Float, y: Float) {
        data[index(index, 0)] = x
        data[index(index, 1)] = y
    }
    fun setXY(index: Int, x: Int, y: Int) = setXY(index, x.toFloat(), y.toFloat())
    fun setXY(index: Int, x: Double, y: Double) = setXY(index, x.toFloat(), y.toFloat())
    fun setXY(index: Int, p: Point) = setXY(index, p.x, p.y)
    operator fun set(index: Int, p: Point) = setXY(index, p.x, p.y)

    fun transform(matrix: Matrix) {
        for (n in 0 until size) set(n, matrix.transform(this[n]))
    }

    override fun equals(other: Any?): Boolean = other is PointArrayList && data == other.data
    override fun hashCode(): Int = data.hashCode()

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('[')
        for (n in 0 until size) {
            val (x, y) = this[n]
            if (n != 0) sb.append(", ")
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
        override fun compare(p: PointArrayList, l: Int, r: Int): Int = Point.compare(p[l], p[r])
        override fun swap(subject: PointArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}

fun pointArrayListOf(vararg values: Int): PointArrayList =
    PointArrayList(values.size / 2).also { it.addRaw(*values.mapDouble { it.toDouble() }) }
fun pointArrayListOf(vararg values: Double): PointArrayList =
    PointArrayList(values.size / 2).also { it.addRaw(*values) }

fun pointArrayListOf(p0: Point): PointArrayList = PointArrayList(1).add(p0)
fun pointArrayListOf(p0: Point, p1: Point): PointArrayList = PointArrayList(2).add(p0).add(p1)
fun pointArrayListOf(p0: Point, p1: Point, p2: Point): PointArrayList = PointArrayList(3).add(p0).add(p1).add(p2)
fun pointArrayListOf(p0: Point, p1: Point, p2: Point, p3: Point): PointArrayList = PointArrayList(4).add(p0).add(p1).add(p2).add(p3)
@KormaExperimental("allocates and boxes all Point")
inline fun <T : Point> pointArrayListOf(vararg points: T): PointArrayList =
    PointArrayList(points.size).also { list -> for (element in points) list.add(element) }
    //PointArrayList(points.size).also { list -> points.fastForEach { list.add(Point.fromRaw(Float2Pack.fromRaw(it as Long))) } }
