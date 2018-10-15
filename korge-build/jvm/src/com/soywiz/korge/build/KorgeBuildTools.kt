package com.soywiz.korge.build

import com.soywiz.korio.util.AsyncCacheItem
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsFile

object KorgeBuildTools {
	private val binaryRootCache = AsyncCacheItem<VfsFile>()

	suspend fun BINARY_ROOT() =
		binaryRootCache { LocalVfs(System.getProperty("user.home"))[".korge"].apply { mkdirs() }.jail() }
}
