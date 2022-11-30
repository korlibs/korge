package com.soywiz.kmem

import java.nio.*

actual class NBuffer(val buffer: ByteBuffer, val offset: Int, val size: Int) {
    fun slicedBuffer(roffset: Int = 0, rsize: Int = this.size - roffset): ByteBuffer {
        val pos = this.offset + roffset
        return buffer.duplicate().also {
            it.order(ByteOrder.nativeOrder())
            it.positionSafe(pos)
            it.limitSafe(pos + rsize)
        }
    }

    override fun toString(): String = NBuffer_toString(this)
    actual companion object { }
}

actual fun NBuffer(size: Int, direct: Boolean): NBuffer {
    checkNBufferSize(size)
    return NBuffer(
        if (direct) ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()) else ByteBuffer.wrap(ByteArray(size)).order(ByteOrder.nativeOrder()),
        0, size
    )
}

actual fun NBuffer(array: ByteArray, offset: Int, size: Int): NBuffer {
    checkNBufferWrap(array, offset, size)
    return NBuffer(ByteBuffer.wrap(array, offset, size).order(ByteOrder.nativeOrder()), offset, size)
}

actual val NBuffer.byteOffset: Int get() = offset
actual val NBuffer.sizeInBytes: Int get() = size
actual fun NBuffer.sliceInternal(start: Int, end: Int): NBuffer = NBuffer(buffer, offset + start, end - start)

actual fun NBuffer.Companion.copy(src: NBuffer, srcPosBytes: Int, dst: NBuffer, dstPosBytes: Int, sizeInBytes: Int) {

    //val srcBuf = src.buffer
    //val dstBuf = dst.buffer
    //val size = sizeInBytes
    //val srcPos = src.offset + srcPosBytes
    //val dstPos = dst.offset + dstPosBytes
    //dstBuf.keepPositionLimit {
    //    srcBuf.keepPositionLimit {
    //        dstBuf.positionSafe(dstPos)
    //        srcBuf.positionSafe(srcPos)
    //        srcBuf.limitSafe(srcPos + size)
    //        dstBuf.put(srcBuf)
    //    }
    //}
    val srcBuf = src.buffer
    val dstBuf = dst.buffer
    val srcPos = src.offset + srcPosBytes
    val dstPos = dst.offset + dstPosBytes
    val size = sizeInBytes

    if (!srcBuf.isDirect && !dstBuf.isDirect) {
        System.arraycopy(srcBuf.array(), srcPos, dstBuf.array(), dstPos, size)
        return
    }

    if (srcBuf === dstBuf) {
        arraycopy(size, srcBuf, srcPosBytes, dstBuf, dstPosBytes, { it, value -> dst.setUnalignedUInt8(it, value) }, { src.getUnalignedUInt8(it) })
        return
    }

    dst.slicedBuffer(dstPosBytes, sizeInBytes).put(src.slicedBuffer(srcPosBytes, sizeInBytes))
}

//internal inline fun <T> java.nio.ByteBuffer.keepPositionLimit(block: () -> T): T {
//    val oldPos = this.position()
//    val oldLimit = this.limit()
//    try {
//        return block()
//    } finally {
//        this.limitSafe(oldLimit)
//        this.positionSafe(oldPos)
//    }
//}

// Unaligned versions

actual fun NBuffer.getUnalignedInt8(byteOffset: Int): Byte = buffer.get(offset + byteOffset)
actual fun NBuffer.getUnalignedInt16(byteOffset: Int): Short = buffer.getShort(offset + byteOffset)
actual fun NBuffer.getUnalignedInt32(byteOffset: Int): Int = buffer.getInt(offset + byteOffset)
actual fun NBuffer.getUnalignedInt64(byteOffset: Int): Long = buffer.getLong(offset + byteOffset)
actual fun NBuffer.getUnalignedFloat32(byteOffset: Int): Float = buffer.getFloat(offset + byteOffset)
actual fun NBuffer.getUnalignedFloat64(byteOffset: Int): Double = buffer.getDouble(offset + byteOffset)

actual fun NBuffer.setUnalignedInt8(byteOffset: Int, value: Byte) {
    buffer.put(offset + byteOffset, value)
}

actual fun NBuffer.setUnalignedInt16(byteOffset: Int, value: Short) {
    buffer.putShort(offset + byteOffset, value)
}

actual fun NBuffer.setUnalignedInt32(byteOffset: Int, value: Int) {
    buffer.putInt(offset + byteOffset, value)
}

actual fun NBuffer.setUnalignedInt64(byteOffset: Int, value: Long) {
    buffer.putLong(offset + byteOffset, value)
}

actual fun NBuffer.setUnalignedFloat32(byteOffset: Int, value: Float) {
    buffer.putFloat(offset + byteOffset, value)
}

actual fun NBuffer.setUnalignedFloat64(byteOffset: Int, value: Double) {
    buffer.putDouble(offset + byteOffset, value)
}

@PublishedApi
internal fun java.nio.Buffer.checkSliceBounds(offset: Int, size: Int) {
    //val end = offset + size - 1
    //if (offset !in 0 until this.capacity()) error("offset=$offset, size=$size not inside ${this.capacity()}")
    //if (end !in 0 until this.capacity()) error("offset=$offset, size=$size not inside ${this.capacity()}")
}

fun Buffer.positionSafe(newPosition: Int) {
    position(newPosition)
}

fun Buffer.limitSafe(newLimit: Int) {
    limit(newLimit)
}

fun Buffer.flipSafe() {
    flip()
}

fun Buffer.clearSafe() {
    clear()
}

inline fun <T : Buffer> T._slice(offset: Int, size: Int, dup: (T) -> T): T {
    checkSliceBounds(offset, size)
    val out = dup(this)
    val start = this.position() + offset
    val end = start + size
    out.positionSafe(start)
    out.limitSafe(end)
    return out
}

fun ByteBuffer.slice(offset: Int, size: Int): ByteBuffer = _slice(offset, size) { it.duplicate() }
fun ShortBuffer.slice(offset: Int, size: Int): ShortBuffer = _slice(offset, size) { it.duplicate() }
fun IntBuffer.slice(offset: Int, size: Int): IntBuffer = _slice(offset, size) { it.duplicate() }
fun FloatBuffer.slice(offset: Int, size: Int): FloatBuffer = _slice(offset, size) { it.duplicate() }
fun DoubleBuffer.slice(offset: Int, size: Int): DoubleBuffer = _slice(offset, size) { it.duplicate() }

val NBuffer.nioBuffer: java.nio.ByteBuffer get() = this.slicedBuffer()
val NBuffer.nioIntBuffer: java.nio.IntBuffer get() = this.slicedBuffer().asIntBuffer()
val NBuffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.slicedBuffer().asFloatBuffer()
