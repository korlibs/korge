package com.soywiz.kmem

interface StandardList<T> : List<T> {
    override fun containsAll(elements: Collection<T>): Boolean {
        val elementsSet = elements.toSet()
        for (n in 0 until size) if (this[n] in elementsSet) return true
        return false
    }
    override fun contains(element: T): Boolean = indexOf(element) >= 0
    override fun isEmpty(): Boolean = size == 0
    override fun iterator(): Iterator<T> = listIterator(0)
    override fun listIterator(): ListIterator<T> = listIterator(0)
    override fun listIterator(index: Int): ListIterator<T> = StandardListIterator(this, index)
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = StandardSubList(this, fromIndex, toIndex)
    override fun lastIndexOf(element: T): Int {
        for (n in size - 1 downTo 0) if (this[n] == element) return n
        return -1
    }
    override fun indexOf(element: T): Int {
        for (n in 0 until size) if (this[n] == element) return n
        return -1
    }
}

class StandardSubList<T>(val list: List<T>, val start: Int, val end: Int) : StandardList<T> {
    override val size: Int get() = end - start

    override fun get(index: Int): T {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException()
        return list[start + index]
    }
}

class StandardListIterator<T>(val list: List<T>, var index: Int) : ListIterator<T> {
    override fun hasNext(): Boolean = index < list.size

    override fun hasPrevious(): Boolean = (index > 0)

    override fun next(): T {
        return list[index++]
    }

    override fun nextIndex(): Int {
        return index
    }

    override fun previous(): T {
        return list[--index]
    }

    override fun previousIndex(): Int {
        return index - 1
    }
}
