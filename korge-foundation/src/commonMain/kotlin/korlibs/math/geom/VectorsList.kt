package korlibs.math.geom

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.number.*
import kotlin.math.*

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
fun Array<out Point>.toPointArrayList(): PointArrayList = PointArrayList(size).also { for (p in this) it.add(p) }

open class PointArrayList(capacity: Int = 7) : PointList, Extra by Extra.Mixin() {
    override var closed: Boolean = false
    private val data = FloatArrayList(capacity * 2)
    override val size get() = data.size / 2

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

    operator fun plusAssign(other: Point): Unit { add(other) }
    operator fun plusAssign(other: PointList): Unit { addAll(other) }

    fun add(p: Point) = add(p.x, p.y)
    @Deprecated("", ReplaceWith("addAll(p)")) fun add(p: PointList) = addAll(p)
    fun addAll(p: PointList) = this.apply { p.fastForEach { (x, y) -> add(x, y) } }
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











sealed interface PointIntList {
    val closed: Boolean
    val size: Int
    fun getX(index: Int): Int
    fun getY(index: Int): Int
    fun get(index: Int): PointInt = PointInt(getX(index), getY(index))
}

open class PointIntArrayList(capacity: Int = 7) : PointIntList, Extra by Extra.Mixin() {
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
        operator fun invoke(capacity: Int = 7, callback: PointIntArrayList.() -> Unit): PointIntArrayList = PointIntArrayList(
            capacity
        ).apply(callback)
        operator fun invoke(points: List<PointInt>): PointIntArrayList = PointIntArrayList(points.size) {
            for (n in points.indices) add(points[n].x, points[n].y)
        }
        operator inline fun <reified T : PointInt> invoke(vararg points: T): PointIntArrayList =
            PointIntArrayList(points.size) {
                for (n in points.indices) add(points[n].x, points[n].y)
            }
    }

    fun add(x: Int, y: Int) = this.apply {
        xList += x
        yList += y
    }
    fun add(p: Vector2Int) = add(p.x, p.y)
    fun add(p: PointIntList) = this.apply { p.fastForEach { x, y -> add(x, y) } }
    fun addReverse(p: PointIntList) = this.apply { p.fastForEachReverse { x, y -> add(x, y) } }

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

    operator fun set(index: Int, value: PointInt) { setX(index, value.x); setY(index, value.y) }
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
        override fun compare(p: PointIntArrayList, l: Int, r: Int): Int =
            MPointInt.compare(p.getX(l), p.getY(l), p.getX(r), p.getY(r))
        override fun swap(subject: PointIntArrayList, indexL: Int, indexR: Int) = subject.swap(indexL, indexR)
    }
}


fun PointIntList.toPoints(): List<PointInt> = (0 until size).map { get(it) }
fun PointIntList.contains(x: Int, y: Int): Boolean {
    for (n in 0 until size) if (getX(n) == x && getY(n) == y) return true
    return false
}
inline fun PointIntList.fastForEach(block: (x: Int, y: Int) -> Unit) {
    for (n in 0 until size) {
        block(getX(n), getY(n))
    }
}

inline fun PointIntList.fastForEachReverse(block: (x: Int, y: Int) -> Unit) {
    for (n in 0 until size) {
        val m = size - 1 - n
        block(getX(m), getY(m))
    }
}

fun List<PointList>.flatten(): PointList =
    PointArrayList(this.sumOf { it.size }).also { out -> this.fastForEach { out.add(it) } }

sealed interface IVectorArrayList : Extra {
    fun isEmpty(): Boolean = size == 0
    fun isNotEmpty(): Boolean = size != 0

    val closed: Boolean
    val size: Int
    val dimensions: Int
    operator fun get(index: Int, dim: Int): Float
    fun getGeneric(index: Int): GenericVector = GenericVector(dimensions, FloatArray(dimensions) { get(index, it) })
}

inline fun IVectorArrayList.getOrElse(index: Int, dim: Int, default: Float = 0f): Float {
    if (index < 0 || index >= size) return default
    if (dim < 0 || dim >= dimensions) return default
    return this[index, dim]
}

inline fun <T : IVectorArrayList> T.fastForEachGeneric(block: T.(n: Int) -> Unit): Unit {
    for (n in 0 until size) {
        block(this, n)
    }
}

fun IVectorArrayList.getX(index: Int): Float = get(index, 0)
fun IVectorArrayList.getY(index: Int): Float = get(index, 1)
fun IVectorArrayList.getZ(index: Int): Float = get(index, 2)

class VectorArrayList(
    override val dimensions: Int,
    capacity: Int = 7,
) : IVectorArrayList, Extra by Extra.Mixin() {
    val data = FloatArrayList(capacity * dimensions)

    override var closed: Boolean = false
    override val size: Int get() = data.size / dimensions

    override fun get(index: Int, dim: Int): Float = data[index * dimensions + dim]
    override fun getGeneric(index: Int): GenericVector = GenericVector(dimensions, data.data, index * dimensions)

    private fun checkDimensions(dim: Int) {
        if (dim != dimensions) error("Invalid dimensions $dim != $dimensions")
    }

    operator fun set(index: Int, dim: Int, value: Float) {
        data[index * dimensions + dim] = value
    }
    private inline fun setInternal(dims: Int, index: Int, block: (Int) -> Unit) {
        checkDimensions(dims)
        block(index * dims)
    }
    fun set(index: Int, vararg values: Float) {
        val rindex = index * dimensions
        for (n in 0 until dimensions) data[rindex + n] = values[n]
    }
    fun set(index: Int, v0: Double) = set(index, v0.toFloat())
    fun set(index: Int, v0: Double, v1: Double) = set(index, v0.toFloat(), v1.toFloat())
    fun set(index: Int, v0: Double, v1: Double, v2: Double) = set(index, v0.toFloat(), v1.toFloat(), v2.toFloat())
    fun set(index: Int, v0: Double, v1: Double, v2: Double, v3: Double) = set(index, v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat())
    fun set(index: Int, v0: Double, v1: Double, v2: Double, v3: Double, v4: Double) = set(index, v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat(), v4.toFloat())
    fun set(index: Int, v0: Double, v1: Double, v2: Double, v3: Double, v4: Double, v5: Double) = set(index, v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat(), v4.toFloat(), v5.toFloat())

    fun set(index: Int, v0: Float) = setInternal(1, index) { data[it] = v0 }
    fun set(index: Int, v0: Float, v1: Float) = setInternal(2, index) { data[it] = v0; data[it + 1] = v1 }
    fun set(index: Int, v0: Float, v1: Float, v2: Float) = setInternal(3, index) { data[it] = v0; data[it + 1] = v1; data[it + 2] = v2 }
    fun set(index: Int, v0: Float, v1: Float, v2: Float, v3: Float) = setInternal(4, index) { data[it] = v0; data[it + 1] = v1; data[it + 2] = v2; data[it + 3] = v3 }
    fun set(index: Int, v0: Float, v1: Float, v2: Float, v3: Float, v4: Float) = setInternal(5, index) { data[it] = v0; data[it + 1] = v1; data[it + 2] = v2; data[it + 3] = v3; data[it + 4] = v4 }
    fun set(index: Int, v0: Float, v1: Float, v2: Float, v3: Float, v4: Float, v5: Float) = setInternal(6, index) { data[it] = v0; data[it + 1] = v1; data[it + 2] = v2; data[it + 3] = v3; data[it + 4] = v4; data[it + 5] = v5 }

    fun set(index: Int, values: DoubleArray, offset: Int = 0) {
        val rindex = index * dimensions
        for (n in 0 until dimensions) data[rindex + n] = values[offset + n].toFloat()
    }
    fun set(index: Int, values: FloatArray, offset: Int = 0) {
        val rindex = index * dimensions
        for (n in 0 until dimensions) data[rindex + n] = values[offset + n]
    }
    fun set(index: Int, vector: GenericVector) {
        set(index, vector.data, vector.offset)
    }
    fun set(index: Int, vector: IGenericVector) {
        val rindex = index * dimensions
        for (n in 0 until dimensions) data[rindex + n] = vector.get(n)
    }
    fun add(values: DoubleArrayList, offset: Int = 0, count: Int = 1) = add(values.data, offset, count)
    fun add(values: DoubleArray, offset: Int = 0, count: Int = 1) {
        val itemCount = dimensions * count
        data.add(values.slice(offset until (offset + itemCount)).mapFloat { it.toFloat() }.toFloatArray(), 0, itemCount)
    }

    fun add(values: FloatArrayList, offset: Int = 0, count: Int = 1) = add(values.data, offset, count)
    fun add(values: FloatArray, offset: Int = 0, count: Int = 1) { data.add(values, offset, dimensions * count) }

    fun add(v0: Float) = checkDimensions(1).also { data.add(v0) }
    fun add(v0: Float, v1: Float) = checkDimensions(2).also { data.add(v0, v1) }
    fun add(v0: Float, v1: Float, v2: Float) = checkDimensions(3).also { data.add(v0, v1, v2) }
    fun add(v0: Float, v1: Float, v2: Float, v3: Float) = checkDimensions(4).also { data.add(v0, v1, v2, v3) }
    fun add(v0: Float, v1: Float, v2: Float, v3: Float, v4: Float) = checkDimensions(5).also { data.add(v0, v1, v2, v3, v4) }
    fun add(v0: Float, v1: Float, v2: Float, v3: Float, v4: Float, v5: Float) = checkDimensions(6).also { data.add(v0, v1, v2, v3, v4, v5) }
    fun add(vararg values: Float) = checkDimensions(values.size).also { data.add(values) }
    
    fun add(v0: Double) = add(v0.toFloat())
    fun add(v0: Double, v1: Double) = add(v0.toFloat(), v1.toFloat())
    fun add(v0: Double, v1: Double, v2: Double) = add(v0.toFloat(), v1.toFloat(), v2.toFloat())
    fun add(v0: Double, v1: Double, v2: Double, v3: Double) = add(v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat())
    fun add(v0: Double, v1: Double, v2: Double, v3: Double, v4: Double) = add(v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat(), v4.toFloat())
    fun add(v0: Double, v1: Double, v2: Double, v3: Double, v4: Double, v5: Double) = add(v0.toFloat(), v1.toFloat(), v2.toFloat(), v3.toFloat(), v4.toFloat(), v5.toFloat())
    fun add(vararg values: Double) = checkDimensions(values.size).also { for (n in 0 until values.size) data.add(values[n].toFloat()) }
    
    fun add(vector: GenericVector) {
        add(vector.data, vector.offset)
    }
    fun add(vector: IGenericVector) {
        for (n in 0 until dimensions) data.add(vector[n])
    }

    fun vectorToStringBuilder(index: Int, out: StringBuilder, roundDecimalPlaces: Int? = null) {
        out.appendGenericArray(dimensions) {
            val v = this@VectorArrayList[index, it].toDouble()
            appendNice(if (roundDecimalPlaces != null) v.roundDecimalPlaces(roundDecimalPlaces) else v)
        }
    }

    fun vectorToString(index: Int): String = buildString { vectorToStringBuilder(index, this) }

    override fun equals(other: Any?): Boolean = other is VectorArrayList && this.dimensions == other.dimensions && this.data == other.data
    override fun hashCode(): Int = data.hashCode()

    override fun toString(): String = toString(roundDecimalPlaces = null)

    fun toString(roundDecimalPlaces: Int? = null): String = buildString {
        append("VectorArrayList[${this@VectorArrayList.size}](\n")
        for (n in 0 until this@VectorArrayList.size) {
            if (n != 0) append(", \n")
            append("   ")
            this@VectorArrayList.vectorToStringBuilder(n, this, roundDecimalPlaces)
        }
        append("\n)")
    }

    fun add(other: VectorArrayList, index: Int, count: Int = 1) {
        add(other.data.data, index * dimensions, count)
    }

    fun clone(): VectorArrayList = VectorArrayList(dimensions, this.size).also { it.add(this, 0, size) }

    fun roundDecimalPlaces(places: Int): VectorArrayList {
        for (n in 0 until data.size) data[n] = data[n].roundDecimalPlaces(places)
        return this
    }

    fun clear() {
        data.clear()
    }
}

fun <T> IVectorArrayList.mapVector(block: (list: IVectorArrayList, index: Int) -> T): List<T> {
    val out = fastArrayListOf<T>()
    for (n in 0 until size) out.add(block(this, n))
    return out
}

fun vectorArrayListOf(vararg vectors: IGenericVector, dimensions: Int = vectors.first().dimensions): VectorArrayList =
    VectorArrayList(dimensions, vectors.size).also { array -> vectors.fastForEach { array.add(it) } }

fun vectorArrayListOf(vararg vectors: GenericVector, dimensions: Int = vectors.first().dimensions): VectorArrayList =
    VectorArrayList(dimensions, vectors.size).also { array -> vectors.fastForEach { array.add(it) } }

fun vectorArrayListOf(vararg data: Float, dimensions: Int): VectorArrayList {
    if (data.size % dimensions != 0) error("${data.size} is not multiple of $dimensions")
    val out = VectorArrayList(dimensions, data.size / dimensions)
    out.data.add(data)
    return out
}
fun vectorArrayListOf(vararg data: Double, dimensions: Int): VectorArrayList =
    vectorArrayListOf(*data.mapFloat { it.toFloat() }, dimensions = dimensions)
fun vectorArrayListOf(vararg data: Int, dimensions: Int): VectorArrayList =
    vectorArrayListOf(*data.mapFloat { it.toFloat() }, dimensions = dimensions)

sealed interface IGenericVector {
    val dimensions: Int
    operator fun get(dim: Int): Float
    operator fun set(dim: Int, value: Float)
}

val IGenericVector.length: Double get() {
    var ssum = 0.0
    for (n in 0 until dimensions) ssum += this[n]
    return sqrt(ssum)
}

fun IGenericVector.toStringBuilder(out: StringBuilder) {
    out.appendGenericArray(dimensions) { appendNice(this@toStringBuilder[it]) }
}

@PublishedApi internal fun StringBuilder.appendGenericArray(size: Int, appendElement: StringBuilder.(Int) -> Unit) {
    append("[")
    for (n in 0 until size) {
        if (n != 0) append(", ")
        appendElement(n)
    }
    append("]")
}

// @TODO: Potential candidate for value class when multiple values are supported
class GenericVector(override val dimensions: Int, val data: FloatArray, val offset: Int = 0) : IGenericVector {
    constructor(vararg data: Double) : this(data.size, data.mapFloat { it.toFloat() })
    constructor(vararg data: Float) : this(data.size, data)
    constructor(vararg data: Int) : this(data.size, data.mapFloat { it.toFloat() })

    override operator fun get(dim: Int): Float = data[offset + dim]
    override operator fun set(dim: Int, value: Float) { data[offset + dim] = value }

    override fun toString(): String = buildString { toStringBuilder(this) }
}
