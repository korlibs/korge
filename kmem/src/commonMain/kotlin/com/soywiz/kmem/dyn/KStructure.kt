package com.soywiz.kmem.dyn

import kotlin.reflect.KProperty
import kotlin.math.*

expect class KArena() {
    fun allocBytes(size: Int): KPointer
    fun clear(): Unit
}

inline fun <T> kmemScoped(block: KArena.() -> T): T {
    val arena = KArena()
    try {
        return block(arena)
    } finally {
        arena.clear()
    }
}

expect val POINTER_SIZE: Int
expect val LONG_SIZE: Int
expect class KPointer

inline class KPointerT<T>(val pointer: KPointer)

operator fun KPointerT<Int>.get(offset: Int) = pointer.getInt(offset * 4)
operator fun KPointerT<Float>.get(offset: Int) = pointer.getFloat(offset * 4)
operator fun KPointerT<Long>.get(offset: Int) = pointer.getLong(offset * 8)
operator fun KPointerT<Double>.get(offset: Int) = pointer.getDouble(offset * 8)

operator fun KPointerT<Int>.set(offset: Int, value: Int) = pointer.setInt(offset * 4, value)
operator fun KPointerT<Float>.set(offset: Int, value: Float) = pointer.setFloat(offset * 4, value)
operator fun KPointerT<Long>.set(offset: Int, value: Long) = pointer.setLong(offset * 8, value)
operator fun KPointerT<Double>.set(offset: Int, value: Double) = pointer.setDouble(offset * 8, value)

//expect fun KPointerAlloc(size: Int): KPointer = TODO()
//expect fun KPointerFree(address: Long) = TODO()

expect fun KPointerCreate(address: Long): KPointer
expect val KPointer.address: Long

expect fun KPointer.getByte(offset: Int): Byte
expect fun KPointer.setByte(offset: Int, value: Byte)
expect fun KPointer.getInt(offset: Int): Int
expect fun KPointer.setInt(offset: Int, value: Int)
expect fun KPointer.getFloat(offset: Int): Float
expect fun KPointer.setFloat(offset: Int, value: Float)
expect fun KPointer.getDouble(offset: Int): Double
expect fun KPointer.setDouble(offset: Int, value: Double)
expect fun KPointer.getLong(offset: Int): Long
expect fun KPointer.setLong(offset: Int, value: Long)

fun KPointer.getPointer(offset: Int): KPointer {
    return if (POINTER_SIZE == 8) KPointerCreate(getLong(offset)) else KPointerCreate(getInt(offset).toLong())
}
fun KPointer.setPointer(offset: Int, value: KPointer) {
    if (POINTER_SIZE == 8) setLong(offset, value.address) else setInt(offset, value.address.toInt())
}
fun KPointer.getNativeLong(offset: Int): Long {
    return if (POINTER_SIZE == 8) getLong(offset) else getInt(offset).toLong()
}
fun KPointer.setNativeLong(offset: Int, value: Long) {
    return if (POINTER_SIZE == 8) setLong(offset, value) else setInt(offset, value.toInt())
}

expect abstract class KStructureBase() {
    abstract val pointer: KPointer?
}

open class KStructure(pointer: KPointer?) : KStructureBase() {
    override var pointer: KPointer? = pointer
    val pointerSure: KPointer get() = pointer!!

    val layout: KMemLayoutBuilder = KMemLayoutBuilder()

    val size: Int get() = layout.size
    fun byte(): KMemDelegateByteProperty = layout.byte()
    fun bool(): KMemDelegateBoolProperty = layout.bool()
    fun int(): KMemDelegateIntProperty = layout.int()
    fun nativeFloat(): KMemDelegateNativeDoubleProperty = layout.nativeFloat()
    fun nativeLong(): KMemDelegateNativeLongProperty = layout.nativeLong()
    fun kpointer(): KMemDelegateKPointerProperty = layout.kpointer()
    fun <T> pointer(): KMemDelegatePointerProperty<T> = layout.pointer<T>()

    //private var _pointer: KPointer? = pointer; private set
    //val pointer: KPointer by lazy {
    //    if (_pointer == null) _pointer = KMemory(size.toLong())
    //    _pointer!!
    //}
}

open class KMemLayoutBuilder {
    private var offset = 0
    private var maxAlign = 4

    private fun align(size: Int): KMemLayoutBuilder {
        maxAlign = max(maxAlign, size)
        while (this.offset % size != 0) this.offset++
        return this
    }

    val size: Int by lazy {
        align(maxAlign)
        offset
    }

    private fun alloc(size: Int) = align(size).offset.also { this.offset += size }

    //fun int() = alloc(Int.SIZE_BYTES)
    //fun nativeLong() = alloc(NativeLong.SIZE)

    fun byte(): KMemDelegateByteProperty = KMemDelegateByteProperty(alloc(Byte.SIZE_BYTES))
    fun bool(): KMemDelegateBoolProperty = KMemDelegateBoolProperty(alloc(Int.SIZE_BYTES))
    fun int() = KMemDelegateIntProperty(alloc(Int.SIZE_BYTES))
    fun long() = KMemDelegateLongProperty(alloc(Long.SIZE_BYTES))
    fun float() = KMemDelegateFloatProperty(alloc(4))
    fun double() = KMemDelegateFloatProperty(alloc(8))
    fun nativeFloat() = KMemDelegateNativeDoubleProperty(alloc(POINTER_SIZE))
    fun nativeLong() = KMemDelegateNativeLongProperty(alloc(LONG_SIZE))
    fun kpointer() = KMemDelegateKPointerProperty(alloc(POINTER_SIZE))
    fun <T> pointer() = KMemDelegatePointerProperty<T>(alloc(POINTER_SIZE))
}


inline class KMemDelegateByteProperty(val offset: Int) {
    operator fun getValue(obj: KStructure, property: KProperty<*>): Byte = obj.pointerSure.getByte(offset)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Byte) { obj.pointerSure.setByte(offset, i) }
}

inline class KMemDelegateBoolProperty(val offset: Int) {
    operator fun getValue(obj: KStructure, property: KProperty<*>): Boolean = obj.pointerSure.getInt(offset) != 0
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Boolean) { obj.pointerSure.setInt(offset, if (i) 1 else 0) }
}

inline class KMemDelegateIntProperty(val offset: Int) {
    fun get(pointer: KPointer): Int = pointer.getInt(offset)
    fun set(pointer: KPointer, value: Int) = pointer.setInt(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): Int = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Int): Unit = set(obj.pointerSure, i)
}

inline class KMemDelegateFloatProperty(val offset: Int) {
    fun get(pointer: KPointer): Float = pointer.getFloat(offset)
    fun set(pointer: KPointer, value: Float) = pointer.setFloat(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): Float = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Float): Unit = set(obj.pointerSure, i)
}

inline class KMemDelegateDoubleProperty(val offset: Int) {
    fun get(pointer: KPointer): Double = pointer.getDouble(offset)
    fun set(pointer: KPointer, value: Double) = pointer.setDouble(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): Double = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Double): Unit = set(obj.pointerSure, i)
}

inline class KMemDelegateLongProperty(val offset: Int) {
    fun get(pointer: KPointer): Long = pointer.getLong(offset)
    fun set(pointer: KPointer, value: Long) = pointer.setLong(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): Long = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Long): Unit = set(obj.pointerSure, i)
}

inline class KMemDelegateNativeDoubleProperty(val offset: Int) {
    fun get(pointer: KPointer): Double = when (POINTER_SIZE) {
        4 -> pointer.getFloat(offset).toDouble()
        else -> pointer.getDouble(offset)
    }
    fun set(pointer: KPointer, value: Double) {
        when (POINTER_SIZE) {
            4 -> pointer.setFloat(offset, value.toFloat())
            else -> pointer.setDouble(offset, value)
        }
    }
    operator fun getValue(obj: KStructure, property: KProperty<*>): Double = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Double): Unit = set(obj.pointerSure, i)
}

inline class KMemDelegateNativeLongProperty(val offset: Int) {
    fun get(pointer: KPointer): Long = pointer.getNativeLong(offset)
    fun set(pointer: KPointer, value: Long) = pointer.setNativeLong(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): Long = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Long): Unit = set(obj.pointerSure, i)
}

inline class KMemDelegateKPointerProperty(val offset: Int) {
    fun get(pointer: KPointer): KPointer = pointer.getPointer(offset)
    fun set(pointer: KPointer, value: KPointer) = pointer.setPointer(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): KPointer = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: KPointer): Unit { set(obj.pointerSure, i) }
}

inline class KMemDelegatePointerProperty<T>(val offset: Int) {
    fun get(pointer: KPointer): KPointerT<T> = KPointerT(pointer.getPointer(offset))
    fun set(pointer: KPointer, value: KPointerT<T>) = pointer.setPointer(offset, value.pointer)
    operator fun getValue(obj: KStructure, property: KProperty<*>): KPointerT<T> = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: KPointerT<T>): Unit { set(obj.pointerSure, i) }
}
