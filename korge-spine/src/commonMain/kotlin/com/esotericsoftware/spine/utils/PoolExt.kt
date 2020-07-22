package com.esotericsoftware.spine.utils

import com.soywiz.kds.*

fun <T> Pool<T>.obtain() = alloc()

fun <T> Pool<T>.freeAll(array: JArray<T>) {
    array.fastForEach { this.free(it) }
}
