package korlibs.wasm

import korlibs.io.lang.*
import korlibs.memory.*
import kotlin.coroutines.*

expect open class WASMLib(content: ByteArray) : IWASMLib

interface IWASMLib : Closeable {
    val isAvailable: Boolean get() = true
    val content: ByteArray

    fun initOnce(context: CoroutineContext) { }
    fun readBytes(pos: Int, size: Int): ByteArray = TODO()
    fun writeBytes(pos: Int, data: ByteArray): Unit = TODO()
    fun invokeFunc(name: String, vararg params: Any?): Any? = TODO()
    fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? = TODO()

    // EXTRA

    fun invokeFuncFloat(name: String, vararg params: Any?): Float = (invokeFunc(name, *params) as Number).toFloat()
    fun invokeFuncInt(name: String, vararg params: Any?): Int = (invokeFunc(name, *params) as? Number)?.toInt() ?: 0
    fun invokeFuncUnit(name: String, vararg params: Any?): Unit { invokeFunc(name, *params) }

    fun readShorts(pos: Int, size: Int): ShortArray {
        val bytes = readBytes(pos, size * 2)
        return ShortArray(size) { bytes.getS16LE(it * 2).toShort() }
    }
    fun readInts(pos: Int, size: Int): IntArray {
        val bytes = readBytes(pos, size * 4)
        return IntArray(size) { bytes.getS32LE(it * 4) }
    }

    fun writeShorts(pos: Int, data: ShortArray) = writeBytes(pos, data.toByteArray())
    fun writeInts(pos: Int, data: IntArray) = writeBytes(pos, data.toByteArray())

    fun ShortArray.toByteArray(): ByteArray = ByteArray(this.size * 2).also { out ->
        for (n in indices) out.set16LE(n * 2, this[n].toInt())
    }
    fun IntArray.toByteArray(): ByteArray = ByteArray(this.size * 4).also { out ->
        for (n in indices) out.set16LE(n * 4, this[n].toInt())
    }

    fun allocBytes(size: Int): Int = invokeFuncInt("malloc", size)
    fun allocBytes(bytes: ByteArray): Int = allocBytes(bytes.size).also { writeBytes(it, bytes) }
    fun freeBytes(vararg ptrs: Int) {
        for (ptr in ptrs) invokeFunc("free", ptr)
    }

    fun stackSave(): Int = invokeFuncInt("stackSave")
    fun stackRestore(ptr: Int): Unit = invokeFuncUnit("stackRestore", ptr)
    fun stackAlloc(size: Int): Int = invokeFuncInt("stackAlloc", size)
    fun stackAllocAndWrite(bytes: ByteArray): Int = stackAlloc(bytes.size).also { writeBytes(it, bytes) }
    fun stackAllocAndWrite(data: ShortArray): Int = stackAllocAndWrite(data.toByteArray())
    fun stackAllocAndWrite(data: IntArray): Int = stackAllocAndWrite(data.toByteArray())
}

inline fun <T> IWASMLib.stackKeep(block: () -> T): T {
    val ptr = stackSave()
    try {
        return block()
    } finally {
        stackRestore(ptr)
    }
}

abstract class BaseWASMLib(override val content: ByteArray) : IWASMLib {
    val loaded: Boolean get() = true
    protected var _context: CoroutineContext? = null

    override fun initOnce(context: CoroutineContext) {
        super.initOnce(context)
        _context = context
    }

    /*
    companion object {
        class FuncType(val params: List<KType?>, val ret: KType?) {
            val paramsClass = params.map { it?.classifier }
            val retClass = ret?.classifier
        }

        fun extractTypeFunc(type: KType): FuncType {
            val generics = type.arguments.map { it.type }
            val ret = generics.last()
            val params = generics.dropLast(1)
            return FuncType(params, ret)
        }
    }

    class FuncDelegate<T>(val base: BaseWASMLib, val name: String, val type: KType) : ReadOnlyProperty<BaseWASMLib, T> {
        val parts = extractTypeFunc(type)
        //val generics = type.arguments.map { it.type?.classifier }
        val params = parts.paramsClass
        val ret = parts.retClass
        var cached: T? = null
        override fun getValue(thisRef: BaseWASMLib, property: KProperty<*>): T {
            if (cached == null) cached = base.symGet(name, type)
            return cached.fastCastTo()
        }
    }

    class FuncInfo<T>(val type: KType, val extraName: String?) {
        operator fun provideDelegate(
            thisRef: BaseWASMLib,
            prop: KProperty<*>
        ): ReadOnlyProperty<BaseWASMLib, T> = FuncDelegate<T>(thisRef, extraName ?: prop.name, type).also {
            thisRef.functions.add(it)
            if (!thisRef.lazyCreate) it.getValue(thisRef, prop)
        }
    }

    val lazyCreate = true
    val functions = arrayListOf<FuncDelegate<*>>()
    inline fun <reified T : Function<*>> func(name: String? = null): FuncInfo<T> = FuncInfo<T>(typeOf<T>(), name)
    open fun <T> symGet(name: String, type: KType): T = TODO()
    open fun <T : Function<*>> funcPointer(address: Int, type: KType): T = TODO()
    inline fun <reified T : Function<*>> funcPointer(address: Int): T = funcPointer(address, typeOf<T>())

    */

    override fun close() {
    }
}
