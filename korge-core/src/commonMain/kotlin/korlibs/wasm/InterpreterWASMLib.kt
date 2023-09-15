package korlibs.wasm

import korlibs.io.stream.*

open class InterpreterWASMLib(content: ByteArray) : BaseWASMLib(content) {
    //val interpreter = WasmRunInterpreter(WasmReaderBinary().read(content.openSync()).toModule())
    val interpreter = WasmRunInterpreter(WasmReaderBinary().read(content.openSync()).toModule())

    override fun invokeFunc(name: String, vararg params: Any?): Any? = interpreter.invoke(name, *params)
    override fun readBytes(pos: Int, size: Int): ByteArray = interpreter.readBytes(pos, size)
    override fun writeBytes(pos: Int, data: ByteArray) = interpreter.writeBytes(pos, data)
}
