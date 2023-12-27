package korlibs.datastructure

open class CustomHashMap<K, V>(
    val hasher: (key: K) -> Int,
    val equalKey: (a: K, b: K) -> Boolean,
    val equalValue: (a: V, b: V) -> Boolean,
    val initialCapacity: Int = 16,
) : MutableMap<K, V> {
    inner class Bucket {
        val keys = fastArrayListOf<K>()
        val values = fastArrayListOf<V>()

        fun getKeyIndex(key: K): Int {
            for (n in keys.indices) if (equalKey(keys[n], key)) return n
            return -1
        }

        fun getValueIndex(value: V): Int {
            for (n in values.indices) if (equalValue(values[n], value)) return n
            return -1
        }
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = buckets.values
            .flatMap { bucket -> bucket?.keys?.indices?.map { it to bucket } ?: listOf() }
            .map { (index, bucket) ->
                object : MutableMap.MutableEntry<K, V> {
                    override val key: K = bucket.keys[index]
                    override val value: V = bucket.values[index]
                    override fun setValue(newValue: V): V = bucket.values[index].also { bucket.values[index] = newValue }
                }
            }
            .toMutableSet()
    override val keys: MutableSet<K> get() = buckets.values.flatMap { it?.keys ?: listOf() }.toMutableSet()
    override val values: MutableCollection<V> get() = buckets.values.flatMap { it?.values ?: listOf() }.toMutableList()

    @PublishedApi
    internal val buckets = IntMap<Bucket>(initialCapacity)
    private fun getOrCreateBucket(key: K): Bucket = buckets.getOrPut(hasher(key)) { Bucket() }
    private fun getBucketOrNull(key: K): Bucket? = buckets.get(hasher(key))

    override var size: Int = 0
        protected set

    override fun clear() {
        size = 0
        buckets.clear()
    }

    override fun isEmpty(): Boolean = size == 0

    override fun get(key: K): V? {
        val bucket = getBucketOrNull(key) ?: return null
        val keys = bucket.keys
        for (n in keys.indices) {
            if (equalKey(keys[n], key)) return bucket.values[n]
        }
        return null
    }

    override fun containsValue(value: V): Boolean {
        buckets.fastForEach { _, bucket ->
            if (bucket.getValueIndex(value) >= 0) return true
        }
        return false
    }

    override fun containsKey(key: K): Boolean {
        val bucket = getBucketOrNull(key) ?: return false
        return bucket.getKeyIndex(key) >= 0
    }

    override fun remove(key: K): V? {
        val bucketKey = hasher(key)
        val bucket = buckets[bucketKey] ?: return null
        val index = bucket.getKeyIndex(key).takeIf { it >= 0 } ?: return null
        size--
        bucket.keys.removeAt(index)
        try {
            return bucket.values.removeAt(index)
        } finally {
            if (bucket.keys.isEmpty()) buckets.remove(bucketKey)
        }
    }

    override fun put(key: K, value: V): V? {
        val bucket = getOrCreateBucket(key)
        val index = bucket.getKeyIndex(key)
        return if (index >= 0) {
            val oldValue = bucket.values[index]
            bucket.values[index] = value
            oldValue
        } else {
            size++
            bucket.keys.add(key)
            bucket.values.add(value)
            null
        }
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) {
            put(k, v)
        }
    }
}
