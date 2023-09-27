package korlibs.wasm

import korlibs.memory.*
import kotlinx.cinterop.*
import platform.JavaScriptCore.*
import platform.posix.*

actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by NativeWASMLib(content)

class NativeWASMLib(content: ByteArray) : IWASMLib, BaseWASMLib(content) {
    private val runner = WASMRunner().also {
        it.loadWasmModule(content)
    }

    override fun readBytes(pos: Int, size: Int): ByteArray = runner.readBytes(pos, size)
    override fun writeBytes(pos: Int, data: ByteArray) = runner.writeBytes(pos, data)
    override fun invokeFunc(name: String, vararg params: Any?): Any? = runner.invokeFunction(name, *params)?.toAny()

    private fun JSValue.toAny(): Any? = when {
        this.isNull -> null
        this.isUndefined -> null
        this.isString -> this.toString_()
        this.isBoolean -> this.toBool()
        this.isNumber -> this.toNumber()?.doubleValue
        else -> this.toString_()
    }
    //override fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? = runner.invokeFuncIndirect(address, *params)

    override fun close() = runner.close()

    //override fun <T> symGet(name: String, type: KType): T = super.symGet(name, type)
    //override fun <T : Function<*>> funcPointer(address: Int, type: KType): T = super.funcPointer(address, type)
}

@OptIn(ExperimentalForeignApi::class)
class WASMRunner {
    private var _lastException: JSValue? = null

    val js = JSContext().also {
        it.exceptionHandler = { context, exception ->
            _lastException = exception
            //println("Exception: ${platform.posix.exception}")
        }
    }

    fun evaluateScript(script: String): JSValue {
        _lastException = null
        val result = js.evaluateScript(script)
        if (_lastException != null) {
            throw Exception("$_lastException\n${_lastException?.objectForKeyedSubscript("stack")}")
        }
        return result ?: JSValue.valueWithUndefinedInContext(js)!!
    }

    fun JSContext.createUint8Array(bytes: ByteArray): JSValue {
        val ctxRef = JSGlobalContextRef_
        val arrRef = JSObjectMakeTypedArray(ctxRef, JSTypedArrayType.kJSTypedArrayTypeUint8Array, bytes.size.convert(), null)
        val ptr = JSObjectGetTypedArrayBytesPtr(ctxRef, arrRef, null)
        bytes.usePinned { memcpy(ptr, it.addressOf(0), bytes.size.convert()) }
        return JSValue.valueWithJSValueRef(arrRef, this) ?: error("Can't create Uint8Array")
    }
    fun JSValue.toByteArray(): ByteArray {
        val typedArray = this
        val ctxRef = this.context!!.JSGlobalContextRef_
        //typeArray.valueForProperty(JSValueProtect())
        val len = typedArray.objectForKeyedSubscript("length")!!.toInt32()
        val ptr = JSObjectGetTypedArrayBytesPtr(ctxRef, typedArray.JSValueRef_, null)
        val bytes = ByteArray(len)
        if (len > 0) {
            bytes.usePinned { memcpy(it.startAddressOf, ptr, bytes.size.convert()) }
        }
        //println("len=$len")
        return bytes
    }
    // @TODO: Polyfill
    fun loadWasmModule(wasmBytes: ByteArray) {
        js.globalObject!!.setObject(js.createUint8Array(wasmBytes), forKeyedSubscript = "tempBytes")

        if (evaluateScript("WebAssembly").isUndefined) {
            evaluateScript(WASMJSPolyfill)
        }

        evaluateScript(/* language=javascript */ """
          globalThis.wasmInstance = new WebAssembly.Instance(new WebAssembly.Module(tempBytes), {
            "wasi_snapshot_preview1": {
              proc_exit: () => console.error(arguments),
              fd_close: () => console.error(arguments),
              fd_write: () => console.error(arguments),
              fd_seek: () => console.error(arguments),
            },
          });
          globalThis.tempBytes = undefined;
        """.trimIndent())
    }

    fun writeBytes(ptr: Int, bytes: ByteArray) {
        js.globalObject!!.setObject(js.createUint8Array(bytes), forKeyedSubscript = "tempBytes")
        evaluateScript("""
          new Uint8Array(globalThis.wasmInstance.exports.memory.buffer).set(tempBytes, $ptr);
          globalThis.tempBytes = undefined;
        """.trimIndent()).also {
            //println("eval=$it")
        }
    }

    fun readBytes(ptr: Int, len: Int): ByteArray {
        return evaluateScript("""
          new Uint8Array(globalThis.wasmInstance.exports.memory.buffer, $ptr, $len)
        """)?.toByteArray() ?: error("Can't extract bytes")
    }

    fun invokeFunction(name: String, vararg values: Any?): JSValue? {
        return evaluateScript("wasmInstance.exports.$name(${values.joinToString(", ")})")
    }

    fun close() {
    }
}
