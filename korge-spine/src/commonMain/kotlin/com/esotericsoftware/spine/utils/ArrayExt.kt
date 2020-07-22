package com.esotericsoftware.spine.utils

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

fun FloatArrayList.setSize(size: Int) = run { this.size = size }.let { this.data }
fun IntArrayList.setSize(size: Int) = run { this.size = size }.let { this.data }
fun ShortArrayList.setSize(size: Int) = run { this.size = size }.let { this }

fun FloatArrayList.toArray() = this.toFloatArray()

fun BooleanArrayList.setSize(size: Int) {
    this.size = size
}

fun IntArrayList.toArray() = this.toIntArray()
fun ShortArrayList.toArray(): ShortArray = ShortArray(size) { this[it] }

fun <T> ArrayList<T>.setAndGrow(index: Int, value: T) {
    if (index >= size) {
        val items = this as MutableList<Any?>
        while (items.size <= index) items.add(null)
    }
    this[index] = value
}

fun <T> ArrayList<T>.indexOfIdentity(value: T?): Int {
    fastForEachWithIndex { index, current -> if (current === value) return index }
    return -1
}
fun <T> ArrayList<T>.removeValueIdentity(value: T?): Boolean {
    val index = indexOfIdentity(value)
    val found = index >= 0
    if (found) removeAt(index)
    return found
}
fun <T> ArrayList<T>.containsIdentity(value: T?): Boolean = indexOfIdentity(value) >= 0

fun <T> ArrayList<T>.shrink() = run { if (size != size) resize(size) }
fun <T> ArrayList<T>.setSize(newSize: Int): ArrayList<T> {
    truncate(kotlin.math.max(8, newSize))
    return this
}
fun <T> ArrayList<T>.resize(newSize: Int) = run {
    truncate(newSize)
}

fun <T> ArrayList<T>.truncate(newSize: Int) {
    require(newSize >= 0) { "newSize must be >= 0: $newSize" }
    while (size > newSize) removeAt(size - 1)
}
