package korlibs.memory

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.experimental.*

@OptIn(ExperimentalNativeApi::class)
actual class Buffer(val data: ByteArray, val offset: Int, val size: Int, dummy: Unit) {
    actual constructor(size: Int, direct: Boolean) : this(ByteArray(checkNBufferSize(size)), 0, size, Unit)
    actual constructor(array: ByteArray, offset: Int, size: Int): this(checkNBufferWrap(array, offset, size), offset, size, Unit)
    actual val byteOffset: Int get() = offset
    actual val sizeInBytes: Int get() = size

    val end: Int = offset + size
    internal actual fun sliceInternal(start: Int, end: Int): Buffer = Buffer(data, offset + start, end - start)

    actual fun getS8(byteOffset: Int): Byte = data[offset + byteOffset]
    actual fun getS16LE(byteOffset: Int): Short = data.getShortAt(offset + byteOffset)
    actual fun getS32LE(byteOffset: Int): Int = data.getIntAt(offset + byteOffset)
    actual fun getS64LE(byteOffset: Int): Long = data.getLongAt(offset + byteOffset)
    actual fun getF32LE(byteOffset: Int): Float = data.getFloatAt(offset + byteOffset)
    actual fun getF64LE(byteOffset: Int): Double = data.getDoubleAt(offset + byteOffset)

    actual fun getS16BE(byteOffset: Int): Short = getS16LE(byteOffset).reverseBytes()
    actual fun getS32BE(byteOffset: Int): Int = getS32LE(byteOffset).reverseBytes()
    actual fun getS64BE(byteOffset: Int): Long = getS64LE(byteOffset).reverseBytes()
    actual fun getF32BE(byteOffset: Int): Float = getS32BE(byteOffset).reinterpretAsFloat()
    actual fun getF64BE(byteOffset: Int): Double = getS64BE(byteOffset).reinterpretAsDouble()

    actual fun set8(byteOffset: Int, value: Byte) { data[offset + byteOffset] = value }
    actual fun set16LE(byteOffset: Int, value: Short) { data.setShortAt(offset + byteOffset, value) }
    actual fun set32LE(byteOffset: Int, value: Int) { data.setIntAt(offset + byteOffset, value) }
    actual fun set64LE(byteOffset: Int, value: Long) { data.setLongAt(offset + byteOffset, value) }
    actual fun setF32LE(byteOffset: Int, value: Float) { data.setFloatAt(offset + byteOffset, value) }
    actual fun setF64LE(byteOffset: Int, value: Double) { data.setDoubleAt(offset + byteOffset, value) }

    actual fun set16BE(byteOffset: Int, value: Short) { set16LE(byteOffset, value.reverseBytes()) }
    actual fun set32BE(byteOffset: Int, value: Int) { set32LE(byteOffset, value.reverseBytes()) }
    actual fun set64BE(byteOffset: Int, value: Long) { set64LE(byteOffset, value.reverseBytes()) }
    actual fun setF32BE(byteOffset: Int, value: Float) { set32BE(byteOffset, value.reinterpretAsInt()) }
    actual fun setF64BE(byteOffset: Int, value: Double) { set64BE(byteOffset, value.reinterpretAsLong()) }


    override fun hashCode(): Int = hashCodeCommon(this)
    override fun equals(other: Any?): Boolean = equalsCommon(this, other)
    override fun toString(): String = NBuffer_toString(this)

    actual fun transferBytes(bufferOffset: Int, array: ByteArray, arrayOffset: Int, len: Int, toArray: Boolean) {
        val bufOffset = this.byteOffset + bufferOffset
        if (toArray) {
            arraycopy(this.data, bufOffset, array, arrayOffset, len)
        } else {
            arraycopy(array, arrayOffset, this.data, bufOffset, len)
        }
    }

    actual companion object {
        actual fun equals(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int): Boolean {
            check(srcPosBytes + sizeInBytes <= src.sizeInBytes)
            check(dstPosBytes + sizeInBytes <= dst.sizeInBytes)
            src.data.usePinned { srcPin ->
                dst.data.usePinned { dstPin ->
                    return memcmp(srcPin.startAddressOf + src.offset + srcPosBytes, dstPin.startAddressOf + dst.offset + dstPosBytes, sizeInBytes.convert()) == 0
                }
            }
        }
        actual fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {
            arraycopy(
                src.data, src.offset + srcPosBytes,
                dst.data, dst.offset + dstPosBytes,
                sizeInBytes
            )
        }
    }
}

class NBufferTempAddress {
    val pool = arrayListOf<Pinned<ByteArray>>()
    companion object {
        val ARRAY1 = ByteArray(1)
    }
    fun Buffer.unsafeAddress(): CPointer<ByteVar> {
        val byteArray = this.data
        val rbyteArray = if (byteArray.size > 0) byteArray else ARRAY1
        val pin = rbyteArray.pin()
        pool += pin
        return pin.addressOf(this.byteOffset)
    }

    fun start() {
        pool.clear()
    }

    fun dispose() {
        // Kotlin-native: Try to avoid allocating an iterator (lists not optimized yet)
        for (n in 0 until pool.size) pool[n].unpin()
        //for (p in pool) p.unpin()
        pool.clear()
    }

    inline operator fun <T> invoke(callback: NBufferTempAddress.() -> T): T {
        start()
        try {
            return callback()
        } finally {
            dispose()
        }
    }
}
