package com.soywiz.kds.sync

import com.soywiz.kds.BaseMutableMap
import com.soywiz.kds.lock.NonRecursiveLock

open class SynchronizedMap<K, V>(
    protected val base: MutableMap<K, V>,
    protected val lock: NonRecursiveLock = NonRecursiveLock()
) : BaseMutableMap<K, V> {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = SynchronizedSet(base.entries)
    override val keys: MutableSet<K> get() = SynchronizedSet(base.keys)
    override val size: Int get() = base.size
    override val values: MutableCollection<V> get() = SynchronizedCollection(base.values, lock)
    override fun clear() { lock { base.clear() } }
    override fun remove(key: K): V? = lock { base.remove(key) }
    override fun put(key: K, value: V): V? = lock { base.put(key, value) }
    override fun get(key: K): V? = lock { base[key] }
    override fun putAll(from: Map<out K, V>) { lock { base.putAll(from) } }
    override fun isEmpty(): Boolean = lock { base.isEmpty() }
    override fun containsKey(key: K): Boolean = lock { base.containsKey(key) }
    override fun containsValue(value: V): Boolean = lock { base.containsValue(value) }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SynchronizedMap<*, *>) return false
        return lock { this.base == other.base }
    }
    override fun hashCode(): Int = lock { base.hashCode() }
    override fun toString(): String = lock { base.toString() }
}
