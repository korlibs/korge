package com.soywiz.kds

class CacheMap<K, V> private constructor(
    private val map: LinkedHashMap<K, V> = LinkedHashMap(),
    val maxSize: Int = 16,
    val free: (K, V) -> Unit = { k, v -> }
) : MutableMap<K, V> by map {
    constructor(
        maxSize: Int = 16,
        free: (K, V) -> Unit = { k, v -> }
    ) : this(LinkedHashMap(), maxSize, free)

    override val size: Int get() = map.size

    override fun remove(key: K): V? {
        val value = map.remove(key)
        if (value != null) free(key, value)
        return value
    }

    override fun putAll(from: Map<out K, V>) = run { for ((k, v) in from) put(k, v) }
    override fun put(key: K, value: V): V? {
        if (size >= maxSize && !map.containsKey(key)) remove(map.keys.first())

        val oldValue = map[key]
        if (oldValue != value) {
            remove(key) // refresh if exists
            map[key] = value
        }
        return oldValue
    }

    override fun clear() {
        val keys = map.keys.toList()
        for (key in keys) remove(key)
    }

    override fun toString(): String = map.toString()

    override fun equals(other: Any?): Boolean = (other is CacheMap<*, *>) && (this.map == other.map) && (this.free == other.free)
    override fun hashCode(): Int = this.map.hashCode() + maxSize
}
