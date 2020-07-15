package com.soywiz.kds

/**
 * Structure containing a set of reusable objects.
 *
 * The method [alloc] retrieves from the pool or allocates a new object,
 * while the [free] method pushes back one element to the pool and resets it to reuse it.
 */
class Pool<T>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T) {
    constructor(preallocate: Int = 0, gen: (Int) -> T) : this({}, preallocate, gen)

    private val items = Stack<T>()
    private var lastId = 0

    val itemsInPool: Int get() = items.size

    init {
        for (n in 0 until preallocate) items.push(gen(lastId++))
    }

    fun alloc(): T = if (items.isNotEmpty()) items.pop() else gen(lastId++)

    fun free(element: T) {
        reset(element)
        items.push(element)
    }

    fun free(vararg elements: T) = run { for (element in elements) free(element) }

    fun free(elements: Iterable<T>) = run { for (element in elements) free(element) }

    inline fun <R> alloc(crossinline callback: (T) -> R): R {
        val temp = alloc()
        try {
            return callback(temp)
        } finally {
            free(temp)
        }
    }

    override fun hashCode(): Int = items.hashCode()
    override fun equals(other: Any?): Boolean = (other is Pool<*>) && this.items == other.items && this.itemsInPool == other.itemsInPool
}
