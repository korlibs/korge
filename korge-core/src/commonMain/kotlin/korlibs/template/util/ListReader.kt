package korlibs.template.util

class ListReader<T> constructor(val list: List<T>, val ctx: T? = null) {
    class OutOfBoundsException(val list: ListReader<*>, val pos: Int) : RuntimeException()

    var position = 0
    val size: Int get() = list.size
    val eof: Boolean get() = position >= list.size
    val hasMore: Boolean get() = position < list.size
    fun peekOrNull(): T? = list.getOrNull(position)
    fun peek(): T = list.getOrNull(position) ?: throw OutOfBoundsException(this, position)
    fun tryPeek(ahead: Int): T? = list.getOrNull(position + ahead)
    fun skip(count: Int = 1) = this.apply { this.position += count }
    fun read(): T = peek().apply { skip(1) }
    fun tryPrev(): T? = list.getOrNull(position - 1)
    fun prev(): T = tryPrev() ?: throw OutOfBoundsException(this, position - 1)
    fun tryRead(): T? = if (hasMore) read() else null
    fun prevOrContext(): T = tryPrev() ?: ctx ?: throw TODO("Context not defined")
    override fun toString(): String = "ListReader($list)"
}
