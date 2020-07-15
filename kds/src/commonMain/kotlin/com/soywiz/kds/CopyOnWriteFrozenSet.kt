package com.soywiz.kds

class CopyOnWriteFrozenSet<T> : MutableSet<T> {
    private val map = CopyOnWriteFrozenMap<T, Unit>()
    override val size: Int get() = map.size

    override fun add(element: T): Boolean = map.containsKey(element).also { map[element] = Unit }
    override fun addAll(elements: Collection<T>): Boolean = true.also { map.putAll(elements.associateWith { Unit }) }
    override fun clear() = map.clear()
    override fun iterator(): MutableIterator<T> = map.keys.iterator()
    override fun remove(element: T): Boolean = map.remove(element) != null
    override fun removeAll(elements: Collection<T>): Boolean = map.removeAll(elements)
    override fun retainAll(elements: Collection<T>): Boolean = map.retainAll(elements)

    override fun contains(element: T): Boolean = map.containsKey(element)
    override fun containsAll(elements: Collection<T>): Boolean = elements.all { map.containsKey(it) }
    override fun isEmpty(): Boolean = map.isEmpty()
}
