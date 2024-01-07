package korlibs.datastructure

open class CacheMap<K, V>(
    val maxSize: Int = 16,
) : BaseCacheMap<K, V>() {
    override fun mustFree(key: K, value: V): Boolean = size > maxSize
}

open class BaseCacheMap<K, V>() : BaseMutableMap<K, V> {
    val map: LinkedHashMap<K, V> = LinkedHashMap<K, V>()

    protected open fun mustFree(key: K, value: V): Boolean = false
    protected open fun keyToRemove(key: K, value: V): K = map.keys.first()
    protected open fun freed(key: K, value: V): Unit {
    }

    override val size: Int get() = map.size

    override fun remove(key: K): V? {
        val value = map.remove(key)
        if (value != null) freed(key, value)
        return value
    }

    override fun putAll(from: Map<out K, V>) { for ((k, v) in from) put(k, v) }
    override fun put(key: K, value: V): V? {
        val oldValue = map[key]
        if (oldValue != value) {
            remove(key) // refresh if exists
            map[key] = value
            putNew(key, value)
        }
        //while (isNotEmpty() && mustFree(key, value) && !map.containsKey(key)) {
        while (isNotEmpty() && mustFree(key, value)) {
            val keyToRemove = keyToRemove(key, value)
            remove(keyToRemove)
        }
        return oldValue
    }

    protected open fun putNew(key: K, value: V) {
    }

    override fun clear() {
        val keys = map.keys.toList()
        for (key in keys) remove(key)
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries
    override val keys: MutableSet<K> get() = map.keys
    override val values: MutableCollection<V> get() = map.values

    override fun get(key: K): V? = map.get(key)

    override fun toString(): String = map.toString()

    override fun equals(other: Any?): Boolean = (other is BaseCacheMap<*, *>) && (this.map == other.map)
    override fun hashCode(): Int = this.map.hashCode()
}
