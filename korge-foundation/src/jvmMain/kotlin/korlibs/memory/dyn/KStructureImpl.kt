package korlibs.memory.dyn

import com.sun.jna.*
import kotlin.math.*
import kotlin.reflect.*

actual class KArena actual constructor() {
    private val pointers = arrayListOf<Memory>()
    actual fun allocBytes(size: Int): KPointer = KPointer(Memory(size.toLong()).also {
        it.clear()
        pointers += it
    }.address)
    actual fun clear() {
        for (n in 0 until pointers.size) pointers[n].clear()
        pointers.clear()
    }
}

actual val POINTER_SIZE: Int = 8
actual val LONG_SIZE: Int = 8

abstract class KPointed

class KPointerTT<T : KPointed>(val optr: Pointer?, val ref: T? = null) {
    val ptr: Pointer get() = optr!!
}
fun <T : KPointed> KPointer(ptr: Pointer): KPointerTT<T> = KPointerTT<T>(ptr, null)
val Pointer.kpointer: KPointer get() = KPointer(this.address)
class KFunctionTT<T : Function<*>>(val func: T) : KPointed()
//actual typealias NativeLong = com.sun.jna.NativeLong

open class KStructure(var pointer: KPointer?) : NativeMapped {
    override fun nativeType(): Class<*> = Pointer::class.java
    override fun toNative(): Any? = this.pointer
    override fun fromNative(nativeValue: Any?, context: FromNativeContext?): Any = this::class.constructors.first().call(nativeValue)
    val pointerSure: KPointer get() = pointer!!

    val layout: KMemLayoutBuilder = KMemLayoutBuilder()

    val size: Int get() = layout.size
    fun bool(): KMemDelegateBoolProperty = layout.bool()
    fun byte(): KMemDelegateByteProperty = layout.byte()
    fun short(): KMemDelegateShortProperty = layout.short()
    fun int(): KMemDelegateIntProperty = layout.int()
    fun double(): KMemDelegateDoubleProperty = layout.double()
    fun long(): KMemDelegateLongProperty = layout.long()
    fun nativeFloat(): KMemDelegateNativeDoubleProperty = layout.nativeFloat()
    fun nativeLong(): KMemDelegateNativeLongProperty = layout.nativeLong()
    fun kpointer(): KMemDelegateKPointerProperty = layout.kpointer()
    fun <T> pointer(): KMemDelegatePointerProperty<T> = layout.pointer<T>()
    fun fixedBytes(size: Int): KMemDelegateFixedBytesProperty = layout.fixedBytes(size)

    //private var _pointer: KPointer? = pointer; private set
    //val pointer: KPointer by lazy {
    //    if (_pointer == null) _pointer = KMemory(size.toLong())
    //    _pointer!!
    //}
}


open class KMemLayoutBuilder {
    var offset = 0
        internal set
    var maxAlign = 4
        internal set

    private fun align(size: Int): KMemLayoutBuilder {
        maxAlign = max(maxAlign, size)
        while (this.offset % size != 0) this.offset++
        return this
    }

    val size: Int by lazy {
        align(maxAlign)
        offset
    }

    fun rawAlloc(size: Int, align: Int = size): Int = align(align).offset.also { this.offset += size }

    //fun int() = alloc(Int.SIZE_BYTES)
    //fun nativeLong() = alloc(NativeLong.SIZE)

    fun bool(): KMemDelegateBoolProperty = KMemDelegateBoolProperty(rawAlloc(Int.SIZE_BYTES))
    fun byte(): KMemDelegateByteProperty = KMemDelegateByteProperty(rawAlloc(Byte.SIZE_BYTES))
    fun short(): KMemDelegateShortProperty = KMemDelegateShortProperty(rawAlloc(Short.SIZE_BYTES))
    fun int() = KMemDelegateIntProperty(rawAlloc(Int.SIZE_BYTES))
    fun long() = KMemDelegateLongProperty(rawAlloc(Long.SIZE_BYTES))
    fun float() = KMemDelegateFloatProperty(rawAlloc(4))
    fun double() = KMemDelegateDoubleProperty(rawAlloc(8))
    fun nativeFloat() = KMemDelegateNativeDoubleProperty(rawAlloc(POINTER_SIZE))
    fun nativeLong() = KMemDelegateNativeLongProperty(rawAlloc(LONG_SIZE))
    fun kpointer() = KMemDelegateKPointerProperty(rawAlloc(POINTER_SIZE))
    fun <T> pointer() = KMemDelegatePointerProperty<T>(rawAlloc(POINTER_SIZE))
    fun fixedBytes(size: Int, align: Int = 1): KMemDelegateFixedBytesProperty = KMemDelegateFixedBytesProperty(rawAlloc(size * Byte.SIZE_BYTES, align), size)
}


inline class KMemDelegateByteProperty(val offset: Int) {
    operator fun getValue(obj: KStructure, property: KProperty<*>): Byte = obj.pointerSure.getByte(offset)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Byte) { obj.pointerSure.setByte(offset, i) }
}

class KMemDelegateFixedBytesProperty(val offset: Int, val size: Int) {
    val bytes = ByteArray(size)
    operator fun getValue(obj: KStructure, property: KProperty<*>): ByteArray {
        for (n in 0 until size) bytes[n] = obj.pointerSure.getByte(offset + n)
        return bytes
    }
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: ByteArray) {
        for (n in 0 until size) obj.pointerSure.setByte(offset + n, i[n])
    }
}


inline class KMemDelegateBoolProperty(val offset: Int) {
    operator fun getValue(obj: KStructure, property: KProperty<*>): Boolean = obj.pointerSure.getInt(offset) != 0
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Boolean) { obj.pointerSure.setInt(offset, if (i) 1 else 0) }
}

inline class KMemDelegateShortProperty(val offset: Int) {
    fun get(pointer: KPointer): Short = pointer.getShort(offset)
    fun set(pointer: KPointer, value: Short) = pointer.setShort(offset, value)
    operator fun getValue(obj: KStructure, property: KProperty<*>): Short = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: Short): Unit = set(obj.pointerSure, i)
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
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: KPointer) { set(obj.pointerSure, i) }
}

inline class KMemDelegatePointerProperty<T>(val offset: Int) {
    fun get(pointer: KPointer): KPointerT<T> = KPointerT(pointer.getPointer(offset))
    fun set(pointer: KPointer, value: KPointerT<T>) = pointer.setPointer(offset, value.pointer)
    operator fun getValue(obj: KStructure, property: KProperty<*>): KPointerT<T> = get(obj.pointerSure)
    operator fun setValue(obj: KStructure, property: KProperty<*>, i: KPointerT<T>) { set(obj.pointerSure, i) }
}


val KPointer.ptr: Pointer get() = Pointer(address)
//actual fun KPointer(address: Long): KPointer = KPointer(Pointer(address))
//actual val KPointer.address: Long get() = Pointer.nativeValue(this.ptr)

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
