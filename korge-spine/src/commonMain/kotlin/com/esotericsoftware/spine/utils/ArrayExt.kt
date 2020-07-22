package com.esotericsoftware.spine.utils

import com.soywiz.kds.*

val FloatArrayList.items get() = this.data
fun FloatArrayList.setSize(size: Int): FloatArray {
    this.size = size
    return this.data
}
fun FloatArrayList.toArray() = this.toFloatArray()
fun FloatArrayList.addAll(array: FloatArray, offset: Int = 0, size: Int = array.size - offset) { add(array, offset, size) }
fun FloatArrayList.addAll(array: FloatArrayList, offset: Int = 0, size: Int = array.size - offset) { add(array.data, offset, size) }
