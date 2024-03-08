package korlibs.io.compression.util

import korlibs.math.unsigned

internal class SlidingWindow(val nbits: Int) {
	val data = ByteArray(1 shl nbits)
	val mask = data.size - 1
	var pos = 0

	fun get(offset: Int): Int {
		return data[(pos - offset) and mask].toInt() and 0xFF
	}

	fun getPut(offset: Int): Int = put(get(offset))

	fun put(value: Int): Int {
		data[pos] = value.toByte()
		pos = (pos + 1) and mask
		return value
	}

    // @TODO: Optimize?
	fun putBytes(bytes: ByteArray, offset: Int, len: Int) {
		for (n in 0 until len) put(bytes[offset + n].unsigned)
	}
}
