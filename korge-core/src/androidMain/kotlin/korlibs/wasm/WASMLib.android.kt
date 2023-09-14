package korlibs.wasm

import android.content.*
import androidx.javascriptengine.*
import korlibs.io.android.*
import korlibs.io.lang.*

actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib, BaseWASMLib(content) {
    val executor: AndroidWASMExecutor by lazy {
        val _context = _context ?: error("Must call WASMLib.initOnce before calling any method")
        AndroidWASMExecutor(_context.androidContext())
    }

    override fun readBytes(pos: Int, size: Int): ByteArray = executor.readBytes(pos, size)
    override fun writeBytes(pos: Int, data: ByteArray) = executor.writeBytes(pos, data)

    override fun invokeFunc(name: String, vararg params: Any?): Any? = executor.executeFunction(name, *params)

    //override fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? = wasm.invokeIndirect(address, *params)

    override fun close() {
        executor.close()
    }

    /*
    override fun <T> symGet(name: String, type: KType): T = _createWasmFunctionToPlainFunction(type, -1, name)
    override fun <T : Function<*>> funcPointer(address: Int, type: KType): T = _createWasmFunctionToPlainFunction(type, address, null)

    fun <T : kotlin.Function<*>> _createWasmFunctionToPlainFunction(type: KType, address: Int, funcName: String?): T {
        val ftype = extractTypeFunc(type)
        return Proxy.newProxyInstance(
            ClassLoader.getSystemClassLoader(),
            arrayOf((type.classifier as KClass<*>).java)
        ) { proxy, method, args ->
            val sargs = args ?: emptyArray()
            //return wasm.evalWASMFunction(nfunc.name, *sargs)
            if (funcName != null) {
            } else {
                TODO("Not implemented invoke indirect yet")
                //wasm.invokeIndirect(address, *sargs)
            }
        } as T
    }
    */
}

class AndroidWASMExecutor(val context: Context) : Closeable {
    private val sandbox: JavaScriptSandbox by lazy {
        //runBlocking { JavaScriptSandbox.createConnectedInstanceAsync(context).await(mainExecutor) }
        JavaScriptSandbox.createConnectedInstanceAsync(context).get()
    }

    private val _js by lazy {
        sandbox.createIsolate(IsolateStartupParameters()).also { js ->
            if (sandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_CONSOLE_MESSAGING)) {
                js.setConsoleCallback {
                    println("JS: $it")
                }
            }
        }
    }

    private val js by lazy {
        _js.also {
            _js.evaluateJavaScriptAsync(//language:js
                """
            // note: `buffer` arg can be an ArrayBuffer or a Uint8Array
            globalThis.bufferToHex = (buffer) => {
                if (!globalThis.hexTable) {
                    globalThis.hexTable = Array.from({ length: 256 }, (_, i) => i.toString(16).padStart(2, '0') );
                }
                const byteToHex = globalThis.hexTable;
                const buff = new Uint8Array(buffer);
                const hexOctets = new Array(buff.length);
                for (let i = 0; i < buff.length; ++i) hexOctets[i] = byteToHex[buff[i]];
                return hexOctets.join("");
            };
            function charCodeToHexDigit(code) {
                if (code >= 48 && code <= 57) return code - 48;
                if (code >= 65) return code - 65 + 10;
                if (code >= 97) return code - 97 + 10;
                return 0;
            }
            globalThis.hexToBuffer = (hex) => {
                const out = new Uint8Array(hex.length / 2);
                for (let n = 0; n < out.length; n++) {
                    const d1 = charCodeToHexDigit(hex.charCodeAt(n * 2 + 0));
                    const d2 = charCodeToHexDigit(hex.charCodeAt(n * 2 + 1));
                    out[n] = (d1 << 4) | d2;
                }
                return out;
            };
        """.trimIndent()).get()
        }
    }

    private fun execJs(code: String): String {
        return js.evaluateJavaScriptAsync(code).get()
    }

    private fun setJsBytes(name: String, bytes: ByteArray) {
        if (sandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROVIDE_CONSUME_ARRAY_BUFFER)) {
            val jsName = "wasmBytes"
            val res = js.provideNamedData(jsName, bytes)
            if (res) {
                execJs(" android.consumeNamedDataAsArrayBuffer('$jsName').then((value) => { globalThis.$name = value; });")
                return
            }
        }

        val hex = byteArrayHex(bytes)
        execJs("globalThis.$name = globalThis.hexToBuffer('$hex')")
        return
    }

    fun loadWASM(wasm: ByteArray) {
        val jsKey = "wasmBytes"
        setJsBytes(jsKey, wasm)
        execJs("""
        WebAssembly.compile(globalThis.$jsKey).then((module) => {
            const imports = {
                env: { abort: () => { throw new Error(); } },
                wasi_snapshot_preview1: { 
                    proc_exit: () => { throw new Error(); },
                    fd_close: () => { throw new Error(); },
                    fd_write: () => { throw new Error(); }, 
                    fd_seek: () => { throw new Error(); }, 
                },
            };
            globalThis.wasm = new WebAssembly.Instance(module, imports);
            globalThis.u8 = new Uint8Array(globalThis.wasm.exports.memory.buffer)
        })
        """.trimIndent())
    }

    fun allocBytes(size: Int): Int {
        return executeFunction("malloc", size) as Int
    }
    fun allocBytes(bytes: ByteArray): Int {
        val ptr = allocBytes(bytes.size)
        writeBytes(ptr, bytes)
        return ptr
    }
    fun freeBytes(vararg ptrs: Int) {
        for (ptr in ptrs) if (ptr != 0) executeFunction("free", ptr)
    }
    fun stackSave(): Int = executeFunction("stackSave") as Int
    fun stackRestore(ptr: Int): Unit { executeFunction("stackRestore", ptr) }
    fun stackAlloc(size: Int): Int = executeFunction("stackAlloc", size) as Int

    fun readBytes(address: Int, size: Int): ByteArray {
        val res = execJs("globalThis.bufferToHex(globalThis.u8.subarray($address, ${address + size}))")
        return hexStringToByteArray(res)
    }

    fun writeBytes(address: Int, data: ByteArray) {
        val jsKey = "wasmBytes"
        setJsBytes(jsKey, data)
        execJs("globalThis.u8.set($jsKey, $address)")
    }

    fun executeFunction(name: String, vararg args: Any?): Any? {
        return jsonParse(execJs("JSON.stringify(globalThis.wasm.exports.$name(${args.joinToString(", ")}))"))
    }

    override fun close() {
        js.close()
        sandbox.close()
    }

    private fun jsonParse(str: String): Any? {
        if (str.isEmpty()) return null
        if (str == "null" || str == "undefined") return null
        if (str == "true" || str == "false") return str.toBoolean()
        if (str.startsWith("\"")) TODO()
        if (str.startsWith("{")) TODO()
        if (str.startsWith("[")) TODO()
        if (str.startsWith("t") || str.startsWith("f") || str.startsWith("n") || str.startsWith("u")) TODO()
        if (str.startsWith("[")) TODO()
        return str.toInt()
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val data = ByteArray(s.length / 2)
        for (n in data.indices) {
            val d1 = s[n * 2 + 0].digitToInt(16)
            val d2 = s[n * 2 + 1].digitToInt(16)
            data[n] = ((d1 shl 4) or (d2)).toByte()
        }
        return data
    }
    private fun byteArrayHex(d: ByteArray): String {
        val out = StringBuilder(d.size * 2)
        for (b in d) {
            val byte = b.toInt() and 0xFF
            out.append(((byte ushr 4) and 0xF).digitToChar(16))
            out.append(((byte ushr 0) and 0xF).digitToChar(16))
        }
        return out.toString()
    }
}
