package korlibs.io.runtime.deno

import korlibs.io.*
import korlibs.io.file.*
import korlibs.io.runtime.*
import korlibs.io.runtime.node.*
import korlibs.io.stream.*
import korlibs.js.*
import korlibs.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.khronos.webgl.*
import org.w3c.dom.url.*
import kotlin.js.Promise


fun def(result: dynamic, vararg params: dynamic, nonblocking: Boolean = false): dynamic =
    jsObject("parameters" to params, "result" to result, "nonblocking" to nonblocking)

/*
val denoBase = Deno.dlopen<dynamic>(
    "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
    jsObject(
        "memcpy" to def("pointer", "buffer", "pointer", "usize"),
        "strlen" to def("i32", "pointer"),
        "malloc" to def("pointer", "usize"),
        "free" to def("void", "pointer"),
    )
).symbols
val denoBase2 = Deno.dlopen<dynamic>(
    "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
    jsObject(
        "memcpy" to def("pointer", "pointer", "buffer", "usize"),
    )
).symbols

val denoBaseSize = Deno.dlopen<dynamic>(
    "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
    jsObject(
        "memcpy" to def("usize", "usize", "usize", "usize"),
        "strlen" to def("i32", "usize"),
        "malloc" to def("usize", "usize"),
        "free" to def("void", "usize"),
    )
).symbols

fun DenoPointer.view() {
    Deno.UnsafePointer.create()
}

fun Deno_allocBytes(bytes: ByteArray): DenoPointer {
    val ptr = denoBase.malloc(bytes.size)
    denoBase2.memcpy(ptr, bytes, bytes.size)
    return ptr
}

fun Deno_free(ptr: DenoPointer) {
    denoBase.free(ptr)
}

fun DenoPointer.readStringz(): String {
    val len = strlen()
    return readBytes(len).decodeToString()
}

fun DenoPointer.strlen(): Int = denoBase.strlen(this)

fun DenoPointer.readBytes(size: Int): ByteArray {
    val data = ByteArray(size)
    denoBase.memcpy(data, this, size)
    return data
}

fun DenoPointer.writeBytes(data: ByteArray) {
    denoBase2.memcpy(this, data, data.size)
}
*/

external private val import: dynamic

object JsRuntimeDeno : JsRuntime() {
    override fun existsSync(path: String): Boolean = try {
        Deno.statSync(path)
        true
    } catch (e: dynamic) {
        false
    }

    override fun currentDir(): String {
        //val url = URL(import.meta.url)
        //console.log("Deno.mainModule", Deno.mainModule)
        //console.log("import.meta.url", import.meta.url)
        try {
            val str = URL(".", Deno.mainModule).pathname
            return if (Platform.isWindows) str.substring(1) else str
        } catch (e: dynamic) {
            return "."
        }
        //return Deno.cwd()
    }

    override fun env(key: String): String? = Deno.env.get(key)
    override fun envs() = jsObjectToMap(Deno.env.toObject())

    override fun openVfs(path: String): VfsFile {
        return DenoLocalVfs()[if (path == ".") currentDir() else path]
    }
}

class DenoLocalVfs : Vfs() {
    private fun getFullPath(path: String): String {
        return path.pathInfo.normalize()
    }

    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        val options = jsObject(
            "read" to mode.read,
            "write" to mode.write,
            "append" to mode.append,
            "truncate" to mode.truncate,
            //"create" to !mode.createIfNotExists,
            "create" to mode.write,
            "createNew" to mode.createIfNotExists,
            "mode" to "666".toInt(8)
        )
        val file = Deno.open(getFullPath(path), options).await()
        return DenoAsyncStreamBase(file).toAsyncStream()
    }

    override suspend fun listFlow(path: String): Flow<VfsFile> =
        Deno.readDir(getFullPath(path)).toFlow().map { VfsFile(this, "$path/${it.name}") }

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = try {
        Deno.mkdir(getFullPath(path), jsObject("recursive" to true)).await()
        true
    } catch (e: Throwable) {
        false
    }

    override suspend fun delete(path: String): Boolean = try {
        Deno.remove(getFullPath(path), jsObject("recursive" to false)).await()
        true
    } catch (e: Throwable) {
        false
    }

    override suspend fun rename(src: String, dst: String): Boolean = try {
        Deno.rename(getFullPath(src), getFullPath(dst)).await()
        true
    } catch (e: Throwable) {
        false
    }

    override suspend fun stat(path: String): VfsStat {
        return try {
            Deno.stat(getFullPath(path)).await().let {
                createExistsStat(
                    path, it.isDirectory, it.size.toLong(), it.dev.toLong(),
                    it.ino?.toLong() ?: -1L, it.mode?.toInt() ?: "777".toInt(8)
                )
            }
        } catch (e: Throwable) {
            createNonExistsStat(path)
        }
    }
}

class DenoAsyncStreamBase(val file: DenoFsFile) : AsyncStreamBase() {
    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
        file.seek(position.toDouble(), Deno.SeekMode.Start).await()
        val read = file.read(Uint8Array(buffer.asUint8Array().buffer, offset, len)).await()
        return read?.toInt() ?: -1
    }

    override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
        file.seek(position.toDouble(), Deno.SeekMode.Start).await()
        file.write(Uint8Array(buffer.asUint8Array().buffer, offset, len)).await()
    }

    override suspend fun setLength(value: Long) {
        file.truncate(value.toDouble()).await()
    }

    override suspend fun getLength(): Long {
        return file.stat().await().size.toLong()
    }

    override suspend fun close() {
        file.close()
    }
}

suspend fun <T> JSAsyncIterable<T>.toFlow(): Flow<T> = flow {
    val iterator = (this@toFlow.asDynamic())[Symbol_asyncIterator]
    val gen = iterator.call(this)
    //println(gen)
    while (true) {
        val prom = gen.next().unsafeCast<Promise<JSIterableResult<T>>>()
        val value = prom.await()
        if (value.done) break
        emit(value.value)
    }
}
