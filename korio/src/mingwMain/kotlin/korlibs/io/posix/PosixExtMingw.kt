package korlibs.io.posix

import kotlinx.cinterop.*
import platform.posix.*

actual val POSIX: BasePosix = PosixMingw

object PosixMingw : BasePosix() {
    override fun posixFopen(filename: String, mode: String): CPointer<FILE>? {
        return memScoped {
            //setlocale(LC_ALL, ".UTF-8") // On Windows 10 : https://docs.microsoft.com/en-us/cpp/c-runtime-library/reference/setlocale-wsetlocale?redirectedfrom=MSDN&view=msvc-160#utf-8-support
            platform.posix._wfopen(filename.wcstr, mode.wcstr)
        }
    }

    override fun posixReadlink(path: String): String? = null

    override fun posixRealpath(path: String): String = path

    override fun posixGetcwd(): String = memScoped {
        val temp = allocArray<ByteVar>(PATH_MAX + 1)
        getcwd(temp, PATH_MAX)
        temp.toKString()
    }

    override fun posixMkdir(path: String, attr: Int): Int {
        return platform.posix.mkdir(path)
    }

    override fun ioctlSocketFionRead(sockfd: Int): Int {
        val v = uintArrayOf(0u)
        ioctlsocket(sockfd.convert(), FIONREAD, v.refTo(0))
        return v[0].toInt()
    }
}