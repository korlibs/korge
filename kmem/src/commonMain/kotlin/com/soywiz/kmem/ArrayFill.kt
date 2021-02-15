package com.soywiz.kmem

/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun <T> arrayfill(array: Array<T>, value: T, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: BooleanArray, value: Boolean, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: LongArray, value: Long, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: IntArray, value: Int, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size): Unit
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
expect fun arrayfill(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size): Unit

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
