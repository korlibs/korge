package com.codeazur.as3swf.utils

import com.codeazur.as3swf.data.SWFMatrix
import com.soywiz.korfl.amf.AMF3
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.async.executeInWorkerSync
import com.soywiz.korio.stream.*
import java.io.ByteArrayOutputStream
import java.lang.Float
import java.nio.charset.Charset
import java.util.zip.Deflater
import java.util.zip.Inflater

object ColorUtils {
	fun alpha(color: Int): Double = (color ushr 24).toDouble() / 255
	fun rgb(color: Int): Int = (color and 0xffffff)
	fun r(color: Int): Double = ((rgb(color) ushr 16) and 0xff).toDouble() / 255
	fun g(color: Int): Double = ((rgb(color) ushr 8) and 0xff).toDouble() / 255
	fun b(color: Int): Double = ((rgb(color) ushr 0) and 0xff).toDouble() / 255

	fun interpolate(color1: Int, color2: Int, ratio: Double): Int {
		val r1 = r(color1)
		val g1 = g(color1)
		val b1 = b(color1)
		val alpha1 = alpha(color1)
		val ri = ((r1 + (r(color2) - r1) * ratio) * 255).toInt()
		val gi = ((g1 + (g(color2) - g1) * ratio) * 255).toInt()
		val bi = ((b1 + (b(color2) - b1) * ratio) * 255).toInt()
		val alphai = ((alpha1 + (alpha(color2) - alpha1) * ratio) * 255).toInt()
		return bi or (gi shl 8) or (ri shl 16) or (alphai shl 24)
	}

	fun rgbToString(color: Int): String = "#%06x".format((color and 0xffffff))
	fun rgbaToString(color: Int): String = "#%06x(%02x)".format((color and 0xffffff), (color ushr 24))
	fun argbToString(color: Int): String = "#(%02x)%06x".format((color ushr 24), (color and 0xffffff))
}

object MatrixUtils {
	fun interpolate(matrix1: SWFMatrix, matrix2: SWFMatrix, ratio: Double): SWFMatrix {
		// TODO: not sure about this at all
		val matrix = SWFMatrix()
		matrix.scaleX = matrix1.scaleX + (matrix2.scaleX - matrix1.scaleX) * ratio
		matrix.scaleY = matrix1.scaleY + (matrix2.scaleY - matrix1.scaleY) * ratio
		matrix.rotateSkew0 = matrix1.rotateSkew0 + (matrix2.rotateSkew0 - matrix1.rotateSkew0) * ratio
		matrix.rotateSkew1 = matrix1.rotateSkew1 + (matrix2.rotateSkew1 - matrix1.rotateSkew1) * ratio
		matrix.translateX = (matrix1.translateX + (matrix2.translateX - matrix1.translateX) * ratio).toInt()
		matrix.translateY = (matrix1.translateY + (matrix2.translateY - matrix1.translateY) * ratio).toInt()
		return matrix
	}
}

object NumberUtils {
	fun roundPixels20(pixels: Double): Double = Math.round(pixels * 100).toDouble() / 100
	fun roundPixels400(pixels: Double): Double = Math.round(pixels * 10000).toDouble() / 10000
}

fun ByteArray.toFlash(): FlashByteArray = FlashByteArray(this)

@Suppress("UNUSED_PARAMETER")
open class FlashByteArray() {
	constructor(data: ByteArray) : this() {
		this.data.writeBytes(data)
		this.length = this.data.length.toInt()
	}

	val content = MemorySyncStreamBase()
	val data = SyncStream(content)

	var endian = Endian.BIG_ENDIAN
	var position: Int; get() = data.position.toInt(); set(value) = run { data.position = value.toLong() }
	var length: Int; get() = data.length.toInt(); set(value) = run { data.length = value.toLong() }

	private fun ensureIndex(index: Int) = this.apply {
		content.data.size = Math.max(content.data.size, index + 1)
	}

	operator fun get(index: Int): Int {
		return ensureIndex(index).content.data.data[index].toInt() and 0xFF
	}

	operator fun set(index: Int, value: Int): Int = value.apply {
		ensureIndex(index).content.data.data[index] = value.toByte()
	}

	val little: Boolean get() = endian == Endian.LITTLE_ENDIAN

	fun readByte(): Int = data.readU8()
	fun readShort(): Int = if (little) data.readS16_le() else data.readS16_be()
	fun readInt(): Int = if (little) data.readS32_le() else data.readS32_be()
	fun readLong(): Long = if (little) data.readS64_le() else data.readS64_be()

	fun readUnsignedByte(): Int = readByte() and 0xFF
	fun readUnsignedShort(): Int = readShort() and 0xFFFF
	fun readUnsignedInt(): Int = readInt()

	fun readFloat(): Double = Float.intBitsToFloat(readInt()).toDouble()
	fun readDouble(): Double = java.lang.Double.longBitsToDouble(readLong())

	fun readUTFBytes(len: Int): String = String(content.data.data, position, len, Charset.forName("UTF-8")).apply { position += len }

	fun writeByte(i: Int): Unit = data.write8(i)
	fun writeShort(i: Int): Unit = if (little) data.write16_le(i) else data.write16_be(i)
	fun writeInt(i: Int): Unit = if (little) data.write32_le(i) else data.write32_be(i)
	fun writeLong(i: Long): Unit = run { if (little) data.write64_le(i) else data.write64_be(i) }

	fun writeUnsignedByte(i: Int): Unit = writeByte(i)
	fun writeUnsignedShort(i: Int): Unit = writeShort(i)
	fun writeUnsignedInt(i: Int): Unit = writeInt(i)

	fun writeFloat(v: Double): Unit = writeInt(Float.floatToIntBits(v.toFloat()))
	fun writeDouble(v: Double): Unit = writeLong(java.lang.Double.doubleToLongBits(v))

	fun writeUTF(value: String): Unit = TODO()
	fun writeUTFBytes(str: String, position: Int = 0, length: Int = -1): Unit = throw Error("")

	fun writeBytes(bytes: FlashByteArray, offset: Int = 0, length: Int = -1): Unit {
		val len = if (length >= 0) length else bytes.length
		bytes.position = offset
		for (n in 0 until len) writeByte(bytes.readByte())
	}

	private fun _uncompress(data: ByteArray, method: String = "zlib"): ByteArray {
		when (method) {
			"zlib" -> {
				val inflater = Inflater()
				inflater.setInput(data)
				val outputStream = ByteArrayOutputStream(data.size)
				val buffer = ByteArray(1024)
				while (!inflater.finished()) {
					val count = inflater.inflate(buffer)
					outputStream.write(buffer, 0, count)
				}
				outputStream.close()
				return outputStream.toByteArray()
			}
			else -> {
				TODO("Unsupported compression method $method")
			}
		}
	}

	private fun _compress(data: ByteArray, method: String = "zlib"): ByteArray {
		when (method) {
			"zlib" -> {
				val deflater = Deflater()
				deflater.setInput(data)
				val outputStream = ByteArrayOutputStream()
				val buffer = ByteArray(1024)
				while (!deflater.finished()) {
					val count = deflater.deflate(buffer)
					if (count <= 0) break
					outputStream.write(buffer, 0, count)
				}
				outputStream.close()
				return outputStream.toByteArray()
			}
			else -> {
				TODO("Unsupported compression method $method")
			}
		}
	}

	fun replaceBytes(content: ByteArray): Unit {
		data.position = 0L
		data.length = 0L
		data.writeBytes(content)
		position = 0
	}

	fun uncompress(method: String = "zlib") = replaceBytes(_uncompress(cloneToNewByteArray(), method))
	fun compress(method: String = "zlib"): Unit = replaceBytes(_compress(cloneToNewByteArray(), method))

	suspend fun uncompressInWorker(method: String = "zlib") = replaceBytes(executeInWorkerSync { _uncompress(cloneToNewByteArray(), method) })
	suspend fun compressInWorker(method: String = "zlib"): Unit = replaceBytes(executeInWorkerSync { _compress(cloneToNewByteArray(), method) })

	fun readBytes(bytes: FlashByteArray) = readBytes(bytes, bytes.position, bytesAvailable)

	fun readBytes(bytes: FlashByteArray, position: Int, length: Int = -1) {
		val len = if (length >= 0) length else bytesAvailable
		bytes.position = position
		for (n in 0 until len) bytes.writeByte(this.readByte())
	}

	fun readUTF(): String = TODO()
	val bytesAvailable: Int get() = length - position
	open fun resetBitsPending(): FlashByteArray = this

	fun readObject(): Any? {
		resetBitsPending()
		return AMF3.read(data)
	}

	fun writeObject(metaData: Any?): Unit = TODO()

	fun cloneToNewFlashByteArray(): FlashByteArray = cloneToNewByteArray().toFlash()
	fun cloneToNewByteArray(): ByteArray = this.content.data.toByteArray()
}


open class BitArray : FlashByteArray() {
	protected var bitsPending: Int = 0

	fun readBits(_bits: Int, _bitBuffer: Int = 0): Int {
		var bits = _bits
		var bitBuffer = _bitBuffer
		if (bits == 0) {
			return bitBuffer
		}
		val partial: Int
		val bitsConsumed: Int
		if (bitsPending > 0) {
			val byte: Int = this[position - 1] and (0xff ushr (8 - bitsPending))
			bitsConsumed = Math.min(bitsPending, bits)
			bitsPending -= bitsConsumed
			partial = byte ushr bitsPending
		} else {
			bitsConsumed = Math.min(8, bits)
			bitsPending = 8 - bitsConsumed
			partial = readUnsignedByte() ushr bitsPending
		}
		bits -= bitsConsumed
		bitBuffer = (bitBuffer shl bitsConsumed) or partial
		return if (bits > 0) readBits(bits, bitBuffer) else bitBuffer
	}

	fun writeBits(_bits: Int, _value: Int) {
		var bits = _bits
		var value = _value
		if (bits == 0) {
			return
		}
		value = value and (0xffffffff ushr (32 - bits)).toInt()
		val bitsConsumed: Int
		if (bitsPending > 0) {
			if (bitsPending > bits) {
				this[position - 1] = this[position - 1] or (value shl (bitsPending - bits))
				bitsConsumed = bits
				bitsPending -= bits
			} else if (bitsPending == bits) {
				this[position - 1] = this[position - 1] or value
				bitsConsumed = bits
				bitsPending = 0
			} else {
				this[position - 1] = this[position - 1] or value ushr (bits - bitsPending)
				bitsConsumed = bitsPending
				bitsPending = 0
			}
		} else {
			bitsConsumed = Math.min(8, bits)
			bitsPending = 8 - bitsConsumed
			writeByte((value ushr (bits - bitsConsumed)) shl bitsPending)
		}
		bits -= bitsConsumed
		if (bits > 0) {
			writeBits(bits, value)
		}
	}

	fun writeSingleBit(value: Boolean) {
		writeBits(1, if (value) 1 else 0)
	}

	override fun resetBitsPending(): FlashByteArray = this.apply {
		bitsPending = 0
	}

	fun calculateMaxBits(signed: Boolean, values: List<Int>): Int {
		return calculateMaxBits(signed, *values.toIntArray())
	}

	fun calculateMaxBits(signed: Boolean, vararg values: Int): Int {
		var b: Int = 0
		var vmax = Integer.MIN_VALUE
		if (!signed) {
			for (usvalue in values) {
				b = b or usvalue
			}
		} else {
			for (svalue in values) {
				if (svalue >= 0) {
					b = b or svalue
				} else {
					b = b or (svalue.inv() shl 1)
				}
				if (vmax < svalue) {
					vmax = svalue
				}
			}
		}
		var bits: Int = 0
		if (b > 0) {
			bits = b.toString(2).length
			if (signed && vmax > 0 && vmax.toString(2).length >= bits) {
				bits++
			}
		}
		return bits
	}
}

enum class Endian { LITTLE_ENDIAN, BIG_ENDIAN }
