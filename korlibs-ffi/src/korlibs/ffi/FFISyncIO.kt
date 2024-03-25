package korlibs.ffi

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
internal annotation class FFISyncIOAPI

@FFISyncIOAPI
internal expect val FFIPlatformSyncIO: FFISyncIO

internal interface FFISyncIO {
    fun exists(path: String): Boolean
    fun isDirectory(path: String): Boolean
    fun readBytes(path: String): ByteArray
    fun listFiles(path: String): List<String>
}

internal class FFISyncIOFile(val impl: FFISyncIO, val fullPath: String) {
    val path: String get() = fullPath.substringBeforeLast('/', "")
    val name: String get() = fullPath.substringAfterLast('/')
    val parent: FFISyncIOFile get() = FFISyncIOFile(impl, path)
    fun exists(): Boolean = impl.exists(fullPath)
    val isDirectory: Boolean get() = impl.isDirectory(fullPath)
    fun readString(): String = impl.readBytes(fullPath).decodeToString()
    fun list(): List<FFISyncIOFile> = impl.listFiles(fullPath).map { this[it] }
    operator fun get(child: String): FFISyncIOFile = FFISyncIOFile(impl, "$fullPath/$child")
}

internal fun FFISyncIO.file(fullPath: String): FFISyncIOFile = FFISyncIOFile(this, fullPath)
