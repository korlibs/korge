package korlibs.datastructure

class FastSmallSet<T> : AbstractMutableSet<T>() {
    @PublishedApi internal val _items = LinkedHashSet<T>()
    //@PublishedApi internal val _items = FastArrayList<T>()
    //val itemsToIndex = LinkedHashMap<T, Int>()
    //val items: List<T> get() = _items

    override val size: Int get() = _items.size
    override fun iterator(): MutableIterator<T> = _items.iterator()

    private var fast0: T? = null
    private var fast1: T? = null
    private var fast2: T? = null

    override fun add(element: T): Boolean {
        if (element in this) return false
        //itemsToIndex[element] = _items.size
        _items.add(element)
        return true
    }

    override fun remove(element: T): Boolean {
        fast0 = null
        fast1 = null
        fast2 = null
        //val index = itemsToIndex.remove(element) ?: return false
        //_items.removeAt(index)
        _items.remove(element)
        return true
    }

    override operator fun contains(element: T): Boolean {
        if (element === fast0 || element === fast1 || element === fast0) return true
        val result = element in _items
        //val result = element in itemsToIndex
        if (result) {
            fast1 = fast0
            fast2 = fast1
            fast0 = element
        }
        return result
    }

    override fun clear() {
        //itemsToIndex.clear()
        _items.clear()
        fast0 = null
        fast1 = null
        fast2 = null
    }

    inline fun fastForEach(block: (T) -> Unit) {
        _items.forEach { block(it) }
        //_items.fastForEach(block)
    }
}
