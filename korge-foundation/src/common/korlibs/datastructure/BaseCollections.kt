@file:Suppress("ReplaceSizeZeroCheckWithIsEmpty")

package korlibs.datastructure

interface BaseMutableMap<K, V> : BaseMap<K, V>, MutableMap<K, V> {
    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from) put(k, v)
    }
}

interface BaseMap<K, V> : Map<K, V> {
    override fun isEmpty(): Boolean = size == 0
    override fun containsKey(key: K): Boolean = keys.contains(key)
    override fun containsValue(value: V): Boolean = values.contains(value)
}

interface BaseMutableList<T> : BaseList<T>, MutableList<T> {
    override fun iterator(): MutableIterator<T> = listIterator(0)
    override fun listIterator(): MutableListIterator<T> = listIterator(0)
    override fun listIterator(index: Int): MutableListIterator<T> = BaseMutableListIterator(this, index)
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = BaseSubMutableList(this, fromIndex, toIndex)

    override fun add(element: T): Boolean {
        add(size, element)
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean = addAll(size, elements)

    override fun clear() {
        while (isNotEmpty()) removeAt(size - 1)
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        val found = index >= 0
        if (found) removeAt(index)
        return found
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val set = elements.toSet()
        return retainAll { it in set }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val set = elements.toSet()
        return removeAll { it in set }
    }
}

interface BaseList<T> : List<T> {
    override fun containsAll(elements: Collection<T>): Boolean {
        val elementsSet = elements.toSet()
        for (n in 0 until size) if (this[n] in elementsSet) return true
        return false
    }
    override fun contains(element: T): Boolean = indexOf(element) >= 0
    override fun isEmpty(): Boolean = size == 0
    override fun iterator(): Iterator<T> = listIterator(0)
    override fun listIterator(): ListIterator<T> = listIterator(0)
    override fun listIterator(index: Int): ListIterator<T> = BaseListIterator(this, index)
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = BaseSubList(this, fromIndex, toIndex)
    override fun lastIndexOf(element: T): Int {
        for (n in size - 1 downTo 0) if (this[n] == element) return n
        return -1
    }
    override fun indexOf(element: T): Int {
        for (n in 0 until size) if (this[n] == element) return n
        return -1
    }
}

open class BaseSubList<T>(val list: List<T>, start: Int, end: Int) : BaseList<T> {
    var start: Int = start ; protected set
    var end: Int = end ; protected set
    override val size: Int get() = end - start
    fun checkIndex(index: Int): Int {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException()
        return start + index
    }

    override fun get(index: Int): T = list[checkIndex(index)]
}

open class BaseSubMutableList<T>(val mlist: MutableList<T>, start: Int, end: Int) : BaseSubList<T>(mlist, start, end), BaseMutableList<T> {
    override fun add(index: Int, element: T) {
        mlist.add(checkIndex(index), element)
        end++
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val before = mlist.size
        val out = mlist.addAll(checkIndex(index), elements)
        end += mlist.size - before
        return out
    }

    override fun removeAt(index: Int): T {
        end--
        return mlist.removeAt(checkIndex(index))
    }

    override fun set(index: Int, element: T): T = mlist.set(checkIndex(index), element)
}

open class BaseListIterator<T>(val list: List<T>, var index: Int) : ListIterator<T> {
    override fun hasNext(): Boolean = index < list.size
    override fun hasPrevious(): Boolean = (index > 0)
    override fun next(): T = list[index++]
    override fun nextIndex(): Int = index
    override fun previous(): T = list[--index]
    override fun previousIndex(): Int = index - 1
}

open class BaseMutableListIterator<T>(val mlist: MutableList<T>, index: Int) : BaseListIterator<T>(mlist, index), MutableListIterator<T> {
    override fun add(element: T) = mlist.add(index, element)
    override fun remove() { mlist.removeAt(index) }
    override fun set(element: T) { mlist[index] = element }
}
