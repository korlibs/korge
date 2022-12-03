package com.soywiz.kgl

import com.soywiz.kmem.*

private val smallDirectBuffer = Buffer.allocDirect(16 * 1024)

internal val Buffer.directBuffer: java.nio.ByteBuffer get() = when {
    this.nioBuffer.isDirect -> this.slicedBuffer()
    else -> {
        if (this.sizeInBytes > smallDirectBuffer.sizeInBytes) {
            this.clone(direct = true).slicedBuffer()
        } else {
            arraycopy(this, 0, smallDirectBuffer, 0, this.sizeInBytes)
            smallDirectBuffer.slicedBuffer(0, this.sizeInBytes)
        }
    }
}
internal val Buffer.directIntBuffer: java.nio.IntBuffer get() = directBuffer.asIntBuffer()
internal val Buffer.directFloatBuffer: java.nio.FloatBuffer get() = directBuffer.asFloatBuffer()
