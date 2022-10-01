package com.soywiz.kmem.buffer

import com.soywiz.kmem.Float32Buffer
import com.soywiz.kmem.Float64Buffer
import com.soywiz.kmem.Int16Buffer
import com.soywiz.kmem.Int32Buffer
import com.soywiz.kmem.Int8Buffer
import com.soywiz.kmem.MemBuffer
import com.soywiz.kmem.Uint16Buffer
import com.soywiz.kmem.Uint8Buffer
import com.soywiz.kmem.size
import com.soywiz.kmem.sliceFloat32Buffer
import com.soywiz.kmem.sliceFloat64Buffer
import com.soywiz.kmem.sliceInt16Buffer
import com.soywiz.kmem.sliceInt32Buffer
import com.soywiz.kmem.sliceInt8Buffer
import com.soywiz.kmem.sliceUint16Buffer
import com.soywiz.kmem.sliceUint8Buffer

public fun Uint8Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 1): Uint8Buffer = mem.sliceUint8Buffer(offset, len)
public fun Uint16Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 2): Uint16Buffer = mem.sliceUint16Buffer(offset, len)
public fun Int8Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 1): Int8Buffer = mem.sliceInt8Buffer(offset, len)
public fun Int16Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 2): Int16Buffer = mem.sliceInt16Buffer(offset, len)
public fun Int32Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 4): Int32Buffer = mem.sliceInt32Buffer(offset, len)
public fun Float32Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 4): Float32Buffer = mem.sliceFloat32Buffer(offset, len)
public fun Float64Buffer(mem: MemBuffer, offset: Int = 0, len: Int = mem.size / 8): Float64Buffer = mem.sliceFloat64Buffer(offset, len)
