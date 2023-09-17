package korlibs.io.posix

import kotlinx.cinterop.*
import platform.posix.*

actual val POSIX: BasePosix = PosixExtDarwin()

class PosixExtDarwin : BasePosixPosix() {
    override fun ioctlSocketFionRead(sockfd: Int): Int {
        val v = intArrayOf(0)
        ioctl(sockfd.convert(), FIONREAD.convert(), v.refTo(0))
        return v[0]
    }
}
