package korlibs.wasm

import android.content.*
import android.os.*
import android.webkit.*
import korlibs.encoding.*
import korlibs.io.android.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import kotlinx.coroutines.*

actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by AndroidWASMLib(content)

class AndroidWASMLib(content: ByteArray) : IWASMLib, BaseWASMLib(content) {
    private val androidContextOpt: Context? get() = _context?.androidContextOrNull()
    private val trace = false

    private val executor: AndroidWASMExecutor by lazy {
        AndroidWASMExecutor(
            androidContextOpt ?: error("Must call WASMLib.initOnce with an android context on it (withAndroidContext()) before calling any method"),
            trace
        ).also {
            it.loadWASM(content)
        }
    }

    override val isAvailable: Boolean get() = androidContextOpt != null

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

private class AndroidWASMExecutor(val context: Context, val trace: Boolean) : Closeable {
    private val js: JavascriptIsolate by lazy {
        JavascriptIsolate(context, trace)
    }

    fun loadWASM(wasm: ByteArray) {
        val jsKey = "wasmBytes"
        js.setBytes(jsKey, wasm)
        val result = js.runJs("""
            const imports = {
                env: { abort: () => { throw new Error(); } },
                wasi_snapshot_preview1: { 
                    proc_exit: () => { throw new Error(); },
                    fd_close: () => { throw new Error(); },
                    fd_write: () => { throw new Error(); }, 
                    fd_seek: () => { throw new Error(); }, 
                },
            };
            return WebAssembly.instantiate(JavaProxy.getJavaBytes('$jsKey'), imports).then((wasmI) => {
                //console.log('globalThis.wasm: ' + JSON.stringify(wasmI.instance))
                globalThis.wasm = wasmI.instance;
                //console.log('globalThis.wasm: ' + JSON.stringify(Object.keys(globalThis.wasm.exports)))
                globalThis.u8 = new Uint8Array(globalThis.wasm.exports.memory.buffer)
                return 'ok';
            });
        """.trimIndent())
        if (trace) println("loadWASM.result=$result")
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
        js.setBytes("bytes", byteArrayOf())
        js.runJs("JavaProxy.setJavaBytes('bytes', globalThis.u8.subarray($address, ${address + size}))")
        return (js.getBytes("bytes") ?: byteArrayOf()).also {
            if (trace) println("readBytes[address=$address, size=$size]: ${it.hex}")
        }
    }

    fun writeBytes(address: Int, data: ByteArray) {
        val jsKey = "wasmBytes"
        js.setBytes(jsKey, data)
        js.runJs("globalThis.u8.set(JavaProxy.getJavaBytes('$jsKey'), $address)")
    }

    fun executeFunction(name: String, vararg args: Any?): Any? {
        if (trace) println("executingFunction[name=$name]: ${args.toList()}...")
        val res = js.runJs("return globalThis.wasm.exports.$name(${args.joinToString(", ")})") ?: "null"
        if (trace) println("executeFunction[name=$name]: ${args.toList()} : res=$res : clazz=${if (res != null) res::class else null}")
        return res
    }

    override fun close() {
        js.close()
    }

    private fun jsonParse(str: String): Any? {
        return Json.parse(str)
        //if (str.isEmpty()) return null
        //if (str == "null" || str == "undefined") return null
        //if (str == "true" || str == "false") return str.toBoolean()
        //if (str.startsWith("\"")) TODO()
        //if (str.startsWith("{")) TODO()
        //if (str.startsWith("[")) TODO()
        //if (str.startsWith("t") || str.startsWith("f") || str.startsWith("n") || str.startsWith("u")) TODO()
        //if (str.startsWith("[")) TODO()
        //return str.toInt()
    }
}

private class JavascriptIsolate(val context: Context, val trace: Boolean) {
    private fun <T> runOnUiThread(block: () -> T): T {
        //return if (Looper.getMainLooper().isCurrentThread) {
        //    TODO()
        //} else {
            return runBlocking {
                val out = CompletableDeferred<T>()
                if (trace) println("BEFORE RUN IN MAIN LOOPER!")
                Handler(context.mainLooper).post {
                //Handler(Looper.getMainLooper()).post {
                    if (trace) println("RUN IN MAIN LOOPER!")
                    out.complete(block().also {
                        if (trace) println("RES RUN IN MAIN LOOPER! = $it")
                    })
                }
                if (trace) println("WAITING!")
                out.await().also {
                    if (trace) println("WAITED! $it")
                }
            }
        //}
    }


    private val javaProxy = JavaProxy()

    private val webView: WebView = run {
        runOnUiThread {
            WebView(context).also {
                it.settings.javaScriptEnabled = true
                it.addJavascriptInterface(javaProxy, "javaProxy")
                it.evaluateJavascript(
                    """
                        const base64abc = [
                            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
                            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "/"
                        ];
                        const base64codes = [
                        	255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                        	255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
                        	255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 62, 255, 255, 255, 63,
                        	52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 255, 255, 255, 0, 255, 255,
                        	255, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                        	15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 255, 255, 255, 255, 255,
                        	255, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
                        	41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
                        ];
                        
                        function getBase64Code(charCode) {
                        	if (charCode >= base64codes.length) {
                        		throw new Error("Unable to parse base64 string.");
                        	}
                        	const code = base64codes[charCode];
                        	if (code === 255) {
                        		throw new Error("Unable to parse base64 string.");
                        	}
                        	return code;
                        }

                        function bytesToBase64(bytes) {
                        	let result = '', i, l = bytes.length;
                        	for (i = 2; i < l; i += 3) {
                        		result += base64abc[bytes[i - 2] >> 2];
                        		result += base64abc[((bytes[i - 2] & 0x03) << 4) | (bytes[i - 1] >> 4)];
                        		result += base64abc[((bytes[i - 1] & 0x0F) << 2) | (bytes[i] >> 6)];
                        		result += base64abc[bytes[i] & 0x3F];
                        	}
                        	if (i === l + 1) { // 1 octet yet to write
                        		result += base64abc[bytes[i - 2] >> 2];
                        		result += base64abc[(bytes[i - 2] & 0x03) << 4];
                        		result += "==";
                        	}
                        	if (i === l) { // 2 octets yet to write
                        		result += base64abc[bytes[i - 2] >> 2];
                        		result += base64abc[((bytes[i - 2] & 0x03) << 4) | (bytes[i - 1] >> 4)];
                        		result += base64abc[(bytes[i - 1] & 0x0F) << 2];
                        		result += "=";
                        	}
                        	return result;
                        }

                        function base64ToBytes(str) {
                        	if (str.length % 4 !== 0) {
                        		throw new Error(`Unable to parse base64 string [0]. str.length=` + str.length);
                        	}
                        	const index = str.indexOf("=");
                        	if (index !== -1 && index < str.length - 2) {
                        		throw new Error("Unable to parse base64 string [1].");
                        	}
                        	let missingOctets = str.endsWith("==") ? 2 : str.endsWith("=") ? 1 : 0,
                        		n = str.length,
                        		result = new Uint8Array(3 * (n / 4)),
                        		buffer;
                        	for (let i = 0, j = 0; i < n; i += 4, j += 3) {
                        		buffer =
                        			getBase64Code(str.charCodeAt(i)) << 18 |
                        			getBase64Code(str.charCodeAt(i + 1)) << 12 |
                        			getBase64Code(str.charCodeAt(i + 2)) << 6 |
                        			getBase64Code(str.charCodeAt(i + 3));
                        		result[j] = buffer >> 16;
                        		result[j + 1] = (buffer >> 8) & 0xFF;
                        		result[j + 2] = buffer & 0xFF;
                        	}
                        	return result.subarray(0, result.length - missingOctets);
                        }
                        
                        globalThis.JavaProxy = {};
                        (() => {
                            globalThis.JavaProxy.getJavaBytes = function(key) { return base64ToBytes(javaProxy.getStr(key)) };
                            globalThis.JavaProxy.setJavaBytes = function(key, bytes) { javaProxy.setStr(key, bytesToBase64(bytes)) };
                        })()
                    """.trimIndent()
                ) {
                }
            }
        }
    }

    fun close() {
    }

    fun getBytes(key: String): ByteArray? = javaProxy.getBytes(key)
    fun setBytes(key: String, value: ByteArray?) = javaProxy.setBytes(key, value)
    suspend fun runJsSuspend(str: String): Any? {
        //println("webView.webViewLooper.isCurrentThread=${webView.webViewLooper.isCurrentThread}")
        if (trace) println("runJsSuspend[1]")
        runOnUiThread {
            javaProxy.deferred = CompletableDeferred<String?>()
            webView.evaluateJavascript("""
                Promise.resolve((async () => {
                    $str
                })()).then((it) => {
                    javaProxy.done(JSON.stringify(it));
                });
            """.trimIndent()) {

            }
        }
        return Json.parse(javaProxy.deferred.await().also {
            if (trace) println("runJsSuspend[2]: '$it'")
        } ?: "null")
    }
    fun runJs(str: String): Any? = runBlocking { runJsSuspend(str) }


    @Suppress("unused")
    class JavaProxy {
        var deferred = CompletableDeferred<String?>()
        val binaries = LinkedHashMap<String, ByteArray?>()
        fun getBytes(key: String): ByteArray? = binaries[key]
        fun setBytes(key: String, value: ByteArray?) = run { binaries[key] = value }
        @JavascriptInterface
        fun getStr(key: String): String? {

            //return binaries[key]?.toString(Charsets.UTF_8)
            return binaries[key]?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP); }
        }
        @JavascriptInterface
        fun setStr(key: String, value: String?): Unit = run {
            binaries[key] = value?.let {
                android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
            }
        }
        @JavascriptInterface
        fun done(json: String?) {
            deferred.complete(json)
        }
    }
}
