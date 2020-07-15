package com.soywiz.krypto

import com.soywiz.krypto.encoding.Base64
import com.soywiz.krypto.encoding.Hex
import com.soywiz.krypto.internal.arraycopy
import kotlin.math.min

open class HasherFactory(val create: () -> Hasher) {
    fun digest(data: ByteArray) = create().also { it.update(data, 0, data.size) }.digest()
}

abstract class Hasher(val chunkSize: Int, val digestSize: Int) {
    private val chunk = ByteArray(chunkSize)
    private var writtenInChunk = 0
    private var totalWritten = 0L

    fun reset(): Hasher {
        coreReset()
        writtenInChunk = 0
        totalWritten = 0L
        return this
    }

    fun update(data: ByteArray, offset: Int, count: Int): Hasher {
        var curr = offset
        var left = count
        while (left > 0) {
            val remainingInChunk = chunkSize - writtenInChunk
            val toRead = min(remainingInChunk, left)
            arraycopy(data, curr, chunk, writtenInChunk, toRead)
            left -= toRead
            curr += toRead
            writtenInChunk += toRead
            if (writtenInChunk >= chunkSize) {
                writtenInChunk -= chunkSize
                coreUpdate(chunk)
            }
        }
        totalWritten += count
        return this
    }

    fun digestOut(out: ByteArray) {
        val pad = corePadding(totalWritten)
        var padPos = 0
        while (padPos < pad.size) {
            val padSize = chunkSize - writtenInChunk
            arraycopy(pad, padPos, chunk, writtenInChunk, padSize)
            coreUpdate(chunk)
            writtenInChunk = 0
            padPos += padSize
        }

        coreDigest(out)
        coreReset()
    }

    protected abstract fun coreReset()
    protected abstract fun corePadding(totalWritten: Long): ByteArray
    protected abstract fun coreUpdate(chunk: ByteArray)
    protected abstract fun coreDigest(out: ByteArray)

    fun update(data: ByteArray) = update(data, 0, data.size)
    fun digest(): Hash = Hash(ByteArray(digestSize).also { digestOut(it) })
}

inline class Hash(val bytes: ByteArray) {
    companion object {
        fun fromHex(hex: String): Hash = Hash(Hex.decode(hex))
        fun fromBase64(base64: String): Hash = Hash(Base64.decodeIgnoringSpaces(base64))
    }
    val base64 get() = Base64.encode(bytes)
    val hex get() = Hex.encode(bytes)
    val hexLower get() = Hex.encodeLower(bytes)
    val hexUpper get() = Hex.encodeUpper(bytes)
}

fun ByteArray.hash(algo: HasherFactory): Hash = algo.digest(this)
