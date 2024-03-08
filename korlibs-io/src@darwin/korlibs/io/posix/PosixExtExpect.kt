package korlibs.io.posix

import korlibs.time.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

data class PosixStatInfo(
    val size: Long,
    val isDirectory: Boolean,
    val mode: Int = 0,
    val timeCreated: DateTime,
    val timeModified: DateTime,
    val timeLastAccess: DateTime,
)

fun posixFread(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong = POSIX.posixFread(__ptr, __size, __nitems, __stream)
fun posixFwrite(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong = POSIX.posixFwrite(__ptr, __size, __nitems, __stream)
fun posixFseek(file: CValuesRef<FILE>?, offset: Long, whence: Int): Int = POSIX.posixFseek(file, offset, whence)
fun posixFtell(file: CValuesRef<FILE>?): ULong = POSIX.posixFtell(file)
fun posixFopen(filename: String, mode: String): CPointer<FILE>? = POSIX.posixFopen(filename, mode)
fun posixFclose(file: CPointer<FILE>?): Int = POSIX.posixFclose(file)
fun posixTruncate(file: String, size: Long): Int = POSIX.posixTruncate(file, size)
fun posixStat(rpath: String): PosixStatInfo? = POSIX.posixStat(rpath)
fun posixChmod(rpath: String, value: Int): Unit = POSIX.posixChmod(rpath, value)
fun posixReadlink(path: String): String? = POSIX.posixReadlink(path)
fun posixRealpath(path: String): String = POSIX.posixRealpath(path)
fun posixGetcwd(): String = POSIX.posixGetcwd()
fun posixMkdir(path: String, attr: Int): Int = POSIX.posixMkdir(path, attr)
fun ioctlSocketFionRead(sockfd: Int): Int = POSIX.ioctlSocketFionRead(sockfd)

expect val POSIX: BasePosix

abstract class BasePosix {
    abstract fun posixFopen(filename: String, mode: String): CPointer<FILE>?
    abstract fun posixReadlink(path: String): String?
    abstract fun posixRealpath(path: String): String
    abstract fun posixGetcwd(): String
    abstract fun posixMkdir(path: String, attr: Int): Int
    abstract fun ioctlSocketFionRead(sockfd: Int): Int

    open fun posixChmod(rpath: String, value: Int) {
    }

    open fun posixFread(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong {
        return fread(__ptr, __size.convert(), __nitems.convert(), __stream).convert()
    }

    open fun posixFwrite(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong {
        return fwrite(__ptr, __size.convert(), __nitems.convert(), __stream).convert()
    }

    open fun posixFseek(file: CValuesRef<FILE>?, offset: Long, whence: Int): Int {
        return fseek(file, offset.convert(), whence.convert())
    }

    open fun posixFtell(file: CValuesRef<FILE>?): ULong {
        return ftell(file).convert()
    }

    open fun posixFclose(file: CPointer<FILE>?): Int {
        return fclose(file)
    }

    open fun posixTruncate(file: String, size: Long): Int {
        return truncate(file, size.convert())
    }

    open fun posixStat(rpath: String): PosixStatInfo? {
        memScoped {
            val s = alloc<stat>()
            if (platform.posix.stat(rpath, s.ptr) == 0) {
                val size: Long = s.st_size.toLong()
                val isDirectory = (s.st_mode.toInt() and S_IFDIR) != 0
                return PosixStatInfo(
                    size = size,
                    isDirectory = isDirectory,
                    mode = s.st_mode.convert(),
                    timeCreated = s.st_ctimespec.toDateTime(),
                    timeModified = s.st_mtimespec.toDateTime(),
                    timeLastAccess = s.st_atimespec.toDateTime(),
                )
            }
        }
        return null
    }

    fun platform.posix.timespec.toDateTime(): DateTime =
        DateTime.EPOCH +
            (this.tv_sec.seconds + this.tv_nsec.nanoseconds)

}
