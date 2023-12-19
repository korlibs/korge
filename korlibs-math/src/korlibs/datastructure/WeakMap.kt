package korlibs.datastructure

expect class WeakMap<K : Any, V>() {
    operator fun contains(key: K): Boolean
    operator fun set(key: K, value: V): Unit
    operator fun get(key: K): V?
    fun remove(key: K): Unit
}

inline fun <K : Any, V> WeakMap<K, V>.getOrPut(key: K, value: (K) -> V): V {
    if (key !in this) this[key] = value(key)
    return this[key]!!
}
