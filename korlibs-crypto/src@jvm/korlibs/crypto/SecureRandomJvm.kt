package korlibs.crypto

import java.security.SecureRandom

private val jrandom = SecureRandom()

actual fun fillRandomBytes(array: ByteArray) {
    jrandom.nextBytes(array)
}

actual fun seedExtraRandomBytes(array: ByteArray) {
    jrandom.setSeed(array)
}
