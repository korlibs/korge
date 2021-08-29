package com.esotericsoftware.spine.utils

import com.soywiz.kds.*
import kotlin.math.*

internal fun FloatArrayList.setSize(size: Int): FloatArray { this.size = size; return this.data }
internal fun IntArrayList.setSize(size: Int): IntArray { this.size = size; return this.data }
internal fun ShortArrayList.setSize(size: Int): ShortArrayList { this.size = size; return this}

internal fun FloatArrayList.toArray() = this.toFloatArray()

internal fun BooleanArrayList.setSize(size: Int) {
    this.size = size
}

internal fun IntArrayList.toArray() = this.toIntArray()
internal fun ShortArrayList.toArray(): ShortArray = ShortArray(size) { this[it] }

internal fun <T> FastArrayList<T>.setAndGrow(index: Int, value: T) {
    if (index >= size) {
        val items = this as MutableList<Any?>
        while (items.size <= index) items.add(null)
    }
    this[index] = value
}

internal fun <T> FastArrayList<T>.indexOfIdentity(value: T?): Int {
    fastForEachWithIndex { index, current -> if (current === value) return index }
    return -1
}
internal fun <T> FastArrayList<T>.removeValueIdentity(value: T?): Boolean {
    val index = indexOfIdentity(value)
    val found = index >= 0
    if (found) removeAt(index)
    return found
}
internal fun <T> FastArrayList<T>.containsIdentity(value: T?): Boolean = indexOfIdentity(value) >= 0

internal fun <T> FastArrayList<T>.shrink() { if (size != size) resize(size) }
internal fun <T> FastArrayList<T>.setSize(newSize: Int): FastArrayList<T> {
    truncate(max(8, newSize))
    return this
}
internal fun <T> FastArrayList<T>.resize(newSize: Int) { truncate(newSize) }

internal fun <T> FastArrayList<T>.truncate(newSize: Int) {
    require(newSize >= 0) { "newSize must be >= 0: $newSize" }
    while (size > newSize) removeAt(size - 1)
}
