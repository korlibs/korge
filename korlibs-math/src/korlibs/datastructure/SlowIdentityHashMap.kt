package korlibs.datastructure

open class SlowIdentityHashMap<K, V>(initialCapacity: Int = 16) : CustomHashMap<K, V>(
    { it.identityHashCode() }, { a, b -> a === b}, { a, b -> a === b }, initialCapacity
)

fun <K, V> slowIdentityHashMapOf(vararg pairs: Pair<K, V>): SlowIdentityHashMap<K, V> =
    SlowIdentityHashMap<K, V>().also { it.putAll(pairs) }
