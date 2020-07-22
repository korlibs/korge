package com.esotericsoftware.spine.utils

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*

fun <T> Pool<T>.obtain() = alloc()

fun <T> Pool<T>.freeAll(array: ArrayList<T>) {
    array.fastForEach { this.free(it) }
}
