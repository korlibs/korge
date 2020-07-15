package com.soywiz.korio.file.std

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.io.FileNotFoundException
import java.io.IOException
import java.net.*
import java.nio.channels.CompletionHandler
import java.nio.file.*
import java.nio.file.Path
import java.util.concurrent.*
import kotlin.coroutines.*

private val absoluteCwd by lazy { File(".").absolutePath }
val tmpdir: String by lazy { System.getProperty("java.io.tmpdir") }

actual val resourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root.jail() }
actual val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy { localVfs(tmpdir) }

actual fun localVfs(path: String): VfsFile = LocalVfsJvm()[path]

// Extensions
operator fun LocalVfs.Companion.get(base: File) = localVfs(base)

fun localVfs(base: File): VfsFile = localVfs(base.absolutePath)
fun jailedLocalVfs(base: File): VfsFile = localVfs(base.absolutePath).jail()
suspend fun File.open(mode: VfsOpenMode) = localVfs(this).open(mode)
fun File.toVfs() = localVfs(this)
fun UrlVfs(url: URL): VfsFile = UrlVfs(url.toString())
operator fun File.get(path: String) = File(this, path)

fun ClassLoader.tryGetURLs(): List<URL> = try {
	when {
		this is URLClassLoader -> this.urLs.toList()
		else -> KDynamic { this@tryGetURLs["ucp"]["path"] } as List<URL>
	}
} catch (e: Throwable) {
	//System.err.println("Error trying to get URLs from classloader $this")
	//e.printStackTrace()
	listOf()
}

private class ResourcesVfsProviderJvm {
	operator fun invoke(): Vfs = invoke(ClassLoader.getSystemClassLoader())

	fun findSrcs(base: File, classPath: File): List<File> {
		val relative = classPath.relativeTo(base).path.replace('\\', '/')
		val out = arrayListOf<File>()
		var current = base
		for (part in relative.split('/')) {
			current = File(current, part)
			val srcDir = File(current, "src")
			if (srcDir.isDirectory) {
				out += srcDir
			}
		}
		return out
	}

	operator fun invoke(classLoader: ClassLoader): Vfs {
		val merged = MergedVfs()

		return object : Vfs.Decorator(merged.root) {
			override suspend fun init() {
				val currentDir = localCurrentDirVfs.absolutePath
				val urls = classLoader.tryGetURLs()
				//val urlsApp = urls.filter { File(it.toURI()).absolutePath.startsWith(currentDir) }
				val classPaths = urls.filter { it.toString().startsWith("file:") }.map { File(it.toURI()).absolutePath }
				val classPathsApp = classPaths.filter { it.startsWith(currentDir) }

				if (resourcesVfsDebug) {
					println("currentDirectory: ${localCurrentDirVfs.absolutePath}")
					if (classLoader is URLClassLoader) {
						println("classLoader is URLClassLoader")
					} else {
						println("classLoader !is URLClassLoader but $classLoader")
					}
					for (path in classPaths) {
						println("classLoader: $path")
					}
					for (path in classPathsApp) {
						println("classPathsApp: $path")
					}
				}

				val srcDirs = arrayListOf<File>()

				for (path in classPathsApp) {
					val relativePath = File(path).relativeTo(File(currentDir))
					if (resourcesVfsDebug) println("classPathsApp.relative: $relativePath")
					val srcs = findSrcs(File(currentDir), File(path))
					if (resourcesVfsDebug) println("classPathsApp.relative: $srcs")
					srcDirs += srcs
				}


				//println("localCurrentDirVfs: $localCurrentDirVfs, ${localCurrentDirVfs.absolutePath}")

				// @TODO: IntelliJ doesn't properly set resources folder for MPP just yet (on gradle works just fine),
				// @TODO: so at least we try to load resources from sources until this is fixed.
				run {
					for (folder in listOf(
						localCurrentDirVfs["src/commonMain/resources"],
						localCurrentDirVfs["src/jvmMain/resources"],
						localCurrentDirVfs["resources"],
						localCurrentDirVfs["jvmResources"],
						localCurrentDirVfs["src/commonTest/resources"],
						localCurrentDirVfs["src/jvmTest/resources"]
					)) {
						if (folder.exists() && folder.isDirectory()) {
							merged += folder.jail()
						}
					}

					for (srcDir in srcDirs.map { it.toVfs() }) {
						for (folder in listOf(
							srcDir["commonMain/resources"],
							srcDir["jvmMain/resources"],
							srcDir["commonTest/resources"],
							srcDir["jvmTest/resources"],
							// Korge
							srcDir["../build/genMainResources"],
							srcDir["../build/genTestResources"]
						)) {
							if (folder.exists() && folder.isDirectory()) {
								merged += folder.jail()
							}
						}
					}
				}

				for (url in urls) {
					//println("ResourcesVfsProviderJvm.url: $url")
					val urlStr = url.toString()
					val vfs = when {
						urlStr.startsWith("http") -> UrlVfs(url)
						else -> localVfs(File(url.toURI()))
					}

					//println(vfs)

					when {
						vfs.extension in setOf("jar", "zip") -> {
							//merged.vfsList += vfs.openAsZip()
						}
						else -> merged += vfs.jail()
					}
				}
				//println(merged.options)

				//println("ResourcesVfsProviderJvm:classLoader:$classLoader")

				merged += object : Vfs() {
					private fun normalize(path: String): String = path.trim('/')

					private fun getResourceAsStream(npath: String) = classLoader.getResourceAsStream(npath)
						?: classLoader.getResourceAsStream("/$npath")
						?: invalidOp("Can't find '$npath' in ResourcesVfsProviderJvm")

					override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:open: $path")
						return MemorySyncStream(getResourceAsStream(npath).readBytes()).toAsync()
					}

					override suspend fun stat(path: String): VfsStat = run {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:stat: $npath")
						try {
							val s = getResourceAsStream(npath)
							val size = s.available()
							s.read()
							createExistsStat(npath, isDirectory = false, size = size.toLong())
						} catch (e: Throwable) {
							//e.printStackTrace()
							createNonExistsStat(npath)
						}
					}

					override fun toString(): String = "ResourcesVfsProviderJvm"
				}.root

				//println("ResourcesVfsProviderJvm: $merged")
			}

			override fun toString(): String = "ResourcesVfs"
		}
	}
}

//private val IOContext by lazy { newSingleThreadContext("IO") }
private val IOContext by lazy { Dispatchers.Unconfined }

private class LocalVfsJvm : LocalVfsV2() {
	val that = this
	override val absolutePath: String = ""

	private suspend inline fun <T> executeIo(crossinline callback: suspend () -> T): T = withContext(IOContext) { callback() }
	//private suspend inline fun <T> executeIo(callback: suspend () -> T): T = callback()

	fun resolve(path: String) = path
	fun resolvePath(path: String) = Paths.get(resolve(path))
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
			val start = kotlin.math.min(range.start, fileLength)
			val end = kotlin.math.min(range.endInclusive, fileLength - 1) + 1
			val totalRead = (end - start).toInt()
			val out = ByteArray(totalRead)
			raf.seek(start)
			val read = raf.read(out)
			if (read != totalRead) out.copyOf(read) else out
		}
	}

	@Suppress("BlockingMethodInNonBlockingContext")
	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		try {
			val raf = executeIo {
				val file = resolveFile(path)
				if (file.exists() && (mode == VfsOpenMode.CREATE_NEW)) {
					throw IOException("File $file already exists")
				}
				RandomAccessFile(file, when (mode) {
					VfsOpenMode.READ -> "r"
					VfsOpenMode.WRITE -> "rw"
					VfsOpenMode.APPEND -> "rw"
					VfsOpenMode.CREATE -> "rw"
					VfsOpenMode.CREATE_NEW -> "rw"
					VfsOpenMode.CREATE_OR_TRUNCATE -> "rw"
				}).apply {
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
		} catch (e: java.nio.file.NoSuchFileException) {
			throw FileNotFoundException(e.message)
		}
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

	/*
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun listFlow(path: String): kotlinx.coroutines.flow.Flow<VfsFile> = flow {
        for (it in (File(path).listFiles() ?: emptyArray<File>())) {
            emit(that.file("$path/${it.name}"))
        }
    }.flowOn(IOContext)
	*/

	override suspend fun listSimple(path: String): List<VfsFile> =
			(File(path).listFiles() ?: emptyArray<File>()).map { that.file("$path/${it.name}") }

	override suspend fun listFlow(path: String): kotlinx.coroutines.flow.Flow<VfsFile> = flow {
		try {
			TODO()
		} catch (e: Throwable) {
			e.printStackTrace()
		}
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

	suspend fun <T> completionHandler(callback: (CompletionHandler<T, Unit>) -> Unit): T {
		return suspendCancellableCoroutine<T> { c ->
			callback(object : CompletionHandler<T, Unit> {
				override fun completed(result: T, attachment: Unit?) = c.resume(result)
				override fun failed(exc: Throwable, attachment: Unit?) = c.resumeWithException(exc)
			})
		}
	}

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
		var running = true
		val fs = FileSystems.getDefault()
		val watcher = fs.newWatchService()

		fs.getPath(path).register(
			watcher,
			StandardWatchEventKinds.ENTRY_CREATE,
			StandardWatchEventKinds.ENTRY_DELETE,
			StandardWatchEventKinds.ENTRY_MODIFY
		)

		launchImmediately(coroutineContext) {
			while (running) {
				val key = executeIo {
					var r: WatchKey?
					do {
						r = watcher.poll(100L, TimeUnit.MILLISECONDS)
					} while (r == null && running)
					r
				} ?: continue

				for (e in key.pollEvents()) {
					val kind = e.kind()
					val filepath = e.context() as Path
					val rfilepath = fs.getPath(path, filepath.toString())
					val file = rfilepath.toFile()
					val absolutePath = file.absolutePath
					val vfsFile = file(absolutePath)
					when (kind) {
						StandardWatchEventKinds.OVERFLOW -> {
							println("Overflow WatchService")
						}
						StandardWatchEventKinds.ENTRY_CREATE -> {
							handler(
								FileEvent(
									FileEvent.Kind.CREATED,
									vfsFile
								)
							)
						}
						StandardWatchEventKinds.ENTRY_MODIFY -> {
							handler(
								FileEvent(
									FileEvent.Kind.MODIFIED,
									vfsFile
								)
							)
						}
						StandardWatchEventKinds.ENTRY_DELETE -> {
							handler(
								FileEvent(
									FileEvent.Kind.DELETED,
									vfsFile
								)
							)
						}
					}
				}
				key.reset()
			}
		}

		return Closeable {
			running = false
			watcher.close()
		}
	}

	override fun toString(): String = "LocalVfs"
}
