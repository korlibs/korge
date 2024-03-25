package korlibs.wasm

/*
open class DenoWASMLib(content: ByteArray) : BaseWASMLib(content) {
    val runner = DenoWasmProcessStdin.open(content)

    override fun writeBytes(pos: Int, data: ByteArray) = runner.writeBytes(pos, data)
    override fun readBytes(pos: Int, size: Int): ByteArray = runner.readBytes(pos, size)
    override fun allocBytes(bytes: ByteArray): Int = runner.allocAndWrite(bytes)
    override fun freeBytes(vararg ptrs: Int) = runner.free(*ptrs)
    override fun stackSave(): Int = runner.stackSave()
    override fun stackRestore(ptr: Int) = runner.stackRestore(ptr)
    override fun stackAlloc(size: Int): Int = runner.stackAlloc(size)
    override fun stackAllocAndWrite(bytes: ByteArray): Int = runner.stackAllocAndWrite(bytes)
    override fun invokeFunc(name: String, vararg params: Any?): Any? = runner.executeFunction(name, *params)
    override fun invokeFuncIndirect(address: Int, vararg params: Any?): Any? = runner.executeFunctionIndirect(address, *params)
    override fun close() = runner.close()
}
*/
