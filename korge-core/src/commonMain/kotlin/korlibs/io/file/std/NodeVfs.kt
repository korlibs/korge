@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.io.file.std

import korlibs.datastructure.iterators.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import kotlinx.coroutines.flow.*

open class NodeVfs(val caseSensitive: Boolean = true) : Vfs() {
	val events = Signal<FileEvent>()

	val nodeTree = MemoryNodeTree(caseSensitive)
    val rootNode get() = nodeTree.rootNode

	private fun createStream(s: SyncStreamBase, vfsFile: VfsFile) = object : AsyncStreamBase() {
		override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
			return s.read(position, buffer, offset, len)
		}

		override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
			s.write(position, buffer, offset, len)
			events(FileEvent(FileEvent.Kind.MODIFIED, vfsFile))
		}

		override suspend fun setLength(value: Long) {
			s.length = value
			events(FileEvent(FileEvent.Kind.MODIFIED, vfsFile))
		}

		override suspend fun getLength(): Long = s.length
		override suspend fun close() = s.close()
	}.toAsyncStream()

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		//if (mode.truncate) {
		//	delete(path)
		//}
		val pathInfo = PathInfo(path)
		val folder = rootNode.access(pathInfo.folder)
		var node = folder.child(pathInfo.baseName)
		val vfsFile = this@NodeVfs[path]
		if (node == null && mode.createIfNotExists) {
			node = folder.createChild(pathInfo.baseName, isDirectory = false)
			node.stream = createStream(MemorySyncStream().base, vfsFile)
		} else if (mode.truncate) {
			node?.stream = createStream(MemorySyncStream().base, vfsFile)
		}
		return node?.stream?.duplicate() ?: throw FileNotFoundException(path)
	}

	override suspend fun stat(path: String): VfsStat {
		return try {
			val node = rootNode.access(path)
			//createExistsStat(path, isDirectory = node.isDirectory, size = node.stream?.getLength() ?: 0L) // @TODO: Kotlin wrong code generated!
			val length = node.stream?.getLength() ?: 0L
			createExistsStat(path, isDirectory = node.isDirectory, size = length)
		} catch (e: Throwable) {
			createNonExistsStat(path)
		}
	}

	override suspend fun listFlow(path: String): Flow<VfsFile> = flow {
		val node = rootNode[path]
		for ((name, _) in node.children) {
			emit(file("$path/$name"))
		}
	}

	override suspend fun delete(path: String): Boolean {
		val node = rootNode.getOrNull(path)
		return if (node != null) {
			node.parent = null
			events(FileEvent(FileEvent.Kind.DELETED, this[path]))
			true
		} else {
			false
		}
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		val pathInfo = PathInfo(path)
        val parentFolder = rootNode.accessOrNull(pathInfo.folder) ?: return false
		val out = parentFolder.mkdir(pathInfo.baseName)
		events(FileEvent(FileEvent.Kind.CREATED, this[path]))
		return out
	}

	override suspend fun rename(src: String, dst: String): Boolean {
		if (src == dst) return false
		val dstInfo = PathInfo(dst)
		val srcNode = rootNode.access(src)
        srcNode.parent = null

		val dstFolder = rootNode.access(dstInfo.folder)
        val dstNode = dstFolder.createChild(dstInfo.baseName)
        dstNode.data = srcNode.data
        dstNode.stream = srcNode.stream
        for (child in srcNode.children.values) {
            child.parent = dstNode
        }

		events(
			FileEvent(
				FileEvent.Kind.RENAMED,
				this[src],
				this[dst]
			)
		)
		return true
	}

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
		return events { handler(it) }
	}

	override fun toString(): String = "NodeVfs"
}
