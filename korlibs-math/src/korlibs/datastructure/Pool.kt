package korlibs.datastructure

import korlibs.datastructure.iterators.fastForEach
import korlibs.datastructure.lock.NonRecursiveLock

class TemporalPool<T : Any>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T) {
    private val lock = NonRecursiveLock()
    private val pool = ConcurrentPool<T>(reset, preallocate, gen)
    private val toFree = fastArrayListOf<T>()

    fun alloc(): T = lock { pool.alloc().also { toFree += it } }

    fun freeAll() {
        lock {
            toFree.fastForEach { pool.free(it) }
            toFree.clear()
        }
    }
}

open class ConcurrentPool<T : Any>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T)
    : Pool<T>(reset, preallocate, gen) {
    private val lock = NonRecursiveLock()

    override fun alloc(): T {
        return lock { super.alloc() }
    }

    override fun clear() {
        lock { super.clear() }
    }

    override fun free(element: T) {
        lock { super.free(element) }
    }
}

/**
 * Structure containing a set of reusable objects.
 *
 * The method [alloc] retrieves from the pool or allocates a new object,
 * while the [free] method pushes back one element to the pool and resets it to reuse it.
 */
open class Pool<T : Any>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T) {
    companion object {
        fun <T : Poolable> fromPoolable(preallocate: Int = 0, gen: (Int) -> T): Pool<T> =
            Pool(reset = { it.reset() }, preallocate = preallocate, gen = gen)
    }

    constructor(preallocate: Int = 0, gen: (Int) -> T) : this({}, preallocate, gen)

    private val items = Stack<T>()
    private var lastId = 0

    val totalAllocatedItems get() = lastId
    val totalItemsInUse get() = totalAllocatedItems - itemsInPool
    val itemsInPool: Int get() = items.size

    init {
        for (n in 0 until preallocate) items.push(gen(lastId++))
    }

    open fun alloc(): T {
        return if (items.isNotEmpty()) items.pop() else gen(lastId++)
    }

    interface Poolable {
        fun reset()
    }

    open fun clear() {
        items.clear()
        lastId = 0
    }

    open fun free(element: T) {
        reset(element)
        items.push(element)
    }

    fun freeNotNull(element: T?) {
        if (element == null) return
        free(element)
    }

    fun free(vararg elements: T) { elements.fastForEach { free(it) } }
    fun free(elements: Iterable<T>) { for (element in elements) free(element) }
    fun free(elements: List<T>) { elements.fastForEach { free(it) } }

    inline operator fun <R> invoke(callback: (T) -> R): R = alloc(callback)

    inline fun <R> alloc(callback: (T) -> R): R {
        val temp = alloc()
        try {
            return callback(temp)
        } finally {
            free(temp)
        }
    }

    inline fun <R> alloc2(callback: (T, T) -> R): R {
        val temp1 = alloc()
        val temp2 = alloc()
        try {
            return callback(temp1, temp2)
        } finally {
            free(temp2)
            free(temp1)
        }
    }

    inline fun <R> alloc3(callback: (T, T, T) -> R): R {
        val temp1 = alloc()
        val temp2 = alloc()
        val temp3 = alloc()
        try {
            return callback(temp1, temp2, temp3)
        } finally {
            free(temp3)
            free(temp2)
            free(temp1)
        }
    }

    inline fun <R> allocMultiple(count: Int, temp: FastArrayList<T> = FastArrayList(), callback: (FastArrayList<T>) -> R): R {
        temp.clear()
        for (n in 0 until count) temp.add(alloc())
        try {
            return callback(temp)
        } finally {
            while (temp.isNotEmpty()) free(temp.removeLast())
        }
    }

    inline fun <R> allocThis(callback: T.() -> R): R {
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

class ReturnablePool<T : Any>(private val _reset: (T) -> Unit = { }, private val gen: (index: Int) -> T) {
    private val listToReturn = fastArrayListOf<T>()
    private val list = Pool<T>(reset = { _reset(it) }) { gen(it) }
    var current: T = list.alloc()
        private set

    fun next(): T {
        listToReturn += current
        current = list.alloc()
        return current
    }

    fun reset() {
        listToReturn.fastForEach { list.free(it) }
        listToReturn.clear()
    }
}
