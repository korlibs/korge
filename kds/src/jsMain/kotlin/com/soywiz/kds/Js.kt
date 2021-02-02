@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.kds

actual class FastIntMap<T>(dummy: Boolean)

@JsName("Map")
private external class JsMap { }
@JsName("Array")
private external class JsArray {
    companion object {
        fun from(value: dynamic): Array<dynamic>
    }
}

actual fun <T> FastIntMap(): FastIntMap<T> = JsMap().asDynamic()
actual val <T> FastIntMap<T>.size: Int get() = (this.asDynamic()).size
actual fun <T> FastIntMap<T>.keys(): List<Int> = Array_from((this.asDynamic()).keys()).unsafeCast<Array<Int>>().toList()
actual inline operator fun <T> FastIntMap<T>.get(key: Int): T? = (this.asDynamic()).get(key)
actual inline operator fun <T> FastIntMap<T>.set(key: Int, value: T): Unit = run { (this.asDynamic()).set(key, value) }
actual inline operator fun <T> FastIntMap<T>.contains(key: Int): Boolean = (this.asDynamic()).contains(key) != undefined
actual inline fun <T> FastIntMap<T>.remove(key: Int): Unit = run { (this.asDynamic()).delete(key) }
actual inline fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int) {
    //@Suppress("UNUSED_VARIABLE") val obj = this.asDynamic()
    //js("for (var key in obj.keys()) if (key >= src && key <= dst) obj.delete(key);")
    for (key in keys) if (key in src..dst) remove(key)
}

actual inline fun <T> FastIntMap<T>.clear() {
    (this.asDynamic()).clear()
}

@Suppress("UnsafeCastFromDynamic")
actual inline fun <T> FastIntMap<T>.fastKeyForEach(callback: (key: Int) -> Unit): Unit {
    //println("FastStringMap<T>.fastKeyForEach")
    val mapIterator = this.asDynamic().keys()
    //console.log(mapIterator)
    while (true) {
        val v = mapIterator.next()
        //console.log(v)
        if (v.done) break
        callback(v.value)
    }
}

/////////////////

actual class FastStringMap<T>(dummy: Boolean)
//actual typealias FastStringMap<T> = Any<T>

actual fun <T> FastStringMap(): FastStringMap<T> = JsMap().asDynamic()
actual val <T> FastStringMap<T>.size: Int get() = this.asDynamic().size
actual fun <T> FastStringMap<T>.keys(): List<String> =
    Array_from((this.asDynamic()).keys()).unsafeCast<Array<String>>().toList()

actual inline operator fun <T> FastStringMap<T>.get(key: String): T? = (this.asDynamic()).get(key)
actual inline operator fun <T> FastStringMap<T>.set(key: String, value: T): Unit =
    run { (this.asDynamic()).set(key, value) }

actual inline operator fun <T> FastStringMap<T>.contains(key: String): Boolean = (this.asDynamic()).has(key)
actual inline fun <T> FastStringMap<T>.remove(key: String): Unit = run { (this.asDynamic()).delete(key) }
actual inline fun <T> FastStringMap<T>.clear() = run { (this.asDynamic()).clear() }

@Suppress("UnsafeCastFromDynamic")
actual inline fun <T> FastStringMap<T>.fastKeyForEach(callback: (key: String) -> Unit): Unit {
    //println("FastStringMap<T>.fastKeyForEach")
    val mapIterator = this.asDynamic().keys()
    //console.log(mapIterator)
    while (true) {
        val v = mapIterator.next()
        //console.log(v)
        if (v.done) break
        callback(v.value)
    }
}

///////////////

actual class FastIdentityMap<K, V>(dummy: Boolean)

actual fun <K, V> FastIdentityMap(): FastIdentityMap<K, V> = JsMap().asDynamic()
actual val <K, V> FastIdentityMap<K, V>.size: Int get() = this.asDynamic().size
actual fun <K, V> FastIdentityMap<K, V>.keys(): List<K> = Array_from((this.asDynamic()).keys()).unsafeCast<Array<K>>().toList()
actual operator fun <K, V> FastIdentityMap<K, V>.get(key: K): V? = (this.asDynamic()).get(key)
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V): Unit = run { (this.asDynamic()).set(key, value) }
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = (this.asDynamic()).has(key)
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K): Unit = run { (this.asDynamic()).delete(key) }
actual fun <K, V> FastIdentityMap<K, V>.clear() = run { (this.asDynamic()).clear() }
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit): Unit {
    //println("FastStringMap<T>.fastKeyForEach")
    val mapIterator = this.asDynamic().keys()
    //console.log(mapIterator)
    while (true) {
        val v = mapIterator.next()
        //console.log(v)
        if (v.done) break
        callback(v.value)
    }
}

//////////////

@JsName("WeakMap")
external class JsWeakMap {
    fun has(k: dynamic): Boolean
    fun set(k: dynamic, v: dynamic): Unit
    fun get(k: dynamic): dynamic
}

actual class WeakMap<K : Any, V> {
    val wm = JsWeakMap()

    actual operator fun contains(key: K): Boolean = wm.has(key)
    actual operator fun set(key: K, value: V) {
        if (key is String) error("Can't use String as WeakMap keys")
        wm.set(key, value)
    }

    actual operator fun get(key: K): V? = wm.get(key).unsafeCast<V?>()
}

internal fun Array_from(value: dynamic): Array<dynamic> = JsArray.from(value)

//@JsName("delete")
//external fun jsDelete(v: dynamic): Unit

public actual open class FastArrayList<E> internal constructor(private var array: Array<Any?>) : AbstractMutableList<E>(), MutableList<E>, RandomAccess {
    private var isReadOnly: Boolean = false

    /**
     * Creates an empty [FastArrayList].
     */
    public actual constructor() : this(emptyArray()) {}

    /**
     * Creates an empty [FastArrayList].
     * @param initialCapacity initial capacity (ignored)
     */
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual constructor(initialCapacity: Int = 0) : this(emptyArray()) {}

    /**
     * Creates an [FastArrayList] filled from the [elements] collection.
     */
    public actual constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>()) {}

    /** Does nothing in this FastArrayList implementation. */
    public actual fun trimToSize() {}

    /** Does nothing in this FastArrayList implementation. */
    public actual fun ensureCapacity(minCapacity: Int) {}

    actual override val size: Int get() = array.size
    @Suppress("UNCHECKED_CAST")
    actual override fun get(index: Int): E = array[rangeCheck(index)].asDynamic()
    actual override fun set(index: Int, element: E): E {
        rangeCheck(index)
        @Suppress("UNCHECKED_CAST")
        return array[index].apply { array[index] = element }.asDynamic()
    }

    actual override fun add(element: E): Boolean {
        array.asDynamic().push(element)
        modCount++
        return true
    }

    actual override fun add(index: Int, element: E): Unit {
        array.asDynamic().splice(insertionRangeCheck(index), 0, element)
        modCount++
    }

    actual override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) return false

        array += elements.toTypedArray<Any?>()
        modCount++
        return true
    }

    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        insertionRangeCheck(index)

        if (index == size) return addAll(elements)
        if (elements.isEmpty()) return false
        when (index) {
            size -> return addAll(elements)
            0 -> array = elements.toTypedArray<Any?>() + array
            else -> array = array.copyOfRange(0, index).asDynamic().concat(elements.toTypedArray<Any?>(), array.copyOfRange(index, size))
        }

        modCount++
        return true
    }

    actual override fun removeAt(index: Int): E {
        rangeCheck(index)
        modCount++
        return if (index == lastIndex)
            array.asDynamic().pop()
        else
            array.asDynamic().splice(index, 1)[0]
    }

    actual override fun remove(element: E): Boolean {
        for (index in array.indices) {
            if (array[index] == element) {
                array.asDynamic().splice(index, 1)
                modCount++
                return true
            }
        }
        return false
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        modCount++
        array.asDynamic().splice(fromIndex, toIndex - fromIndex)
    }

    actual override fun clear() {
        array = emptyArray()
        modCount++
    }

    actual override fun indexOf(element: E): Int = array.indexOf(element)

    actual override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

    override fun toString() = "[" + array.joinToString(", ") + "]"
    override fun toArray(): Array<Any?> = js("[]").slice.call(array)

    private fun rangeCheck(index: Int) = index.apply {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }

    private fun insertionRangeCheck(index: Int) = index.apply {
        if (index < 0 || index > size) {
            throw IndexOutOfBoundsException("index: $index, size: $size")
        }
    }
}
