package korlibs.render

import korlibs.io.file.*
import korlibs.io.file.std.*
import korlibs.io.lang.*
import korlibs.io.net.*
import korlibs.io.util.*

/** Represents a class that have a reference to a [dialogInterface] */
interface DialogInterfaceProvider {
    val dialogInterface: DialogInterface
}

/**
 * Provides an interface with typical window dialogs and functionality.
 *
 * Providing:
 * - [browse], [alert], [confirm], [prompt], [openFileDialog]
 */
interface DialogInterface : DialogInterfaceProvider {
    override val dialogInterface: DialogInterface get() = this

    /**
     * Opens a [url] using the default web browser
     **/
    suspend fun browse(url: URL): Unit = unsupported()
    /**
     * Opens an alert dialog showing a [message] and an OK button
     **/
    suspend fun alert(message: String): Unit = unsupported()
    /**
     * Opens a dialog with a [message] requesting the user to accept or cancel.
     * It returns true if the user accepted. */
    suspend fun confirm(message: String): Boolean = unsupported()
    /**
     * Opens a dialog with a [message] and a [default] text asking the user
     * to provide a text message and returns it as a [String]
     **/
    suspend fun prompt(message: String, default: String = ""): String = unsupported()
    /**
     * Opens a file dialog for [write] or not, selecting [multi]ple files
     * and in an optional [currentDir] allowing to select specific [filter] files,
     * and returns a list (that may be empty) of files selected by the user
     **/
    suspend fun openFileDialog(filter: FileFilter? = null, write: Boolean = false, multi: Boolean = false, currentDir: VfsFile? = null): List<VfsFile> =
        unsupported()

    object Unsupported : DialogInterface
}

suspend fun DialogInterfaceProvider.browse(url: URL): Unit = dialogInterface.browse(url)
suspend fun DialogInterfaceProvider.alert(message: String): Unit = dialogInterface.alert(message)
suspend fun DialogInterfaceProvider.confirm(message: String): Boolean = dialogInterface.confirm(message)
suspend fun DialogInterfaceProvider.prompt(message: String, default: String = ""): String = dialogInterface.prompt(message, default)
suspend fun DialogInterfaceProvider.openFileDialog(filter: FileFilter? = null, write: Boolean = false, multi: Boolean = false, currentDir: VfsFile? = null): List<VfsFile> =
    dialogInterface.openFileDialog(filter, write, multi, currentDir)

suspend fun DialogInterfaceProvider.openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false): List<VfsFile> {
    return dialogInterface.openFileDialog(null, write, multi)
}

suspend fun DialogInterfaceProvider.alertError(e: Throwable) {
    dialogInterface.alert(e.stackTraceToString().lines().take(16).joinToString("\n"))
}

data class FileFilter(val entries: List<Pair<String, List<String>>>) {
    private val regexps = entries.flatMap { it.second }.map { Regex.fromGlob(it) }

    constructor(vararg entries: Pair<String, List<String>>) : this(entries.toList())
    fun matches(fileName: String): Boolean = entries.isEmpty() || regexps.any { it.matches(fileName) }
}

open class ZenityDialogs : DialogInterface {
    companion object : ZenityDialogs()

    open suspend fun exec(vararg args: String): String = localCurrentDirVfs.execToString(args.toList())
    override suspend fun browse(url: URL) { exec("xdg-open", url.toString()) }
    override suspend fun alert(message: String) { exec("zenity", "--warning", "--text=$message") }
    override suspend fun confirm(message: String): Boolean =
        try {
            exec("zenity", "--question", "--text=$message")
            true
        } catch (e: Throwable) {
            false
        }

    override suspend fun prompt(message: String, default: String): String = try {
        exec(
            "zenity",
            "--question",
            "--text=$message",
            "--entry-text=$default"
        )
    } catch (e: Throwable) {
        e.printStackTrace()
        ""
    }

    override suspend fun openFileDialog(filter: FileFilter?, write: Boolean, multi: Boolean, currentDir: VfsFile?): List<VfsFile> {
        return exec(*korlibs.io.util.buildList<String> {
            add("zenity")
            add("--file-selection")
            if (multi) add("--multiple")
            if (write) add("--save")
            if (filter != null) {
                //add("--file-filter=$filter")
            }
        }.toTypedArray())
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { localVfs(it.trim()) }
    }
}
