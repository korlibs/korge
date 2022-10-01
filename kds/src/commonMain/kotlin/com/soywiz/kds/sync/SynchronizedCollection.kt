package com.soywiz.kds.sync

import com.soywiz.kds.lock.NonRecursiveLock

open class SynchronizedCollection<T>(
    protected val base: MutableCollection<T>,
    protected val lock: NonRecursiveLock = NonRecursiveLock()
) : MutableCollection<T> {
    override val size: Int get() = lock { base.size }
    override fun clear() = lock { base.clear() }
    override fun addAll(elements: Collection<T>): Boolean = lock { base.addAll(elements) }
    override fun add(element: T): Boolean = lock { base.add(element) }
    override fun isEmpty(): Boolean = lock { base.isEmpty() }
    override fun iterator(): MutableIterator<T> = SynchronizedMutableIterator(lock { base.iterator() }, lock)
    override fun retainAll(elements: Collection<T>): Boolean = lock { base.retainAll(elements) }
    override fun removeAll(elements: Collection<T>): Boolean = lock { base.removeAll(elements) }
    override fun remove(element: T): Boolean = lock { base.remove(element) }
    override fun containsAll(elements: Collection<T>): Boolean = lock { base.containsAll(elements) }
    override fun contains(element: T): Boolean = lock { base.contains(element) }
}
