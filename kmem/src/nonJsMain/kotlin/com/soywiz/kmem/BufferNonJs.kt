package com.soywiz.kmem


/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Int8Buffer, srcPos: Int, dst: Int8Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos * 1, dst.mem, dstPos * 1, size * 1)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: ByteArray, srcPos: Int, dst: Int8Buffer, dstPos: Int, size: Int): Unit = arraycopy(src, srcPos, dst.mem, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Int8Buffer, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos, dst, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Int16Buffer, srcPos: Int, dst: Int16Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos * 2, dst.mem, dstPos * 2, size * 2)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: ShortArray, srcPos: Int, dst: Int16Buffer, dstPos: Int, size: Int): Unit = arraycopy(src, srcPos, dst.mem, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Int16Buffer, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos, dst, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Int32Buffer, srcPos: Int, dst: Int32Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos * 4, dst.mem, dstPos * 4, size * 4)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: IntArray, srcPos: Int, dst: Int32Buffer, dstPos: Int, size: Int): Unit = arraycopy(src, srcPos, dst.mem, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Int32Buffer, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos, dst, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Float32Buffer, srcPos: Int, dst: Float32Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos * 4, dst.mem, dstPos * 4, size * 4)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: FloatArray, srcPos: Int, dst: Float32Buffer, dstPos: Int, size: Int): Unit = arraycopy(src, srcPos, dst.mem, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Float32Buffer, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos, dst, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Float64Buffer, srcPos: Int, dst: Float64Buffer, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos * 8, dst.mem, dstPos * 8, size * 8)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: DoubleArray, srcPos: Int, dst: Float64Buffer, dstPos: Int, size: Int): Unit = arraycopy(src, srcPos, dst.mem, dstPos, size)
/** Copies [size] elements of [src] starting at [srcPos] into [dst] at [dstPos]  */
public actual fun arraycopy(src: Float64Buffer, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Unit = arraycopy(src.mem, srcPos, dst, dstPos, size)
