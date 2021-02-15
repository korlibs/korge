package com.soywiz.kmem

import java.nio.*

actual /*inline*/ class Fast32Buffer(val bb: ByteBuffer)

actual fun NewFast32Buffer(mem: MemBuffer): Fast32Buffer = Fast32Buffer(mem.buffer)
//actual typealias Fast32Buffer = ByteBuffer

actual val Fast32Buffer.length: Int get() = this.bb.limit() * 4
actual fun Fast32Buffer.getF(index: Int): Float = this.bb.getFloat(index * 4)
actual fun Fast32Buffer.setF(index: Int, value: Float) { this.bb.putFloat(index * 4, value) }
actual fun Fast32Buffer.getI(index: Int): Int = this.bb.getInt(index * 4)
actual fun Fast32Buffer.setI(index: Int, value: Int) { this.bb.putInt(index * 4, value) }
