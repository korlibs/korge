package korlibs.datastructure.concurrent

import kotlin.concurrent.AtomicReference

class ConcurrentDeque<T : Any> {
    private val items = AtomicReference<List<T>>(emptyList<T>())

    val size get() = items.value.size

    fun add(item: T) {
        do {
            val oldList = this.items.value
            val newList = (oldList + item)
        } while (!this.items.compareAndSet(oldList, newList))
    }

    //val length get() = items.value.size

    fun consume(): T? {
        while (true) {
            val oldList = this.items.value
            if (oldList.isEmpty()) return null
            val lastItem = oldList.first()
            val newList = oldList.subList(1, oldList.size)
            if (this.items.compareAndSet(oldList, newList)) return lastItem
        }
    }
}
