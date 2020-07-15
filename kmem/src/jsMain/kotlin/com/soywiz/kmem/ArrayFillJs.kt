package com.soywiz.kmem

actual fun <T> _arrayfill(array: Array<T>, value: T, start: Int, end: Int): Unit = run { for (n in start until end) array[n] = value }
actual fun  _arrayfill(array: BooleanArray, value: Boolean, start: Int, end: Int): Unit = run { for (n in start until end) array[n] = value }
actual fun  _arrayfill(array: LongArray, value: Long, start: Int, end: Int): Unit = run { for (n in start until end) array[n] = value }
actual inline fun _arrayfill(array: ByteArray, value: Byte, start: Int, end: Int): Unit = run { array.asDynamic().fill(value, start, end) }
actual inline fun _arrayfill(array: ShortArray, value: Short, start: Int, end: Int): Unit = run { array.asDynamic().fill(value, start, end) }
actual inline fun _arrayfill(array: IntArray, value: Int, start: Int, end: Int): Unit = run { array.asDynamic().fill(value, start, end) }
actual inline fun _arrayfill(array: FloatArray, value: Float, start: Int, end: Int): Unit = run { array.asDynamic().fill(value, start, end) }
actual inline fun _arrayfill(array: DoubleArray, value: Double, start: Int, end: Int): Unit = run { array.asDynamic().fill(value, start, end) }
