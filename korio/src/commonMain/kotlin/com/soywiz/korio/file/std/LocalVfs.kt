package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
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

/** Contains files from `src/...Main/resources` and generated files by the build system */
expect val resourcesVfs: VfsFile
expect fun cleanUpResourcesVfs()

/** @TODO */
expect val rootLocalVfs: VfsFile
/** @TODO */
expect val applicationVfs: VfsFile
/** @TODO */
expect val applicationDataVfs: VfsFile

/** A Memory Virtual File System for cache */
expect val cacheVfs: VfsFile
/** @TODO */
expect val externalStorageVfs: VfsFile
/** User home folder, usually `~`, `/Users/something`, `/home/something` or equivalent */
expect val userHomeVfs: VfsFile
/** Temp folder, usually `/tmp` or equivalent */
expect val tempVfs: VfsFile
/** Alias for [applicationVfs] */
val localCurrentDirVfs: VfsFile get() = applicationVfs

/** Gets a [VfsFile] in the Operating System filesystem in [path]. It supports accessing parent folders. */
expect fun localVfs(path: String, async: Boolean = true): VfsFile
/** Gets a [VfsFile] in the Operating System filesystem in [base]. Jailed. Doesn't support accessing parent folders. */
fun jailedLocalVfs(base: String): VfsFile = localVfs(base).jail()
