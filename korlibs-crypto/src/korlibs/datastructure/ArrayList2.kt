package korlibs.datastructure

private fun Boolean.toInt() = if (this) 1 else 0

class BooleanArrayList(initialCapacity: Int = 7) {
    val array = IntArrayList(initialCapacity)

    var size: Int
        get() = array.size
        set(value) {
            array.size = value
        }

    operator fun get(index: Int): Boolean = getAt(index)
    operator fun set(index: Int, value: Boolean) = setAt(index, value)

    fun getAt(index: Int): Boolean = array.getAt(index) != 0
    fun setAt(index: Int, value: Boolean) = array.setAt(index, value.toInt())

    fun removeIndex(index: Int): Boolean = array.removeAt(index) != 0

    fun clear() = array.clear()
    fun add(value: Boolean) = array.add(value.toInt())
    fun ensureCapacity(capacity: Int) = array.ensure(capacity)
}

class ShortArrayList(initialCapacity: Int = 7) {
    val array = IntArrayList(initialCapacity)

    var size: Int
        get() = array.size
        set(value) {
            array.size = value
        }

    operator fun get(index: Int): Short = getAt(index)
    operator fun set(index: Int, value: Short) = setAt(index, value)

    fun getAt(index: Int): Short = array.getAt(index).toShort()
    fun setAt(index: Int, value: Short) = array.setAt(index, value.toInt())

    fun removeIndex(index: Int): Short = array.removeAt(index).toShort()

    fun clear() = array.clear()
    fun add(value: Short) = array.add(value.toInt())
    fun ensureCapacity(capacity: Int) = array.ensure(capacity)

    fun toShortArray(): ShortArray {
        val out = ShortArray(size)
        for (n in 0 until size) out[n] = array[n].toShort()
        return out
    }
}
