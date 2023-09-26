package korlibs.memory

import java.nio.*


fun java.nio.Buffer.positionSafe(newPosition: Int) {
    position(newPosition)
}

fun java.nio.Buffer.limitSafe(newLimit: Int) {
    limit(newLimit)
}

fun java.nio.Buffer.flipSafe() {
    flip()
}

fun java.nio.Buffer.clearSafe() {
    clear()
}

internal fun java.nio.ByteBuffer.slicedBuffer(offset: Int, size: Int): ByteBuffer {
    return this.duplicate().also {
        it.order(ByteOrder.nativeOrder())
        it.positionSafe(offset)
        it.limitSafe(offset + size)
    }
}

@PublishedApi
internal fun java.nio.Buffer.checkSliceBounds(offset: Int, size: Int) {
    //val end = offset + size - 1
    //if (offset !in 0 until this.capacity()) error("offset=$offset, size=$size not inside ${this.capacity()}")
    //if (end !in 0 until this.capacity()) error("offset=$offset, size=$size not inside ${this.capacity()}")
}

inline fun <T : java.nio.Buffer> T._slice(offset: Int, size: Int, dup: (T) -> T): T {
    checkSliceBounds(offset, size)
    val out = dup(this)
    val start = this.position() + offset
    val end = start + size
    out.positionSafe(start)
    out.limitSafe(end)
    return out
}

fun ByteBuffer._slice(offset: Int, size: Int): ByteBuffer = _slice(offset, size) { it.duplicate() }
fun ShortBuffer._slice(offset: Int, size: Int): ShortBuffer = _slice(offset, size) { it.duplicate() }
fun IntBuffer._slice(offset: Int, size: Int): IntBuffer = _slice(offset, size) { it.duplicate() }
fun FloatBuffer._slice(offset: Int, size: Int): FloatBuffer = _slice(offset, size) { it.duplicate() }
fun DoubleBuffer._slice(offset: Int, size: Int): DoubleBuffer = _slice(offset, size) { it.duplicate() }

fun Buffer.slicedBuffer(): java.nio.ByteBuffer = data.bufferLE
fun Buffer.slicedBuffer(offset: Int, length: Int): java.nio.ByteBuffer = data.bufferLE._slice(offset, length)
val Buffer.nioBuffer: java.nio.ByteBuffer get() = this.slicedBuffer()
val Buffer.nioIntBuffer: java.nio.IntBuffer get() = this.slicedBuffer().asIntBuffer()
val Buffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.slicedBuffer().asFloatBuffer()
