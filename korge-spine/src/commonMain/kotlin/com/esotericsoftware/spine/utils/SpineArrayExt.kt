package com.esotericsoftware.spine.utils

import com.esotericsoftware.spine.internal.*
import com.esotericsoftware.spine.internal.max2
import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

internal fun FloatArrayList.setSize(size: Int) = run { this.size = size }.let { this.data }
internal fun IntArrayList.setSize(size: Int) = run { this.size = size }.let { this.data }
internal fun ShortArrayList.setSize(size: Int) = run { this.size = size }.let { this }

internal fun FloatArrayList.toArray() = this.toFloatArray()

internal fun BooleanArrayList.setSize(size: Int) {
    this.size = size
}

internal fun IntArrayList.toArray() = this.toIntArray()
internal fun ShortArrayList.toArray(): ShortArray = ShortArray(size) { this[it] }

internal fun <T> ArrayList<T>.setAndGrow(index: Int, value: T) {
    if (index >= size) {
        val items = this as MutableList<Any?>
        while (items.size <= index) items.add(null)
    }
    this[index] = value
}

internal fun <T> ArrayList<T>.indexOfIdentity(value: T?): Int {
    fastForEachWithIndex { index, current -> if (current === value) return index }
    return -1
}
internal fun <T> ArrayList<T>.removeValueIdentity(value: T?): Boolean {
    val index = indexOfIdentity(value)
    val found = index >= 0
    if (found) removeAt(index)
    return found
}
internal fun <T> ArrayList<T>.containsIdentity(value: T?): Boolean = indexOfIdentity(value) >= 0

internal fun <T> ArrayList<T>.shrink() = run { if (size != size) resize(size) }
internal fun <T> ArrayList<T>.setSize(newSize: Int): ArrayList<T> {
    truncate(max2(8, newSize))
    return this
}
internal fun <T> ArrayList<T>.resize(newSize: Int) = run {
    truncate(newSize)
}

internal fun <T> ArrayList<T>.truncate(newSize: Int) {
    require(newSize >= 0) { "newSize must be >= 0: $newSize" }
    while (size > newSize) removeAt(size - 1)
}
