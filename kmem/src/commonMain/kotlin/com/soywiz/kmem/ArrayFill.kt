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
