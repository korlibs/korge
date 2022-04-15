package com.soywiz.korgw

import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import platform.AppKit.*

actual fun createDialogInterfaceForComponent(nativeComponent: Any?): DialogInterface {
    return DialogInterfaceMacos { null }
}

class DialogInterfaceMacos(val gwProvider: () -> MyDefaultGameWindow?) : DialogInterface {
    val gw get() = gwProvider?.invoke()
    // @TODO: https://developer.apple.com/documentation/appkit/nsworkspace/1533463-openurl
    override suspend fun browse(url: URL) {
        super.browse(url)
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
        val openDlg: NSOpenPanel = NSOpenPanel().apply {
            setCanChooseFiles(true)
            setAllowsMultipleSelection(false)
            setCanChooseDirectories(false)
        }
        if (openDlg.runModalForDirectory(null, null).toInt() == NSOKButton.toInt()) {
            return openDlg.filenames().filterIsInstance<String>().map { localVfs(it) }
        } else {
            throw CancelException()
        }
    }
}
