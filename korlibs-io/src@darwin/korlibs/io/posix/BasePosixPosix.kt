package korlibs.io.posix

import kotlinx.cinterop.*
import platform.posix.*

abstract class BasePosixPosix : BasePosix() {
    override fun posixFopen(filename: String, mode: String): CPointer<FILE>? {
        return fopen(filename, mode)
    }

    override fun posixReadlink(path: String): String? = memScoped {
        val addr = allocArray<ByteVar>(PATH_MAX)
        val finalSize = readlink(path, addr, PATH_MAX.convert()).toInt()
        if (finalSize < 0) null else addr.toKString()
    }

    override fun posixRealpath(path: String): String = memScoped {
        val temp = allocArray<ByteVar>(PATH_MAX)
        realpath(path, temp)
        temp.toKString()
    }

    override fun posixGetcwd(): String = memScoped {
        val temp = allocArray<ByteVar>(PATH_MAX + 1)
        getcwd(temp, PATH_MAX.convert())
        temp.toKString()
    }

    override fun posixMkdir(path: String, attr: Int): Int {
        return platform.posix.mkdir(path, attr.convert())
    }

    override fun posixChmod(rpath: String, value: Int) {
        chmod(rpath, value.convert())
    }
}
