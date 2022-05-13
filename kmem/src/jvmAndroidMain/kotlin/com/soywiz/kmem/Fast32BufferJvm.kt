package com.soywiz.kmem

import java.nio.ByteBuffer

//private val unsafe: Unsafe? = try {
//    Unsafe::class.java.getDeclaredField("theUnsafe")
//        .also { it.isAccessible = true }
//        .get(null) as? Unsafe?
//} catch (e: Exception) {
//    throw AssertionError(e)
//}

public actual typealias Fast32Buffer = ByteBuffer

//actual abstract class Fast32Buffer(@JvmField val bb: ByteBuffer) {
    //val address = if (bb is DirectBuffer) (bb as DirectBuffer).address() else 0L
//}

public actual fun NewFast32Buffer(mem: MemBuffer): Fast32Buffer = mem.buffer
//actual typealias Fast32Buffer = ByteBuffer

public actual val Fast32Buffer.length: Int get() = this.limit() * 4
/*
actual fun Fast32Buffer.getF(index: Int): Float = this.bb.getFloat(index * 4)
actual fun Fast32Buffer.setF(index: Int, value: Float) { this.bb.putFloat(index * 4, value) }
actual fun Fast32Buffer.getI(index: Int): Int = this.bb.getInt(index * 4)
actual fun Fast32Buffer.setI(index: Int, value: Int) { this.bb.putInt(index * 4, value) }
*/
public actual inline fun Fast32Buffer.getF(index: Int): Float {
    //if (unsafe != null && address != 0L) {
    //    return unsafe.getFloat(address + index * 4)
    //} else {
        return getFloat(index * 4)
    //}
}
public actual inline fun Fast32Buffer.setF(index: Int, value: Float) {
    //if (unsafe != null && address != 0L) {
    //    unsafe.putFloat(address + index * 4, value)
    //} else {
        putFloat(index * 4, value)
    //}
}
public actual inline fun Fast32Buffer.getI(index: Int): Int {
    //if (unsafe != null && address != 0L) {
    //    return unsafe.getInt(address + index * 4)
    //} else {
        return getInt(index * 4)
    //}
}
public actual inline fun Fast32Buffer.setI(index: Int, value: Int) {
    //if (unsafe != null && address != 0L) {
    //    unsafe.putInt(address + index * 4, value)
    //} else {
        putInt(index * 4, value)
    //}
}
