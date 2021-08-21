package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.lang.tempPath

private val absoluteCwd: String by lazy {
    val path = jsRuntime.currentDir()
    if (jsRuntime.isBrowser) {
        path
    } else {
        when {
            jsRuntime.existsSync("$path/node_modules") && jsRuntime.existsSync("$path/kotlin") && jsRuntime.existsSync("$path/adapter-nodejs.js") ->
                // We are probably on tests `build/js/packages/korlibs-next-korge-test` and resources are in the `kotlin` directory
                "$path/kotlin"
            else -> path
        }
    }
}
//private val absoluteCwd: String by lazy { "." }

actual val resourcesVfs: VfsFile by lazy { applicationVfs.jail() }
actual val rootLocalVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val applicationDataVfs: VfsFile by lazy { jsRuntime.localStorage().root }
actual val cacheVfs: VfsFile by lazy { MemoryVfs() }
actual val externalStorageVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val userHomeVfs: VfsFile by lazy { localVfs(absoluteCwd) }
actual val tempVfs: VfsFile by lazy { jsRuntime.tempVfs() }
actual fun localVfs(path: String, async: Boolean): VfsFile = jsRuntime.openVfs(path)

actual fun cleanUpResourcesVfs() = Unit
