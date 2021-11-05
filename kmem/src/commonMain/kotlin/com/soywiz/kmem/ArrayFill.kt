package com.soywiz.kmem

import kotlin.collections.fill as stdFill

/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun <T> arrayfill(array: Array<T>, value: T, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: BooleanArray, value: Boolean, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: LongArray, value: Long, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: IntArray, value: Int, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)
/** Fills the [array] with the [value] starting a [start] end ending at [end] (end is not inclusive) */
public fun arrayfill(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size): Unit = array.stdFill(value, start, end)

/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun <T> Array<T>.fill(value: T, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun BooleanArray.fill(value: Boolean, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun LongArray.fill(value: Long, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun ByteArray.fill(value: Byte, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun ShortArray.fill(value: Short, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun IntArray.fill(value: Int, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun FloatArray.fill(value: Float, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
/** Fills [this] array with the [value] starting a [start] end ending at [end] (end is not inclusive) */
@Deprecated("Use Array.fill from stdlib")
public inline fun DoubleArray.fill(value: Double, start: Int = 0, end: Int = this.size): Unit = this.stdFill(value, start, end)
