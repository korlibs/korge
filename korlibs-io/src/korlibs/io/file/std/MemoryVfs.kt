@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package korlibs.io.file.std

import korlibs.io.file.*
import korlibs.io.lang.*
import korlibs.io.stream.*

fun SingleFileMemoryVfsWithName(data: ByteArray, name: String) = MemoryVfsMix(mapOf(name to data))[name]
fun SingleFileMemoryVfsWithName(data: String, name: String, charset: Charset = Charsets.UTF8) = MemoryVfsMix(mapOf(name to data), charset = charset)[name]

fun VfsFileFromData(data: ByteArray, ext: String = "bin", basename: String = "file") = SingleFileMemoryVfsWithName(data, "$basename.$ext")
fun VfsFileFromData(data: String, ext: String = "bin", charset: Charset = Charsets.UTF8, basename: String = "file") = SingleFileMemoryVfsWithName(data, "$basename.$ext", charset)

fun SingleFileMemoryVfs(data: ByteArray, ext: String = "bin", basename: String = "file") = VfsFileFromData(data, ext, basename)
fun SingleFileMemoryVfs(data: String, ext: String = "bin", charset: Charset = Charsets.UTF8, basename: String = "file") = VfsFileFromData(data, ext, charset, basename)

fun MemoryVfs(items: Map<String, AsyncStream> = LinkedHashMap(), caseSensitive: Boolean = true): VfsFile {
	val vfs = NodeVfs(caseSensitive)
	for ((path, stream) in items) {
		val info = PathInfo(path)
		val folderNode = vfs.rootNode.access(info.folder, createFolders = true)
		val fileNode = folderNode.createChild(info.baseName, isDirectory = false)
		fileNode.stream = stream
	}
	return vfs.root
}

fun MemoryVfsMix(
	items: Map<String, Any> = LinkedHashMap(),
	caseSensitive: Boolean = true,
	charset: Charset = UTF8
): VfsFile = MemoryVfs(items.mapValues { (_, v) ->
	when (v) {
		is SyncStream -> v.toAsync()
		is ByteArray -> v.openAsync()
		is String -> v.openAsync(charset)
		else -> v.toString().toByteArray(charset).openAsync()
	}
}, caseSensitive)

fun MemoryVfsMix(vararg items: Pair<String, Any>, caseSensitive: Boolean = true, charset: Charset = UTF8): VfsFile = MemoryVfsMix(items.toMap(), caseSensitive, charset)

fun ByteArray.asMemoryVfsFile(name: String = "temp.bin"): VfsFile = MemoryVfs(mapOf(name to openAsync()))[name]
suspend fun VfsFile.cachedToMemory(): VfsFile = this.readAll().asMemoryVfsFile(this.fullName)
