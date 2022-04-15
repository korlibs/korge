package com.soywiz.korgw.awt

import com.soywiz.korgw.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.URL
import kotlinx.coroutines.*
import java.awt.*
import java.net.*
import javax.swing.*

class DialogInterfaceAwt(val componentProvider: () -> Component?) : DialogInterface {
    val component get() = componentProvider()

    override suspend fun browse(url: URL) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url.toString()));
        }
    }

    override suspend fun alert(message: String) {
        invokeLater { JOptionPane.showMessageDialog(component, message, "Message", JOptionPane.WARNING_MESSAGE) }
    }

    override suspend fun confirm(message: String): Boolean {
        return invokeLater { JOptionPane.showConfirmDialog(component, message, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION }
    }

    override suspend fun prompt(message: String, default: String): String {
        return invokeLater { JOptionPane.showInputDialog(component, message, "Input", JOptionPane.PLAIN_MESSAGE, null, null, default).toString() }
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        //val chooser = JFileChooser()
        return invokeLater {
            val mode = if (write) FileDialog.SAVE else FileDialog.LOAD
            val chooser = FileDialog(this.component?.getContainerFrame(), "Select file", mode)
            if (currentDir != null) {
                chooser.directory = currentDir.absolutePath
            }
            chooser.setFilenameFilter { dir, name -> filter == null || filter.matches(name) }
            chooser.setLocationRelativeTo(null)
            //chooser.fileFilter = filter // @TODO: Filters
            chooser.isMultipleMode = multi
            //chooser.isMultiSelectionEnabled = multi
            chooser.isVisible = true
            chooser.files.map { localVfs(it) }
        }
    }

    suspend inline fun <T> invokeLater(crossinline block: () -> T): T {
        val deferred = CompletableDeferred<T>()
        EventQueue.invokeLater {
            deferred.complete(block())
        }
        return deferred.await()
    }
}
