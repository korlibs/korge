package com.soywiz.kmem

import java.util.*

actual fun <T> arrayfill(array: Array<T>, value: T, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: BooleanArray, value: Boolean, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: LongArray, value: Long, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: ByteArray, value: Byte, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: ShortArray, value: Short, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: IntArray, value: Int, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: FloatArray, value: Float, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
actual fun arrayfill(array: DoubleArray, value: Double, start: Int, end: Int): Unit = Arrays.fill(array, start, end, value)
