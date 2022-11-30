package com.soywiz.kmem

public fun arrayadd(array: ByteArray, value: Byte, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toByte() }
public fun arrayadd(array: ShortArray, value: Short, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toShort() }
public fun arrayadd(array: IntArray, value: Int, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: LongArray, value: Long, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: FloatArray, value: Float, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: DoubleArray, value: Double, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }

public fun arrayadd(array: NBufferUInt8, value: Byte, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value) }
public fun arrayadd(array: NBufferUInt16, value: Short, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value) }
public fun arrayadd(array: NBufferInt8, value: Byte, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toByte() }
public fun arrayadd(array: NBufferInt16, value: Short, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = (array[n] + value).toShort() }
public fun arrayadd(array: NBufferInt32, value: Int, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: NBufferFloat32, value: Float, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
public fun arrayadd(array: NBufferFloat64, value: Double, start: Int = 0, end: Int = array.size) { for (n in start until end) array[n] = array[n] + value }
