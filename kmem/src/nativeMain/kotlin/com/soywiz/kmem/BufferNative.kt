package com.soywiz.kmem

import kotlinx.cinterop.*

actual class Buffer(val data: ByteArray, val offset: Int, val size: Int, dummy: Unit) {
    val end: Int = offset + size
    override fun toString(): String = NBuffer_toString(this)
    actual companion object {
        actual fun copy(src: Buffer, srcPosBytes: Int, dst: Buffer, dstPosBytes: Int, sizeInBytes: Int) {
            arraycopy(
                src.data, src.offset + srcPosBytes,
                dst.data, dst.offset + dstPosBytes,
                sizeInBytes
            )
        }
    }

    // @TODO: Optimize by using words instead o bytes
    override fun hashCode(): Int {
        var h = 1
        for (n in 0 until size) h = 31 * h + data[offset + n]
        return h
    }

    // @TODO: Optimize by using words instead o bytes
    override fun equals(other: Any?): Boolean {
        if (other !is Buffer || this.size != other.size) return false
        val t = this.data
        val o = other.data
        for (n in 0 until size) if (t[this.offset + n] != o[other.offset + n]) return false
        return true
    }
}
actual fun Buffer(size: Int, direct: Boolean): Buffer {
    checkNBufferSize(size)
    return Buffer(ByteArray(size), 0, size, Unit)
}
actual fun Buffer(array: ByteArray, offset: Int, size: Int): Buffer {
    checkNBufferWrap(array, offset, size)
    return Buffer(array, offset, size, Unit)
}
actual val Buffer.byteOffset: Int get() = offset
actual val Buffer.sizeInBytes: Int get() = size
internal actual fun Buffer.sliceInternal(start: Int, end: Int): Buffer = Buffer(data, offset + start, end - start)

// Unaligned versions

actual fun Buffer.getUnalignedInt8(byteOffset: Int): Byte = data[offset + byteOffset]
actual fun Buffer.getUnalignedInt16(byteOffset: Int): Short = data.getShortAt(offset + byteOffset)
actual fun Buffer.getUnalignedInt32(byteOffset: Int): Int = data.getIntAt(offset + byteOffset)
actual fun Buffer.getUnalignedInt64(byteOffset: Int): Long = data.getLongAt(offset + byteOffset)
actual fun Buffer.getUnalignedFloat32(byteOffset: Int): Float = data.getFloatAt(offset + byteOffset)
actual fun Buffer.getUnalignedFloat64(byteOffset: Int): Double = data.getDoubleAt(offset + byteOffset)

actual fun Buffer.setUnalignedInt8(byteOffset: Int, value: Byte) { data[offset + byteOffset] = value }
actual fun Buffer.setUnalignedInt16(byteOffset: Int, value: Short) { data.setShortAt(offset + byteOffset, value) }
actual fun Buffer.setUnalignedInt32(byteOffset: Int, value: Int) { data.setIntAt(offset + byteOffset, value) }
actual fun Buffer.setUnalignedInt64(byteOffset: Int, value: Long) { data.setLongAt(offset + byteOffset, value) }
actual fun Buffer.setUnalignedFloat32(byteOffset: Int, value: Float) { data.setFloatAt(offset + byteOffset, value) }
actual fun Buffer.setUnalignedFloat64(byteOffset: Int, value: Double) { data.setDoubleAt(offset + byteOffset, value) }

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
