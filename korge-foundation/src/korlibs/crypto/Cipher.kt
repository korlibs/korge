package korlibs.crypto

interface Cipher {
    val blockSize: Int
    fun encrypt(data: ByteArray, offset: Int = 0, len: Int = data.size - offset)
    fun decrypt(data: ByteArray, offset: Int = 0, len: Int = data.size - offset)
}

class CipherWithModeAndPadding(val cipher: Cipher, val mode: CipherMode, val padding: CipherPadding, val iv: ByteArray? = null) {
    fun encrypt(data: ByteArray, offset: Int = 0, len: Int = data.size - offset): ByteArray {
        return mode.encryptSafe(data.copyOfRange(offset, offset + len), cipher, padding, iv)
    }

    fun decrypt(data: ByteArray, offset: Int = 0, len: Int = data.size - offset): ByteArray =
        mode.decryptSafe(data.copyOfRange(offset, offset + len), cipher, padding, iv)
}

fun Cipher.with(mode: CipherMode, padding: CipherPadding, iv: ByteArray? = null): CipherWithModeAndPadding = CipherWithModeAndPadding(this, mode, padding, iv)
operator fun Cipher.get(mode: CipherMode, padding: CipherPadding, iv: ByteArray? = null): CipherWithModeAndPadding = with(mode, padding, iv)
