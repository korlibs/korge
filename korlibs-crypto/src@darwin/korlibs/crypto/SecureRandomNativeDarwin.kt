package korlibs.crypto

import kotlinx.cinterop.*
import platform.Security.*
import platform.posix.*

// https://developer.apple.com/documentation/security/randomization_services
actual fun fillRandomBytes(array: ByteArray) {
    if (array.isEmpty()) return

    array.usePinned { pin ->
        val ptr = pin.addressOf(0)
        val status = SecRandomCopyBytes(kSecRandomDefault, array.size.convert(), ptr)
        if (status != errSecSuccess) {
            error("Error filling random bytes. errorCode=$status")
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
