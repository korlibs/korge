package com.soywiz.kmem.dyn

actual class KArena actual constructor() {
    actual fun allocBytes(size: Int): KPointer = KPointer(ByteArray(size))
    actual fun clear(): Unit = Unit
}

actual val POINTER_SIZE: Int = 4
actual val LONG_SIZE: Int = 8

actual abstract class KPointed
actual class KPointerTT<T : KPointed>(val ptr: ByteArray)
fun <T : KPointed> KPointer(ptr: ByteArray): KPointerTT<T> = KPointerTT<T>(ptr)
actual class KFunctionTT<T : Function<*>>(val func: T) : KPointed()

actual abstract class KStructureBase {
    actual abstract val pointer: KPointer?
}
actual fun KPointer(address: Long): KPointer = TODO()
actual val KPointer.address: Long get() = TODO()

actual fun KPointer.getByte(offset: Int): Byte = TODO()
actual fun KPointer.setByte(offset: Int, value: Byte): Unit = TODO()
actual fun KPointer.getInt(offset: Int): Int = TODO()
actual fun KPointer.setInt(offset: Int, value: Int): Unit = TODO()
actual fun KPointer.getFloat(offset: Int): Float = TODO()
actual fun KPointer.setFloat(offset: Int, value: Float): Unit = TODO()
actual fun KPointer.getDouble(offset: Int): Double = TODO()
actual fun KPointer.setDouble(offset: Int, value: Double): Unit = TODO()
actual fun KPointer.getLong(offset: Int): Long = TODO()
actual fun KPointer.setLong(offset: Int, value: Long): Unit = TODO()
