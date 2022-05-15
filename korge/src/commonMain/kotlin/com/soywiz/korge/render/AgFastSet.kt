package com.soywiz.korge.render

import com.soywiz.kds.FastArrayList

// @TODO: Use HashSet if items.size increases
internal class AgFastSet<T> {
    val items = FastArrayList<T>()

    fun add(item: T) {
        if (item in items) return
        items.add(item)
    }

    fun remove(item: T) {
        items.remove(item)
    }

    inline operator fun contains(item: T): Boolean = item in items

    fun clear() {
        items.clear()
    }

    inline fun fastForEach(block: (T) -> Unit) {
        items.fastForEach(block)
    }
}
