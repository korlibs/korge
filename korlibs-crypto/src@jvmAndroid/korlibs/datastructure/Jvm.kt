@file:Suppress("NOTHING_TO_INLINE")

package korlibs.datastructure

import java.util.*

actual class WeakMap<K : Any, V> {
    val wm = WeakHashMap<K, V>()
    actual operator fun contains(key: K): Boolean = wm.containsKey(key)
    actual operator fun set(key: K, value: V) {
        if (key is String) error("Can't use String as WeakMap keys")
        wm[key] = value
    }
    actual operator fun get(key: K): V? = wm[key]
    actual fun remove(key: K) {
        wm.remove(key)
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
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V) { this.map[key] = value }
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = key in this.map
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K) { this.map.remove(key) }
actual fun <K, V> FastIdentityMap<K, V>.clear() = this.map.clear()
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit) {
    for (key in this.keys()) {
        callback(key)
    }
}
