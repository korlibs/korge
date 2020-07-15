package org.jbox2d.internal

internal inline fun <T> arraycopy(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun arraycopy(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, size: Int): Unit =
    run { src.copyInto(dst, dstPos, srcPos, srcPos + size) }

internal inline fun assert(boolean: Boolean) = check(boolean)
internal inline fun assert(boolean: Boolean, message: () -> String) = check(boolean)

internal fun <T : Comparable<T>> Arrays_sort(array: Array<T>, fromIndex: Int, toIndex: Int) {
    val sorted = array.copyOfRange(fromIndex, toIndex).sortedArray()
    arraycopy(sorted, 0, array, fromIndex, toIndex - fromIndex)
}

internal fun Arrays_sort(array: LongArray, fromIndex: Int, toIndex: Int) {
    val sorted = array.copyOfRange(fromIndex, toIndex).sortedArray()
    arraycopy(sorted, 0, array, fromIndex, toIndex - fromIndex)
}

expect fun System_nanoTime(): Long
