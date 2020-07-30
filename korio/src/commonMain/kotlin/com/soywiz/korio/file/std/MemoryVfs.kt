@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

fun VfsFileFromData(data: ByteArray, ext: String = "bin") = MemoryVfsMix(mapOf("file.$ext" to data))["file.$ext"]
fun VfsFileFromData(data: String, ext: String = "bin", charset: Charset = Charsets.UTF8) = MemoryVfsMix(mapOf("file.$ext" to data), charset = charset)["file.$ext"]

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
