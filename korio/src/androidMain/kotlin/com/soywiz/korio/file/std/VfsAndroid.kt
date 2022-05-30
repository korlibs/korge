package com.soywiz.korio.file.std

import android.content.Context
import com.soywiz.klock.DateTime
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.VfsProcessHandler
import com.soywiz.korio.file.VfsStat
import com.soywiz.korio.file.withOnce
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.LONG_ZERO_TO_MAX_RANGE
import com.soywiz.korio.util.OS
import com.soywiz.korio.util.endExclusiveClamped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.net.URL
import kotlin.math.min

private val absoluteCwd by lazy { File(".").absolutePath }
val tmpdir: String by lazy { System.getProperty("java.io.tmpdir") }

class AndroidDeferredVfs(private val generate: (Context) -> VfsFile) : Vfs.Proxy() {
    private var _generated: VfsFile? = null
    suspend fun generated(): VfsFile {
        val context = androidContext()
        if (_generated == null) {
            _generated = generate(context)
        }
        return _generated!!
    }

    override suspend fun access(path: String): VfsFile {
        return generated()[path]
    }
}

fun AndroidDeferredFile(generate: (Context) -> File): VfsFile =
    AndroidDeferredVfs { localVfs(generate(it)).jail() }.root

actual val standardVfs: StandardVfs = object : StandardVfs() {
    override val resourcesVfs: VfsFile by lazy { AndroidResourcesVfs().root }
    override val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
}

actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { AndroidDeferredFile { it.getDir("korio", Context.MODE_PRIVATE) } }
actual val cacheVfs: VfsFile by lazy { AndroidDeferredFile { it.cacheDir } }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy { AndroidDeferredFile { it.cacheDir } }

actual fun localVfs(path: String, async: Boolean): VfsFile = LocalVfsJvm()[path]

// Extensions
operator fun LocalVfs.Companion.get(base: File) = localVfs(base)

fun localVfs(base: File): VfsFile = localVfs(base.absolutePath)
fun jailedLocalVfs(base: File): VfsFile = localVfs(base.absolutePath).jail()
suspend fun File.open(mode: VfsOpenMode) = localVfs(this).open(mode)
fun File.toVfs() = localVfs(this)
fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString())

class AndroidResourcesVfs : Vfs() {
	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream =
        readRange(path, LONG_ZERO_TO_MAX_RANGE).openAsync(mode.cmode)

	override suspend fun readRange(path: String, range: LongRange): ByteArray {
        val context = androidContext()
        return executeIo {

            //val path = "/assets/" + path.trim('/')
            val rpath = path.trim('/')

            val fs = context.assets.open(rpath)
            fs.skip(range.start)
            val out = ByteArrayOutputStream()
            val temp = ByteArray(16 * 1024)
            var available = (range.endExclusiveClamped - range.start)
            while (available >= 0) {
                val read = fs.read(temp, 0, min(temp.size.toLong(), available).toInt())
                if (read <= 0) break
                out.write(temp, 0, read)
                available -= read
            }
            out.toByteArray()
        }
    }
}


//private val IOContext by lazy { newSingleThreadContext("IO") }
private val IOContext by lazy { Dispatchers.Unconfined }

private suspend fun <T> executeIo(callback: suspend CoroutineScope.() -> T): T = withContext(IOContext, callback)

private class LocalVfsJvm : LocalVfsV2() {
	val that = this
	override val absolutePath: String = ""

	//private suspend inline fun <T> executeIo(callback: suspend () -> T): T = callback()

	fun resolve(path: String) = path
	fun resolveFile(path: String) = File(resolve(path))

	override suspend fun exec(
		path: String,
		cmdAndArgs: List<String>,
		env: Map<String, String>,
		handler: VfsProcessHandler
	): Int = executeIo {
		val actualCmd = if (OS.isWindows) listOf("cmd", "/c") + cmdAndArgs else cmdAndArgs
		val pb = ProcessBuilder(actualCmd)
		pb.environment().putAll(LinkedHashMap())
		pb.directory(resolveFile(path))

		val p = pb.start()
		var closing = false
		while (true) {
			val o = p.inputStream.readAvailableChunk(readRest = closing)
			val e = p.errorStream.readAvailableChunk(readRest = closing)
			if (o.isNotEmpty()) handler.onOut(o)
			if (e.isNotEmpty()) handler.onErr(e)
			if (closing) break
			if (o.isEmpty() && e.isEmpty() && !p.isAliveJre7) {
				closing = true
				continue
			}
			Thread.sleep(1L)
		}
		p.waitFor()
		//handler.onCompleted(p.exitValue())
		p.exitValue()
	}

	private val Process.isAliveJre7: Boolean
		get() = try {
			exitValue()
			false
		} catch (e: IllegalThreadStateException) {
			true
		}


	private fun InputStream.readAvailableChunk(readRest: Boolean): ByteArray {
		val out = ByteArrayOutputStream()
		while (if (readRest) true else available() > 0) {
			val c = this.read()
			if (c < 0) break
			out.write(c)
		}
		return out.toByteArray()
	}

	private fun InputStreamReader.readAvailableChunk(i: InputStream, readRest: Boolean): String {
		val out = java.lang.StringBuilder()
		while (if (readRest) true else i.available() > 0) {
			val c = this.read()
			if (c < 0) break
			out.append(c.toChar())
		}
		return out.toString()
	}

	override suspend fun readRange(path: String, range: LongRange): ByteArray = executeIo {
		RandomAccessFile(resolveFile(path), "r").use { raf ->
			val fileLength = raf.length()
			val start = min(range.start, fileLength)
			val end = min(range.endInclusive, fileLength - 1) + 1
			val totalRead = (end - start).toInt()
			val out = ByteArray(totalRead)
			raf.seek(start)
			val read = raf.read(out)
			if (read != totalRead) out.copyOf(read) else out
		}
	}

	@Suppress("BlockingMethodInNonBlockingContext")
	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val raf = executeIo {
			val file = resolveFile(path)
			if (file.exists() && (mode == VfsOpenMode.CREATE_NEW)) {
				throw IOException("File $file already exists")
			}
			RandomAccessFile(
				file, when (mode) {
					VfsOpenMode.READ -> "r"
					VfsOpenMode.WRITE -> "rw"
					VfsOpenMode.APPEND -> "rw"
					VfsOpenMode.CREATE -> "rw"
					VfsOpenMode.CREATE_NEW -> "rw"
					VfsOpenMode.CREATE_OR_TRUNCATE -> "rw"
				}
			).apply {
				if (mode.truncate) {
					setLength(0L)
				}
				if (mode == VfsOpenMode.APPEND) {
					seek(length())
				}
			}
		}

		return object : AsyncStreamBase() {
			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = executeIo {
				raf.seek(position)
				raf.read(buffer, offset, len)
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = executeIo {
				raf.seek(position)
				raf.write(buffer, offset, len)
			}

			override suspend fun setLength(value: Long): Unit = executeIo {
				raf.setLength(value)
			}

			override suspend fun getLength(): Long = executeIo { raf.length() }
			override suspend fun close() = raf.close()

			override fun toString(): String = "$that($path)"
		}.toAsyncStream()
	}

	override suspend fun setSize(path: String, size: Long): Unit = executeIo {
		val file = resolveFile(path)
		FileOutputStream(file, true).channel.use { outChan ->
			outChan.truncate(size)
		}
		Unit
	}

	override suspend fun stat(path: String): VfsStat = executeIo {
		val file = resolveFile(path)
		val fullpath = "$path/${file.name}"
		if (file.exists()) {
			val lastModified = DateTime.fromUnix(file.lastModified())
			createExistsStat(
				fullpath,
				isDirectory = file.isDirectory,
				size = file.length(),
				createTime = lastModified,
				modifiedTime = lastModified,
				lastAccessTime = lastModified
			)
		} else {
			createNonExistsStat(fullpath)
		}
	}

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun listFlow(path: String): Flow<VfsFile> = flow {
        for (it in (File(path).listFiles() ?: emptyArray<File>())) {
            emit(that.file("$path/${it.name}"))
        }
    }.flowOn(IOContext)

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean =
		executeIo { resolveFile(path).mkdirs() }

	override suspend fun touch(path: String, time: DateTime, atime: DateTime): Unit =
		executeIo { resolveFile(path).setLastModified(time.unixMillisLong); Unit }

	override suspend fun delete(path: String): Boolean = executeIo { resolveFile(path).delete() }
	override suspend fun rmdir(path: String): Boolean = executeIo { resolveFile(path).delete() }
	override suspend fun rename(src: String, dst: String): Boolean =
		executeIo { resolveFile(src).renameTo(resolveFile(dst)) }

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable = TODO()

	override fun toString(): String = "LocalVfs"
}
