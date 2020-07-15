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
    final override suspend fun list(path: String): ReceiveChannel<VfsFile> = listFlow(path).toChannel()
    override suspend fun listFlow(path: String): Flow<VfsFile> = emptyFlow()
}

var resourcesVfsDebug = false
expect val resourcesVfs: VfsFile
expect val rootLocalVfs: VfsFile
expect val applicationVfs: VfsFile
expect val applicationDataVfs: VfsFile
expect val cacheVfs: VfsFile
expect val externalStorageVfs: VfsFile
expect val userHomeVfs: VfsFile
expect val tempVfs: VfsFile
val localCurrentDirVfs: VfsFile get() = applicationVfs

expect fun localVfs(path: String): VfsFile
fun jailedLocalVfs(base: String): VfsFile = localVfs(base).jail()
