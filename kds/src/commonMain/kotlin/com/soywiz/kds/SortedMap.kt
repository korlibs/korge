package com.soywiz.kds

import com.soywiz.kds.comparator.*
import kotlin.math.*

open class SortedMap<K, V>(val comparator: Comparator<K>) {
    private val keysToIndex = hashMapOf<K, Int>() // @TODO: Maybe we could just try to perform a binary search?
    private val keys = FastArrayList<K>()
    private val values = FastArrayList<V>()
    private var isSorted = true
    val size get() = keys.size

    companion object {
        inline operator fun <K : Comparable<K>, V> invoke() = SortedMap<K, V>(ComparatorComparable())
    }

    fun swap(indexL: Int, indexR: Int) {
        val keyL = keys[indexL]
        val keyR = keys[indexR]
        keys.swap(indexL, indexR)
        values.swap(indexL, indexR)
        keysToIndex[keyL] = indexR
        keysToIndex[keyR] = indexL
    }

    object Sorting : SortOps<SortedMap<Any, Any>>() {
        override fun compare(subject: SortedMap<Any, Any>, l: Int, r: Int): Int {
            return subject.comparator.compare(subject.keys[l], subject.keys[r])
        }

        override fun swap(subject: SortedMap<Any, Any>, indexL: Int, indexR: Int) {
            subject.swap(indexL, indexR)
        }
    }

    fun removeAt(index: Int) {
        isSorted = false
        val key = keys[index]
        swap(index, size - 1)
        keys.removeAt(size - 1)
        values.removeAt(size - 1)
        keysToIndex.remove(key)
    }

    fun remove(key: K) {
        removeAt(keysToIndex[key] ?: return)
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
            values[index] = value
        } else {
            isSorted = false
            keysToIndex[key] = keys.size
            keys.add(key)
            values.add(value)
        }
    }

    operator fun get(key: K): V? {
        return values[keysToIndex[key] ?: return null]
    }

    fun getKeyAt(index: Int): K {
        ensureSorted()
        return keys[index]
    }

    fun getValueAt(index: Int): V {
        ensureSorted()
        return values[index]
    }

    inline fun fastForEach(block: (index: Int, key: K, value: V) -> Unit) {
        for (n in 0 until size) {
            block(n, getKeyAt(n), getValueAt(n))
        }
    }

    fun nearestLowHighIndex(key: K, doHigh: Boolean): Int {
        ensureSorted()
        return genericBinarySearch(0, keys.size, { from, to, low, high -> if (doHigh) max(low, high) else min(low, high) }) { comparator.compare(keys[it], key) }
    }

    fun nearestLowIndex(key: K): Int = nearestLowHighIndex(key, doHigh = false)
    fun nearestHighIndex(key: K): Int = nearestLowHighIndex(key, doHigh = true)

    fun nearestLow(key: K): K? = keys.getOrNull(nearestLowIndex(key))
    fun nearestHigh(key: K): K? = keys.getOrNull(nearestHighIndex(key))

    fun nearestLowExcludingExact(key: K): K? {
        val bindex = nearestLowIndex(key)
        val index = if (key in keysToIndex) bindex - 1 else bindex
        return keys.getOrNull(index)
    }
    fun nearestHighExcludingExact(key: K): K? {
        val bindex = nearestHighIndex(key)
        val index = if (key in keysToIndex) bindex + 1 else bindex
        return keys.getOrNull(index)
    }

    fun keysToList(): List<K> {
        ensureSorted()
        return keys.toFastList()
    }

    fun valuesToList(): List<V> {
        ensureSorted()
        return values.toFastList()
    }

    fun toList(): List<Pair<K, V>> {
        ensureSorted()
        return (0 until size).map { keys[it] to values[it] }
    }

    fun toMap(): Map<K, V> {
        ensureSorted()
        return (0 until size).map { keys[it] to values[it] }.toLinkedMap()
    }
}
