package korlibs.memory.dyn

import kotlin.math.max
import kotlin.reflect.KProperty

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

typealias KNativeLong = KPointer
typealias KPointer = KPointerTT<out KPointed>
//expect class KPointer
expect abstract class KPointed
expect class KPointerTT<T : KPointed>
expect class KFunctionTT<T : Function<*>> : KPointed
//expect class NativeLong

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

expect fun KPointer(address: Long): KPointer
expect val KPointer.address: Long

fun Long.toKPointer(): KPointer = KPointer(this)

expect fun KPointer.getByte(offset: Int): Byte
expect fun KPointer.setByte(offset: Int, value: Byte)
expect fun KPointer.getShort(offset: Int): Short
expect fun KPointer.setShort(offset: Int, value: Short)
expect fun KPointer.getInt(offset: Int): Int
expect fun KPointer.setInt(offset: Int, value: Int)
expect fun KPointer.getFloat(offset: Int): Float
expect fun KPointer.setFloat(offset: Int, value: Float)
expect fun KPointer.getDouble(offset: Int): Double
expect fun KPointer.setDouble(offset: Int, value: Double)
expect fun KPointer.getLong(offset: Int): Long
expect fun KPointer.setLong(offset: Int, value: Long)

fun KPointer.getPointer(offset: Int): KPointer {
    return if (POINTER_SIZE == 8) KPointer(getLong(offset)) else KPointer(getInt(offset).toLong())
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