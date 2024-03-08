package korlibs.io.core

@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class CoreSyncIOAPI

@CoreSyncIOAPI
object CoreSyncIO

expect fun CoreSyncIO.simpleListFolder(path: String): List<String>
expect fun CoreSyncIO.exists(path: String): Boolean
expect fun CoreSyncIO.readBytes(path: String): ByteArray
expect fun CoreSyncIO.writeBytes(path: String, bytes: ByteArray)

/*
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class SyncIOAPI

@SyncIOAPI
object CoreSyncIO

// Basic I/O
@SyncIOAPI expect fun CoreSyncIO.fopen(path: String, mode: String): CoreSyncIOFD
@SyncIOAPI expect fun CoreSyncIO.fseek(fd: CoreSyncIOFD, pos: Long): Unit
@SyncIOAPI expect fun CoreSyncIO.fskip(fd: CoreSyncIOFD, skip: Long): Unit
@SyncIOAPI expect fun CoreSyncIO.ftell(fd: CoreSyncIOFD): Long
@SyncIOAPI expect fun CoreSyncIO.fsize(fd: CoreSyncIOFD): Long
@SyncIOAPI expect fun CoreSyncIO.ftruncate(fd: CoreSyncIOFD, size: Long): Unit
@SyncIOAPI expect fun CoreSyncIO.fwrite(fd: CoreSyncIOFD, bytes: ByteArray, offset: Int = 0, size: Int = bytes.size - offset): Int
@SyncIOAPI expect fun CoreSyncIO.fread(fd: CoreSyncIOFD, bytes: ByteArray, offset: Int = 0, size: Int = bytes.size - offset): Int
@SyncIOAPI expect fun CoreSyncIO.fclose(fd: CoreSyncIOFD)
// Info
@SyncIOAPI expect fun CoreSyncIO.stat(path: String): CoreSyncIOStat
@SyncIOAPI expect fun CoreSyncIO.touch(path: String): CoreSyncIOStat
// Folders
@SyncIOAPI expect fun CoreSyncIO.mkdir(path: String, attr: Int)
@SyncIOAPI expect fun CoreSyncIO.rmdir(path: String)
@SyncIOAPI expect fun CoreSyncIO.delete(path: String)
@SyncIOAPI expect fun CoreSyncIO.list(path: String): List<String>
// Symlinks
@SyncIOAPI expect fun CoreSyncIO.realpath(path: String): String
@SyncIOAPI expect fun CoreSyncIO.readlink(path: String): String
@SyncIOAPI expect fun CoreSyncIO.writelink(path: String, link: String)
// Exec
@SyncIOAPI expect fun CoreSyncIO.exec(vararg command: String, cwd: String? = null, envs: Map<String, String>? = null): CoreSyncIOExecResult
// Environment variables
@SyncIOAPI expect fun CoreSyncIO.envs(): Map<String, String>

@SyncIOAPI
interface CoreSyncIOFD
@SyncIOAPI
data class CoreSyncIOStat(
    val isFile: Boolean,
    val isDirectory: Boolean,
    val size: Long,
    val timeLastModified: Long,
    val timeCreated: Long,
    val timeLastAccessed: Long,
    val linuxPermissions: Int,
    val inode: Long,
    val idev: Long,
)
@SyncIOAPI
class CoreSyncIOExecResult(
    val exitCode: Int,
    val stdoutBytes: ByteArray,
    val stderrBytes: ByteArray,
)
*/
