package com.soywiz.korio.compression.util

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.experimental.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import kotlin.math.*

@KorioExperimentalApi
open class BitReader(val s: AsyncInputStream) : AsyncInputStreamWithLength {
	@PublishedApi
	internal var bitdata = 0
	@PublishedApi
	internal var bitsavailable = 0

	inline fun discardBits(): BitReader {
		this.bitdata = 0
		this.bitsavailable = 0
		return this
	}

	private val sbuffers = ByteArrayDeque()

	val BIG_CHUNK_SIZE = 8 * 1024
	val requirePrepare get() = sbuffers.availableRead < BIG_CHUNK_SIZE

	suspend fun prepareBigChunk(): BitReader = prepareBytesUpTo(BIG_CHUNK_SIZE)

	suspend inline fun prepareBigChunkIfRequired() {
		if (requirePrepare) prepareBigChunk()
	}

	fun returnToBuffer(data: ByteArray, offset: Int, size: Int) {
		sbuffers.write(data, offset, size)
	}

	private val tempBA = ByteArray(BIG_CHUNK_SIZE)
	suspend fun prepareBytesUpTo(expectedBytes: Int): BitReader {
		while (sbuffers.availableRead < expectedBytes) {
			val read = s.read(tempBA, 0, min(tempBA.size, expectedBytes))
			if (read <= 0) break // No more data
			sbuffers.write(tempBA, 0, read)
		}
		return this
	}

	fun readBits(bitcount: Int): Int {
		while (this.bitsavailable < bitcount) {
			this.bitdata = this.bitdata or (_su8() shl this.bitsavailable)
			this.bitsavailable += 8
		}
		val readed = this.bitdata and ((1 shl bitcount) - 1)
		this.bitdata = this.bitdata ushr bitcount
		this.bitsavailable -= bitcount
		return readed
	}

	fun sreadBit(): Boolean = readBits(1) != 0

	private fun _su8(): Int = sbuffers.readByte()

	fun sbytes_noalign(count: Int, out: ByteArray): ByteArray {
		for (n in 0 until count) out[n] = _su8().toByte()
		return out
	}

	fun sbytes(count: Int): ByteArray = sbytes(count, ByteArray(count))
	fun sbytes(count: Int, out: ByteArray): ByteArray = discardBits().sbytes_noalign(count, out)
	fun su8(): Int = discardBits()._su8()
	fun su16LE(): Int = sbytes(2, temp).readU16LE(0)
	fun su32LE(): Int = sbytes(4, temp).readS32LE(0)
	fun su32BE(): Int = sbytes(4, temp).readS32BE(0)

	private val temp = ByteArray(4)
	suspend fun abytes(count: Int, out: ByteArray = ByteArray(count)) = prepareBytesUpTo(count).sbytes(count, out)
	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		return prepareBytesUpTo(len).sbuffers.read(buffer, offset, len)
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

	suspend fun copyTo(o: AsyncOutputStream) {
		while (true) {
			prepareBigChunkIfRequired()
			val read = sbuffers.read(tempBA, 0, tempBA.size)
			if (read <= 0) break
			o.writeBytes(tempBA, 0, read)
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

	override suspend fun getPosition(): Long = sbuffers.read
	override suspend fun getLength(): Long = (s as? AsyncGetLengthStream)?.getLength() ?: error("Length not available on Stream")
}

