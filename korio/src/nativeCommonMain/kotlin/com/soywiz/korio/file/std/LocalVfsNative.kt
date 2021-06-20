package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.flow.*
import com.soywiz.korio.util.*
import kotlin.collections.set
import kotlin.reflect.*
import kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.process.posixExec

val tmpdir: String by lazy { Environment["TMPDIR"] ?: Environment["TEMP"] ?: Environment["TMP"] ?: "/tmp" }

var customCwd: String? = null
val nativeCwd by lazy { com.soywiz.korio.nativeCwd() }
val cwd: String get() = customCwd ?: nativeCwd

val cwdVfs: VfsFile by lazy { DynamicRootVfs(rootLocalVfsNative) { cwd } }
actual val resourcesVfs: VfsFile by lazy { cwdVfs.jail() }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val tempVfs: VfsFile by lazy { localVfs(tmpdir) }

actual val rootLocalVfs: VfsFile get() = cwdVfs
actual val applicationVfs: VfsFile get() = cwdVfs
actual val applicationDataVfs: VfsFile get() = cwdVfs
actual val externalStorageVfs: VfsFile get() = cwdVfs
actual val userHomeVfs: VfsFile get() = cwdVfs

val rootLocalVfsNative by lazy { LocalVfsNative() }

actual fun localVfs(path: String): VfsFile = rootLocalVfsNative[path]

private val IOWorker by lazy { Worker.start().also { kotlin.native.Platform.isMemoryLeakCheckerActive = false } }

suspend fun <T, R> executeInIOWorker(value: T, func: (T) -> R): R {
    return executeInWorker(IOWorker, value, func)
    //return executeInTempWorker(value, func)
}

internal suspend fun fileOpen(name: String, mode: String): CPointer<FILE>? {
	data class Info(val name: String, val mode: String)
	return executeInIOWorker(Info(name, mode)) { it ->
		platform.posix.fopen(it.name, it.mode)
	}
}

internal suspend fun fileClose(file: CPointer<FILE>): Unit = executeInIOWorker(file) { fd ->
	platform.posix.fclose(fd)
	Unit
}

internal suspend fun fileLength(file: CPointer<FILE>): Long = executeInIOWorker(file) { fd ->
	val prev = platform.posix.ftell(fd)
	platform.posix.fseek(fd, 0L.convert(), platform.posix.SEEK_END)
	val end = platform.posix.ftell(fd)
	platform.posix.fseek(fd, prev.convert(), platform.posix.SEEK_SET)
	end.toLong()
}

internal suspend fun fileSetLength(file: String, length: Long): Unit {
	data class Info(val file: String, val length: Long)

	return executeInIOWorker(Info(file, length)) { (fd, len) ->
		platform.posix.truncate(fd, len.convert())
		Unit
	}
}

internal suspend fun fileRead(file: CPointer<FILE>, position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
	val data = fileRead(file, position, len) ?: return -1
	arraycopy(data, 0, buffer, offset, data.size)
	return data.size
}

internal suspend fun fileWrite(file: CPointer<FILE>, position: Long, buffer: ByteArray, offset: Int, len: Int) {
	if (len > 0) {
		fileWrite(file, position, buffer.copyOfRange(offset, offset + len))
	}
}

internal suspend fun fileRead(file: CPointer<FILE>, position: Long, size: Int): ByteArray? {
	data class Info(val file: CPointer<FILE>, val position: Long, val size: Int)

	if (size < 0) return null
	if (size == 0) return byteArrayOf()

	return executeInIOWorker(Info(file, position, size)) { (fd, position, len) ->
		val data = ByteArray(len)
		val read = data.usePinned { pin ->
			platform.posix.fseek(fd, position.convert(), platform.posix.SEEK_SET)
			platform.posix.fread(pin.addressOf(0), 1, len.convert(), fd).toInt()
		}
		if (read < 0) null else data.copyOf(read)
	}
}

internal suspend fun fileWrite(file: CPointer<FILE>, position: Long, data: ByteArray): Long {
	data class Info(val file: CPointer<FILE>, val position: Long, val data: ByteArray)

	if (data.isEmpty()) return 0L

	return executeInIOWorker(Info(file, position, if (data.isFrozen) data else data.copyOf())) { (fd, position, data) ->
		data.usePinned { pin ->
			platform.posix.fseek(fd, position.convert(), platform.posix.SEEK_SET)
			platform.posix.fwrite(pin.addressOf(0), 1.convert(), data.size.convert(), fd).toLong()
		}.toLong()
	}
}

class LocalVfsNative : LocalVfsV2() {
	val that get() = this
	override val absolutePath: String get() = ""

	fun resolve(path: String) = path

	override suspend fun exec(
		path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
	): Int = posixExec(path, cmdAndArgs, env, handler)

	override suspend fun readRange(path: String, range: LongRange): ByteArray {
		data class Info(val path: String, val range: LongRange)

		return executeInIOWorker(Info(path, range)) { (path, range) ->
			val fd = fopen(path, "rb")
			if (fd != null) {
				fseek(fd, 0L.convert(), SEEK_END)
				//val length = ftell(fd).toLong() // @TODO: Kotlin native bug?
				val length: Long = ftell(fd).convert()

				val start = min(range.start, length)
				val end = min(range.endInclusive, length - 1) + 1
				val totalRead = (end - start).toInt()

				//println("range=$range")
				//println("length=$length")
				//println("start=$start")
				//println("end=$end")
				//println("totalRead=$totalRead")

				val byteArray = ByteArray(totalRead)
				val finalRead = if (byteArray.isNotEmpty()) {
					byteArray.usePinned { pin ->
						fseek(fd, start.convert(), SEEK_SET)
						fread(pin.addressOf(0), 1.convert(), totalRead.convert(), fd).toInt()
					}
				} else {
					0
				}

				//println("finalRead=$finalRead")

				fclose(fd)
				if (finalRead != totalRead) byteArray.copyOf(finalRead) else byteArray
			} else {
				null
			}
		} ?: throw FileNotFoundException("Can't open '$path' for reading")
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val rpath = resolve(path)
		var fd: CPointer<FILE>? = fileOpen(rpath, mode.cmode)
		val errno = posix_errno()
		//if (fd == null || errno != 0) {
		if (fd == null) {
			val errstr = strerror(errno)?.toKString()
			throw FileNotFoundException("Can't open '$rpath' with mode '${mode.cmode}' errno=$errno, errstr=$errstr")
		}

		fun checkFd() {
			if (fd == null) error("Error with file '$rpath'")
		}

		return object : AsyncStreamBase() {
			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				checkFd()
				return fileRead(fd!!, position, buffer, offset, len)
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				checkFd()
				return fileWrite(fd!!, position, buffer, offset, len)
			}

			override suspend fun setLength(value: Long): Unit {
				checkFd()
				fileSetLength(rpath, value)
			}

			override suspend fun getLength(): Long {
				checkFd()
				return fileLength(fd!!)
			}
			override suspend fun close() {
				if (fd != null) {
					fileClose(fd!!)
				}
				fd = null
			}

			override fun toString(): String = "$that($path)"
		}.toAsyncStream()
	}

	override suspend fun setSize(path: String, size: Long): Unit = run {
		platform.posix.truncate(resolve(path), size.convert())
		Unit
	}

	override suspend fun stat(path: String): VfsStat = run {
		val rpath = resolve(path)
		val result = memScoped {
			val s = alloc<stat>()
			if (platform.posix.stat(rpath, s.ptr) == 0) {
				val size: Long = s.st_size.toLong()
				val isDirectory = (s.st_mode.toInt() and S_IFDIR) != 0
				createExistsStat(rpath, isDirectory, size)
			} else {
				createNonExistsStat(rpath)
			}
		}
		result
	}

	override suspend fun listFlow(path: String) = flow {
		val dir = opendir(resolve(path))
		val out = ArrayList<VfsFile>()
		if (dir != null) {
			try {
				while (true) {
					val dent = readdir(dir) ?: break
					val name = dent.pointed.d_name.toKString()
					emit(file(name))
				}
			} finally {
				closedir(dir)
			}
		}
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = run {
		com.soywiz.korio.doMkdir(resolve(path), "0777".toInt(8).convert()) == 0
	}

	override suspend fun touch(path: String, time: DateTime, atime: DateTime): Unit = run {
		// @TODO:
		println("TODO:LocalVfsNative.touch")
	}

	override suspend fun delete(path: String): Boolean = run {
		platform.posix.unlink(resolve(path)) == 0
	}

	override suspend fun rmdir(path: String): Boolean = run {
		platform.posix.rmdir(resolve(path)) == 0
	}

	override suspend fun rename(src: String, dst: String): Boolean = run {
		platform.posix.rename(resolve(src), resolve(dst)) == 0
	}

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
		// @TODO:
		println("TODO:LocalVfsNative.watch")
		return DummyCloseable
	}

	override fun toString(): String = "LocalVfs"
}

actual fun cleanUpResourcesVfs() {
}
