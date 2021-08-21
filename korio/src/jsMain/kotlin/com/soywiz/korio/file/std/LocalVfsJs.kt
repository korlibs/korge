package com.soywiz.korio.file.std

import com.soywiz.korio.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.lang.tempPath

val tmpdir: String by lazy { Environment.tempPath }

private val absoluteCwd: String by lazy {
    when {
        NodeDeno.available -> {
            val path = NodeDeno.currentDir()
            when {
                NodeDeno.existsSync("$path/node_modules") && NodeDeno.existsSync("$path/kotlin") && NodeDeno.existsSync("$path/adapter-nodejs.js") ->
                    // We are probably on tests `build/js/packages/korlibs-next-korge-test` and resources are in the `kotlin` directory
                    "$path/kotlin"
                else -> path
            }.also {
                println("absoluteCwd=$it")
            }
        }
        else -> "."
    }
}
//private val absoluteCwd: String by lazy { "." }

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

actual fun localVfs(path: String, async: Boolean): VfsFile {
	return when {
		isNodeJs -> NodeJsLocalVfs()[path]
		else -> {
			//println("localVfs.url: href=$href, url=$url")
			UrlVfs(jsbaseUrl)[path]
		}
	}
}

actual fun cleanUpResourcesVfs() {
}
