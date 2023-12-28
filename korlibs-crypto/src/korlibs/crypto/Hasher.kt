package korlibs.crypto

import korlibs.encoding.Base64
import korlibs.encoding.Hex
import korlibs.internal.arraycopy
import kotlin.math.min

open class HasherFactory(val name: String, val create: () -> Hasher) {
    operator fun invoke(): Hasher = create()
    fun digest(data: ByteArray): Hash = create().also { it.update(data, 0, data.size) }.digest()

    inline fun digest(temp: ByteArray = ByteArray(0x1000), readBytes: (data: ByteArray) -> Int): Hash =
        this.create().also {
            while (true) {
                val count = readBytes(temp)
                if (count <= 0) break
                it.update(temp, 0, count)
            }
        }.digest()

    override fun toString(): String = "HasherFactory($name)"
}

abstract class NonCoreHasher(chunkSize: Int, digestSize: Int, name: String) : Hasher(chunkSize, digestSize, name) {
    abstract override fun reset(): Hasher
    abstract override fun update(data: ByteArray, offset: Int, count: Int): Hasher
    abstract override fun digestOut(out: ByteArray): Unit

    override fun coreReset() = TODO()
    override fun corePadding(totalWritten: Long): ByteArray = TODO()
    override fun coreUpdate(chunk: ByteArray) = TODO()
    override fun coreDigest(out: ByteArray) = TODO()
}

/**
 * [chunkSize] in bytes
 */
abstract class Hasher(val chunkSize: Int, val digestSize: Int, val name: String) {
    /**
     * In bits
     */
    val blockSize: Int get() = chunkSize * 8

    private val chunk = ByteArray(chunkSize)
    private var writtenInChunk = 0
    protected var totalWritten = 0L

    open fun reset(): Hasher {
        coreReset()
        writtenInChunk = 0
        totalWritten = 0L
        return this
    }

    open fun update(data: ByteArray, offset: Int, count: Int): Hasher {
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

    open fun digestOut(out: ByteArray) {
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

    override fun toString(): String = "Hasher($name)"
}

class Hash(val bytes: ByteArray) {
    companion object {
        fun fromHex(hex: String): Hash = Hash(Hex.decode(hex))
        fun fromBase64(base64: String): Hash = Hash(Base64.decodeIgnoringSpaces(base64))
    }
    val base64 get() = Base64.encode(bytes)
    val base64Url get() = Base64.encode(bytes, true)
    val hex get() = Hex.encode(bytes)
    val hexLower get() = Hex.encodeLower(bytes)
    val hexUpper get() = Hex.encodeUpper(bytes)

    override fun equals(other: Any?): Boolean = other is Hash && this.bytes.contentEquals(other.bytes)
    override fun hashCode(): Int = bytes.contentHashCode()
    override fun toString(): String = hexLower
}

fun ByteArray.hash(algo: HasherFactory): Hash = algo.digest(this)
