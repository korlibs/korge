package korlibs.datastructure

open class LRUCache<K, V>(
    val maxSize: Int = Int.MAX_VALUE,
    val maxMemory: Long = Long.MAX_VALUE,
    val atLeastOne: Boolean = true,
    val getElementMemory: (V) -> Int = { 1 }
) : BaseCacheMap<K, V>() {
    var computedMemory: Long = 0L
        private set

    override fun mustFree(key: K, value: V): Boolean {
        if (atLeastOne && size <= 1) return false
        if (size > maxSize) return true
        if (computedMemory > maxMemory) return true
        return false
    }

    override fun putNew(key: K, value: V) {
        computedMemory += getElementMemory(value)
    }

    override fun freed(key: K, value: V) {
        computedMemory -= getElementMemory(value)
    }

    // LRU
    override fun get(key: K): V? {
        if (map.isNotEmpty() && map.keys.last() == key) return map[key]
        val value = map.remove(key)
        if (value != null) map[key] = value
        return value
    }
}
