package com.soywiz.korio.stream

import com.soywiz.kmem.*
import com.soywiz.korio.concurrent.*
import java.io.*

class FileSyncStreamBase(val file: java.io.File, val mode: String = "r") : SyncStreamBase() {
	val ra = RandomAccessFile(file, mode)

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		ra.seek(position)
		return ra.read(buffer, offset, len)
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		ra.seek(position)
		ra.write(buffer, offset, len)
	}

	override var length: Long
		get() = ra.length()
		set(value) = run { ra.setLength(value) }

	override fun close() = ra.close()
}

fun File.openSync(mode: String = "r"): SyncStream = FileSyncStreamBase(this, mode).toSyncStream()

fun InputStream.toSyncStream(): SyncInputStream {
	val iss = this
	val tempByte = ByteArray(1)
	return object : SyncInputStream {
		override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
			return iss.read(buffer, offset, len)
		}

		override fun read(): Int {
			val size = read(tempByte, 0, 1)
			if (size <= 0) return -1
			return tempByte[0].unsigned
		}
	}
}

fun SyncStream.toInputStream(): InputStream {
	val ss = this
	return object : InputStream() {
		override fun read(): Int = if (ss.eof) -1 else ss.readU8()
		override fun read(b: ByteArray, off: Int, len: Int): Int = ss.read(b, off, len)
		override fun available(): Int = ss.available.toInt()
	}
}
