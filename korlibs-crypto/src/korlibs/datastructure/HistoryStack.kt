package korlibs.datastructure

class HistoryStack<T>(var maxLength: Int = Int.MAX_VALUE - 10, initialCapacity: Int = 7) {
    private val deque = Deque<T>(initialCapacity)
    private var position = 0

    fun push(value: T) {
        while (deque.isNotEmpty() && deque.size > position) deque.removeLast()
        deque.add(value)
        position = deque.size
        while (deque.isNotEmpty() && deque.size > maxLength) {
            deque.removeFirst()
            position--
        }
    }

    fun current(): T? {
        return deque.getOrNull(position - 1)
    }

    fun undo(): T? {
        position = (position - 1).coerceAtLeast(0)
        return deque.getOrNull(position - 1)
    }

    fun redo(): T? {
        return deque.getOrNull(position).also {
            position = (position + 1).coerceAtMost(deque.size + 1)
        }
    }
}
