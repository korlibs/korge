package com.soywiz.korgw

import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import platform.windows.*

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface {
    return DialogInterfaceWin { nativeComponent }
}

class DialogInterfaceWin(val nativeComponentProvider: () -> Any?) : DialogInterface {
    val hwnd: HWND? get() = nativeComponentProvider() as? HWND?

    // https://stackoverflow.com/questions/3037088/how-to-open-the-default-web-browser-in-windows-in-c
    override suspend fun browse(url: URL) {
        ShellExecuteW(null, "open", url.fullUrl, null, null, SW_SHOWNORMAL)
    }

    override suspend fun alert(message: String) {
        super.alert(message)
    }

    override suspend fun confirm(message: String): Boolean {
        return super.confirm(message)
    }

    override suspend fun prompt(message: String, default: String): String {
        return super.prompt(message, default)
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        val selectedFile = openSelectFile(hwnd = hwnd)
        if (selectedFile != null) {
            return listOf(com.soywiz.korio.file.std.localVfs(selectedFile))
        } else {
            throw com.soywiz.korio.lang.CancelException()
        }
    }
}
