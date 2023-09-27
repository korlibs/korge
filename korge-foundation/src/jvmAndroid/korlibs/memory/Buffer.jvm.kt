package korlibs.memory

import java.nio.*

actual class Buffer(val buffer: ByteBuffer) {
    val bufferLE = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
    val bufferBE = buffer.duplicate().order(ByteOrder.BIG_ENDIAN)
    actual val byteOffset: Int = buffer.position()
    actual val sizeInBytes: Int = buffer.limit() - buffer.position()

    actual constructor(size: Int, direct: Boolean) : this(
        (if (direct) ByteBuffer.allocateDirect(checkNBufferSize(size)) else ByteBuffer.wrap(ByteArray(checkNBufferSize(size)))).order(ByteOrder.nativeOrder())
    )
    actual constructor(array: ByteArray, offset: Int, size: Int) : this(
        ByteBuffer.wrap(checkNBufferWrap(array, offset, size), offset, size).order(ByteOrder.nativeOrder())
            .also { it.positionSafe(offset); it.limitSafe(offset + size) }
    )

    actual fun sliceInternal(start: Int, end: Int): Buffer = Buffer(slicedBuffer(start, end - start))
    actual fun transferBytes(bufferOffset: Int, array: ByteArray, arrayOffset: Int, len: Int, toArray: Boolean): Unit {
        val temp = slicedBuffer(bufferOffset)
        if (toArray) {
            temp.get(array, arrayOffset, len)
        } else {
            temp.put(array, arrayOffset, len)
        }
    }

    fun slicedBuffer(roffset: Int = 0, rsize: Int = this.size - roffset): ByteBuffer {
        val pos = this.byteOffset + roffset
        return buffer.duplicate().also {
            it.order(ByteOrder.nativeOrder())
            it.positionSafe(pos)
            it.limitSafe(pos + rsize)
        }
    }

    actual fun getS8(byteOffset: Int): Byte = bufferLE.get(this.byteOffset + byteOffset)
    actual fun getS16LE(byteOffset: Int): Short = bufferLE.getShort(this.byteOffset + byteOffset)
    actual fun getS32LE(byteOffset: Int): Int = bufferLE.getInt(this.byteOffset + byteOffset)
    actual fun getS64LE(byteOffset: Int): Long = bufferLE.getLong(this.byteOffset + byteOffset)
    actual fun getF32LE(byteOffset: Int): Float = bufferLE.getFloat(this.byteOffset + byteOffset)
    actual fun getF64LE(byteOffset: Int): Double = bufferLE.getDouble(this.byteOffset + byteOffset)
    actual fun getS16BE(byteOffset: Int): Short = bufferBE.getShort(this.byteOffset + byteOffset)
    actual fun getS32BE(byteOffset: Int): Int = bufferBE.getInt(this.byteOffset + byteOffset)
    actual fun getS64BE(byteOffset: Int): Long = bufferBE.getLong(this.byteOffset + byteOffset)
    actual fun getF32BE(byteOffset: Int): Float = bufferBE.getFloat(this.byteOffset + byteOffset)
    actual fun getF64BE(byteOffset: Int): Double = bufferBE.getDouble(this.byteOffset + byteOffset)


    actual fun set8(byteOffset: Int, value: Byte) { bufferLE.put(this.byteOffset + byteOffset, value) }
    actual fun set16LE(byteOffset: Int, value: Short) { bufferLE.putShort(this.byteOffset + byteOffset, value) }
    actual fun set32LE(byteOffset: Int, value: Int) { bufferLE.putInt(this.byteOffset + byteOffset, value) }
    actual fun set64LE(byteOffset: Int, value: Long) { bufferLE.putLong(this.byteOffset + byteOffset, value) }
    actual fun setF32LE(byteOffset: Int, value: Float) { bufferLE.putFloat(this.byteOffset + byteOffset, value) }
    actual fun setF64LE(byteOffset: Int, value: Double) { bufferLE.putDouble(this.byteOffset + byteOffset, value) }
    actual fun set16BE(byteOffset: Int, value: Short) { bufferBE.putShort(this.byteOffset + byteOffset, value) }
    actual fun set32BE(byteOffset: Int, value: Int) { bufferBE.putInt(this.byteOffset + byteOffset, value) }
    actual fun set64BE(byteOffset: Int, value: Long) { bufferBE.putLong(this.byteOffset + byteOffset, value) }
    actual fun setF32BE(byteOffset: Int, value: Float) { bufferBE.putFloat(this.byteOffset + byteOffset, value) }
    actual fun setF64BE(byteOffset: Int, value: Double) { bufferBE.putDouble(this.byteOffset + byteOffset, value) }

    override fun hashCode(): Int = hashCodeCommon(this)
    override fun equals(other: Any?): Boolean = equalsCommon(this, other)
    override fun toString(): String = NBuffer_toString(this)

    actual companion object {
        actual fun equals(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int): Boolean =
            src.slicedBuffer(srcPosBytes, sizeInBytes) == dst.slicedBuffer(dstPosBytes, sizeInBytes)
        actual fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {
            val srcBuf = src.buffer
            val dstBuf = dst.buffer
            val srcPos = srcPosBytes
            val dstPos = dstPosBytes
            val size = sizeInBytes
            if (!srcBuf.isDirect && !dstBuf.isDirect) {
                System.arraycopy(srcBuf.array(), srcBuf.position() + srcPos, dstBuf.array(), dstBuf.position() + dstPos, size)
                return
            }
            //if (srcBuf === dstBuf) {
            //    arraycopy(size, srcBuf, srcPosBytes, dstBuf, dstPosBytes, { it, value -> dst.setUnalignedUInt8(it, value) }, { src.getUnalignedUInt8(it) })
            //    return
            //}
            dst.slicedBuffer(dstPosBytes, sizeInBytes).put(src.slicedBuffer(srcPosBytes, sizeInBytes))
        }
    }
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

inline fun <T : java.nio.Buffer> T.sliceBuffer(offset: Int, size: Int, dup: (T) -> T): T {
    checkSliceBounds(offset, size)
    val out = dup(this)
    val start = this.position() + offset
    val end = start + size
    out.positionSafe(start)
    out.limitSafe(end)
    return out
}

fun ByteBuffer.sliceBuffer(offset: Int, size: Int): ByteBuffer = this.sliceBuffer(offset, size) { it.duplicate() }
fun ShortBuffer.sliceBuffer(offset: Int, size: Int): ShortBuffer = this.sliceBuffer(offset, size) { it.duplicate() }
fun IntBuffer.sliceBuffer(offset: Int, size: Int): IntBuffer = this.sliceBuffer(offset, size) { it.duplicate() }
fun FloatBuffer.sliceBuffer(offset: Int, size: Int): FloatBuffer = this.sliceBuffer(offset, size) { it.duplicate() }
fun DoubleBuffer.sliceBuffer(offset: Int, size: Int): DoubleBuffer = this.sliceBuffer(offset, size) { it.duplicate() }

val Buffer.nioBuffer: java.nio.ByteBuffer get() = this.slicedBuffer()
val Buffer.nioIntBuffer: java.nio.IntBuffer get() = this.slicedBuffer().asIntBuffer()
val Buffer.nioFloatBuffer: java.nio.FloatBuffer get() = this.slicedBuffer().asFloatBuffer()
