package com.soywiz.kmem.dyn

import kotlinx.cinterop.*
import platform.posix.*

actual class KArena actual constructor() {
    private val arena = Arena()
    actual fun allocBytes(size: Int): KPointer {
        return arena.allocArray<ByteVar>(size).also {
            memset(it, 0, size.convert())
        }
    }
    actual fun clear(): Unit = arena.clear()
}

actual val POINTER_SIZE: Int = sizeOf<COpaquePointerVar>().toInt()
actual val LONG_SIZE: Int = 8
//actual typealias KPointer = kotlin.native.internal.NativePtr
actual typealias KPointed = CPointed
actual typealias KPointerTT<T> = CPointer<T>
actual typealias KFunctionTT<T> = CFunction<T>
//@kotlinx.cinterop.UnsafeNumber actual typealias NativeLong = size_t

actual abstract class KStructureBase {
    actual abstract val pointer: KPointer?
}
actual fun KPointer(address: Long): KPointer = address.toCPointer<ByteVar>() as KPointer
actual val KPointer.address: Long get() = this.toLong()
inline fun <T : CPointed> KPointer?.toCPointer(): CPointer<T>? = interpretCPointer(this.rawValue)

private inline fun <T : CPointed> KPointer.offset(offset: Int): CPointer<T> = interpretCPointer<T>(this.rawValue + offset.toLong())!!

actual fun KPointer.getByte(offset: Int): Byte = offset<ByteVar>(offset)[0]
actual fun KPointer.setByte(offset: Int, value: Byte) { offset<ByteVar>(offset)[0] = value }
actual fun KPointer.getShort(offset: Int): Short = offset<ShortVar>(offset)[0]
actual fun KPointer.setShort(offset: Int, value: Short) { offset<ShortVar>(offset)[0] = value }
actual fun KPointer.getInt(offset: Int): Int = offset<IntVar>(offset)[0]
actual fun KPointer.setInt(offset: Int, value: Int) { offset<IntVar>(offset)[0] = value }
actual fun KPointer.getFloat(offset: Int): Float = offset<FloatVar>(offset)[0]
actual fun KPointer.setFloat(offset: Int, value: Float) { offset<FloatVar>(offset)[0] = value }
actual fun KPointer.getDouble(offset: Int): Double = offset<DoubleVar>(offset)[0]
actual fun KPointer.setDouble(offset: Int, value: Double) { offset<DoubleVar>(offset)[0] = value }
actual fun KPointer.getLong(offset: Int): Long = offset<LongVar>(offset)[0]
actual fun KPointer.setLong(offset: Int, value: Long) { offset<LongVar>(offset)[0] = value }
