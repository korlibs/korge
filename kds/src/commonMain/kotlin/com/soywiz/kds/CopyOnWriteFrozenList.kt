package com.soywiz.kds

import com.soywiz.kds.atomic.KdsAtomicRef
import com.soywiz.kds.fakemutable.asFakeMutable
import com.soywiz.kds.sub.SubMutableList

class CopyOnWriteFrozenList<T> : MutableList<T> {
    private val list = KdsAtomicRef(emptyList<T>())
    override val size: Int get() = list.value.size

    override fun contains(element: T): Boolean = list.value.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = list.value.containsAll(elements)
    override fun get(index: Int): T = list.value[index]
    override fun indexOf(element: T): Int = list.value.indexOf(element)
    override fun isEmpty(): Boolean = list.value.isEmpty()
    override fun iterator(): MutableIterator<T> = list.value.iterator().asFakeMutable()
    override fun lastIndexOf(element: T): Int = list.value.lastIndexOf(element)
    override fun add(element: T): Boolean = true.also { list.value = list.value + element }
    override fun add(index: Int, element: T) {
        val oldList = list.value
        list.value = (oldList.slice(0 until index) + element) + oldList.slice(index until oldList.size)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val oldList = list.value
        list.value = (oldList.slice(0 until index) + elements) + oldList.slice(index until oldList.size)
        return true
    }

    override fun addAll(elements: Collection<T>): Boolean = true.also { list.value = list.value + elements }

    override fun clear() = run { list.value = emptyList() }
    override fun listIterator(): MutableListIterator<T> = list.value.listIterator().asFakeMutable()
    override fun listIterator(index: Int): MutableListIterator<T> = list.value.listIterator(index).asFakeMutable()
    override fun remove(element: T): Boolean {
        val oldList = list.value
        return oldList.contains(element).also { list.value = oldList - element }
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val oldList = list.value
        return oldList.containsAll(elements).also { list.value = oldList - elements }
    }

    override fun removeAt(index: Int): T = list.value[index].also {
        val oldList = list.value
        list.value = oldList.slice(0 until index) + oldList.slice(index + 1 until oldList.size)
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        val elementsSet = elements.toSet()
        list.value = list.value.filter { it in elementsSet }
        return true
    }

    override fun set(index: Int, element: T): T {
        val oldList = list.value
        return oldList[index].also { list.value = (oldList.slice(0 until index) + element) + oldList.slice(index until oldList.size) }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> = SubMutableList(this, fromIndex, toIndex)

}
