@file:Suppress("NOTHING_TO_INLINE")

package korlibs.datastructure

import Array_from
import JsArray
import JsMap
import JsWeakMap
import get
import set

actual inline fun <T> Any?.fastCastTo(): T = this.unsafeCast<T>()

actual class FastIntMap<T>(dummy: Boolean)


actual fun <T> FastIntMap(): FastIntMap<T> = JsMap().asDynamic()
actual val <T> FastIntMap<T>.size: Int get() = (this.asDynamic()).size
actual fun <T> FastIntMap<T>.keys(): List<Int> = Array_from((this.asDynamic()).keys()).unsafeCast<Array<Int>>().toList()
actual inline operator fun <T> FastIntMap<T>.get(key: Int): T? = (this.asDynamic()).get(key)
actual inline operator fun <T> FastIntMap<T>.set(key: Int, value: T) { (this.asDynamic()).set(key, value) }
actual inline operator fun <T> FastIntMap<T>.contains(key: Int): Boolean = (this.asDynamic()).contains(key) != undefined
actual inline fun <T> FastIntMap<T>.remove(key: Int) { (this.asDynamic()).delete(key) }
actual inline fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int) {
    //@Suppress("UNUSED_VARIABLE") val obj = this.asDynamic()
    //js("for (var key in obj.keys()) if (key >= src && key <= dst) obj.delete(key);")
    for (key in keys) if (key in src..dst) remove(key)
}

actual inline fun <T> FastIntMap<T>.clear() {
    (this.asDynamic()).clear()
}

@Suppress("UnsafeCastFromDynamic")
actual inline fun <T> FastIntMap<T>.fastKeyForEach(callback: (key: Int) -> Unit) {
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
actual inline fun <T> FastStringMap<T>.remove(key: String) { (this.asDynamic()).delete(key) }
actual inline fun <T> FastStringMap<T>.clear() { (this.asDynamic()).clear() }
actual fun <T> FastStringMap<T>.putAll(other: FastStringMap<T>) {
    for (key in other.keys) {
        this[key] = other[key].asDynamic()
    }
}
@Suppress("UnsafeCastFromDynamic")
actual inline fun <T> FastStringMap<T>.fastKeyForEach(callback: (key: String) -> Unit) {
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
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V) { (this.asDynamic()).set(key, value) }
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = (this.asDynamic()).has(key)
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K) { (this.asDynamic()).delete(key) }
actual fun <K, V> FastIdentityMap<K, V>.clear() { (this.asDynamic()).clear() }
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit) {
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

actual class WeakMap<K : Any, V> {
    val wm = JsWeakMap()

    actual operator fun contains(key: K): Boolean = wm.has(key)
    actual operator fun set(key: K, value: V) {
        if (key is String) error("Can't use String as WeakMap keys")
        wm.set(key, value)
    }

    actual operator fun get(key: K): V? = wm.get(key).unsafeCast<V?>()
    actual fun remove(key: K) {
        wm.delete(key)
    }

}


public actual open class FastArrayList<E> internal constructor(@PublishedApi internal val __array: Array<E>) : AbstractMutableList<E>(),
    MutableListEx<E>, RandomAccess {
    @PublishedApi inline internal val jsArray: JsArray<E> get() = __array.unsafeCast<JsArray<E>>()
    public actual constructor() : this(emptyArray())
    @Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
    public actual constructor(initialCapacity: Int) : this(emptyArray())
    public actual constructor(elements: Collection<E>) : this(elements.toTypedArray<Any?>().unsafeCast<Array<E>>()) {}
    public actual fun trimToSize() {}
    public actual fun ensureCapacity(minCapacity: Int) {}

    actual override val size: Int get() = jsArray.length
    @Suppress("UNCHECKED_CAST")
    actual override fun get(index: Int): E = jsArray[rangeCheck(index)]
    actual override fun set(index: Int, element: E): E {
        rangeCheck(index)
        @Suppress("UNCHECKED_CAST")
        return jsArray[index].apply { jsArray[index] = element }
    }

    actual override fun add(element: E): Boolean {
        jsArray.push(element)
        modCount++
        return true
    }

    actual override fun add(index: Int, element: E) {
        jsArray.splice(insertionRangeCheck(index), 0, element)
        modCount++
    }

    private fun _addAll(elements: Array<E>): Boolean {
        if (elements.isEmpty()) return false
        jsArray.push(*elements)
        modCount++
        return true
    }

    override fun addAll(elements: FastArrayList<E>): Boolean = _addAll(elements.jsArray.unsafeCast<Array<E>>())
    actual override fun addAll(elements: Collection<E>): Boolean = _addAll(elements.toTypedArray())

    actual override fun addAll(index: Int, elements: Collection<E>): Boolean {
        insertionRangeCheck(index)
        if (elements.isEmpty()) return false
        jsArray.splice(index, 0, *elements.toTypedArray())
        modCount++
        return true
    }

    actual override fun removeAt(index: Int): E {
        rangeCheck(index)
        modCount++
        return when (index) {
            0 -> jsArray.shift()
            lastIndex -> jsArray.pop()
            else -> jsArray.splice(index, 1).unsafeCast<Array<E>>()[0]
        }
    }

    actual override fun remove(element: E): Boolean {
        val array = this.jsArray
        for (index in 0 until array.length) {
            if (array[index] == element) {
                array.splice(index, 1)
                modCount++
                return true
            }
        }
        return false
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        jsArray.splice(fromIndex, toIndex - fromIndex)
        modCount++
    }

    actual override fun clear() {
        jsArray.length = 0
        modCount++
    }

    actual override fun indexOf(element: E): Int = jsArray.indexOf(element)

    actual override fun lastIndexOf(element: E): Int = jsArray.lastIndexOf(element)

    override fun toString() = "[" + jsArray.unsafeCast<Array<E>>().joinToString(", ") + "]"
    override fun toArray(): Array<Any?> = jsArray.concat().unsafeCast<Array<Any?>>()

    actual inline fun fastForEach(callback: (E) -> Unit) {
        val array = this.jsArray
        var n = 0
        while (n < array.length) {
            callback(array[n++].unsafeCast<E>())
        }
    }
    
    actual inline fun fastForEachWithIndex(callback: (index: Int, value: E) -> Unit) {
        val array = this.jsArray
        var n = 0
        while (n < array.length) {
            callback(n, array[n].unsafeCast<E>())
            n++
        }
    }

    actual inline fun fastForEachReverse(callback: (E) -> Unit) {
        val array = this.jsArray
        var n = 0
        while (n < array.length) {
            callback(array[size - n - 1].unsafeCast<E>())
            n++
        }
    }

    actual inline fun fastForEachReverseWithIndex(callback: (index: Int, value: E) -> Unit) {
        val array = this.jsArray
        var n = 0
        while (n < array.length) {
            val index = array.length - n - 1
            callback(index, array[index].unsafeCast<E>())
            n++
        }
    }

    private fun rangeCheck(index: Int): Int {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
        return index
    }

    private fun insertionRangeCheck(index: Int): Int {
        if (index < 0 || index > size) throw IndexOutOfBoundsException("index: $index, size: $size")
        return index
    }
}
