package com.soywiz.kds

import com.soywiz.kds.internal.*

@Suppress("UNCHECKED_CAST")
actual inline fun <T> Any?.fastCastTo(): T = this as T

//actual typealias FastArrayList<E> = ArrayList<E>
public actual open class FastArrayList<E> internal constructor(
    @PublishedApi internal var array: Array<Any?>,
    @PublishedApi internal var _size: Int = array.size,
    @PublishedApi internal var arrayCapacity: Int = array.size
) : AbstractMutableList<E>(), MutableList<E>, RandomAccess {
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
    public actual constructor(initialCapacity: Int = 16) : this(arrayOfNulls(initialCapacity), 0) {}

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

    actual override fun add(index: Int, element: E): Unit {
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
        return false
    }

    actual override fun removeAt(index: Int): E {
        val out = array[rangeCheck(index)] as E
        _removeRange(index, index + 1)
        return out
    }

    fun _removeRange(fromIndex: Int, toIndex: Int) {
        val count = toIndex - fromIndex
        if (count <= 0) return
        val array = this.array
        rangeCheck(fromIndex)
        rangeCheck(toIndex - 1)
        arraycopy(array, toIndex, array, fromIndex, _size - toIndex)
        _size -= count
        array.fill(null, _size, _size + count)
    }

    actual override fun clear() {
        _removeRange(0, size)
    }

    // @TODO: This is checking nulls too since backing array is bigger, should we do a plain for?
    actual override fun indexOf(element: E): Int = array.indexOf(element)
    actual override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

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
