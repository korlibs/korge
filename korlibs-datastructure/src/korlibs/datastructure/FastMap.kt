package korlibs.datastructure

expect class FastIntMap<T>

expect fun <T> FastIntMap(): FastIntMap<T>
expect val <T> FastIntMap<T>.size: Int
expect fun <T> FastIntMap<T>.keys(): List<Int>
expect operator fun <T> FastIntMap<T>.get(key: Int): T?
expect operator fun <T> FastIntMap<T>.set(key: Int, value: T)
expect operator fun <T> FastIntMap<T>.contains(key: Int): Boolean
expect fun <T> FastIntMap<T>.remove(key: Int)
expect fun <T> FastIntMap<T>.removeRange(src: Int, dst: Int)
expect fun <T> FastIntMap<T>.clear()

fun <T> FastIntMap<T>.values(): List<T> = this.keys().map { this[it] } as List<T>
val <T> FastIntMap<T>.keys: List<Int> get() = keys()
val <T> FastIntMap<T>.values: List<T> get() = values()

fun <T> FastIntMap<T>.toLinkedMap(): Map<Int, T> {
    val out = linkedHashMapOf<Int, T>()
    fastForEachNullable { key, value ->
        if (value != null) out[key] = value
    }
    return out
}

expect inline fun <T> FastIntMap<T>.fastKeyForEach(callback: (key: Int) -> Unit)

inline fun <T : Any?> FastIntMap<T>.fastValueForEachNullable(callback: (value: T?) -> Unit) {
    fastKeyForEach { callback(this[it]) }
}
inline fun <T : Any?> FastIntMap<T>.fastForEachNullable(callback: (key: Int, value: T?) -> Unit) {
    fastKeyForEach { callback(it, this[it]) }
}

inline fun <T : Any> FastIntMap<T>.fastValueForEach(callback: (value: T) -> Unit) {
    fastKeyForEach { callback(this[it]!!) }
}
inline fun <T : Any> FastIntMap<T>.fastForEach(callback: (key: Int, value: T) -> Unit) {
    fastKeyForEach { callback(it, this[it]!!) }
}

inline fun <T> FastIntMap<T>.getNull(key: Int?): T? = if (key == null) null else get(key)

inline fun <T> FastIntMap<T>.getOrPut(key: Int, callback: () -> T): T {
    val res = get(key)
    if (res != null) return res
    val out = callback()
    set(key, out)
    return out
}

////////////////////////////

expect class FastStringMap<T>

expect fun <T> FastStringMap(): FastStringMap<T>
expect val <T> FastStringMap<T>.size: Int
expect fun <T> FastStringMap<T>.keys(): List<String>
expect operator fun <T> FastStringMap<T>.get(key: String): T?
expect operator fun <T> FastStringMap<T>.set(key: String, value: T): Unit
expect operator fun <T> FastStringMap<T>.contains(key: String): Boolean
expect fun <T> FastStringMap<T>.remove(key: String)
expect fun <T> FastStringMap<T>.clear()
expect fun <T> FastStringMap<T>.putAll(other: FastStringMap<T>)

fun <T> FastStringMap<T>.values(): List<T> = this.keys().map { this[it] } as List<T>
val <T> FastStringMap<T>.keys: List<String> get() = keys()
val <T> FastStringMap<T>.values: List<T> get() = values()

fun <T> FastStringMap<T>.toMap(): Map<String, T> = LinkedHashMap<String, T>(this.size).also { out ->
    fastForEachNullable { key, value -> value?.let { out[key] = value } }
}

expect inline fun <T> FastStringMap<T>.fastKeyForEach(callback: (key: String) -> Unit): Unit

inline fun <T : Any?> FastStringMap<T>.fastValueForEachNullable(callback: (value: T?) -> Unit) {
    fastKeyForEach { callback(this[it]) }
}
inline fun <T : Any?> FastStringMap<T>.fastForEachNullable(callback: (key: String, value: T?) -> Unit) {
    fastKeyForEach { callback(it, this[it]) }
}

inline fun <T : Any> FastStringMap<T>.fastValueForEach(callback: (value: T) -> Unit) {
    fastKeyForEach { callback(this[it]!!) }
}
inline fun <T : Any> FastStringMap<T>.fastForEach(callback: (key: String, value: T) -> Unit) {
    fastKeyForEach { callback(it, this[it]!!) }
}

inline fun <T> FastStringMap<T>.getNull(key: String?): T? = if (key == null) null else get(key)

inline fun <T> FastStringMap<T>.getOrPut(key: String, callback: () -> T): T {
    val res = get(key)
    if (res != null) return res
    val out = callback()
    set(key, out)
    return out
}

////////////////////////////

expect class FastIdentityMap<K, V>
expect fun <K, V> FastIdentityMap(): FastIdentityMap<K, V>
expect val <K, V> FastIdentityMap<K, V>.size: Int
expect fun <K, V> FastIdentityMap<K, V>.keys(): List<K>
expect operator fun <K, V> FastIdentityMap<K, V>.get(key: K): V?
expect operator fun <K, V> FastIdentityMap<K, V>.set(key: K, value: V): Unit
expect operator fun <K, V> FastIdentityMap<K, V>.contains(key: K): Boolean
expect fun <K, V> FastIdentityMap<K, V>.remove(key: K)
expect fun <K, V> FastIdentityMap<K, V>.clear()
expect inline fun <K, V> FastIdentityMap<K, V>.fastKeyForEach(callback: (key: K) -> Unit): Unit

fun <K, V> FastIdentityMap<K, V>.values(): List<V> = this.keys().map { this[it] } as List<V>
val <K, V> FastIdentityMap<K, V>.keys: List<K> get() = keys()
val <K, V> FastIdentityMap<K, V>.values: List<V> get() = values()
fun <K, V> FastIdentityMap<K, V>.getAndRemove(key: K): V? = get(key).also { remove(key) }

fun <K, V> FastIdentityMap<K, V>.toMap(): Map<K, V> = keys.associateWith { this[it] as V }

inline fun <K, V : Any?> FastIdentityMap<K, V>.fastValueForEachNullable(callback: (value: V?) -> Unit) {
    fastKeyForEach { callback(this[it]) }
}
inline fun <K, V : Any?> FastIdentityMap<K, V>.fastForEachNullable(callback: (key: K, value: V?) -> Unit) {
    fastKeyForEach { callback(it, this[it]) }
}

inline fun <K, V : Any> FastIdentityMap<K, V>.fastValueForEach(callback: (value: V) -> Unit) {
    fastKeyForEach { callback(this[it]!!) }
}
inline fun <K, V : Any> FastIdentityMap<K, V>.fastForEach(callback: (key: K, value: V) -> Unit) {
    fastKeyForEach { callback(it, this[it]!!) }
}

inline fun <K, V> FastIdentityMap<K, V>.getNull(key: K?): V? = if (key == null) null else get(key)

inline fun <K, V> FastIdentityMap<K, V>.getOrPut(key: K, callback: (K) -> V): V {
    val res = get(key)
    if (res != null) return res
    val out = callback(key)
    set(key, out)
    return out
}

fun <T : Any> Map<String, T>.toFast() = FastStringMap<T>().apply {
    @Suppress("MapGetWithNotNullAssertionOperator")
    for (k in this@toFast.keys) {
        this[k] = this@toFast[k]!!
    }
}
