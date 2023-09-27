package korlibs.ffi

import kotlin.math.*
import kotlin.reflect.*

open class FFIStructure(var ptr: FFIPointer?) {
    val ptrSure: FFIPointer get() = ptr!!

    val layout: FFIMemLayoutBuilder = FFIMemLayoutBuilder()

    val size: Int get() = layout.size
    fun bool(): FFIDelegateBoolProperty = layout.bool()
    fun byte(): FFIDelegateByteProperty = layout.byte()
    fun short(): FFIDelegateShortProperty = layout.short()
    fun int(): FFIDelegateIntProperty = layout.int()
    fun double(): FFIDelegateDoubleProperty = layout.double()
    fun long(): FFIDelegateLongProperty = layout.long()
    fun nativeFloat(): FFIDelegateNativeDoubleProperty = layout.nativeFloat()
    fun nativeLong(): FFIDelegateNativeLongProperty = layout.nativeLong()
    fun pointer(): FFIDelegateFFIPointerProperty = layout.kpointer()
    fun <T> pointer(): FFIDelegateFFIPointerPropertyT<T> = layout.kpointerT()

    fun fixedBytes(size: Int): FFIDelegateFixedBytesProperty = layout.fixedBytes(size)
}

open class FFIMemLayoutBuilder {
    var offset = 0
        internal set
    var maxAlign = 4
        internal set

    private fun align(size: Int): FFIMemLayoutBuilder {
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

    fun bool(): FFIDelegateBoolProperty = FFIDelegateBoolProperty(rawAlloc(Int.SIZE_BYTES))
    fun byte(): FFIDelegateByteProperty = FFIDelegateByteProperty(rawAlloc(Byte.SIZE_BYTES))
    fun short(): FFIDelegateShortProperty = FFIDelegateShortProperty(rawAlloc(Short.SIZE_BYTES))
    fun int() = FFIDelegateIntProperty(rawAlloc(Int.SIZE_BYTES))
    fun long() = FFIDelegateLongProperty(rawAlloc(Long.SIZE_BYTES))
    fun float() = FFIDelegateFloatProperty(rawAlloc(4))
    fun double() = FFIDelegateDoubleProperty(rawAlloc(8))
    fun nativeFloat() = FFIDelegateNativeDoubleProperty(rawAlloc(FFI_POINTER_SIZE))
    fun nativeLong() = FFIDelegateNativeLongProperty(rawAlloc(FFI_NATIVE_LONG_SIZE))
    fun kpointer() = FFIDelegateFFIPointerProperty(rawAlloc(FFI_POINTER_SIZE))
    fun <T> kpointerT() = FFIDelegateFFIPointerPropertyT<T>(rawAlloc(FFI_POINTER_SIZE))
    fun fixedBytes(size: Int, align: Int = 1): FFIDelegateFixedBytesProperty = FFIDelegateFixedBytesProperty(rawAlloc(size * Byte.SIZE_BYTES, align), size)
}


inline class FFIDelegateByteProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Byte = obj.ptrSure.getS8(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Byte) { obj.ptrSure.set8(i, offset) }
}

class FFIDelegateFixedBytesProperty(val offset: Int, val size: Int) {
    val bytes = ByteArray(size)
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): ByteArray {
        for (n in 0 until size) bytes[n] = obj.ptrSure.getS8(offset + n)
        return bytes
    }
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: ByteArray) {
        for (n in 0 until size) obj.ptrSure.set8(i[n], offset + n)
    }
}

inline class FFIDelegateBoolProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Boolean = obj.ptrSure.getS32(offset) != 0
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Boolean) { obj.ptrSure.set32(if (i) 1 else 0, offset) }
}

inline class FFIDelegateShortProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Short = obj.ptrSure.getS16(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Short): Unit = obj.ptrSure.set16(i, offset)
}

inline class FFIDelegateIntProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Int = obj.ptrSure.getS32(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Int): Unit = obj.ptrSure.set32(i, offset)
}

inline class FFIDelegateFloatProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Float = obj.ptrSure.getF32(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Float): Unit = obj.ptrSure.setF32(i, offset)
}

inline class FFIDelegateDoubleProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Double = obj.ptrSure.getF64(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Double): Unit = obj.ptrSure.setF64(i, offset)
}

inline class FFIDelegateLongProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Long = obj.ptrSure.getS64(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Long): Unit = obj.ptrSure.set64(i, offset)
}

inline class FFIDelegateNativeDoubleProperty(val offset: Int) {
    fun get(pointer: FFIPointer): Double = when (FFI_POINTER_SIZE) {
        4 -> pointer.getF32(offset).toDouble()
        else -> pointer.getF64(offset)
    }
    fun set(pointer: FFIPointer, value: Double) {
        when (FFI_POINTER_SIZE) {
            4 -> pointer.setF32(value.toFloat(), offset)
            else -> pointer.setF64(value, offset)
        }
    }
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Double = get(obj.ptrSure)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Double): Unit = set(obj.ptrSure, i)
}

inline class FFIDelegateNativeLongProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): Long = obj.ptrSure.getFFIPointer(offset).address
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: Long): Unit = obj.ptrSure.setFFIPointer(CreateFFIPointer(i), offset)
}

inline class FFIDelegateFFIPointerProperty(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): FFIPointer? = obj.ptrSure.getFFIPointer(offset)
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: FFIPointer?) = obj.ptrSure.setFFIPointer(i, offset)
}

inline class FFIDelegateFFIPointerPropertyT<T>(val offset: Int) {
    operator fun getValue(obj: FFIStructure, property: KProperty<*>): FFITypedPointer<T>? = obj.ptrSure.getFFIPointer(offset)?.let { FFITypedPointer<T>(it) }
    operator fun setValue(obj: FFIStructure, property: KProperty<*>, i: FFITypedPointer<T>?) = obj.ptrSure.setFFIPointer(i?.pointer, offset)
}
