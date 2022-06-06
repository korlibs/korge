package com.soywiz.korma.geom

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.Extra
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kds.mapDouble
import com.soywiz.kds.mapFloat
import kotlin.math.sqrt

interface IVectorArrayList : Extra {
    val closed: Boolean
    val size: Int
    val dimensions: Int
    fun get(index: Int, dim: Int): Double
    fun getGeneric(index: Int): GenericVector = GenericVector(dimensions, DoubleArray(dimensions) { get(index, it) })
}

inline fun IVectorArrayList.getOrElse(index: Int, dim: Int, default: Double = 0.0): Double {
    if (index < 0 || index >= size) return default
    if (dim < 0 || dim >= dimensions) return default
    return this.get(index, dim)
}

inline fun <T : IVectorArrayList> T.fastForEachGeneric(block: T.(n: Int) -> Unit): Unit {
    for (n in 0 until size) {
        block(this, n)
    }
}

fun IVectorArrayList.getX(index: Int): Double = get(index, 0)
fun IVectorArrayList.getY(index: Int): Double = get(index, 1)
fun IVectorArrayList.getZ(index: Int): Double = get(index, 2)

class VectorArrayList(
    override val dimensions: Int,
    capacity: Int = 7,
) : IVectorArrayList, Extra by Extra.Mixin() {
    val data = DoubleArrayList(capacity * dimensions)

    override var closed: Boolean = false
    override val size: Int get() = data.size / dimensions

    override fun get(index: Int, dim: Int): Double = data[index * dimensions + dim]
    override fun getGeneric(index: Int): GenericVector = GenericVector(dimensions, data.data, index * dimensions)
    fun set(index: Int, dim: Int, value: Double) {
        data[index * dimensions + dim] = value
    }
    fun set(index: Int, vararg values: Double) {
        val rindex = index * dimensions
        for (n in 0 until dimensions) data[rindex + n] = values[n]
    }
    fun set(index: Int, values: DoubleArray, offset: Int = 0) {
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
    fun add(values: DoubleArray, offset: Int = 0) {
        data.add(values, offset, dimensions)
    }
    fun add(vararg values: Double) {
        if (values.size != dimensions) error("Invalid dimensions ${values.size} != $dimensions")
        data.add(values)
    }
    fun add(vector: GenericVector) {
        add(vector.data, vector.offset)
    }
    fun add(vector: IGenericVector) {
        for (n in 0 until dimensions) data.add(vector[n])
    }

    fun vectorToStringBuilder(index: Int, out: StringBuilder) {
        out.append("[")
        for (dim in 0 until dimensions) {
            if (dim != 0) out.append(", ")
            out.append(get(index, dim))
        }
        out.append("]")
    }

    fun vectorToString(index: Int): String = buildString { vectorToStringBuilder(index, this) }

    override fun equals(other: Any?): Boolean = other is VectorArrayList && this.dimensions == other.dimensions && this.data == other.data
    override fun hashCode(): Int = data.hashCode()

    override fun toString(): String = buildString {
        append("VectorArrayList[${this@VectorArrayList.size}](\n")
        for (n in 0 until this@VectorArrayList.size) {
            if (n != 0) append(", \n")
            append("   ")
            this@VectorArrayList.vectorToStringBuilder(n, this)
        }
        append("\n)")
    }
}

fun vectorArrayListOf(vararg vectors: IGenericVector, dimensions: Int = vectors.first().dimensions): VectorArrayList =
    VectorArrayList(dimensions, vectors.size).also { array -> vectors.fastForEach { array.add(it) } }

fun vectorArrayListOf(vararg vectors: GenericVector, dimensions: Int = vectors.first().dimensions): VectorArrayList =
    VectorArrayList(dimensions, vectors.size).also { array -> vectors.fastForEach { array.add(it) } }

fun vectorArrayListOf(vararg data: Double, dimensions: Int): VectorArrayList {
    if (data.size % dimensions != 0) error("${data.size} is not multiple of $dimensions")
    val out = VectorArrayList(dimensions, data.size / dimensions)
    out.data.add(data)
    return out
}
fun vectorArrayListOf(vararg data: Float, dimensions: Int): VectorArrayList =
    vectorArrayListOf(*data.mapDouble { it.toDouble() }, dimensions = dimensions)
fun vectorArrayListOf(vararg data: Int, dimensions: Int): VectorArrayList =
    vectorArrayListOf(*data.mapDouble { it.toDouble() }, dimensions = dimensions)

interface IGenericVector {
    val dimensions: Int
    operator fun get(dim: Int): Double
    operator fun set(dim: Int, value: Double)
}

val IGenericVector.length: Double get() {
    var ssum = 0.0
    for (n in 0 until dimensions) ssum += this.get(n)
    return sqrt(ssum)
}

fun IGenericVector.toStringBuilder(out: StringBuilder) {
    out.append("[")
    for (dim in 0 until dimensions) {
        if (dim != 0) out.append(", ")
        out.append(get(dim))
    }
    out.append("]")
}


// @TODO: Potential candidate for value class when multiple values are supported
class GenericVector(override val dimensions: Int, val data: DoubleArray, val offset: Int = 0) : IGenericVector {
    constructor(vararg data: Double) : this(data.size, data)
    constructor(vararg data: Float) : this(data.size, data.mapDouble { it.toDouble() })
    constructor(vararg data: Int) : this(data.size, data.mapDouble { it.toDouble() })

    override operator fun get(dim: Int): Double = data[offset + dim]
    override operator fun set(dim: Int, value: Double) { data[offset + dim] = value }

    override fun toString(): String = buildString { toStringBuilder(this) }
}
