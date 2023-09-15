package korlibs.wasm

//actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by DenoWASMLib(content)
actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by InterpreterWASMLib(content)
