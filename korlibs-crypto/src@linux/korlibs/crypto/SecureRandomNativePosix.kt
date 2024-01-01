package korlibs.crypto

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite

actual fun fillRandomBytes(array: ByteArray) {
    if (array.isEmpty()) return

    array.usePinned { pin ->
        val ptr = pin.addressOf(0)
        val file = fopen("/dev/urandom", "rb")
        if (file != null) {
            fread(ptr, 1.convert(), array.size.convert(), file)
            for (n in 0 until array.size) array[n] = ptr[n]
            fclose(file)
        }
    }
}

actual fun seedExtraRandomBytes(array: ByteArray) {
    if (array.isEmpty()) return

    try {
        array.usePinned { pin ->
            val ptr = pin.addressOf(0)
            val file = fopen("/dev/urandom", "wb")
            if (file != null) {
                fwrite(ptr, 1.convert(), array.size.convert(), file)
                for (n in 0 until array.size) array[n] = ptr[n]
                fclose(file)
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}
