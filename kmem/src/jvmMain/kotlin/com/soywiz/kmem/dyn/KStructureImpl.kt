package com.soywiz.kmem.dyn

import com.sun.jna.*

actual class KArena actual constructor() {
    private val pointers = arrayListOf<Memory>()
    actual fun allocBytes(size: Int): KPointer = Memory(size.toLong()).also { pointers += it }
    actual fun clear() {
        for (n in 0 until pointers.size) pointers[n].clear()
        pointers.clear()
    }
}

actual val POINTER_SIZE: Int = 8
actual val LONG_SIZE: Int = 8
actual typealias KPointer = Pointer
actual abstract class KStructureBase : NativeMapped {
    actual abstract val pointer: Pointer?
    override fun nativeType(): Class<*> = Pointer::class.java
    override fun toNative(): Any? = this.pointer
    override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any = this::class.constructors.first().call(nativeValue)
}
actual fun KPointerCreate(address: Long): KPointer = Pointer(address)
actual val KPointer.address: Long get() = Pointer.nativeValue(this)

actual fun KPointer.getByte(offset: Int): Byte = this.getByte(offset.toLong())
actual fun KPointer.setByte(offset: Int, value: Byte): Unit = this.setByte(offset.toLong(), value)
actual fun KPointer.getInt(offset: Int): Int = this.getInt(offset.toLong())
actual fun KPointer.setInt(offset: Int, value: Int): Unit = this.setInt(offset.toLong(), value)
actual fun KPointer.getFloat(offset: Int): Float = this.getFloat(offset.toLong())
actual fun KPointer.setFloat(offset: Int, value: Float): Unit = this.setFloat(offset.toLong(), value)
actual fun KPointer.getDouble(offset: Int): Double = this.getDouble(offset.toLong())
actual fun KPointer.setDouble(offset: Int, value: Double): Unit = this.setDouble(offset.toLong(), value)
actual fun KPointer.getLong(offset: Int): Long = this.getLong(offset.toLong())
actual fun KPointer.setLong(offset: Int, value: Long): Unit = this.setLong(offset.toLong(), value)
