package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*

val tmpdir: String by lazy {
	when {
		//isNodeJs -> require_node("os").tmpdir().unsafeCast<String>()
		else -> "/tmp"
	}
}

//private val absoluteCwd: String by lazy { if (isNodeJs) require_node("path").resolve(".") else "." }
private val absoluteCwd: String by lazy { "." }

actual val resourcesVfs: VfsFile by lazy { applicationVfs.jail() }
actual val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { jsLocalStorageVfs.root }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy {
	if (isNodeJs) {
		localVfs(tmpdir)
	} else {
		MemoryVfs()
	}
}

actual fun localVfs(path: String): VfsFile {
	return when {
		//isNodeJs -> NodeJsLocalVfs()[path]
		else -> {
			//println("localVfs.url: href=$href, url=$url")
			UrlVfs(jsbaseUrl)[path]
		}
	}
}

actual fun cleanUpResourcesVfs() {
}
