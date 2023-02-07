package com.soywiz.kmem.dyn

import com.sun.jna.FromNativeContext
import com.sun.jna.Memory
import com.sun.jna.NativeMapped
import com.sun.jna.Pointer

actual class KArena actual constructor() {
    private val pointers = arrayListOf<Memory>()
    actual fun allocBytes(size: Int): KPointer = KPointer(Memory(size.toLong()).also { pointers += it })
    actual fun clear() {
        for (n in 0 until pointers.size) pointers[n].clear()
        pointers.clear()
    }
}

actual val POINTER_SIZE: Int = 8
actual val LONG_SIZE: Int = 8

actual abstract class KPointed
actual class KPointerTT<T : KPointed>(val optr: Pointer?, val ref: T? = null) {
    val ptr: Pointer get() = optr!!
}
fun <T : KPointed> KPointer(ptr: Pointer): KPointerTT<T> = KPointerTT<T>(ptr, null)
val Pointer.kpointer: KPointer get() = KPointer(this)
actual class KFunctionTT<T : Function<*>>(val func: T) : KPointed()
//actual typealias NativeLong = com.sun.jna.NativeLong

//actual typealias KPointer = Pointer
actual abstract class KStructureBase : NativeMapped {
    actual abstract val pointer: KPointer?
    override fun nativeType(): Class<*> = Pointer::class.java
    override fun toNative(): Any? = this.pointer
    override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any = this::class.constructors.first().call(nativeValue)
}
actual fun KPointer(address: Long): KPointer = KPointer(Pointer(address))
actual val KPointer.address: Long get() = Pointer.nativeValue(this.ptr)

val Pointer.address: Long get() = Pointer.nativeValue(this)

actual fun KPointer.getByte(offset: Int): Byte = this.ptr.getByte(offset.toLong())
actual fun KPointer.setByte(offset: Int, value: Byte): Unit = this.ptr.setByte(offset.toLong(), value)
actual fun KPointer.getShort(offset: Int): Short = this.ptr.getShort(offset.toLong())
actual fun KPointer.setShort(offset: Int, value: Short): Unit = this.ptr.setShort(offset.toLong(), value)
actual fun KPointer.getInt(offset: Int): Int = this.ptr.getInt(offset.toLong())
actual fun KPointer.setInt(offset: Int, value: Int): Unit = this.ptr.setInt(offset.toLong(), value)
actual fun KPointer.getFloat(offset: Int): Float = this.ptr.getFloat(offset.toLong())
actual fun KPointer.setFloat(offset: Int, value: Float): Unit = this.ptr.setFloat(offset.toLong(), value)
actual fun KPointer.getDouble(offset: Int): Double = this.ptr.getDouble(offset.toLong())
actual fun KPointer.setDouble(offset: Int, value: Double): Unit = this.ptr.setDouble(offset.toLong(), value)
actual fun KPointer.getLong(offset: Int): Long = this.ptr.getLong(offset.toLong())
actual fun KPointer.setLong(offset: Int, value: Long): Unit = this.ptr.setLong(offset.toLong(), value)
