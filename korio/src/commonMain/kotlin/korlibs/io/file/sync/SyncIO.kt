package korlibs.io.file.sync

import korlibs.memory.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*

internal expect val platformSyncIO: SyncIO

interface SyncIO {
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

    fun write(path: String, data: ByteArray) {
        mkdir(PathInfo(path).parent.fullPathNormalized)
        open(path, "wb").use { it.write(data) }
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
            arraycopy(data, offset, bytes, positionInt, size)
            positionInt += size
            return size
        }

        override fun read(data: ByteArray, offset: Int, size: Int): Int {
            val rsize = minOf(size, available.toInt())
            arraycopy(bytes, positionInt, data, offset, rsize)
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
        val node = root.accessOrNull(path) ?: return null
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

interface SyncIOFD : Closeable {
    var length: Long
    var position: Long
    fun write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int
    fun read(data: ByteArray, offset: Int = 0, size: Int = data.size - offset): Int
}