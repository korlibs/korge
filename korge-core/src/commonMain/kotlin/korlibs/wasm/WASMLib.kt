package korlibs.wasm

import korlibs.io.lang.*
import korlibs.memory.*
import kotlin.coroutines.*

expect open class WASMLib(content: ByteArray) : BaseWASMLib

abstract class BaseWASMLib(val content: ByteArray) : Closeable {
    val loaded: Boolean get() = true
    protected var _context: CoroutineContext? = null

    open fun initOnce(context: CoroutineContext) {
        _context = context
    }

    fun invokeFuncFloat(name: String, vararg params: Any?): Float = (invokeFunc(name, *params) as Number).toFloat()
    fun invokeFuncInt(name: String, vararg params: Any?): Int = (invokeFunc(name, *params) as Number).toInt()
    fun invokeFuncUnit(name: String, vararg params: Any?): Unit { invokeFunc(name, *params) }

    open fun invokeFunc(name: String, vararg params: Any?): Any? {
        TODO()
    }

    open fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? {
        TODO()
    }


    //inline fun <reified T : Function<*>> castToFunc(ptr: FFIPointer): T = sym.castToFunc(ptr, FuncInfo(typeOf<T>(), null))
    protected fun finalize() {
    }

    //val memory: Buffer by lazy { sym.memory }

    //val malloc: (Int) -> Int by func()
    //val free: (Int) -> Unit by func()

    inline fun <T> stackKeep(block: () -> T): T {
        val ptr = stackSave()
        try {
            return block()
        } finally {
            stackRestore(ptr)
        }
    }

    open fun readBytes(pos: Int, size: Int): ByteArray {
        TODO()
    }
    fun readShorts(pos: Int, size: Int): ShortArray {
        val bytes = readBytes(pos, size * 2)
        return ShortArray(size) { bytes.readS16LE(it * 2).toShort() }
    }
    fun readInts(pos: Int, size: Int): IntArray {
        val bytes = readBytes(pos, size * 4)
        return IntArray(size) { bytes.readS32LE(it * 4) }
    }

    open fun writeBytes(pos: Int, data: ByteArray): Unit = TODO()
    fun writeShorts(pos: Int, data: ShortArray) = writeBytes(pos, data.toByteArray())
    fun writeInts(pos: Int, data: IntArray) = writeBytes(pos, data.toByteArray())

    fun ShortArray.toByteArray(): ByteArray = ByteArray(this.size * 2).also { out ->
        for (n in 0 until this.size) out.write16LE(n * 2, this[n].toInt())
    }
    fun IntArray.toByteArray(): ByteArray = ByteArray(this.size * 4).also { out ->
        for (n in 0 until this.size) out.write16LE(n * 4, this[n].toInt())
    }

    open fun allocBytes(bytes: ByteArray): Int {
        return invokeFuncInt("malloc", bytes.size).also { writeBytes(it, bytes) }
    }
    open fun freeBytes(vararg ptrs: Int) {
        for (ptr in ptrs) invokeFunc("free", ptr)
    }

    //val stackSave: () -> Int by func()
    //val stackRestore: (ptr: Int) -> Unit by func()
    //val stackAlloc: (size: Int) -> Int by func()

    open fun stackSave(): Int = invokeFuncInt("stackSave")
    open fun stackRestore(ptr: Int): Unit = invokeFuncUnit("stackRestore")
    open fun stackAlloc(size: Int): Int = invokeFuncInt("stackAlloc")
    open fun stackAllocAndWrite(bytes: ByteArray): Int = stackAlloc(bytes.size).also { writeBytes(it, bytes) }
    open fun stackAllocAndWrite(data: ShortArray): Int = stackAllocAndWrite(data.toByteArray())
    open fun stackAllocAndWrite(data: IntArray): Int = stackAllocAndWrite(data.toByteArray())

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
