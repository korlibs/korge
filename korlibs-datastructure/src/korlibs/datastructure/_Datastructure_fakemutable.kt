@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.fakemutable

class FakeMutableMap<K, V>(val map: Map<K, V>) : MutableMap<K, V>, Map<K, V> by map {
    override fun clear() = TODO()
    override fun put(key: K, value: V): V? = TODO()
    override fun putAll(from: Map<out K, V>) = TODO()
    override fun remove(key: K): V? = TODO()
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = map.entries.asFakeMutable()
    override val keys: MutableSet<K> get() = map.keys.asFakeMutable()
    override val values: MutableCollection<V> = map.values.asFakeMutable()
}

class FakeMutableCollection<K>(val collection: Collection<K>) : MutableCollection<K>, Collection<K> by collection {
    override fun add(element: K): Boolean = TODO()
    override fun addAll(elements: Collection<K>): Boolean = TODO()
    override fun clear() = TODO()
    override fun remove(element: K): Boolean = TODO()
    override fun removeAll(elements: Collection<K>): Boolean = TODO()
    override fun retainAll(elements: Collection<K>): Boolean = TODO()
    override fun iterator(): MutableIterator<K> = collection.iterator().asFakeMutable()
}

class FakeMutableSet<K>(val set: Set<K>) : MutableSet<K>, Set<K> by set {
    override fun add(element: K): Boolean = TODO()
    override fun addAll(elements: Collection<K>): Boolean = TODO()
    override fun clear() = TODO()
    override fun remove(element: K): Boolean = TODO()
    override fun removeAll(elements: Collection<K>): Boolean = TODO()
    override fun retainAll(elements: Collection<K>): Boolean = TODO()
    override fun iterator(): MutableIterator<K> = set.iterator().asFakeMutable()
}

class FakeMutableMapEntry<K, V>(val entry: Map.Entry<K, V>) : MutableMap.MutableEntry<K, V> {
    override val key: K get() = entry.key
    override val value: V get() = entry.value
    override fun setValue(newValue: V): V = TODO()
}

class FakeMutableSetMapEntry<K, V>(val set: Set<Map.Entry<K, V>>) : MutableSet<MutableMap.MutableEntry<K, V>> {
    override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = TODO()
    override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = TODO()
    override fun clear() = TODO()
    override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = set.map { FakeMutableMapEntry(it) }.iterator().asFakeMutable()
    override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean = TODO()
    override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = TODO()
    override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = TODO()
    override val size: Int get() = set.size
    override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean = set.contains(element)
    override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean = set.containsAll(elements)
    override fun isEmpty(): Boolean = set.isEmpty()
}

class FakeMutableIterator<K>(val iterator: Iterator<K>) : MutableIterator<K>, Iterator<K> by iterator {
    override fun remove() = TODO()
}

class FakeMutableListIterator<K>(val iterator: ListIterator<K>) : MutableListIterator<K>, ListIterator<K> by iterator {
    override fun add(element: K) = TODO()
    override fun remove() = TODO()
    override fun set(element: K) = TODO()
}

fun <K, V> Map<K, V>.asFakeMutable() = FakeMutableMap(this)
fun <K> Set<K>.asFakeMutable() = FakeMutableSet(this)
fun <K> Collection<K>.asFakeMutable() = FakeMutableCollection(this)
fun <K> Iterator<K>.asFakeMutable() = FakeMutableIterator(this)
fun <K> ListIterator<K>.asFakeMutable() = FakeMutableListIterator(this)

fun <K, V> Set<Map.Entry<K, V>>.asFakeMutable(): MutableSet<MutableMap.MutableEntry<K, V>> = FakeMutableSetMapEntry(this)
