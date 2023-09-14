package korlibs.wasm

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.stream.*
import kotlin.test.*

class WasmJVMTest : WasmTest() {
    override val interpreter: Boolean = false

    @Test
    fun testJvm() = suspendTest {
    }

    override suspend fun createModuleRuntime(file: String, loadTrace: Boolean, memPages: Int, codeTrace: Boolean, validate: Boolean): WasmRuntime {
        val reader = WasmReaderBinary().also { it.doTrace = loadTrace }.read(resourcesVfs[file].readBytes().openSync())
        val wasmModule = WasmRunJVMOutput().also { it.trace = codeTrace; it.validate = validate }.generate(reader.toModule())
        return wasmModule
    }

}
