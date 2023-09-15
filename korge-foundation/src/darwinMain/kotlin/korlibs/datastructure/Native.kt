@file:OptIn(ExperimentalNativeApi::class)

package korlibs.datastructure

import kotlin.experimental.*
import kotlin.native.identityHashCode
import kotlin.native.ref.WeakReference

actual class WeakMap<K : Any, V> {
    inner class Bucket {
        val keys = ArrayList<WeakReference<K>>()
        val values = ArrayList<V>()

        fun getIndex(key: K): Int {
            gc()
            for (n in 0 until keys.size) {
                if (keys[n].get() == key) return n
            }
            return -1
        }
    }

    private val buckets = IntMap<Bucket>()
    private fun bucketHashKey(key: K): Int = key.identityHashCode()
    private fun tryGetBucketForKey(key: K): Bucket? = buckets[bucketHashKey(key)]
    private fun getOrCreateBucketForKey(key: K): Bucket = buckets.getOrPut(bucketHashKey(key)) { Bucket() }

    private var cleanupCounter = 0

    private fun doCleanup() {
        buckets.fastValueForEach { bucket ->
            val keys = bucket.keys
            val values = bucket.values
            for (n in keys.size - 1 downTo 0) {
                if (keys[n].get() == null) {
                    keys.removeAt(n)
                    values.removeAt(n)
                }
            }
        }
    }

    private fun gc() {
        if (cleanupCounter++ >= 1000) {
            cleanupCounter = 0
            doCleanup()
        }
    }

    actual operator fun contains(key: K): Boolean {
        val bucket = tryGetBucketForKey(key) ?: return false
        return bucket.getIndex(key) >= 0
    }
    actual operator fun set(key: K, value: V) {
        if (key is String) error("Can't use String as WeakMap keys")
        val bucket = getOrCreateBucketForKey(key)
        val index = bucket.getIndex(key)
        if (index >= 0) {
            bucket.keys[index] = WeakReference(key)
            bucket.values[index] = value
        } else {
            bucket.keys.add(WeakReference(key))
            bucket.values.add(value)
        }
    }
    actual operator fun get(key: K): V? {
        val bucket = tryGetBucketForKey(key) ?: return null
        val index = bucket.getIndex(key)
        return if (index >= 0) bucket.values[index] else null
    }

    actual fun remove(key: K) {
        val bucketHash = bucketHashKey(key)
        val bucket = buckets[bucketHash] ?: return
        val index = bucket.getIndex(key)
        if (index < 0) return
        bucket.keys.removeAt(index)
        bucket.values.removeAt(index)
        if (bucket.keys.isEmpty()) {
            buckets.remove(bucketHash)
        }
    }
}

/*
@PublishedApi
@SymbolName("Kotlin_Any_hashCode")
external internal fun Any.identityHashCode(): Int
 */
actual class FastIdentityMap<K, V>(dummy: Boolean) {
    val map = SlowIdentityHashMap<K, V>()
    val size get() = map.size
}
actual fun <K, V> FastIdentityMap(): FastIdentityMap<K, V> = FastIdentityMap(true)
actual val <K, V> FastIdentityMap<K, V>.size: Int get() = this.map.size
actual fun <K, V> FastIdentityMap<K, V>.keys(): List<K> = this.map.keys.toList()
actual operator fun <K, V> FastIdentityMap<K, V>.get(key: K): V? = this.map[key]
actual operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V) { this.map[key] = value }
actual operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean = this.map.containsKey(key)
actual fun <K, V> FastIdentityMap<K, V>.remove(key: K) { this.map.remove(key) }
actual fun <K, V> FastIdentityMap<K, V>.clear() { this.map.clear() }
actual inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit) {
    this.map.buckets.fastForEach { _, bucket ->
        bucket.keys.fastForEach {
            callback(it)
        }
    }
}
