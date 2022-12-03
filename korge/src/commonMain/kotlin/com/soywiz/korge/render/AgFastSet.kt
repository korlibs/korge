package com.soywiz.korge.render

import com.soywiz.kds.FastArrayList

// @TODO: Use HashSet if items.size increases
internal class AgFastSet<T> : AbstractMutableSet<T>() {
    val items = FastArrayList<T>()
    override val size: Int get() = items.size
    override fun iterator(): MutableIterator<T> = items.iterator()

    private var fast0: T? = null
    private var fast1: T? = null
    private var fast2: T? = null

    override fun add(element: T): Boolean {
        if (element in this) return false
        items.add(element)
        return true
    }

    override fun remove(element: T): Boolean {
        fast0 = null
        fast1 = null
        fast2 = null
        return items.remove(element)
    }

    override operator fun contains(element: T): Boolean {
        if (element === fast0 || element === fast1 || element === fast0) return true
        val result = element in items
        if (result) {
            fast1 = fast0
            fast2 = fast1
            fast0 = element
        }
        return result
    }

    override fun clear() {
        items.clear()
        fast0 = null
        fast1 = null
        fast2 = null
    }

    inline fun fastForEach(block: (T) -> Unit) {
        items.fastForEach(block)
    }
}
