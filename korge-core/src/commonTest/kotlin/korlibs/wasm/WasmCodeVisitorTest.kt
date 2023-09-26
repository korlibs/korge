package korlibs.wasm

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.stream.*
import korlibs.memory.*
import kotlin.test.*

class WasmCodeVisitorTest {
    @Test
    fun test() = suspendTest {
        val module = WasmReaderBinary()
            .doTrace(false)
            .read(resourcesVfs["wasm/webp.wasm"].readBytes().openSync())
            .toModule()

        //val newInterpreter = WasmRunInterpreterNew(module)
        val newInterpreter = WasmRunInterpreter(module).initGlobals()
        //val newInterpreter = WasmRunInterpreterNew(module).initGlobals()

        //for (func in module.functions) {
        //    newInterpreter.compile(func)
        //}

        //val module = createJIT("webp.wasm", codeTrace = true, validate = true)
        //val module = createJIT("webp.wasm", codeTrace = false)
        val webpBytes = resourcesVfs["wasm/webp.webp"].readBytes()
        val ptr = newInterpreter.invoke("malloc", webpBytes.size) as Int
        newInterpreter.memory.setArrayInt8(ptr, webpBytes)

        //repeat(100) {
        run {
            val infoPtr = newInterpreter.invoke("get_info", ptr, webpBytes.size) as Int
            val success = newInterpreter.memory.getS32(infoPtr + 0)
            val width = newInterpreter.memory.getS32(infoPtr + 4)
            val height = newInterpreter.memory.getS32(infoPtr + 8)
            assertEquals("1,32x32", "$success,${width}x${height}")
        }
    }
}
