package com.soywiz.kds

import com.soywiz.kds.iterators.fastForEach
import kotlin.native.ref.*

// @TODO: use a IntFastMap using the hash of the key to reduce the complexity on big collections.
actual class WeakMap<K : Any, V> {
    private val keys = ArrayList<WeakReference<K>>()
    private val values = ArrayList<V>()

    private var cleanupCounter = 0

    private fun doCleanup() {
        for (n in keys.size - 1 downTo 0) {
            if (keys[n].get() == null) {
                keys.removeAt(n)
                values.removeAt(n)
            }
        }
    }

    private fun gc() {
        if (cleanupCounter++ >= 1000) {
            cleanupCounter = 0
            doCleanup()
        }
    }

    private fun getIndex(key: K): Int {
        gc()
        for (n in 0 until keys.size) {
            if (keys[n].get() == key) return n
        }
        return -1
    }

    actual operator fun contains(key: K): Boolean = getIndex(key) >= 0
    actual operator fun set(key: K, value: V) {
        if (key is String) error("Can't use String as WeakMap keys")
        val index = getIndex(key)
        if (index >= 0) {
            keys[index] = WeakReference(key)
            values[index] = value
        } else {
            keys.add(WeakReference(key))
            values.add(value)
        }
    }
    actual operator fun get(key: K): V? {
        val index = getIndex(key)
        return if (index >= 0) values[index] else null
    }
}


////////////

actual typealias FastIntMap<T> = IntMap<T>

actual inline fun <T> FastIntMap(): FastIntMap<T> = IntMap()
actual val <T> FastIntMap<T>.size: Int get() = (this as IntMap<T>).size
actual fun <T> FastIntMap<T>.keys(): List<Int> = (this as IntMap<T>).keys.toList()
actual inline operator fun <T> FastIntMap<T>.get(key: Int): T? = (this as IntMap<T>).get(key)
actual inline operator fun <T> FastIntMap<T>.set(key: Int, value: T): Unit = run { (this as IntMap<T>).set(key, value) }
actual inline operator fun <T> FastIntMap<T>.contains(key: Int): Boolean = (this as IntMap<T>).contains(key)
actual inline fun <T> FastIntMap<T>.remove(key: Int): Unit = run { (this as IntMap<T>).remove(key) }
actual inline fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int) = (this as IntMap<T>).removeRange(src, dst)
actual inline fun <T> FastIntMap<T>.clear() = (this as IntMap<T>).clear()
actual inline fun <T> FastIntMap<T>.fastKeyForEach(callback: (key: Int) -> Unit): Unit {
    (this as IntMap<T>).fastKeyForEach(callback)
}

actual class FastStringMap<T>(val dummy: Boolean) {
    val map = LinkedHashMap<String, T>()
}

actual inline fun <T> FastStringMap(): FastStringMap<T> = FastStringMap(true)
actual val <T> FastStringMap<T>.size: Int get() = (this.map).size
actual inline operator fun <T> FastStringMap<T>.get(key: String): T? = (this.map).get(key)
actual inline operator fun <T> FastStringMap<T>.set(key: String, value: T): Unit = run { (this.map).set(key, value) }
actual inline operator fun <T> FastStringMap<T>.contains(key: String): Boolean = (this.map).contains(key)
actual inline fun <T> FastStringMap<T>.remove(key: String): Unit = run { (this.map).remove(key) }
actual inline fun <T> FastStringMap<T>.clear() = (this.map).clear()
actual fun <T> FastStringMap<T>.keys(): List<String> = map.keys.toList()

actual inline fun <T> FastStringMap<T>.fastKeyForEach(callback: (key: String) -> Unit): Unit {
    for (key in this.keys()) {
        callback(key)
    }
}

///////////

// @TODO: THIS IS SLOW (linear time for all the operations, but might be enough for small sets)!
/*
@PublishedApi
@SymbolName("Kotlin_Any_hashCode")
external internal fun Any.identityHashCode(): Int
 */
actual class FastIdentityMap<K, V>(dummy: Boolean) {
    val keys = arrayListOf<K>()
    val values = arrayListOf<V>()
    val size get() = keys.size
}
actual fun <K, V> FastIdentityMap(): FastIdentityMap<K, V> = FastIdentityMap(true)
actual val <K, V> FastIdentityMap<K, V>.size: Int get() = this.keys.size
actual fun <K, V> FastIdentityMap<K, V>.keys(): List<K> = this.keys.toList()
actual operator fun <K, V> FastIdentityMap<K, V>.get(key: K): V? {
    val index = keys.indexOf(key)
    return if (index >= 0) values[index] else null
}
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V): Unit {
    val index = keys.indexOf(key)
    if (index >= 0) {
        this.values[index] = value
    } else {
        this.keys.add(key)
        this.values.add(value)
    }
}
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = keys.indexOf(key) >= 0
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K): Unit {
    val index = keys.indexOf(key)
    if (index >= 0) {
        this.keys.removeAt(index)
        this.values.removeAt(index)
    }
}
actual fun <K, V> FastIdentityMap<K, V>.clear() {
    this.keys.clear()
    this.values.clear()
}
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit): Unit {
    this.keys.fastForEach {
        callback(it)
    }
}

