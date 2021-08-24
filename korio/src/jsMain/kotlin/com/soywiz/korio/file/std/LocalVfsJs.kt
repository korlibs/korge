package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.lang.tempPath

actual val resourcesVfs: VfsFile by lazy { applicationVfs.jail() }
actual val rootLocalVfs: VfsFile by lazy { localVfs(".") }
actual val applicationVfs: VfsFile by lazy { localVfs(".") }
actual val applicationDataVfs: VfsFile by lazy { jsRuntime.localStorage().root }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(".") }
actual val userHomeVfs: VfsFile by lazy { localVfs(".") }
actual val tempVfs: VfsFile by lazy { jsRuntime.tempVfs() }
actual fun localVfs(path: String, async: Boolean): VfsFile = jsRuntime.openVfs(path)

actual fun cleanUpResourcesVfs() = Unit
