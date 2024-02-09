@file:Suppress("NOTHING_TO_INLINE")

package korlibs.datastructure

import korlibs.memory.arraycopy

actual inline fun <T> Any?.fastCastTo(): T = this as T

//@JsName("Map")
//private external class JsMap { }
//@JsName("Array")
//@PublishedApi
//internal external class JsArray<T> {
//    var length: Int
//    //@nativeGetter operator fun get(index: Int): T = definedExternally
//    //@nativeSetter operator fun set(index: Int, value: T): T = definedExternally
//    fun concat(vararg arrays: JsArray<T>): JsArray<T>
//    fun indexOf(e: T): Int
//    fun lastIndexOf(e: T): Int
//    fun splice(start: Int, deleteCount: Int, vararg items: T): JsArray<T>
//    fun unshift(vararg items: T)
//    fun push(vararg items: T)
//    fun shift(): T
//    fun pop(): T
//    companion object {
//        fun from(value: dynamic): Array<dynamic>
//    }
//}

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

actual class FastIdentityMap<K, V>(dummy: Boolean) {
    val map = SlowIdentityHashMap<K, V>()
    val size get() = map.size
}
actual fun <K, V> FastIdentityMap(): FastIdentityMap<K, V> = FastIdentityMap(true)
actual val <K, V> FastIdentityMap<K, V>.size: Int get() = this.map.size
actual fun <K, V> FastIdentityMap<K, V>.keys(): List<K> = this.map.keys.toList()
actual operator fun <K, V> FastIdentityMap<K, V>.get(key: K): V? = this.map[key]
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V) { this.map[key] = value }
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = this.map.containsKey(key)
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K) { this.map.remove(key) }
actual fun <K, V> FastIdentityMap<K, V>.clear() { this.map.clear() }
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit) {
    this.map.buckets.fastForEach { _, bucket ->
        bucket.keys.fastForEach {
            callback(it)
        }
    }
}

//////////////

//@JsName("WeakMap")
//external class JsWeakMap {
//}
//
//@JsFun("(map, k) => { return map.has(k); }")
//external fun JsWeakMap_has(map: JsWeakMap, k: Any?): Boolean
//
//@JsFun("(map, k) => { map.delete(k); }")
//external fun JsWeakMap_delete(map: JsWeakMap, k: Any?)
//
//@JsFun("(map, k) => { return map.has(k); }")
//external fun JsWeakMap_get(map: JsWeakMap, k: Any?): Any?
//
//@JsFun("(map, k, v) => { map.set(k, v); }")
//external fun JsWeakMap_set(map: JsWeakMap, k: Any?, v: Any?): Boolean
//actual class WeakMap<K : Any, V> {
//    val wm = JsWeakMap()
//
//    actual operator fun contains(key: K): Boolean = JsWeakMap_has(wm, key)
//    actual operator fun set(key: K, value: V) {
//        if (key is String) error("Can't use String as WeakMap keys")
//        JsWeakMap_set(wm, key, value)
//    }
//
//    actual operator fun get(key: K): V? = JsWeakMap_get(wm, key).fastCastTo<V?>()
//    actual fun remove(key: K) {
//        JsWeakMap_delete(wm, key)
//    }
//
//}

// @TODO:
actual class WeakMap<K : Any, V> {
    val wm = HashMap<K, V>()

    init {
        println("WARNING! WeakMap not implemented in WASM just yet!")
    }

    actual operator fun contains(key: K): Boolean = wm.contains(key)
    actual operator fun set(key: K, value: V) {
        if (key is String) error("Can't use String as WeakMap keys")
        wm[key] = value
    }

    actual operator fun get(key: K): V? = wm[key]
    actual fun remove(key: K) {
        wm.remove(key)
    }

}

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
