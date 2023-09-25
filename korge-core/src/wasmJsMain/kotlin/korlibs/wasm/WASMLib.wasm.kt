package korlibs.wasm

import korlibs.io.*
import korlibs.io.util.*
import korlibs.io.wasm.*
import org.khronos.webgl.*

actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by WASMWASMLib(content)

//class WASMWASMLib(content: ByteArray) : IWASMLib, BaseWASMLib(content)

class WASMWASMLib(content: ByteArray) : IWASMLib, BaseWASMLib(content) {
    private var _wasmExports: JsAny? = null

    private val wasmExports: JsAny?
        get() {
            if (_wasmExports == null) {
                _wasmExports = try {
                    val module = WebAssembly.Module(content.toInt8Array())
                    val dummyFunc = { println("WASMWASMLib.dummyFunc") }
                    val imports = jsObject(
                        "env" to jsObject(
                            "abort" to dummyFunc,
                        ),
                        "wasi_snapshot_preview1" to jsObject(
                            "proc_exit" to dummyFunc,
                            "fd_close" to dummyFunc,
                            "fd_write" to dummyFunc,
                            "fd_seek" to dummyFunc,
                        )
                    )
                    WebAssembly.Instance(module, imports).exports
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
            }
            return _wasmExports
        }

    private val mem: ArrayBuffer by lazy {
        wasmExports?.getAny("memory")?.getAny("buffer")?.unsafeCast<ArrayBuffer>()
            ?: error("Can't find memory.buffer")
    }
    private val u8: Uint8Array by lazy {
        Uint8Array(mem)
    }
    private val dataView: DataView by lazy {
        DataView(mem)
    }

    override fun readBytes(pos: Int, size: Int): ByteArray {
        val out = ByteArray(size)
        val u8 = this.u8
        for (n in out.indices) out[n] = u8[pos + n]
        return out
    }

    override fun writeBytes(pos: Int, data: ByteArray) {
        for (n in data.indices) u8[pos + n] = data[n]
    }

    override fun invokeFunc(name: String, vararg params: Any?): Any? {
        val result = wasmExports!!.dynamicInvoke(name.toJsString(), params.toList().mapToJsArray {
            when (it) {
                is Number -> it.toDouble().toJsNumber()
                is String -> it.toJsString()
                else -> TODO("param: $it")
            }
        })
        return when (result) {
            is JsNumber -> result.toDouble()
            else -> TODO("result: $result")
        }
    }

    override fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? {
        TODO()
    }

    override fun close() {
        super.close()
    }
}

@JsName("WebAssembly")
private external class WebAssembly {
    class Instance(module: Module, imports: JsAny) {
        val exports: JsAny
        val memory: ArrayBuffer
    }

    class Module(data: ArrayBufferView)
}
