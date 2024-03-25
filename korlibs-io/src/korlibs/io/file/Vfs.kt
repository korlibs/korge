@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.io.file

import korlibs.io.async.*
import korlibs.io.experimental.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.stream.*
import korlibs.logger.*
import korlibs.memory.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*
import kotlin.math.min
import kotlin.reflect.*

abstract class Vfs : AsyncCloseable {
	protected open val absolutePath: String get() = ""

	open fun getAbsolutePath(path: String) = absolutePath.pathInfo.lightCombine(path.pathInfo).fullPath

	//val root = VfsFile(this, "")
	val root get() = VfsFile(this, "")

	open val supportedAttributeTypes: List<KClass<out Attribute>> get() = emptyList<KClass<out Attribute>>()

	operator fun get(path: String) = root[path]

	fun file(path: String) = root[path]

	override suspend fun close(): Unit = Unit

	fun createExistsStat(
		path: String, isDirectory: Boolean, size: Long, device: Long = -1, inode: Long = -1, mode: Int = 511,
		owner: String = "nobody", group: String = "nobody", createTime: DateTime = DateTime.EPOCH, modifiedTime: DateTime = DateTime.EPOCH,
		lastAccessTime: DateTime = modifiedTime, extraInfo: Any? = null, id: String? = null,
        cache: Boolean = false
	) = VfsStat(
		file = file(path), exists = true, isDirectory = isDirectory, size = size, device = device, inode = inode,
		mode = mode, owner = owner, group = group, createTime = createTime, modifiedTime = modifiedTime,
		lastAccessTime = lastAccessTime, extraInfo = extraInfo, id = id
	).also {
        if (cache) it.file.cachedStat = it
    }

	fun createNonExistsStat(path: String, extraInfo: Any? = null, cache: Boolean = false, exception: Throwable? = null) = VfsStat(
		file = file(path), exists = false, isDirectory = false, size = 0L,
		device = -1L, inode = -1L, mode = 511, owner = "nobody", group = "nobody",
		createTime = DateTime.EPOCH, modifiedTime = DateTime.EPOCH, lastAccessTime = DateTime.EPOCH, extraInfo = extraInfo,
        exception = exception
	)

	suspend fun exec(
		path: String,
		cmdAndArgs: List<String>,
		handler: VfsProcessHandler = VfsProcessHandler()
	): Int = exec(path, cmdAndArgs, Environment.getAll(), handler)

    protected suspend fun checkExecFolder(path: String, cmdAndArgs: List<String>) {
        val stat = stat(path)
        //println("stat=$stat")
        if (!stat.isDirectory) {
            throw FileNotFoundException("'$path' is not a directory, to execute '${cmdAndArgs.first()}'")
        }
    }

	open suspend fun exec(
		path: String,
		cmdAndArgs: List<String>,
		env: Map<String, String>,
		handler: VfsProcessHandler = VfsProcessHandler()
	): Int {
        checkExecFolder(path, cmdAndArgs)
        unsupported()
    }

	open suspend fun open(path: String, mode: VfsOpenMode): AsyncStream = unsupported()

	open suspend fun openInputStream(path: String): AsyncInputStream = open(path, VfsOpenMode.READ)

	open suspend fun readRange(path: String, range: LongRange): ByteArray = open(path, VfsOpenMode.READ).useIt { s ->
        s.position = range.start
        s.readBytesUpTo(min(Int.MAX_VALUE.toLong() - 1, (range.endInclusive - range.start)).toInt() + 1)
    }

	interface Attribute

    inline fun <reified T : Attribute> List<Attribute>.getOrNull(): T? = filterIsInstance<T>().firstOrNull()

    inline class UnixPermission(val bits: Int) {
        constructor(readable: Boolean = true, writable: Boolean = true, executable: Boolean = false) : this(
            0.insert(readable, 2).insert(writable, 1).insert(executable, 0)
        )
        val executable: Boolean get() = bits.extract(0)
        val writable: Boolean get() = bits.extract(1)
        val readable: Boolean get() = bits.extract(2)
    }

    inline class UnixPermissions(val bits: Int) : Attribute {
        override fun toString(): String = ("0000" + bits.toString(8)).substr(-4)

        constructor(owner: UnixPermission, group: UnixPermission = owner, other: UnixPermission = UnixPermission(0), extra: Int = 0) : this(
            0.insert(owner.bits, 6, 3).insert(group.bits, 3, 3).insert(other.bits, 0, 3).insert(extra, 9, 7)
        )

        fun copy(
            owner: UnixPermission = this.owner,
            group: UnixPermission = this.group,
            other: UnixPermission = this.other,
            extra: Int = 0,
        ): UnixPermissions = UnixPermissions(owner, group, other, extra)

        val extra: Int get() = bits.extract(9, 7)
        val rbits: Int get() = bits.extract(0, 9)
        val owner: UnixPermission get() = UnixPermission(bits.extract(6, 3))
        val group: UnixPermission get() = UnixPermission(bits.extract(3, 3))
        val other: UnixPermission get() = UnixPermission(bits.extract(0, 3))
    }

    interface FileKind : Attribute {
        val name: String

        companion object {
            val BINARY get() = Standard.BINARY
            val STRING get() = Standard.STRING
            val LONG get() = Standard.LONG
            val INT get() = Standard.INT
        }
        enum class Standard : FileKind {
            BINARY, STRING, LONG, INT
        }
    }

    open fun getKind(value: Any?): FileKind = when (value) {
        null, is ByteArray -> FileKind.BINARY
        is String -> FileKind.STRING
        is Int -> FileKind.INT
        is Long -> FileKind.LONG
        else -> FileKind.BINARY
    }

    inline fun <reified T> Iterable<Attribute>.get(): T? = this.firstOrNull { it is T } as T?

	open suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute> = listOf()): Long {
		return open(path, VfsOpenMode.CREATE_OR_TRUNCATE).use {
			content.copyTo(this)
		}
	}

	suspend fun put(path: String, content: ByteArray, attributes: List<Attribute> = listOf()): Long {
		return put(path, content.openAsync(), attributes)
	}

	suspend fun readChunk(path: String, offset: Long, size: Int): ByteArray {
		val s = open(path, VfsOpenMode.READ)
		if (offset != 0L) s.setPosition(offset)
		return s.readBytesUpTo(size)
	}

	suspend fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean) {
		val s = open(path, if (resize) VfsOpenMode.CREATE_OR_TRUNCATE else VfsOpenMode.CREATE)
		s.setPosition(offset)
		s.writeBytes(data)
	}

	open suspend fun setSize(path: String, size: Long) {
		open(path, mode = VfsOpenMode.CREATE).use { this.setLength(size) }
	}

	open suspend fun setAttributes(path: String, attributes: List<Attribute>): Unit {
        attributes.getOrNull<UnixPermissions>()?.let {
            chmod(path, it)
        }
    }
    open suspend fun getAttributes(path: String): List<Attribute> = emptyList()

    /**
     * Change Unix Permissions for [path] to [mode]
     */
    open suspend fun chmod(path: String, mode: UnixPermissions): Unit = Unit

	open suspend fun stat(path: String): VfsStat = createNonExistsStat(path)

    private fun unsupported(): Nothing = korlibs.io.lang.unsupported("unsupported for ${this::class} : $this")

	suspend fun listSimple(path: String): List<VfsFile> = this.listFlow(path).toList()
    open suspend fun listFlow(path: String): Flow<VfsFile> = unsupported()

	open suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = unsupported()
    open suspend fun mkdirs(path: String, attributes: List<Attribute>): Boolean {
        if (path == "") return false
        if (stat(path).exists) return false // Already exists, and it is a directory
        //println("mkdirs: $path")
        if (mkdir(path, attributes)) return true
        //println("::mkdirs: $parent")
        mkdirs(PathInfo(path).parent.fullPath, attributes)
        return mkdir(path, attributes)
    }
	open suspend fun rmdir(path: String): Boolean = delete(path) // For compatibility
	open suspend fun delete(path: String): Boolean = unsupported()
	open suspend fun rename(src: String, dst: String): Boolean {
        if (file(src).isDirectory()) error("Unsupported renaming directories in $this")
		file(src).copyTo(file(dst))
		delete(src)
		return true
	}

	open suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable =
		DummyCloseable

	open suspend fun touch(path: String, time: DateTime, atime: DateTime) = Unit

	open suspend fun getUnderlyingUnscapedFile(path: String): FinalVfsFile =
		FinalVfsFile(this, path)

	abstract class Proxy : Vfs() {
		private val logger = Logger("Vfs.Proxy")
		protected abstract suspend fun access(path: String): VfsFile
		protected open suspend fun VfsFile.transform(): VfsFile = file(this.path)
		//suspend protected fun transform2_f(f: VfsFile): VfsFile = transform(f)

		final override suspend fun getUnderlyingUnscapedFile(path: String): FinalVfsFile = initOnce().access(path).getUnderlyingUnscapedFile()

		protected open suspend fun init() {
		}

        private var initialized: Deferred<Unit>? = null
		protected suspend fun initOnce(): Proxy {
			if (initialized == null) {
                initialized = async(coroutineContext) {
                    try {
                        init()
                    } catch (e: Throwable) {
                        logger.error { "Error initializing $this" }
                        e.printStackTrace()
                    }
                }
			}
            initialized!!.await()
			return this
		}

        override suspend fun exec(
			path: String,
			cmdAndArgs: List<String>,
			env: Map<String, String>,
			handler: VfsProcessHandler
		): Int = initOnce().access(path).exec(cmdAndArgs, env, handler)

		override suspend fun open(path: String, mode: VfsOpenMode) = initOnce().access(path).open(mode)

		override suspend fun readRange(path: String, range: LongRange): ByteArray =
			initOnce().access(path).readRangeBytes(range)

		override suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>) =
			initOnce().access(path).put(content, *attributes.toTypedArray())

		override suspend fun setSize(path: String, size: Long): Unit = initOnce().access(path).setSize(size)
		override suspend fun stat(path: String): VfsStat = initOnce().access(path).stat().copy(file = file(path))
        override suspend fun listFlow(path: String): Flow<VfsFile> = flow {
            initOnce()
            access(path).list().collect { emit(it.transform()) }
        }

        override suspend fun delete(path: String): Boolean = initOnce().access(path).delete()
		override suspend fun setAttributes(path: String, attributes: List<Attribute>) =
			initOnce().access(path).setAttributes(*attributes.toTypedArray())
        override suspend fun getAttributes(path: String) =
            initOnce().access(path).getAttributes()

        override suspend fun chmod(path: String, mode: Vfs.UnixPermissions) =
            initOnce().access(path).chmod(mode)

        override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean =
			initOnce().access(path).mkdir(*attributes.toTypedArray())

		override suspend fun touch(path: String, time: DateTime, atime: DateTime): Unit =
			initOnce().access(path).touch(time, atime)

		override suspend fun rename(src: String, dst: String): Boolean {
			initOnce()
			val srcFile = access(src)
			val dstFile = access(dst)
			if (srcFile.vfs != dstFile.vfs) throw IllegalArgumentException("Can't rename between filesystems. Use copyTo instead, and remove later.")
			return srcFile.renameTo(dstFile.path)
		}

		override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
			initOnce()
			return access(path).watch { e ->
				launchImmediately(coroutineContext) {
					val f1 = e.file.transform()
					val f2 = e.other?.transform()
					handler(e.copy(file = f1, other = f2))
				}
			}
		}
	}

	open class Decorator(val parent: VfsFile) : Proxy() {
		val parentVfs = parent.vfs
		override suspend fun access(path: String): VfsFile = parentVfs[path]
	}

	data class FileEvent(val kind: Kind, val file: VfsFile, val other: VfsFile? = null) {
		enum class Kind { DELETED, MODIFIED, CREATED, RENAMED }

		override fun toString() = if (other != null) "$kind($file, $other)" else "$kind($file)"
	}

	override fun toString(): String = this::class.portableSimpleName
}

enum class VfsOpenMode(
    val cmode: String,
    val write: Boolean,
    val createIfNotExists: Boolean = false,
    val truncate: Boolean = false,
    val read: Boolean = true,
    val append: Boolean = false,
    val createNew: Boolean = false,
) {
    READ("rb", write = false),
    WRITE("r+b", write = true, createIfNotExists = true),
    APPEND("a+b", write = true, createIfNotExists = true, append = true),
    CREATE_OR_TRUNCATE("w+b", write = true, createIfNotExists = true, truncate = true),
    CREATE("w+b", write = true, createIfNotExists = true),
    CREATE_NEW("w+b", write = true);
}

//"r"	Open for reading only. Invoking any of the write methods of the resulting object will cause an IOException to be thrown.
//"rw"	Open for reading and writing. If the file does not already exist then an attempt will be made to create it.
//"rws"	Open for reading and writing, as with "rw", and also require that every update to the file's content or metadata be written synchronously to the underlying storage device.
//"rwd"  	Open for reading and writing, as with "rw", and also require that every update to the file's content be written synchronously to the underlying storage device.

open class VfsProcessHandler {
	open suspend fun onOut(data: ByteArray): Unit = Unit
	open suspend fun onErr(data: ByteArray): Unit = Unit
}

class VfsProcessException(message: String) : IOException(message)

@OptIn(KorioExperimentalApi::class)
data class VfsStat(
    val file: VfsFile,
    val exists: Boolean,
    val isDirectory: Boolean,
    val size: Long,
    val device: Long = -1L,
    val inode: Long = -1L,
    val mode: Int = 511,
    val owner: String = "nobody",
    val group: String = "nobody",
    val createTime: DateTime = DateTime.EPOCH,
    val modifiedTime: DateTime = createTime,
    val lastAccessTime: DateTime = modifiedTime,
    val extraInfo: Any? = null,
    val kind: Vfs.FileKind? = null,
    val id: String? = null,
    val exception: Throwable? = null,
) : Path by file {
    val isFile: Boolean get() = exists && !isDirectory

    /**
     * Gets the unix-like permissions inverse of [VfsFile.chmod]
     */
    val permissions: Vfs.UnixPermissions get() = Vfs.UnixPermissions(mode)

    val enrichedFile: VfsFile get() = file.copy().also { it.cachedStat = this }

	fun toString(showFile: Boolean): String = "VfsStat(" + ArrayList<String>(16).also { al ->
		if (showFile) al.add("file=$file") else al.add("file=${file.absolutePath}")
		al.add("exists=$exists")
		al.add("isDirectory=$isDirectory")
		al.add("size=$size")
		al.add("device=$device")
		al.add("inode=$inode")
		al.add("mode=$mode")
		al.add("owner=$owner")
		al.add("group=$group")
		al.add("createTime=$createTime")
		al.add("modifiedTime=$modifiedTime")
		al.add("lastAccessTime=$lastAccessTime")
		al.add("extraInfo=$extraInfo")
        if (kind != null) {
            al.add("kind=$kind")
        }
		al.add("id=$id")
	}.joinToString(", ") + ")"

	override fun toString(): String = toString(showFile = true)
}

class VfsCachedStatContext(val stat: VfsStat?) : CoroutineContext.Element {
    companion object : CoroutineContext.Key<VfsCachedStatContext>

    override val key get() = VfsCachedStatContext
}

//val VfsStat.createLocalDate: LocalDateTime get() = LocalDateTime.ofEpochSecond(createTime / 1000L, ((createTime % 1_000L) * 1_000_000L).toInt(), ZoneOffset.UTC)
//val VfsStat.modifiedLocalDate: LocalDateTime get() = LocalDateTime.ofEpochSecond(modifiedTime / 1000L, ((modifiedTime % 1_000L) * 1_000_000L).toInt(), ZoneOffset.UTC)
//val VfsStat.lastAccessLocalDate: LocalDateTime get() = LocalDateTime.ofEpochSecond(lastAccessTime / 1000L, ((lastAccessTime % 1_000L) * 1_000_000L).toInt(), ZoneOffset.UTC)

//val INIT = Unit.apply { println("UTC_OFFSET: $UTC_OFFSET")  }

val VfsStat.createDate: DateTime get() = createTime
val VfsStat.modifiedDate: DateTime get() = modifiedTime
val VfsStat.lastAccessDate: DateTime get() = lastAccessTime

suspend fun ByteArray.writeToFile(path: String) = localVfs(path).write(this)
suspend fun ByteArray.writeToFile(file: VfsFile) = file.write(this)
