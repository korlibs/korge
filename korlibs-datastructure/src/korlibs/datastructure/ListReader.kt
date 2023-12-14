package korlibs.datastructure

/**
 * A utility to read [List].
 */
class ListReader<T>(val list: List<T>) {
    var position = 0
    val size: Int get() = list.size
    val eof: Boolean get() = position >= list.size
    val hasMore: Boolean get() = position < list.size
    fun peekOrNull(): T? = list.getOrNull(position)
    fun peek(): T = list[position]
    fun peek(offset: Int): T = list[position + offset]
    fun skip(count: Int = 1) = this.apply { this.position += count }
    fun read(): T = peek().apply { skip(1) }
    override fun toString(): String = "ListReader($list)"
    override fun equals(other: Any?): Boolean = (other is ListReader<*>) && this.list == other.list && this.position == other.position
    override fun hashCode(): Int = this.list.hashCode()
}

fun <T> List<T>.reader() = ListReader(this)

fun <T> ListReader<T>.expect(value: T): T {
    val v = read()
    if (v != value) error("Expecting '$value' but found '$v'")
    return v
}

fun <T> ListReader<T>.dump() {
    println("ListReader:")
    for (item in list) {
        println(" - $item")
    }
}
