package korlibs.wasm

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.io.stream.*
import korlibs.memory.*
import kotlin.test.*

class WasmJVMTest : WasmTest() {
    override val interpreter: Boolean = false

    @Test
    fun testJvm() = suspendTest {
    }
}

open class WasmTest {
    open val interpreter = true

    @Test
    fun testMandelbrot() = suspendTest {
        val module = createModuleRuntime("wasm/mandelbrot.wasm", codeTrace = false)
        //println("instructionsExecuted: ${module.instructionsExecuted}")
        //for (n in 0 until 10) {
        run {
            module.invoke("update", 100, 100, 5)
            //for ((op, count) in module.instructionsHistoriogram.withIndex().sortedByDescending { it.value }) if (count != 0) println("op[${op.hex}] = $count")
            //println("instructionsExecuted: ${module.instructionsExecuted}")
            //module.trace = true
        }
    }
    @Test
    fun testMp3() = suspendTest {
        val MINIMP3_MAX_SAMPLES_PER_FRAME = (1152*2)
        val MP3_DEC_SIZE = 6668

        val module = createModuleRuntime("wasm/mp3.wasm", codeTrace = false, validate = false)
        //module.usedClassMemory=30297
        println("module.usedClassMemory=${module.usedClassMemory}")
        val mp3Bytes = resourcesVfs["wasm/demo.mp3"].readBytes()
        //val mp3dec_init: (ptr: Int) -> Unit by func()
        //val mp3dec_decode_frame: (dec: Int, mp3: Int, mp3_bytes: Int, pcm: Int, info: Int) -> Int by func()

        //for (n in 0 until 1000000) {
        //for (n in 0 until 100) {
        run {
            val stack = module.stackSave()
            val decoder = module.stackAlloc(MP3_DEC_SIZE)
            val mp3Data = module.stackAllocAndWrite(mp3Bytes)
            val samplesData = module.stackAlloc(MINIMP3_MAX_SAMPLES_PER_FRAME * 2)
            val info = module.stackAlloc(6 * 4)
            val result = module("mp3dec_decode_frame", decoder, mp3Data, mp3Bytes.size, samplesData, info) as Int
            val infos = module.readBytes(info, 6 * 4).getS32ArrayLE(0, 6).toList()
            println("mp3dec_decode_frame(decoder=$decoder, mp3Data=$mp3Data, mp3Bytes.size=${mp3Bytes.size}, samplesData=$samplesData, info=$info) ::: result=$result, infos=$infos")
            module.stackRestore(stack)
        }
    }
    @Test
    fun testWebp() = suspendTest {
        //for (n in 0 until 10000) createJIT("webp.wasm", codeTrace = false, validate = false)
        //val module = createModuleRuntime("wasm/webp.wasm", loadTrace = false, codeTrace = true, validate = false)
        val module = createModuleRuntime("wasm/webp.wasm", loadTrace = false, codeTrace = false, validate = false)
        //val module = createModuleRuntime("wasm/webp-O0-full.wasm", loadTrace = true, codeTrace = false, validate = false)
        //val module = createModuleRuntime("wasm/webp-O0-full.wasm", loadTrace = false, codeTrace = false, validate = false)
        //val module = createJIT("webp.wasm", codeTrace = true, validate = true)
        //val module = createJIT("webp.wasm", codeTrace = false)
        val webpBytes = resourcesVfs["wasm/webp.webp"].readBytes()
        val ptr = module.invoke("malloc", webpBytes.size) as Int
        module.memory.setArrayInt8(ptr, webpBytes)

        if (interpreter) {
            println("SKIPPING Webp decoding with WASM interpreter")
        } else {
            val memTemp = module.allocAndWrite(ByteArray(16))
            val output = module.invoke("decode", ptr, webpBytes.size, memTemp, memTemp + 4) as Int
            val width = module.memory.getUnalignedInt32(memTemp + 0)
            val height = module.memory.getUnalignedInt32(memTemp + 4)
            assertNotEquals(0, output)
            assertEquals(100792, output)
            assertEquals("32x32", "${width}x${height}")

            //for (n in 0 until 100) module.invoke("decode", ptr, webpBytes.size, memTemp, memTemp + 4) as Int
        }

        //repeat(100) {
        run {
            val infoPtr = module.invoke("get_info", ptr, webpBytes.size) as Int
            val success = module.memory.getUnalignedInt32(infoPtr + 0)
            val width = module.memory.getUnalignedInt32(infoPtr + 4)
            val height = module.memory.getUnalignedInt32(infoPtr + 8)
            assertEquals("1,32x32", "$success,${width}x${height}")
        }

        //println(module.call("malloc", webpBytes))
        //println(module.call("malloc", 102400))
        //println(module.call("malloc", 1024))
        //println(module.call("free", 0))
        //println(module.call("stackAlloc", 16))
        //println(module.call("stackAlloc", 16))
        //println(module.call("stackAlloc", 16))
        //interpreter.interpret(reader.getFuncByName("malloc"), 16)

    }

    @Test
    fun testFib() = suspendTest {
        val wasmModule = createModuleRuntime("wasm/fib.wasm")
        wasmModule.register("log", "integer") {
            //println(it[0])
        }
        assertEquals(987, wasmModule.invoke("fib", 16))
    }

    @Test
    fun testFib64() = suspendTest {
        val wasmModule = createModuleRuntime("wasm/fib64.wasm", codeTrace = false)
        assertEquals(987L, wasmModule.invoke("fib", 16L))
    }

    @Test
    fun testSample1() = suspendTest {
        val wasmModule = createModuleRuntime("wasm/sample1.wasm", codeTrace = false)
        wasmModule.register("log", "integer") { println(it[0]) }
        wasmModule.register("log", "boolean") { println(it[0]) }
        wasmModule.invoke("sample1")
    }

    @Test
    fun testSample2() = suspendTest {
        val wasmModule = createModuleRuntime("wasm/sample2.wasm", codeTrace = false)
        wasmModule.register("log", "integer") { println(it[0]) }
        wasmModule.register("log", "boolean") { println(it[0]) }
        wasmModule.invoke("sample2")
    }

    @Test
    fun testMemops32() = suspendTest {
        val log = arrayListOf<Any?>()
        val wasmModule = createModuleRuntime("wasm/memops32.wasm", codeTrace = false)
        wasmModule.register("log", "integer") { log += it[0]; Unit }
        wasmModule.register("log", "float") { log += it[0]; Unit }
        wasmModule.register("log", "double") { log += it[0]; Unit }
        wasmModule.register("log", "long") { log += it[0]; Unit }
        wasmModule.register("log", "string") { log += WasmRuntime.read_string(it[0] as Int, this); Unit }
        wasmModule.invoke("main")
        assertEquals(listOf<Any?>(
            3, 0, 1, 2, 3, 5f, 7, 8, 9, 1, 2, 3, 4, 6f, 8, 9, 10, 0, -11, 245, "hello", "worlds"
        ), log)
    }

    @Test
    fun testMemops() = suspendTest {
        val log = arrayListOf<Any?>()
        val wasmModule = createModuleRuntime("wasm/memops.wasm", codeTrace = false)
        wasmModule.register("log", "integer") { log += it[0]; Unit }
        wasmModule.register("log", "float") { log += it[0]; Unit }
        wasmModule.register("log", "double") { log += it[0]; Unit }
        wasmModule.register("log", "long") { log += it[0]; Unit }
        wasmModule.register("log", "string") { log += WasmRuntime.read_string(it[0] as Int, this); Unit }
        wasmModule.invoke("main")
        assertEquals(listOf<Any?>(0, 1, 2, 3, 4L, 5f, 6.0, 7, 8, 9, 10L, 1, 2, 3, 4, 5L, 6f, 7.0, 8, 9, 10, 11L, 0, -11, 245, "hello", "worlds", 37660L), log)
    }

    @Test
    fun testFuncCall() = suspendTest {
        val wasmModule = createModuleRuntime("wasm/func_call.wasm")
        assertEquals((3 + 7) * (7 - 9), wasmModule.invoke("myfunc", 3, 7, 9))
    }

    @Test
    fun testMalloc() = suspendTest {
        //val wasmModule = createJIT("malloc.wasm", codeTrace = true)
        val wasmModule = createModuleRuntime("wasm/malloc.wasm", codeTrace = false)

        val res1 = wasmModule.invoke("malloc", 16)
        val res2 = wasmModule.invoke("malloc", 1024)
        val res3 = wasmModule.invoke("malloc", 1024)
        assertEquals(35520, res1)
        assertEquals(35552, res2)
        assertEquals(36592, res3)
    }

    @Test
    fun testLocalGet0() = suspendTest {
        val module = createModuleRuntime("wasm/test-core/local_get0.wasm", loadTrace = false)
        val logs = arrayListOf<Int>()
        module.register("log", "integer") { logs += it.first() as Int; Unit }
        module.register("log", "boolean") { logs += (it.first() as Int); Unit }
    }

    protected open suspend fun createModuleRuntime(file: String, loadTrace: Boolean = false, memPages: Int = 10, codeTrace: Boolean = false, validate: Boolean = true): WasmRuntime {
        if (interpreter) {
            return createModuleRuntimeInterpreter(file, loadTrace, memPages, codeTrace, validate)
        } else {
            return createModuleRuntimeJVM(file, loadTrace, memPages, codeTrace, validate)
        }
    }

    protected suspend fun createModuleRuntimeJVM(file: String, loadTrace: Boolean = false, memPages: Int = 10, codeTrace: Boolean = false, validate: Boolean = true): WasmRuntime {
        val reader = WasmReaderBinary().also { it.doTrace = loadTrace }.read(resourcesVfs[file].readBytes().openSync())
        val wasmModule = WasmRunJVMOutputExt().also { it.trace = codeTrace; it.validate = validate }.generate(reader.toModule())
        return wasmModule
    }

    protected suspend fun createModuleRuntimeInterpreter(file: String, loadTrace: Boolean = false, memPages: Int = 10, codeTrace: Boolean = false, validate: Boolean = true): WasmRuntime {
        val reader = WasmReaderBinary().doTrace(loadTrace).read(resourcesVfs[file].readBytes().openSync())
        //return WasmRunInterpreter(reader.toModule(), memPages).also { it.trace = codeTrace }.initGlobals().also { int ->
        return WasmRunInterpreter(reader.toModule(), memPages)
            .also { it.trace = codeTrace }.initGlobals().also { int ->
                int.register("env", "abort") {
                    val (msg, file, line, column) = it.map { it as Int }
                    error("abort: msg='${int.readStringz16(msg)}', file='${int.readStringz16(file as Int)}', line=$line, column=$column")
                }
                int.register("wasi_snapshot_preview1", "proc_exit") { TODO("proc_exit: ${it.toList()}") }
                int.register("wasi_snapshot_preview1", "fd_close") { TODO("fd_close: ${it.toList()}") }
                int.register("wasi_snapshot_preview1", "fd_write") { TODO("fd_write: ${it.toList()}") }
                int.register("wasi_snapshot_preview1", "fd_seek") { TODO("fd_seek: ${it.toList()}") }
            }
    }
}
