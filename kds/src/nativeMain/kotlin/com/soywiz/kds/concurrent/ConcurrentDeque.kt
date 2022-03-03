package com.soywiz.kds.concurrent

import kotlin.native.concurrent.*

class ConcurrentDeque<T : Any> {
    private val items = AtomicReference<List<T>>(emptyList<T>().freeze())

    init {
        this.freeze()
    }

    val size get() = items.value.size

    fun add(item: T) {
        do {
            val oldList = this.items.value
            val newList = (oldList + item).freeze()
        } while (!this.items.compareAndSet(oldList, newList))
    }

    //val length get() = items.value.size

    fun consume(): T? {
        while (true) {
            val oldList = this.items.value
            if (oldList.isEmpty()) return null
            val lastItem = oldList.first()
            val newList = oldList.subList(1, oldList.size).freeze()
            if (this.items.compareAndSet(oldList, newList)) return lastItem
        }
    }
}
