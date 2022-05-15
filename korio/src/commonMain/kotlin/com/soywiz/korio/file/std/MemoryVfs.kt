@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.file.std

import com.soywiz.korio.file.PathInfo
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.folder
import com.soywiz.korio.file.fullName
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.SyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsync

fun VfsFileFromData(data: ByteArray, ext: String = "bin") = MemoryVfsMix(mapOf("file.$ext" to data))["file.$ext"]
fun VfsFileFromData(data: String, ext: String = "bin", charset: Charset = Charsets.UTF8) = MemoryVfsMix(mapOf("file.$ext" to data), charset = charset)["file.$ext"]

fun SingleFileMemoryVfs(data: ByteArray, ext: String = "bin") = VfsFileFromData(data, ext)
fun SingleFileMemoryVfs(data: String, ext: String = "bin", charset: Charset = Charsets.UTF8) = VfsFileFromData(data, ext, charset)

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
