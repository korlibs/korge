package korlibs.io.file.sync

import korlibs.io.file.std.*
import korlibs.io.stream.*
import kotlin.contracts.*

//typealias SyncIOAPI = korlibs.io.core.SyncIOAPI
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class SyncIOAPI

@SyncIOAPI
internal expect fun platformSyncIO(caseSensitive: Boolean): SyncIO

@SyncIOAPI
val platformSyncIO: SyncIO = platformSyncIO(caseSensitive = true)

@SyncIOAPI
val platformSyncIOCaseInsensitive: SyncIO = platformSyncIO(caseSensitive = false)

private fun splitPathName(fullPath: String): Pair<String, String> {
    val normalized = fullPath.replace('\\', '/')
    return normalized.substringBeforeLast('/') to normalized.substringAfterLast('/')
}

class SyncIOFile(val impl: SyncIO, val fullPath: String) {
    companion object {
        @SyncIOAPI
        operator fun invoke(path: String): SyncIOFile = SyncIOFile(platformSyncIO, path)
    }

    operator fun get(child: String): SyncIOFile = SyncIOFile(impl, "$fullPath/$child")

    private val pathName by lazy { splitPathName(fullPath) }
    val path: String get() = pathName.first
    val name: String get() = pathName.second
    val parent: SyncIOFile get() = SyncIOFile(impl, path)

    //val absolutePath: String get() = TODO()
    val absolutePath: String get() = fullPath

    fun stat() = impl.stat(fullPath)

    val isDirectory: Boolean get() = stat()?.isDirectory == true
    fun exists(): Boolean = stat() != null

    fun readBytes(): ByteArray = impl.readAllBytes(fullPath)
    fun readString(): String = impl.readString(fullPath)

    fun writeBytes(content: ByteArray) = impl.writeAllBytes(fullPath, content)
    fun writeString(content: String) = impl.writeString(fullPath, content)
    fun writeText(content: String) = impl.writeString(fullPath, content)

    fun list(): List<SyncIOFile> = impl.list(fullPath).map { this[it] }

    fun mkdir() = impl.mkdir(fullPath)
    fun rmdir() = impl.rmdir(fullPath)
    fun delete() = impl.delete(fullPath)
    fun realpath() = impl.realpath(fullPath)
    fun readlink() = impl.readlink(fullPath)
    fun writelink(link: String?) = impl.writelink(fullPath, link)
    fun open(mode: String) = impl.open(fullPath, mode)

    //override fun toString(): String = "SyncIOFile($fullPath)[$impl]"
    override fun toString(): String = "SyncIOFile($fullPath)"
}

fun SyncIO.file(path: String): SyncIOFile = SyncIOFile(this, path)

interface SyncIO {
    @SyncIOAPI
    companion object : SyncIO by platformSyncIO

    open fun realpath(path: String): String = path
    open fun readlink(path: String): String? = null
    open fun writelink(path: String, link: String?): Unit = TODO()
    open fun open(path: String, mode: String): SyncIOFD = TODO()
    open fun stat(path: String): SyncIOStat? = TODO()
    open fun mkdir(path: String): Boolean = TODO()
    open fun rmdir(path: String): Boolean = TODO()
    open fun delete(path: String): Boolean = TODO()
    open fun list(path: String): List<String> = TODO()

    fun readString(path: String): String = readAllBytes(path).decodeToString()
    fun writeString(path: String, data: String) {
        writeAllBytes(path, data.encodeToByteArray())
    }
    fun readAllBytes(path: String): ByteArray {
        return open(path, "r").use { file ->
            ByteArray(file.length.toInt()).also {
                file.read(it)
            }
        }
    }

    fun writeAllBytes(path: String, data: ByteArray) {
        mkdir(splitPathName(path).first)
        //open(path, "wb").use { it.write(data) }
        open(path, "rw").use { it.write(data) }
    }
    fun write(path: String, data: ByteArray) {
        writeAllBytes(path, data)
    }

    open fun exec(commands: List<String>, envs: Map<String, String>, cwd: String = "."): SyncExecProcess {
        TODO()
    }
}

open class SyncExecProcess(
    val stdin: SyncOutputStream,
    val stdout: SyncInputStream,
    val stderr: SyncInputStream,
) : SyncIOCloseable {
    open val exitCode: Int get() = TODO()

    open fun destroy() {
    }

    override fun close() {
    }
}

class MemorySyncIO : SyncIO {
    private val tree = MemoryNodeTree()
    private val root = tree.rootNode

    class NodeSyncIOFD(val node: MemoryNodeTree.Node) : SyncIOFD {
        val bytes get() = node.bytes ?: byteArrayOf()
        var positionInt: Int = 0
        override var length: Long
            get() = bytes.size.toLong()
            set(value) { node.bytes = bytes.copyOf(value.toInt()) }
        override var position: Long
            get() = positionInt.toLong()
            set(value) { positionInt = value.toInt() }
        val available: Long get() = length - position
        override fun write(data: ByteArray, offset: Int, size: Int): Int {
            length = maxOf(length, position + size)
            data.copyInto(bytes, positionInt, offset, offset + size)
            positionInt += size
            return size
        }

        override fun read(data: ByteArray, offset: Int, size: Int): Int {
            val rsize = minOf(size, available.toInt())
            data.copyInto(data, offset, positionInt, positionInt + rsize)
            position += rsize
            return rsize
        }

        override fun close() {
        }
    }

    override fun realpath(path: String): String = root.access(path).followLinks().path
    override fun readlink(path: String): String? = root.accessOrNull(path)?.link
    override fun writelink(path: String, link: String?) {
        val node = root.access(path, createFolders = true)
        node.link = link
    }
    override fun open(path: String, mode: String): SyncIOFD {
        val write = mode.contains('w')
        return NodeSyncIOFD(root.access(path, createFolders = write).followLinks())
    }
    override fun stat(path: String): SyncIOStat? {
        val node = root.accessOrNull(path)
        //println("STAT: $path : $node . isDirectory=${node?.isDirectory}")
        if (node == null) return null
        return SyncIOStat(node.name, node.isDirectory, (node.data as? ByteArray?)?.size?.toLong() ?: 0L)
    }

    override fun mkdir(path: String): Boolean {
        root.access(path, createFolders = true)
        return true
    }

    override fun rmdir(path: String): Boolean = delete(path)
    override fun delete(path: String): Boolean {
        val node = root.accessOrNull(path) ?: return false
        node.delete()
        return true
    }

    override fun list(path: String): List<String> = root.access(path, createFolders = false).map { it.name }
}

data class SyncIOStat(val path: String, val isDirectory: Boolean, val size: Long)

interface SyncIOFD : SyncIOCloseable {
    var length: Long
    var position: Long
    fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int
    fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int
}

@OptIn(ExperimentalStdlibApi::class)
interface SyncIOCloseable : AutoCloseable {

}

@OptIn(ExperimentalContracts::class)
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECTED_DECLARATION_WITH_BODY")
public expect inline fun <T : SyncIOCloseable, R> T.use(block: (T) -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return try { block(this) } finally { close() }
}
