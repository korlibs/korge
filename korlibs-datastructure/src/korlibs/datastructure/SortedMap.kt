package korlibs.datastructure

import korlibs.datastructure.comparator.ComparatorComparable
import korlibs.datastructure.map.MutableEntryExt
import korlibs.datastructure.map.MutableMapExt
import kotlin.math.max
import kotlin.math.min

fun <K, V> sortedMapOf(comparator: Comparator<K>, vararg values: Pair<K, V>): SortedMap<K, V> =
    SortedMap<K, V>(comparator).also { it.putAll(values) }

fun <K : Comparable<K>, V> sortedMapOf(vararg values: Pair<K, V>): SortedMap<K, V> =
    SortedMap<K, V>().also { it.putAll(values) }

open class SortedMap<K, V>(val comparator: Comparator<K>) : MutableMapExt<K, V> {
    private val keysToIndex = hashMapOf<K, Int>() // @TODO: Maybe we could just try to perform a binary search?
    private val _keys = FastArrayList<K>()
    private val _values = FastArrayList<V>()
    private var isSorted = true
    override val size get() = _keys.size

    override fun containsKey(key: K): Boolean = keysToIndex.contains(key) // O(1)
    override fun containsValue(value: V): Boolean = _values.contains(value) // O(n)

    override val keys: MutableSet<K> get() {
        ensureSorted()
        return _keys.toMutableSet()
    }
    override val values: MutableCollection<V> get() {
        ensureSorted()
        return _values.toMutableList()
    }
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() {
        ensureSorted()
        return MutableEntryExt.fromMap(this, _keys)
    }

    override fun clear() {
        keysToIndex.clear()
        _keys.clear()
        _values.clear()
        isSorted = true
    }

    override fun put(key: K, value: V): V? {
        val oldValue = get(key)
        set(key, value)
        return oldValue
    }

    companion object {
        inline operator fun <K : Comparable<K>, V> invoke() = SortedMap<K, V>(ComparatorComparable())
    }

    fun swap(indexL: Int, indexR: Int) {
        val keyL = _keys[indexL]
        val keyR = _keys[indexR]
        _keys.swap(indexL, indexR)
        _values.swap(indexL, indexR)
        keysToIndex[keyL] = indexR
        keysToIndex[keyR] = indexL
    }

    object Sorting : SortOps<SortedMap<Any, Any>>() {
        override fun compare(subject: SortedMap<Any, Any>, l: Int, r: Int): Int {
            return subject.comparator.compare(subject._keys[l], subject._keys[r])
        }

        override fun swap(subject: SortedMap<Any, Any>, indexL: Int, indexR: Int) {
            subject.swap(indexL, indexR)
        }
    }

    fun removeAt(index: Int) {
        isSorted = false
        val key = _keys[index]
        swap(index, size - 1)
        _keys.removeAt(size - 1)
        _values.removeAt(size - 1)
        keysToIndex.remove(key)
    }

    override fun remove(key: K): V? {
        val index = keysToIndex[key] ?: return null
        val value = _values[index]
        removeAt(index)
        return value
    }

    @PublishedApi
    internal fun ensureSorted() {
        if (isSorted) return
        genericSort(this, 0, size - 1, Sorting as SortOps<SortedMap<K, V>>)
        isSorted = true
    }

    operator fun set(key: K, value: V) {
        val index = keysToIndex[key]
        if (index != null) {
            _values[index] = value
        } else {
            isSorted = false
            keysToIndex[key] = _keys.size
            _keys.add(key)
            _values.add(value)
        }
    }

    override operator fun get(key: K): V? {
        return _values[keysToIndex[key] ?: return null]
    }

    fun getKeyAt(index: Int): K {
        ensureSorted()
        return _keys[index]
    }

    fun getValueAt(index: Int): V {
        ensureSorted()
        return _values[index]
    }

    inline fun fastForEach(block: (index: Int, key: K, value: V) -> Unit) {
        for (n in 0 until size) {
            block(n, getKeyAt(n), getValueAt(n))
        }
    }

    fun nearestLowHighIndex(key: K, doHigh: Boolean): Int {
        ensureSorted()
        return genericBinarySearch(0, _keys.size, { from, to, low, high -> if (doHigh) max(low, high) else min(low, high) }) { comparator.compare(_keys[it], key) }
    }

    fun nearestLowIndex(key: K): Int = nearestLowHighIndex(key, doHigh = false)
    fun nearestHighIndex(key: K): Int = nearestLowHighIndex(key, doHigh = true)

    fun nearestLow(key: K): K? = _keys.getOrNull(nearestLowIndex(key))
    fun nearestHigh(key: K): K? = _keys.getOrNull(nearestHighIndex(key))

    fun nearestLowExcludingExact(key: K): K? {
        val bindex = nearestLowIndex(key)
        val index = if (key in keysToIndex) bindex - 1 else bindex
        return _keys.getOrNull(index)
    }
    fun nearestHighExcludingExact(key: K): K? {
        val bindex = nearestHighIndex(key)
        val index = if (key in keysToIndex) bindex + 1 else bindex
        return _keys.getOrNull(index)
    }

    fun keysToList(): List<K> {
        ensureSorted()
        return _keys.toFastList()
    }

    fun valuesToList(): List<V> {
        ensureSorted()
        return _values.toFastList()
    }

    fun toList(): List<Pair<K, V>> {
        ensureSorted()
        return (0 until size).map { _keys[it] to _values[it] }
    }

    fun toMap(): Map<K, V> {
        ensureSorted()
        return (0 until size).map { _keys[it] to _values[it] }.toLinkedMap()
    }
}
