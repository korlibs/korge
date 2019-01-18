package com.soywiz.korge.build

import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.util.*

object KorgeBuildTools {
	private val binaryRootCache = AsyncOnce<VfsFile>()

	suspend fun BINARY_ROOT() =
		binaryRootCache { localVfs(System.getProperty("user.home"))[".korge"].apply { mkdir() }.jail() }
}
