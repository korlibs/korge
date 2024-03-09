package korlibs.ffi

open class LDLibraries internal constructor(internal val fs: FFISyncIO) {
    @FFISyncIOAPI
    companion object : LDLibraries(FFIPlatformSyncIO)

    private val libFolders = LinkedHashSet<FFISyncIOFile>()
    private val loadConfFiles = LinkedHashSet<FFISyncIOFile>()
    private val libFoldersInitializeOnce by lazy {
        try {
            // Fixed paths as described https://renenyffenegger.ch/notes/Linux/fhs/etc/ld_so_conf
            addPath("/lib")
            addPath("/usr/lib")
            // Load config file
            loadConfFile(fs.file("/etc/ld.so.conf"))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        libFolders
    }

    internal val ldFoldersFile: List<FFISyncIOFile> get() = libFoldersInitializeOnce.toList()
    val ldFolders: List<String> get() = ldFoldersFile.map { it.fullPath }

    // /etc/ld.so.conf
    // include /etc/ld.so.conf.d/*.conf

    fun addPath(path: String) {
        val file = fs.file(path)
        if (file.isDirectory) {
            //println("included '$file' directory")
            libFolders.add(file)
        } else {
            //println("'$file' is not a directory")
        }
    }

    fun hasLibrary(name: String): Boolean = libFoldersInitializeOnce.any { it[name].exists() }

    private fun loadConfFile(file: FFISyncIOFile) {
        if (file in loadConfFiles) return
        loadConfFiles.add(file)
        for (line in file.readString().lines()) {
            val tline = line.trim().substringBefore('#').takeIf { it.isNotEmpty() } ?: continue

            //println("tline=$tline")

            if (tline.startsWith("include ")) {
                val glob = tline.removePrefix("include ")
                val fullFile = fs.file(glob)
                val globFolder = fullFile.parent
                //val globPattern = Glob(fullFile.name)
                val globPattern = Regex("^" + fullFile.name.replace(".", "\\.").replace("*", ".*").replace("?", ".") + "$")
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
