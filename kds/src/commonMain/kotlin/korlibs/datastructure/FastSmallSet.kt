package korlibs.datastructure

class FastSmallSet<T> : AbstractMutableSet<T>() {
    @PublishedApi internal val _items = FastArrayList<T>()
    val items: List<T> get() = _items

    override val size: Int get() = _items.size
    override fun iterator(): MutableIterator<T> = _items.iterator()

    private var fast0: T? = null
    private var fast1: T? = null
    private var fast2: T? = null

    override fun add(element: T): Boolean {
        if (element in this) return false
        _items.add(element)
        return true
    }

    override fun remove(element: T): Boolean {
        fast0 = null
        fast1 = null
        fast2 = null
        return _items.remove(element)
    }

    override operator fun contains(element: T): Boolean {
        if (element === fast0 || element === fast1 || element === fast0) return true
        val result = element in _items
        if (result) {
            fast1 = fast0
            fast2 = fast1
            fast0 = element
        }
        return result
    }

    override fun clear() {
        _items.clear()
        fast0 = null
        fast1 = null
        fast2 = null
    }

    inline fun fastForEach(block: (T) -> Unit) {
        _items.fastForEach(block)
    }
}