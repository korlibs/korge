package com.esotericsoftware.spine.utils

import com.soywiz.kds.*

val FloatArrayList.items get() = this.data
val IntArrayList.items get() = this.data
//val ShortArrayList.items get() = this.data

fun FloatArrayList.setSize(size: Int) = run { this.size = size }.let { this.data }
fun IntArrayList.setSize(size: Int) = run { this.size = size }.let { this.data }
fun ShortArrayList.setSize(size: Int) = run { this.size = size }.let { this }

fun FloatArrayList.toArray() = this.toFloatArray()

fun FloatArrayList.addAll(array: FloatArray, offset: Int = 0, size: Int = array.size - offset) { add(array, offset, size) }
fun FloatArrayList.addAll(array: FloatArrayList, offset: Int = 0, size: Int = array.size - offset) { add(array.data, offset, size) }
fun BooleanArrayList.setSize(size: Int) {
    this.size = size
}


//////////////////

fun IntArrayList.toArray() = this.toIntArray()
fun ShortArrayList.toArray(): ShortArray = ShortArray(size) { this[it] }
