package korlibs.ffi

import com.sun.jna.*
import com.sun.jna.Function
import korlibs.io.file.sync.*
import korlibs.memory.*
import kotlinx.coroutines.*
import java.lang.reflect.*
import java.util.concurrent.*
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.reflect.*

actual fun FFILibSym(lib: FFILib): FFILibSym {
    return FFILibSymJVM(lib)
}

actual typealias FFIPointer = Pointer
actual typealias FFIMemory = Memory

actual val FFI_SUPPORTED: Boolean = true

actual fun CreateFFIMemory(size: Int): FFIMemory = Memory(size.toLong())
actual fun CreateFFIMemory(bytes: ByteArray): FFIMemory = Memory(bytes.size.toLong()).also { it.write(0L, bytes, 0, bytes.size) }
actual val FFIMemory.pointer: FFIPointer get() = this

@JvmName("FFIPointerCreation")
actual fun CreateFFIPointer(ptr: Long): FFIPointer? = if (ptr == 0L) null else Pointer(ptr)
actual val FFI_POINTER_SIZE: Int = 8

actual fun FFIPointer.getStringz(): String = this.getString(0L)
actual fun FFIPointer.getWideStringz(): String = this.getWideString(0L)
actual val FFIPointer?.address: Long get() = Pointer.nativeValue(this)
actual val FFIPointer?.str: String get() = this.toString()
actual fun FFIPointer.getIntArray(size: Int, byteOffset: Int): IntArray = this.getIntArray(0L, size)

actual fun FFIPointer.getS8(byteOffset: Int): Byte = this.getByte(byteOffset.toLong())
actual fun FFIPointer.getS16(byteOffset: Int): Short = this.getShort(byteOffset.toLong())
actual fun FFIPointer.getS32(byteOffset: Int): Int = this.getInt(byteOffset.toLong())
actual fun FFIPointer.getS64(byteOffset: Int): Long = this.getLong(byteOffset.toLong())
actual fun FFIPointer.getF32(byteOffset: Int): Float = this.getFloat(byteOffset.toLong())
actual fun FFIPointer.getF64(byteOffset: Int): Double = this.getDouble(byteOffset.toLong())

actual fun FFIPointer.set8(value: Byte, byteOffset: Int) = this.setByte(byteOffset.toLong(), value)
actual fun FFIPointer.set16(value: Short, byteOffset: Int) = this.setShort(byteOffset.toLong(), value)
actual fun FFIPointer.set32(value: Int, byteOffset: Int) = this.setInt(byteOffset.toLong(), value)
actual fun FFIPointer.set64(value: Long, byteOffset: Int) = this.setLong(byteOffset.toLong(), value)
actual fun FFIPointer.setF32(value: Float, byteOffset: Int) = this.setFloat(byteOffset.toLong(), value)
actual fun FFIPointer.setF64(value: Double, byteOffset: Int) = this.setDouble(byteOffset.toLong(), value)

actual class FFIArena actual constructor() {
    private val pointers = arrayListOf<Memory>()
    actual fun allocBytes(size: Int): FFIPointer = Memory(size.toLong()).also {
        it.clear()
        pointers += it
    }
    actual fun clear() {
        for (n in 0 until pointers.size) pointers[n].clear()
        pointers.clear()
    }
}

actual fun <T> FFIPointer.castToFunc(type: KType, config: FFIFuncConfig): T =
    createJNAFunctionToPlainFunc(Function.getFunction(this), type, config)

fun <T : kotlin.Function<*>> createJNAFunctionToPlainFunc(func: Function, type: KType, config: FFIFuncConfig): T {
    val ftype = FFILib.extractTypeFunc(type)

    return Proxy.newProxyInstance(
        FFILibSymJVM::class.java.classLoader,
        arrayOf((type.classifier as KClass<*>).java)
    ) { proxy, method, args ->
        val targs = (args ?: emptyArray()).map {
            when (it) {
                is FFIPointerArray -> it.data
                is Buffer -> it.buffer
                is String -> if (config.wideString) com.sun.jna.WString(it) else it
                else -> it
            }
        }.toTypedArray()

        var ret = ftype.retClass
        val isDeferred = ret == Deferred::class
        if (isDeferred) {
            ret = ftype.ret?.arguments?.first()?.type?.classifier
        }

        fun call(): Any? {
            return when (ret) {
                Unit::class -> func.invokeVoid(targs)
                Int::class -> func.invokeInt(targs)
                Float::class -> func.invokeFloat(targs)
                Double::class -> func.invokeDouble(targs)
                else -> func.invoke((ftype.retClass as KClass<*>).java, targs)
            }
        }

        if (isDeferred) {
            CompletableDeferred<Any?>().also { value ->
                Executors.newCachedThreadPool().execute { value.complete(call()) }
                //value.complete(call())
            }
        } else {
            call()
        }
    } as T
}

inline fun <reified T : kotlin.Function<*>> createJNAFunctionToPlainFunc(func: Function, config: FFIFuncConfig): T =
    createJNAFunctionToPlainFunc(func, typeOf<T>(), config)

class FFILibSymJVM(val lib: FFILib) : FFILibSym {
    @OptIn(SyncIOAPI::class)
    val nlib by lazy {
        lib as FFILib
        val resolvedPaths = listOf(LibraryResolver.resolve(*lib.paths.toTypedArray()))
        resolvedPaths.firstNotNullOfOrNull {
            NativeLibrary.getInstance(it)
        }
    }

    fun <T : kotlin.Function<*>> createFunction(funcName: String, type: KType, config: FFIFuncConfig): T {
        val func: Function = nlib!!.getFunction(funcName) ?: error("Can't find function ${funcName}")
        return createJNAFunctionToPlainFunc<T>(func, type, config)
    }

    val functions: Map<String, kotlin.Function<*>> by lazy {
        lib.functions.associate { nfunc ->
            //val lib = NativeLibrary.getInstance("")
            nfunc.name to createFunction(nfunc.name, nfunc.type, nfunc.config)
        }
    }

    override fun <T> get(name: String, type: KType): T = functions[name] as T

    override fun close() {
    }
}
