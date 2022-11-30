package com.soywiz.kmem

import java.nio.*

actual class Buffer(val buffer: ByteBuffer, val offset: Int, val size: Int) {
    fun slicedBuffer(roffset: Int = 0, rsize: Int = this.size - roffset): ByteBuffer {
        val pos = this.offset + roffset
        return buffer.duplicate().also {
            it.order(ByteOrder.nativeOrder())
            it.positionSafe(pos)
            it.limitSafe(pos + rsize)
        }
    }

    override fun toString(): String = NBuffer_toString(this)

    actual companion object {
        actual fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {

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

    }
}

actual fun Buffer(size: Int, direct: Boolean): Buffer {
    checkNBufferSize(size)
    return Buffer(
        if (direct) ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()) else ByteBuffer.wrap(ByteArray(size)).order(ByteOrder.nativeOrder()),
        0, size
    )
}

actual fun Buffer(array: ByteArray, offset: Int, size: Int): Buffer {
    checkNBufferWrap(array, offset, size)
    return Buffer(ByteBuffer.wrap(array, offset, size).order(ByteOrder.nativeOrder()), offset, size)
}

actual val Buffer.byteOffset: Int get() = offset
actual val Buffer.sizeInBytes: Int get() = size
actual fun Buffer.sliceInternal(start: Int, end: Int): Buffer =
    Buffer(buffer, offset + start, end - start)


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

actual fun Buffer.getUnalignedInt8(byteOffset: Int): Byte = buffer.get(offset + byteOffset)
actual fun Buffer.getUnalignedInt16(byteOffset: Int): Short = buffer.getShort(offset + byteOffset)
actual fun Buffer.getUnalignedInt32(byteOffset: Int): Int = buffer.getInt(offset + byteOffset)
actual fun Buffer.getUnalignedInt64(byteOffset: Int): Long = buffer.getLong(offset + byteOffset)
actual fun Buffer.getUnalignedFloat32(byteOffset: Int): Float = buffer.getFloat(offset + byteOffset)
actual fun Buffer.getUnalignedFloat64(byteOffset: Int): Double = buffer.getDouble(offset + byteOffset)

actual fun Buffer.setUnalignedInt8(byteOffset: Int, value: Byte) {
    buffer.put(offset + byteOffset, value)
}

actual fun Buffer.setUnalignedInt16(byteOffset: Int, value: Short) {
    buffer.putShort(offset + byteOffset, value)
}

actual fun Buffer.setUnalignedInt32(byteOffset: Int, value: Int) {
    buffer.putInt(offset + byteOffset, value)
}

actual fun Buffer.setUnalignedInt64(byteOffset: Int, value: Long) {
    buffer.putLong(offset + byteOffset, value)
}

actual fun Buffer.setUnalignedFloat32(byteOffset: Int, value: Float) {
    buffer.putFloat(offset + byteOffset, value)
}

actual fun Buffer.setUnalignedFloat64(byteOffset: Int, value: Double) {
    buffer.putDouble(offset + byteOffset, value)
}

@PublishedApi
internal fun java.nio.Buffer.checkSliceBounds(offset: Int, size: Int) {
    //val end = offset + size - 1
    //if (offset !in 0 until this.capacity()) error("offset=$offset, size=$size not inside ${this.capacity()}")
    //if (end !in 0 until this.capacity()) error("offset=$offset, size=$size not inside ${this.capacity()}")
}

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

inline fun <T : java.nio.Buffer> T._slice(offset: Int, size: Int, dup: (T) -> T): T {
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

val Buffer.nioBuffer: java.nio.ByteBuffer get() = this.slicedBuffer()
val Buffer.nioIntBuffer: java.nio.IntBuffer get() = this.slicedBuffer().asIntBuffer()
val Buffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.slicedBuffer().asFloatBuffer()
