package com.soywiz.kmem

internal expect fun <T> _arrayfill(array: Array<T>, value: T, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: BooleanArray, value: Boolean, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: LongArray, value: Long, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: ByteArray, value: Byte, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: ShortArray, value: Short, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: IntArray, value: Int, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: FloatArray, value: Float, start: Int, end: Int): Unit
internal expect fun _arrayfill(array: DoubleArray, value: Double, start: Int, end: Int): Unit

/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun <T> arrayfill(array: Array<T>, value: T, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: BooleanArray, value: Boolean, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: LongArray, value: Long, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: IntArray, value: Int, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
fun arrayfill(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size): Unit = _arrayfill(array, value, start, end)

/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun <T> Array<T>.fill(value: T, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun BooleanArray.fill(value: Boolean, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun LongArray.fill(value: Long, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun ByteArray.fill(value: Byte, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun ShortArray.fill(value: Short, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun IntArray.fill(value: Int, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun FloatArray.fill(value: Float, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
inline fun DoubleArray.fill(value: Double, start: Int = 0, end: Int = this.size): Unit = arrayfill(this, value, start, end)
