package korlibs.memory.dyn

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

inline class KPointer(val address: Long)


/*
typealias KNativeLong = KPointer
typealias KPointer = KPointerTT<out KPointed>
//expect class KPointer
expect open class KPointed
expect class KPointerTT<T : KPointed>
expect class KFunctionTT<T : Function<*>> : KPointed
*/
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

//expect fun KPointer(address: Long): KPointer
//expect val KPointer.address: Long

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
