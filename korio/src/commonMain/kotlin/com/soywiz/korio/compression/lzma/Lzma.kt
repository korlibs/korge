package com.soywiz.korio.compression.lzma

import com.soywiz.korio.compression.CompressionContext
import com.soywiz.korio.compression.CompressionMethod
import com.soywiz.korio.compression.util.BitReader
import com.soywiz.korio.experimental.KorioExperimentalApi
import com.soywiz.korio.stream.AsyncOutputStream
import com.soywiz.korio.stream.MemorySyncStreamToByteArray
import com.soywiz.korio.stream.openSync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.readBytesExact
import com.soywiz.korio.stream.readS64LE
import com.soywiz.korio.stream.write64LE
import com.soywiz.korio.stream.writeBytes

/**
 * @TODO: Streaming! (right now loads the whole stream in-memory)
 */
@OptIn(KorioExperimentalApi::class)
object Lzma : CompressionMethod {
	override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) {
		val input = reader.readAll().openSync()
		val properties = input.readBytesExact(5)
		val decoder = SevenZip.LzmaDecoder()
		if (!decoder.setDecoderProperties(properties)) throw Exception("Incorrect stream properties")
		val outSize = input.readS64LE()

		out.writeBytes(MemorySyncStreamToByteArray {
			if (!decoder.code(input, this, outSize)) throw Exception("Error in data stream")
		})
	}

	override suspend fun compress(i: BitReader, o: AsyncOutputStream, context: CompressionContext) {
		val algorithm = 2
		val matchFinder = 1
		val dictionarySize = 1 shl 23
		val lc = 3
		val lp = 0
		val pb = 2
		val fb = 128
		val eos = false

		val input = i.readAll()

		val out = MemorySyncStreamToByteArray {
			val output = this
			val encoder = SevenZip.LzmaEncoder()
			if (!encoder.setAlgorithm(algorithm)) throw Exception("Incorrect compression mode")
			if (!encoder.setDictionarySize(dictionarySize))
				throw Exception("Incorrect dictionary size")
			if (!encoder.setNumFastBytes(fb)) throw Exception("Incorrect -fb value")
			if (!encoder.setMatchFinder(matchFinder)) throw Exception("Incorrect -mf value")
			if (!encoder.setLcLpPb(lc, lp, pb)) throw Exception("Incorrect -lc or -lp or -pb value")
			encoder.setEndMarkerMode(eos)
			encoder.writeCoderProperties(this)
			val fileSize: Long = if (eos) -1 else input.size.toLong()
			this.write64LE(fileSize)
			encoder.code(input.openSync(), output, -1, -1, null)
		}

		o.writeBytes(out)
	}
}
