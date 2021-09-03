package com.soywiz.korlibs.tensork

class TensorShape private constructor(
    val dims: IntArray,
    val elementCount: Int,
    private val indexMult: IntArray,
) : Comparable<TensorShape> {
    val rank get() = dims.size

    companion object {
        operator fun invoke(vararg dims: Int) = TensorShape(
            dims,
            dims.fold(1) { acc, it -> acc * it },
            IntArray(dims.size).also {
                var acc = 1
                for (n in it.indices) {
                    it[n] = acc
                    acc *= dims[n]
                }
            }
        )
    }

    fun index(vararg pos: Int): Int {
        check(pos.size == dims.size)
        var sum = 0
        for (n in pos.indices) sum += pos[n] * indexMult[n]
        return sum
    }

    fun get(array: FloatArray, vararg pos: Int) = array[index(*pos)]
    fun set(array: FloatArray, vararg pos: Int, value: Float) { array[index(*pos)] = value }

    override fun equals(other: Any?): Boolean = other is TensorShape && dims.contentEquals(other.dims)
    override fun hashCode(): Int = dims.contentHashCode()
    fun clone() = TensorShape(dims, elementCount, indexMult)
    override fun toString(): String = "[${dims.joinToString(",")}]"
    override fun compareTo(other: TensorShape): Int = this.elementCount.compareTo(other.elementCount)
}
