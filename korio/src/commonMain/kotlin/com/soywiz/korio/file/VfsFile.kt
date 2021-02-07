@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*

data class VfsFile(
	val vfs: Vfs,
	val path: String
) : VfsNamed(path.pathInfo), AsyncInputOpenable, Extra by Extra.Mixin() {
    fun relativePathTo(relative: VfsFile): String? {
        if (relative.vfs != this.vfs) return null
        return this.pathInfo.relativePathTo(relative.pathInfo)
    }

    val parent: VfsFile get() = VfsFile(vfs, folder)
	val root: VfsFile get() = vfs.root
	val absolutePath: String get() = vfs.getAbsolutePath(this.path)
    val absolutePathInfo: PathInfo get() = PathInfo(absolutePath)

	operator fun get(path: String): VfsFile =
		VfsFile(vfs, this.path.pathInfo.combine(path.pathInfo).fullPath)

	// @TODO: Kotlin suspend operator not supported yet!
	//suspend fun set(path: String, content: String) = run { this[path].put(content.toByteArray(UTF8).openAsync()) }
	//suspend fun set(path: String, content: ByteArray) = run { this[path].put(content.openAsync()) }
	//suspend fun set(path: String, content: AsyncStream) = run { this[path].writeStream(content) }
	//suspend fun set(path: String, content: VfsFile) = run { this[path].writeFile(content) }

	suspend fun put(content: AsyncInputStream, attributes: List<Vfs.Attribute> = listOf()): Long = vfs.put(this.path, content, attributes)
	suspend fun put(content: AsyncInputStream, vararg attributes: Vfs.Attribute): Long = vfs.put(this.path, content, attributes.toList())
	suspend fun write(data: ByteArray, vararg attributes: Vfs.Attribute): Long = vfs.put(this.path, data, attributes.toList())
	suspend fun writeBytes(data: ByteArray, vararg attributes: Vfs.Attribute): Long = vfs.put(this.path, data, attributes.toList())

	suspend fun writeStream(src: AsyncInputStream, vararg attributes: Vfs.Attribute, autoClose: Boolean = true): Long {
		try {
			return put(src, *attributes)
		} finally {
			if (autoClose) src.close()
		}
	}

	suspend fun writeFile(file: VfsFile, vararg attributes: Vfs.Attribute): Long = file.copyTo(this, *attributes)

	suspend fun listNames(): List<String> = listSimple().map { it.baseName }

	suspend fun copyTo(target: AsyncOutputStream) = this.openUse { this.copyTo(target) }
	suspend fun copyTo(target: VfsFile, vararg attributes: Vfs.Attribute): Long = this.openInputStream().use { target.writeStream(this, *attributes) }

	fun withExtension(ext: String): VfsFile =
		VfsFile(vfs, fullNameWithoutExtension + if (ext.isNotEmpty()) ".$ext" else "")

	fun withCompoundExtension(ext: String): VfsFile =
		VfsFile(vfs, fullNameWithoutCompoundExtension + if (ext.isNotEmpty()) ".$ext" else "")

	fun appendExtension(ext: String): VfsFile =
		VfsFile(vfs, "$fullName.$ext")

	suspend fun open(mode: VfsOpenMode = VfsOpenMode.READ): AsyncStream = vfs.open(this.path, mode)
	suspend fun openInputStream(): AsyncInputStream = vfs.openInputStream(this.path)

	override suspend fun openRead(): AsyncStream = open(VfsOpenMode.READ)

	suspend inline fun <T> openUse(mode: VfsOpenMode = VfsOpenMode.READ, callback: AsyncStream.() -> T): T = open(mode).use(callback)

	suspend fun readRangeBytes(range: LongRange): ByteArray = vfs.readRange(this.path, range)
	suspend fun readRangeBytes(range: IntRange): ByteArray = vfs.readRange(this.path, range.toLongRange())

	// Aliases
	suspend fun readAll(): ByteArray = vfs.readRange(this.path, LONG_ZERO_TO_MAX_RANGE)

	suspend fun read(): ByteArray = readAll()
	suspend fun readBytes(): ByteArray = readAll()

	suspend fun readLines(charset: Charset = UTF8): Sequence<String> = readString(charset).lineSequence()
	suspend fun writeLines(lines: Iterable<String>, charset: Charset = UTF8) =
		writeString(lines.joinToString("\n"), charset = charset)

	suspend fun readString(charset: Charset = UTF8): String = read().toString(charset)

	suspend fun writeString(data: String, vararg attributes: Vfs.Attribute, charset: Charset = UTF8): Unit =
		run { write(data.toByteArray(charset), *attributes) }

	suspend fun readChunk(offset: Long, size: Int): ByteArray = vfs.readChunk(this.path, offset, size)
	suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit =
		vfs.writeChunk(this.path, data, offset, resize)

	suspend fun readAsSyncStream(): SyncStream = read().openSync()

	suspend fun stat(): VfsStat = vfs.stat(this.path)
	suspend fun touch(time: DateTime, atime: DateTime = time): Unit = vfs.touch(this.path, time, atime)
	suspend fun size(): Long = vfs.stat(this.path).size
	suspend fun exists(): Boolean = runIgnoringExceptions { vfs.stat(this.path).exists } ?: false
    suspend fun takeIfExists() = takeIf { it.exists() }
	suspend fun isDirectory(): Boolean = stat().isDirectory
    suspend fun isFile(): Boolean = !stat().isDirectory
	suspend fun setSize(size: Long): Unit = vfs.setSize(this.path, size)

	suspend fun delete() = vfs.delete(this.path)

	suspend fun setAttributes(attributes: List<Vfs.Attribute>) = vfs.setAttributes(this.path, attributes)
	suspend fun setAttributes(vararg attributes: Vfs.Attribute) = vfs.setAttributes(this.path, attributes.toList())

	suspend fun mkdir(attributes: List<Vfs.Attribute>) = vfs.mkdir(this.path, attributes)
	suspend fun mkdir(vararg attributes: Vfs.Attribute) = mkdir(attributes.toList())

	suspend fun copyToTree(
		target: VfsFile,
		vararg attributes: Vfs.Attribute,
		notify: suspend (Pair<VfsFile, VfsFile>) -> Unit = {}
	): Unit {
		notify(this to target)
		if (this.isDirectory()) {
			target.mkdir()
            list().collect { file ->
                file.copyToTree(target[file.baseName], *attributes, notify = notify)
            }
		} else {
			//println("copyToTree: $this -> $target")
			this.copyTo(target, *attributes)
		}
	}

	suspend fun ensureParents() = this.apply { parent.mkdir() }

	suspend fun renameTo(dstPath: String) = vfs.rename(this.path, dstPath)

	suspend fun listSimple(): List<VfsFile> = vfs.listSimple(this.path)
    suspend fun list(): Flow<VfsFile> = vfs.listFlow(this.path)

	suspend fun listRecursiveSimple(filter: (VfsFile) -> Boolean = { true }): List<VfsFile> = ArrayList<VfsFile>().apply {
		for (file in listSimple()) {
			if (filter(file)) {
				add(file)
				val stat = file.stat()
				if (stat.isDirectory) {
					addAll(file.listRecursiveSimple(filter))
				}
			}
		}
	}

	suspend fun listRecursive(filter: (VfsFile) -> Boolean = { true }): Flow<VfsFile> = flow {
        list().collect { file ->
            if (filter(file)) {
                emit(file)
                val stat = file.stat()
                if (stat.isDirectory) {
                    file.listRecursive(filter).collect { emit(it) }
                }
            }
        }
    }

	suspend fun exec(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		handler: VfsProcessHandler = VfsProcessHandler()
	): Int = vfs.exec(this.path, cmdAndArgs, env, handler)

	suspend fun execToString(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8,
		captureError: Boolean = false,
		throwOnError: Boolean = true
	): String {
		val out = ByteArrayBuilder()
		val err = ByteArrayBuilder()

		val result = exec(cmdAndArgs, env, object : VfsProcessHandler() {
			override suspend fun onOut(data: ByteArray) {
				out.append(data)
			}

			override suspend fun onErr(data: ByteArray) {
				if (captureError) out.append(data)
				err.append(data)
			}
		})

		val errString = err.toByteArray().toString(charset)
		val outString = out.toByteArray().toString(charset)

		if (throwOnError && result != 0) throw VfsProcessException("Process not returned 0, but $result. Error: $errString, Output: $outString")

		return outString
	}

	suspend fun execToString(vararg cmdAndArgs: String, charset: Charset = UTF8): String =
		execToString(cmdAndArgs.toList(), charset = charset)

	suspend fun passthru(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8
	): Int {
		return exec(cmdAndArgs.toList(), env, object : VfsProcessHandler() {
			override suspend fun onOut(data: ByteArray) = print(data.toString(charset))
			override suspend fun onErr(data: ByteArray) = print(data.toString(charset))
		}).also {
			println()
		}
	}

	suspend fun passthru(
		vararg cmdAndArgs: String,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8
	): Int = passthru(cmdAndArgs.toList(), env, charset)

	suspend fun watch(handler: suspend (Vfs.FileEvent) -> Unit): Closeable {
		//val cc = coroutineContext
		val cc = coroutineContext
		return vfs.watch(this.path) { event -> launchImmediately(cc) { handler(event) } }
	}

	suspend fun redirected(pathRedirector: suspend VfsFile.(String) -> String): VfsFile {
		val actualFile = this
		return VfsFile(object : Vfs.Proxy() {
			override suspend fun access(path: String): VfsFile =
				actualFile[actualFile.pathRedirector(path)]

			override fun toString(): String = "VfsRedirected"
		}, this.path)
	}

	fun jail(): VfsFile = JailVfs(this)
    fun jailParent(): VfsFile = JailVfs(parent)[this.baseName]

	suspend fun getUnderlyingUnscapedFile(): FinalVfsFile = vfs.getUnderlyingUnscapedFile(this.path)

	override fun toString(): String = "$vfs[${this.path}]"
}

fun VfsFile.toUnscaped() = FinalVfsFile(this)
fun FinalVfsFile.toFile() = this.file

//inline class FinalVfsFile(val file: VfsFile) {
data class FinalVfsFile(val file: VfsFile) {
	constructor(vfs: Vfs, path: String) : this(vfs[path])
	val vfs: Vfs get() = file.vfs
	val path: String get() = file.path
}

// @TODO: https://youtrack.jetbrains.com/issue/KT-31490
//suspend inline fun <R> VfsFile.useVfs(callback: suspend (VfsFile) -> R): R = vfs.use { callback(this@useVfs) }
suspend fun <R> VfsFile.useVfs(callback: suspend (VfsFile) -> R): R = vfs.use { callback(this@useVfs) }
