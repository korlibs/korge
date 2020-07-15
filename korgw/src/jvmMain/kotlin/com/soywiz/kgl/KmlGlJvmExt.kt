package com.soywiz.kgl

import com.soywiz.kmem.*
import java.awt.image.*
import java.nio.*

val FBuffer.nioBuffer: java.nio.ByteBuffer get() = this.mem.buffer.apply { (this as Buffer).rewind() }
val FBuffer.nioByteBuffer: java.nio.ByteBuffer get() = this.mem.buffer.apply { (this as Buffer).rewind() }
val FBuffer.nioIntBuffer: java.nio.IntBuffer get() = this.arrayInt.jbuffer.apply { (this as Buffer).rewind() }
val FBuffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.arrayFloat.jbuffer.apply { (this as Buffer).rewind() }

/*
class BufferedImageKmlNativeImageData(val buffered: BufferedImage) : NativeImage {
	override val width: Int get() = buffered.width
	override val height: Int get() = buffered.height
	val bytes = (buffered.raster.dataBuffer as DataBufferByte).data

	init {
		//for (y in 0 until 32) {
		//    val rowSize = 32 * 4
		//    val index = y * rowSize
		//    println(bytes.sliceArray(index + 32 until index + 64).toList())
		//}

		for (n in 0 until bytes.size step 4) {
			val v0 = bytes[n + 0]
			val v1 = bytes[n + 1]
			val v2 = bytes[n + 2]
			val v3 = bytes[n + 3]
			bytes[n + 0] = v3
			bytes[n + 1] = v2
			bytes[n + 2] = v1
			bytes[n + 3] = v0
		}
	}

	val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
		clear()
		put(bytes)
		//println("BYTES: ${bytes.size}")
		flip()
	}
}
*/
