package com.soywiz.kmem.dyn

import kotlinx.cinterop.*

actual class KArena actual constructor() {
    private val arena = Arena()
    actual fun allocBytes(size: Int): KPointer = arena.allocArray<ByteVar>(size).rawValue
    actual fun clear(): Unit = arena.clear()
}

actual val POINTER_SIZE: Int = sizeOf<COpaquePointerVar>().toInt()
actual val LONG_SIZE: Int = 8
actual typealias KPointer = kotlin.native.internal.NativePtr
actual abstract class KStructureBase {
    actual abstract val pointer: KPointer?
}
actual fun KPointerCreate(address: Long): KPointer = address.toCPointer<ByteVar>().rawValue
actual val KPointer.address: Long get() = this.toLong()

private inline fun <T : CPointed> KPointer.offset(offset: Int) = interpretCPointer<T>(this + offset.toLong())!!

actual fun KPointer.getByte(offset: Int): Byte = offset<ByteVar>(offset)[0]
actual fun KPointer.setByte(offset: Int, value: Byte) { offset<ByteVar>(offset)[0] = value }
actual fun KPointer.getInt(offset: Int): Int = offset<IntVar>(offset)[0]
actual fun KPointer.setInt(offset: Int, value: Int) { offset<IntVar>(offset)[0] = value }
actual fun KPointer.getFloat(offset: Int): Float = offset<FloatVar>(offset)[0]
actual fun KPointer.setFloat(offset: Int, value: Float) { offset<FloatVar>(offset)[0] = value }
actual fun KPointer.getDouble(offset: Int): Double = offset<DoubleVar>(offset)[0]
actual fun KPointer.setDouble(offset: Int, value: Double) { offset<DoubleVar>(offset)[0] = value }
actual fun KPointer.getLong(offset: Int): Long = offset<LongVar>(offset)[0]
actual fun KPointer.setLong(offset: Int, value: Long) { offset<LongVar>(offset)[0] = value }
