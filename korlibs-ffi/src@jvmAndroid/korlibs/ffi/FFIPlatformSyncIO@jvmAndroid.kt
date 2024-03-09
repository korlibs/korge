package korlibs.ffi

import java.io.*

internal actual val FFIPlatformSyncIO: FFISyncIO = object : FFISyncIO {
    override fun exists(path: String): Boolean = File(path).exists()
    override fun isDirectory(path: String): Boolean = File(path).isDirectory
    override fun readBytes(path: String): ByteArray  = File(path).readBytes()
    override fun listFiles(path: String): List<String> = File(path).list()?.toList() ?: emptyList()
}
