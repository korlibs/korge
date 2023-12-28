package korlibs.crypto

import korlibs.internal.arraycopy
import kotlin.random.Random

typealias Padding = CipherPadding

/**
 * Symmetric Cipher Padding
 */
abstract class CipherPadding {
    companion object {
        val NoPadding: CipherPadding get() = CipherPaddingNo
        val PKCS7Padding: CipherPadding get() = CipherPaddingPKCS7
        val ANSIX923Padding: CipherPadding get() = CipherPaddingANSIX923
        val ISO10126Padding: CipherPadding get() = CipherPaddingISO10126
        val ZeroPadding: CipherPadding get() = CipherPaddingZero

        fun padding(data: ByteArray, blockSize: Int, padding: Padding): ByteArray = padding.add(data, blockSize)
        fun removePadding(data: ByteArray, padding: Padding): ByteArray = padding.remove(data)
    }

    fun add(data: ByteArray, blockSize: Int): ByteArray {
        //padding(data, blockSize, this)
        val paddingSize = paddingSize(data.size, blockSize)
        val result = ByteArray(data.size + paddingSize)
        arraycopy(data, 0, result, 0, data.size)
        addInternal(result, data.size, paddingSize)
        return result
    }
    fun remove(data: ByteArray): ByteArray {
        val result = data.copyOf()
        val size = removeInternal(data)
        return result.copyOf(size)
    }

    protected open fun paddingSize(dataSize: Int, blockSize: Int): Int = blockSize - dataSize % blockSize
    protected open fun addInternal(result: ByteArray, dataSize: Int, paddingSize: Int) : Unit = Unit
    protected open fun removeInternal(data: ByteArray) : Int = data.size - (data[data.size - 1].toInt() and 0xFF)
}

private object CipherPaddingNo : CipherPadding() {
    override fun paddingSize(dataSize: Int, blockSize: Int): Int {
        if (dataSize % blockSize != 0) {
            throw IllegalArgumentException("Data ($dataSize) is not multiple of ${blockSize}, and padding was set to $NoPadding")
        }
        return 0
    }
    override fun addInternal(result: ByteArray, dataSize: Int, paddingSize: Int) = Unit
    override fun removeInternal(data: ByteArray): Int = data.size
}
private object CipherPaddingPKCS7 : CipherPadding() {
    override fun addInternal(result: ByteArray, dataSize: Int, paddingSize: Int) {
        for (i in dataSize until result.size) result[i] = paddingSize.toByte()
    }
}
private object CipherPaddingANSIX923 : CipherPadding() {
    override fun addInternal(result: ByteArray, dataSize: Int, paddingSize: Int) {
        result[result.size - 1] = paddingSize.toByte()
    }
}
private object CipherPaddingISO10126 : CipherPadding() {
    override fun addInternal(result: ByteArray, dataSize: Int, paddingSize: Int) {
        val randomBytes = Random.nextBytes(paddingSize)
        randomBytes[paddingSize - 1] = paddingSize.toByte()
        arraycopy(randomBytes, 0, result, dataSize, randomBytes.size)
    }
}
private object CipherPaddingZero : CipherPadding() {
    override fun removeInternal(data: ByteArray): Int {
        var paddingSize = 0
        for (i in data.size - 1 downTo 0) {
            if (data[i].toInt() != 0) {
                break
            }
            ++paddingSize
        }
        return data.size - paddingSize
    }
}
