package korlibs.ffi

import korlibs.datastructure.*
import korlibs.datastructure.closeable.*
import korlibs.memory.*
import kotlin.jvm.*
import kotlin.properties.*
import kotlin.reflect.*

expect fun <T> FFICreateProxyFunction(type: KType, handler: (args: Array<Any?>) -> Any?): T

inline fun <reified T> FFICreateProxyFunction(noinline handler: (args: Array<Any?>) -> Any?): T = FFICreateProxyFunction(typeOf<T>(), handler)

expect class FFIPointer

expect class FFIMemory

expect val FFI_SUPPORTED: Boolean

expect fun CreateFFIMemory(size: Int): FFIMemory
expect fun CreateFFIMemory(bytes: ByteArray): FFIMemory

expect val FFIMemory.pointer: FFIPointer

expect fun CreateFFIPointer(ptr: Long): FFIPointer?
expect val FFIPointer?.address: Long
expect val FFIPointer?.str: String
expect fun FFIPointer.getStringz(): String
expect fun FFIPointer.getWideStringz(): String
expect fun <T> FFIPointer.castToFunc(type: KType, config: FFIFuncConfig = FFIFuncConfig.DEFAULT): T
inline fun <reified T> FFIPointer.castToFunc(): T = castToFunc(typeOf<T>())
expect val FFI_POINTER_SIZE: Int
val FFI_NATIVE_LONG_SIZE: Int get() = FFI_POINTER_SIZE
expect fun FFIPointer.getIntArray(size: Int, byteOffset: Int = 0): IntArray
expect fun FFIPointer.getS8(byteOffset: Int = 0): Byte
expect fun FFIPointer.getS16(byteOffset: Int = 0): Short
expect fun FFIPointer.getS32(byteOffset: Int = 0): Int
expect fun FFIPointer.getS64(byteOffset: Int = 0): Long
expect fun FFIPointer.getF32(byteOffset: Int = 0): Float
expect fun FFIPointer.getF64(byteOffset: Int = 0): Double
expect fun FFIPointer.set8(value: Byte, byteOffset: Int = 0)
expect fun FFIPointer.set16(value: Short, byteOffset: Int = 0)
expect fun FFIPointer.set32(value: Int, byteOffset: Int = 0)
expect fun FFIPointer.set64(value: Long, byteOffset: Int = 0)
expect fun FFIPointer.setF32(value: Float, byteOffset: Int = 0)
expect fun FFIPointer.setF64(value: Double, byteOffset: Int = 0)

fun FFIPointer.getByteArray(size: Int, byteOffset: Int = 0): ByteArray = ByteArray(size) { getS8(byteOffset + (it * Byte.SIZE_BYTES)) }
fun FFIPointer.getShortArray(size: Int, byteOffset: Int = 0): ShortArray = ShortArray(size) { getS16(byteOffset + (it * Short.SIZE_BYTES)) }

expect class FFIArena() {
    fun allocBytes(size: Int): FFIPointer
    fun clear(): Unit
}

inline fun FFIArena.allocBytes(size: Int, gen: (Int) -> Byte): FFIPointer {
    return allocBytes(size).also { for (n in 0 until size) it.set8(gen(n), n) }
}

inline fun <T> ffiScoped(block: FFIArena.() -> T): T {
    val arena = FFIArena()
    try {
        return block(arena)
    } finally {
        arena.clear()
    }
}

fun FFIPointer.setFFIPointer(value: FFIPointer?, byteOffset: Int = 0) {
    if (FFI_POINTER_SIZE == 8) set64(value.address, byteOffset) else set32(value.address.toInt(), byteOffset)
}

fun FFIPointer.getFFIPointer(byteOffset: Int = 0): FFIPointer? =
    if (FFI_POINTER_SIZE == 8) CreateFFIPointer(getS64(byteOffset)) else CreateFFIPointer(getS32(byteOffset).toLong())

@JvmInline
value class FFIVarargs(val args: List<Any?>) {
    constructor(vararg args: Any?) : this(args.toList())
    override fun toString(): String = "FFIVarargs(${args.joinToString(", ")})"
}

interface FFICallback

/** Might be 32-bit or 64-bit depending on the OS */
class FFINativeLong(val value: Long) {

}

fun FFIPointer.getAlignedS16(offset: Int = 0): Short = getS16(offset * 2)
fun FFIPointer.getAlignedS32(offset: Int = 0): Int = getS32(offset * 4)
fun FFIPointer.getAlignedS64(offset: Int = 0): Long = getS64(offset * 8)
fun FFIPointer.getAlignedF32(offset: Int = 0): Float = getF32(offset * 4)
fun FFIPointer.getAlignedF64(offset: Int = 0): Double = getF64(offset * 8)

fun FFIPointer.setAligned16(value: Short, offset: Int = 0) = set16(value, offset * 2)
fun FFIPointer.setAligned32(value: Int, offset: Int = 0) = set32(value, offset * 4)
fun FFIPointer.setAligned64(value: Long, offset: Int = 0) = set64(value, offset * 8)
fun FFIPointer.setAlignedF32(value: Float, offset: Int = 0) = setF32(value, offset * 4)
fun FFIPointer.setAlignedF64(value: Double, offset: Int = 0) = setF64(value, offset * 8)

fun FFIPointer.getAlignedFFIPointer(offset: Int = 0): FFIPointer? =
    if (FFI_POINTER_SIZE == 8) CreateFFIPointer(getAlignedS64(offset)) else CreateFFIPointer(getAlignedS32(offset).toLong())

fun FFIPointer.setAlignedFFIPointer(value: FFIPointer?, offset: Int = 0) =
    setFFIPointer(value, offset * FFI_POINTER_SIZE)

fun ffiPointerArrayOf(vararg pointers: FFIPointer?): FFIPointerArray = FFIPointerArray(pointers.size).also {
    for (n in pointers.indices) it[n] = pointers[n]
}

@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
data class FFIPointerArray(val data: IntArray) : List<FFIPointer?> {
    constructor(size: Int) : this(IntArray(size * 2))
    override operator fun get(index: Int): FFIPointer? {
        val address = Long.fromLowHigh(data[index * 2 + 0], data[index * 2 + 1])
        if (address == 0L) return null
        return CreateFFIPointer(address)
    }
    operator fun set(index: Int, value: FFIPointer?) {
        val address = value.address
        data[index * 2 + 0] = address.low
        data[index * 2 + 1] = address.high
    }
    override val size: Int get () = data.size / 2
    override fun isEmpty(): Boolean = size == 0

    private val vlist get() = (0 until size).map { this[it] }

    override fun iterator(): Iterator<FFIPointer?> = vlist.iterator()
    override fun listIterator(): ListIterator<FFIPointer?> = vlist.listIterator()
    override fun listIterator(index: Int): ListIterator<FFIPointer?> = vlist.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<FFIPointer?> = TODO()
    override fun indexOf(element: FFIPointer?): Int {
        for (n in 0 until size) if (this[n] == element) return n
        return -1
    }
    override fun lastIndexOf(element: FFIPointer?): Int {
        for (n in size - 1 downTo 0) if (this[n] == element) return n
        return -1
    }
    override fun containsAll(elements: Collection<FFIPointer?>): Boolean = vlist.containsAll(elements)
    override fun contains(element: FFIPointer?): Boolean = indexOf(element) >= 0
}

fun FFIPointer.withOffset(offset: Int): FFIPointer? = CreateFFIPointer(address + offset)

fun Buffer.getFFIPointer(offset: Int): FFIPointer? {
    return CreateFFIPointer(if (FFI_POINTER_SIZE == 8) getInt64(offset) else getInt32(offset).toLong())
}
fun Buffer.setFFIPointer(offset: Int, value: FFIPointer?) {
    if (FFI_POINTER_SIZE == 8) setInt64(offset, value.address) else setInt32(offset, value.address.toInt())
}

fun Buffer.getUnalignedFFIPointer(offset: Int): FFIPointer? {
    return CreateFFIPointer(if (FFI_POINTER_SIZE == 8) getS64(offset) else getS32(offset).toLong())
}
fun Buffer.setUnalignedFFIPointer(offset: Int, value: FFIPointer?) {
    if (FFI_POINTER_SIZE == 8) set64(offset, value.address) else set32(offset, value.address.toInt())
}

expect fun FFILibSym(lib: FFILib): FFILibSym

interface FFILibSym : Closeable {
    fun <T> get(name: String, type: KType): T = TODO()
    override fun close() {}
}

class FuncType(val params: List<KType?>, val ret: KType?) {
    val paramsClass = params.map { it?.classifier }
    val retClass = ret?.classifier
}

data class FFIFuncConfig(
    val wideString: Boolean = false,
    // Alternate to C ABI convention
    val altAbiConvention: Boolean = false,
) {
    companion object {
        val DEFAULT = FFIFuncConfig()
        val WIDE_STRING = DEFAULT.copy(wideString = true)
    }
}

open class FFILib(val paths: List<String>, val lazyCreate: Boolean = true) {
    @OptIn(FFISyncIOAPI::class)
    val resolvedPath by lazy { LibraryResolver.resolve(*paths.toTypedArray()) }

    constructor(vararg paths: String?, lazyCreate: Boolean = true) : this(paths.toList().filterNotNull(), lazyCreate = lazyCreate)

    val functions = arrayListOf<FuncDelegate<*>>()
    open protected fun createFFILibSym(): FFILibSym =  FFILibSym(this)
    var _sym: FFILibSym? = null
    val sym: FFILibSym get() {
        if (_sym == null) _sym = createFFILibSym()
        return _sym!!
    }
    //val loaded: Boolean get() = sym != null
    val loaded: Boolean get() = true

    companion object {
        val isFFISupported = FFI_SUPPORTED

        fun extractTypeFunc(type: KType): FuncType {
            val generics = type.arguments.map { it.type }
            val ret = generics.last()
            val params = generics.dropLast(1)
            return FuncType(params, ret)
        }
    }

    class FuncDelegate<T>(val base: FFILib, val bname: String, val name: String, val type: KType, val config: FFIFuncConfig, val required: Boolean) : ReadOnlyProperty<FFILib, T> {
        val parts = extractTypeFunc(type)
        //val generics = type.arguments.map { it.type?.classifier }
        val params = parts.paramsClass
        val ret = parts.retClass
        var cached: T? = null
        override fun getValue(thisRef: FFILib, property: KProperty<*>): T {
            if (cached == null) cached = base.sym.get(bname, type)
            return cached.fastCastTo()
        }
    }

    class FuncInfo<T>(val type: KType, val extraName: String?, val config: FFIFuncConfig, val required: Boolean = true) {
        operator fun provideDelegate(
            thisRef: FFILib,
            prop: KProperty<*>
        ): ReadOnlyProperty<FFILib, T> = FuncDelegate<T>(thisRef, prop.name, extraName ?: prop.name, type, config, required).also {
            thisRef.functions.add(it)
            if (!thisRef.lazyCreate) it.getValue(thisRef, prop)
        }
    }

    inline fun <reified T : Function<*>> func(name: String? = null, config: FFIFuncConfig = FFIFuncConfig.DEFAULT, required: Boolean = true): FuncInfo<T> = FuncInfo<T>(typeOf<T>(), name, config, required)

    //inline fun <reified T : Function<*>> castToFunc(ptr: FFIPointer): T = sym.castToFunc(ptr, FuncInfo(typeOf<T>(), null))
    protected fun finalize() {
        sym
    }
}

open class SymbolResolverFFILib(val resolve: (String) -> FFIPointer?) : FFILib(lazyCreate = false) {
    override fun createFFILibSym(): FFILibSym {
        return object : FFILibSym {
            override fun <T> get(name: String, type: KType): T = resolve(name)?.castToFunc(type) ?: error("Can't find symbol '$name'")
        }
    }
}

/*
object LibC : FFILib("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation") {
    val cos by func<(value: Double) -> Double>()
    val cosf by func<(value: Float) -> Float>()
    init {
        finalize()
    }
}
 */


inline class FFITypedPointer<T>(val pointer: FFIPointer)

fun <T> FFIPointer.typed(): FFITypedPointer<T> = FFITypedPointer<T>(this)
fun <T> FFITypedPointer<*>.reinterpret(): FFITypedPointer<T> = FFITypedPointer<T>(this.pointer)

operator fun FFITypedPointer<Byte>.get(index: Int): Byte = pointer.getS8(index * 1)
operator fun FFITypedPointer<Short>.get(index: Int): Short = pointer.getS16(index * 2)
operator fun FFITypedPointer<Int>.get(index: Int): Int = pointer.getS32(index * 4)
operator fun FFITypedPointer<Long>.get(index: Int): Long = pointer.getS64(index * 8)
operator fun FFITypedPointer<Float>.get(index: Int): Float = pointer.getF32(index * 4)
operator fun FFITypedPointer<Double>.get(index: Int): Double = pointer.getF64(index * 8)
operator fun FFITypedPointer<FFIPointer?>.get(index: Int): FFIPointer? = pointer.getFFIPointer(index * FFI_POINTER_SIZE)

operator fun FFITypedPointer<Byte>.set(index: Int, value: Byte) = pointer.set8(value, index * 1)
operator fun FFITypedPointer<Short>.set(index: Int, value: Short) = pointer.set16(value, index * 2)
operator fun FFITypedPointer<Int>.set(index: Int, value: Int) = pointer.set32(value, index * 4)
operator fun FFITypedPointer<Long>.set(index: Int, value: Long) = pointer.set64(value, index * 8)
operator fun FFITypedPointer<Float>.set(index: Int, value: Float) = pointer.setF32(value, index * 4)
operator fun FFITypedPointer<Double>.set(index: Int, value: Double) = pointer.setF64(value, index * 8)
operator fun FFITypedPointer<FFIPointer?>.set(index: Int, value: FFIPointer?) = pointer.setFFIPointer(value, index * FFI_POINTER_SIZE)
