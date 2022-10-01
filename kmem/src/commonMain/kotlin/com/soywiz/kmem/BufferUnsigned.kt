package com.soywiz.kmem

import kotlin.jvm.JvmInline

interface BaseIntBuffer {
    val size: Int
    operator fun get(index: Int): Int
    operator fun set(index: Int, value: Int)
}

@JvmInline
value class IntArrayIntBuffer(val array: IntArray) : BaseIntBuffer {
    override val size: Int get() = array.size
    override operator fun get(index: Int): Int = array[index]
    override operator fun set(index: Int, value: Int) { array[index] = value }
}

public inline class Uint8Buffer(public val b: Int8Buffer) : BaseIntBuffer {
    public companion object;
    val buffer: MemBuffer get() = b.mem
    override val size: Int get() = b.size
    val offset: Int get() = b.offset
    val mem: MemBuffer get() = b.mem
    override operator fun get(index: Int): Int = b[index].toInt() and 0xFF
    override operator fun set(index: Int, value: Int) { b[index] = value.toByte() }

    override fun toString(): String = "[" + (0 until kotlin.math.min(size, 100)).joinToString(", ") { "${this[it]}" } + "...]"
}
public fun Uint8Buffer.subarray(begin: Int, end: Int = this.size): Uint8Buffer = Uint8Buffer(this.b.subarray(begin, end))

public inline class Uint8ClampedBuffer(public val b: Int8Buffer) : BaseIntBuffer {
    public companion object;
    val buffer: MemBuffer get() = b.mem
    override val size: Int get() = b.size
    val offset: Int get() = b.offset
    val mem: MemBuffer get() = b.mem
    override operator fun get(index: Int): Int = b[index].toInt() and 0xFF
    override operator fun set(index: Int, value: Int) { b[index] = value.clamp(0, 255).toByte() }
}
public fun Uint8ClampedBuffer.subarray(begin: Int, end: Int = this.size): Uint8ClampedBuffer = Uint8ClampedBuffer(this.b.subarray(begin, end))

public inline class Uint16Buffer(public val b: Int16Buffer) : BaseIntBuffer {
    public companion object;
    val buffer: MemBuffer get() = b.mem
    public override val size: Int get() = b.size
    val offset: Int get() = b.offset
    val mem: MemBuffer get() = b.mem
    override operator fun get(index: Int): Int = b[index].toInt() and 0xFFFF
    override operator fun set(index: Int, value: Int) { b[index] = value.toShort() }
}
public fun Uint16Buffer.subarray(begin: Int, end: Int = this.size): Uint16Buffer = Uint16Buffer(this.b.subarray(begin, end))

@Deprecated("", ReplaceWith("Uint8Buffer(size)"))
public fun Uint8BufferAlloc(size: Int): Uint8Buffer = Uint8Buffer(size)
public fun Uint8Buffer(size: Int): Uint8Buffer = Uint8Buffer(Int8Buffer(size))
public fun Uint8Buffer(array: ByteArray): Uint8Buffer = Uint8Buffer(com.soywiz.kmem.buffer.Int8Buffer(MemBufferWrap(array)))
public fun Uint8Buffer(buffer: MemBuffer): Uint8Buffer = Uint8Buffer(com.soywiz.kmem.buffer.Int8Buffer(buffer))

public fun Uint8ClampedBuffer(size: Int): Uint8ClampedBuffer = Uint8ClampedBuffer(Int8Buffer(size))
public fun Uint8ClampedBuffer(array: ByteArray): Uint8ClampedBuffer = Uint8ClampedBuffer(com.soywiz.kmem.buffer.Int8Buffer(MemBufferWrap(array)))
public fun Uint8ClampedBuffer(buffer: MemBuffer): Uint8ClampedBuffer = Uint8ClampedBuffer(com.soywiz.kmem.buffer.Int8Buffer(buffer))

public fun Uint32Buffer(size: Int): Uint32Buffer = Uint32Buffer(Int32Buffer(size))
public fun Uint32Buffer(buffer: MemBuffer): Uint32Buffer = Uint32Buffer(com.soywiz.kmem.buffer.Int32Buffer(buffer))

@Deprecated("", ReplaceWith("Uint16Buffer(size)"))
public fun Uint16BufferAlloc(size: Int): Uint16Buffer = Uint16Buffer(size)
public fun Uint16Buffer(size: Int): Uint16Buffer = Uint16Buffer(Int16Buffer(size))

@Deprecated("", ReplaceWith("Uint8Buffer(mem, offset, len)"))
public fun NewUint8Buffer(mem: MemBuffer, offset: Int, len: Int): Uint8Buffer = com.soywiz.kmem.buffer.Uint8Buffer(mem, offset, len)
@Deprecated("", ReplaceWith("Uint16Buffer(mem, offset, len)"))
public fun NewUint16Buffer(mem: MemBuffer, offset: Int, len: Int): Uint16Buffer = com.soywiz.kmem.buffer.Uint16Buffer(mem, offset, len)
@Deprecated("", ReplaceWith("Int8Buffer(mem, offset, len)"))
public fun NewInt8Buffer(mem: MemBuffer, offset: Int, len: Int): Int8Buffer = com.soywiz.kmem.buffer.Int8Buffer(mem, offset, len)

/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: Uint8Buffer, srcPos: Int, dst: Uint8Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.mem, (src.offset + srcPos) * 1, dst.mem, (dst.offset + dstPos) * 1, size * 1)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: UByteArray, srcPos: Int, dst: Uint8Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.asByteArray(), srcPos, dst.mem, (dst.offset + dstPos), size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public fun arraycopy(src: Uint8Buffer, srcPos: Int, dst: UByteArray, dstPos: Int, size: Int): Unit = arraycopy(src.mem, (src.offset + srcPos), dst.asByteArray(), dstPos, size)
