package com.soywiz.kmem

public fun arrayadd(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = (array[n] + value).toByte() }
public fun arrayadd(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = (array[n] + value).toShort() }
public fun arrayadd(array: IntArray, value: Int, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: LongArray, value: Long, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }

public fun arrayadd(array: Int8Buffer, value: Byte, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = (array[n] + value).toByte() }
public fun arrayadd(array: Int16Buffer, value: Short, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = (array[n] + value).toShort() }
public fun arrayadd(array: Int32Buffer, value: Int, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: Float32Buffer, value: Float, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: Float64Buffer, value: Double, start: Int = 0, end: Int = array.size): Unit { for (n in start until end) array[n] = array[n] + value }
