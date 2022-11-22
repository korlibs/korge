package com.soywiz.kds.sync

import com.soywiz.kds.BaseMutableList
import com.soywiz.kds.BaseSubMutableList
import com.soywiz.kds.lock.NonRecursiveLock

open class SynchronizedList<T>(
    protected val base: MutableList<T>,
    protected val lock: NonRecursiveLock = NonRecursiveLock()
) : BaseMutableList<T> {
    override fun clear() = lock { base.clear() }
    override fun add(index: Int, element: T) { lock { base.add(index, element) } }
    override fun addAll(index: Int, elements: Collection<T>): Boolean = lock { base.addAll(index, elements) }
    override val size: Int get() = lock { base.size }
    override fun get(index: Int): T = lock { base[index] }
    override fun removeAt(index: Int): T = lock { base.removeAt(index) }
    override fun set(index: Int, element: T): T = lock { base.set(index, element) }
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = SynchronizedBaseSubMutableList(this, fromIndex, toIndex)

    private inner class SynchronizedBaseSubMutableList<T>(mlist: MutableList<T>, start: Int, end: Int) : BaseSubMutableList<T>(mlist, start, end) {
        override fun add(index: Int, element: T) = lock { super.add(index, element) }
        override fun addAll(index: Int, elements: Collection<T>): Boolean = lock { super.addAll(index, elements) }
        override fun removeAt(index: Int): T = lock { super.removeAt(index) }
        override fun set(index: Int, element: T): T = lock { super.set(index, element) }
    }
}
