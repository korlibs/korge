package korlibs.io.compression.util

import korlibs.datastructure.RingBuffer
import korlibs.datastructure.internal.KdsInternalApi
import korlibs.math.ilog2
import korlibs.math.nextPowerOfTwo
import korlibs.io.experimental.KorioExperimentalApi
import korlibs.io.lang.ASCII
import korlibs.io.lang.toString
import korlibs.io.stream.AsyncGetLengthStream
import korlibs.io.stream.AsyncInputStream
import korlibs.io.stream.AsyncInputStreamWithLength
import korlibs.io.stream.AsyncOutputStream
import korlibs.io.stream.MemorySyncStreamToByteArray
import korlibs.io.stream.write8
import korlibs.io.stream.writeBytes
import korlibs.memory.*
import kotlin.math.max
import kotlin.math.min

@KorioExperimentalApi
open class BitReader constructor(
    val s: AsyncInputStream,
    val bigChunkSize: Int = BIG_CHUNK_SIZE,
    val readWithSize: Int = READ_WHEN_LESS_THAN
) : AsyncInputStreamWithLength {
    override fun toString(): String = "BitReader($s, bigChunkSize=$bigChunkSize, readWithSize=$readWithSize)"

    companion object {
        const val BIG_CHUNK_SIZE = 8 * 1024 * 1024 // 8 MB
        //const val BIG_CHUNK_SIZE = 128 * 1024 // 128 KB
        //const val BIG_CHUNK_SIZE = 8 * 1024
        const val READ_WHEN_LESS_THAN = 32 * 1024

        suspend fun forInput(s: AsyncInputStream): BitReader {
            if (s is AsyncGetLengthStream && s.hasLength()) {
                val bigChunkSize = max(READ_WHEN_LESS_THAN, min(s.getLength(), BIG_CHUNK_SIZE.toLong()).toInt())
                val readWithSize = max(bigChunkSize / 2, READ_WHEN_LESS_THAN)
                //println("BitReader: bigChunkSize=$bigChunkSize, readWithSize=$readWithSize")
                return BitReader(s, bigChunkSize, readWithSize)
            }
            return BitReader(s)
        }
    }

	@PublishedApi
	internal var bitdata = 0
	@PublishedApi
	internal var bitsavailable = 0

	inline fun discardBits(): BitReader {
        //if (bitsavailable > 0) println("discardBits: $bitsavailable")
		this.bitdata = 0
		this.bitsavailable = 0
		return this
	}

    private val sbuffers = RingBuffer(ilog2(bigChunkSize.nextPowerOfTwo))
    private var sbuffersReadPos = 0.0
    private var sbuffersPos = 0.0
	val requirePrepare get() = sbuffers.availableRead < readWithSize

	suspend inline fun prepareBigChunkIfRequired() {
		if (requirePrepare) prepareBytesUpTo(bigChunkSize)
	}

    @KorioExperimentalApi
    fun internalPeekBytes(out: ByteArray, offset: Int = 0, size: Int = out.size - offset): ByteArray {
        sbuffers.peek(out, offset, size)
        return out
    }

	fun returnToBuffer(data: ByteArray, offset: Int, size: Int) {
		sbuffers.write(data, offset, size)
        sbuffersPos += size
	}

	@OptIn(KdsInternalApi::class)
    suspend fun prepareBytesUpTo(expectedBytes: Int) {
		while (sbuffers.availableRead < expectedBytes) {
            val readCount = min(expectedBytes, sbuffers.availableWriteBeforeWrap)
            if (readCount <= 0) break

            val transferred = s.read(sbuffers.internalBuffer, sbuffers.internalWritePos, readCount)
            if (transferred <= 0) break
            sbuffers.internalWriteSkip(transferred)

            sbuffersPos += transferred
		}
	}

    fun ensureBits(bitcount: Int) {
        while (this.bitsavailable < bitcount) {
            this.bitdata = this.bitdata or (_su8() shl this.bitsavailable)
            this.bitsavailable += 8
        }
    }

    fun peekBits(bitcount: Int): Int {
        return this.bitdata and ((1 shl bitcount) - 1)
    }


    fun skipBits(bitcount: Int) {
        this.bitdata = this.bitdata ushr bitcount
        this.bitsavailable -= bitcount
    }

	fun readBits(bitcount: Int): Int {
        ensureBits(bitcount)
		val readed = peekBits(bitcount)
        skipBits(bitcount)
		return readed
	}

	fun sreadBit(): Boolean = readBits(1) != 0

    //var lastReadByte = 0

	private inline fun _su8(): Int {
        sbuffersReadPos++
	    return sbuffers.readByte()
        //val byte = sbuffers.readByte()
        //lastReadByte = byte // @TODO: Check performance of this
        //return byte
    }

	fun sbytes_noalign(count: Int, out: ByteArray) {
        var offset = 0
        var count = count
        if (bitsavailable >= 8) {
            //println("EXPECTED: $lastReadByte, bitsavailable=$bitsavailable")
            if (bitsavailable % 8 != 0) {
                val bits = (bitsavailable % 8)
                skipBits(bits)
                //println("SKIP $bits")
            }
            //println("bitsavailable=$bitsavailable")
            while (bitsavailable >= 8) {
                val byte = readBits(8).toByte()
                //println("RECOVERED $byte")
                out[offset++] = byte
                count--
            }
        }
        discardBits()
        val readCount = sbuffers.read(out, offset, count)
        if (readCount > 0) sbuffersReadPos += readCount
		//for (n in 0 until count) out[offset + n] = _su8().toByte()
	}

	fun sbytes(count: Int): ByteArray = sbytes(count, ByteArray(count))
	fun sbytes(count: Int, out: ByteArray): ByteArray {
        sbytes_noalign(count, out)
        return out
    }
	fun su8(): Int = discardBits()._su8()
    fun su16LE(): Int {
        sbytes_noalign(2, temp)
        return temp.getU16LE(0)
    }
    fun su32LE(): Int {
        sbytes_noalign(4, temp)
        return temp.getS32LE(0)
    }
    fun su32BE(): Int {
        sbytes_noalign(4, temp)
        return temp.getS32BE(0)
    }

	private val temp = ByteArray(4)
	suspend fun abytes(count: Int, out: ByteArray = ByteArray(count)): ByteArray {
        prepareBytesUpTo(count)
        return sbytes(count, out)
    }
	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        prepareBytesUpTo(len)
		val out = sbuffers.read(buffer, offset, len)
        sbuffersReadPos += out
        return out
	}

	override suspend fun close() {
		s.close()
	}

	suspend fun strz(): String = MemorySyncStreamToByteArray {
		discardBits()
		while (true) {
			prepareBigChunkIfRequired()
			val c = _su8()
			if (c == 0) break
			write8(c)
		}
	}.toString(ASCII)

	@Suppress("EXPERIMENTAL_API_USAGE")
    suspend fun copyTo(o: AsyncOutputStream) {
		while (true) {
			prepareBigChunkIfRequired()
            val read = sbuffers.availableReadBeforeWrap
			if (read <= 0) break
            sbuffersReadPos += read
			o.writeBytes(sbuffers.internalBuffer, sbuffers.internalReadPos, read)
            sbuffers.internalReadSkip(read)
		}
	}

	//suspend fun readAll(): ByteArray {
	//	val temp = ByteArray(sbuffers.availableRead)
	//	sbuffers.readBytes(temp, 0, sbuffers.availableRead)
	//	return temp + s.readAll()
	//}
//
	//suspend fun hasAvailable() = s.hasAvailable()
	//suspend fun getAvailable(): Long = s.getAvailable()
	//suspend fun readBytesExact(count: Int): ByteArray = abytes(count)

	override suspend fun getPosition(): Long = sbuffersReadPos.toLong()
    override suspend fun hasLength(): Boolean = (s as? AsyncGetLengthStream)?.hasLength() ?: false
    override suspend fun getLength(): Long = (s as? AsyncGetLengthStream)?.getLength() ?: error("Length not available on Stream")
}
