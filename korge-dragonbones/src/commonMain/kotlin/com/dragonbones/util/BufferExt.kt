package com.dragonbones.util

import com.soywiz.kmem.*

internal fun Int16Buffer.toFloat(): Float32Buffer {
	val out = Float32BufferAlloc(this.size)
	for (n in 0 until out.size) out[n] = this[n].toFloat()
	return out
}
