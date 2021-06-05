package com.soywiz.kds

import com.soywiz.kds.iterators.*

// @TODO: ArrayList that prevents isObject + jsInstanceOf on getter on Kotlin/JS
// @TODO: This class should be temporal until Kotlin/JS fixes this issue
expect class FastArrayList<E> : MutableList<E>, RandomAccess {
    constructor()
    constructor(initialCapacity: Int)
    constructor(elements: Collection<E>)

    fun trimToSize()
    fun ensureCapacity(minCapacity: Int)

    // From List

    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: @UnsafeVariance E): Boolean
    override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
    override operator fun get(index: Int): E
    override fun indexOf(element: @UnsafeVariance E): Int
    override fun lastIndexOf(element: @UnsafeVariance E): Int

    // From MutableCollection

    override fun iterator(): MutableIterator<E>

    // From MutableList

    override fun add(element: E): Boolean
    override fun remove(element: E): Boolean
    override fun addAll(elements: Collection<E>): Boolean
    override fun addAll(index: Int, elements: Collection<E>): Boolean
    override fun removeAll(elements: Collection<E>): Boolean
    override fun retainAll(elements: Collection<E>): Boolean
    override fun clear()
    override operator fun set(index: Int, element: E): E
    override fun add(index: Int, element: E)
    override fun removeAt(index: Int): E
    override fun listIterator(): MutableListIterator<E>
    override fun listIterator(index: Int): MutableListIterator<E>
    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E>

    inline fun fastForEach(callback: (E) -> Unit)
    inline fun fastForEachWithIndex(callback: (index: Int, value: E) -> Unit)
    inline fun fastForEachReverse(callback: (E) -> Unit)
    inline fun fastForEachReverseWithIndex(callback: (index: Int, value: E) -> Unit)
}

fun <T> fastArrayListOf(vararg values: T): FastArrayList<T> = FastArrayList<T>(values.size).also { it.addAll(values) }

fun <T> List<T>.toFastList(): List<T> = if (this is FastArrayList) this else FastArrayList<T>(this.size).also { out -> fastForEach { out.add(it) } }
fun <T> Array<T>.toFastList(): List<T> = FastArrayList<T>(this.size).also { out -> fastForEach { out.add(it) } }
