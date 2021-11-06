package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.klock.*
import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.util.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.flow.*
import kotlin.reflect.*
import kotlin.coroutines.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.process.posixExec

@ThreadLocal
val tmpdir: String by lazy { Environment["TMPDIR"] ?: Environment["TEMP"] ?: Environment["TMP"] ?: "/tmp" }
@ThreadLocal
var customCwd: String? = null
@ThreadLocal
val nativeCwd by lazy { com.soywiz.korio.nativeCwd() }
val cwd: String get() = customCwd ?: nativeCwd

//@ThreadLocal
//val cwdVfs: VfsFile by lazy { DynamicRootVfs(rootLocalVfsNative) { cwd } }
val cwdVfs: VfsFile get() = rootLocalVfsNative[cwd]
@ThreadLocal
actual val resourcesVfs: VfsFile by lazy { cwdVfs.jail() }
@ThreadLocal
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
@ThreadLocal
actual val tempVfs: VfsFile by lazy { localVfs(tmpdir) }

actual val rootLocalVfs: VfsFile get() = cwdVfs
actual val applicationVfs: VfsFile get() = cwdVfs
actual val applicationDataVfs: VfsFile get() = cwdVfs
actual val externalStorageVfs: VfsFile get() = cwdVfs
actual val userHomeVfs: VfsFile get() = cwdVfs

@ThreadLocal
val rootLocalVfsNative by lazy { LocalVfsNative(async = true) }
@ThreadLocal
val rootLocalVfsNativeSync by lazy { LocalVfsNative(async = false) }

actual fun localVfs(path: String, async: Boolean): VfsFile = (if (async) rootLocalVfsNative else rootLocalVfsNativeSync)[path]

@ThreadLocal
@PublishedApi
internal val IOWorker by lazy { Worker.start().also { kotlin.native.Platform.isMemoryLeakCheckerActive = false } }

expect open class LocalVfsNative(async: Boolean = true) : LocalVfsNativeBase

open class LocalVfsNativeBase(val async: Boolean = true) : LocalVfsV2() {
	val that get() = this
	override val absolutePath: String get() = ""

	fun resolve(path: String) = path

    suspend inline fun <T, R> executeInIOWorker(value: T, noinline func: (T) -> R): R = if (async) executeInWorker(IOWorker, value, func) else func(value)

    override suspend fun exec(
		path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
	): Int = posixExec(path, cmdAndArgs, env, handler)

	override suspend fun readRange(path: String, range: LongRange): ByteArray {
        val rpath = resolve(path)
		data class Info(val path: String, val range: LongRange)

		return executeInIOWorker(Info(rpath, range)) { (rpath, range) ->
			val fd = posixFopen(rpath, "rb")
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
		} ?: throw FileNotFoundException("Can't open '${rpath}' for reading")
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        val rpath = resolve(path)

        data class FileOpenInfo(val name: String, val mode: String)
        val (initialFd, initialFileLength) = executeInIOWorker(FileOpenInfo(rpath, mode.cmode)) { it ->
            val fd = posixFopen(it.name, it.mode)
            val len = if (fd != null) {
                fseek(fd, 0L.convert(), SEEK_END)
                val end = ftell(fd)
                fseek(fd, 0L.convert(), SEEK_SET)
                end.toLong()
            } else {
                0L
            }
            Pair(fd, len)
        }
        val errno = posix_errno()
        //if (initialFd == null || errno != 0) {
        if (initialFd == null) {
            val errstr = strerror(errno)?.toKString()
            throw FileNotFoundException("Can't open '$rpath' with mode '${mode.cmode}' errno=$errno, errstr=$errstr")
        }

		return object : AsyncStreamBase() {
            var fd: CPointer<FILE>? = initialFd
            var currentFileLength: Long = initialFileLength

            fun checkFd() {
                if (fd == null) error("Error with file '$rpath'")
            }

            internal suspend fun fileTransfer(fd: CPointer<FILE>, position: Long, buffer: ByteArray, offset: Int, len: Int, write: Boolean): Long {
                data class Info(val fd: CPointer<FILE>, val position: Long, val buffer: CPointer<ByteVar>, val size: Int, val write: Boolean)
                if (len <= 0) return 0L
                if (offset + len > buffer.size) throw OutOfBoundsException(offset + len)
                var transferredCount: Long = 0L
                //fileWrite(fd, position, buffer.copyOfRange(offset, offset + len))
                val time = measureTime {
                    transferredCount = buffer.usePinned { pin ->
                        executeInIOWorker(Info(fd, position, pin.addressOf(offset), len, write)) { (fd, position, buffer, len) ->
                            fseek(fd, position.convert(), SEEK_SET)
                            if (write) {
                                //platform.posix.fwrite(buffer, 1.convert(), len.convert(), fd).toLong()
                                fwrite(buffer, len.convert(), 1.convert(), fd).toLong() // Write it at once (len, 1)
                                len.toLong()
                            } else {
                                fread(buffer, 1.convert(), len.convert(), fd).toLong() // Allow to read less values (1, len)
                            }
                        }
                    }
                }
                //println("fileTransfer: len=$len, write=$write, transferred=$transferredCount, async=$async, time=$time")
                return transferredCount
            }

            override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				checkFd()
				return fileTransfer(fd!!, position, buffer, offset, len, write = false).toInt()
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				checkFd()
                currentFileLength = max(currentFileLength, position + len)
				fileTransfer(fd!!, position, buffer, offset, len, write = true)
			}

			override suspend fun setLength(value: Long): Unit {
				checkFd()
                data class Info(val file: String, val length: Long)

                executeInIOWorker(Info(rpath, value)) { (fd, len) ->
                    truncate(fd, len.convert())
                    Unit
                }
                currentFileLength = value
			}

			override suspend fun getLength(): Long = currentFileLength
			override suspend fun close() {
				if (fd != null) {
                    executeInIOWorker(fd!!) { fd ->
                        fclose(fd)
                        Unit
                    }
				}
				fd = null
                currentFileLength = 0L
			}

			override fun toString(): String = "$that($path)"
		}.toAsyncStream()
	}

	override suspend fun setSize(path: String, size: Long): Unit {
		truncate(resolve(path), size.convert())
	}

	override suspend fun stat(path: String): VfsStat {
		val rpath = resolve(path)
		return memScoped {
			val s = alloc<stat>()
			if (platform.posix.stat(rpath, s.ptr) == 0) {
				val size: Long = s.st_size.toLong()
				val isDirectory = (s.st_mode.toInt() and S_IFDIR) != 0
				createExistsStat(rpath, isDirectory, size)
			} else {
				createNonExistsStat(rpath)
			}
		}
	}

	override suspend fun listFlow(path: String) = flow {
		val dir = opendir(resolve(path))
		val out = ArrayList<VfsFile>()
		if (dir != null) {
			try {
				while (true) {
					val dent = readdir(dir) ?: break
					val name = dent.pointed.d_name.toKString()
                    if (name != "." && name != "..") {
                        emit(file("$path/$name"))
                    }
				}
			} finally {
				closedir(dir)
			}
		}
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = com.soywiz.korio.doMkdir(resolve(path), "0777".toInt(8).convert()) == 0

	override suspend fun touch(path: String, time: DateTime, atime: DateTime): Unit {
		// @TODO:
		println("TODO:LocalVfsNative.touch")
	}

	override suspend fun delete(path: String): Boolean = platform.posix.unlink(resolve(path)) == 0
	override suspend fun rmdir(path: String): Boolean = platform.posix.rmdir(resolve(path)) == 0

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
