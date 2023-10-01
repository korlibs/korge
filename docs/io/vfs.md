---
permalink: /io/vfs/
group: io
layout: default
title: File System
title_short: File System
description: "PathInfo, VfsFile, Vfs, IsoVfs, JailVfs, LocalVfs, LogVfs, MemoryVfs, MergedVfs, MountableVfs, NodeVfs, UrlVfs, ZipVfs"
fa-icon: fa-copy
priority: 1
---

KorIO has a Virtual File System functionality.

## File System

## PathInfo

```kotlin
expect val File_separatorChar: Char

inline class PathInfo(val fullPath: String)
val String.pathInfo: PathInfo
interface Path { val pathInfo: PathInfo }

fun PathInfo.parts(): List<String>
fun PathInfo.normalize(): String 
fun PathInfo.combine(access: PathInfo): PathInfo
fun PathInfo.lightCombine(access: PathInfo): PathInfo
fun PathInfo.isAbsolute(): Boolean
fun PathInfo.normalizeAbsolute(): PathInfo

// Direct PathInfo
val PathInfo.fullPathNormalized: String
val PathInfo.folder: String
val PathInfo.folderWithSlash: String
val PathInfo.baseName: String
val PathInfo.fullPathWithoutExtension: String
fun PathInfo.fullPathWithExtension(ext: String): String
val PathInfo.baseNameWithoutExtension: String
val PathInfo.baseNameWithoutCompoundExtension: String
val PathInfo.fullNameWithoutExtension: String
val PathInfo.fullNameWithoutCompoundExtension: String
fun PathInfo.baseNameWithExtension(ext: String): String
fun PathInfo.baseNameWithCompoundExtension(ext: String): String
val PathInfo.extension: String
val PathInfo.extensionLC: String
val PathInfo.compoundExtension: String
val PathInfo.compoundExtensionLC: String
val PathInfo.mimeTypeByExtension: MimeType
fun PathInfo.getPathComponents(): List<String>
fun PathInfo.getPathFullComponents(): List<String>
val PathInfo.fullName: String

// For instances including a pathInfo
val Path.fullPathNormalized: String
val Path.folder: String
val Path.folderWithSlash: String
val Path.baseName: String
val Path.fullPathWithoutExtension: String
fun Path.fullPathWithExtension(ext: String): String
val Path.fullNameWithoutExtension: String
val Path.baseNameWithoutExtension: String
val Path.fullNameWithoutCompoundExtension: String
val Path.baseNameWithoutCompoundExtension: String
fun Path.baseNameWithExtension(ext: String): String
fun Path.baseNameWithCompoundExtension(ext: String): String
val Path.extension: String
val Path.extensionLC: String
val Path.compoundExtension: String
val Path.compoundExtensionLC: String
val Path.mimeTypeByExtension: MimeType
fun Path.getPathComponents(): List<String>
fun Path.getPathFullComponents(): List<String>
val Path.fullName: String
```

## VfsFile

```kotlin
data class VfsFile(
	val vfs: Vfs,
	val path: String
) : VfsNamed(path.pathInfo), AsyncInputOpenable, Extra by Extra.Mixin() {
	val parent: VfsFile
	val root: VfsFile
	val absolutePath: String

	operator fun get(path: String): VfsFile

	// @TODO: Kotlin suspend operator not supported yet!
	suspend fun set(path: String, content: String)
	suspend fun set(path: String, content: ByteArray)
	suspend fun set(path: String, content: AsyncStream)
	suspend fun set(path: String, content: VfsFile)

	suspend fun put(content: AsyncInputStream, attributes: List<Vfs.Attribute> = listOf()): Long
	suspend fun put(content: AsyncInputStream, vararg attributes: Vfs.Attribute): Long
	suspend fun write(data: ByteArray, vararg attributes: Vfs.Attribute): Long
	suspend fun writeBytes(data: ByteArray, vararg attributes: Vfs.Attribute): Long

	suspend fun writeStream(src: AsyncInputStream, vararg attributes: Vfs.Attribute, autoClose: Boolean = true): Long
	suspend fun writeFile(file: VfsFile, vararg attributes: Vfs.Attribute): Long
	suspend fun listNames(): List<String>
	suspend fun copyTo(target: AsyncOutputStream)
	suspend fun copyTo(target: VfsFile, vararg attributes: Vfs.Attribute): Long
	fun withExtension(ext: String): VfsFile
	fun withCompoundExtension(ext: String): VfsFile
	fun appendExtension(ext: String): VfsFile

	suspend fun open(mode: VfsOpenMode = VfsOpenMode.READ): AsyncStream
	suspend fun openInputStream(): AsyncInputStream

	override suspend fun openRead(): AsyncStream
	suspend inline fun <T> openUse(mode: VfsOpenMode
	suspend fun readRangeBytes(range: LongRange): ByteArray
	suspend fun readRangeBytes(range: IntRange): ByteArray

	suspend fun readAll(): ByteArray

	suspend fun read(): ByteArray
	suspend fun readBytes(): ByteArray

	suspend fun readLines(charset: Charset = UTF8): List<String>
	suspend fun writeLines(lines: List<String>, charset: Charset = UTF8)

	suspend fun readString(charset: Charset = UTF8): String
	suspend fun writeString(data: String, vararg attributes: Vfs.Attribute, charset: Charset = UTF8): Unit

	suspend fun readChunk(offset: Long, size: Int): ByteArray
	suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit 

	suspend fun readAsSyncStream(): SyncStream

	suspend fun stat(): VfsStat
	suspend fun touch(time: DateTime, atime: DateTime = time): Unit
	suspend fun size(): Long
	suspend fun exists(): Boolean
	suspend fun isDirectory(): Boolean
	suspend fun setSize(size: Long): Unit
	suspend fun delete(): Unit
	suspend fun setAttributes(attributes: List<Vfs.Attribute>)
	suspend fun setAttributes(vararg attributes: Vfs.Attribute)

	suspend fun mkdir(attributes: List<Vfs.Attribute>)
	suspend fun mkdir(vararg attributes: Vfs.Attribute)

	suspend fun copyToTree(
		target: VfsFile,
		vararg attributes: Vfs.Attribute,
		notify: suspend (Pair<VfsFile, VfsFile>) -> Unit = {}
	): Unit

	suspend fun ensureParents(): VfsFile
	suspend fun renameTo(dstPath: String): Unit
	suspend fun list(): ReceiveChannel<VfsFile>
	suspend fun listRecursive(filter: (VfsFile) -> Boolean = { true }): ReceiveChannel<VfsFile>

	suspend fun exec(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		handler: VfsProcessHandler = VfsProcessHandler()
	): Int

	suspend fun execToString(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8,
		captureError: Boolean = false,
		throwOnError: Boolean = true
	): String

	suspend fun execToString(vararg cmdAndArgs: String, charset: Charset = UTF8): String

	suspend fun passthru(
		cmdAndArgs: List<String>,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8
	): Int

	suspend fun passthru(
		vararg cmdAndArgs: String,
		env: Map<String, String> = LinkedHashMap(),
		charset: Charset = UTF8
	): Int

	suspend fun watch(handler: suspend (Vfs.FileEvent) -> Unit): Closeable
	suspend fun redirected(pathRedirector: suspend VfsFile.(String) -> String): VfsFile
	fun jail(): VfsFile
	suspend fun getUnderlyingUnscapedFile(): FinalVfsFile
}

fun VfsFile.toUnscaped(): FinalVfsFile
fun FinalVfsFile.toFile(): VfsFile

data class FinalVfsFile(val file: VfsFile) {
    constructor(vfs: Vfs, path: String) : this(vfs[path])
    val vfs: Vfs
    val path: String
}

suspend inline fun <R> VfsFile.useVfs(callback: suspend (VfsFile) -> R): R
```

## Vfs

```kotlin
open class VfsNamed(override val pathInfo: PathInfo) : Path
```

```kotlin
interface SimpleStorage {
	suspend fun get(key: String): String?
	suspend fun set(key: String, value: String)
	suspend fun remove(key: String)
}
```

```kotlin
abstract class Vfs : AsyncCloseable {
	open fun getAbsolutePath(path: String): String
	
    val root: VfsFile
	open val supportedAttributeTypes: List<KClass<out Attribute>>()
	
    operator fun get(path: String): VfsFile
	fun file(path: String) = root[path]

	override suspend fun close(): Unit = Unit

	fun createExistsStat(
		path: String, isDirectory: Boolean, size: Long, device: Long = -1, inode: Long = -1, mode: Int = 511,
		owner: String = "nobody", group: String = "nobody", createTime: DateTime = DateTime.EPOCH, modifiedTime: DateTime = DateTime.EPOCH,
		lastAccessTime: DateTime = modifiedTime, extraInfo: Any? = null, id: String? = null
	): VfsStat

	fun createNonExistsStat(path: String, extraInfo: Any? = null): VfsStat

	suspend fun exec(path: String, cmdAndArgs: List<String>, handler: VfsProcessHandler = VfsProcessHandler()): Int

	open suspend fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler = VfsProcessHandler()): Int

	open suspend fun open(path: String, mode: VfsOpenMode): AsyncStream
	open suspend fun openInputStream(path: String): AsyncInputStream
	open suspend fun readRange(path: String, range: LongRange): ByteArray

	open suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute> = listOf()): Long
	suspend fun put(path: String, content: ByteArray, attributes: List<Attribute> = listOf()): Long
	suspend fun readChunk(path: String, offset: Long, size: Int): ByteArray
	suspend fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean)
	open suspend fun setSize(path: String, size: Long)
	open suspend fun setAttributes(path: String, attributes: List<Attribute>): Unit
	open suspend fun stat(path: String): VfsStat
	open suspend fun list(path: String): ReceiveChannel<VfsFile>
	open suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean
	open suspend fun rmdir(path: String): Boolean
	open suspend fun delete(path: String): Boolean
	open suspend fun rename(src: String, dst: String): Boolean
	open suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable
	open suspend fun touch(path: String, time: DateTime, atime: DateTime)
	open suspend fun getUnderlyingUnscapedFile(path: String): FinalVfsFile

	interface Attribute
	inline fun <reified T> Iterable<Attribute>.get(): T?

	abstract class Proxy : Vfs() {
		protected abstract suspend fun access(path: String): VfsFile
		protected open suspend fun VfsFile.transform(): VfsFile = file(this.path)
		final override suspend fun getUnderlyingUnscapedFile(path: String): FinalVfsFile = initOnce().access(path).getUnderlyingUnscapedFile()
		protected open suspend fun init() = Unit
		var initialized = false
	}

	open class Decorator(val parent: VfsFile) : Proxy() {
		val parentVfs = parent.vfs
		override suspend fun access(path: String): VfsFile = parentVfs[path]
	}

	data class FileEvent(val kind: Kind, val file: VfsFile, val other: VfsFile? = null) {
		enum class Kind { DELETED, MODIFIED, CREATED, RENAMED }
	}
}

enum class VfsOpenMode(
    val cmode: String,
    val write: Boolean,
    val createIfNotExists: Boolean = false,
    val truncate: Boolean = false
) {
    READ("rb", write = false),
    WRITE("r+b", write = true, createIfNotExists = true),
    APPEND("a+b", write = true, createIfNotExists = true),
    CREATE_OR_TRUNCATE("w+b", write = true, createIfNotExists = true, truncate = true),
    CREATE("w+b", write = true, createIfNotExists = true),
    CREATE_NEW("w+b", write = true);
}

open class VfsProcessHandler {
    open suspend fun onOut(data: ByteArray): Unit = Unit
    open suspend fun onErr(data: ByteArray): Unit = Unit
}

class VfsProcessException(message: String) : IOException(message)

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
    val id: String? = null
) : Path by file

val VfsStat.createDate: DateTime get() = createTime
val VfsStat.modifiedDate: DateTime get() = modifiedTime
val VfsStat.lastAccessDate: DateTime get() = lastAccessTime

suspend fun ByteArray.writeToFile(path: String) = localVfs(path).write(this)
suspend fun ByteArray.writeToFile(file: VfsFile) = file.write(this)
```

## Standard File Systems

```kotlin
var resourcesVfsDebug = false
val resourcesVfs: VfsFile
val rootLocalVfs: VfsFile
val applicationVfs: VfsFile
val applicationDataVfs: VfsFile
val cacheVfs: VfsFile
val externalStorageVfs: VfsFile
val userHomeVfs: VfsFile
val tempVfs: VfsFile
val localCurrentDirVfs: VfsFile
fun localVfs(path: String): VfsFile
fun jailedLocalVfs(base: String): VfsFile
```

### IsoVfs

```kotlin
suspend fun IsoVfs(file: VfsFile): VfsFile
suspend fun IsoVfs(s: AsyncStream): VfsFile
suspend fun AsyncStream.openAsIso(): IsoVfs
suspend fun VfsFile.openAsIso(): IsoVfs
suspend fun <R> AsyncStream.openAsIso(callback: suspend (VfsFile) -> R): R
suspend fun <R> VfsFile.openAsIso(callback: suspend (VfsFile) -> R): R
```

### JailVfs

```kotlin
fun VfsFile.jail(): VfsFile
fun JailVfs(jailRoot: VfsFile): VfsFile
```

### LocalVfs

```kotlin
abstract class LocalVfs : Vfs() {
	companion object { operator fun get(base: String): VfsFile = localVfs(base) }
	override fun toString(): String = "LocalVfs"
}
```

### LogVfs

```kotlin
fun VfsFile.log(): VfsFile = LogVfs(this).root

class LogVfs(val parent: VfsFile) : Vfs.Proxy() {
	val log: List<String>
	val logstr: Strin
	val modifiedFiles: Set<String>()
}
```

### MapLikeStorageVfs

```kotlin
fun SimpleStorage.toVfs(): VfsFile = MapLikeStorageVfs(this).root
class MapLikeStorageVfs(val storage: SimpleStorage) : Vfs()
```

### MemoryVfs

```kotlin
fun MemoryVfs(items: Map<String, AsyncStream> = LinkedHashMap(), caseSensitive: Boolean = true): VfsFile
fun MemoryVfsMix(
	items: Map<String, Any> = LinkedHashMap(),
	caseSensitive: Boolean = true,
	charset: Charset = UTF8
): VfsFile
fun MemoryVfsMix(vararg items: Pair<String, Any>, caseSensitive: Boolean = true, charset: Charset = UTF8): VfsFile
```

### MergedVfs

```kotlin
open class MergedVfs(vfsList: List<VfsFile> = listOf()) : Vfs.Proxy() {
	operator fun plusAssign(other: VfsFile)
	operator fun minusAssign(other: VfsFile)
}
```

### MountableVfs

```kotlin
interface Mountable {
	fun mount(folder: String, file: VfsFile): Mountable
	fun unmount(folder: String): Mountable
}

// VfsFile.vfs is Mountable
suspend fun MountableVfs(closeMounts: Boolean = false, callback: suspend Mountable.() -> Unit): VfsFile
```

### NodeVfs

```kotlin
open class NodeVfs(val caseSensitive: Boolean = true) : Vfs() {
	val events = Signal<FileEvent>()
	val rootNode = Node("", isDirectory = true)

	open inner class Node() : Iterable<Node> {
		val name: String
		val isDirectory: Boolean
		parent: Node? = null
		val nameLC: String
		
		var parent: Node?
		var data: Any?
		val children: Map<String, Node>()
		val childrenLC: Map<String, Node>()
		val root: Node
		var stream: AsyncStream? = null

		fun child(name: String): Node?
		fun createChild(name: String, isDirectory: Boolean = false): Node

		operator fun get(path: String): Node

		fun getOrNull(path: String): Node?

		fun access(path: String, createFolders: Boolean = false): Node
		fun mkdir(name: String): Boolean
	}
}
```

### UniversalVfs

```kotlin
val String.uniVfs: UniversalVfs
fun String.uniVfs(providers: UniSchemaProviders, base: VfsFile? = null): VfsFile 

object UniversalVfs {
	operator fun invoke(uri: String, providers: UniSchemaProviders, base: VfsFile? = null): VfsFile
}

class UniSchema(val name: String, val provider: (URL) -> VfsFile)

class UniSchemaProviders(val providers: Map<String, UniSchema>) {
	constructor(providers: Iterable<UniSchema>)
	constructor(vararg providers: UniSchema)
}

var defaultUniSchema = UniSchemaProviders(
	UniSchema("http") { UrlVfs(it) },
	UniSchema("https") { UrlVfs(it) },
	UniSchema("file") { rootLocalVfs[it.path] }
)

fun registerUniSchema(schema: UniSchema)

inline fun <T> registerUniSchemaTemporarily(schema: UniSchema, callback: () -> T): T

operator fun UniSchemaProviders.plus(other: UniSchemaProviders)
operator fun UniSchemaProviders.plus(other: UniSchema)

operator fun UniSchemaProviders.minus(other: UniSchemaProviders): UniSchemaProviders
operator fun UniSchemaProviders.minus(other: UniSchema)
```

### UrlVfs

```kotlin
fun UrlVfs(url: String, client: HttpClient = createHttpClient()): VfsFile
fun UrlVfs(url: URL, client: HttpClient = createHttpClient()): VfsFile
fun UrlVfsJailed(url: String, client: HttpClient = createHttpClient()): VfsFile
fun UrlVfsJailed(url: URL, client: HttpClient = createHttpClient()): VfsFile

class UrlVfs(val url: String, val dummy: Unit, val client: HttpClient = createHttpClient()) : Vfs() {
	class HttpHeaders(val headers: Http.Headers) : Attribute

	fun getFullUrl(path: String): String {
		val result = url.trim('/') + '/' + path.trim('/')
		//println("UrlVfs.getFullUrl: url=$url, path=$path, result=$result")
		return result
	}
}
```

### ZipVfs

```kotlin
// Create Zip File
suspend fun VfsFile.createZipFromTree(): ByteArray
suspend fun VfsFile.createZipFromTreeTo(s: AsyncStream)

// Opening Zip File
suspend fun VfsFile.openAsZip(caseSensitive: Boolean = true): VfsFile
suspend fun AsyncStream.openAsZip(caseSensitive: Boolean = true)suspend fun <R> VfsFile.openAsZip(caseSensitive: Boolean = true, callback: suspend (VfsFile) -> R): R
suspend fun <R> AsyncStream.openAsZip(caseSensitive: Boolean = true, callback: suspend (VfsFile) -> R): R
suspend fun ZipVfs(s: AsyncStream, zipFile: VfsFile? = null, caseSensitive: Boolean = true, closeStream: Boolean = false): VfsFile
```
