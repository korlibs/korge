package com.soywiz.korio.compression.deflate

import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.util.*
import com.soywiz.korio.experimental.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.checksum.*
import com.soywiz.krypto.encoding.*

open class GZIP(deflater: () -> CompressionMethod) : GZIPBase(true, deflater) {
	companion object : GZIP({ Deflate })
}
open class GZIPNoCrc(deflater: () -> CompressionMethod) : GZIPBase(false, deflater) {
	companion object : GZIPNoCrc({ Deflate })
}

@UseExperimental(KorioExperimentalApi::class)
open class GZIPBase(val checkCrc: Boolean, val deflater: () -> CompressionMethod) : CompressionMethod {
	override suspend fun uncompress(reader: BitReader, out: AsyncOutputStream) {
		val r = reader
		r.prepareBigChunkIfRequired()
		if (r.su8() != 31 || r.su8() != 139) error("Not a GZIP file")
		val method = r.su8()
		if (method != 8) error("Just supported deflate in GZIP")
		val ftext = r.sreadBit()
		val fhcrc = r.sreadBit()
		val fextra = r.sreadBit()
		val fname = r.sreadBit()
		val fcomment = r.sreadBit()
		val reserved = r.readBits(3)
		val mtime = r.su32LE()
		val xfl = r.su8()
		val os = r.su8()
		val extra = if (fextra) r.abytes(r.su16LE()) else byteArrayOf()
		val name = if (fname) r.strz() else null
		val comment = if (fcomment) r.strz() else null
		val crc16 = if (fhcrc) r.su16LE() else 0
		var ccrc32 = CRC32.initialValue
		var csize = 0
		deflater().uncompress(r, object : AsyncOutputStream by out {
			override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
				if (len > 0) {
					//val oldCrc32 = ccrc32
					ccrc32 = CRC32.update(ccrc32, buffer, offset, len)
					csize += len

					//val chunk = buffer.sliceArray(offset until offset + len)
					//println("DECOMPRESS:" + chunk.toList() + " sum=${chunk.toList().sum()} oldCrc32=$oldCrc32, crc32=$ccrc32 [offset=$offset, len=$len]")
					out.write(buffer, offset, len)
				}
			}
		})
		r.prepareBigChunkIfRequired()
		val crc32 = r.su32LE()
		val size = r.su32LE()
		//println("COMPRESS: crc32=$crc32, size=$size, ccrc32=$ccrc32, csize=$csize")
		if (checkCrc && (csize != size || ccrc32 != crc32)) {
			invalidOp("Size doesn't match SIZE(${csize.hex} != ${size.hex}) || CRC32(${ccrc32.hex} != ${crc32.hex})")
		}
	}

	override suspend fun compress(
		i: BitReader,
		o: AsyncOutputStream,
		context: CompressionContext
	) {
		o.write8(31) // MAGIC[0]
		o.write8(139) // MAGIC[1]
		o.write8(8) // METHOD=8 (deflate)
		o.write8(0) // Presence bits
		o.write32LE(0) // Time
		o.write8(0) // xfl
		o.write8(0) // os

		var size = 0
		var crc32 = CRC32.initialValue
		deflater().compress(BitReader.forInput(object : AsyncInputStreamWithLength by i {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
				val read = i.read(buffer, offset, len)
				if (read > 0) {
					//val oldCrc32 = crc32
					crc32 = CRC32.update(crc32, buffer, offset, read)
					//val chunk = buffer.sliceArray(offset until offset + read)
					//println("COMPRESS:" + chunk.toList() + " sum=${chunk.toList().sum()} oldCrc32=$oldCrc32, crc32=$crc32 [offset=$offset, read=$read]")
					size += read
				}
				return read
			}
		}), o, context)
		o.write32LE(crc32)
		o.write32LE(size)
		//println("COMPRESS: crc32=$crc32, size=$size")
	}
}
