package korlibs.ffi

import korlibs.platform.*

open class ExecutableResolver internal constructor(internal val fs: FFISyncIO) {
    @OptIn(FFISyncIOAPI::class)
    companion object : ExecutableResolver(FFIPlatformSyncIO)

    fun findInPaths(exec: String, paths: List<String>): String? {
        val fileSeparator = if (Platform.isWindows) "\\" else "/"
        for (path in paths) {
            val rpath = path.trimEnd('/', '\\') + fileSeparator
            val files = listOf(fs.file("$rpath$exec"), fs.file("$rpath$exec.exe"), fs.file("$rpath$exec.cmd"), fs.file("$rpath$exec.bat"))
            for (file in files) {
                if (file.exists()) return file.fullPath
            }
        }
        return null
    }
}
