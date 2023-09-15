package korlibs.wasm

import korlibs.io.stream.*
import java.lang.reflect.*
import kotlin.reflect.*

//actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by DenoWASMLib(content)
//actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by InterpreterWASMLib(content)

//open class WASMLib2(content: ByteArray) : IWASMLib, BaseWASMLib(content) {
actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by JVMWasmLib(content)

class JVMWasmLib(content: ByteArray) : IWASMLib, BaseWASMLib(content) {
    private val wasm: WasmRunJVMJIT by lazy {
        WasmRunJVMOutput().generate(WasmReaderBinary().read(content.openSync()).toModule())
    }

    override fun readBytes(pos: Int, size: Int): ByteArray = wasm.readBytes(pos, size)
    override fun writeBytes(pos: Int, data: ByteArray) = wasm.writeBytes(pos, data)
    override fun invokeFunc(name: String, vararg params: Any?): Any? = wasm.invoke(name, *params)
    override fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? = wasm.invokeIndirect(address, *params)

    /*
    val functionsNamed: Map<String, kotlin.Function<*>> by lazy {
        functions.associate { nfunc ->
            //val lib = NativeLibrary.getInstance("")
            nfunc.name to createFunction(nfunc.name, nfunc.type)
        }
    }

    fun <T : kotlin.Function<*>> createFunction(funcName: String, type: KType): T {
        return _createWasmFunctionToPlainFunction(wasm, type, -1, funcName)
    }

    override fun <T : Function<*>> funcPointer(address: Int, type: KType): T =
        _createWasmFunctionToPlainFunction(wasm, type, address, null)

    override fun <T> symGet(name: String, type: KType): T {
        return functionsNamed[name] as T
    }

    override fun close() {
        wasm.close()
    }

    fun <T : kotlin.Function<*>> _createWasmFunctionToPlainFunction(wasm: WasmRunJVMJIT, type: KType, address: Int, funcName: String?): T {
        val ftype = extractTypeFunc(type)
        return Proxy.newProxyInstance(
            WASMLib::class.java.classLoader,
            arrayOf((type.classifier as KClass<*>).java)
        ) { proxy, method, args ->
            val sargs = args ?: emptyArray()
            //return wasm.evalWASMFunction(nfunc.name, *sargs)
            if (funcName != null) {
                wasm.invoke(funcName, *sargs)
            } else {
                wasm.invokeIndirect(address, *sargs)
            }
        } as T
    }

     */
}

/*
actual open class WASMLib actual constructor(content: ByteArray) : BaseWASMLib(content) {
    val functionsNamed: Map<String, kotlin.Function<*>> by lazy {
        functions.associate { nfunc ->
            //val lib = NativeLibrary.getInstance("")
            nfunc.name to createFunction(nfunc.name, nfunc.type)
        }
    }

    fun <T : kotlin.Function<*>> createFunction(funcName: String, type: KType): T {
        return _createWasmFunctionToPlainFunction(wasm, type, -1, funcName)
    }

    val wasm: DenoWASM by lazy {
        DenoWasmProcessStdin.open(content)
    }

    override fun readBytes(pos: Int, size: Int): ByteArray = wasm.readBytes(pos, size)
    override fun writeBytes(pos: Int, data: ByteArray) = wasm.writeBytes(pos, data)
    override fun allocBytes(bytes: ByteArray): Int = wasm.allocAndWrite(bytes)
    override fun freeBytes(vararg ptrs: Int) = wasm.free(*ptrs)
    override fun stackSave(): Int = wasm.stackSave()
    override fun stackRestore(ptr: Int) = wasm.stackRestore(ptr)
    override fun stackAlloc(size: Int): Int = wasm.stackAlloc(size)
    override fun stackAllocAndWrite(bytes: ByteArray): Int = wasm.stackAllocAndWrite(bytes)

    override fun <T : Function<*>> funcPointer(address: Int, type: KType): T =
        _createWasmFunctionToPlainFunction(wasm, type, address, null)

    override fun <T> symGet(name: String, type: KType): T {
        return functionsNamed[name] as T
    }

    override fun close() {
        wasm.close()
    }


    fun <T : kotlin.Function<*>> _createWasmFunctionToPlainFunction(wasm: DenoWASM, type: KType, address: Int, funcName: String?): T {
        val ftype = extractTypeFunc(type)
        return Proxy.newProxyInstance(
            FFILibSymJVM::class.java.classLoader,
            arrayOf((type.classifier as KClass<*>).java)
        ) { proxy, method, args ->
            val sargs = args ?: emptyArray()
            //return wasm.evalWASMFunction(nfunc.name, *sargs)
            if (funcName != null) {
                wasm.executeFunction(funcName, *sargs)
            } else {
                wasm.executeFunctionIndirect(address, *sargs)
            }
        } as T
    }
}
*/
