package korlibs.datastructure

typealias MapList<K, V> = Map<K, List<V>>
typealias MutableMapList<K, V> = MutableMap<K, ArrayList<V>>
typealias LinkedHashMapList<K, V> = LinkedHashMap<K, ArrayList<V>>

fun <K, V> MapList<K, V>.getFirst(key: K): V? = this[key]?.firstOrNull()
fun <K, V> MapList<K, V>.getLast(key: K): V? = this[key]?.lastOrNull()

fun <K, V> MapList<K, V>.flatten(): List<Pair<K, V>> = flatMapIterable().toList()

fun <K, V> MapList<K, V>.flatMapIterable(): Iterable<Pair<K, V>> = object : Iterable<Pair<K, V>> {
    override fun iterator(): Iterator<Pair<K, V>> = flatMapIterator()
}

fun <K, V> MapList<K, V>.flatMapIterator(): Iterator<Pair<K, V>> =
    entries.flatMap { item -> item.value.map { item.key to it } }.iterator()

fun <K, V> MutableMapList<K, V>.append(key: K, value: V): MutableMapList<K, V> {
    getOrPut(key) { arrayListOf() }
    this[key]!! += value
    return this
}

fun <K, V> MutableMapList<K, V>.replace(key: K, value: V): MutableMapList<K, V> {
    remove(key)
    append(key, value)
    return this
}

fun <K, V> MutableMapList<K, V>.appendAll(vararg items: Pair<K, V>): MutableMapList<K, V> =
    this.apply { for ((k, v) in items) append(k, v) }

fun <K, V> MutableMapList<K, V>.replaceAll(vararg items: Pair<K, V>): MutableMapList<K, V> =
    this.apply { for ((k, v) in items) replace(k, v) }

fun <K, V> linkedHashMapListOf(vararg items: Pair<K, V>): MutableMapList<K, V> = LinkedHashMapList<K, V>().apply {
    for ((k, v) in items) append(k, v)
}

fun <K, V> LinkedHashMapList(items: List<Pair<K, V>>): MutableMapList<K, V> = LinkedHashMapList<K, V>().apply {
    for ((k, v) in items) append(k, v)
}

fun <K, V> LinkedHashMapList(items: MapList<K, V>): MutableMapList<K, V> = LinkedHashMapList<K, V>().apply {
    for ((k, values) in items) for (v in values) append(k, v)
}
