package korlibs.ffi

import com.sun.jna.*
import com.sun.jna.Function
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

actual fun <T> FFICreateProxyFunction(type: KType, handler: (args: Array<Any?>) -> Any?): T = Proxy.newProxyInstance(
    FFILibSym::class.java.classLoader,
    arrayOf((type.classifier as KClass<*>).java)
) { _, _, args ->
    handler(args ?: arrayOf())
} as T

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

actual fun FFIPointer.set8(value: Byte, byteOffset: Int): Unit = this.setByte(byteOffset.toLong(), value)
actual fun FFIPointer.set16(value: Short, byteOffset: Int): Unit = this.setShort(byteOffset.toLong(), value)
actual fun FFIPointer.set32(value: Int, byteOffset: Int): Unit = this.setInt(byteOffset.toLong(), value)
actual fun FFIPointer.set64(value: Long, byteOffset: Int): Unit = this.setLong(byteOffset.toLong(), value)
actual fun FFIPointer.setF32(value: Float, byteOffset: Int): Unit = this.setFloat(byteOffset.toLong(), value)
actual fun FFIPointer.setF64(value: Double, byteOffset: Int): Unit = this.setDouble(byteOffset.toLong(), value)

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
    createJNAFunctionToPlainFunc(Function.getFunction(this), type, config, null)

fun <T : kotlin.Function<*>> createJNAFunctionToPlainFunc(func: Function?, type: KType, config: FFIFuncConfig, name: String?): T {
    val ftype = FFILib.extractTypeFunc(type)
    var ret = ftype.retClass as KClass<*>
    val isDeferred = ret == Deferred::class
    if (isDeferred) {
        ret = ftype.ret?.arguments?.first()?.type?.classifier as KClass<*>
    }

    return Proxy.newProxyInstance(
        FFILibSymJVM::class.java.classLoader,
        arrayOf((type.classifier as KClass<*>).java)
    ) { proxy, method, args ->
        if (func == null) error("Function not available")
        val inputArgs = args ?: emptyArray()
        val targsl = ArrayList<Any?>(inputArgs.size)
        for (it in inputArgs) {
            when (it) {
                is FFIVarargs -> targsl.addAll(it.args)
                is FFIPointerArray -> targsl.add(it.data)
                is Buffer -> targsl.add(it.buffer)
                is String -> targsl.add(if (config.wideString) com.sun.jna.WString(it) else it)
                else -> targsl.add(it)
            }
        }
        val targs = targsl.toTypedArray()
        //println("name=$name, targsl=$targsl")

        fun call(): Any? {
            return when (ret) {
                Unit::class -> func.invokeVoid(targs)
                Int::class -> func.invokeInt(targs)
                Float::class -> func.invokeFloat(targs)
                Double::class -> func.invokeDouble(targs)
                else -> func.invoke((ftype.retClass as KClass<*>).java, targs)
            }.also {
                //println("  -> ret=$it [${ret.simpleName}]")
            }
        }

        if (isDeferred) {
            CompletableDeferred<Any?>().also { value ->
                Executors.newCachedThreadPool().execute { value.complete(call()) }
                //value.complete(call())
            }
        } else {
            call()
        }.also {
            //println("    -> $it")
        }
    } as T
}

inline fun <reified T : kotlin.Function<*>> createJNAFunctionToPlainFunc(func: Function, config: FFIFuncConfig): T =
    createJNAFunctionToPlainFunc(func, typeOf<T>(), config, null)

class FFILibSymJVM(val lib: FFILib) : FFILibSym {
    @OptIn(FFISyncIOAPI::class)
    val nlib by lazy {
        val resolvedPaths = listOf(LibraryResolver.resolve(*lib.paths.toTypedArray()))
        resolvedPaths.firstNotNullOfOrNull {
            NativeLibrary.getInstance(it)
        }
    }

    fun <T : kotlin.Function<*>> createFunction(funcName: String, type: KType, config: FFIFuncConfig, required: Boolean): T {
        val func = runCatching { nlib!!.getFunction(funcName) ?: error("Can't find function $funcName") }
        func.exceptionOrNull()?.let {
            if (required) {
                println("WARNING[${it::class}]: ${it.message} getting $funcName")
                it.printStackTrace()
            }
        }
        return createJNAFunctionToPlainFunc<T>(func.getOrNull(), type, config, funcName)
    }

    val functions: Map<String, kotlin.Function<*>> by lazy {
        lib.functions.associate { nfunc ->
            //val lib = NativeLibrary.getInstance("")
            nfunc.bname to createFunction(nfunc.name, nfunc.type, nfunc.config, nfunc.required)
        }
    }

    override fun <T> get(name: String, type: KType): T = functions[name] as T

    override fun close() {
    }
}
