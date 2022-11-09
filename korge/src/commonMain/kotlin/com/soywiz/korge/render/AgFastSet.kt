package com.soywiz.korge.render

import com.soywiz.kds.FastArrayList

// @TODO: Use HashSet if items.size increases
internal class AgFastSet<T> : AbstractMutableSet<T>() {
    val items = FastArrayList<T>()
    override val size: Int get() = items.size
    override fun iterator(): MutableIterator<T> = items.iterator()

    override fun add(element: T): Boolean {
        if (element in items) return false
        items.add(element)
        return true
    }

    override fun remove(element: T): Boolean {
        return items.remove(element)
    }

    override operator fun contains(element: T): Boolean = element in items

    override fun clear() {
        items.clear()
    }

    inline fun fastForEach(block: (T) -> Unit) {
        items.fastForEach(block)
    }
}
