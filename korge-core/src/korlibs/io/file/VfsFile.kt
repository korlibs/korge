@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.io.file

import korlibs.datastructure.*
import korlibs.time.*
import korlibs.memory.*
import korlibs.io.async.*
import korlibs.io.experimental.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.io.util.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*

@OptIn(KorioExperimentalApi::class)
data class VfsFile(
	val vfs: Vfs,
	val path: String
) : VfsNamed(path.pathInfo), AsyncInputOpenable, Extra by Extra.Mixin() {
    @KorioExperimentalApi
    var cachedStat: VfsStat? = null

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

	suspend fun copyTo(target: AsyncOutputStream): Long = this.openUse { this.copyTo(target) }
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
    suspend inline fun <T> openUseIt(mode: VfsOpenMode = VfsOpenMode.READ, callback: (AsyncStream) -> T): T = open(mode).use(callback)

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
		run { write(data.toByteArray(charset), *attributes, Vfs.FileKind.STRING) }

	suspend fun readChunk(offset: Long, size: Int): ByteArray = vfs.readChunk(this.path, offset, size)
	suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit =
		vfs.writeChunk(this.path, data, offset, resize)

	suspend fun readAsSyncStream(): SyncStream = read().openSync()

    suspend fun stat(): VfsStat = cachedStat ?: vfs.stat(this.path)
	suspend fun touch(time: DateTime, atime: DateTime = time): Unit = vfs.touch(this.path, time, atime)
	suspend fun size(): Long = vfs.stat(this.path).size
	suspend fun exists(): Boolean = runIgnoringExceptions { vfs.stat(this.path).exists } ?: false
    suspend fun takeIfExists() = takeIf { it.exists() }
	suspend fun isDirectory(): Boolean = stat().isDirectory
    suspend fun isFile(): Boolean = stat().isFile
	suspend fun setSize(size: Long): Unit = vfs.setSize(this.path, size)

	suspend fun delete() = vfs.delete(this.path)

	suspend fun setAttributes(attributes: List<Vfs.Attribute>) = vfs.setAttributes(this.path, attributes)
	suspend fun setAttributes(vararg attributes: Vfs.Attribute) = vfs.setAttributes(this.path, attributes.toList())
    suspend fun getAttributes(): List<Vfs.Attribute> = vfs.getAttributes(this.path)
    suspend inline fun <reified T : Vfs.Attribute> getAttribute(): T? = getAttributes().filterIsInstance<T>().firstOrNull()
    suspend fun chmod(mode: Vfs.UnixPermissions): Unit = vfs.chmod(this.path, mode)

	suspend fun mkdir(attributes: List<Vfs.Attribute>) = vfs.mkdir(this.path, attributes)
	suspend fun mkdir(vararg attributes: Vfs.Attribute) = mkdir(attributes.toList())

    suspend fun mkdirs(attributes: List<Vfs.Attribute>) = vfs.mkdirs(this.path, attributes)
    suspend fun mkdirs(vararg attributes: Vfs.Attribute) = mkdirs(attributes.toList())

    /**
     * Copies this [VfsFile] into the [target] VfsFile.
     *
     * If this node is a file, the content will be copied.
     * If the node is a directory, a tree structure with the same content will be created in the target destination.
     */
    suspend fun copyToRecursively(
		target: VfsFile,
		vararg attributes: Vfs.Attribute,
		notify: suspend (Pair<VfsFile, VfsFile>) -> Unit = {}
	) {
		notify(this to target)
		if (this.isDirectory()) {
			target.mkdirs()
            list().collect { file ->
                file.copyToRecursively(target[file.baseName], *attributes, notify = notify)
            }
		} else {
			//println("copyToTree: $this -> $target")
			this.copyTo(target, *attributes)
		}
	}

	suspend fun ensureParents() = this.apply { parent.mkdir() }

    /** Renames this file into the [dstPath] relative to the root of this [vfs] */
	suspend fun renameTo(dstPath: String) = vfs.rename(this.path, dstPath)

    /** Renames the file determined by this plus [src] to this plus [dst] */
    suspend fun rename(src: String, dst: String) = vfs.rename("$path/$src", "$path/$dst")

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

    data class ProcessResult(val exitCode: Int, val stdout: String, val stderr: String)

    suspend fun execProcess(
        cmdAndArgs: List<String>,
        env: Map<String, String> = LinkedHashMap(),
        captureError: Boolean = false,
        charset: Charset = UTF8,
    ): ProcessResult {
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

        return ProcessResult(result, outString, errString)
    }

	suspend fun execToString(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8,
		captureError: Boolean = false,
		throwOnError: Boolean = true
	): String {
        val result = execProcess(cmdAndArgs, env, captureError, charset)
        if (throwOnError && result.exitCode != 0) {
            throw VfsProcessException("Process not returned 0, but ${result.exitCode}. Error: ${result.stderr}, Output: ${result.stdout}")
        }
        return result.stdout
    }

    suspend fun execProcess(
        vararg cmdAndArgs: String,
        env: Map<String, String> = LinkedHashMap(),
        captureError: Boolean = false,
        charset: Charset = UTF8,
    ): ProcessResult = execProcess(cmdAndArgs.toList(), env, captureError, charset)

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

suspend inline fun VfsFile.setUnixPermission(permissions: Vfs.UnixPermissions): Unit = setAttributes(permissions)
suspend inline fun VfsFile.getUnixPermission(): Vfs.UnixPermissions = getAttribute<Vfs.UnixPermissions>() ?: Vfs.UnixPermissions(0b111111111)

/**
 * Deletes all the files in this folder recursively.
 * If the entry is a file instead of a directory, the file is deleted.
 *
 * When [includeSelf] is set to false, this function will delete all
 * the descendants but the folder itself.
 */
suspend fun VfsFile.deleteRecursively(includeSelf: Boolean = true) {
    if (this.isDirectory()) {
        this.list().collect {
            if (it.isDirectory()) {
                it.deleteRecursively()
            } else {
                it.delete()
            }
        }
    }
    if (includeSelf) this.delete()
}

fun VfsFile.proxied(transform: suspend (VfsFile) -> VfsFile): VfsFile {
    val file = this
    return object : Vfs.Proxy() {
        override suspend fun access(path: String): VfsFile {
            return transform(file[path])
        }
    }[file.path]
}

fun VfsFile.withOnce(once: suspend (VfsFile) -> Unit): VfsFile {
    val file = this
    val executed = AsyncOnce<Unit>()
    return proxied {
        it.also { executed { once(file) } }
    }
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
