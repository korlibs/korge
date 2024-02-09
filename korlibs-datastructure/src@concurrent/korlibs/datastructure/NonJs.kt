package korlibs.datastructure

import korlibs.memory.arraycopy

@Suppress("UNCHECKED_CAST")
actual inline fun <T> Any?.fastCastTo(): T = this as T

//actual typealias FastArrayList<E> = ArrayList<E>
public actual open class FastArrayList<E> internal constructor(
    @PublishedApi internal var array: Array<Any?>,
    @PublishedApi internal var _size: Int = array.size,
    @PublishedApi internal var arrayCapacity: Int = array.size
) : AbstractMutableList<E>(), MutableListEx<E>, RandomAccess {
//) : MutableList<E>, RandomAccess {
    private var isReadOnly: Boolean = false

    /**
     * Creates an empty [FastArrayList].
     */
    public actual constructor() : this(arrayOfNulls(16), 0) {}

    /**
     * Creates an empty [FastArrayList].
     * @param initialCapacity initial capacity (ignored)
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual constructor(initialCapacity: Int) : this(arrayOfNulls(initialCapacity), 0) {}

    /**
     * Creates an [FastArrayList] filled from the [elements] collection.
     */
    public actual constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>()) {}

    /** Does nothing in this FastArrayList implementation. */
    public actual fun trimToSize() {}

    /** Does nothing in this FastArrayList implementation. */
    public actual fun ensureCapacity(minCapacity: Int) {
        if (arrayCapacity >= minCapacity) return
        val newSize = kotlin.math.max(arrayCapacity * 2, minCapacity)
        val newArray = array.copyOf(newSize)
        array = newArray
        arrayCapacity = newSize
    }

    actual override val size: Int get() = _size

    @Suppress("UNCHECKED_CAST")
    actual override fun get(index: Int): E = array[rangeCheck(index)] as E
    actual override fun set(index: Int, element: E): E {
        array[rangeCheck(index)] = element
        return element
    }

    actual override fun add(element: E): Boolean {
        ensureCapacity(this.size + 1)
        array[_size++] = element
        return true
    }

    actual override fun add(index: Int, element: E) {
        allocSpace(index, 1)
        this.array[index] = element
    }

    actual override fun addAll(elements: Collection<E>): Boolean {
        return addAll(size, elements)
    }

    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false
        val elementsCount = elements.size
        allocSpace(index, elementsCount)
        arraycopy(elements.toTypedArray<Any?>(), 0, array, index, elementsCount)
        return true
    }

    private fun allocSpace(index: Int, count: Int) {
        ensureCapacity(this.size + count)
        val displaceCount = _size - index
        if (displaceCount > 0) {
            arraycopy(array, index, array, index + count, displaceCount)
        }
        _size += count
    }

    actual override fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    actual override fun removeAt(index: Int): E {
        val out = array[rangeCheck(index)] as E
        removeRange(index, index + 1)
        return out
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        val count = toIndex - fromIndex
        if (count <= 0) return
        val array = this.array
        rangeCheck(fromIndex)
        rangeCheck(toIndex - 1)
        arraycopy(array, toIndex, array, fromIndex, _size - toIndex)
        _size -= count
        array.fill(null, _size, _size + count)
    }

    override fun setAll(index: Int, elements: FastArrayList<E>, offset: Int, size: Int) {
        if (size == 0) return
        rangeCheck(index + size - 1)
        arraycopy(elements.array, offset, this.array, index, size)
    }

    override fun addAll(elements: FastArrayList<E>, offset: Int, size: Int) {
        ensureCapacity(this._size + size)
        arraycopy(elements.array, offset, this.array, this.size, size)
        this._size += size
    }

    actual override fun clear() {
        removeRange(0, size)
    }

    actual override fun contains(element: E): Boolean = indexOf(element) >= 0

    actual override fun indexOf(element: E): Int {
        for (index in 0 until size) {
            if (this[index] == element) {
                return index
            }
        }
        return -1
    }
    actual override fun lastIndexOf(element: E): Int {
        for (index in 0 until size) {
            val i = size - index - 1
            if (this[i] == element) {
                return i
            }
        }
        return -1
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        fastForEachWithIndex { index, value ->
            if (index != 0) {
                sb.append(", ")
            }
            sb.append(value)
        }
        sb.append("]")
        return sb.toString()
    }

    actual inline fun fastForEach(callback: (E) -> Unit) {
        val array = this.array
        val initialSize = _size
        var n = 0
        while (true) {
            val size = kotlin.math.min(initialSize, _size)
            if (n >= size) break
            callback(array[n++] as E)
        }
    }

    actual inline fun fastForEachWithIndex(callback: (index: Int, value: E) -> Unit) {
        val array = this.array
        val initialSize = _size
        var n = 0
        while (true) {
            val size = kotlin.math.min(initialSize, _size)
            if (n >= size) break
            callback(n, array[n] as E)
            n++
        }
    }

    actual inline fun fastForEachReverse(callback: (E) -> Unit) {
        val array = this.array
        val initialSize = _size
        var n = 0
        while (true) {
            val size = kotlin.math.min(initialSize, _size)
            if (n >= size) break
            val index = size - n - 1
            callback(array[index] as E)
            n++
        }
    }

    actual inline fun fastForEachReverseWithIndex(callback: (index: Int, value: E) -> Unit) {
        val array = this.array
        val initialSize = _size
        var n = 0
        while (true) {
            val size = kotlin.math.min(initialSize, _size)
            if (n >= size) break
            val index = size - n - 1
            callback(index, array[index] as E)
            n++
        }
    }

    private inline fun rangeCheck(index: Int): Int {
        if (index < 0 || index >= size) {
            throwIndexOtOfBounds(index)
        }
        return index
    }

    private fun throwIndexOtOfBounds(index: Int) {
        throw IndexOutOfBoundsException("index: $index, size: $size")
    }
}


////////////

actual typealias FastIntMap<T> = IntMap<T>

actual inline fun <T> FastIntMap(): FastIntMap<T> = IntMap()
actual val <T> FastIntMap<T>.size: Int get() = (this as IntMap<T>).size
actual fun <T> FastIntMap<T>.keys(): List<Int> = (this as IntMap<T>).keys.toList()
actual inline operator fun <T> FastIntMap<T>.get(key: Int): T? = (this as IntMap<T>).get(key)
actual inline operator fun <T> FastIntMap<T>.set(key: Int, value: T) { (this as IntMap<T>).set(key, value) }
actual inline operator fun <T> FastIntMap<T>.contains(key: Int): Boolean = (this as IntMap<T>).contains(key)
actual inline fun <T> FastIntMap<T>.remove(key: Int) { (this as IntMap<T>).remove(key) }
actual inline fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int) = (this as IntMap<T>).removeRange(src, dst)
actual inline fun <T> FastIntMap<T>.clear() = (this as IntMap<T>).clear()
actual inline fun <T> FastIntMap<T>.fastKeyForEach(callback: (key: Int) -> Unit) {
    (this as IntMap<T>).fastKeyForEach(callback)
}

///////////

actual class FastStringMap<T>(val dummy: Boolean) {
    //val map = LinkedHashMap<String, T>()
    val map = HashMap<String, T>()
}

actual inline fun <T> FastStringMap(): FastStringMap<T> = FastStringMap(true)
actual val <T> FastStringMap<T>.size: Int get() = (this.map).size
actual inline operator fun <T> FastStringMap<T>.get(key: String): T? = (this.map).get(key)
actual inline operator fun <T> FastStringMap<T>.set(key: String, value: T) { (this.map).set(key, value) }
actual inline operator fun <T> FastStringMap<T>.contains(key: String): Boolean = (this.map).contains(key)
actual inline fun <T> FastStringMap<T>.remove(key: String) { (this.map).remove(key) }
actual inline fun <T> FastStringMap<T>.clear() = (this.map).clear()
actual fun <T> FastStringMap<T>.keys(): List<String> = map.keys.toList()
actual fun <T> FastStringMap<T>.putAll(other: FastStringMap<T>) {
    val that = this as FastStringMap<T?>
    for (key in other.keys) {
        that[key] = other[key]
    }
}

actual inline fun <T> FastStringMap<T>.fastKeyForEach(callback: (key: String) -> Unit) {
    for (key in this.keys()) {
        callback(key)
    }
}

///////////
