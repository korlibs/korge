package com.soywiz.krypto

import com.soywiz.krypto.internal.arraycopy
import com.soywiz.krypto.internal.arrayxor
import com.soywiz.krypto.internal.getIV

/**
 * Symmetric Cipher Mode
 */
interface CipherMode {
    companion object {
        val ECB: CipherMode get() = CipherModeECB
        val CBC: CipherMode get() = CipherModeCBC
        val PCBC: CipherMode get() = CipherModePCBC
        val CFB: CipherMode get() = CipherModeCFB
        val OFB: CipherMode get() = CipherModeOFB
        val CTR: CipherMode get() = CipherModeCTR
    }

    val name: String
    fun encrypt(data: ByteArray, cipher: Cipher, padding: Padding, iv: ByteArray?): ByteArray
    fun decrypt(data: ByteArray, cipher: Cipher, padding: Padding, iv: ByteArray?): ByteArray
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private object CipherModeECB : CipherModeBase("ECB") {
    override fun encrypt(data: ByteArray, cipher: Cipher, padding: Padding, iv: ByteArray?): ByteArray {
        val pData = padding.add(data, cipher.blockSize)
        cipher.encrypt(pData, 0, pData.size)
        return pData
    }

    override fun decrypt(data: ByteArray, cipher: Cipher, padding: Padding, iv: ByteArray?): ByteArray {
        cipher.decrypt(data, 0, data.size)
        return padding.remove(data)
    }
}

private object CipherModeCBC : CipherModeIV("CBC") {
    override fun coreEncrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        for (n in pData.indices step cipher.blockSize) {
            arrayxor(pData, n, ivb)
            cipher.encrypt(pData, n, cipher.blockSize)
            arraycopy(pData, n, ivb, 0, cipher.blockSize)
        }
    }

    override fun coreDecrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val tempBytes = ByteArray(blockSize)

        for (n in pData.indices step blockSize) {
            arraycopy(pData, n, tempBytes, 0, blockSize)
            cipher.decrypt(pData, n, blockSize)
            arrayxor(pData, n, ivb)
            arraycopy(tempBytes, 0, ivb, 0, blockSize)
        }
    }
}

private object CipherModePCBC : CipherModeIV("PCBC") {
    override fun coreEncrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val plaintext = ByteArray(blockSize)

        for (n in pData.indices step blockSize) {
            arraycopy(pData, n, plaintext, 0, blockSize)
            arrayxor(pData, n, ivb)
            cipher.encrypt(pData, n, cipher.blockSize)
            arraycopy(pData, n, ivb, 0, blockSize)
            arrayxor(ivb, 0, plaintext)
        }
    }

    override fun coreDecrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val cipherText = ByteArray(cipher.blockSize)

        for (n in pData.indices step cipher.blockSize) {
            arraycopy(pData, n, cipherText, 0, blockSize)
            cipher.decrypt(pData, n, cipher.blockSize)
            arrayxor(pData, n, ivb)
            arraycopy(pData, n, ivb, 0, blockSize)
            arrayxor(ivb, 0, cipherText)
        }
    }
}

private object CipherModeCFB : CipherModeIV("CFB") {
    override fun coreEncrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val cipherText = ByteArray(blockSize)

        cipher.encrypt(ivb)
        arraycopy(ivb, 0, cipherText, 0, blockSize)
        for (n in pData.indices step blockSize) {
            arrayxor(cipherText, 0, blockSize, pData, n)
            arraycopy(cipherText, 0, pData, n, blockSize)

            if (n + blockSize < pData.size) {
                cipher.encrypt(cipherText)
            }
        }
    }

    override fun coreDecrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val plainText = ByteArray(blockSize)
        val cipherText = ByteArray(blockSize)

        cipher.encrypt(ivb)
        arraycopy(ivb, 0, cipherText, 0, blockSize)
        for (n in pData.indices step blockSize) {
            arraycopy(cipherText, 0, plainText, 0, blockSize)
            arrayxor(plainText, 0, blockSize, pData, n)

            arraycopy(pData, n, cipherText, 0, blockSize)
            arraycopy(plainText, 0, pData, n, blockSize)
            if (n + blockSize < pData.size) {
                cipher.encrypt(cipherText)
            }
        }
    }
}

private object CipherModeOFB : CipherModeIVDE("OFB") {
    override fun coreCrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val cipherText = ByteArray(blockSize)
        cipher.encrypt(ivb)
        for (n in pData.indices step blockSize) {
            arraycopy(pData, n, cipherText, 0, blockSize)
            arrayxor(cipherText, 0, ivb)
            arraycopy(cipherText, 0, pData, n, blockSize)
            if (n + blockSize < pData.size) {
                cipher.encrypt(ivb)
            }
        }
    }
}

// https://github.com/Jens-G/haxe-crypto/blob/dcf6d994773abba80b0720b2f5e9d5b26de0dbe3/src/com/hurlant/crypto/symmetric/mode/CTRMode.hx
private object CipherModeCTR : CipherModeIVDE("CTR") {
    override fun coreCrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) {
        val blockSize = cipher.blockSize
        val temp = ByteArray(ivb.size)
        for (n in pData.indices step blockSize) {
            arraycopy(ivb, 0, temp, 0, temp.size)
            cipher.encrypt(temp, 0, blockSize)
            arrayxor(pData, n, temp)
            for (j in blockSize - 1 downTo 0) {
                ivb[j]++
                if (ivb[j].toInt() != 0) break
            }
        }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private abstract class CipherModeBase(override val name: String) : CipherMode {
    override fun toString(): String = name
}

private abstract class CipherModeIV(name: String) : CipherModeBase(name) {
    final override fun encrypt(data: ByteArray, cipher: Cipher, padding: Padding, iv: ByteArray?): ByteArray {
        val ivb = getIV(iv, cipher.blockSize)
        val pData = padding.add(data, cipher.blockSize)
        coreEncrypt(pData, cipher, ivb)
        return pData
    }

    final override fun decrypt(data: ByteArray, cipher: Cipher, padding: Padding, iv: ByteArray?): ByteArray {
        val ivb = getIV(iv, cipher.blockSize)
        val pData = data.copyOf()
        coreDecrypt(pData, cipher, ivb)
        return padding.remove(pData)
    }

    protected abstract fun coreEncrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray)
    protected abstract fun coreDecrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray)
}

private abstract class CipherModeIVDE(name: String) : CipherModeIV(name) {
    final override fun coreEncrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) = coreCrypt(pData, cipher, ivb)
    final override fun coreDecrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray) = coreCrypt(pData, cipher, ivb)

    protected abstract fun coreCrypt(pData: ByteArray, cipher: Cipher, ivb: ByteArray)
}
