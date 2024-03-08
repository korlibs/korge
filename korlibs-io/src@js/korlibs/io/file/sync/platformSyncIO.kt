package korlibs.io.file.sync

import korlibs.io.*
import korlibs.io.lang.*
import korlibs.memory.*
import korlibs.platform.*
import org.khronos.webgl.*

private external val Deno: dynamic

@JsName("Array")
private external class JsArray<T> {
    companion object {
        fun from(value: dynamic): Array<dynamic>
    }
}
private fun Array_from(value: dynamic): Array<dynamic> = JsArray.from(value)

external interface JSIterable<T>
private fun <T> JSIterable<T>.toArray(): Array<T> {
    return Array_from(this)
}

open class DenoSyncIO(val caseSensitive: Boolean) : SyncIO {
    companion object : DenoSyncIO(caseSensitive = true)

    override fun realpath(path: String): String = Deno.realPathSync(path)
    override fun readlink(path: String): String? = Deno.readLinkSync(path)
    override fun writelink(path: String, link: String?) { Deno.linkSync(path, link) }
    override fun stat(path: String): SyncIOStat? = runCatching {
        //try {
        //println("STAT: '$path'")
        val stat = Deno.statSync(path)// DenoFileInfo
        //.toSyncIOStat(path)
        //} catch (e: Throwable) {
        //    e.printStackTrace()
        //    throw e
        //}
        SyncIOStat(path, stat.isDirectory, stat.size.unsafeCast<Number>().toLong())
    }.getOrNull()
    override fun mkdir(path: String): Boolean = runCatching { Deno.mkdirSync(path) }.isSuccess
    override fun rmdir(path: String): Boolean = runCatching { Deno.removeSync(path) }.isSuccess
    override fun delete(path: String): Boolean = runCatching { Deno.removeSync(path) }.isSuccess
    override fun list(path: String): List<String> = Deno.readDirSync(path).unsafeCast<JSIterable<dynamic>>().toArray().map { it.name }
    override fun readAllBytes(path: String): ByteArray = Deno.readFileSync(path)
    override fun writeAllBytes(path: String, data: ByteArray) = Deno.writeFileSync(path, data)

    //fun DenoFileInfo.toSyncIOStat(path: String): SyncIOStat = SyncIOStat(path, this.isDirectory, this.size.toLong())

    override fun open(path: String, mode: String): SyncIOFD {
        return DenoSyncIOFD(path, mode)
    }
}

class DenoSyncIOFD(val path: String, val mode: String) : SyncIOFD {
    val file = Deno.openSync(path, jsObject("read" to mode.contains("r"), "write" to (mode.contains("+") || mode.contains("w"))))

    override var length: Long
        get() {
            val pos = position
            val total = file.seekSync(0.0, Deno.SeekMode.End).toLong()
            position = pos
            return total
        }
        set(value) { file.truncateSync(value.toDouble()) }
    override var position: Long
        get() = file.seekSync(0.0, Deno.SeekMode.Current).toLong()
        set(value) { file.seekSync(value.toDouble(), Deno.SeekMode.Start) }

    override fun write(data: ByteArray, offset: Int, size: Int): Int =
        file.writeSync((data.unsafeCast<Uint8Array>()).subarray(offset, size)).toInt()

    override fun read(data: ByteArray, offset: Int, size: Int): Int =
        file.readSync((data.unsafeCast<Uint8Array>()).subarray(offset, size))?.toInt() ?: error("Couldn't read")

    override fun close() {
        file.close()
    }
}

internal actual fun platformSyncIO(caseSensitive: Boolean): SyncIO {
    return when {
        Platform.isJsDenoJs -> DenoSyncIO(caseSensitive)
        //Platform.isJsNodeJs -> NodeSyncIO
        else -> unsupported("Not supported SyncIO on browser")
    }
}
