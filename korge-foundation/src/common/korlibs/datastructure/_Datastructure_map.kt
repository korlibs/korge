@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.map

import kotlin.collections.set

interface MutableMapExt<K, V> : MutableMap<K, V> {
    override fun isEmpty(): Boolean = size == 0

    override fun putAll(from: Map<out K, V>) {
        from.forEach { set(it.key, it.value) }
    }
}

class MutableEntryExt<K, V>(
    val map: MutableMap<K, V>,
    override val key: K,
) : MutableMap.MutableEntry<K, V> {
    override val value: V get() = map[key]!!

    override fun setValue(newValue: V): V {
        val oldValue = value
        map[key] = newValue
        return oldValue
    }

    companion object {
        fun <K, V> fromMap(map: MutableMap<K, V>, keys: Collection<K>): MutableSet<MutableMap.MutableEntry<K, V>> {
            return keys.map { MutableEntryExt(map, it) }.toMutableSet()
        }
    }
}
