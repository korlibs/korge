package korlibs.kgl

import korlibs.memory.*

fun Buffer.toAsciiString(): String {
	var out = ""
	for (n in 0 until size) {
		val b = getS8(n)
		if (b == 0.toByte()) break
		out += b.toInt().toChar()
	}
    //println("BUFFER.toAsciiString: $out")
	return out
}

fun Buffer.putAsciiString(str: String): Buffer {
	var n = 0
	for (c in str) {
		if (size >= n) set8(n++, c.code.toByte())
	}
	if (size >= n) set8(n++, 0)
	return this
}

fun <T> IntArray.toTempBuffer(callback: (Buffer) -> T): T {
	return BufferTemp(this.size) { buffer: Buffer ->
		val ints = buffer.i32
		for (n in this.indices) ints[n] = this[n]
		callback(buffer)
	}
}
