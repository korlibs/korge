package korlibs.ffi

import korlibs.io.file.sync.*

open class LDLibraries(val fs: LDFileAPI) {
    @SyncIOAPI
    companion object : LDLibraries(platformSyncIOCaseInsensitive)

    private val libFolders = LinkedHashSet<LDFileRef>()
    private val loadConfFiles = LinkedHashSet<LDFileRef>()
    private val libFoldersInitializeOnce by lazy {
        try {
            // Fixed paths as described https://renenyffenegger.ch/notes/Linux/fhs/etc/ld_so_conf
            addPath("/lib")
            addPath("/usr/lib")
            // Load config file
            loadConfFile("/etc/ld.so.conf")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        libFolders
    }

    val ldFolders: List<LDFileRef> get() = libFoldersInitializeOnce.toList()

    // /etc/ld.so.conf
    // include /etc/ld.so.conf.d/*.conf

    fun addPath(path: String) {
        val file = LDFileRef(path)
        if (file.isDirectory) {
            //println("included '$file' directory")
            libFolders.add(file)
        } else {
            //println("'$file' is not a directory")
        }
    }

    fun hasLibrary(name: String) = libFoldersInitializeOnce.any { it[name].exists() }

    private fun loadConfFile(file: LDFileRef) {
        if (file in loadConfFiles) return
        loadConfFiles.add(file)
        for (line in file.readString().lines()) {
            val tline = line.trim().substringBefore('#').takeIf { it.isNotEmpty() } ?: continue

            //println("tline=$tline")

            if (tline.startsWith("include ")) {
                val glob = tline.removePrefix("include ")
                val fullFile = LDFileRef(glob)
                val globFolder = fullFile.parent
                val globPattern = Regex("^" + fullFile.name.replace(".", "\\.").replace("*", ".*") + "$")
                if (globFolder.isDirectory) {
                    for (item in globFolder.list()) {
                        if (globPattern matches item.name) {
                            loadConfFile(item)
                        }
                    }
                }
            } else {
                addPath(tline)
            }
        }
    }
}

data class LDFileRef(val api: LDFileAPI, val path: String) {
    val parent: LDFileRef by lazy { LDFileRef(path.substringBeforeLast('/')) }
}

interface LDFileAPI {
    fun readString(path: String): String
    fun listDir(path: String): List<String>
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
}
