@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file.std

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korio.async.Signal
import com.soywiz.korio.file.PathInfo
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.VfsStat
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.folder
import com.soywiz.korio.file.parts
import com.soywiz.korio.file.pathInfo
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.MemorySyncStream
import com.soywiz.korio.stream.SyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

open class NodeVfs(val caseSensitive: Boolean = true) : Vfs() {
	val events = Signal<FileEvent>()

	open inner class Node(
		val name: String,
		val isDirectory: Boolean = false,
		parent: Node? = null
	) : Iterable<Node> {
		val nameLC = name.toLowerCase()
		override fun iterator(): Iterator<Node> = children.values.iterator()

		var parent: Node? = null
			set(value) {
				if (field != null) {
					field!!.children.remove(this.name)
					field!!.childrenLC.remove(this.nameLC)
				}
				field = value
				field?.children?.set(name, this)
				field?.childrenLC?.set(nameLC, this)
			}

		init {
			this.parent = parent
		}

		var data: Any? = null
		val children = linkedMapOf<String, Node>()
		val childrenLC = linkedMapOf<String, Node>()
		val root: Node get() = parent?.root ?: this
		var stream: AsyncStream? = null

		fun child(name: String): Node? = when (name) {
			"", "." -> this
			".." -> parent
			else -> if (caseSensitive) {
				children[name]
			} else {
				childrenLC[name.toLowerCase()]
			}
		}

		fun createChild(name: String, isDirectory: Boolean = false): Node =
			Node(name, isDirectory = isDirectory, parent = this)

		operator fun get(path: String): Node = access(path, createFolders = false)
		fun getOrNull(path: String): Node? = try {
			access(path, createFolders = false)
		} catch (e: FileNotFoundException) {
			null
		}

		fun access(path: String, createFolders: Boolean = false): Node {
			var node = if (path.startsWith('/')) root else this
			path.pathInfo.parts().fastForEach { part ->
				var child = node.child(part)
				if (child == null && createFolders) child = node.createChild(part, isDirectory = true)
				node = child ?: throw FileNotFoundException("Can't find '$part' in $path")
			}
			return node
		}

		fun mkdir(name: String): Boolean {
			if (child(name) != null) {
				return false
			} else {
				createChild(name, isDirectory = true)
				return true
			}
		}
	}

	val rootNode = Node("", isDirectory = true)

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
		val out = rootNode.access(pathInfo.folder).mkdir(pathInfo.baseName)
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
