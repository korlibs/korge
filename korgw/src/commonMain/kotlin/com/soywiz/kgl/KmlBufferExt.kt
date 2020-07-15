package com.soywiz.kgl

import com.soywiz.kmem.*

fun FBuffer.toAsciiString(): String {
	var out = ""
	for (n in 0 until mem.size) {
		val b = getByte(n)
		if (b == 0.toByte()) break
		out += b.toChar()
	}
	return out
}

fun FBuffer.putAsciiString(str: String): FBuffer {
	var n = 0
	for (c in str) {
		if (mem.size >= n) setByte(n++, c.toByte())
	}
	if (mem.size >= n) setByte(n++, 0.toByte())
	return this
}

fun kmlByteBufferOf(vararg values: Byte) =
	FBuffer(values.size * 1).apply { for (n in 0 until values.size) this.setByte(n, values[n]) }

fun kmlShortBufferOf(vararg values: Short) =
    FBuffer(values.size * 2).apply { for (n in 0 until values.size) this.setShort(n, values[n]) }

fun kmlIntBufferOf(vararg values: Int) =
    FBuffer(values.size * 4).apply { for (n in 0 until values.size) this.setInt(n, values[n]) }

fun kmlFloatBufferOf(vararg values: Float) =
    FBuffer(values.size * 4).apply { for (n in 0 until values.size) this.setFloat(n, values[n]) }

inline fun <T> DataBufferAlloc(size: Int, callback: (FBuffer) -> T): T {
	val buffer = FBuffer(size)
	try {
		return callback(buffer)
	} finally {
		//buffer.dispose()
	}
}

fun <T> IntArray.toTempBuffer(callback: (FBuffer) -> T): T {
	return fbuffer(this.size) { buffer: FBuffer ->
		val ints = buffer.arrayInt
		for (n in this.indices) ints[n] = this[n]
		callback(buffer)
	}
}
