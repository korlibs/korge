package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.klock.max
import com.soywiz.klock.min
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*
import com.soywiz.korio.stream.*
import com.soywiz.krypto.encoding.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

fun SimpleStorage.toVfs(): VfsFile = MapLikeStorageVfs(this).root
fun SimpleStorage.toVfs(timeProvider: TimeProvider): VfsFile = MapLikeStorageVfs(this).also { it.timeProvider = timeProvider }.root

class MapLikeStorageVfs(val storage: SimpleStorage) : VfsV2() {
    var timeProvider: TimeProvider = TimeProvider

    private fun now() = timeProvider.now()

	private val files = StorageFiles(storage) { timeProvider }
	private var initialized = false
    // @TODO: Create and use an AsyncRecursiveLock (so we can use the same lock inside a block of this lock)
    private val writeLock = AsyncThread2()

	private suspend fun initOnce() {
		if (!initialized) {
			initialized = true
			// Create root
			if (!files.hasEntryInfo("/")) {
			    val now = now()
                files.setEntryInfo(StorageFiles.EntryInfo(fullPath = "/", isFile = false, size = 0L, modifiedTime = now, createdTime = now))
			}
		}
	}

	fun String.normalizePath() = "/" + this.trim('/').replace('\\', '/')

	suspend fun remove(path: String, directory: Boolean): Boolean = writeLock {
        initOnce()
        val npath = path.normalizePath()
        val entry = files.getEntryInfo(npath) ?: return@writeLock false
        if (entry.isDirectory != directory) return@writeLock false
        if (directory && entry.children.isNotEmpty()) throw IOException("Directory '$npath' is not empty")
        files.removeEntryInfo(entry)
        return@writeLock true
    }

	override suspend fun rmdir(path: String): Boolean = remove(path, directory = true)
	override suspend fun delete(path: String): Boolean = remove(path, directory = false)

	override suspend fun stat(path: String): VfsStat {
		initOnce()
		val npath = path.normalizePath()
		val entry = files.getEntryInfo(npath) ?: return createNonExistsStat(path)
		return createExistsStat(
			path,
			entry.isDirectory,
			entry.size,
			createTime = entry.createdTime,
			modifiedTime = entry.modifiedTime
		)
	}

	private suspend fun ensureParentDirectory(nparent: String, npath: String): StorageFiles.EntryInfo {
		if (!files.hasEntryInfo(nparent)) throw IOException("Parent directory '$nparent' for file '$npath' doesn't exists")
		val parent = files.getEntryInfo(nparent)!!
		if (parent.isFile) throw IOException("'$nparent' is a file")
		return parent
	}

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		initOnce()
		val npath = path.normalizePath()
		val nparent = PathInfo(npath).folder.normalizePath()
		if (!files.hasEntryInfo(nparent)) mkdir(nparent, attributes) // Create Parents
        return writeLock {
            val parent = ensureParentDirectory(nparent, npath)
            val now = now()
            if (files.hasEntryInfo(npath)) return@writeLock false
            files.setEntryInfo(parent.copy(children = parent.children + npath))
            files.setEntryInfo(StorageFiles.EntryInfo(fullPath = npath, isFile = false, size = 0L, children = listOf(), createdTime = now, modifiedTime = now))
            return@writeLock true
        }
	}

	override suspend fun listFlow(path: String): Flow<VfsFile> {
		initOnce()
		val npath = path.normalizePath()
		val entry = files.getEntryInfo(npath) ?: throw IOException("Can't find '$path'")
		return entry.children.map { VfsFile(this, it) }.asFlow()
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		initOnce()
		val npath = path.normalizePath()
		val nparent = PathInfo(npath).folder.normalizePath()
		val parent = ensureParentDirectory(nparent, npath)

		if (!files.hasEntryInfo(npath)) {
			if (!mode.createIfNotExists) throw IOException("File '$npath' doesn't exists")
			files.setEntryInfo(parent.copy(children = parent.children + npath))
		}

		val now = now()
		var info = files.getEntryInfo(npath) ?: StorageFiles.EntryInfo(
			isFile = true,
			size = 0L,
			children = listOf(),
			createdTime = now,
			modifiedTime = now,
            fullPath = npath
		)
		if (info.isDirectory) throw IOException("Can't open a directory")

		return object : AsyncStreamBase() {
			private suspend fun updateInfo(newInfo: StorageFiles.EntryInfo) {
				if (info != newInfo) {
					info = newInfo
					files.setEntryInfo(info)
				}
			}

			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				return files.readData(npath, position, buffer, offset, len)
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				files.writeData(npath, position, buffer, offset, len)
				updateInfo(info.copy(size = max(info.size, position + len)))
			}

			override suspend fun setLength(value: Long) {
				updateInfo(info.copy(size = value))
			}

			override suspend fun getLength(): Long {
				return info.size
			}

			override suspend fun close() {
			}
		}.toAsyncStream()
	}

	override suspend fun touch(path: String, time: DateTime, atime: DateTime) = writeLock {
		initOnce()
		val npath = path.normalizePath()
		if (files.hasEntryInfo(npath)) {
			files.setEntryInfo(files.getEntryInfo(npath)!!.copy(modifiedTime = time))
		}
	}

	override fun toString(): String = "MapLikeStorageVfs"
}

private class StorageFiles(val storage: SimpleStorage, val timeProvider: () -> TimeProvider) {
    private fun now() = timeProvider().now()

	companion object {
		val CHUNK_SIZE = 16 * 1024 // 16K
	}

	fun getStatsKey(fileName: String) = "korio_stats_v1_$fileName"
	fun getChunkKey(fileName: String, chunk: Int) = "korio_chunk${chunk}_v1_$fileName"
    fun getChunkCount(size: Long) = (size divCeil CHUNK_SIZE.toLong()).toInt()

	data class EntryInfo(
        val fullPath: String,
		val isFile: Boolean,
		val size: Long = 0L,
		val children: List<String> = listOf(),
		val createdTime: DateTime = DateTime.EPOCH,
		val modifiedTime: DateTime = DateTime.EPOCH,
    ) {
        val parentFullPath by lazy { fullPath.substringBeforeLast('/') }
		val isDirectory get() = !isFile
	}

    suspend fun setEntryInfo(info: EntryInfo) {
        //setEntryInfo(info.fullPath, info.isFile, info.size, info.children, info.createdTime, info.modifiedTime)
        val oldEntry = getEntryInfo(info.fullPath)

        if (oldEntry != null) {
            // @TODO: Prune old chunks/children!
        }

        val key = getStatsKey(info.fullPath)
        val content = Json.stringify(
            linkedMapOf(
                EntryInfo::isFile.name to info.isFile,
                EntryInfo::size.name to info.size.toDouble(),
                EntryInfo::children.name to info.children,
                EntryInfo::createdTime.name to info.createdTime.unixMillisDouble,
                EntryInfo::modifiedTime.name to info.modifiedTime.unixMillisDouble
            )
        )

        storage.set(key, content)
    }

	suspend fun hasEntryInfo(fileName: String): Boolean = getEntryInfo(fileName) != null

	suspend fun getEntryInfo(fileName: String): EntryInfo? {
		val info = storage.get(getStatsKey(fileName)) ?: return null
		val di = Json.parse(info) as Map<String, Any>
		return EntryInfo(
            fullPath = fileName,
            isFile = di[EntryInfo::isFile.name]!! as Boolean,
			size = (di[EntryInfo::size.name]!! as Number).toLong(),
			children = (di[EntryInfo::children.name] as Iterable<String>).toList(),
			createdTime = (DateTime.fromUnix((di[EntryInfo::createdTime.name] as Number).toDouble())),
			modifiedTime = (DateTime.fromUnix((di[EntryInfo::modifiedTime.name] as Number).toDouble())),
        )
	}

    suspend fun removeEntryInfo(entry: EntryInfo) {
        // Remove own children
        entry.children.fastForEach { child ->
            removeEntryInfo(child)
        }
        // Remove data chunks
        for (n in 0 until getChunkCount(entry.size)) {
            storage.remove(getChunkKey(entry.fullPath, n))
        }
        // Remove entry
        storage.remove(getStatsKey(entry.fullPath))
        // Remove child from parent
        val parentEntry = getEntryInfo(entry.parentFullPath)
        if (parentEntry != null) {
            setEntryInfo(parentEntry.copy(children = parentEntry.children.without(entry.fullPath), modifiedTime = now()))
        }
    }

	suspend fun removeEntryInfo(fileName: String): Boolean {
		val entry = getEntryInfo(fileName) ?: return false
        removeEntryInfo(entry)
        return true
	}

	suspend fun setFileChunk(fileName: String, chunk: Int, data: ByteArray) = run {
		storage.set(getChunkKey(fileName, chunk), data.hex)
	}

	suspend fun getFileChunk(fileName: String, chunk: Int): ByteArray? =
		storage.get(getChunkKey(fileName, chunk))?.unhex

	suspend fun writeData(fileName: String, position: Long, buffer: ByteArray, offset: Int, len: Int) {
		var pending = len
		var apos = position
		var aoffset = offset
		while (pending > 0) {
			val chunk = (apos / CHUNK_SIZE).toInt()
			val inChunk = (apos % CHUNK_SIZE).toInt()
			val c = getFileChunk(fileName, chunk) ?: byteArrayOf()
			val available = CHUNK_SIZE - inChunk
			val written = min(available, pending)
			if (written <= 0) invalidOp("Unexpected written")
			val cc = c.copyOf(inChunk + written)
			arraycopy(buffer, aoffset, cc, inChunk, written)
			setFileChunk(fileName, chunk, cc)
			pending -= written
			apos += written
			aoffset += written
		}
	}

	suspend fun readData(fileName: String, position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val info = getEntryInfo(fileName) ?: return -1
		if (position >= info.size) return 0
		val chunk = (position / CHUNK_SIZE).toInt()
		val inChunk = (position % CHUNK_SIZE).toInt()
		val c = getFileChunk(fileName, chunk) ?: return 0
		val available = c.size - inChunk
		val read = min(available, len)
		arraycopy(c, inChunk, buffer, offset, read)
		return read
	}
}
