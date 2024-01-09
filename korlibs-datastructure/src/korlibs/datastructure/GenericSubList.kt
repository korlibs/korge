package korlibs.datastructure

import korlibs.datastructure.internal.equaler
import korlibs.datastructure.internal.hashCoder

// @TODO: Optimize
class GenericSubList<T>(val base: List<T>, val start: Int, val end: Int) : List<T> {
    init {
        if (start !in 0..base.size) throw IndexOutOfBoundsException("$start")
        if (end !in 0..base.size) throw IndexOutOfBoundsException("$end")
    }

    override val size: Int get() = end - start

    private fun Int.translateIndex(): Int {
        if (this !in 0 until size) throw IndexOutOfBoundsException("$this")
        return start + this
    }

    override fun contains(element: T): Boolean = (0 until size).any { this[it] == element }

    override fun containsAll(elements: Collection<T>): Boolean {
        val elementsSet = elements.toMutableSet()
        for (n in 0 until size) elementsSet -= this[n]
        return elementsSet.isEmpty()
    }

    override fun get(index: Int): T = base[index.translateIndex()]

    override fun indexOf(element: T): Int {
        for (n in 0 until size) if (this[n] == element) return n
        return -1
    }

    override fun lastIndexOf(element: T): Int {
        for (n in size - 1 downTo 0) if (this[n] == element) return n
        return -1
    }

    override fun isEmpty(): Boolean = size == 0
    override fun iterator(): Iterator<T> = GenericListIterator(this)
    override fun listIterator(): ListIterator<T> = GenericListIterator(this)
    override fun listIterator(index: Int): ListIterator<T> = GenericListIterator(this, index)
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = GenericSubList(this, fromIndex, toIndex)

    override fun toString(): String = (0 until size).map { this[it] }.toString()

    override fun equals(other: Any?): Boolean = (other is GenericSubList<*>) && equaler(size) { this[it] == other[it] }
    override fun hashCode(): Int = hashCoder(size) { this[it].hashCode() }
}

class GenericListIterator<T>(val list: List<T>, val iindex: Int = 0) : ListIterator<T> {
    init {
        if (iindex !in 0 until list.size) throw IndexOutOfBoundsException("$iindex")
    }

    private var index = iindex

    override fun hasNext(): Boolean = index < list.size

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        return list[index++]
    }

    override fun hasPrevious(): Boolean = index > 0

    override fun nextIndex(): Int = index

    override fun previous(): T {
        if (!hasPrevious()) throw NoSuchElementException()
        return list.get(--index)
    }

    override fun previousIndex(): Int = index - 1

    override fun equals(other: Any?): Boolean = (other is GenericListIterator<*>) && this.list == other.list && this.index == other.index
    override fun hashCode(): Int = list.hashCode() + index.hashCode()
}
