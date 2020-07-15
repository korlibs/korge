@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.kds

import java.util.*

actual class WeakMap<K : Any, V> {
    val wm = WeakHashMap<K, V>()
    actual operator fun contains(key: K): Boolean = wm.containsKey(key)
    actual operator fun set(key: K, value: V) = run {
        if (key is String) error("Can't use String as WeakMap keys")
        wm[key] = value
    }
    actual operator fun get(key: K): V? = wm[key]
}

//////////////////

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

/////////////////

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

/////////////////

actual class FastIdentityMap<K, V>(dummy: Boolean) {
    val map = IdentityHashMap<K, V>()
}
actual fun <K, V> FastIdentityMap(): FastIdentityMap<K, V> = FastIdentityMap(true)
actual val <K, V> FastIdentityMap<K, V>.size: Int get() = this.map.size
actual fun <K, V> FastIdentityMap<K, V>.keys(): List<K> = this.map.keys.toList()
actual operator fun <K, V> FastIdentityMap<K, V>.get(key: K): V? = this.map[key]
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V): Unit = run { this.map[key] = value }
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = key in this.map
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K): Unit = run { this.map.remove(key) }
actual fun <K, V> FastIdentityMap<K, V>.clear() = this.map.clear()
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit): Unit {
    for (key in this.keys()) {
        callback(key)
    }
}
