package korlibs.datastructure

class FastIdentityCacheMap<K, V> {
    @PublishedApi internal var fast1K: K? = null
    @PublishedApi internal var fast1V: V? = null
    @PublishedApi internal var fast2K: K? = null
    @PublishedApi internal var fast2V: V? = null
    //@PublishedApi internal var fast3K: K? = null
    //@PublishedApi internal var fast3V: V? = null
    @PublishedApi internal var cache: FastIdentityMap<K, V> = FastIdentityMap()

    val size: Int get() = cache.size

    inline fun getOrPut(key: K, gen: (K) -> V): V {
        if (key === fast1K) return fast1V!!
        if (key === fast2K) return fast2V!!
        //if (key === fast3K) return fast3V!!
        val value = cache.getOrPut(key) { gen(key) }
        //fast3K = fast2K; fast3V = fast2V
        fast2K = fast1K; fast2V = fast1V
        fast1K = key; fast1V = value
        return value
    }

    fun get(key: K): V? {
        if (key === fast1K) return fast1V
        if (key === fast2K) return fast2V
        //if (key === fast3K) return fast3V
        return cache[key]
    }

    fun remove(key: K) {
        clearFast()
        cache.remove(key)
    }

    fun clear() {
        clearFast()
        cache.clear()
    }

    @PublishedApi
    internal fun clearFast() {
        fast1K = null; fast1V = null
        fast2K = null; fast2V = null
        //fast3K = null; fast3V = null
    }
}
