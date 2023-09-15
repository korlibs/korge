package korlibs.wasm

actual open class WASMLib actual constructor(content: ByteArray) : IWASMLib by WASMWASMLib(content)

class WASMWASMLib(content: ByteArray) : IWASMLib, BaseWASMLib(content)

