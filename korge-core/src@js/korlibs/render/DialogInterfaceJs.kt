package korlibs.render

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.net.*
import kotlinx.browser.*
import kotlinx.coroutines.*
import org.w3c.dom.*
import org.w3c.files.*

class DialogInterfaceJs : DialogInterface {

    override suspend fun browse(url: URL) {
        document.open(url.fullUrl, "", "noopener=true")
    }

    override suspend fun alert(message: String) {
        window.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return window.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return window.prompt(message, default) ?: throw CancellationException("cancelled")
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        val deferred = CompletableDeferred<List<VfsFile>>()
        val input = document.createElement("input").unsafeCast<HTMLInputElement>()
        input.style.position = "absolute"
        input.style.top = "0px"
        input.style.left = "0px"
        input.style.visibility = "hidden"
        input.type = "file"
        input.multiple = multi
        input.onchange = {
            val files = input.files
            //document.body?.removeChild(input)
            if (files != null) {
                deferred.complete((0 until files.length).map { files[it]?.toVfs() }.filterNotNull())
            } else {
                deferred.complete(listOf())
            }
        }
        input.oncancel = {
            //document.body?.removeChild(input)
        }
        document.body?.appendChild(input)
        input.click()
        window.setTimeout({
            document.body?.removeChild(input)
        }, 100)
        return deferred.await()
    }
}
