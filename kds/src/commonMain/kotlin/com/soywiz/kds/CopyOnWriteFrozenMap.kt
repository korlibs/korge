package com.soywiz.kds

import com.soywiz.kds.atomic.KdsAtomicRef
import com.soywiz.kds.fakemutable.asFakeMutable

class CopyOnWriteFrozenMap<K, V>() : MutableMap<K, V> {
    private val map = KdsAtomicRef(mapOf<K, V>())

    override val size: Int get() = map.value.size
    override fun containsKey(key: K): Boolean = map.value.containsKey(key)
    override fun containsValue(value: V): Boolean = map.value.containsValue(value)
    override fun get(key: K): V? = map.value[key]
    override fun isEmpty(): Boolean = map.value.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.value.entries.asFakeMutable()
    override val keys: MutableSet<K> get() = map.value.keys.asFakeMutable()
    override val values: MutableCollection<V> get() = map.value.values.asFakeMutable()

    override fun clear() = run { map.value = mapOf<K, V>() }
    override fun put(key: K, value: V): V? = map.value[key].also { map.value = map.value + mapOf(key to value) }
    override fun putAll(from: Map<out K, V>) = run { map.value = map.value + from }
    override fun remove(key: K): V? = map.value[key].also { map.value = map.value - key }
    fun removeAll(keys: Collection<K>) = true.also { map.value = map.value - keys }
    fun retainAll(keys: Collection<K>) = true.also {
        val keysSet = keys.toSet()
        map.value = map.value.filterKeys { it in keysSet }
    }
}
