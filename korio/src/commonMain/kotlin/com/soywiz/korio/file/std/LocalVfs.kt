package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

abstract class LocalVfs : Vfs() {
	companion object {
		operator fun get(base: String) = localVfs(base)
	}

	override fun toString(): String = "LocalVfs"
}

abstract class LocalVfsV2 : LocalVfs() {
	override suspend fun listSimple(path: String): List<VfsFile> = listFlow(path).toList()
    override suspend fun listFlow(path: String): Flow<VfsFile> = emptyFlow()
}

var resourcesVfsDebug = false
expect val resourcesVfs: VfsFile
expect fun cleanUpResourcesVfs()

expect val rootLocalVfs: VfsFile
expect val applicationVfs: VfsFile
expect val applicationDataVfs: VfsFile
expect val cacheVfs: VfsFile
expect val externalStorageVfs: VfsFile
expect val userHomeVfs: VfsFile
expect val tempVfs: VfsFile
val localCurrentDirVfs: VfsFile get() = applicationVfs

expect fun localVfs(path: String, async: Boolean = true): VfsFile
fun jailedLocalVfs(base: String): VfsFile = localVfs(base).jail()
