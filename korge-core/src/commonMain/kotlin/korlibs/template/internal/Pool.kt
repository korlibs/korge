package korlibs.template.internal

internal class Pool<T>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: (Int) -> T) {
    constructor(preallocate: Int = 0, gen: (Int) -> T) : this({}, preallocate, gen)

    private val items = arrayListOf<T>()
    private var lastId = 0

    init {
        for (n in 0 until preallocate) items.add(gen(lastId++))
    }

    fun alloc(): T = if (items.isNotEmpty()) items.removeAt(items.size - 1) else gen(lastId++)

    fun free(element: T) {
        reset(element)
        items.add(element)
    }

    inline fun <R> alloc(callback: (T) -> R): R {
        val temp = alloc()
        try {
            return callback(temp)
        } finally {
            free(temp)
        }
    }
}
